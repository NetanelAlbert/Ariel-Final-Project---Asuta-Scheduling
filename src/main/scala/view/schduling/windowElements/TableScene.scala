package view.schduling.windowElements

import model.DTOs.{Block, FutureSurgeryInfo}
import org.joda.time.{LocalDate, LocalTime}
import scalafx.Includes.jfxDialogPane2sfx
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.{Button, ButtonType, ComboBox, Dialog, Label}
import scalafx.scene.layout.{GridPane, HBox, Priority, StackPane, VBox}
import scalafx.stage.{Screen, Stage}
import scalafx.util.StringConverter
import view.common.UiUtils
import view.common.UiUtils.{askIfToKeepMappingAndLoadPastSurgeries, getPathFromUserAndCall}
import view.schduling.SchedulingUserActions
import common.Utils._

class TableScene(futureSurgeryInfo : Iterable[FutureSurgeryInfo],
                 blocks : Map[LocalDate, Set[Block]],
                 stage : Stage,
                 userActions : SchedulingUserActions) extends Scene
{
    val APP_NAME = "Surgery Scheduling"
    stage.title = APP_NAME
    
    //todo remove
    userActions.getSurgeriesSuggestions(LocalTime.now(), LocalTime.now(), LocalDate.now())
    
    val table = new DailyTableView(futureSurgeryInfo, blocks)
//    table.refresh() todo
    
    def loadPastSurgeriesAndCall = getPathFromUserAndCall(stage, "Select Past Surgeries File")(_)
    def loadProfitAndCall = getPathFromUserAndCall(stage, "Select Profit File")(_)
    def loadDoctorsIDMappingAndCall = getPathFromUserAndCall(stage, "Select Doctors ID Mapping File")(_)
    def loadSurgeryIDMappingAndCall = getPathFromUserAndCall(stage, "Select Surgery ID Mapping File")(_)
    def loadScheduleAndCall = getPathFromUserAndCall(stage, "Select Schedule File")(_)
    
    val menu = new SchedulingMenu(
        loadPastSurgeriesListener = _ => loadPastSurgeriesAndCall(askIfToKeepMappingAndLoadPastSurgeries(stage, userActions)),
        loadProfitListener = _ => loadProfitAndCall(userActions.loadProfitListener),
        loadSurgeryIDMappingListener = _ => loadSurgeryIDMappingAndCall(userActions.loadSurgeryIDMappingListener),
        loadDoctorsIDMappingListener = _ => loadDoctorsIDMappingAndCall(userActions.loadDoctorsIDMappingListener),
        loadScheduleListener = _ => loadScheduleAndCall(UiUtils.askIfToKeepMappingAndLoadSchedule(stage, userActions))
        )
    
    val suggestionsButton = new Button("Get Suggestions")
    {
        onAction = _ => showGetSuggestionsDialog
        prefWidth = 500
        margin = Insets(0, 50, 0, 0)
        style = "-fx-font-weight: bold"
    }
    
    val hBox = new HBox(menu, suggestionsButton)
    HBox.setHgrow(menu, Priority.Always)
    HBox.setHgrow(suggestionsButton, Priority.Never)
    
    
    // Create Table
    val stackPane = new StackPane()
    stackPane.getChildren.add(table)
    stackPane.prefHeight = Screen.primary.bounds.height
    val vBox = new VBox()
    vBox.children = List(hBox, stackPane)
    root = vBox
    
    
    def showGetSuggestionsDialog
    {
        // TODO get values from table selection items
        val hours = table.workingHours
        val from = hours.head
        val to = from.plusHours(3)
    
        val converter = new StringConverter[LocalTime]
        {
            override def fromString(string : String) = timeFormat.parseLocalTime(string)
        
            override def toString(t : LocalTime) = t.toString(timeFormat)
        }

        val fromChoice = new ComboBox(hours)
        fromChoice.setValue(from)
        fromChoice.converter = converter
        val toChoice = new ComboBox(hours)
        toChoice.setValue(to)
        toChoice.converter = converter

        
        case class Result(from : LocalTime, to : LocalTime)
        // Create the custom dialog.
        val dialog = new Dialog[Result]() {
            initOwner(stage)
            title = "Get Suggestions"
            headerText = "Choose time and room"
            // Set the button types.
            dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)
        }
    
    
        val grid = new GridPane() {
            hgap = 10
            vgap = 10
            padding = Insets(20, 100, 10, 10)
        
            add(new Label("From:"), 0, 0)
            add(fromChoice, 0, 1)
            
            add(new Label("To:"), 1, 0)
            add(toChoice, 1, 1)
        }
        
        dialog.dialogPane().setContent(grid)
        
        dialog.resultConverter = dialogButton =>
            if (dialogButton == ButtonType.OK)
                Result(fromChoice.getValue, toChoice.getValue)
            else
                null
    
        val result = dialog.showAndWait()
    
        result match {
            case Some(Result(from, to)) => userActions.getSurgeriesSuggestions(from, to, table.date)
            case _ => // Do nothing
        }
    }
}
