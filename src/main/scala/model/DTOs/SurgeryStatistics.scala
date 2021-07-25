package model.DTOs

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution

case class SurgeryStatistics
(
    operationCode : Double,
    restingDistribution : EnumeratedIntegerDistribution,
    hospitalizationDistribution : EnumeratedIntegerDistribution,
    profit : Double
)
