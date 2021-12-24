package view.mangerStatistics.windowElements

import model.DTOs.{DoctorStatistics, OperationCodeAndName, SurgeryAvgInfo, SurgeryAvgInfoByDoctor}
import scalafx.collections.ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType, ChoiceDialog, TableView}
import scalafx.scene.layout.VBox
import scalafx.stage.{FileChooser, Screen, Stage}
import view.common.UiUtils.{askIfToKeepMappingAndLoadPastSurgeries, getPathFromUserAndCall}
import view.mangerStatistics.StatisticsUserActions
import view.mangerStatistics.windowElements.ManagerMenu.Modes._

import java.io.File
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt


//Custom selection
//    table.selectionModel.apply.setCellSelectionEnabled(false)
//    table.selectionModel.apply.setSelectionMode(SelectionMode.Multiple)


class TableScene(val doctorsBaseStatistics : Seq[DoctorStatistics],
                 val surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]],
                 val surgeryAvgInfoList : Seq[SurgeryAvgInfo],
                 operationCodeAndNames : Seq[OperationCodeAndName],
                 stage : Stage,
                 userActions : StatisticsUserActions) extends Scene
{
    val APP_NAME = "Doctors Statistics"
    val m_settings = Await.result(userActions.getSettings, 5 seconds)
    
    val data = ObservableBuffer.empty[DoctorStatistics] ++= doctorsBaseStatistics
    val table = new TableView[DoctorStatistics](data)
    table.columns ++= Columns.columns
    table.onMouseClicked = e =>
    {
        if (e.getClickCount == 2){
            val doctorStatistics = table.getSelectionModel.getSelectedItem
            showDetailsDialog(doctorStatistics)
        }
    }
    
    // Dimensions settings
    table.columns.foreach(_.setPrefWidth(Screen.primary.bounds.width / Columns.columns.size))
    table.prefHeight = Screen.primary.bounds.height
    
    table.columns.foreach(_.setStyle("-fx-alignment: center;"))
    
    def loadPastSurgeriesAndCall = getPathFromUserAndCall(stage, "Select Past Surgeries File")(_)
    def loadProfitAndCall = getPathFromUserAndCall(stage, "Select Profit File")(_)
    def loadDoctorsIDMappingAndCall = getPathFromUserAndCall(stage, "Select Doctors ID Mapping File")(_)
    def loadSurgeryIDMappingAndCall = getPathFromUserAndCall(stage, "Select Surgery ID Mapping File")(_)
    
    val menu = new ManagerMenu(
        loadPastSurgeriesListener = _ => loadPastSurgeriesAndCall(askIfToKeepMappingAndLoadPastSurgeries(stage, userActions)),
        loadProfitListener = _ => loadProfitAndCall(userActions.loadProfitListener),
        loadSurgeryIDMappingListener = _ => loadSurgeryIDMappingAndCall(userActions.loadSurgeryIDMappingListener),
        loadDoctorsIDMappingListener = _ => loadDoctorsIDMappingAndCall(userActions.loadDoctorsIDMappingListener),
        
        radioBasicInformationListener = _ => setNormalState(),
        radioImprovementInformationAverageListener = _ => setImproveAvgState(),
        radioImprovementInformationByOperationListener = _ => getOperationFromUserAndSetMappers(),
        changeSettingsListener = _ => userActions.changeSetting(stage)
        )
    
    setNormalState()
    val vBox = new VBox()
    vBox.children = List(menu, table)
    root = vBox
    
    def setNormalState()
    {
        stage.title = s"$APP_NAME - $BASIC"
        table.items = data
        Columns.setColumnsNames(ColumnsNormalNames)
        Columns.setColumnsMappers(new TableSceneNormalMappers, m_settings)
        table.refresh()
    }
   
    def setImproveAvgState()
    {
        stage.title = s"$APP_NAME - $IMPROVE_AVG"
        table.items = data
        Columns.setColumnsNames(ColumnsAvgNames)
        Columns.setColumnsMappers(new TableSceneImprovementMappers(this), m_settings)
        table.refresh()
    }
   
    def setImproveByOpState(opName : String)
    {
        stage.title = s"$APP_NAME - $IMPROVE_BY_OP ($opName)"
        Columns.setColumnsNames(ColumnsAvgNames)
        table.refresh()
    }
    
    def getOperationFromUserAndSetMappers()
    {
        val dialog = new ChoiceDialog(defaultChoice = operationCodeAndNames.head, choices = operationCodeAndNames)
        {
            initOwner(stage)
            title = "Operation Choice"
            headerText = "Show improvement information by Operation"
            contentText = "Choose Operation"
        }
        
        val result = dialog.showAndWait()
        
        result match
        {
            case Some(choice) =>
            {
                println("Your choice: " + choice)
                val doctorsIds = surgeryAvgInfoByDoctorMap.values.flatten.filter(_.operationCode == choice.operationCode).map(_.doctorId).toSet
                table.items = data.filter(doc => doctorsIds.contains(doc.id))
                Columns.setColumnsMappers(new TableSceneImprovementMappersBySurgery(this, choice.operationCode), m_settings)
                setImproveByOpState(choice.toString)
            }
            
            case None => println("No selection")
        }
    }
    
    
    def showDetailsDialog(doctorStatistics : DoctorStatistics)
    {
        println(doctorStatistics)
        //todo show details Dialog
    }
}
