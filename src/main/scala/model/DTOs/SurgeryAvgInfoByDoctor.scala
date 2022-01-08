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
    private var weightCache : Option[Int] = None
    //TODO use also the global avg in case of not enough data
    def durationIncludePrepareTime(settings : Settings) : Int =
    {
        weightCache match
        {
            case Some(value) => value
            
            case None =>
            {
                val weight = surgeryDurationAvgMinutes.toInt + prepareTime(settings)
                weightCache = Some(weight)
                weight
            }
        }
    }
    
    def prepareTime(settings : Settings) : Int =
    {
        if (surgeryDurationAvgMinutes < settings.longSurgeryDefinitionMinutes)
        {
            settings.shortSurgeryPrepareTimeMinutes
        } else
        {
            settings.longSurgeryPrepareTimeMinutes
        }
    }
    
    def value : Int = amountOfData
}
