package work

import model.DTOs.{DoctorStatistics, OperationCodeAndName, SurgeryAvgInfo, SurgeryAvgInfoByDoctor, SurgeryBasicInfo, SurgeryStatistics}
import model.probability.IntegerDistribution

import java.util.Date

trait GetDataWork extends Work

case class GetDoctorsStatisticsWork
(
    doctorsBaseStatistics : Option[Seq[DoctorStatistics]] = None,
    surgeryAvgInfoByDoctorMap : Option[Map[Int, Seq[SurgeryAvgInfoByDoctor]]] = None,
    surgeryAvgInfoList : Option[Seq[SurgeryAvgInfo]] = None,
    operationCodeAndNames : Option[Seq[OperationCodeAndName]] = None
) extends GetDataWork

case class GetOptionsForFreeBlockWork
(
    startTime : Date,
    endTime : Date,
    dayOfWeek: Int,
    optionalDoctorsWithSurgeries : Option[Map[Int, Seq[Double]]],
    surgeryStatistics: Option[Seq[SurgeryStatistics]],
    currentRestingDistribution : Option[IntegerDistribution] = None,
    currentHospitalizationDistribution : Option[IntegerDistribution] = None,
    topOptions : Option[Seq[BlockFillingOption]] = None,
) extends GetDataWork

case class BlockFillingOption
(
    doctorId : Double,
    doctorName : Option[String],
    surgeries : Seq[SurgeryBasicInfo],
   
    // TODO find out the relevant fields
)

