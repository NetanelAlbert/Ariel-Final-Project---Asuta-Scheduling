package model.database

import model.DTOs.DoctorStatistics
import slick.jdbc.HsqldbProfile.api._
import slick.lifted.ProvenShape.proveShapeOf
import slick.lifted.Tag
import slick.jdbc.HsqldbProfile.backend.DatabaseDef

import scala.concurrent.Future



class DoctorStatisticsSchema(tag: Tag) extends Table[DoctorStatistics](tag, "DoctorStatistics")
{
    
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
        globalAvg
    )
    
    override def * = columns.mapTo[DoctorStatistics]
}

class DoctorStatisticsTable(m_db : DatabaseDef) extends TableQuery(new DoctorStatisticsSchema(_))
{
    def create() : Future[Unit] =
    {
        m_db.run(this.schema.createIfNotExists)
    }
    
    def insert(element : DoctorStatistics) : Future[Int] =
    {
        m_db.run(this += element)
    }
    
    def insertAll(elements : Seq[DoctorStatistics])  =
    {
        m_db.run(this ++= elements)
    }
    
    def selectAll() : Future[Seq[DoctorStatistics]] =
    {
        m_db.run(this.result)
    }
    
    def clear() : Future[Int] =
    {
        m_db.run(this.delete)
    }
}
