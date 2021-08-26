package model.DTOs

import common.Utils.timeFormat
import org.joda.time.{LocalDate, LocalTime}

case class Block(
    doctorId : Int,
    doctorName : Option[String],
    operationRoom : Int,
    day : LocalDate,
    blockStart : LocalTime,
    blockEnd : LocalTime,
)
{
    def prettyString = s"${doctorName.getOrElse(s"$doctorId (Doctor ID)")} ${blockStart.toString(timeFormat)} - ${blockEnd.toString(timeFormat)}"
    
    def prettyStringMultiLine =
        s"""${doctorName.getOrElse(s"$doctorId (Doctor ID)")}
           |${blockStart.toString(timeFormat)} - ${blockEnd.toString(timeFormat)}""".stripMargin
    
    def prettyStringByLines(lines : Int) = if(lines > 1) prettyStringMultiLine else prettyString
}

object Block
{
    def fromFutureSurgery(futureSurgeryInfo : FutureSurgeryInfo, doctorName : Option[String]) : Block =
    {
        val FutureSurgeryInfo(_ , doctorId , plannedStart , operationRoom , blockStart , blockEnd , _) = futureSurgeryInfo

        Block(doctorId, doctorName, operationRoom, plannedStart.toLocalDate, blockStart, blockEnd)
    }
}
