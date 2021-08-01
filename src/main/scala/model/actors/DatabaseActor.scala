package model.actors

import akka.Done
import akka.actor.ActorRef
import model.DTOs.{DoctorStatistics, SurgeryAvgInfo, SurgeryAvgInfoByDoctor, SurgeryStatistics}
import model.database._
import work.{ReadPastSurgeriesExcelWork, WorkFailure, WorkSuccess}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class DatabaseActor(m_controller : ActorRef, m_modelManager : ActorRef)(implicit ec : ExecutionContext) extends MyActor
{
    val m_db = DBConnection.get()
    
    val surgeryStatisticsTable = new SurgeryStatisticsTable(m_db)
    val surgeryAvgInfoTable = new SurgeryAvgInfoTable(m_db)
    val surgeryAvgInfoByDoctorTable = new SurgeryAvgInfoByDoctorTable(m_db)
    val doctorStatisticsTable = new DoctorStatisticsTable(m_db)
    
    
    override def receive =
    {
        case work @ ReadPastSurgeriesExcelWork(_, _, Some(surgeryStatistics), Some(surgeryAvgInfo), Some(surgeryAvgInfoByDoctor), Some(doctorStatistics)) => readPastSurgeriesExcelWork(work, surgeryStatistics, surgeryAvgInfo, surgeryAvgInfoByDoctor, doctorStatistics)
        
        case _ =>
    }
    
    def readPastSurgeriesExcelWork(work : ReadPastSurgeriesExcelWork, surgeryStatistics : Iterable[SurgeryStatistics], surgeryAvgInfo : Iterable[SurgeryAvgInfo], surgeryAvgInfoByDoctor : Iterable[SurgeryAvgInfoByDoctor], doctorStatistics : Iterable[DoctorStatistics])
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
            
            for
            {
                _ <- surgeryStatistics
                _ <- surgeryAvgInfo
                _ <- surgeryAvgInfoByDoctor
                _ <- doctorStatistics
            } yield Done.done()
        })
        
        val inserting = cleaning.flatMap(_ =>
        {
            val surgeryStatisticsFuture = surgeryStatisticsTable.insertAll(surgeryStatistics)
            val surgeryAvgInfoFuture = surgeryAvgInfoTable.insertAll(surgeryAvgInfo)
            val surgeryAvgInfoByDoctorFuture = surgeryAvgInfoByDoctorTable.insertAll(surgeryAvgInfoByDoctor)
            val doctorStatisticsFuture = doctorStatisticsTable.insertAll(doctorStatistics)
    
            for
            {
                _ <- surgeryStatisticsFuture
                _ <- surgeryAvgInfoFuture
                _ <- surgeryAvgInfoByDoctorFuture
                _ <- doctorStatisticsFuture
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
                m_controller ! WorkSuccess(work)
            }
    
            case Failure(exception) =>
            {
                val info = "Failed to treat ReadPastSurgeriesExcelWork"
                m_controller ! WorkFailure(work, Some(exception), Some(info))
            }
        }
    }
}
