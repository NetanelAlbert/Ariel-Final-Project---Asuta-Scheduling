package model.DTOs

import org.joda.time.LocalDateTime

case class FutureSurgeryInfo
(
    operationCode : Double,
    doctorId : Int,
    surgeryStartTime : LocalDateTime,
    surgeryFinishTime : LocalDateTime,
    operationRoom : Int
)

