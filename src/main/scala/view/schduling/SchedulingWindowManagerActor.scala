package view.schduling

import akka.actor.{Actor, ActorRef, Props}
import model.actors.MyActor
import org.joda.time.{LocalDate, LocalTime}
import view.common.{CommonUserActions, MainWindowActions}
import work.{FileWork, GetCurrentScheduleWork, GetOptionsForFreeBlockWork, ReadFutureSurgeriesExcelWork, WorkFailure, WorkSuccess}

import java.io.File
import scala.concurrent.{ExecutionContext, Future}

object SchedulingWindowManagerActor
{
    def props(m_controller : ActorRef, mainWindow : SchedulingMainWindowActions)(implicit ec : ExecutionContext) : Props = Props(new SchedulingWindowManagerActor(m_controller, mainWindow))
}

class SchedulingWindowManagerActor(override val m_controller : ActorRef, override val mainWindow : SchedulingMainWindowActions)(implicit override val ec : ExecutionContext) extends MyActor with SchedulingUserActions
{
    reloadDefaultData
    
    override def receive =
    {
        case WorkSuccess(work @ GetOptionsForFreeBlockWork(startTime, endTime, date, _, _, _, _, _, _, Some(topOptions)), _) => mainWindow.showOptionsForFreeBlock(startTime, endTime, date, topOptions)

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
