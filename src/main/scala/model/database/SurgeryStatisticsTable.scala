package model.database

import model.DTOs.SurgeryStatistics
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution
import slick.jdbc.HsqldbProfile.api._
import slick.jdbc.HsqldbProfile.backend.DatabaseDef
import slick.lifted.ProvenShape.proveShapeOf
import slick.lifted.Tag

import scala.concurrent.Future


class SurgeryStatisticsSchema(tag: Tag) extends Table[SurgeryStatistics](tag, "SurgeryStatistics")
{
    import model.DTOs.FormattingProtocols._
    
    
    def operationCode = column[Double]("operationCode", O.PrimaryKey)

    def restingDistribution = column[EnumeratedIntegerDistribution]("restingDistribution")
    
    def hospitalizationDistribution = column[EnumeratedIntegerDistribution]("hospitalizationDistribution")

    def profit = column[Double]("profit")

    
    def columns = (
        operationCode,
        restingDistribution,
        hospitalizationDistribution,
        profit
    )
    
    override def * = columns.mapTo[SurgeryStatistics]
}

class SurgeryStatisticsTable(m_db : DatabaseDef) extends TableQuery(new SurgeryStatisticsSchema(_))
{
    def create() : Future[Unit] =
    {
        m_db.run(this.schema.createIfNotExists)
    }
    
    def insert(element : SurgeryStatistics) : Future[Int] =
    {
        m_db.run(this += element)
    }
    
    def insertAll(elements : Seq[SurgeryStatistics])  =
    {
        m_db.run(this ++= elements)
    }
    
    def selectAll() : Future[Seq[SurgeryStatistics]] =
    {
        m_db.run(this.result)
    }
    
    def clear() : Future[Int] =
    {
        m_db.run(this.delete)
    }
}
