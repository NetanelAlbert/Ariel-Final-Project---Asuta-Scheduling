package model.DTOs

import model.probability.IntegerDistribution

case class SurgeryStatistics
(
    operationCode : Double,
    operationName : Option[String],
    restingDistribution : IntegerDistribution,
    hospitalizationDistribution : IntegerDistribution,
    profit : Option[Double]
)
