package model.database

import model.DTOs.SurgeryAvgInfo
import slick.jdbc.HsqldbProfile.api._
import slick.jdbc.HsqldbProfile.backend.DatabaseDef
import slick.lifted.ProvenShape.proveShapeOf
import slick.lifted.Tag

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


class SurgeryAvgInfoSchema(tag: Tag) extends Table[SurgeryAvgInfo](tag, "SurgeryAvgInfo")
{
    
    def operationCode = column[Double]("operationCode")
    
    def amountOfData = column[Int]("amountOfData")
    
    def surgeryDurationAvgMinutes = column[Double]("surgeryDurationAvgMinutes")
    
    def restingDurationAvgMinutes = column[Double]("restingDurationAvgMinutes")
    
    def hospitalizationDurationAvgHours = column[Double]("hospitalizationDurationAvgHours")
    
    def columns = (
        operationCode,
        amountOfData,
        surgeryDurationAvgMinutes,
        restingDurationAvgMinutes,
        hospitalizationDurationAvgHours,
    )
    
    override def * = columns.mapTo[SurgeryAvgInfo]
}

class SurgeryAvgInfoTable(m_db : DatabaseDef)(implicit ec : ExecutionContext) extends TableQuery(new SurgeryAvgInfoSchema(_)) with BaseDB[SurgeryAvgInfo]
{
    def create() : Future[Unit] =
    {
        m_db.run(this.schema.createIfNotExists)
    }
    
    def insert(element : SurgeryAvgInfo) : Future[Int] =
    {
        m_db.run(this += element)
    }
    
    def insertAll(elements : Iterable[SurgeryAvgInfo]) : Future[Option[Int]] =
    {
        m_db.run(this ++= elements)
    }
    
    def selectAll() : Future[Seq[SurgeryAvgInfo]] =
    {
        m_db.run(this.result)
    }
    
    def getByIDsAndValidateSize(ids : Iterable[Double]) : Future[Seq[SurgeryAvgInfo]] =
    {
        val idsSet = ids.toSet
        m_db.run(this.filter(_.operationCode inSet ids).result).transform
        {
            case Success(statistics) if statistics.size != idsSet.size =>
            {
                Failure(new Exception(s"Didn't find all SurgeryAvgInfo for the requested IDs in DB. requested: $idsSet, found: ${statistics.map(_.operationCode)}"))
            }
            
            case other => other
        }
    }
    
    def clear() : Future[Int] =
    {
        m_db.run(this.delete)
    }
    
    def selectByOperationCode(opCode : Double) : Future[Option[SurgeryAvgInfo]] =
    {
        m_db.run(this.filter(_.operationCode === opCode).result.headOption)
    }
    
    def selectByOperationCodes(opCodes : Seq[Double]) : Future[Seq[SurgeryAvgInfo]] =
    {
        m_db.run(this.filter(_.operationCode inSet opCodes).result)
    }
}
