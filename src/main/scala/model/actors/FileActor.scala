package model.actors

import org.apache.poi.ss.usermodel.{DataFormatter, Row, Sheet, WorkbookFactory}
import spray.json.DefaultJsonProtocol
import spray.json._
import DefaultJsonProtocol._
import akka.actor.{Actor, ActorRef, Props}

import java.io.File
import java.io.PrintWriter
import collection.JavaConversions._
import model.DTOs.FormattingProtocols._
import model.DTOs.{DoctorStatistics, PastSurgeryInfo}
import org.joda.time.LocalDate
import work.{ReadDoctorsMappingExcelWork, ReadPastSurgeriesExcelWork, ReadSurgeryMappingExcelWork, WorkFailure}

import java.sql.{Date, Timestamp}
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object FileActor
{
    def props(m_controller : ActorRef, m_modelManager : ActorRef, m_databaseActor : ActorRef, m_AnalyzeDataActor : ActorRef)(implicit ec : ExecutionContext) : Props = Props(new FileActor(m_controller, m_modelManager, m_databaseActor, m_AnalyzeDataActor))
}

class FileActor(m_controller : ActorRef,
                m_modelManager : ActorRef,
                m_databaseActor : ActorRef,
                m_AnalyzeDataActor : ActorRef)(implicit ec : ExecutionContext) extends MyActor
{
    
    override def receive =
    {
        case work : ReadPastSurgeriesExcelWork => readPastSurgeriesExcelWork(work, work.filePath)
        
        case work : ReadSurgeryMappingExcelWork => readSurgeryMappingExcelWork(work, work.filePath)
        
        case work : ReadDoctorsMappingExcelWork => readDoctorMappingExcelWork(work, work.filePath)
    }
    
    def readSurgeryMappingExcelWork(work : ReadSurgeryMappingExcelWork, filePath : String)
    {
        implicit val formatter = new DataFormatter()
        val surgeryMapping = getSheet(filePath).flatMap(getDoubleStringFromRow(_)).toMap
        
        if(surgeryMapping.nonEmpty)
        {
            m_databaseActor ! work.copy(surgeryMapping = Some(surgeryMapping))
        }else
        {
            val info = "Surgery mapping file must have ID (real number) in the first column, and name on the second"
            m_controller ! WorkFailure(work, None, Some(info))
        }
    }
    
    def getDoubleStringFromRow(row : Row)(implicit formatter: DataFormatter) : Option[(Double, Option[String])] =
    {
        Try
        {
            val operationCode = formatter.formatCellValue(row.getCell(0)).toDouble
            val name = formatter.formatCellValue(row.getCell(1))
            (operationCode, Some(name))
        }.toOption
    }
    
    def readDoctorMappingExcelWork(work : ReadDoctorsMappingExcelWork, filePath : String)
    {
        implicit val formatter = new DataFormatter()
        val doctorMapping = getSheet(filePath).flatMap(getIntStringFromRow(_)).toMap
        
        if(doctorMapping.nonEmpty)
        {
            m_databaseActor ! work.copy(doctorMapping = Some(doctorMapping))
        } else
        {
            val info = "Doctor mapping file must have ID (natural number) in the first column, and name on the second"
            m_controller ! WorkFailure(work, None, Some(info))
        }
        
    }
    
    def getIntStringFromRow(row : Row)(implicit formatter: DataFormatter) : Option[(Int, Option[String])] =
    {
        Try
        {
            val operationCode = formatter.formatCellValue(row.getCell(0)).toInt
            val name = formatter.formatCellValue(row.getCell(1))
            (operationCode, Some(name))
        }.toOption
    }
    
    def readPastSurgeriesExcelWork(readPastSurgeriesExcelWork : ReadPastSurgeriesExcelWork, filePath : String)
    {
        getAllPastSurgeryFromExcel(filePath).onComplete
        {
            case Success(pastSurgeryInfoIterable) =>
            {
                m_AnalyzeDataActor ! readPastSurgeriesExcelWork.copy(pasteSurgeries = Some(pastSurgeryInfoIterable))
            }

            case Failure(exception) =>
            {
                val info = s"Can't read old surgeries from '$filePath''"
                m_controller ! WorkFailure(readPastSurgeriesExcelWork, Some(exception), Some(info))
            }
        }
    }
    
    
    
    def getAllPastSurgeryFromExcel(path : String) : Future[Iterable[PastSurgeryInfo]] =
    {
        Future
        {
            //todo extract info about who can work in each hour
            implicit val formatter = new DataFormatter()
            getSheet(path).flatMap(getPastSurgeryFromRow(_))
        }
    }
    
    def getPastSurgeryFromRow(row : Row)(implicit formatter: DataFormatter) : Option[PastSurgeryInfo] =
    {
        import SurgeryDataExcel._
        Try
        {
            val operationCode = formatter.formatCellValue(row.getCell(operationCodeIndex)).toDouble
            val doctorId = formatter.formatCellValue(row.getCell(doctorIdIndex)).toInt
            val surgeryDurationMinutes = dateDiff(row, surgeryStartIndex, surgeryEndIndex, TimeUnit.MINUTES)
            val restingMinutes = dateDiff(row, restingStartIndex, restingEndIndex, TimeUnit.MINUTES)
            //TODO :: get hospitalization info from new file (Roy)
            val hospitalizationHours = dateDiff(row, restingStartIndex, restingEndIndex, TimeUnit.MINUTES)
        
            val blockStartString = formatter.formatCellValue(row.getCell(blockStartIndex))
            val blockStart = new LocalDate(sdFormat.parse(blockStartString).getTime)
            val blockEndString = formatter.formatCellValue(row.getCell(blockEndIndex))
            val blockEnd = new LocalDate(sdFormat.parse(blockEndString).getTime)
        
            PastSurgeryInfo(operationCode,
                            doctorId,
                            surgeryDurationMinutes,
                            restingMinutes,
                            hospitalizationHours,
                            blockStart,
                            blockEnd)
        }.toOption
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
    
    //TODO :: handle fail? maybe validate file on UI?
    def getSheet(path : String, index : Int = 0) : Sheet =
    {
        val file = new File(path)
        val workbook = WorkbookFactory.create(file)
        workbook.getSheetAt(index)
    }
    
    
    //json files operations
    
//    val SURGEON_STATISTICS_JSON_PATH = "SURGEON_STATISTICS"
//    //TODO :: handle fail?
//    def saveJsonToFile(jsValue : JsValue, path : String)
//    {
//        new PrintWriter(path)
//        {
//            try
//            {
//                write(jsValue.toString)
//            } finally close()
//        }
//    }
//
//    def saveDoctorInfo(doctorInfoList : List[DoctorStatistics])
//    {
//        saveJsonToFile(doctorInfoList.toJson, SURGEON_STATISTICS_JSON_PATH)
//    }
//
//    def getDoctorInfo : List[DoctorStatistics] =
//    {
//        val source = scala.io.Source.fromFile("file.txt")
//
//        try
//        {
//            source.mkString.toJson.convertTo[List[DoctorStatistics]]
//        } finally source.close
//    }
//
//    def deleteFile(filePath : String) : Boolean =
//    {
//        new File(filePath).delete()
//    }
}
