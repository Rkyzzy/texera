package edu.uci.ics.amber.engine.architecture.worker

import java.util.concurrent.{CompletableFuture, Executors, LinkedBlockingDeque}

import edu.uci.ics.amber.engine.architecture.controller.promisehandlers.WorkerExecutionCompletedHandler.WorkerExecutionCompleted
import edu.uci.ics.amber.engine.architecture.controller.promisehandlers.LinkCompletedHandler.LinkCompleted
import edu.uci.ics.amber.engine.architecture.controller.promisehandlers.LocalOperatorExceptionHandler.LocalOperatorException
import edu.uci.ics.amber.engine.architecture.controller.promisehandlers.WorkerExecutionCompletedHandler.WorkerExecutionCompleted
import edu.uci.ics.amber.engine.architecture.messaginglayer.TupleToBatchConverter
import edu.uci.ics.amber.engine.architecture.worker.WorkerInternalQueue._
import edu.uci.ics.amber.engine.common.{IOperatorExecutor, InputExhausted, WorkflowLogger}
import edu.uci.ics.amber.engine.common.ambermessage.ControlPayload
import edu.uci.ics.amber.engine.common.rpc.{AsyncRPCClient, AsyncRPCServer}
import edu.uci.ics.amber.engine.common.rpc.AsyncRPCClient.{ControlInvocation, ReturnPayload}
import edu.uci.ics.amber.engine.common.statetransition.WorkerStateManager
import edu.uci.ics.amber.engine.common.statetransition.WorkerStateManager.{
  Completed,
  Ready,
  Running
}
import edu.uci.ics.amber.engine.common.tuple.ITuple
import edu.uci.ics.amber.engine.common.virtualidentity.{
  ActorVirtualIdentity,
  LinkIdentity,
  VirtualIdentity
}
import edu.uci.ics.amber.error.ErrorUtils.safely
import edu.uci.ics.amber.error.WorkflowRuntimeError
import java.util.concurrent.{ExecutorService, Executors, Future}

