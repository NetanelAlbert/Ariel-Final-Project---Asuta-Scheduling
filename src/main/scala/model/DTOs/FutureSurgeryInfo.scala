package model.DTOs

import org.joda.time.LocalDate

case class FutureSurgeryInfo
(
    operationCode : Double,
    doctorId : Int,
    surgeryFinishTime : LocalDate,
)

