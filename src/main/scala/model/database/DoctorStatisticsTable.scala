package model.database

import model.DTOs.{DoctorStatistics, Priority}
import model.DTOs.Priority.Priority
import slick.jdbc.HsqldbProfile.api._
import slick.lifted.ProvenShape.proveShapeOf
import slick.lifted.Tag
import slick.jdbc.HsqldbProfile.backend.DatabaseDef

import scala.concurrent.{ExecutionContext, Future}

case class DoctorPriorityAndName(id : Int, priority : Priority, nameOption : Option[String])
{
    override def toString = s"${nameOption.getOrElse("")} ($id)"
}

class DoctorStatisticsSchema(tag: Tag) extends Table[DoctorStatistics](tag, "DoctorStatistics")
{
    import model.DTOs.FormattingProtocols._
    
    def id = column[Int]("id", O.PrimaryKey)
    
    def name = column[Option[String]]("name")
    
    def amountOfData = column[Int]("amountOfData")
    
    def profit = column[Option[Int]]("profit")
    
    def surgeryDurationAvgMinutes = column[Double]("surgeryDurationAvgMinutes")
    
    def restingDurationAvgMinutes = column[Double]("restingDurationAvgMinutes")
    
    def hospitalizationDurationAvgHours = column[Double]("hospitalizationDurationAvgHours")
    
    def priority = column[Priority]("priority")
    
    def columns = (
        id,
        name,
        amountOfData,
        profit,
        surgeryDurationAvgMinutes,
        restingDurationAvgMinutes,
        hospitalizationDurationAvgHours,
        priority,
        )
    
    override def * = columns.mapTo[DoctorStatistics]
}

class DoctorStatisticsTable(m_db : DatabaseDef)(implicit ec : ExecutionContext) extends TableQuery(new DoctorStatisticsSchema(_)) with BaseDB[DoctorStatistics]
{
    import model.DTOs.FormattingProtocols._
    
    def create() : Future[Unit] =
    {
        m_db.run(this.schema.createIfNotExists)
    }
    
    def insert(element : DoctorStatistics) : Future[Int] =
    {
        m_db.run(this += element)
    }
    
    def insertAll(elements : Iterable[DoctorStatistics]) : Future[Option[Int]] =
    {
        m_db.run(this ++= elements)
    }
    
    def selectAll() : Future[Seq[DoctorStatistics]] =
    {
        m_db.run(this.result)
    }
    
    def getDoctorMapping() : Future[Map[Int, String]] =
    {
        m_db.run(this.map(row => (row.id, row.name)).result)
            .map(_.filter(_._2.nonEmpty).toMap.mapValues(_.get))
    }
    
    def getDoctorMapping(ids : Iterable[Int]) : Future[Map[Int, String]] =
    {
        m_db.run(this.filter(_.id inSet ids).map(row => (row.id, row.name)).result)
            .map(_.filter(_._2.nonEmpty).toMap.mapValues(_.get))
    }
    
    def setDoctorNames(doctorMapping : Map[Int, String]) : Future[Int] =
    {
        val updates = doctorMapping.map
        {
            case (id, name) =>
            {
                this.filter(_.id === id).map(_.name).update(Some(name))
            }
        }
        
        m_db.run(DBIO.sequence(updates)).map(_.sum)
    }
    
    def setDoctorsProfit(doctorsProfit : Iterable[(Int, Int)]) : Future[Int] =
    {
        val updates = doctorsProfit.map
        {
            case (id, profit) =>
            {
                this.filter(_.id === id).map(_.profit).update(Some(profit))
            }
        }
        
        m_db.run(DBIO.sequence(updates).transactionally).map(_.sum)
    }
    
    def setDoctorsPriority(doctorsPriority : Iterable[(Int, Priority)]) : Future[Int] =
    {
        val updates = doctorsPriority.map
        {
            case (id, priority) =>
            {
                this.filter(_.id === id).map(_.priority).update(priority)
            }
        }
    
        m_db.run(DBIO.sequence(updates).transactionally).map(_.sum)
    }
    
    def getDoctorsPriority() : Future[Map[Int, Priority]] =
    {
        val query = this.map(row => (row.id, row.priority)).result
        m_db.run(query).map(_.toMap)
    }
    
    def getDoctorsIDsWithNotHiddenPriority() : Future[Seq[Int]] =
    {
        val query = this.filter(_.priority inSet Set(Priority.STAR, Priority.NORMAL)).map(_.id).result
        m_db.run(query)
    }
    
    def getDoctorsPriorityAndNames() : Future[Seq[DoctorPriorityAndName]] =
    {
        val query = this.map(row => (row.id, row.priority, row.name)).result
        m_db.run(query).map(_.map(DoctorPriorityAndName.tupled))
    }
    
    def clear() : Future[Int] =
    {
        m_db.run(this.delete)
    }
}
