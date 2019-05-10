package shredding.algebra

sealed trait Type{
  def isLabel: Boolean = false
}

trait PrimitiveType extends Type

case object BoolType extends PrimitiveType

case object IntType extends PrimitiveType

case object StringType extends PrimitiveType

case class BagType(tp: Type) extends Type

// tuple type that comes from above
case class TupleType(attrTps: Map[String, Type]) extends Type {
  def apply(n: String): Type = attrTps(n)
}

// (K, V)
case class KVTupleType(e1: Type, e2: Type) extends Type
