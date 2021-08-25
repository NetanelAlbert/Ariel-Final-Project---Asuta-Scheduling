package model.DTOs

import model.probability.IntegerDistribution

case class SurgeryStatistics
(
    operationCode : Double,
    operationName : Option[String],
    restingDistribution : IntegerDistribution,
    hospitalizationDistribution : IntegerDistribution,
    profit : Option[Int],
    amountOfData : Int
)

object SurgeryStatisticsImplicits
{
    implicit class SurgeryStatisticsToSurgeryBasicInfo(surgeryStatistics: SurgeryStatistics)
    {
        def basicInfo : SurgeryBasicInfo =
        {
            val SurgeryStatistics(operationCode, operationName, _, _, _, _) = surgeryStatistics
            SurgeryBasicInfo(operationCode, operationName)
        }
    }
}
