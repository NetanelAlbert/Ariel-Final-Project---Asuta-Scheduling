package model.DTOs

case class SurgeonStatistics
(
    id : Int,
    name : Option[String],
    amountOfData : Int,
    profitAvg : Double,
    surgeryDurationAvgMinutes : Double,
    restingDurationAvgMinutes : Double,
    hospitalizationDurationAvgHours : Double,
    globalAvg : Double
)
object SurgeonStatistics
{
    val tupled = (this.apply _).tupled
}

object SurgeonStatisticsAutoAvg
{
    // To compute "globalAvg" auto, but still enable json formatting
    def apply(id : Int,
              name : Option[String],
              amountOfData : Int,
              profitAvg : Double,
              surgeryDurationAvgMinutes : Double,
              restingDurationAvgMinutes : Double,
              hospitalizationDurationAvgHours : Double
             ) : SurgeonStatistics =
    {
        SurgeonStatistics(
            id,
            name,
            amountOfData,
            profitAvg,
            surgeryDurationAvgMinutes,
            restingDurationAvgMinutes,
            hospitalizationDurationAvgHours,
            //TODO :: choose real weights instead of 1
            {
                profitAvg * 1 +
                surgeryDurationAvgMinutes * 1 +
                restingDurationAvgMinutes * 1 +
                hospitalizationDurationAvgHours * 1
            }
        )
    }
}

