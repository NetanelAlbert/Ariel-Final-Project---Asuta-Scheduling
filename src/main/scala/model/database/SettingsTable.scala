package model.database

import akka.Done
import model.DTOs.Settings
import slick.jdbc.HsqldbProfile.api._
import slick.jdbc.HsqldbProfile.backend.DatabaseDef
import slick.lifted.ProvenShape.proveShapeOf
import slick.lifted.Tag

import scala.concurrent.{ExecutionContext, Future}


class SettingsSchema(tag: Tag) extends Table[Settings](tag, "Settings")
{
    def doctorRankingProfitWeight = column[Int]("doctorRankingProfitWeight")
    def doctorRankingSurgeryTimeWeight = column[Int]("doctorRankingSurgeryTimeWeight")
    def doctorRankingHospitalizationTimeWeight = column[Int]("doctorRankingHospitalizationTimeWeight")
    
    def columns = (
        doctorRankingProfitWeight,
        doctorRankingSurgeryTimeWeight,
        doctorRankingHospitalizationTimeWeight
        )

    override def * = columns.mapTo[Settings]
}

class SettingsTable(m_db : DatabaseDef)(implicit ec : ExecutionContext) extends TableQuery(new SettingsSchema(_))
{
    def create() : Future[Done] =
    {
        for
        {
            _ <- m_db.run(this.schema.createIfNotExists)
            exist <- m_db.run(this.result.headOption)
            _ <- if (exist.isEmpty) m_db.run(this += Settings()) else Future.successful()
        } yield Done
    }
    
    def get : Future[Settings] =
    {
        m_db.run(this.result.head)
    }
    
    def set(settings : Settings) : Future[Done] =
    {
        val query = Seq(
            this.delete,
            this += settings
            )
        m_db.run(DBIO.sequence(query)).map(_ => Done)
    }
    
    def clear() : Future[Int] =
    {
        m_db.run(this.delete)
    }
}
