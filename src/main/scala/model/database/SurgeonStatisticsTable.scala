package model.database

import model.DTOs.SurgeonStatistics
import slick.jdbc.HsqldbProfile.api._
import slick.lifted.ProvenShape.proveShapeOf
import slick.lifted.Tag
import slick.jdbc.HsqldbProfile.backend.DatabaseDef

import scala.concurrent.Future



class SurgeonStatisticsSchema(tag: Tag) extends Table[SurgeonStatistics](tag, "SurgeonStatistics")
{
    
    def id = column[Int]("id", O.PrimaryKey)
    
    def name = column[String]("name")
    
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
    
    override def * = columns.mapTo[SurgeonStatistics]
}

class SurgeonStatisticsTable(m_db : DatabaseDef) extends TableQuery(new SurgeonStatisticsSchema(_))
{
    def create() : Future[Unit] =
    {
        m_db.run(this.schema.createIfNotExists)
    }
    
    def insert(element : SurgeonStatistics) : Future[Int] =
    {
        m_db.run(this += element)
    }
    
    def insertAll(elements : Seq[SurgeonStatistics])  =
    {
        m_db.run(this ++= elements)
    }
    
    def selectAll() : Future[Seq[SurgeonStatistics]] =
    {
        m_db.run(this.result)
    }
    
    def clear() : Future[Int] =
    {
        m_db.run(this.delete)
    }
}
