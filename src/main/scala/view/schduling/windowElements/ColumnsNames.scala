package view.schduling.windowElements

object ColumnsNames
{
    val HOUR = "Hour"
    val ROOM_BASE = "Room "
    def roomWithNumber(num : Int) = ROOM_BASE + num
    
    def getColumnsNames(rooms : Int) = List(HOUR) ++ (1 to rooms).map(roomWithNumber)
}
