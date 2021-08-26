package model.actors

import akka.Done
import akka.actor.{ActorRef, Props}
import model.DTOs._
import model.database._
import org.joda.time.LocalDate
import work._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object DatabaseActor
{
    def props(m_controller : ActorRef, m_modelManager : ActorRef)(implicit ec : ExecutionContext) : Props = Props(new DatabaseActor(m_controller, m_modelManager))
}

class DatabaseActor(m_controller : ActorRef, m_modelManager : ActorRef)(implicit ec : ExecutionContext) extends MyActor
{
    val m_db = DBConnection.get()
    
    val surgeryStatisticsTable = new SurgeryStatisticsTable(m_db)
    val surgeryAvgInfoTable = new SurgeryAvgInfoTable(m_db)
    val surgeryAvgInfoByDoctorTable = new SurgeryAvgInfoByDoctorTable(m_db)
    val doctorStatisticsTable = new DoctorStatisticsTable(m_db)
    val doctorAvailabilityTable = new DoctorAvailabilityTable(m_db)
    val scheduleTable = new ScheduleTable(m_db)
    
    override def preStart()
    {
        super.preStart()
        
        val createTables = for
        {
            _ <- surgeryStatisticsTable.create()
            _ <- surgeryAvgInfoTable.create()
            _ <- surgeryAvgInfoByDoctorTable.create()
            _ <- doctorStatisticsTable.create()
            _ <- doctorAvailabilityTable.create()
            _ <- scheduleTable.create()
        } yield ()
    
        Await.result(createTables, 10 seconds)
    }
    
    override def receive =
    {
        case work @ ReadPastSurgeriesExcelWork(_, keepOldMapping, _, Some(surgeryStatistics), Some(surgeryAvgInfo), Some(surgeryAvgInfoByDoctor), Some(doctorStatistics), Some(doctorAvailabilities)) =>
        {
            readPastSurgeriesExcelWork(work, keepOldMapping, surgeryStatistics, surgeryAvgInfo, surgeryAvgInfoByDoctor, doctorStatistics, doctorAvailabilities)
        }
        
        case work : ReadSurgeryMappingExcelWork => readSurgeryMappingExcelWork(work, work.surgeryMapping)
        
        case work : ReadDoctorsMappingExcelWork => readDoctorMappingExcelWork(work, work.doctorMapping)
        
        case work : GetDoctorsStatisticsWork => getDoctorsStatisticsWork(work)
        
        case work : GetOptionsForFreeBlockWork => getOptionsForFreeBlockWork(work, work.date)
        
        case work @ GetCurrentScheduleWork(from : LocalDate, to : LocalDate, _, _) => getCurrentScheduleWork(work, from, to)
        
        case work @ ReadProfitExcelWork(_, Some(surgeriesProfit), Some(doctorsProfit)) => readProfitExcelWork(work, surgeriesProfit, doctorsProfit)

        case work @ ReadFutureSurgeriesExcelWork(_, keepOldMapping, Some(futureSurgeries)) => readFutureSurgeriesExcelWork(work, keepOldMapping, futureSurgeries)
    
    }
    
    def readFutureSurgeriesExcelWork(work : ReadFutureSurgeriesExcelWork, keepOldMapping : Boolean, futureSurgeries : Iterable[FutureSurgeryInfo])
    {
        val remove =
            if(keepOldMapping)
            {
                Future()
            }
            else
            {
                scheduleTable.clear()
            }
            
        remove.flatMap(_ => scheduleTable.insertAll(futureSurgeries)).onComplete
        {
            case Success(_) => m_controller ! WorkSuccess(work, Some("Schedule Read successfully"))
    
            case Failure(exception) =>
            {
                val info = s"Was not able to write the schedule to DB"
                m_controller ! WorkFailure(work, Some(exception), Some(info))
            }
        }
    }
    
    def readProfitExcelWork(work : ReadProfitExcelWork, surgeriesProfit : Iterable[(Double, Int)], doctorsProfit : Iterable[(Int, Int)])
    {
        val surgeriesFuture = surgeryStatisticsTable.setSurgeriesProfit(surgeriesProfit)
        val doctorsFuture = doctorStatisticsTable.setDoctorsProfit(doctorsProfit)
        val updates = for
        {
            _ <- surgeriesFuture
            _ <- doctorsFuture
        } yield Done.done()
        
        updates.onComplete
        {
            case Success(_) => m_controller ! WorkSuccess(work, Some("Profit Read successfully"))
            
            case Failure(exception) =>
            {
                val info = s"Was not able to set profit data in DB"
                m_controller ! WorkFailure(work, Some(exception), Some(info))
            }
        }
    }
    
