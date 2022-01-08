package view.common

import scalafx.application.Platform
import scalafx.scene.control.{Alert, ButtonType, Dialog, ProgressIndicator}
import scalafx.scene.control.Alert.AlertType
import scalafx.stage.Stage

trait MainWindowActions
{
    def stage : Stage
    
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
    
    
    def askAndReloadData(userAction : CommonUserActions)
    {
        hideProgressIndicator(true)
        System.gc()
        Platform.runLater
        {
            val message = "Data loaded successfully. \nDo you want to reload?"

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
    
    private var m_progressDialog : Option[ProgressDialog] = None
    
    def showProgressIndicator(progress : String)
    {
        Platform.runLater
        {
            val progressDialog = new ProgressDialog(stage, progress)
            m_progressDialog = Some(progressDialog)
            progressDialog.showAndWait()
        }
    }
    
    def hideProgressIndicator(status : Boolean)
    {
        Platform.runLater
        {
            m_progressDialog.foreach(progressDialog =>
            {
                progressDialog.finish(status)
            })
            m_progressDialog = None
        }
    }
}
