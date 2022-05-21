package work

import common.Utils
import model.DTOs.Priority.Priority
import model.DTOs.{Block, DoctorStatistics, FutureSurgeryInfo, OperationCodeAndName, Priority, Settings, SurgeryAvgInfo, SurgeryAvgInfoByDoctor, SurgeryBasicInfo, SurgeryStatistics}
import model.probability.IntegerDistribution
import org.joda.time.{LocalDate, LocalTime}
import view.common.UiUtils

import java.util.Date

trait GetDataWork extends Work

case class GetDoctorsStatisticsWork
(
    doctorsBaseStatistics : Option[Seq[DoctorStatistics]] = None,
    surgeryAvgInfoByDoctorMap : Option[Map[Int, Seq[SurgeryAvgInfoByDoctor]]] = None,
    surgeryAvgInfoList : Option[Seq[SurgeryAvgInfo]] = None,
    operationCodeAndNames : Option[Seq[OperationCodeAndName]] = None
) extends GetDataWork

case class GetCurrentScheduleWork(from : LocalDate,
                                  to : LocalDate,
                                  schedule : Option[Seq[FutureSurgeryInfo]] = None,
                                  blocks : Option[Map[LocalDate, Set[Block]]] = None,
                                 ) extends GetDataWork

case class GetOptionsForFreeBlockWork
(
    startTime : LocalTime,
    endTime : LocalTime,
    date : LocalDate,
    doctorsWithSurgeries : Option[Map[Int, Seq[SurgeryAvgInfoByDoctor]]] = None,
    doctorMapping : Option[Map[Int, String]] = None,
    surgeryStatistics: Option[Seq[SurgeryStatistics]] = None,
    plannedSurgeriesAvgInfo: Option[Seq[SurgeryAvgInfo]] = None,
    plannedSurgeries : Option[Seq[FutureSurgeryInfo]] = None,
    plannedSurgeryStatistics : Option[Seq[SurgeryStatistics]] = None,
    doctorsPriorityMap : Option[Map[Int, Priority]] = None,
    topOptions : Option[Seq[BlockFillingOption]] = None,
) extends GetDataWork

case class BlockFillingOption
(
    doctorId : Int,
    doctorName : Option[String],
    surgeries : Seq[SurgeryBasicInfo],
    chanceForRestingShort : Double,
    chanceForHospitalizeShort : Double,
    expectedProfit : Option[Int],
    doctorPriority : Priority,
)
{
    require(0 <= chanceForRestingShort && chanceForRestingShort <= 1, s"chanceForRestingShort must be in [0-1], but it's: $chanceForRestingShort")
    require(0 <= chanceForHospitalizeShort && chanceForHospitalizeShort <= 1, s"chanceForHospitalizeShort must be in [0-1], but its: $chanceForHospitalizeShort")
    
    def nameOrID = doctorName.getOrElse(s"$doctorId (id)")
    
    var m_totalScoreCache : Option[Int] = None
    def totalScoreCache = m_totalScoreCache
    def totalScoreCache_=(newTotalScore : Int) : Unit = m_totalScoreCache = Some(newTotalScore)
    def getTotalScore(settings : Settings, profitNormalizer : Int => Double) : Int =
    {
        Utils.cache(totalScoreCache, totalScoreCache_=)
        {
            val weightedResting = (1 - this.chanceForRestingShort) * settings.blockOptionsRestingShortWeight
            val weightedHospitalize = (1 - this.chanceForHospitalizeShort) * settings.blockOptionsHospitalizeShortWeight
    
            val normalizedScore = this.expectedProfit match
            {
                case Some(expectedProfit) =>
                {
                    val weightedProfit = profitNormalizer(expectedProfit) * settings.blockOptionsProfitWeight
                    val weightedSum = weightedResting + weightedHospitalize + weightedProfit
                    val div = settings.blockOptionsRestingShortWeight + settings.blockOptionsHospitalizeShortWeight + settings.blockOptionsProfitWeight
                    weightedSum / div
                }
                
                case None =>
                {
                    val weightedSum = weightedResting + weightedHospitalize
                    val div = settings.blockOptionsRestingShortWeight + settings.blockOptionsHospitalizeShortWeight
                    weightedSum / div
                }
            }
            UiUtils.doubleToPercent(normalizedScore)
        }
    }
}

