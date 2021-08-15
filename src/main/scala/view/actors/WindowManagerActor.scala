package view.actors

import akka.actor.{ActorRef, Props}
import model.DTOs.{DoctorStatistics, OperationCodeAndName, Settings, SurgeryAvgInfo, SurgeryAvgInfoByDoctor}
import model.actors.MyActor
import view.mangerStatistics.MainWindowActions
import work.{GetDoctorsStatisticsWork, ReadDoctorsMappingExcelWork, ReadPastSurgeriesExcelWork, ReadProfitExcelWork, ReadSurgeryMappingExcelWork, TellAboutSettingsActorWork}

import java.io.File

object WindowManagerActor
{
    def props(m_controller : ActorRef, mainWindow : MainWindowActions) : Props = Props(new WindowManagerActor(m_controller, mainWindow))
}

class WindowManagerActor(m_controller : ActorRef, mainWindow : MainWindowActions) extends MyActor with UserActions
{
    private var m_settingsActor : Option[ActorRef] = None
    
    override def receive =
    {
        case GetDoctorsStatisticsWork(Some(doctorsBaseStatistics), Some(surgeryAvgInfoByDoctorMap), Some(surgeryAvgInfoList), Some(operationCodeAndNames)) => getDoctorsStatisticsWork(doctorsBaseStatistics, surgeryAvgInfoByDoctorMap, surgeryAvgInfoList, operationCodeAndNames)

        case TellAboutSettingsActorWork(settingsActor) => m_settingsActor = Some(settingsActor)
    }
    
    def getDoctorsStatisticsWork(doctorsBaseStatistics : Seq[DoctorStatistics], surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]], surgeryAvgInfoList : Seq[SurgeryAvgInfo], operationCodeAndNames : Seq[OperationCodeAndName])
    {
        mainWindow.initializeWithData(doctorsBaseStatistics : Seq[DoctorStatistics], surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]], surgeryAvgInfoList : Seq[SurgeryAvgInfo], operationCodeAndNames, this)
    }
    
    override def loadPastSurgeriesListener(file : File) : Unit = m_controller ! ReadPastSurgeriesExcelWork(file)
    
    override def loadProfitListener(file : File) : Unit = m_controller ! ReadProfitExcelWork(file)
    
    override def loadDoctorsIDMappingListener(file : File) : Unit = m_controller ! ReadDoctorsMappingExcelWork(file)
    
    override def loadSurgeryIDMappingListener(file : File) : Unit = m_controller ! ReadSurgeryMappingExcelWork(file)
}

trait UserActions
{
    def loadPastSurgeriesListener(file : File)
    
    def loadProfitListener(file : File)
    
    def loadDoctorsIDMappingListener(file : File)
    
    def loadSurgeryIDMappingListener(file : File)
    
}
