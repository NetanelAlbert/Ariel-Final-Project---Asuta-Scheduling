package view.schduling.windowElements

import model.DTOs.{DoctorStatistics, FutureSurgeryInfo, OperationCodeAndName, SurgeryAvgInfo, SurgeryAvgInfoByDoctor}
import scalafx.collections.ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.control.{ChoiceDialog, TableView}
import scalafx.scene.layout.VBox
import scalafx.stage.{FileChooser, Screen, Stage}
import view.common.actors.UserActions

import java.io.File


//Custom selection
//    table.selectionModel.apply.setCellSelectionEnabled(false)
//    table.selectionModel.apply.setSelectionMode(SelectionMode.Multiple)


class TableScene(futureSurgeryInfo : Iterable[FutureSurgeryInfo],
                 stage : Stage,
                 userActions : UserActions) extends Scene
{
    val APP_NAME = "Surgery Scheduling"
    stage.title = APP_NAME
    
    val table = new DailyTableView(futureSurgeryInfo)
    table.refresh()
    val menu = new SchedulingMenu(
        loadPastSurgeriesListener = _ => getPathFromUserAndCall(userActions.loadPastSurgeriesListener),
        loadProfitListener = _ => getPathFromUserAndCall(userActions.loadProfitListener),
        loadDoctorsIDMappingListener = _ => getPathFromUserAndCall(userActions.loadDoctorsIDMappingListener),
        loadSurgeryIDMappingListener = _ => getPathFromUserAndCall(userActions.loadSurgeryIDMappingListener),
        )

    val vBox = new VBox()
    vBox.children = List(menu, table)
    root = vBox
    
    def getPathFromUserAndCall(action : File => Unit)
    {
        val fileChooser = new FileChooser
        fileChooser.extensionFilters.addAll(new FileChooser.ExtensionFilter("Excel files", "*.xlsx"))
        val selectedFileOption = Option(fileChooser.showOpenDialog(stage))
        selectedFileOption.foreach(action)
    }
}
