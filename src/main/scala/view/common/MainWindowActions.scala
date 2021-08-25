package view.common

import scalafx.application.Platform
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.scene.control.Alert.AlertType
import scalafx.stage.Stage

trait MainWindowActions
{
    def showSuccessDialog(message : String)
    {
        Platform.runLater
        {
            new Alert(AlertType.Information, message).showAndWait()
        }
    }
    
    def showFailDialog(message : String)
    {
        Platform.runLater
        {
            new Alert(AlertType.Error, message).showAndWait()
        }
    }
    
    def showFailDialog(cause : Option[Throwable], message : Option[String])
    {
        showFailDialog(
            s"""Action failed.
              |Message: ${message.getOrElse(None)}.
              |Cause: ${cause.map(_.getMessage).getOrElse("unknown")}""".stripMargin)
    }
    
    def stage : Stage
    
    
    def askAndReloadData(userAction : CommonUserActions)
    {
        Platform.runLater
        {
            val message = "Data loaded succesfully.\nDo you want to reload?"

            // Create and show confirmation alert
            val alert = new Alert(AlertType.Confirmation) {
                initOwner(stage)
                title = "Reload Data"
                headerText = message
            }
            
            val result = alert.showAndWait()
            
            // React to user's selection
            result match {
                case Some(ButtonType.OK) => userAction.reloadDefaultData
                case _ => // Do nothing
            }
        }
    }
}
