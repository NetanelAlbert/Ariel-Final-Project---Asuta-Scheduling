
import model.DTOs.{DoctorStatistics, DoctorStatisticsAutoAvg}
import spray.json.DefaultJsonProtocol
import spray.json._
import DefaultJsonProtocol._


object Main {
    def main(args : Array[String])
    {
        implicit val doctorFormat = jsonFormat8(DoctorStatistics.apply)
        
    }
}
