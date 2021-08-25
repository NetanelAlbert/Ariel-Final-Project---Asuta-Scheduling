package model.DTOs

import org.joda.time.LocalDateTime


case class PastSurgeryInfo
(
    operationCode : Double,
    doctorId : Int,
    surgeryDurationMinutes : Int,
    restingMinutes : Int,
    hospitalizationHours : Int,
    blockStart : LocalDateTime,
    blockEnd : LocalDateTime
)
