
import model.database.DoctorStatisticsTable
import model.utils.{AnalyzeData, FileOps}

import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import scala.util.Try
import slick.jdbc.HsqldbProfile.api._
import slick.util.AsyncExecutor


object SandBox
{
    def main(args : Array[String]) : Unit =
    {
        val start = System.currentTimeMillis()
        
        val surgeries = AnalyzeData.getAllSurgeryInfo("SurgeriesData.xlsx")
        //val doctors = AnalyzeData.getDoctorStatisticsFromSurgeryInfo(surgeries)
    
        val surgeriesStatistic = AnalyzeData.getSurgeryStatisticsFromSurgeryInfo(surgeries)
        val end = System.currentTimeMillis()
        surgeriesStatistic.foreach(println)
    
        val diff = end - start
        val second = 1000
        val minute = 60*second
        println(s"\nRun in ${diff/minute} minutes, ${(diff % minute)/second} seconds and ${diff % second} millis (or $diff millis)")
    }
    
    def vla
    {
        val db = Database.forURL("jdbc:hsqldb:file:database/model.db", "SA", "", executor = AsyncExecutor.default("DbExecutor", 3))
        val table = new DoctorStatisticsTable(db)
    }

}
