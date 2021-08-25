package work

import model.DTOs.{Block, DoctorStatistics, FutureSurgeryInfo, OperationCodeAndName, SurgeryAvgInfo, SurgeryAvgInfoByDoctor, SurgeryBasicInfo, SurgeryStatistics}
import model.probability.IntegerDistribution
import org.joda.time.{LocalDate, LocalTime}

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
    surgeryAvgInfo: Option[Seq[SurgeryAvgInfo]] = None,
    plannedSurgeries : Option[Seq[FutureSurgeryInfo]] = None,
    topOptions : Option[Seq[BlockFillingOption]] = None,
) extends GetDataWork

case class BlockFillingOption
(
    doctorId : Int,
    doctorName : Option[String],
    surgeries : Seq[SurgeryBasicInfo],

    chanceForRestingShort : Double,
    chanceForHospitalizeShort : Double,
    expectedProfit : Double,
    windowUtilization : Double,
    frequency : Int,
    // TODO find out the relevant fields
) extends Ordered[BlockFillingOption]
{
    require(0 <= chanceForRestingShort && chanceForRestingShort <= 1, "chanceForRestingShort must be in [0-1]")
    require(0 <= chanceForHospitalizeShort && chanceForHospitalizeShort <= 1, "chanceForHospitalizeShort must be in [0-1]")
    require(0 <= windowUtilization && windowUtilization <= 1, "windowUtilization must be in [0-1]")
    
    def totalScore : Int = ??? //todo (val?)
    
    override def compare(that : BlockFillingOption) : Int =
    {
        // NOTE - Reverse to sort from high score to low
        that.totalScore - this.totalScore
    }
    
    def explanation : String =
        s"""Properties:
          |
          |Chance for resting beds short: \t$chanceForRestingShort
          |Chance for hospitalize beds short: \t$chanceForHospitalizeShort
          |Expected profit: \t$expectedProfit
          |Window utilization: \t$windowUtilization
          |Frequency in the past: \t$frequency
          |""".stripMargin
}

