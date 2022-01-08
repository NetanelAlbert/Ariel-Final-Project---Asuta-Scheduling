package view.mangerStatistics.windowElements

import common.Utils
import model.DTOs.{DoctorStatistics, Settings, SurgeryAvgInfo, SurgeryAvgInfoByDoctor}
import scalafx.beans.property.ObjectProperty
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.TableCell
import scalafx.scene.control.TableColumn.CellDataFeatures
import view.common.ComparableOptionWithFallbackToString
import view.common.UiUtils.{Double2digitsMapper, double2digits, doubleToPercent}

trait TableSceneBaseMappers
{
    def profitAvgColMapper(cdf : CellDataFeatures[DoctorStatistics, ComparableOptionWithFallbackToString[Int]]) : ObservableValue[ComparableOptionWithFallbackToString[Int], ComparableOptionWithFallbackToString[Int]]
    
    def personalAvgDiffColumnMapper : PersonalAvgDiffColumnMapper
    
    def globalAvgColMapper(settings : Settings) : CellDataFeatures[DoctorStatistics, Int] =>  ObservableValue[Int, Int]
}

class TableSceneNormalMappers(doctorsStatistics : Seq[DoctorStatistics], override val personalAvgDiffColumnMapper : PersonalAvgDiffColumnMapper) extends TableSceneBaseMappers
{
    override def profitAvgColMapper(cdf : CellDataFeatures[DoctorStatistics, ComparableOptionWithFallbackToString[Int]]) : ObservableValue[ComparableOptionWithFallbackToString[Int], ComparableOptionWithFallbackToString[Int]] =
    {
        ObjectProperty(ComparableOptionWithFallbackToString[Int](cdf.value.profit))
    }
   
    override def globalAvgColMapper(settings : Settings) : CellDataFeatures[DoctorStatistics, Int] =>  ObservableValue[Int, Int] =
    {
        val surgeryDurationNormalizer = Utils.getNormalizer(doctorsStatistics.map(_.surgeryDurationAvgMinutes))
        val restingDurationNormalizer = Utils.getNormalizer(doctorsStatistics.map(_.restingDurationAvgMinutes))
        val hospitalizationDurationNormalizer = Utils.getNormalizer(doctorsStatistics.map(_.hospitalizationDurationAvgHours))
        val profitNormalizer = Utils.getNormalizer(doctorsStatistics.flatMap(_.profit.orElse(settings.avgSurgeryProfit)))
        
        def globalMapper(cdf : CellDataFeatures[DoctorStatistics, Int]) :  ObservableValue[Int, Int] =
        {
            val result = personalAvgDiffColumnMapper.diffMapper(cdf.value).globalAvg(
                settings,
                surgeryDurationNormalizer,
                restingDurationNormalizer,
                hospitalizationDurationNormalizer,
                profitNormalizer)
            ObjectProperty(doubleToPercent(result))
        }
        
        globalMapper
    }
}

