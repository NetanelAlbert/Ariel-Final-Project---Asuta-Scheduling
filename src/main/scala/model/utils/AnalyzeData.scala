package model.utils


import org.apache.poi.ss.usermodel.{DataFormatter, Row}
import model.DTOs.{DoctorStatistics, DoctorStatisticsAutoAvg, PastSurgeryInfo, SurgeryStatistics}
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution

import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`
import scala.util.Try

object AnalyzeData
{
    def getDoctorStatisticsFromSurgeryInfo(surgeryList : List[PastSurgeryInfo]) : List[DoctorStatistics] =
    {
        surgeryList.groupBy(_.doctorId).map
        {
            case (id, list) =>
            {
    
                val name = None // todo get names
                val amountOfData = list.length
                val profitAvg = 1.11 // todo get profit
                
                val (surgery, resting, hospitalization) = list.aggregate((0, 0 ,0))(addSurgeryInfoToTuple, sumTwoTuples)
                
                val surgeryDurationAvgMinutes = surgery.toDouble/amountOfData
                val restingDurationAvgMinutes = resting.toDouble/amountOfData
                val hospitalizationDurationAvgHours = hospitalization.toDouble/amountOfData
    
                DoctorStatisticsAutoAvg(
                    id,
                    name,
                    amountOfData,
                    profitAvg,
                    surgeryDurationAvgMinutes,
                    restingDurationAvgMinutes,
                    hospitalizationDurationAvgHours
                )
            }
        }.toList
    }
    
    def getSurgeryStatisticsFromSurgeryInfo(surgeryList : List[PastSurgeryInfo]) : List[SurgeryStatistics] =
    {
        surgeryList.groupBy(_.operationCode).map
        {
            case (operationCode, list) =>
            {
                val restingDistribution = new EnumeratedIntegerDistribution(list.map(_.restingMinutes).toArray)
                val hospitalizationDistribution = new EnumeratedIntegerDistribution(list.map(_.hospitalizationHours).toArray)
                val profit = 1.234 // Todo get real profit
    
                SurgeryStatistics(operationCode, restingDistribution, hospitalizationDistribution, profit)
            }
        }.toList
    }
    
    def getSurgeryStatisticsByDoctorFromSurgeryInfo(surgeryList : List[PastSurgeryInfo]) : Map[Int, List[SurgeryStatistics]] =
    {
        surgeryList.groupBy(_.doctorId).mapValues(getSurgeryStatisticsFromSurgeryInfo)
    }
    
    def addSurgeryInfoToTuple(tuple : (Int, Int, Int), surgeryInfo : PastSurgeryInfo) : (Int, Int, Int) = surgeryInfo match
    {
        case PastSurgeryInfo(_, _, surgeryDurationMinutes, restingMinutes, hospitalizationHours, _, _) =>
        {
            sumTwoTuples(tuple, (surgeryDurationMinutes, restingMinutes, hospitalizationHours))
        }
    }
    
    def sumTwoTuples(tuple1 : (Int, Int, Int), tuple2 : (Int, Int, Int)) : (Int, Int, Int) = (tuple1, tuple2) match
    {
        case((a1, b1, c1), (a2, b2, c2)) => (a1 + a2, b1 + b2, c1 + c2)
    }
    
    
    
    def getAllSurgeryInfo(path : String) : List[PastSurgeryInfo] =
    {
        import SurgeryDataExcel._
        //todo extract info about who can work in each hour
        implicit val formatter = new DataFormatter()
        FileOps.getSheet(path).flatMap(row =>
        {
           Try
           {
               val operationCode = formatter.formatCellValue(row.getCell(operationCodeIndex)).toDouble
               val doctorId = formatter.formatCellValue(row.getCell(doctorIdIndex)).toInt
               val surgeryDurationMinutes = dateDiff(row, surgeryStartIndex, surgeryEndIndex, TimeUnit.MINUTES)
               val restingMinutes = dateDiff(row, restingStartIndex, restingEndIndex, TimeUnit.MINUTES)
               //TODO :: get hospitalization info from new file (Roy)
               val hospitalizationHours = dateDiff(row, restingStartIndex, restingEndIndex, TimeUnit.MINUTES)
        
               val blockStartString = formatter.formatCellValue(row.getCell(blockStartIndex))
               val blockStart = new Timestamp(sdFormat.parse(blockStartString).getTime)
               val blockEndString = formatter.formatCellValue(row.getCell(blockEndIndex))
               val blockEnd = new Timestamp(sdFormat.parse(blockEndString).getTime)
    
               PastSurgeryInfo(operationCode,
                           doctorId,
                           surgeryDurationMinutes,
                           restingMinutes,
                           hospitalizationHours,
                           blockStart,
                           blockEnd)
           }.toOption
        }).toList
    }
    
    val sdFormat = new java.text.SimpleDateFormat("MM/dd/yyyy hh:mm")
    def dateDiff(row : Row, startIndex : Int, endIndex : Int, unit : TimeUnit)(implicit formatter : DataFormatter) : Int =
    {
        val start = formatter.formatCellValue(row.getCell(startIndex))
        val end = formatter.formatCellValue(row.getCell(endIndex))
        val diff = sdFormat.parse(end).getTime - sdFormat.parse(start).getTime
        unit.convert(diff, TimeUnit.MILLISECONDS).toInt
    }
    
    
    object SurgeryDataExcel
    {
        val operationCodeIndex = 2
        val doctorIdIndex = 1
        val surgeryStartIndex = 20
        val surgeryEndIndex = 23
        val restingStartIndex = 25
        val restingEndIndex = 26
//        val hospitalizationStartIndex =
//        val hospitalizationEndIndex =
        val blockStartIndex = 27
        val blockEndIndex = 28
    }
}
