package view.mangerStatistics.windows

import akka.actor.ActorSystem
import controller.Controller
import model.DTOs.{DoctorStatistics, SurgeryAvgInfo, SurgeryAvgInfoByDoctor}
import scalafx.application.{JFXApp3, Platform}

object MainWindow extends JFXApp3 with MainWindowActions
{
    override def start()
    {
        stage = MainStage(Seq(), Map(), Seq())
    }
    
    def getDoctorsStatisticsWorkSuccess(doctorsBaseStatistics : Seq[DoctorStatistics], surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]], surgeryAvgInfoList : Seq[SurgeryAvgInfo])
    {
        Platform.runLater
        {
            stage.scene = NormalTableScene(doctorsBaseStatistics, surgeryAvgInfoByDoctorMap, surgeryAvgInfoList)
            stage.maximized = false
            stage.maximized = true
        }
    }
    
    val m_system = ActorSystem("MangerStatisticsSystem")
    val m_controller = m_system.actorOf(Controller.props(this))
    
    m_system.registerOnTermination
    {
        println("ActorSystem is down. App stopped.")
        stopApp()
    }
    
    override def stopApp() : Unit =
    {
        m_system.terminate()
        super.stopApp()
    }
}

case class MainStage(doctorsBaseStatistics : Seq[DoctorStatistics],
                     surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]],
                     surgeryAvgInfoList : Seq[SurgeryAvgInfo]) extends JFXApp3.PrimaryStage
{
    maximized = true
//    scene = NormalTableScene(doctorsBaseStatistics, surgeryAvgInfoByDoctorMap, surgeryAvgInfoList)
    scene = ImprovementTableSceneBySurgery(doctorsBaseStatistics, surgeryAvgInfoByDoctorMap, surgeryAvgInfoList, 68.2)
}

trait MainWindowActions
{
    def getDoctorsStatisticsWorkSuccess(doctorsBaseStatistics : Seq[DoctorStatistics], surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]], surgeryAvgInfoList : Seq[SurgeryAvgInfo])
}
