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
    
    def profitAvg = column[Double]("profitAvg")
    
    def surgeryDurationAvgMinutes = column[Double]("surgeryDurationAvgMinutes")
    
    def restingDurationAvgMinutes = column[Double]("restingDurationAvgMinutes")
    
    def hospitalizationDurationAvgHours = column[Double]("hospitalizationDurationAvgHours")
    
    def globalAvg = column[Double]("globalAvg")
    
    def columns = (
        id,
        name,
        amountOfData,
        profitAvg,
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
    
    def selectAll() : Future[Seq[DoctorStatistics]] =
    {
        m_db.run(this.result)
    }
    
    def getDoctorMapping() : Future[Map[Int, Option[String]]] =
    {
        m_db.run(this.map(row => (row.id, row.name)).result)
            .map(_.filter(_._2.nonEmpty).toMap)
    }
    
    def setDoctorNames(doctorMapping : Map[Int, Option[String]]) : Future[Int] =
    {
        val updates = doctorMapping.map
        {
            case (id, name) =>
            {
                this.filter(_.id === id).map(_.name).update(name)
            }
        }
        
        m_db.run(DBIO.sequence(updates)).map(_.sum)
    }
    
    def clear() : Future[Int] =
    {
        m_db.run(this.delete)
    }
}
