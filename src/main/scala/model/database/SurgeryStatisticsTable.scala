package model.database

import model.DTOs.SurgeryStatistics
import model.probability.IntegerDistribution
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
    
    def operationName  = column[Option[String]]("operationName")

    def restingDistribution = column[IntegerDistribution]("restingDistribution")
    
    def hospitalizationDistribution = column[IntegerDistribution]("hospitalizationDistribution")

    def profit = column[Option[Double]]("profit")

    
    def columns = (
        operationCode,
        operationName,
        restingDistribution,
        hospitalizationDistribution,
        profit
    )
    
    override def * = columns.mapTo[SurgeryStatistics]
}

class SurgeryStatisticsTable(m_db : DatabaseDef) extends TableQuery(new SurgeryStatisticsSchema(_)) with BaseDB[SurgeryStatistics]
{
    def create() : Future[Unit] =
    {
        m_db.run(this.schema.createIfNotExists)
    }
    
    def insert(element : SurgeryStatistics) : Future[Int] =
    {
        m_db.run(this += element)
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
