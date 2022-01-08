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
import scala.concurrent.ExecutionContext

object StatisticsWindowManagerActor
{
    def props(m_controller : ActorRef, mainWindow : StatisticsMainWindowActions)(implicit ec : ExecutionContext) : Props = Props(new StatisticsWindowManagerActor(m_controller, mainWindow))
}

class StatisticsWindowManagerActor(override val m_controller : ActorRef, override val mainWindow : StatisticsMainWindowActions)(implicit override val ec : ExecutionContext) extends MyActor with StatisticsUserActions
{
    m_controller ! GetDoctorsStatisticsWork()
    
    
    override def receive =
    {
        case WorkSuccess(GetDoctorsStatisticsWork(Some(doctorsBaseStatistics), Some(surgeryAvgInfoByDoctorMap), Some(surgeryAvgInfoList), Some(operationCodeAndNames)), _) =>
        {
            mainWindow.hideProgressIndicator(true)
            getDoctorsStatisticsWork(doctorsBaseStatistics, surgeryAvgInfoByDoctorMap, surgeryAvgInfoList, operationCodeAndNames)
        }
        
        case WorkSuccess(_ : FileWork, _) =>
        {
            mainWindow.hideProgressIndicator(true)
            mainWindow.askAndReloadData(this)
        }

        case WorkSuccess(work, message) =>
        {
            mainWindow.hideProgressIndicator(true)
            mainWindow.showSuccessDialog(message.getOrElse("Action succeed"))
        }

        case WorkFailure(work, cause, message) =>
        {
            mainWindow.hideProgressIndicator(false)
            mainWindow.showFailDialog(cause, message)
        }
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