//class TableSceneImprovementMappers(tableScene : TableScene) extends TableSceneNormalMappers(tableScene)
//{
//    val doctorsImprovement : Map[Int, DoctorStatistics] = tableScene.doctorsBaseStatistics.par.map
//    {
//        doctorStatistics => doctorStatistics.id -> createDoctorImprovement(doctorStatistics)
//    }.toList.toMap
//
//    def createDoctorImprovement(doctorStatistics : DoctorStatistics) : DoctorStatistics =
//    {
//        val doctorSurgeries = tableScene.surgeryAvgInfoByDoctorMap.getOrElse(doctorStatistics.id, Seq()).map(_.operationCode)
////        val amountOfDoctorSurgeries = doctorSurgeries.map(_.amountOfData)
////        val x = doctorSurgeries.map(doctorSurgery =>
////                                    {
////                                        val globalSurgery = tableScene.surgeryAvgInfoList.find(_.operationCode == doctorSurgery.operationCode)
////
////                                    })
//        val filteredSurgeryAvgInfoList = tableScene.surgeryAvgInfoList.filter(surg => doctorSurgeries.contains(surg.operationCode))
//        val totalSurgeries = filteredSurgeryAvgInfoList.map(_.amountOfData).sum.toDouble
//        val surgeryDurationAvgMinutes = filteredSurgeryAvgInfoList.map(surg => surg.amountOfData * surg.surgeryDurationAvgMinutes).sum / totalSurgeries
//        val restingDurationAvgMinutes = filteredSurgeryAvgInfoList.map(surg => surg.amountOfData * surg.restingDurationAvgMinutes).sum / totalSurgeries
//        val hospitalizationDurationAvgHours = filteredSurgeryAvgInfoList.map(surg => surg.amountOfData * surg.hospitalizationDurationAvgHours).sum / totalSurgeries
//
//        val surgeryDurationAvgMinutesDiff = doctorStatistics.surgeryDurationAvgMinutes - surgeryDurationAvgMinutes
//        val restingDurationAvgMinutesDiff = doctorStatistics.restingDurationAvgMinutes - restingDurationAvgMinutes
//        val hospitalizationDurationAvgHoursDiff = doctorStatistics.hospitalizationDurationAvgHours - hospitalizationDurationAvgHours
//
//        doctorStatistics.copy(
//            surgeryDurationAvgMinutes = surgeryDurationAvgMinutesDiff,
//            restingDurationAvgMinutes = restingDurationAvgMinutesDiff,
//            hospitalizationDurationAvgHours = hospitalizationDurationAvgHoursDiff,
//            profit = None,
//        )
////        SurgeryAvgInfo(0, totalSurgeries.toInt, surgeryDurationAvgMinutes, restingDurationAvgMinutes, hospitalizationDurationAvgHours)
//    }
//
//    override def surgeryDurationAvgMinutesColMapper(cdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] =
//    {
//        ObjectProperty(doctorsImprovement(cdf.value.id).surgeryDurationAvgMinutes.double2digits)
//    }
//
//    override def restingDurationAvgMinutesColMapper(cdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] =
//    {
//        ObjectProperty(doctorsImprovement(cdf.value.id).restingDurationAvgMinutes.double2digits)
//    }
//
//    override def hospitalizationDurationAvgHoursColMapper(cdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] =
//    {
//        ObjectProperty(doctorsImprovement(cdf.value.id).hospitalizationDurationAvgHours.double2digits)
//    }
    
//    override def profitAvgColMapper(cdf : CellDataFeatures[DoctorStatistics, ComparableOptionWithFallbackToString[Int]]) : ObservableValue[ComparableOptionWithFallbackToString[Int], ComparableOptionWithFallbackToString[Int]] =
//    {
//        // show always "Unknown"
//        ObjectProperty(ComparableOptionWithFallbackToString.Empty)
//    }
    
//    override def globalAvgColMapper(settings : Settings, doctorStatistics : DoctorStatistics) :  ObservableValue[Double, Double] =
//    {
//        val doctorsStatistics = doctorsImprovement.values
//        val surgeryDurationNormalizer = Utils.getNormalizer(doctorsStatistics.map(_.surgeryDurationAvgMinutes))
//        val restingDurationNormalizer = Utils.getNormalizer(doctorsStatistics.map(_.restingDurationAvgMinutes))
//        val hospitalizationDurationNormalizer = Utils.getNormalizer(doctorsStatistics.map(_.hospitalizationDurationAvgHours))
//        val profitNormalizer = Utils.constantNormalizer()(_)
//
//        doctorsImprovement(doctorStatistics.id).globalAvg(settings,
//                                                          surgeryDurationNormalizer,
//                                                          restingDurationNormalizer,
//                                                          hospitalizationDurationNormalizer,
//                                                          profitNormalizer)
//    }
    
//    private def improvement(doctor : DoctorStatistics,
//                            doctorField : DoctorStatistics => Double,
//                            surgeryField : SurgeryAvgInfo => Double,
//                            globalMinusPerson : Boolean = false) : Double =
//    {
//        val surgeryGeneralAvgByDoctor = surgeryGeneralAvgByDoctorMap.get(doctor.id)
//        surgeryGeneralAvgByDoctor match
//        {
//            case Some(surgeryGeneralAvgInfo) =>
//            {
//                val personalData = doctorField(doctor)
//                val globalData = surgeryField(surgeryGeneralAvgInfo)
//                if (globalMinusPerson)
//                {
//                    double2digits(globalData - personalData)
//                }
//                else
//                {
//                    double2digits(personalData - globalData)
//                }
//            }
//
//            case _ => 0
//        }
//    }
//}

