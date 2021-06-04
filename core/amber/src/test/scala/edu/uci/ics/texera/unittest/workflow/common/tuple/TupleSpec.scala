package edu.uci.ics.texera.unittest.workflow.common.tuple

import edu.uci.ics.texera.workflow.common.tuple.Tuple
import edu.uci.ics.texera.workflow.common.tuple.exception.TupleBuildingException
import edu.uci.ics.texera.workflow.common.tuple.schema.{Attribute, AttributeType, Schema}
import org.scalatest.flatspec.AnyFlatSpec

class TupleSpec extends AnyFlatSpec {
  val stringAttribute = new Attribute("col-string", AttributeType.STRING)
  val integerAttribute = new Attribute("col-int", AttributeType.INTEGER)
  val boolAttribute = new Attribute("col-bool", AttributeType.BOOLEAN)

  it should "create a tuple using new builder, based on another tuple using old builder" in {
    val inputTuple = Tuple.newBuilder().add(stringAttribute, "string-value").build()
    val newTuple = Tuple.newBuilder(inputTuple.getSchema).add(inputTuple).build()

    assert(newTuple.size == inputTuple.size)
  }

  it should "fail when unknown attribute is added to tuple" in {
    val schema = Schema.newBuilder().add(stringAttribute).build()
    assertThrows[TupleBuildingException] {
      Tuple.newBuilder(schema).add(integerAttribute, 1)
    }
  }

  it should "fail when tuple does not conform to complete schema" in {
    val schema = Schema.newBuilder().add(stringAttribute).add(integerAttribute).build()
    assertThrows[TupleBuildingException] {
      Tuple.newBuilder(schema).add(integerAttribute, 1).build()
    }
  }

  it should "not fail when entire tuple passed in has extra attributes" in {
    val inputSchema = Schema.newBuilder().add(stringAttribute).add(integerAttribute).add(boolAttribute).build()
    val inputTuple = Tuple.newBuilder(inputSchema).add(integerAttribute, 1).add(stringAttribute, "string-attr").add(boolAttribute, true).build()

    val outputSchema = Schema.newBuilder().add(stringAttribute).add(integerAttribute).build()
    val outputTuple = Tuple.newBuilder(outputSchema).add(inputTuple).build()

    assert(outputTuple.size == 2)
  }
}
