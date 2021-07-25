package model.DTOs

import java.sql.Timestamp

case class PastSurgeryInfo
(
    operationCode : Double,
    doctorId : Int,
    surgeryDurationMinutes : Int,
    restingMinutes : Int,
    hospitalizationHours : Int,
    blockStart : Timestamp,
    blockEnd : Timestamp
)
