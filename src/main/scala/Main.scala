
import model.DTOs.{SurgeonStatistics, SurgeonStatisticsAutoAvg}
import spray.json.DefaultJsonProtocol
import spray.json._
import DefaultJsonProtocol._


object Main {
    def main(args : Array[String])
    {
        implicit val surgeonFormat = jsonFormat8(SurgeonStatistics.apply)
    
        val surgeonInfo = SurgeonStatisticsAutoAvg(12, "Dani", 1, 2, 3, 4, 5)
    
        val surgeonInfoMap = List(SurgeonStatisticsAutoAvg(12, "Dani", 1, 2, 3, 4, 5),
                                  SurgeonStatisticsAutoAvg(12, "Dani", 1, 2, 3, 4, 5),
                                  SurgeonStatisticsAutoAvg(13, "Nati", 1, 2, 3, 4, 5),
                                  SurgeonStatisticsAutoAvg(13, "Nati", 1, 2, 3, 4, 5)).groupBy(_.id.toString)
        
        println(surgeonInfoMap.toJson.prettyPrint)
    
//        implicit class MapFunctions[B](val map: Map[String, B]) extends AnyVal {
//            def keysToInt : Map[Int, B] = map.map({ case (a, b) => (a.toInt, b) })
//        }
        
        val newSurMap = surgeonInfoMap.toJson.convertTo[Map[String, List[SurgeonStatistics]]].map({ case (a, b) => (a.toInt, b) })
        println(newSurMap)
    
        val surJson = surgeonInfo.toJson
    
        val newSur = surJson.convertTo[SurgeonStatistics]
        println(surJson.prettyPrint)
        println(newSur)
    }
}
