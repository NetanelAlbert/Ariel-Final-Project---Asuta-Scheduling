package model.DTOs

import java.util.Date

case class FutureSurgeryInfo
(
    operationCode : Double,
    doctorId : Int,
    surgeryFinishTime : Date,
)