    def getCurrentScheduleWork(work : GetCurrentScheduleWork, from : LocalDate, to : LocalDate)
    {
        val getData =for
        {
//            blocks <- scheduleTable.selectBlocksByDates(from, to)
//            schedule <- scheduleTable.selectByDates(from, to)
            schedule <- scheduleTable.selectAll()
            doctorMapping <- doctorStatisticsTable.getDoctorMapping(schedule.map(_.doctorId))
            blocks = schedule
                .map(
                {
                    surg => Block.fromFutureSurgery(surg, doctorMapping.get(surg.doctorId))
                })
                .groupBy(_.day)
                .mapValues(_.toSet)
        } yield (blocks, schedule)
        
        getData.onComplete
        {
            case Success((blocks, schedule)) =>
            {
                m_controller ! WorkSuccess(work.copy(schedule = Some(schedule),
                                                     blocks = Some(blocks)),
                                           Some(s"Successfully got schedule between $from anf $to"))
            }
    
            case Failure(exception) =>
            {
                val info = s"Was not able to get schedule from DB"
                m_controller ! WorkFailure(work, Some(exception), Some(info))
            }
        }
    }
    
    def getOptionsForFreeBlockWork(work : GetOptionsForFreeBlockWork, date : LocalDate)
    {
        val copyWork = for
        {
            availableDoctors <- doctorAvailabilityTable.getAvailableDoctorsIDs(date.getDayOfWeek)
            doctorsWithSurgeries <- surgeryAvgInfoByDoctorTable.getSurgeriesByDoctors(availableDoctors)
            
            doctorMapping <-doctorStatisticsTable.getDoctorMapping(doctorsWithSurgeries.keySet)
        
     
            allSurgeriesID = doctorsWithSurgeries.values.flatten.map(_.operationCode).toSet
            surgeryStatistics <- surgeryStatisticsTable.getByIDs(allSurgeriesID)
            surgeryAvgInfo <- surgeryAvgInfoTable.getByIDs(allSurgeriesID)
            
            //todo date days from setting, and shrink the default duration
            // (month is too much if we check every hour)
            plannedSurgeries <- scheduleTable.selectByDates(date.minusDays(14), date.plusDays(14)).map(_.filter(! _.released))
     
        } yield work.copy(doctorsWithSurgeries = Some(doctorsWithSurgeries),
                          doctorMapping = Some(doctorMapping),
                          surgeryStatistics = Some(surgeryStatistics),
                          surgeryAvgInfo = Some(surgeryAvgInfo),
                          plannedSurgeries = Some(plannedSurgeries))
        
        copyWork.onComplete
        {
            case Success(workCopy) => m_modelManager ! workCopy // Route to AnalyzeDataActor
            
            case Failure(exception) =>
            {
                val info = s"Was not able to GetOptionsForFreeBlockWork from DB"
                m_controller ! WorkFailure(work, Some(exception), Some(info))
            }
        }
        
    }
    
    def getDoctorsStatisticsWork(work : GetDoctorsStatisticsWork)
    {
        val doctorsBaseStatisticsFuture = doctorStatisticsTable.selectAll()
        val surgeryAvgInfoByDoctorMapFuture = surgeryAvgInfoByDoctorTable.selectAll()
        val surgeryAvgInfoListFuture = surgeryAvgInfoTable.selectAll()
        val operationCodeAndNamesFuture = surgeryStatisticsTable.getOperationCodeAndNames()
        
        val workCopyFuture = for
        {
            doctorsBaseStatistics <- doctorsBaseStatisticsFuture
            surgeryAvgInfoByDoctor <- surgeryAvgInfoByDoctorMapFuture
            surgeryAvgInfoList <- surgeryAvgInfoListFuture
            operationCodeAndNames <- operationCodeAndNamesFuture
        } yield work.copy(
            doctorsBaseStatistics = Some(doctorsBaseStatistics),
            surgeryAvgInfoByDoctorMap = Some(surgeryAvgInfoByDoctor.groupBy(_.doctorId)),
            surgeryAvgInfoList = Some(surgeryAvgInfoList),
            operationCodeAndNames = Some(operationCodeAndNames)
            )
        
        workCopyFuture.onComplete
        {
            case Success(workCopy) => m_controller ! WorkSuccess(workCopy, None)
            
            case Failure(exception) =>
            {
                val info = s"Was not able to get doctors statistics from DB"
                m_controller ! WorkFailure(work, Some(exception), Some(info))
            }
        }
    }
    
