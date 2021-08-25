package model.database

import model.DTOs.{Block, FutureSurgeryInfo, SurgeryAvgInfo}
import model.probability.IntegerDistribution
import org.joda.time.format.DateTimeFormat
import org.joda.time.{LocalDate, LocalDateTime, LocalTime}
import slick.jdbc.HsqldbProfile.api._
import slick.jdbc.HsqldbProfile.backend.DatabaseDef
import slick.lifted.ProvenShape.proveShapeOf
import slick.lifted.Tag
import common.Utils._

import scala.concurrent.{ExecutionContext, Future}


class ScheduleSchema(tag: Tag) extends Table[FutureSurgeryInfo](tag, "Schedule")
{
    implicit val LocalDateTimeMapping = MappedColumnType.base[LocalDateTime, String](_.toString(dateTimeFormat), dateTimeFormat.parseLocalDateTime)
    implicit val LocalTimeMapping = MappedColumnType.base[LocalTime, String](_.toString(timeFormat), timeFormat.parseLocalTime)
    
    def operationCode = column[Double]("operationCode")
    
    def doctorId = column[Int]("doctorId")
    
    def plannedStart = column[LocalDateTime]("plannedStart")
    
    def operationRoom = column[Int]("operationRoom")
    
    def blockStart = column[LocalTime]("blockStart")
    
    def blockEnd = column[LocalTime]("blockEnd")
    
    def released = column[Boolean]("released")
    
    
    def columns = (
        operationCode,
        doctorId,
        plannedStart,
        operationRoom,
        blockStart,
        blockEnd,
        released,
    )
    
    override def * = columns.mapTo[FutureSurgeryInfo]
}

class ScheduleTable(m_db : DatabaseDef)(implicit ex : ExecutionContext) extends TableQuery(new ScheduleSchema(_)) with BaseDB[FutureSurgeryInfo]
{
    def create() : Future[Unit] =
    {
        m_db.run(this.schema.createIfNotExists)
    }
    
    def insert(element : FutureSurgeryInfo) : Future[Int] =
    {
        m_db.run(this += element)
    }
    
    def insertAll(elements : Iterable[FutureSurgeryInfo]) : Future[Option[Int]] =
    {
        m_db.run(this ++= elements)
    }
    
    def selectAll() : Future[Seq[FutureSurgeryInfo]] =
    {
        m_db.run(this.result)
    }
    
    def selectByDates(from : LocalDate, to : LocalDate) : Future[Seq[FutureSurgeryInfo]] =
    {
        selectAll().map(_.filter(surg =>
                                 {
                                     ! surg.plannedStart.toLocalDate.isBefore(from) &&
                                     ! surg.plannedStart.toLocalDate.isAfter(to)
                                 }))
    }
    
    def selectBlocksByDates(from : LocalDate, to : LocalDate) : Future[Map[LocalDate, Set[Block]]] =
    {
        selectByDates(from, to).map(_.map(Block.fromFutureSurgery).toSet.groupBy(_.day))
    }
    
    def clear() : Future[Int] =
    {
        m_db.run(this.delete)
    }
}
