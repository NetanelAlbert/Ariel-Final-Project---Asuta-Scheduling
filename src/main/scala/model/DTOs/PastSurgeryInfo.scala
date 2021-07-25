package model.DTOs

import java.sql.Timestamp

case class PastSurgeryInfo
(
    operationCode : Double,
    surgeonId : Int,
    surgeryDurationMinutes : Int,
    restingMinutes : Int,
    hospitalizationHours : Int,
    blockStart : Timestamp,
    blockEnd : Timestamp
)
