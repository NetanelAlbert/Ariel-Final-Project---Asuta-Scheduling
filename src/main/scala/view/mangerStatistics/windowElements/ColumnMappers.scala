package view.mangerStatistics.windowElements

import model.DTOs.{DoctorStatistics, Settings, SurgeryAvgInfo, SurgeryAvgInfoByDoctor}
import scalafx.beans.property.ObjectProperty
import scalafx.beans.value.ObservableValue
import scalafx.scene.control.TableColumn.CellDataFeatures

import java.text.DecimalFormat

case class ComparableOptionWithFallbackToString[T](value : Option[T])(implicit ev : T => Ordered[T]) extends Ordered[ComparableOptionWithFallbackToString[T]]
{
    override def toString = value.map(_.toString).getOrElse("Unknown")
    
    def compare(that : ComparableOptionWithFallbackToString[T]) : Int =  (this.value, that.value) match
    {
        case (None, None) => 0

        case (Some(_), None) => 1
        
        case (None, Some(_)) => - 1

        case (Some(thisV), Some(thatV)) => thisV.compare(thatV)
    }
    
}

trait TableSceneBaseMappers
{
    
    import TableSceneBaseMappers._
    
    def profitAvgColMapper(sdf : CellDataFeatures[DoctorStatistics, ComparableOptionWithFallbackToString[Int]]) : ObservableValue[ComparableOptionWithFallbackToString[Int], ComparableOptionWithFallbackToString[Int]]
    
    def surgeryDurationAvgMinutesColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double]
    
    def restingDurationAvgMinutesColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double]
    
    def hospitalizationDurationAvgHoursColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double]
    
    def globalAvgColMapper(settings : Settings)(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double]
    
    def double2digits(double : Double) : Double = df.format(double).toDouble
}

object TableSceneBaseMappers
{
    val df : DecimalFormat = new DecimalFormat("#.##")
}

class TableSceneNormalMappers extends TableSceneBaseMappers
{
    def profitAvgColMapper(sdf : CellDataFeatures[DoctorStatistics, ComparableOptionWithFallbackToString[Int]]) : ObservableValue[ComparableOptionWithFallbackToString[Int], ComparableOptionWithFallbackToString[Int]] =
    {
        ObjectProperty(ComparableOptionWithFallbackToString[Int](sdf.value.profit))
    }
    
    def surgeryDurationAvgMinutesColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] =
    {
        ObjectProperty(double2digits(sdf.value.surgeryDurationAvgMinutes))
    }
    
    def restingDurationAvgMinutesColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] =
    {
        ObjectProperty(double2digits(sdf.value.restingDurationAvgMinutes))
    }
    
    def hospitalizationDurationAvgHoursColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] =
    {
        ObjectProperty(double2digits(sdf.value.hospitalizationDurationAvgHours))
    }
    
    def globalAvgColMapper(settings : Settings)(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] =
    {
        ObjectProperty(double2digits(sdf.value.globalAvg(settings)))
    }
}

class TableSceneImprovementMappers(tableScene : TableScene) extends TableSceneNormalMappers
{
    val surgeryGeneralAvgByDoctorMap : Map[Int, SurgeryAvgInfo] = tableScene.doctorsBaseStatistics.par.map(doctorStatistics => doctorStatistics.id -> createSurgeryGeneralAvgByDoctor(doctorStatistics)).toList.toMap
    