    def readSurgeryMappingExcelWork(work : ReadSurgeryMappingExcelWork, surgeryMapping : Option[Map[Double, String]])
    {
        surgeryMapping match
        {
            case Some(mapping) =>
            {
                surgeryStatisticsTable.setSurgeryNames(mapping).onComplete
                {
                    case Success(updated) =>
                    {
                        val info = s"Successfully mapped $updated surgeries in the database"
                        m_controller ! WorkSuccess(work, Some(info))
                    }
                    
                    case Failure(exception) =>
                    {
                        val info = s"Was not able to update surgeryStatisticsTable with the new names"
                        m_controller ! WorkFailure(work, Some(exception), Some(info))
                    }
                }
            }
            
            case None =>
            {
                val info = s"Got ReadSurgeryMappingExcelWork with an empty map. work: $work"
                m_controller ! WorkFailure(work, None, Some(info))
            }
        }
    }
    
    def readDoctorMappingExcelWork(work : ReadDoctorsMappingExcelWork, doctorMapping : Option[Map[Int, String]])
    {
        doctorMapping match
        {
            case Some(mapping) =>
            {
                doctorStatisticsTable.setDoctorNames(mapping).onComplete
                {
                    case Success(updated) =>
                    {
                        val info = s"Successfully mapped $updated doctors in the database"
                        m_controller ! WorkSuccess(work, Some(info))
                    }
                    
                    case Failure(exception) =>
                    {
                        val info = s"Was not able to update doctorStatisticsTable with the new names"
                        m_controller ! WorkFailure(work, Some(exception), Some(info))
                    }
                }
            }
            
            case None =>
            {
                val info = s"Got ReadDoctorsMappingExcelWork with an empty map. work: $work"
                m_controller ! WorkFailure(work, None, Some(info))
            }
        }
    }
    
    def readPastSurgeriesExcelWork(work : ReadPastSurgeriesExcelWork, keepOldMapping : Boolean, surgeryStatistics : Iterable[SurgeryStatistics], surgeryAvgInfo : Iterable[SurgeryAvgInfo], surgeryAvgInfoByDoctor : Iterable[SurgeryAvgInfoByDoctor], doctorStatistics : Iterable[DoctorStatistics], doctorAvailabilities : Set[DoctorAvailability])
    {
        val actionFuture = for
        {
            // Read old maps
            surgeryMapping <- if(keepOldMapping) surgeryStatisticsTable.getSurgeryMapping() else Future(Map[Double, String]())
            profitMapping <- if(keepOldMapping) surgeryStatisticsTable.getProfitMapping() else Future(Map[Double, Int]())
            doctorMapping <- if(keepOldMapping) doctorStatisticsTable.getDoctorMapping() else Future(Map[Int, String]())
            
            
            // Clean
            surgeryStatisticsCleaning = surgeryStatisticsTable.clear()
            surgeryAvgInfoCleaning = surgeryAvgInfoTable.clear()
            surgeryAvgInfoByDoctorCleaning = surgeryAvgInfoByDoctorTable.clear()
            doctorStatisticsCleaning = doctorStatisticsTable.clear()
            doctorAvailabilitiesCleaning = doctorAvailabilityTable.clear()
            
            _ <- surgeryStatisticsCleaning
            _ <- surgeryAvgInfoCleaning
            _ <- surgeryAvgInfoByDoctorCleaning
            _ <- doctorStatisticsCleaning
            _ <- doctorAvailabilitiesCleaning
            
            // Insert new data
            surgeryStatisticsInsert = surgeryStatisticsTable.insertAll(surgeryStatistics)
            surgeryAvgInfoInsert = surgeryAvgInfoTable.insertAll(surgeryAvgInfo)
            surgeryAvgInfoByDoctorInsert = surgeryAvgInfoByDoctorTable.insertAll(surgeryAvgInfoByDoctor)
            doctorStatisticsInsert = doctorStatisticsTable.insertAll(doctorStatistics)
            doctorAvailabilitiesInsert = doctorAvailabilityTable.insertAll(doctorAvailabilities)
            
            _ <- surgeryStatisticsInsert
            _ <- surgeryAvgInfoInsert
            _ <- surgeryAvgInfoByDoctorInsert
            _ <- doctorStatisticsInsert
            _ <- doctorAvailabilitiesInsert
            
            // Map back
            _ <- surgeryStatisticsTable.setSurgeryNames(surgeryMapping)
            _ <- surgeryStatisticsTable.setSurgeriesProfit(profitMapping)
            _ <- doctorStatisticsTable.setDoctorNames(doctorMapping)
        } yield ()
        
        
        actionFuture.onComplete
        {
            case Success(_) =>
            {
                m_controller ! WorkSuccess(work, Some(s"Successfully load surgery data from ${work.file.getPath}"))
            }
            
            case Failure(exception) =>
            {
                val info = "Failed to treat ReadPastSurgeriesExcelWork"
                m_controller ! WorkFailure(work, Some(exception), Some(info))
            }
        }
    }
}
