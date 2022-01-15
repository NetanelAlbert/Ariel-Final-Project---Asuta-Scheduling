package view.common

import model.DTOs.Priority.Priority
import model.DTOs.{Priority, Settings, SettingsObject}
import model.database.DoctorPriorityAndName
import scalafx.Includes.jfxDialogPane2sfx
import scalafx.geometry.HPos
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.ButtonBar.ButtonData
import scalafx.scene.control.{Alert, ButtonType, Dialog, Label, ListView}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.GridPane
import scalafx.stage.Stage
import view.common.SettingEditorDialog.SettingsGridPane
import view.mangerStatistics.windowElements.Styles.CENTER_BOLD

import scala.collection.mutable

object PriorityEditorDialog
{
    val right = new Image("icons/right-arrow-80.png")
    val left = new Image("icons/left-arrow-80.png")
}

class PriorityEditorDialog(
    stage : Stage,
    doctorsPriorityAndNames : Seq[DoctorPriorityAndName],
) extends Dialog[Iterable[(Int, Priority)]]
{
    import PriorityEditorDialog._
    
    initOwner(stage)
    title = "Doctors Priority Editor"
    
    val changesMap = mutable.Map[Int, Priority]()
    
    val doctorsPriorityAndNamesByPriority = doctorsPriorityAndNames.groupBy(_.priority)
    val staredList = new ListView[DoctorPriorityAndName](doctorsPriorityAndNamesByPriority.getOrElse(Priority.STAR, Nil))
    val normalList = new ListView[DoctorPriorityAndName](doctorsPriorityAndNamesByPriority.getOrElse(Priority.NORMAL, Nil))
    val hiddenList = new ListView[DoctorPriorityAndName](doctorsPriorityAndNamesByPriority.getOrElse(Priority.HIDDEN, Nil))
    
    val staredToNormal = new MoveSelectedButton(right, Priority.NORMAL, staredList, normalList, changesMap.put)
    val normalToStared = new MoveSelectedButton(left, Priority.STAR, normalList, staredList, changesMap.put)
    val normalToHidden = new MoveSelectedButton(right, Priority.HIDDEN, normalList, hiddenList, changesMap.put)
    val hiddenToNormal = new MoveSelectedButton(left, Priority.NORMAL, hiddenList, normalList, changesMap.put)
    
    val grid = new GridPane()
    grid.add(staredList, 0, 1, 1, 2)
    grid.add(new Label("Stared"){style = CENTER_BOLD}, 0, 0)
    
    grid.add(staredToNormal, 1, 1)
    grid.add(normalToStared, 1, 2)
    
    grid.add(new Label("Normal"){style = CENTER_BOLD}, 2, 0)
    grid.add(normalList, 2, 1, 1, 2)
    
    grid.add(normalToHidden, 3, 1)
    grid.add(hiddenToNormal, 3, 2)
    
    grid.add(new Label("Hidden"){style = CENTER_BOLD}, 4, 0)
    grid.add(hiddenList, 4, 1, 1, 2)
    
    dialogPane().setContent(grid)
    
    GridPane.setHalignment(staredToNormal, HPos.Center);
    GridPane.setHalignment(normalToStared, HPos.Center);
    GridPane.setHalignment(normalToHidden, HPos.Center);
    GridPane.setHalignment(hiddenToNormal, HPos.Center);
    
    val resetButtonType = new ButtonType("Reset", ButtonData.Other)
    dialogPane().buttonTypes = Seq(ButtonType.OK, resetButtonType, ButtonType.Cancel)
    
    resultConverter =
    {
        dialogButton =>
        {
            if (dialogButton == ButtonType.OK)
            {
                changesMap.toMap
            }
            else if (dialogButton == resetButtonType)
            {
                val alert = new Alert(AlertType.Confirmation) {
                    initOwner(stage)
                    title = "Reset Settings"
                    headerText =
                        """Are you sure you want to reset all the priorities?
                          |It will set the stared and the hidden to normal.
                          |It can't be undone.""".stripMargin
                }
    
                val result = alert.showAndWait()
    
                result match
                {
                    case Some(ButtonType.OK) => doctorsPriorityAndNames.map(doc => (doc.id, Priority.NORMAL))
                    
                    case _ => null
                }
            }
            else
            {
                null
            }
        }
    }
}

class MoveSelectedButton(image : Image, toPriority : Priority, from : ListView[DoctorPriorityAndName], to : ListView[DoctorPriorityAndName], updatePriority : (Int, Priority) => Unit) extends ImageView(image)
{
    pickOnBounds = true
    
    onMouseClicked = _ =>
    {
        if(! from.selectionModel().isEmpty)
        {
            val selectedItem = from.selectionModel().getSelectedItem
            val selectedItemWithNewPriority = selectedItem.copy(priority = toPriority)
    
            from.items().remove(selectedItem)
    
            to.items().add(selectedItemWithNewPriority)
            to.scrollTo(selectedItemWithNewPriority)
            to.selectionModel().select(selectedItemWithNewPriority)
            to.requestFocus()
    
            updatePriority(selectedItem.id, toPriority)
        }
    }
}


