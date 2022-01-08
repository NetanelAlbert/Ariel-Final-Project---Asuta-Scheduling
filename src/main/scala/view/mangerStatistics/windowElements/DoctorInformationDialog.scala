package view.mangerStatistics.windowElements

import model.DTOs.{DoctorStatistics, OperationCodeAndName, SurgeryAvgInfo, SurgeryAvgInfoByDoctor}
import scalafx.Includes.jfxDialogPane2sfx
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control._
import scalafx.scene.layout.GridPane
import scalafx.stage.Stage
import view.common.UiUtils.Double2digitsMapper

class DoctorInformationDialog(
    doctorStatistics : DoctorStatistics,
    surgeriesAvgInfoByDoctor : Seq[SurgeryAvgInfoByDoctor],
    surgeryAvgInfoList : Seq[SurgeryAvgInfo],
    operationCodeAndNames : Seq[OperationCodeAndName],
    stage : Stage,
) extends Dialog[Nothing]
{
    
    val PRIVATE_COL_NAME = "Personal"
    val GLOBAL_COL_NAME = "Global"
    
    initOwner(stage)
    title = s"Doctor Information for ${doctorStatistics.nameOrId}"

    dialogPane().buttonTypes = Seq(ButtonType.OK)
    
    val operationCodeToNameMap = operationCodeAndNames.flatMap
    {
        case OperationCodeAndName(operationCode, name) => name.map(operationCode -> _)
    }.toMap
    
    val surgeryAvgInfoMap = surgeryAvgInfoList.map(surgeryAvgInfo =>
    {
        surgeryAvgInfo.operationCode -> surgeryAvgInfo
    }).toMap
    
    val optionsTable = getOptionsTable
    
    val grid = new GridPane()
    {
        hgap = 10
        vgap = 10
        padding = Insets(20, 10, 10, 10)
        
        add(optionsTable, 0, 2)
//        prefWidth = 1000
    }
    
    
    this.dialogPane().setContent(grid)
    
    def getOptionsTable =
    {
        new TableView[SurgeryAvgInfoByDoctor](ObservableBuffer.empty[SurgeryAvgInfoByDoctor] ++= surgeriesAvgInfoByDoctor)
        {
            columns ++= getColumns
//            columns.foreach(_.setStyle("-fx-alignment: center;"))
        
            //        // Dimensions settings
            //        columns.foreach(_.setPrefWidth(Screen.primary.bounds.width / Columns.columns.size))
            //        prefHeight = Screen.primary.bounds.height
//                    prefWidth = 9 * 120
        }
    }
    
    def getColumns =
    {
        val surgeryNameCol = new TableColumn[SurgeryAvgInfoByDoctor, String]("Surgery Name")
        {
            cellValueFactory = cdf =>
            {
                val operationCode = cdf.value.operationCode
                StringProperty(operationCodeToNameMap.getOrElse(operationCode, operationCode.toString))
            }
            style = "-fx-alignment: center;"
            prefWidth = 120
        }
        
        val amountOfDataCol = new GlobalPrivateColumn("Amount Of Data", _.amountOfData, _.amountOfData)
        
        val surgeryDurationCol = new GlobalPrivateColumn(ColumnsNormalNames.AVG_SURGERY, _.surgeryDurationAvgMinutes.double2digits, _.surgeryDurationAvgMinutes.double2digits)
        
        val restingDurationCol = new GlobalPrivateColumn(ColumnsNormalNames.AVG_RESTING, _.restingDurationAvgMinutes.double2digits, _.restingDurationAvgMinutes.double2digits)
        
        val hospitalizationDurationCol = new GlobalPrivateColumn(ColumnsNormalNames.AVG_HOSPITALIZATION, _.hospitalizationDurationAvgHours.double2digits, _.hospitalizationDurationAvgHours.double2digits)
        
        List[javafx.scene.control.TableColumn[SurgeryAvgInfoByDoctor, _]](
            surgeryNameCol,
            surgeryDurationCol,
            restingDurationCol,
            hospitalizationDurationCol,
            amountOfDataCol,
            )
    }
    
    private class GlobalPrivateColumn[T](text : String, privateMapper : SurgeryAvgInfoByDoctor => T, globalMapper : SurgeryAvgInfo => T) extends TableColumn[SurgeryAvgInfoByDoctor, T](text)
    {
        val privateCol = new TableColumn[SurgeryAvgInfoByDoctor, T](PRIVATE_COL_NAME)
        {
            cellValueFactory = cdf => ObjectProperty(privateMapper(cdf.value))
            style = "-fx-alignment: center;"
            prefWidth = 100
//            width_=(100)
        }
    
        val globalCol = new TableColumn[SurgeryAvgInfoByDoctor, T](GLOBAL_COL_NAME)
        {
            cellValueFactory = cdf => ObjectProperty(globalMapper(surgeryAvgInfoMap(cdf.value.operationCode)))
            style = "-fx-alignment: center;"
            prefWidth = 120
//            width_=(100)
        }
        
        columns.addAll(privateCol, globalCol)
    }
}
