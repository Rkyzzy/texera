package edu.uci.ics.texera.workflow.operators.limit

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyDescription}
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle
import edu.uci.ics.amber.engine.common.Constants
import edu.uci.ics.amber.engine.operators.OpExecConfig
import edu.uci.ics.texera.workflow.common.metadata.{OperatorGroupConstants, OperatorInfo}
import edu.uci.ics.texera.workflow.common.operators.{OneToOneOpExecConfig, OperatorDescriptor}
import edu.uci.ics.texera.workflow.common.tuple.schema.Schema
import edu.uci.ics.texera.workflow.operators.limit.LimitOpDesc.equallyPartitionGoal
import edu.uci.ics.texera.workflow.operators.util.OperatorDescriptorUtils.equallyPartitionGoal

import scala.collection.mutable

class LimitOpDesc extends OperatorDescriptor {

  @JsonProperty(required = true)
  @JsonSchemaTitle("Limit")
  @JsonPropertyDescription("the max number of output rows")
  var limit: Int = _

  override def operatorExecutor: OpExecConfig = {
    val limitPerWorker = equallyPartitionGoal(limit, Constants.defaultNumWorkers)
    new OneToOneOpExecConfig(this.operatorIdentifier, i => new LimitOpExec(limitPerWorker(i)))
  }

  override def operatorInfo: OperatorInfo = OperatorInfo(
    "Limit", "Limit the number of output rows", OperatorGroupConstants.UTILITY_GROUP, 1, 1
  )

  override def getOutputSchema(schemas: Array[Schema]): Schema = schemas(0)
}
