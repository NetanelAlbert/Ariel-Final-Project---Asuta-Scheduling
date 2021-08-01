package work

import model.DTOs.{DoctorStatistics, SurgeryAvgInfo, SurgeryAvgInfoByDoctor, SurgeryBasicInfo}
import model.probability.IntegerDistribution

import java.util.Date

trait GetDataWork extends Work

case class GetDoctorsStatisticsWork
(
    doctorsBaseStatistics : Option[Seq[DoctorStatistics]] = None,
    surgeryAvgInfoByDoctorMap : Option[Map[Int, Seq[SurgeryAvgInfoByDoctor]]] = None,
    surgeryAvgInfoList : Option[Seq[SurgeryAvgInfo]] = None
) extends GetDataWork

case class GetOptionsForFreeBlockWork
(
    startTime : Date,
    endTime : Date,
    predictedCurrentRestingDistribution : Option[IntegerDistribution] = None,
    predictedCurrentHospitalizationDistribution : Option[IntegerDistribution] = None,
    topOptions : Option[Seq[BlockFillingOption]] = None,
) extends GetDataWork

case class BlockFillingOption
(
    doctorId : Double,
    doctorName : Option[String],
    surgeries : Seq[SurgeryBasicInfo],
    predictedRestingDistribution : Option[IntegerDistribution] = None,
    predictedHospitalizationDistribution : Option[IntegerDistribution] = None,
    // TODO find out the relevant fields
)

