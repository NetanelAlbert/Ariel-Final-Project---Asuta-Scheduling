package model.DTOs

import org.joda.time.{LocalDate, LocalTime}

case class Block(
    doctorId : Int,
    operationRoom : Int,
    day : LocalDate,
    blockStart : LocalTime,
    blockEnd : LocalTime,
)

object Block
{
    def fromFutureSurgery(futureSurgeryInfo : FutureSurgeryInfo) : Block =
    {
        val FutureSurgeryInfo(operationCode , doctorId , plannedStart , operationRoom , blockStart , blockEnd , released) = futureSurgeryInfo

        Block(doctorId, operationRoom, plannedStart.toLocalDate, blockStart, blockEnd)
    }
}
