package model.database

import model.DTOs.DoctorStatistics
import slick.jdbc.HsqldbProfile.api._
import slick.lifted.ProvenShape.proveShapeOf
import slick.lifted.Tag
import slick.jdbc.HsqldbProfile.backend.DatabaseDef

import scala.concurrent.{ExecutionContext, Future}



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
    
    def globalAvg = column[Double]("globalAvg")
    
    def columns = (
        id,
        name,
        amountOfData,
        profit,
        surgeryDurationAvgMinutes,
        restingDurationAvgMinutes,
        hospitalizationDurationAvgHours,
        globalAvg)
    
    override def * = columns.mapTo[DoctorStatistics]
}

class DoctorStatisticsTable(m_db : DatabaseDef)(implicit ec : ExecutionContext) extends TableQuery(new DoctorStatisticsSchema(_)) with BaseDB[DoctorStatistics]
{
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
        
        m_db.run(DBIO.sequence(updates)).map(_.sum)
    }
    
    def clear() : Future[Int] =
    {
        m_db.run(this.delete)
    }
}
