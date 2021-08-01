package model.DTOs

case class SurgeryAvgInfoByDoctor
(
    operationCode : Double,
    doctorId : Int,
    amountOfData : Int,
    surgeryDurationAvgMinutes : Double,
    restingDurationAvgMinutes : Double,
    hospitalizationDurationAvgHours : Double
)
