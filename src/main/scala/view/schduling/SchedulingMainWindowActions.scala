package view.schduling

import model.DTOs._
import org.joda.time.{LocalDate, LocalTime}
import view.common.MainWindowActions
import view.mangerStatistics.StatisticsUserActions
import work.{BlockFillingOption, GetOptionsForFreeBlockWork}

trait SchedulingMainWindowActions extends MainWindowActions
{
    def initializeWithData(futureSurgeryInfo : Iterable[FutureSurgeryInfo], blocks : Map[LocalDate, Set[Block]], userActions : SchedulingUserActions)
    
    def showOptionsForFreeBlock(work : GetOptionsForFreeBlockWork)
}
