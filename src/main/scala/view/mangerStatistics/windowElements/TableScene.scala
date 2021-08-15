package view.mangerStatistics.windowElements

import model.DTOs.{DoctorStatistics, OperationCodeAndName, SurgeryAvgInfo, SurgeryAvgInfoByDoctor}
import scalafx.collections.ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.control.{ChoiceDialog, TableView}
import scalafx.scene.layout.VBox
import scalafx.stage.{FileChooser, Screen, Stage}
import view.actors.UserActions

import java.io.File


//Custom selection
//    table.selectionModel.apply.setCellSelectionEnabled(false)
//    table.selectionModel.apply.setSelectionMode(SelectionMode.Multiple)


class TableScene(val doctorsBaseStatistics : Seq[DoctorStatistics],
                 val surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]],
                 val surgeryAvgInfoList : Seq[SurgeryAvgInfo],
                 operationCodeAndNames : Seq[OperationCodeAndName],
                 stage : Stage,
                 userActions : UserActions) extends Scene
{
    val APP_NAME = "Doctors Statistics"
    
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
    
    val menu = new ManagerMenu(
        loadPastSurgeriesListener = _ => getPathFromUserAndCall(userActions.loadPastSurgeriesListener),
        loadProfitListener = _ => getPathFromUserAndCall(userActions.loadProfitListener),
        loadDoctorsIDMappingListener = _ => getPathFromUserAndCall(userActions.loadDoctorsIDMappingListener),
        loadSurgeryIDMappingListener = _ => getPathFromUserAndCall(userActions.loadSurgeryIDMappingListener),
        
        radioBasicInformationListener = _ => setNormalState(),
        radioImprovementInformationAverageListener = _ => setImproveAvgState(),
        radioImprovementInformationByOperationListener = _ => getOperationFromUserAndSetMappers()
        )
    
    setNormalState()
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
    
    def setNormalState()
    {
        stage.title = s"$APP_NAME - $BASIC"
        table.items = data
        Columns.setColumnsNames(ColumnsNormalNames)
        Columns.setColumnsMappers(new TableSceneNormalMappers)
        table.refresh()
    }
   
    def setImproveAvgState()
    {
        stage.title = s"$APP_NAME - $IMPROVE_AVG"
        table.items = data
        Columns.setColumnsNames(ColumnsAvgNames)
        Columns.setColumnsMappers(new TableSceneImprovementMappers(this))
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
                Columns.setColumnsMappers(new TableSceneImprovementMappersBySurgery(this, choice.operationCode))
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
