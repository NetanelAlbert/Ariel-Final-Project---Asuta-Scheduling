package model.DTOs


case class PastSurgeryInfo
(
    operationCode : Double,
    doctorId : Int,
    surgeryDurationMinutes : Int,
    restingMinutes : Int,
    hospitalizationHours : Int,
    blockStartMillis : Long,
)
