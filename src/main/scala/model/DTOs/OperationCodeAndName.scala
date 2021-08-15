package model.DTOs

case class OperationCodeAndName(operationCode : Double, name : Option[String])
{
    override def toString = name.getOrElse(s"$operationCode (code)")
}
object OperationCodeAndName
{
    def applyPair(pair : (Double, Option[String])) : OperationCodeAndName = apply(pair._1, pair._2)
}
