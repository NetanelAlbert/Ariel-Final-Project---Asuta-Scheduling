package view.schduling

import akka.actor.ActorSystem
import controller.Controller
import model.DTOs._
import org.joda.time.format.DateTimeFormat
import scalafx.application.{JFXApp3, Platform}
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.Alert
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.layout.HBox
import scalafx.scene.paint.Color._
import scalafx.scene.paint.{LinearGradient, Stops}
import scalafx.scene.text.Text
import view.common.actors.UserActions
import view.common.traits.MainWindowActions
import view.schduling.windowElements.TableScene

import java.io.File
import scala.concurrent.Future

object ScheduleMainWindow extends JFXApp3 with MainWindowActions
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
                        stops = Stops(Green, Orange))
                }
            }
        }
        import scala.concurrent.ExecutionContext.Implicits.global
        Future
        { //todo remove block
            Thread.sleep(2000)
            val format = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm")
            val list = List(
                FutureSurgeryInfo(1.121, 12, format.parseLocalDateTime("16/08/2021 07:30"), format.parseLocalDateTime("16/08/2021 07:30"), 1),
                FutureSurgeryInfo(2.126, 12, format.parseLocalDateTime("16/08/2021 09:30"), format.parseLocalDateTime("16/08/2021 07:30"), 1),
                FutureSurgeryInfo(3.152, 13, format.parseLocalDateTime("16/08/2021 12:30"), format.parseLocalDateTime("16/08/2021 07:30"), 1),
                FutureSurgeryInfo(5.51, 554, format.parseLocalDateTime("16/08/2021 11:30"), format.parseLocalDateTime("16/08/2021 07:30"), 2),
                FutureSurgeryInfo(76.42, 12, format.parseLocalDateTime("16/08/2021 15:30"), format.parseLocalDateTime("16/08/2021 07:30"), 2),
                FutureSurgeryInfo(11.2, 322, format.parseLocalDateTime("16/08/2021 07:30"), format.parseLocalDateTime("16/08/2021 07:30"), 2),
            )
            println(s"FutureSurgeryInfo list = ${list.mkString("\n")}")
            initializeWithScheduleData(list, new UserActions
            {
                override def loadPastSurgeriesListener(file : File) : Unit = ???
                
                override def loadProfitListener(file : File) : Unit = ???
                
                override def loadDoctorsIDMappingListener(file : File) : Unit = ???
                
                override def loadSurgeryIDMappingListener(file : File) : Unit = ???
            })
        }
    }
    
    override def initializeWithScheduleData(futureSurgeryInfo : Iterable[FutureSurgeryInfo], userActions : UserActions)
    {
        Platform.runLater
        {
            stage.scene = new TableScene(futureSurgeryInfo, stage, userActions)
            stage.maximized = false
            stage.maximized = true
        }
    }
    
    val m_system = ActorSystem("SchedulingSystem")
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
    
    override def initializeWithStatisticsData(doctorsBaseStatistics : Seq[DoctorStatistics], surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]], surgeryAvgInfoList : Seq[SurgeryAvgInfo], operationCodeAndNames : Seq[OperationCodeAndName], userActions : UserActions)
    {
        // Do nothing
        System.err.println("ScheduleMainWindow.initializeWithStatisticsData() called, but it should be use only for Statistics app.")
    }
}
