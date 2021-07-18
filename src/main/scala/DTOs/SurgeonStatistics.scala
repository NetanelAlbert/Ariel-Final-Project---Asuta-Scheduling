package DTOs

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution

import java.sql.{Date, Timestamp}

case class SurgeonStatistics
(
    id : Int,
    name : String,
    amountOfData : Int,
    profitAvg : Double,
    surgeryDurationAvgMinutes : Double,
    restingDurationAvgMinutes : Double,
    hospitalizationDurationAvgHours : Double,
    globalAvg : Double
)
object SurgeonStatistics
{
    // To compute "globalAvg" auto, but still enable json formatting
    def apply(id : Int,
              name : String,
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

case class SurgeryStatistics
(
    operationCode : Double,
    restingDistribution : EnumeratedIntegerDistribution,
    hospitalizationDistribution : EnumeratedIntegerDistribution,
    profit : Double
)

case class SurgeryInfo
(
    operationCode : Double,
    surgeonId : Int,
    surgeryDurationMinutes : Int,
    restingMinutes : Int,
    hospitalizationHours : Int,
    blockStart : Timestamp,
    blockEnd : Timestamp
)

object FormattingProtocols
{
    import spray.json.DefaultJsonProtocol._
    
    implicit val SurgeonStatisticsFormat = jsonFormat8(SurgeonStatistics.apply)
    
}