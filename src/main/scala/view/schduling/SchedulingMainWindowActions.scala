package view.schduling

import akka.Done
import model.DTOs.Priority.Priority
import model.DTOs._
import org.joda.time.{LocalDate, LocalTime}
import view.common.MainWindowActions
import view.mangerStatistics.StatisticsUserActions
import work.{BlockFillingOption, GetOptionsForFreeBlockWork}

import scala.concurrent.{ExecutionContext, Future}

trait SchedulingMainWindowActions extends MainWindowActions
{
    def initializeWithData(futureSurgeryInfo : Iterable[FutureSurgeryInfo], blocks : Map[LocalDate, Set[Block]], userActions : SchedulingUserActions)
    
    def showOptionsForFreeBlock(
        startTime : LocalTime,
        endTime : LocalTime,
        date : LocalDate,
        topOptions : Seq[BlockFillingOption],
        settings : Settings,
        updateDoctorPriority : (Int, Priority) => Future[Done],
    )(implicit ec : ExecutionContext)
}
