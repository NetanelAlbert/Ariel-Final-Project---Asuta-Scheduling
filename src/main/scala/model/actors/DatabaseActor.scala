package model.actors

import akka.Done
import akka.actor.{ActorRef, Props}
import model.DTOs.{DoctorAvailability, DoctorStatistics, SurgeryAvgInfo, SurgeryAvgInfoByDoctor, SurgeryStatistics}
import model.database._
import work.{GetDoctorsStatisticsWork, GetOptionsForFreeBlockWork, ReadDoctorsMappingExcelWork, ReadPastSurgeriesExcelWork, ReadSurgeryMappingExcelWork, WorkFailure, WorkSuccess}

import scala.concurrent.ExecutionContext
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
    
    override def preStart()
    {
        super.preStart()
    
        surgeryStatisticsTable.create()
        surgeryAvgInfoTable.create()
        surgeryAvgInfoByDoctorTable.create()
        doctorStatisticsTable.create()
        doctorAvailabilityTable.create()
    }
    
    override def receive =
    {
        case work @ ReadPastSurgeriesExcelWork(_, _, Some(surgeryStatistics), Some(surgeryAvgInfo), Some(surgeryAvgInfoByDoctor), Some(doctorStatistics), Some(doctorAvailabilities)) =>
        {
            readPastSurgeriesExcelWork(work, surgeryStatistics, surgeryAvgInfo, surgeryAvgInfoByDoctor, doctorStatistics, doctorAvailabilities)
        }

        case work : ReadSurgeryMappingExcelWork => readSurgeryMappingExcelWork(work, work.surgeryMapping)

        case work : ReadDoctorsMappingExcelWork => readDoctorMappingExcelWork(work, work.doctorMapping)

        case work : GetDoctorsStatisticsWork => getDoctorsStatisticsWork(work)
        
        case work : GetOptionsForFreeBlockWork => getOptionsForFreeBlockWork(work, work.dayOfWeek)
    }
    
    def getOptionsForFreeBlockWork(work : GetOptionsForFreeBlockWork, dayOfWeek : Int) = ??? // TODO Implement
    
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
    
    def readSurgeryMappingExcelWork(work : ReadSurgeryMappingExcelWork, surgeryMapping : Option[Map[Double, Option[String]]])
    {
        surgeryMapping match {
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
    
    def readDoctorMappingExcelWork(work : ReadDoctorsMappingExcelWork, doctorMapping : Option[Map[Int, Option[String]]])
    {
        doctorMapping match {
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
    
    def readPastSurgeriesExcelWork(work : ReadPastSurgeriesExcelWork, surgeryStatistics : Iterable[SurgeryStatistics], surgeryAvgInfo : Iterable[SurgeryAvgInfo], surgeryAvgInfoByDoctor : Iterable[SurgeryAvgInfoByDoctor], doctorStatistics : Iterable[DoctorStatistics], doctorAvailabilities : Set[DoctorAvailability])
    {
        val mappings = for
        {
            surgeryMapping <- surgeryStatisticsTable.getSurgeryMapping()
            doctorMapping <- doctorStatisticsTable.getDoctorMapping()
        } yield (surgeryMapping, doctorMapping)
        
        val cleaning = mappings.flatMap(_ =>
        {
            val surgeryStatistics = surgeryStatisticsTable.clear()
            val surgeryAvgInfo = surgeryAvgInfoTable.clear()
            val surgeryAvgInfoByDoctor = surgeryAvgInfoByDoctorTable.clear()
            val doctorStatistics = doctorStatisticsTable.clear()
            val doctorAvailabilities = doctorAvailabilityTable.clear()
            
            for
            {
                _ <- surgeryStatistics
                _ <- surgeryAvgInfo
                _ <- surgeryAvgInfoByDoctor
                _ <- doctorStatistics
                _ <- doctorAvailabilities
            } yield Done.done()
        })
        
        val inserting = cleaning.flatMap(_ =>
        {
            val surgeryStatisticsFuture = surgeryStatisticsTable.insertAll(surgeryStatistics)
            val surgeryAvgInfoFuture = surgeryAvgInfoTable.insertAll(surgeryAvgInfo)
            val surgeryAvgInfoByDoctorFuture = surgeryAvgInfoByDoctorTable.insertAll(surgeryAvgInfoByDoctor)
            val doctorStatisticsFuture = doctorStatisticsTable.insertAll(doctorStatistics)
            val doctorAvailabilitiesFuture = doctorAvailabilityTable.insertAll(doctorAvailabilities)
    
            for
            {
                _ <- surgeryStatisticsFuture
                _ <- surgeryAvgInfoFuture
                _ <- surgeryAvgInfoByDoctorFuture
                _ <- doctorStatisticsFuture
                _ <- doctorAvailabilitiesFuture
            } yield Done.done()
        })
        
        val mappingBack = inserting.flatMap(_ =>
        {
            for
            {
                surgeryMapping <- mappings.map(_._1)
                doctorMapping <- mappings.map(_._2)
                _ <- surgeryStatisticsTable.setSurgeryNames(surgeryMapping)
                _ <- doctorStatisticsTable.setDoctorNames(doctorMapping)
            } yield Done
        })
        
        mappingBack.onComplete
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
