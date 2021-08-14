package model.DTOs

import org.joda.time.LocalDate

import java.sql.Timestamp

case class PastSurgeryInfo
(
    operationCode : Double,
    doctorId : Int,
    surgeryDurationMinutes : Int,
    restingMinutes : Int,
    hospitalizationHours : Int,
    blockStart : LocalDate,
    blockEnd : LocalDate
)
