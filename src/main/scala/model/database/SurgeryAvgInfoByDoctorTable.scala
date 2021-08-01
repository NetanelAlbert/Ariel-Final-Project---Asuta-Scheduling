package model.database

import model.DTOs.SurgeryAvgInfoByDoctor
import slick.jdbc.HsqldbProfile.api._
import slick.jdbc.HsqldbProfile.backend.DatabaseDef
import slick.lifted.ProvenShape.proveShapeOf
import slick.lifted.Tag

import scala.concurrent.Future


class SurgeryAvgInfoByDoctorSchema(tag: Tag) extends Table[SurgeryAvgInfoByDoctor](tag, "SurgeryAvgInfoByDoctor")
{
    
    def operationCode = column[Double]("operationCode")
    
    def doctorId = column[Int]("doctorId")
    
    def amountOfData = column[Int]("amountOfData")
    
    def surgeryDurationAvgMinutes = column[Double]("surgeryDurationAvgMinutes")
    
    def restingDurationAvgMinutes = column[Double]("restingDurationAvgMinutes")
    
    def hospitalizationDurationAvgHours = column[Double]("hospitalizationDurationAvgHours")
    
    def columns = (
        operationCode,
        doctorId,
        amountOfData,
        surgeryDurationAvgMinutes,
        restingDurationAvgMinutes,
        hospitalizationDurationAvgHours,
    )
    
    override def * = columns.mapTo[SurgeryAvgInfoByDoctor]
}

class SurgeryAvgInfoByDoctorTable(m_db : DatabaseDef) extends TableQuery(new SurgeryAvgInfoByDoctorSchema(_)) with BaseDB[SurgeryAvgInfoByDoctor]
{
    def create() : Future[Unit] =
    {
        m_db.run(this.schema.createIfNotExists)
    }
    
    def insert(element : SurgeryAvgInfoByDoctor) : Future[Int] =
    {
        m_db.run(this += element)
    }
    
    def selectAll() : Future[Seq[SurgeryAvgInfoByDoctor]] =
    {
        m_db.run(this.result)
    }
    
    def clear() : Future[Int] =
    {
        m_db.run(this.delete)
    }
}
