package view.mangerStatistics

import akka.actor.{ActorRef, Props}
import model.DTOs.{DoctorStatistics, OperationCodeAndName, SurgeryAvgInfo, SurgeryAvgInfoByDoctor}
import model.actors.MyActor
import org.joda.time.LocalTime
import scalafx.application.Platform
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.scene.control.Alert.AlertType
import view.common.{CommonUserActions, MainWindowActions}
import work._

import java.io.File

object StatisticsWindowManagerActor
{
    def props(m_controller : ActorRef, mainWindow : StatisticsMainWindowActions) : Props = Props(new StatisticsWindowManagerActor(m_controller, mainWindow))
}

class StatisticsWindowManagerActor(override val m_controller : ActorRef, mainWindow : StatisticsMainWindowActions) extends MyActor with StatisticsUserActions
{
    private var m_settingsActor : Option[ActorRef] = None
    m_controller ! GetDoctorsStatisticsWork()
    
    
    override def receive =
    {
        case WorkSuccess(GetDoctorsStatisticsWork(Some(doctorsBaseStatistics), Some(surgeryAvgInfoByDoctorMap), Some(surgeryAvgInfoList), Some(operationCodeAndNames)), _) => getDoctorsStatisticsWork(doctorsBaseStatistics, surgeryAvgInfoByDoctorMap, surgeryAvgInfoList, operationCodeAndNames)

        case TellAboutSettingsActorWork(settingsActor) => m_settingsActor = Some(settingsActor)

        
        
        case WorkSuccess(_ : FileWork, _) => mainWindow.askAndReloadData(this)

        case WorkSuccess(work, message) => mainWindow.showSuccessDialog(message.getOrElse("Action succeed"))

        case WorkFailure(work, cause, message) => mainWindow.showFailDialog(cause, message)
    }
    
    def getDoctorsStatisticsWork(doctorsBaseStatistics : Seq[DoctorStatistics], surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]], surgeryAvgInfoList : Seq[SurgeryAvgInfo], operationCodeAndNames : Seq[OperationCodeAndName])
    {
        mainWindow.initializeWithStatisticsData(doctorsBaseStatistics : Seq[DoctorStatistics], surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]], surgeryAvgInfoList : Seq[SurgeryAvgInfo], operationCodeAndNames, this)
    }
    
    override def reloadDefaultData
    {
        m_controller ! GetDoctorsStatisticsWork()
    }
    
}

trait StatisticsUserActions extends CommonUserActions
{
    this : MyActor =>
}
