package model.database

import akka.Done
import model.DTOs.{Settings, SettingsObject}
import slick.jdbc.HsqldbProfile.api._
import slick.jdbc.HsqldbProfile.backend.DatabaseDef
import slick.lifted.ProvenShape.proveShapeOf
import slick.lifted.Tag

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}


class SettingsSchema(tag: Tag) extends Table[Settings](tag, "Settings")
{
    def doctorRankingProfitWeight = column[Int]("doctorRankingProfitWeight")
    def doctorRankingSurgeryTimeWeight = column[Int]("doctorRankingSurgeryTimeWeight")
    def doctorRankingRestingTimeWeight = column[Int]("doctorRankingRestingTimeWeight")
    def doctorRankingHospitalizationTimeWeight = column[Int]("doctorRankingHospitalizationTimeWeight")
    
    def blockOptionsRestingShortWeight = column[Int]("blockOptionsRestingShortWeight")
    def blockOptionsHospitalizeShortWeight = column[Int]("blockOptionsHospitalizeShortWeight")
    def blockOptionsProfitWeight = column[Int]("blockOptionsProfitWeight")
    
    def shortSurgeryPrepareTimeMinutes = column[Int]("shortSurgeryPrepareTimeMinutes")
    def longSurgeryPrepareTimeMinutes = column[Int]("longSurgeryPrepareTimeMinutes")
    def longSurgeryDefinitionMinutes = column[Int]("longSurgeryDefinitionMinutes")
    def totalNumberOfRestingBeds = column[Int]("totalNumberOfRestingBeds")
    def totalNumberOfHospitalizeBeds = column[Int]("totalNumberOfHospitalizeBeds")
    def numberOfOperationRooms = column[Int]("numberOfOperationRooms")
    def distributionMaxLength = column[Int]("distributionMaxLength")
    def numberOfPointsToLookForShortage = column[Int]("numberOfPointsToLookForShortage")
    def doctorAvailabilityMonthsToGoBack = column[Int]("doctorAvailabilityMonthsToGoBack")
    def SurgeriesForBedCalculationDaysBefore = column[Int]("SurgeriesForBedCalculationDaysBefore")
    def SurgeriesForBedCalculationDaysAfter = column[Int]("SurgeriesForBedCalculationDaysAfter")
    
    def avgSurgeryProfit = column[Option[Int]]("avgSurgeryProfit")
    def avgDoctorProfit = column[Option[Int]]("avgDoctorProfit")
    
    def columns = (
        doctorRankingProfitWeight,
        doctorRankingSurgeryTimeWeight,
        doctorRankingRestingTimeWeight,
        doctorRankingHospitalizationTimeWeight,
    
        blockOptionsRestingShortWeight,
        blockOptionsHospitalizeShortWeight,
        blockOptionsProfitWeight,
        
        shortSurgeryPrepareTimeMinutes,
        longSurgeryPrepareTimeMinutes,
        longSurgeryDefinitionMinutes,
        totalNumberOfRestingBeds,
        totalNumberOfHospitalizeBeds,
        numberOfOperationRooms,
        distributionMaxLength,
        numberOfPointsToLookForShortage,
        doctorAvailabilityMonthsToGoBack,
        SurgeriesForBedCalculationDaysBefore,
        SurgeriesForBedCalculationDaysAfter,
        avgSurgeryProfit,
        avgDoctorProfit,
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
            exist <- m_db.run(this.exists.result)
            _ <- if (! exist) m_db.run(this += SettingsObject.default) else Future.successful()
        } yield Done
    }
    
    Await.result(create(), 10 second)
    
    def get : Future[Settings] =
    {
        m_db.run(this.result.head)
    }
    
    def set(settings : Settings) : Future[Done] =
    {
        val query = Seq(
            this.delete,
            this += settings)
        m_db.run(DBIO.sequence(query)).map(_ => Done)
    }
    
    def clear() : Future[Int] =
    {
        m_db.run(this.delete)
    }
}
