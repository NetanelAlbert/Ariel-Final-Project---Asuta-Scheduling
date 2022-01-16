package model.DTOs

import model.DTOs.Priority.Priority
import model.probability.IntegerDistribution
import org.joda.time.{Days, LocalTime}

import java.io.{ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.sql.Blob
import javax.sql.rowset.serial.SerialBlob

object FormattingProtocols
{
    import spray.json.DefaultJsonProtocol._
    import slick.jdbc.HsqldbProfile.api._
    import spray.json._
    
    
//    implicit val DoctorStatisticsFormat = jsonFormat8(DoctorStatistics.apply)
    
    implicit val IntegerDistributionMapping = MappedColumnType.base[IntegerDistribution, String](
        distribution => distribution.m_distribution.toList.toJson.toString,
        mapString => new IntegerDistribution(mapString.parseJson.convertTo[List[(Int, Double)]].toMap)
    )
    
    implicit val DaysMapping = MappedColumnType.base[Days, Int](
        day => day.getDays,
        int => Days.days(int))
     
    implicit val LocalTimeMapping = MappedColumnType.base[LocalTime, Int](
        time => 60 * time.getHourOfDay + time.getMinuteOfHour,
        minutes => new LocalTime(minutes / 60, minutes % 60))
    
    implicit val priorityMapping = MappedColumnType.base[Priority, String](
        _.toString,
        Priority.withName
        )
}
