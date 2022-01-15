package view.schduling

import akka.Done
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import model.DTOs.Priority.Priority
import model.actors.MyActor
import org.joda.time.{LocalDate, LocalTime}
import view.common.{CommonUserActions, MainWindowActions, UiUtils}
import work.{BlockFillingOption, FileWork, GetCurrentScheduleWork, GetOptionsForFreeBlockWork, ReadFutureSurgeriesExcelWork, UpdateDoctorPriority, WorkFailure, WorkSuccess}

import java.io.File
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object SchedulingWindowManagerActor
{
    def props(m_controller : ActorRef, mainWindow : SchedulingMainWindowActions)(implicit ec : ExecutionContext) : Props = Props(new SchedulingWindowManagerActor(m_controller, mainWindow))
}

class SchedulingWindowManagerActor(override val m_controller : ActorRef, override val mainWindow : SchedulingMainWindowActions)(implicit override val ec : ExecutionContext) extends MyActor with SchedulingUserActions
{
    reloadDefaultData
    
    override def receive =
    {
        case WorkSuccess(work @ GetOptionsForFreeBlockWork(startTime, endTime, date, _, _, _, _, _, _, _, Some(topOptions)), _) => getOptionsForFreeBlockWorkSuccess(startTime, endTime, date, topOptions)

        case WorkSuccess(GetCurrentScheduleWork(_, _, Some(schedule), Some(blocks)), _) => mainWindow.initializeWithData(schedule, blocks, this) //TODO maybe just change data on second time an on

        
        
        case WorkSuccess(_ : FileWork, _) =>
        {
            mainWindow.hideProgressIndicator()
            mainWindow.askAndReloadData(this)
        }
        
        case WorkSuccess(work, message) =>
        {
            mainWindow.hideProgressIndicator()
            UiUtils.showSuccessDialog(message.getOrElse("Action succeed"))
        }

        case WorkFailure(work, cause, message) =>
        {
            mainWindow.hideProgressIndicator()
            UiUtils.showFailDialog(cause, message)
        }
    }
    
    override def reloadDefaultData
    {
        val now = LocalDate.now()
        getCurrentSchedule(now.minusDays(now.getDayOfWeek),
                           now.plusDays(6 - now.getDayOfWeek))
    }
    
    def updateDoctorPriority(doctorID : Int, priority : Priority) : Future[Done] =
    {
        m_settingActor ? UpdateDoctorPriority(doctorID, priority) map (_ => Done)
    }
    
    def getOptionsForFreeBlockWorkSuccess(
        startTime : LocalTime,
        endTime : LocalTime,
        date : LocalDate,
        topOptions : Seq[BlockFillingOption],
    )
    {
        mainWindow.hideProgressIndicator()
        getSettings.onComplete
        {
            case Success(settings) =>
            {
                mainWindow.showOptionsForFreeBlock(startTime, endTime, date, topOptions, settings, updateDoctorPriority)
            }
            case Failure(exception) =>
            {
                UiUtils.showFailDialog(Some(exception), Some("Failed to get setting, so options cannot be presented."))
            }
        }
    }
}

trait SchedulingUserActions extends CommonUserActions
{
    this : MyActor =>

    def getSurgeriesSuggestions(startTime : LocalTime, endTime : LocalTime, date : LocalDate)
    {
        m_controller ! GetOptionsForFreeBlockWork(startTime, endTime, date)
        mainWindow.showProgressIndicator("Get Options For Free Block")
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
