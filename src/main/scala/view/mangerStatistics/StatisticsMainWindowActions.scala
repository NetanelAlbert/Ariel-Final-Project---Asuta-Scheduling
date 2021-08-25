package view.mangerStatistics

import model.DTOs._
import scalafx.application.Platform
import scalafx.scene.control.Alert
import scalafx.scene.control.Alert.AlertType
import view.common.MainWindowActions

trait StatisticsMainWindowActions extends MainWindowActions
{
    def initializeWithStatisticsData(doctorsBaseStatistics : Seq[DoctorStatistics],
                                     surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]],
                                     surgeryAvgInfoList : Seq[SurgeryAvgInfo],
                                     operationCodeAndNames : Seq[OperationCodeAndName],
                                     userActions : StatisticsUserActions)
    
}
