package view.schduling

import akka.actor.{Actor, ActorRef, Props}
import model.DTOs.{DoctorStatistics, OperationCodeAndName, Settings, SurgeryAvgInfo, SurgeryAvgInfoByDoctor}
import model.actors.MyActor
import org.joda.time.{LocalDate, LocalTime}
import view.common.{CommonUserActions, MainWindowActions}
import work.{FileWork, GetCurrentScheduleWork, GetDoctorsStatisticsWork, GetOptionsForFreeBlockWork, ReadDoctorsMappingExcelWork, ReadFutureSurgeriesExcelWork, ReadPastSurgeriesExcelWork, ReadProfitExcelWork, ReadSurgeryMappingExcelWork, TellAboutSettingsActorWork, WorkFailure, WorkSuccess}

import java.io.File
import scala.concurrent.Future

object SchedulingWindowManagerActor
{
    def props(m_controller : ActorRef, mainWindow : SchedulingMainWindowActions) : Props = Props(new SchedulingWindowManagerActor(m_controller, mainWindow))
}

class SchedulingWindowManagerActor(override val m_controller : ActorRef, mainWindow : SchedulingMainWindowActions) extends MyActor with SchedulingUserActions
{
    private var m_settingsActor : Option[ActorRef] = None
    reloadDefaultData
    
    override def receive =
    {
        case TellAboutSettingsActorWork(settingsActor) => m_settingsActor = Some(settingsActor)

        case WorkSuccess(work : GetOptionsForFreeBlockWork, _) => mainWindow.showOptionsForFreeBlock(work)

        case WorkSuccess(GetCurrentScheduleWork(_, _, Some(schedule), Some(blocks)), _) => mainWindow.initializeWithData(schedule, blocks, this) //TODO maybe just change data on second time an on

        
        
        case WorkSuccess(_ : FileWork, _) => mainWindow.askAndReloadData(this)
        
        case WorkSuccess(work, message) => mainWindow.showSuccessDialog(message.getOrElse("Action succeed"))

        case WorkFailure(work, cause, message) => mainWindow.showFailDialog(cause, message)
    }
    
    override def reloadDefaultData
    {
        val now = LocalDate.now()
        getCurrentSchedule(now.minusDays(now.getDayOfWeek),
                           now.plusDays(6 - now.getDayOfWeek))
    }
}

trait SchedulingUserActions extends CommonUserActions
{
    this : MyActor =>

    def getSurgeriesSuggestions(startTime : LocalTime, endTime : LocalTime, date : LocalDate)
    {
        m_controller ! GetOptionsForFreeBlockWork(startTime, endTime, date)
    }
    
    def getCurrentSchedule(from : LocalDate, to : LocalDate)
    {
        m_controller ! GetCurrentScheduleWork(from, to)
    }
    
    def loadScheduleListener(file : File, keepOldMapping : Boolean)
    {
        m_controller ! ReadFutureSurgeriesExcelWork(file, keepOldMapping)
    }
}
