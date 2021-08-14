package view.mangerStatistics.actors

import akka.actor.{ActorRef, Props}
import model.DTOs.{DoctorStatistics, SurgeryAvgInfo, SurgeryAvgInfoByDoctor}
import model.actors.MyActor
import view.mangerStatistics.windows.MainWindowActions
import work.GetDoctorsStatisticsWork

object WindowManagerActor
{
    def props(m_controller : ActorRef, mainWindow : MainWindowActions) : Props = Props(new WindowManagerActor(m_controller, mainWindow))
}

class WindowManagerActor(m_controller : ActorRef, mainWindow : MainWindowActions) extends MyActor
{
    
    
    override def receive =
    {
        case GetDoctorsStatisticsWork(Some(doctorsBaseStatistics), Some(surgeryAvgInfoByDoctorMap), Some(surgeryAvgInfoList)) => getDoctorsStatisticsWork(doctorsBaseStatistics, surgeryAvgInfoByDoctorMap, surgeryAvgInfoList)
    }
    
    def getDoctorsStatisticsWork(doctorsBaseStatistics : Seq[DoctorStatistics], surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]], surgeryAvgInfoList : Seq[SurgeryAvgInfo])
    {
        mainWindow.getDoctorsStatisticsWorkSuccess(doctorsBaseStatistics : Seq[DoctorStatistics], surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]], surgeryAvgInfoList : Seq[SurgeryAvgInfo])
    }
    
}
