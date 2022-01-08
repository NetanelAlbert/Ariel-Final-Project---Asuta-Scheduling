package view.common

import scalafx.Includes.jfxDialogPane2sfx
import scalafx.geometry.Insets
import scalafx.scene.control.ButtonBar.ButtonData
import scalafx.scene.control.{ButtonType, Dialog, ProgressIndicator}
import scalafx.scene.layout.GridPane
import scalafx.stage.Stage

class ProgressDialog(stage : Stage, progress : String) extends Dialog[Nothing]
{
    initOwner(stage)
    title = "In Progress - " + progress
    
    val progressIndicator = new ProgressIndicator()
    val grid = new GridPane
    {
        padding = Insets(20, 150, 20, 150)
    }
    grid.add(progressIndicator, 0, 0)
    
    dialogPane().setContent(grid)
    
    val resetButtonType = new ButtonType("Hide", ButtonData.CancelClose)
    dialogPane().buttonTypes = Seq(resetButtonType)
    
    def finish(status : Boolean)
    {
        val progress = if(status) 1 else 0
        progressIndicator.progress = progress
        dialogPane().buttonTypes = Seq(ButtonType.OK)
    }
}
