package work

import model.DTOs.{DoctorAvailability, DoctorStatistics, PastSurgeryInfo, SurgeryAvgInfo, SurgeryAvgInfoByDoctor, SurgeryStatistics}
import org.joda.time.Days

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
    doctorAvailabilities : Option[Set[DoctorAvailability]] = None
) extends FileWork

case class ReadSurgeryMappingExcelWork(filePath : String, surgeryMapping : Option[Map[Double, Option[String]]]) extends FileWork

case class ReadDoctorsMappingExcelWork(filePath : String, doctorMapping : Option[Map[Int, Option[String]]]) extends FileWork



case class ReadFutureSurgeriesExcelWork(filePath : String) extends FileWork

case class ReadProfitExcelWork(filePath : String) extends FileWork