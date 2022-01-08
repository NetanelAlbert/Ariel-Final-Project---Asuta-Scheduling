package view.common

import akka.actor.ActorRef
import model.DTOs.Settings
import model.actors.{MyActor, SettingsAccess}
import scalafx.application.Platform
import scalafx.scene.control.Alert
import scalafx.scene.control.Alert.AlertType
import scalafx.stage.Stage
import work.{ReadDoctorsMappingExcelWork, ReadPastSurgeriesExcelWork, ReadProfitExcelWork, ReadSurgeryMappingExcelWork}

import java.io.File
import scala.util.{Failure, Success}

trait CommonUserActions extends SettingsAccess
{
    this : MyActor =>
    
    val m_controller : ActorRef
    val mainWindow : MainWindowActions
    
    def loadPastSurgeriesListener(file : File, keepOldMapping : Boolean)
    {
        m_controller ! ReadPastSurgeriesExcelWork(file, keepOldMapping)
        mainWindow.showProgressIndicator("Load Past Surgeries")
    }
    
    def loadProfitListener(file : File) : Unit = m_controller ! ReadProfitExcelWork(file)
    
    def loadDoctorsIDMappingListener(file : File) : Unit = m_controller ! ReadDoctorsMappingExcelWork(file)
    
    def loadSurgeryIDMappingListener(file : File) : Unit = m_controller ! ReadSurgeryMappingExcelWork(file)
    
    def reloadDefaultData
    
    def changeSettingAndThen(stage : Stage)(onSuccessAction : Settings => Unit)
    {
        getSettings.onComplete
        {
            case Success(settings) =>
            {
                Platform.runLater
                {
                    val settingEditorDialog = new SettingEditorDialog(stage, settings)
                    val result = settingEditorDialog.showAndWait()
                    result match
                    {
                        case Some(newSettings : Settings) =>
                        {
                            setSettings(newSettings).onComplete
                            {
                                case Success(_) =>
                                {
                                    mainWindow.showSuccessDialog("Settings changed successfully.")
                                    onSuccessAction(settings)
                                }
                    
                                case Failure(exception) =>
                                {
                                    val errorInfo = "Failed to change settings."
                                    log.error(exception, errorInfo)
                                    mainWindow.showFailDialog(errorInfo)
                                }
                            }
                        }
            
                        case None => log.info("changeSetting dialog close without action")
                    }
                }
            }
            
            case Failure(exception) =>
            {
                val errorInfo = "Failed to get current settings."
                log.error(exception, errorInfo)
                mainWindow.showFailDialog(errorInfo)
            }
        }
    }
}