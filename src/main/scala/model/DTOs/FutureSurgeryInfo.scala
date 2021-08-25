package model.DTOs

import org.joda.time.{LocalDate, LocalDateTime, LocalTime}

case class FutureSurgeryInfo
(
    operationCode : Double, //3
    doctorId : Int, //2
    plannedStart : LocalDateTime, //10
    operationRoom : Int, //13
    blockStart : LocalTime, //27
    blockEnd : LocalTime, //28
    released : Boolean, //29
)

