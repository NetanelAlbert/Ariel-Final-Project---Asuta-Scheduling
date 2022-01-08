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
    plannedSurgeryStatistics : Option[Seq[SurgeryStatistics]] = None,
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
    // TODO find out the relevant fields
) extends Ordered[BlockFillingOption]
{
    require(0 <= chanceForRestingShort && chanceForRestingShort <= 1, s"chanceForRestingShort must be in [0-1], but it's: $chanceForRestingShort")
    require(0 <= chanceForHospitalizeShort && chanceForHospitalizeShort <= 1, s"chanceForHospitalizeShort must be in [0-1], but its: $chanceForHospitalizeShort")
    
    def nameOrID = doctorName.getOrElse(s"$doctorId (id)")
    
    def totalScore : Int = ??? //todo (val?)
    
    override def compare(that : BlockFillingOption) : Int =
    {
        // NOTE - Reverse to sort from high score to low
        that.totalScore - this.totalScore
    }
}

