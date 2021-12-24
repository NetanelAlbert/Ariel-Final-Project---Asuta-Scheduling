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
    
    def globalAvg(settings : Settings) : Double =
    {
        surgeryDurationAvgMinutes * settings.doctorRankingSurgeryTimeWeight +
        restingDurationAvgMinutes * settings.doctorRankingRestingTimeWeight +
        hospitalizationDurationAvgHours * settings.doctorRankingHospitalizationTimeWeight +
        profit.getOrElse(0) * settings.doctorRankingProfitWeight
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



