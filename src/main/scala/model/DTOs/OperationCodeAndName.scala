package model.DTOs

case class OperationCodeAndName(operationCode : Double, name : Option[String])
{
    override def toString = name.getOrElse(s"$operationCode (code)")
}
