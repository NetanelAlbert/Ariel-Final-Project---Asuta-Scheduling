package view.schduling.windowElements

import model.DTOs.{DoctorStatistics, FutureSurgeryInfo}
import org.joda.time.LocalTime
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.scene.control.TableColumn

class SchedulingColumns(numOfOperationRooms : Int, getSurgeryByTimeAndRoom : (LocalTime, Int) => Option[FutureSurgeryInfo])
{
    val hourCol = new TableColumn[LocalTime, String](ColumnsNames.HOUR)
    hourCol.cellValueFactory = sdf => ObjectProperty(sdf.value.toString("HH:mm"))
    val roomsCols = (1 to numOfOperationRooms).map(RoomColumn).toList
    roomsCols.foreach(column => column.cellValueFactory = sdf =>
                      {
                          ObjectProperty(getSurgeryByTimeAndRoom(sdf.value, column.roomNum))
                      })
    
    val columns = List[javafx.scene.control.TableColumn[LocalTime, _]](hourCol, RoomColumn(1), RoomColumn(2)) // ++ roomsCols
}

case class RoomColumn(roomNum : Int) extends TableColumn[LocalTime, Option[FutureSurgeryInfo]](ColumnsNames.roomWithNumber(roomNum))

object ColumnsNames
{
    val HOUR = "Hour"
    val ROOM_BASE = "Room Number "
    def roomWithNumber(num : Int) = ROOM_BASE + num
}
