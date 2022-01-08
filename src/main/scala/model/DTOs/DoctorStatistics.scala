package model.DTOs

case class DoctorStatistics
(
    id : Int,
    name : Option[String],
    amountOfData : Int,
    profit : Option[Int],
    surgeryDurationAvgMinutes : Double,
    restingDurationAvgMinutes : Double,
    hospitalizationDurationAvgHours : Double,
)
{
    def nameOrId : String = name.getOrElse(s"$id (id)")
    def nameWithId : String = name.map(name => s"$name - $id").getOrElse(s"$id (id)")
    
    def globalAvg(
        settings : Settings,
        surgeryDurationNormalizer : Double => Double,
        restingDurationNormalizer : Double => Double,
        hospitalizationDurationNormalizer : Double => Double,
        profitNormalizer : Int => Double,
    ) : Double =
    {
        val sum = surgeryDurationNormalizer(surgeryDurationAvgMinutes) * settings.doctorRankingSurgeryTimeWeight +
                  restingDurationNormalizer(restingDurationAvgMinutes) * settings.doctorRankingRestingTimeWeight +
                  hospitalizationDurationNormalizer(hospitalizationDurationAvgHours) * settings.doctorRankingHospitalizationTimeWeight +
                  profitNormalizer(profit.orElse(settings.avgSurgeryProfit).getOrElse(0)) * settings.doctorRankingProfitWeight
        
        val div = settings.doctorRankingSurgeryTimeWeight +
                  settings.doctorRankingRestingTimeWeight +
                  settings.doctorRankingHospitalizationTimeWeight +
                  settings.doctorRankingProfitWeight
        
        sum / div
    }
}

object DoctorStatistics
{
    val tupled = (this.apply _).tupled
}

case class HourOfWeek(day : Int, hour : Int)
{
    require(1 <= day && day <= 7, s"day has to be between 1 to 7 but it is $day")
    require(0 <= hour && hour <= 23, s"hour has to be between 0 to 23 but it is $hour")
}

object HourOfWeek
{
    val pattern = "([0-9]+):([0-9]+)".r
    
    def fromDBString(string : String) : HourOfWeek =
    {
        val pattern(day, hour) = string
        new HourOfWeek(day.toInt, hour.toInt)
    }
    
    def toDBString(arg : HourOfWeek) : String = s"${arg.day}:${arg.hour}"
}



