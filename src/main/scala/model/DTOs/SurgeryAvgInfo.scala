package model.DTOs

case class SurgeryAvgInfo
(
    operationCode : Double,
    amountOfData : Int,
    surgeryDurationAvgMinutes : Double,
    restingDurationAvgMinutes : Double,
    hospitalizationDurationAvgHours : Double
)