//class TableSceneImprovementMappersBySurgery(tableScene : TableScene, operationCode : Double) extends TableSceneNormalMappers(tableScene)
//{
//    val surgeryAvgInfoOption = tableScene.surgeryAvgInfoList.find(_.operationCode == operationCode)
//    val improvementItems = tableScene.doctorsBaseStatistics.map(doctorStatistic =>
//    {
//        val surgeryDurationImprovement = improvement(doctorStatistic, _.surgeryDurationAvgMinutes, _.surgeryDurationAvgMinutes)
//        val restingDurationImprovement = improvement(doctorStatistic, _.restingDurationAvgMinutes, _.restingDurationAvgMinutes)
//        val hospitalizationDurationImprovement = improvement(doctorStatistic, _.hospitalizationDurationAvgHours, _.hospitalizationDurationAvgHours)
//
//        doctorStatistic.id -> doctorStatistic.copy(
//            surgeryDurationAvgMinutes = surgeryDurationImprovement,
//            restingDurationAvgMinutes = restingDurationImprovement,
//            hospitalizationDurationAvgHours = hospitalizationDurationImprovement,
//            profit = None
//        )
//    }).toMap
//
//    override def surgeryDurationAvgMinutesColMapper(cdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] =
//    {
//        ObjectProperty(improvementItems(cdf.value.id).surgeryDurationAvgMinutes)
//    }
//
//    override def restingDurationAvgMinutesColMapper(cdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] =
//    {
//        ObjectProperty(improvementItems(cdf.value.id).restingDurationAvgMinutes)
//    }
//
//    override def hospitalizationDurationAvgHoursColMapper(cdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] =
//    {
//        ObjectProperty(improvementItems(cdf.value.id).hospitalizationDurationAvgHours)
//    }
//
//    override def profitAvgColMapper(cdf : CellDataFeatures[DoctorStatistics, ComparableOptionWithFallbackToString[Int]]) : ObservableValue[ComparableOptionWithFallbackToString[Int], ComparableOptionWithFallbackToString[Int]] =
//    {
//        // show always "Unknown"
//        ObjectProperty(ComparableOptionWithFallbackToString.Empty)
//    }
//
//    override def globalAvgColMapper(settings : Settings, doctorStatistics : DoctorStatistics, valueMapper : DoctorStatistics => DoctorStatistics) :  ObservableValue[Int, Int] =
//    {
//        val improvementItemsValues = improvementItems.values
//        val surgeryDurationNormalizer = Utils.getNormalizer(improvementItemsValues.map(_.surgeryDurationAvgMinutes))
//        val restingDurationNormalizer = Utils.getNormalizer(improvementItemsValues.map(_.restingDurationAvgMinutes))
//        val hospitalizationDurationNormalizer = Utils.getNormalizer(improvementItemsValues.map(_.hospitalizationDurationAvgHours))
//        val profitNormalizer = Utils.constantNormalizer(1)(_)
//
//        val result = improvementItems(doctorStatistics.id).globalAvg(settings,
//                                                        surgeryDurationNormalizer,
//                                                        restingDurationNormalizer,
//                                                        hospitalizationDurationNormalizer,
//                                                        profitNormalizer)
//        ObjectProperty(doubleToPercent(result))
//    }
//
//    private def improvement(doctor : DoctorStatistics,
//                            doctorSurgeryField : SurgeryAvgInfoByDoctor => Double,
//                            surgeryField : SurgeryAvgInfo => Double,
//                            globalMinusPerson : Boolean = false) : Double =
//    {
//        val surgeryAvgInfoByDoctorOption = tableScene.surgeryAvgInfoByDoctorMap.get(doctor.id).flatMap(_.find(_.operationCode == operationCode))
//
//        (surgeryAvgInfoByDoctorOption, surgeryAvgInfoOption) match
//        {
//            case (Some(surgeryAvgInfoByDoctor), Some(surgeryAvgInfo)) =>
//            {
//                val personalData = doctorSurgeryField(surgeryAvgInfoByDoctor)
//                val globalData = surgeryField(surgeryAvgInfo)
//
//                if (globalMinusPerson)
//                {
//                    double2digits(globalData - personalData)
//                }
//                else
//                {
//                    double2digits(personalData - globalData)
//                }
//            }
//
//            case _ => 0
//        }
//    }
//}