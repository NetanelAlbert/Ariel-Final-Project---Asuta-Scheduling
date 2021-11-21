package model.DTOs

case class SurgeryBasicInfo
(
    operationCode : Double,
    name : Option[String]
)
{
    def nameOrCode = name.getOrElse(s"$operationCode (code)")
}
