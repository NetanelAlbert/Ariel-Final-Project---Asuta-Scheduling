package model.database

import model.DTOs.DoctorAvailability
import slick.jdbc.HsqldbProfile.api._
import slick.jdbc.HsqldbProfile.backend.DatabaseDef
import slick.lifted.ProvenShape.proveShapeOf
import slick.lifted.Tag

import scala.concurrent.{ExecutionContext, Future}


class DoctorAvailabilitySchema(tag: Tag) extends Table[DoctorAvailability](tag, "DoctorAvailability")
{
    def doctorId = column[Int]("doctorId")
    def day = column[Int]("day")
    //def start = column[Int]("start")
    //def end = column[Int]("end")
    
    def columns = (
        doctorId,
        day)
//        start,
//        end)
    
    override def * = columns.mapTo[DoctorAvailability]
}

class DoctorAvailabilityTable(m_db : DatabaseDef)(implicit ec : ExecutionContext) extends TableQuery(new DoctorAvailabilitySchema(_))
{
    def create() : Future[Unit] =
    {
        m_db.run(this.schema.createIfNotExists)
    }
    
    def insert(doctorAvailability : DoctorAvailability) : Future[Int] =
    {
        m_db.run(this += doctorAvailability)
    }
    
    def insertIfNotExist(doctorAvailability : DoctorAvailability) : Future[Int] =
    {
        val DoctorAvailability(doctorId : Int, day : Int) = doctorAvailability
        for
        {
            exist <- m_db.run(this.filter(row => row.doctorId === doctorId && row.day === day).result.headOption)
            insert <- if(exist.isEmpty) m_db.run(this += doctorAvailability) else Future.successful(0)
        } yield insert
    }
    
    def deleteRow(doctorAvailability : DoctorAvailability) : Future[Int] =
    {
        val DoctorAvailability(doctorId : Int, day : Int) = doctorAvailability
        m_db.run(this.filter(row => row.doctorId === doctorId && row.day === day).delete)
    }
    
    def insertAll(doctorIdToDaysMap : Set[DoctorAvailability]) : Future[Int] =
    {
        val insertFutureSeq = doctorIdToDaysMap.toSeq.map(this += _)
        
        m_db.run(DBIO.sequence(insertFutureSeq)).map(_.sum)
    }
    
    def selectAll() : Future[Seq[DoctorAvailability]] =
    {
        m_db.run(this.result)
    }
    
    def clear() : Future[Int] =
    {
        m_db.run(this.delete)
    }
    
    def getAvailableDoctorsIDs(day : Int) : Future[Seq[Int]] =
    {
        m_db.run(this.filter(_.day === day)
                     .map(_.doctorId)
                     .result)
    }
    
//    private def getAvailableDoctorsWithSurgeries(dayOfTheWeek : Int, surgeryAvgInfoByDoctorTable : SurgeryAvgInfoByDoctorTable) =
//    {
//        this.filter(_.day === dayOfTheWeek).map(_.doctorId) joinLeft
//        surgeryAvgInfoByDoctorTable.map(row => (row.doctorId, row.operationCode))
//
//        val query = for
//        {
//            (_, surgeryAvgInfoByDoctor) <- this.filter(_.day === dayOfTheWeek) joinLeft surgeryAvgInfoByDoctorTable on (_.doctorId === _.doctorId)
//        } yield surgeryAvgInfoByDoctor
//
//        m_db.run(query.)
//    }
}
