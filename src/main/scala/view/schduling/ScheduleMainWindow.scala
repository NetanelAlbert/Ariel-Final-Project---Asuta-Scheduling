package view.schduling

import akka.actor.ActorSystem
import controller.Controller
import model.DTOs._
import org.joda.time.{LocalDate, LocalTime}
import org.joda.time.format.DateTimeFormat
import scalafx.Includes.jfxDialogPane2sfx
import scalafx.application.{JFXApp3, Platform}
import scalafx.geometry.{HPos, Insets, Pos, VPos}
import scalafx.scene.Scene
import scalafx.scene.control.{Alert, ButtonType, Dialog, Label, ListCell, ListView}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.layout.{GridPane, HBox}
import scalafx.scene.paint.Color._
import scalafx.scene.paint.{LinearGradient, Stops}
import scalafx.scene.text.Text
import view.common.MainWindowActions
import view.mangerStatistics.StatisticsUserActions
import view.schduling.windowElements.{ShowOptionsBlocksDialog, TableScene}
import work.{BlockFillingOption, GetOptionsForFreeBlockWork}

import java.io.File
import scala.concurrent.Future

object ScheduleMainWindow extends JFXApp3 with SchedulingMainWindowActions
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
    }
    
    override def initializeWithData(futureSurgeryInfo : Iterable[FutureSurgeryInfo], blocks : Map[LocalDate, Set[Block]], userActions : SchedulingUserActions)
    {
        Platform.runLater
        {
            stage.scene = new TableScene(futureSurgeryInfo, blocks, stage, userActions)
            stage.maximized = false
            stage.maximized = true
        }
    }
    
    val m_system = ActorSystem("SchedulingSystem")
    val m_controller = m_system.actorOf(Controller.props(this), "Controller")
    
    m_system.registerOnTermination
    {
        println("ActorSystem is down. Stopping app.")
        stopApp()
    }
    
    override def stopApp() : Unit =
    {
        println("App stopped. Terminating ActorSystem.")
        m_system.terminate()
        super.stopApp()
    }
    
    def showOptionsForFreeBlock(startTime : LocalTime,
                                endTime : LocalTime,
                                date : LocalDate,
                                topOptions : Seq[BlockFillingOption],
                                settings : Settings)
    {
        Platform.runLater
        {
            val dialog = new ShowOptionsBlocksDialog(stage, startTime, endTime, date, topOptions, settings)
            dialog.showAndWait()
        }
    }
}