import edu.uci.ics.amber.engine.architecture.controller.promisehandlers.WorkerExecutionStartedHandler.WorkerStateUpdated
import edu.uci.ics.amber.engine.recovery.{DPLogManager, InputCounter}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class DataProcessor( // dependencies:
    logger: WorkflowLogger, // logger of the worker actor
    operator: IOperatorExecutor, // core logic
    asyncRPCClient: AsyncRPCClient, // to send controls
    batchProducer: TupleToBatchConverter, // to send output tuples
    pauseManager: PauseManager, // to pause/resume
    breakpointManager: BreakpointManager, // to evaluate breakpoints
    stateManager: WorkerStateManager,
    asyncRPCServer: AsyncRPCServer,
    dpLogManager: DPLogManager,
    inputCounter: InputCounter
) extends WorkerInternalQueue {
  // dp thread stats:
  private var inputTupleCount = 0L
  private var outputTupleCount = 0L
  private var currentInputTuple: Either[ITuple, InputExhausted] = _
  private var currentInputLink: LinkIdentity = _
  private var currentOutputIterator: Iterator[ITuple] = _
  private var isCompleted = false
  private var dataCursor = 0L
  private val controlRecoveryQueue = mutable.Queue[ControlElement]()
  private var startTime = 0L
  private var processingTime = 0L

  // initialize dp thread upon construction
  private val dpThreadExecutor: ExecutorService = Executors.newSingleThreadExecutor
  private val dpThread: Future[_] = dpThreadExecutor.submit(new Runnable() {
    def run(): Unit = {
      try {
        // initialize operator
        operator.open()
        startTime = System.currentTimeMillis()
        runDPThreadMainLogic()
      } catch safely {
        case e: InterruptedException =>
          logger.logInfo("DP Thread exits")
        case e =>
          val error = WorkflowRuntimeError(e, "DP Thread internal logic")
          logger.logError(error)
        // dp thread will stop here
      }
    }
  })

  /** provide API for actor to get stats of this operator
    * @return (input tuple count, output tuple count)
    */
  def collectStatistics(): (Long, Long) = (inputTupleCount, outputTupleCount)

  /** provide API for actor to get current input tuple of this operator
    * @return current input tuple if it exists
    */
  def getCurrentInputTuple: ITuple = {
    if (currentInputTuple != null && currentInputTuple.isLeft) {
      currentInputTuple.left.get
    } else {
      null
    }
  }

  def setCurrentTuple(tuple: Either[ITuple, InputExhausted]): Unit = {
    currentInputTuple = tuple
  }

  /** process currentInputTuple through operator logic.
    * this function is only called by the DP thread
    * @return an iterator of output tuples
    */
  private[this] def processInputTuple(): Iterator[ITuple] = {
    var outputIterator: Iterator[ITuple] = null
    try {
      outputIterator = operator.processTuple(currentInputTuple, currentInputLink)
      if (currentInputTuple.isLeft) {
        inputTupleCount += 1
      }
    } catch safely {
      case e =>
        // forward input tuple to the user and pause DP thread
        handleOperatorException(e)
    }
    outputIterator
  }

  /** transfer one tuple from iterator to downstream.
    * this function is only called by the DP thread
    */
  private[this] def outputOneTuple(): Unit = {
    var outputTuple: ITuple = null
    try {
      outputTuple = currentOutputIterator.next
    } catch safely {
      case e =>
        // invalidate current output tuple
        outputTuple = null
        // also invalidate outputIterator
        currentOutputIterator = null
        // forward input tuple to the user and pause DP thread
        handleOperatorException(e)
    }
    if (outputTuple != null) {
      if (breakpointManager.evaluateTuple(outputTuple)) {
        pauseManager.pause()
      } else {
        outputTupleCount += 1
        batchProducer.passTupleToDownstream(outputTuple)
      }
    }
  }

  /** Provide main functionality of data processing
    * @throws Exception (from engine code only)
    */
  @throws[Exception]
  private[this] def runDPThreadMainLogic(): Unit = {
    // main DP loop
    while (!isCompleted) {
      // take the next data element from internal queue, blocks if not available.
      val elem = getElement
      val start = System.currentTimeMillis()
      elem match {
        case EnableInputCounter =>
          dpLogManager.onComplete { () =>
            inputCounter.enable()
          }
        case InputTuple(tuple) =>
          transitStateToRunningFromReady()
          inputCounter.advanceDataInputCount()
          currentInputTuple = Left(tuple)
          handleInputTuple()
        case SenderChangeMarker(link) =>
          currentInputLink = link
        case EndMarker =>
          transitStateToRunningFromReady()
          if (currentInputLink != null) {
            inputCounter.advanceDataInputCount()
          }
          currentInputTuple = Right(InputExhausted())
          handleInputTuple()
          if (currentInputLink != null) {
            asyncRPCClient.send(LinkCompleted(currentInputLink), ActorVirtualIdentity.Controller)
          }
        case EndOfAllMarker =>
          transitStateToRunningFromReady()
          // end of processing, break DP loop
          isCompleted = true
          batchProducer.emitEndOfUpstream()
        case ctrl: ControlElement =>
          handleControlElement(ctrl)
      }
      processingTime += System.currentTimeMillis() - start
    }
    // Send Completed signal to worker actor.
    logger.logInfo(
      s"${operator.toString} completed in ${(System.currentTimeMillis() - startTime) / 1000f}"
    )
    dataCursor += 1 //increment data cursor to distinguish control messages before/after completion
    asyncRPCClient.send(WorkerExecutionCompleted(), ActorVirtualIdentity.Controller)
    stateManager.transitTo(Completed)
    disableDataQueue()
    takeControlElementsAfterCompletion()
  }

  private[this] def handleOperatorException(e: Throwable): Unit = {
    if (currentInputTuple.isLeft) {
      asyncRPCClient.send(
        LocalOperatorException(currentInputTuple.left.get, e),
        ActorVirtualIdentity.Controller
      )
    } else {
      asyncRPCClient.send(
        LocalOperatorException(ITuple("input exhausted"), e),
        ActorVirtualIdentity.Controller
      )
    }
    logger.logWarning(e.getLocalizedMessage + "\n" + e.getStackTrace.mkString("\n"))
    pauseManager.pause()
  }

  private[this] def handleInputTuple(): Unit = {
    // process controls before processing the input tuple.
    takeControlElementsDuringExecution()
    if (currentInputTuple != null) {
      // pass input tuple to operator logic.
      currentOutputIterator = processInputTuple()
      // process controls before outputting tuples.
      takeControlElementsDuringExecution()
      // output loop: take one tuple from iterator at a time.
      while (outputAvailable(currentOutputIterator)) {
        // send tuple to downstream.
        outputOneTuple()
        // process controls after one tuple has been outputted.
        takeControlElementsDuringExecution()
      }
    }
  }

  def shutdown(): Unit = {
    logger.logInfo(s"processing time: ${processingTime / 1000f}")
    operator.close() // close operator
    dpThread.cancel(true) // interrupt
    dpThreadExecutor.shutdownNow() // destroy thread
  }

  private[this] def outputAvailable(outputIterator: Iterator[ITuple]): Boolean = {
    try {
      outputIterator != null && outputIterator.hasNext
    } catch safely {
      case e =>
        handleOperatorException(e)
        false
    }
  }

  private[this] def takeControlElementsDuringExecution(
      advanceDataCursor: Boolean = true
  ): Unit = {
    if (advanceDataCursor) {
      dataCursor += 1
    }
    if (dpLogManager.isRecovering) {
      replayControlCommands()
    }
    while (!isControlQueueEmpty || pauseManager.isPaused) {
      val control = getElement.asInstanceOf[ControlElement]
      handleControlElement(control)
    }
  }

  private[this] def takeControlElementsAfterCompletion(): Unit = {
    while (true) {
      if (dpLogManager.isRecovering) {
        replayControlCommands()
      }
      val control = getElement.asInstanceOf[ControlElement]
      val start = System.currentTimeMillis()
      handleControlElement(control)
      processingTime += System.currentTimeMillis() - start
    }
  }

  private[this] def handleControlElement(control: ControlElement): Unit = {
    if (dpLogManager.isRecovering) {
      replayControlCommands(control)
    } else {
      processControlCommand(control.cmd, control.from)
    }
  }

  private[this] def replayControlCommands(control: ControlElement = null): Unit = {
    if (control != null) {
      controlRecoveryQueue.enqueue(control)
    }
    while (dpLogManager.isCurrentCorrelated(dataCursor) && controlRecoveryQueue.nonEmpty) {
      val elem = controlRecoveryQueue.dequeue()
      processControlCommand(elem.cmd, elem.from)
      dpLogManager.advanceCursor()
    }
    if (!dpLogManager.isRecovering && controlRecoveryQueue.nonEmpty) {
      controlRecoveryQueue.foreach { elem =>
        processControlCommand(elem.cmd, elem.from)
      }
      controlRecoveryQueue.clear()
    }
  }

  private[this] def processControlCommand(cmd: ControlPayload, from: VirtualIdentity): Unit = {
    inputCounter.advanceControlInputCount()
    cmd match {
      case ShutdownDPThread() =>
        shutdown()
        new CompletableFuture[Void]().get
      case invocation: ControlInvocation =>
        persistCurrentDataCursorIfRecoveryCompleted()
        asyncRPCServer.logControlInvocation(invocation, from)
        asyncRPCServer.receive(invocation, from.asInstanceOf[ActorVirtualIdentity])
      case ret: ReturnPayload =>
        persistCurrentDataCursorIfRecoveryCompleted()
        asyncRPCClient.logControlReply(ret, from)
        asyncRPCClient.fulfillPromise(ret)
    }
  }

  private[this] def persistCurrentDataCursorIfRecoveryCompleted(): Unit = {
    if (!dpLogManager.isRecovering) {
      dpLogManager.persistCurrentDataCursor(dataCursor)
    }
  }

  private def transitStateToRunningFromReady(): Unit = {
    if (stateManager.getCurrentState == Ready) {
      stateManager.transitTo(Running)
      asyncRPCClient.send(
        WorkerStateUpdated(stateManager.getCurrentState),
        ActorVirtualIdentity.Controller
      )
    }
  }

}
