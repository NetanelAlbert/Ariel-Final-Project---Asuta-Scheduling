package work

import model.DTOs.{DoctorStatistics, PastSurgeryInfo, SurgeryAvgInfo, SurgeryAvgInfoByDoctor, SurgeryStatistics}

trait FileWork extends Work
{
    val filePath : String
}

case class ReadPastSurgeriesExcelWork
(
    filePath : String,
    pasteSurgeries : Option[Iterable[PastSurgeryInfo]] = None,
    surgeryStatistics : Option[Iterable[SurgeryStatistics]] = None,
    surgeryAvgInfo : Option[Iterable[SurgeryAvgInfo]] = None,
    surgeryAvgInfoByDoctor : Option[Iterable[SurgeryAvgInfoByDoctor]] = None,
    doctorStatistics : Option[Iterable[DoctorStatistics]] = None,
) extends FileWork

case class ReadProfitExcelWork(filePath : String) extends FileWork

case class ReadDoctorsMappingExcelWork(filePath : String) extends FileWork

case class ReadSurgeryMappingExcelWork(filePath : String) extends FileWork


case class ReadFutureSurgeriesExcelWork(filePath : String) extends FileWork
