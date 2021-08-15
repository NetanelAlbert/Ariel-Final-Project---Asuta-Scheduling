package view.schduling.windowElements

import model.DTOs.FutureSurgeryInfo
import org.joda.time.{LocalDate, LocalTime}
import org.joda.time.format.DateTimeFormat
import scalafx.beans.property.ObjectProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{TableColumn, TableView}
import scalafx.stage.Screen

class DailyTableView(futureSurgeryInfo : Iterable[FutureSurgeryInfo]) extends TableView[LocalTime]
{
    private var today = LocalDate.now()
    val format = DateTimeFormat.forPattern("HH:mm")
    val dayStart = format.parseLocalTime("07:00")
    val dayEnd = format.parseLocalTime("23:00")
    val dayLength = dayEnd.getHourOfDay - dayStart.getHourOfDay
    val workingHours = (for(hour <- 0 to dayLength) yield dayStart.plusHours(hour)).toList
    items = ObservableBuffer.empty[LocalTime] ++= workingHours
    columns ++= schedulingColumns(4, roomColumnMapper)
    println(s"today = $today")
    
    //    onMouseClicked = e =>
//    {
//        if (e.getClickCount == 2){
//            val doctorStatistics = getSelectionModel.getSelectedItem
//            showDetailsDialog(doctorStatistics)
//        }
//    }
    
    // Dimensions settings
    columns.foreach(_.setPrefWidth(Screen.primary.bounds.width / columns.size))
    prefHeight = Screen.primary.bounds.height
    this.setFixedCellSize((Screen.primary.bounds.height - 20)/workingHours.size)
    
    def roomColumnMapper(time : LocalTime, room : Int) : Option[FutureSurgeryInfo] =
    {
        val ans = futureSurgeryInfo.find(surgInfo =>
        {
           surgInfo.operationRoom == room &&
           surgInfo.surgeryStartTime.toLocalDate == today &&
           surgInfo.surgeryStartTime.toLocalTime.isAfter(time) &&
           surgInfo.surgeryStartTime.toLocalTime.isBefore(time.plusHours(1))
           // TODO change last 2 conditions
        })
//        println(s"""
//              |surgInfo.operationRoom == room d d d  : ${surgInfo.operationRoom == room}
//              |surgInfo.surgeryStartTime.toLocalDate == today f  :${surgInfo.surgeryStartTime.toLocalDate == today f  : $
//}
//              |surgInfo.surgeryStartTime.toLocalTime.isAfter(time) f :${surgInfo.surgeryStartTime.toLocalTime.isAfter(time) f}
//              |surgInfo.surgeryStartTime.toLocalTime.isBefore(time.plusHours(1)) :${surgInfo.surgeryStartTime.toLocalTime.isBefore(time.plusHours}
//              |""".stripMargin)
        if(ans.isEmpty) println(s"Didn't find surgery in room $room in time ${time.toString("HH:mm")}")
        ans
    }
    
    def nextDay
    {
        today = today.plusDays(1)
        this.refresh()
    }
    
    def dayBefore
    {
        today = today.plusDays(-1)
        this.refresh()
    }
    
    def date_=(date : LocalDate)
    {
        today = date
        this.refresh()
    }
    
    def schedulingColumns(numOfOperationRooms : Int, getSurgeryByTimeAndRoom : (LocalTime, Int) => Option[FutureSurgeryInfo]) : List[javafx.scene.control.TableColumn[LocalTime, _]] =
    {
        val hourCol = new TableColumn[LocalTime, String](ColumnsNames.HOUR)
        hourCol.cellValueFactory = sdf => ObjectProperty(sdf.value.toString("HH:mm"))
        val roomsCols = (1 to numOfOperationRooms).map(RoomColumn).toList
        roomsCols.foreach(column => column.cellValueFactory = sdf =>
        {
            ObjectProperty(getSurgeryByTimeAndRoom(sdf.value, column.roomNum))
        })
        val col1 = RoomColumn(1)
        col1.cellValueFactory = sdf =>
        {
            ObjectProperty(getSurgeryByTimeAndRoom(sdf.value, col1.roomNum))
        }
        val col2 = RoomColumn(2)
        col2.cellValueFactory = sdf =>
        {
            ObjectProperty(getSurgeryByTimeAndRoom(sdf.value, col2.roomNum))
        }
        List[javafx.scene.control.TableColumn[LocalTime, _]](hourCol, col1, col2) // ++ roomsCols
    }
    
    case class RoomColumn(roomNum : Int) extends TableColumn[LocalTime, Option[FutureSurgeryInfo]](ColumnsNames.roomWithNumber(roomNum))
}
