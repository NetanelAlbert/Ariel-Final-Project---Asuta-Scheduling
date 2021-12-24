package model.DTOs

case class Settings(
    doctorRankingProfitWeight : Int,
    doctorRankingSurgeryTimeWeight : Int,
    doctorRankingRestingTimeWeight : Int,
    doctorRankingHospitalizationTimeWeight : Int,
    shortSurgeryPrepareTimeMinutes : Int,
    longSurgeryPrepareTimeMinutes : Int,
    longSurgeryDefinitionMinutes : Int,
    totalNumberOfRestingBeds : Int,
    totalNumberOfHospitalizeBeds : Int,
    numberOfOperationRooms : Int,
    distributionMaxLength : Int,
    numberOfPointsToLookForShortage : Int,
    doctorAvailabilityMonthsToGoBack : Int,
    surgeriesForBedCalculationDaysBefore : Int,
    surgeriesForBedCalculationDaysAfter : Int,
)

object SettingsObject
{
    def default : Settings =
    {
        Settings(
            doctorRankingProfitWeight = 1,
            doctorRankingSurgeryTimeWeight = 1,
            doctorRankingRestingTimeWeight = 1,
            doctorRankingHospitalizationTimeWeight = 1,
            shortSurgeryPrepareTimeMinutes = 15,
            longSurgeryPrepareTimeMinutes = 30,
            longSurgeryDefinitionMinutes = 2 * 60,
            totalNumberOfRestingBeds = 10,
            totalNumberOfHospitalizeBeds = 20,
            numberOfOperationRooms = 4,
            distributionMaxLength = 10,
            numberOfPointsToLookForShortage = 5,
            doctorAvailabilityMonthsToGoBack = 36,
            surgeriesForBedCalculationDaysBefore = 7,
            surgeriesForBedCalculationDaysAfter = 7,
            )
    }
}
