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
import view.schduling.windowElements.TableScene
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
    
    override def showOptionsForFreeBlock(startTime : LocalTime,
                                         endTime : LocalTime,
                                         date : LocalDate,
                                         topOptions : Seq[BlockFillingOption])
    {
        System.gc()
        Platform.runLater
        {
            // Labels
            val descriptionLabel = new Label()
            {
                GridPane.setValignment(this, VPos.Top)
                padding = Insets(0, 0, 0, 50)
            }
            val explanationLabel = new Label()
            {
                GridPane.setValignment(this, VPos.Top)
                padding = Insets(0, 0, 0, 50)
            }
            
            // ListView
            val optionsList = new ListView[BlockFillingOption](topOptions)
            optionsList.cellFactory = _ =>
            {
                new ListCell[BlockFillingOption]
                {
                    item.onChange{ (_, _, option) =>
                    {
                        if(Option(option).nonEmpty)
                        {
                            text = option.nameOrID
                        }
                    }}
                }
            }
            optionsList.getSelectionModel.selectedItemProperty.addListener( _ =>
            {
                val option = optionsList.selectionModel.apply.getSelectedItem
                descriptionLabel.text = option.description
                explanationLabel.text = option.explanation
            })
            topOptions.headOption.foreach(optionsList.getSelectionModel.select)
    
            val dialog = new Dialog[Nothing]()
            {
                initOwner(stage)
                title = "Block Filling Suggestions"
                //            headerText = "Choose time and room"
                // Set the button types.
                dialogPane().buttonTypes = Seq(ButtonType.OK)
            }
    
            val grid = new GridPane()
            {
                hgap = 10
                vgap = 10
                padding = Insets(20, 10, 10, 10)
                add(optionsList, 0, 0)
                add(descriptionLabel, 1, 0)
                add(explanationLabel, 2, 0)
            }
    
            grid.prefWidth = 1000
            dialog.dialogPane().setContent(grid)
    
            dialog.showAndWait()
        }
    }
}
