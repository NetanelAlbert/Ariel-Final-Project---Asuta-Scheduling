package view.mangerStatistics

import akka.actor.ActorSystem
import controller.Controller
import model.DTOs.{DoctorStatistics, OperationCodeAndName, SurgeryAvgInfo, SurgeryAvgInfoByDoctor}
import scalafx.application.{JFXApp3, Platform}
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.Alert
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.layout.HBox
import scalafx.scene.paint.Color._
import scalafx.scene.paint.{LinearGradient, Stops}
import scalafx.scene.text.Text
import view.actors.UserActions
import view.mangerStatistics.windowElements.TableScene

object MainWindow extends JFXApp3 with MainWindowActions
{
    override def start()
    {
        stage = new JFXApp3.PrimaryStage
        stage.scene = new Scene
        {
            content = new HBox
            {
                padding = Insets(50, 80, 50, 80)
                children = new Text
                {
                    text = "Loading..."
                    style = "-fx-font: normal bold 70pt sans-serif"
                    fill = new LinearGradient(
                        endX = 0,
                        stops = Stops(Green, DarkGreen))
                }
                
            }
        }
    }
    
    def initializeWithData(doctorsBaseStatistics : Seq[DoctorStatistics], surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]], surgeryAvgInfoList : Seq[SurgeryAvgInfo], operationCodeAndNames : Seq[OperationCodeAndName], userActions : UserActions)
    {
        Platform.runLater
        {
            stage.scene = new TableScene(doctorsBaseStatistics, surgeryAvgInfoByDoctorMap, surgeryAvgInfoList, operationCodeAndNames, stage, userActions)
            stage.maximized = false
            stage.maximized = true
        }
    }
    
    val m_system = ActorSystem("MangerStatisticsSystem")
    val m_controller = m_system.actorOf(Controller.props(this), "Controller")
    
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
    
    override def showSuccessDialog(message : String)
    {
        Platform.runLater
        {
            new Alert(AlertType.Information, message).showAndWait()
        }
    }
    
    override def showFailDialog(message : String)
    {
        Platform.runLater
        {
            new Alert(AlertType.Error, message).showAndWait()
        }
    }
}

//case class MainStage(doctorsBaseStatistics : Seq[DoctorStatistics],
//                     surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]],
//                     surgeryAvgInfoList : Seq[SurgeryAvgInfo]) extends JFXApp3.PrimaryStage
//{
//    maximized = true
//}

trait MainWindowActions
{
    def initializeWithData(doctorsBaseStatistics : Seq[DoctorStatistics],
                           surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]],
                           surgeryAvgInfoList : Seq[SurgeryAvgInfo],
                           operationCodeAndNames : Seq[OperationCodeAndName],
                           userActions : UserActions)
    
    def showSuccessDialog(message : String)
    
    def showFailDialog(message : String)
}
