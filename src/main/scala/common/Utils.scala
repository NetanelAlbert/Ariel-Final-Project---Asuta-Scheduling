package common

import org.joda.time.format.DateTimeFormat

object Utils
{
    val dateTimeFormat = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm")
    val dateFormat = DateTimeFormat.forPattern("dd/MM/yyyy")
    val timeFormat = DateTimeFormat.forPattern("HH:mm")
    val roomsStart = 85000
}
