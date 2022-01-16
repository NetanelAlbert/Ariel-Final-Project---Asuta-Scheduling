package model.database

import model.DTOs.{OperationCodeAndName, SurgeryStatistics}
import model.probability.IntegerDistribution
import slick.jdbc.HsqldbProfile.api._
import slick.jdbc.HsqldbProfile.backend.DatabaseDef
import slick.lifted.ProvenShape.proveShapeOf
import slick.lifted.Tag

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


class SurgeryStatisticsSchema(tag: Tag) extends Table[SurgeryStatistics](tag, "SurgeryStatistics")
{
    import model.DTOs.FormattingProtocols._
    
    
    def operationCode = column[Double]("operationCode", O.PrimaryKey)
    
    def operationName  = column[Option[String]]("operationName")

    def restingDistribution = column[IntegerDistribution]("restingDistribution")
    
    def hospitalizationDistribution = column[IntegerDistribution]("hospitalizationDistribution")

    def profit = column[Option[Int]]("profit")
    
    def amountOfData = column[Int]("amountOfData")

    
    def columns = (
        operationCode,
        operationName,
        restingDistribution,
        hospitalizationDistribution,
        profit,
        amountOfData
    )
    
    override def * = columns.mapTo[SurgeryStatistics]
}

class SurgeryStatisticsTable(m_db : DatabaseDef)(implicit ec : ExecutionContext) extends TableQuery(new SurgeryStatisticsSchema(_)) with BaseDB[SurgeryStatistics]
{
    def create() : Future[Unit] =
    {
        m_db.run(this.schema.createIfNotExists)
    }
    
    def insert(element : SurgeryStatistics) : Future[Int] =
    {
        m_db.run(this += element)
    }
    
    def insertAll(elements : Iterable[SurgeryStatistics]) : Future[Option[Int]] =
    {
        m_db.run(this ++= elements)
    }
    
    def selectAll() : Future[Seq[SurgeryStatistics]] =
    {
        m_db.run(this.result)
    }
    
    def getSurgeryMapping() : Future[Map[Double, String]] =
    {
        m_db.run(this.map(row => (row.operationCode, row.operationName)).result)
            .map(_.filter(_._2.nonEmpty).toMap.mapValues(_.get))
    }
   
    def getProfitMapping() : Future[Map[Double, Int]] =
    {
        m_db.run(this.map(row => (row.operationCode, row.profit)).result)
            .map(_.filter(_._2.nonEmpty).toMap.mapValues(_.get))
    }
    
    def setSurgeryNames(surgeryMapping : Map[Double, String]) : Future[Int] =
    {
        val updates = surgeryMapping.map
        {
            case (operationCode, operationName) =>
            {
                this.filter(_.operationCode === operationCode)
                    .map(_.operationName).update(Some(operationName))
            }
        }
        
        m_db.run(DBIO.sequence(updates)).map(_.sum)
    }
    
    def setSurgeriesProfit(surgeriesProfit : Iterable[(Double, Int)]) : Future[Int] =
    {
        val updates = surgeriesProfit.map
        {
            case (operationCode, profit) =>
            {
                this.filter(_.operationCode === operationCode)
                    .map(_.profit).update(Some(profit))
            }
        }
        
        m_db.run(DBIO.sequence(updates)).map(_.sum)
    }
    
    def getOperationCodeAndNames() : Future[Seq[OperationCodeAndName]] =
    {
        val pairsSeqFuture = m_db.run(this.map(row => (row.operationCode, row.operationName)).result)
        pairsSeqFuture.map(_.map(OperationCodeAndName.tupled))
    }
    
    def getByIDsAndValidateSize(ids : Iterable[Double]) : Future[Seq[SurgeryStatistics]] =
    {
        val idsSet = ids.toSet
        m_db.run(this.filter(_.operationCode inSet idsSet).result).transform
        {
            case Success(statistics) if statistics.size != idsSet.size =>
            {
                Failure(new Exception(s"Didn't find all SurgeryStatistics for the requested IDs in DB. requested: $idsSet, found: ${statistics.map(_.operationCode)}"))
            }
            case other => other
        }
    }
    
    def clear() : Future[Int] =
    {
        m_db.run(this.delete)
    }
}
