package model.actors

import akka.actor.{ActorRef, Props}
import model.DTOs.{FutureSurgeryInfo, PastSurgeryInfo}
import org.apache.poi.ss.usermodel.{DataFormatter, Row, Sheet, WorkbookFactory}
import org.joda.time.{Days, Duration, Hours, LocalDate, LocalDateTime, Minutes}
import org.joda.time.format.DateTimeFormat
import work._

import java.io.File
import java.util.concurrent.TimeUnit
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import common.Utils._

object FileActor
{
    def props(m_controller : ActorRef,
              m_modelManager : ActorRef,
              m_databaseActor : ActorRef,
              m_AnalyzeDataActor : ActorRef,
              )(implicit ec : ExecutionContext) : Props =
        Props(new FileActor(m_controller,
                            m_modelManager,
                            m_databaseActor,
                            m_AnalyzeDataActor,
                            ))
}

class FileActor(m_controller : ActorRef,
                m_modelManager : ActorRef,
                m_databaseActor : ActorRef,
                m_AnalyzeDataActor : ActorRef,
                )(implicit override val ec : ExecutionContext) extends MyActor with SettingsAccess
{
    val formatter = new DataFormatter()
    
    override def receive =
    {
        case work : ReadPastSurgeriesExcelWork => readPastSurgeriesExcelWork(work, work.file)
        
        case work : ReadSurgeryMappingExcelWork => readSurgeryMappingExcelWork(work, work.file)
        
        case work : ReadDoctorsMappingExcelWork => readDoctorMappingExcelWork(work, work.file)
        
        case work : ReadProfitExcelWork => readProfitExcelWork(work, work.file)
        
        case work : ReadFutureSurgeriesExcelWork => readFutureSurgeriesExcelWork(work, work.file)
    }
    
    def readFutureSurgeriesExcelWork(work : ReadFutureSurgeriesExcelWork, file : File)
    {
        Future
        {
            getSheet(file).flatMap(getFutureSurgeryFromRow)
        }.onComplete
        {
            case Success(futureSurgeries) if futureSurgeries.nonEmpty =>
            {
                m_databaseActor ! work.copy(futureSurgeries = Some(futureSurgeries))
            }
            
            case Success(_) =>
            {
                import FutureSurgeryDataExcelColumns._
                val info =
                    s"""Can't read schedule.
                       |File is empty or wrong format:
                       |'${file.getPath}'
                       |Dates must be in format "dd/MM/yyyy hh:mm"
                       |Columns indexes are:
                       |operationCodeIndex - $operationCodeIndex
                       |doctorIdIndex - $doctorIdIndex
                       |plannedStartIndex - $plannedStartIndex
                       |operationRoomIndex - $operationRoomIndex
                       |blockStartIndex - $blockStartIndex
                       |blockEndIndex - $blockEndIndex
                       |releasedIndex - $releasedIndex
                       |""".stripMargin
                m_controller ! WorkFailure(work, None, Some(info))
            }
            
            case Failure(exception) =>
            {
                val info = s"Can't read old surgeries from '${file.getPath}''"
                m_controller ! WorkFailure(work, Some(exception), Some(info))
            }
        }
    }
    
    def readProfitExcelWork(work : ReadProfitExcelWork, file : File)
    {
        Future
        {
            val doctorsProfit = getSheet(file, 0).flatMap(getTuple2FromRow(_.toInt, _.toInt))
            val surgeriesProfit = getSheet(file, 1).flatMap(getTuple2FromRow(_.toDouble, _.toInt))
            (doctorsProfit, surgeriesProfit)
        } onComplete
        {
            case Success((doctorsProfit, surgeriesProfit)) if doctorsProfit.nonEmpty && surgeriesProfit.nonEmpty =>
            {
                m_databaseActor ! work.copy(doctorsProfit = Some(doctorsProfit),
                                            surgeriesProfit = Some(surgeriesProfit))
            }
            
            case Success(_) =>
            {
                val info =
                    """Empty or wrong format given profit loading.
                      |The format is:
                      |First sheet - doctors profit - Integer in first and second column.
                      |Second sheet - surgeries profit - Float number in first column and Integer in the second.
                      |""".stripMargin
                m_controller ! WorkFailure(work, None, Some(info))
            }
            
            case Failure(exception) =>
            {
                val info = s"Can't read profit data from '${file.getPath}''"
                m_controller ! WorkFailure(work, Some(exception), Some(info))
            }
        }
        
    }
    
    def readSurgeryMappingExcelWork(work : ReadSurgeryMappingExcelWork, file : File)
    {
        val surgeryMapping = getSheet(file).map(getDoubleStringFromRow).flatten.toMap.filter(_._2.nonEmpty)
        
        if (surgeryMapping.nonEmpty)
        {
            m_databaseActor ! work.copy(surgeryMapping = Some(surgeryMapping))
        } else
        {
            val info = "Surgery mapping file must have ID (real number) in the first column, and name on the second"
            m_controller ! WorkFailure(work, None, Some(info))
        }
    }
    
    def getDoubleStringFromRow(row : Row) : Option[(Double, String)] =
    {
        getTuple2FromRow(_.toDouble, identity)(row)
    }
    
    def getTuple2FromRow[A, B](AConverter : String => A, BConverter : String => B)(row : Row) : Option[(A, B)] =
    {
        Try
        {
            val a = formatter.formatCellValue(row.getCell(0))
            val b = formatter.formatCellValue(row.getCell(1))
            (AConverter(a), BConverter(b))
        }.toOption
    }
    
    def readDoctorMappingExcelWork(work : ReadDoctorsMappingExcelWork, file : File)
    {
        val doctorMapping = getSheet(file).map(getIntStringFromRow).flatten.toMap.filter(_._2.nonEmpty)
        
        if (doctorMapping.nonEmpty)
        {
            m_databaseActor ! work.copy(doctorMapping = Some(doctorMapping))
        } else
        {
            val info = "Doctor mapping file must have ID (natural number) in the first column, and name on the second"
            m_controller ! WorkFailure(work, None, Some(info))
        }
        
    }
    
    def getIntStringFromRow(row : Row) : Option[(Int, String)] =
    {
        getTuple2FromRow(_.toInt, identity)(row)
    }
    
    def readPastSurgeriesExcelWork(readPastSurgeriesExcelWork : ReadPastSurgeriesExcelWork, file : File)
    {
        Future
        {
    
            getSheet(file).par.flatMap(getPastSurgeryFromRow)
        }.onComplete
        {
            case Success(pastSurgeryInfoIterable) if pastSurgeryInfoIterable.nonEmpty =>
            {
                m_AnalyzeDataActor ! readPastSurgeriesExcelWork.copy(pasteSurgeries = Some(pastSurgeryInfoIterable.toList))
            }
            
            case Success(_) =>
            {
                import PastSurgeryDataExcelColumns._
                val info =
                    s"""Can't read old surgeries.
                       |File is empty or wrong format:
                       |'${file.getPath}'
                       |Dates must be in format "dd/MM/yyyy hh:mm"
                       |Columns indexes are:
                       |doctor id: $operationCodeIndex
                       |operation code: $doctorIdIndex
                       |surgery start: $surgeryStartIndex
                       |surgery end: $surgeryEndIndex
                       |resting start: $restingStartIndex
                       |resting end: $restingEndIndex
                       |block start: $blockStartIndex
                       |block end: $blockEndIndex
                       |release date: $releaseDateIndex""".stripMargin
                m_controller ! WorkFailure(readPastSurgeriesExcelWork, None, Some(info))
            }
            
            case Failure(exception) =>
            {
                val info = s"Can't read old surgeries from '${file.getPath}''"
                m_controller ! WorkFailure(readPastSurgeriesExcelWork, Some(exception), Some(info))
            }
        }
    }
    
    def getPastSurgeryFromRow(row : Row) : Option[PastSurgeryInfo] =
    {
        import PastSurgeryDataExcelColumns._
        Try
        {
            val operationCode = formatter.formatCellValue(row.getCell(operationCodeIndex)).toDouble
            val doctorId = formatter.formatCellValue(row.getCell(doctorIdIndex)).toInt
            val surgeryDurationMinutes = minutesDiff(row, surgeryStartIndex, surgeryEndIndex, "surgeryDurationMinutes")
            val restingMinutes = minutesDiff(row, restingStartIndex, restingEndIndex, "restingMinutes")
            val hospitalizationHours = hoursDiff(row, restingStartIndex, releaseDateIndex, "hospitalizationHours")
            
            val blockStartString = formatter.formatCellValue(row.getCell(blockStartIndex))
            val blockStart = dateTimeFormat.parseLocalDateTime(blockStartString)
            val blockEndString = formatter.formatCellValue(row.getCell(blockEndIndex))
            val blockEnd = dateTimeFormat.parseLocalDateTime(blockEndString)
            
//            println(
//                s"""NA:: getPastSurgeryFromRow()
//                   |operationCode = $operationCode
//                   |doctorId = $doctorId
//                   |surgeryDurationMinutes = $surgeryDurationMinutes
//                   |restingMinutes = $restingMinutes
//                   |hospitalizationHours = $hospitalizationHours
//                   |
//                   |blockStartString = $blockStartString
//                   |blockStart = $blockStart
//                   |blockEndString = $blockEndString
//                   |blockEnd = $blockEnd
//                   |""".stripMargin)
            
            PastSurgeryInfo(operationCode,
                            doctorId,
                            surgeryDurationMinutes,
                            restingMinutes,
                            hospitalizationHours,
                            blockStart,
                            blockEnd)
        }.toOption
    }
    
    
    def getFutureSurgeryFromRow(row : Row) : Option[FutureSurgeryInfo] =
    {
        import FutureSurgeryDataExcelColumns._
        val trying = Try
        {
            val operationCode = formatter.formatCellValue(row.getCell(operationCodeIndex)).toDouble
            val doctorId = formatter.formatCellValue(row.getCell(doctorIdIndex)).toInt
            val plannedStart = dateTimeFormat.parseLocalDateTime(formatter.formatCellValue(row.getCell(plannedStartIndex)))
            val operationRoom = formatter.formatCellValue(row.getCell(operationRoomIndex)).toInt
            val released = Try
            {
                dateTimeFormat.parseLocalDateTime(formatter.formatCellValue(row.getCell(releasedIndex)))
            }.isSuccess
            val blockStart = dateTimeFormat.parseLocalTime(formatter.formatCellValue(row.getCell(blockStartIndex)))
            val blockEnd = dateTimeFormat.parseLocalTime(formatter.formatCellValue(row.getCell(blockEndIndex)))
            
            FutureSurgeryInfo(operationCode,
                              doctorId,
                              plannedStart,
                              operationRoom - roomsStart,
                              blockStart,
                              blockEnd,
                              released,
                              )
        }
        trying.failed.foreach(e => println(s"Error while pars row: \n${row.cellIterator().map(formatter.formatCellValue).mkString(", ")} \nError: \n${e.getMessage}"))
        trying.foreach(e => println(s"Succeed pars row: \n${row.cellIterator().map(formatter.formatCellValue).mkString(", ")} into \n$e"))
        trying.toOption
    }
    
    def minutesDiff = dateDiff((start, end) => Minutes.minutesBetween(start, end).getMinutes)(_, _, _, _)
    def hoursDiff = dateDiff((start, end) => Hours.hoursBetween(start, end).getHours)(_, _, _, _)
    
    
    def dateDiff(diff : (LocalDateTime, LocalDateTime) => Int)(row : Row, startIndex : Int, endIndex : Int, description : String) : Int =
    {
        val startString = formatter.formatCellValue(row.getCell(startIndex))
        val endString = formatter.formatCellValue(row.getCell(endIndex))
    
        val start = dateTimeFormat.parseLocalDateTime(startString)
        val end = dateTimeFormat.parseLocalDateTime(endString)
        
//        println(s"NA:: dateDiff() for $description: start ($startIndex): $startString, end ($endIndex): $endString")
        diff(start, end)
    }
    
    
    object PastSurgeryDataExcelColumns
    {
        val doctorIdIndex = 2
        val operationCodeIndex = 3
        val surgeryStartIndex = 20
        val surgeryEndIndex = 23
        val restingStartIndex = 25
        val restingEndIndex = 26
        val blockStartIndex = 27
        val blockEndIndex = 28
        val releaseDateIndex = 29
    }
    
    object FutureSurgeryDataExcelColumns
    {
        val doctorIdIndex = 2
        val operationCodeIndex = 3
        val plannedStartIndex = 10
        val operationRoomIndex = 13
        val blockStartIndex = 27
        val blockEndIndex = 28
        val releasedIndex = 29
    }
    
    //TODO:: handle fail? maybe validate file on UI?
    def getSheet(file : File, index : Int = 0) : Iterable[Row] =
    {
        
        //TODO:: use less memory (option - https://stackoverflow.com/a/28397328/11845387)
        val workbook = WorkbookFactory.create(file)
        val sheet = workbook.getSheetAt(index)
        workbook.close()
        sheet
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
