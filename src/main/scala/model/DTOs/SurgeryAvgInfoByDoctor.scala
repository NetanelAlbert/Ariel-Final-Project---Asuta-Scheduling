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
{
    //TODO use also the global avg in case of not enough data
    def weight : Int = surgeryDurationAvgMinutes.toInt + prepareTime
    
    def prepareTime : Int = if(surgeryDurationAvgMinutes < 2*60) 15 else 30 //TODO user settings
    
    def value : Int = amountOfData // todo - profit might be irrelevant and might pun weights on them
}
