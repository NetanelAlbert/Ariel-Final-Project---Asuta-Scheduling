package view.common

import scalafx.application.Platform
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.stage.{FileChooser, Stage}
import view.schduling.SchedulingUserActions

import java.io.File

object UiUtils
{
//    def getPathFromUserAndCall(stage : Stage)(action : File => Unit)
//    {
//        val fileChooser = new FileChooser
//        fileChooser.extensionFilters.addAll(new FileChooser.ExtensionFilter("Excel files", "*.xlsx"))
//        fileChooser.title = "x x x x x x x"
//        val selectedFileOption = Option(fileChooser.showOpenDialog(stage))
//        selectedFileOption.foreach(action)
//    }
    
    private var lastFolder = System.getProperty("user.home")
    
    def getPathFromUserAndCall(stage : Stage, title : String)(action : File => Unit)
    {
        val fileChooser = new FileChooser
        fileChooser.extensionFilters.addAll(new FileChooser.ExtensionFilter("Excel files", "*.xlsx"))
        fileChooser.title = title
        fileChooser.setInitialDirectory(new File(lastFolder))
        
        val selectedFileOption = Option(fileChooser.showOpenDialog(stage))
        selectedFileOption.foreach(file => lastFolder = file.getParent)
        selectedFileOption.foreach(action)
    }
    
    
    def askIfToKeepMappingAndLoadPastSurgeries(stage: Stage, userActions : CommonUserActions)(file : File)
    {
        val KeepButton = new ButtonType("Keep")
        val RemoveButton = new ButtonType("Remove")
        
        val alert = new Alert(AlertType.Confirmation) {
            initOwner(stage)
            headerText = "Loading past surgeries information."
            contentText = "Do you want to keep the old names of doctors and operations, and profit information?"
            buttonTypes = Seq(KeepButton, RemoveButton, ButtonType.Cancel)
        }
        
        val result = alert.showAndWait()
        
        result match {
            case Some(KeepButton)   => userActions.loadPastSurgeriesListener(file, true)
            
            case Some(RemoveButton)   => userActions.loadPastSurgeriesListener(file, false)
            
            case _ => // Do nothing
        }
    }
    
    def askIfToKeepMappingAndLoadSchedule(stage: Stage, userActions : SchedulingUserActions)(file : File)
    {
        val KeepButton = new ButtonType("Keep")
        val RemoveButton = new ButtonType("Remove")
        
        val alert = new Alert(AlertType.Confirmation) {
            initOwner(stage)
            headerText = "Loading schedule information."
            contentText = "Do you want to keep old schedule data?"
            buttonTypes = Seq(KeepButton, RemoveButton, ButtonType.Cancel)
        }
        
        val result = alert.showAndWait()
        
        result match {
            case Some(KeepButton)   => userActions.loadScheduleListener(file, true)
            
            case Some(RemoveButton)   => userActions.loadScheduleListener(file, false)
            
            case _ => // Do nothing
        }
    }
}
