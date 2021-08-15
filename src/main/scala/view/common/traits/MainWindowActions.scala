package view.common.traits

import model.DTOs.{DoctorStatistics, FutureSurgeryInfo, OperationCodeAndName, SurgeryAvgInfo, SurgeryAvgInfoByDoctor}
import view.common.actors.UserActions

trait MainWindowActions
{
    def initializeWithStatisticsData(doctorsBaseStatistics : Seq[DoctorStatistics],
                                     surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]],
                                     surgeryAvgInfoList : Seq[SurgeryAvgInfo],
                                     operationCodeAndNames : Seq[OperationCodeAndName],
                                     userActions : UserActions)
    
    def initializeWithScheduleData(futureSurgeryInfo : Iterable[FutureSurgeryInfo], userActions : UserActions)
    
    def showSuccessDialog(message : String)
    
    def showFailDialog(message : String)
}
