package work

import model.DTOs.{DoctorAvailability, DoctorStatistics, FutureSurgeryInfo, PastSurgeryInfo, SurgeryAvgInfo, SurgeryAvgInfoByDoctor, SurgeryStatistics}
import org.joda.time.Days

import java.io.File

trait FileWork extends Work
{
    val file : File
}

case class ReadPastSurgeriesExcelWork
(
    file : File,
    keepOldMapping : Boolean,
    pasteSurgeries : Option[Iterable[PastSurgeryInfo]] = None,
    surgeryStatistics : Option[Iterable[SurgeryStatistics]] = None,
    surgeryAvgInfo : Option[Iterable[SurgeryAvgInfo]] = None,
    surgeryAvgInfoByDoctor : Option[Iterable[SurgeryAvgInfoByDoctor]] = None,
    doctorStatistics : Option[Iterable[DoctorStatistics]] = None,
    doctorAvailabilities : Option[Set[DoctorAvailability]] = None
) extends FileWork

case class ReadSurgeryMappingExcelWork(file : File, surgeryMapping : Option[Map[Double, String]] = None) extends FileWork

case class ReadDoctorsMappingExcelWork(file : File, doctorMapping : Option[Map[Int, String]] = None) extends FileWork

case class ReadProfitExcelWork(file : File,
                               surgeriesProfit : Option[Iterable[(Double, Int)]] = None,
                               doctorsProfit : Option[Iterable[(Int, Int)]] = None,
                              ) extends FileWork

case class ReadFutureSurgeriesExcelWork(
    file : File,
    keepOldMapping : Boolean,
    futureSurgeries : Option[Iterable[FutureSurgeryInfo]] = None,
) extends FileWork