    def createSurgeryGeneralAvgByDoctor(doctorStatistics : DoctorStatistics) : SurgeryAvgInfo =
    {
        val doctorSurgeries = tableScene.surgeryAvgInfoByDoctorMap.getOrElse(doctorStatistics.id, Seq()).map(_.operationCode)
        val filteredSurgeryAvgInfoList = tableScene.surgeryAvgInfoList.filter(surg => doctorSurgeries.contains(surg.operationCode))
        val totalSurgeries = filteredSurgeryAvgInfoList.map(_.amountOfData).sum.toDouble
        val surgeryDurationAvgMinutes = filteredSurgeryAvgInfoList.map(surg => surg.amountOfData * surg.surgeryDurationAvgMinutes).sum / totalSurgeries
        val restingDurationAvgMinutes = filteredSurgeryAvgInfoList.map(surg => surg.amountOfData * surg.restingDurationAvgMinutes).sum / totalSurgeries
        val hospitalizationDurationAvgHours = filteredSurgeryAvgInfoList.map(surg => surg.amountOfData * surg.hospitalizationDurationAvgHours).sum / totalSurgeries
        
        SurgeryAvgInfo(0, totalSurgeries.toInt, surgeryDurationAvgMinutes, restingDurationAvgMinutes, hospitalizationDurationAvgHours)
    }
    
    override def surgeryDurationAvgMinutesColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] =
    {
        ObjectProperty(improvement(sdf.value, _.surgeryDurationAvgMinutes, _.surgeryDurationAvgMinutes))
    }
    
    override def restingDurationAvgMinutesColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] =
    {
        ObjectProperty(improvement(sdf.value, _.restingDurationAvgMinutes, _.restingDurationAvgMinutes))
    }
    
    override def hospitalizationDurationAvgHoursColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] =
    {
        ObjectProperty(improvement(sdf.value, _.hospitalizationDurationAvgHours, _.hospitalizationDurationAvgHours))
    }
    
    private def improvement(doctor : DoctorStatistics,
                            doctorField : DoctorStatistics => Double,
                            surgeryField : SurgeryAvgInfo => Double,
                            globalMinusPerson : Boolean = false) : Double =
    {
        val surgeryGeneralAvgByDoctor = surgeryGeneralAvgByDoctorMap.get(doctor.id)
        surgeryGeneralAvgByDoctor match
        {
            case Some(surgeryGeneralAvgInfo) =>
            {
                val personalData = doctorField(doctor)
                val globalData = surgeryField(surgeryGeneralAvgInfo)
                if (globalMinusPerson)
                {
                    double2digits(globalData - personalData)
                }
                else
                {
                    double2digits(personalData - globalData)
                }
            }
            
            case _ => 0
        }
    }
}

class TableSceneImprovementMappersBySurgery(tableScene : TableScene, operationCode : Double) extends TableSceneNormalMappers
{
    override def surgeryDurationAvgMinutesColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] =
    {
        ObjectProperty(improvement(sdf.value, _.surgeryDurationAvgMinutes, _.surgeryDurationAvgMinutes))
    }
    
    override def restingDurationAvgMinutesColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] =
    {
        ObjectProperty(improvement(sdf.value, _.restingDurationAvgMinutes, _.restingDurationAvgMinutes))
    }
    
    override def hospitalizationDurationAvgHoursColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] =
    {
        ObjectProperty(improvement(sdf.value, _.hospitalizationDurationAvgHours, _.hospitalizationDurationAvgHours))
    }
    
    private def improvement(doctor : DoctorStatistics,
                            doctorSurgeryField : SurgeryAvgInfoByDoctor => Double,
                            surgeryField : SurgeryAvgInfo => Double,
                            globalMinusPerson : Boolean = false) : Double =
    {
        val surgeryAvgInfoByDoctorOption = tableScene.surgeryAvgInfoByDoctorMap.get(doctor.id).flatMap(_.find(_.operationCode == operationCode))
        val surgeryAvgInfoOption = tableScene.surgeryAvgInfoList.find(_.operationCode == operationCode)
        
        (surgeryAvgInfoByDoctorOption, surgeryAvgInfoOption) match
        {
            case (Some(surgeryAvgInfoByDoctor), Some(surgeryAvgInfo)) =>
            {
                val personalData = doctorSurgeryField(surgeryAvgInfoByDoctor)
                val globalData = surgeryField(surgeryAvgInfo)
                if (globalMinusPerson)
                {
                    double2digits(globalData - personalData)
                }
                else
                {
                    double2digits(personalData - globalData)
                }
            }
            
            case _ => 0
        }
    }
    
}