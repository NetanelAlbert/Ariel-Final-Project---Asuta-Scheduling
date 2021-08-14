package view.mangerStatistics.windows

import model.DTOs.{DoctorStatistics, SurgeryAvgInfo, SurgeryAvgInfoByDoctor}
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.control.TableColumn.CellDataFeatures
import scalafx.scene.control.{SelectionMode, TableColumn, TableView}
import scalafx.scene.layout.VBox
import scalafx.stage.Screen

import java.text.DecimalFormat

case class NormalTableScene(doctorsBaseStatistics : Seq[DoctorStatistics],
                            surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]],
                            surgeryAvgInfoList : Seq[SurgeryAvgInfo])
    extends TableScene with TableSceneNormalMappers

case class ImprovementTableSceneB(doctorsBaseStatistics : Seq[DoctorStatistics],
                            surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]],
                            surgeryAvgInfoList : Seq[SurgeryAvgInfo])
    extends TableScene with TableSceneImprovementMappers

case class ImprovementTableSceneBySurgery(doctorsBaseStatistics : Seq[DoctorStatistics],
                                          surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]],
                                          surgeryAvgInfoList : Seq[SurgeryAvgInfo],
                                          operationCode : Double)
    extends TableScene with TableSceneImprovementMappersBySurgery

trait TableScene extends Scene with TableSceneBaseMappers
{
    val doctorsBaseStatistics : Seq[DoctorStatistics]
    val surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]]
    val surgeryAvgInfoList : Seq[SurgeryAvgInfo]
    
    val data = ObservableBuffer.empty[DoctorStatistics] ++= doctorsBaseStatistics
    val table = new TableView[DoctorStatistics](data)
    
    val nameCol = new TableColumn[DoctorStatistics, String]("Doctor Name")
    nameCol.cellValueFactory = sdf => StringProperty(sdf.value.nameOrId)
    
    val profitAvgCol = new TableColumn[DoctorStatistics, Double]("Average Profit")
    profitAvgCol.cellValueFactory = profitAvgColMapper
    
    val surgeryDurationAvgMinutesCol = new TableColumn[DoctorStatistics, Double]("Average Surgery Duration (minutes)")
    surgeryDurationAvgMinutesCol.cellValueFactory = surgeryDurationAvgMinutesColMapper
    
    val restingDurationAvgMinutesCol = new TableColumn[DoctorStatistics, Double]("Average Resting Duration (minutes)")
    restingDurationAvgMinutesCol.cellValueFactory = restingDurationAvgMinutesColMapper
    
    val hospitalizationDurationAvgHoursCol = new TableColumn[DoctorStatistics, Double]("Average Hospitalization Duration (hours)")
    hospitalizationDurationAvgHoursCol.cellValueFactory = hospitalizationDurationAvgHoursColMapper
    
    val globalAvgCol = new TableColumn[DoctorStatistics, Double]("Global Average")
    globalAvgCol.cellValueFactory = globalAvgColMapper
    
    val columns = List[javafx.scene.control.TableColumn[DoctorStatistics, _]](nameCol,
                                                                              profitAvgCol,
                                                                              surgeryDurationAvgMinutesCol,
                                                                              restingDurationAvgMinutesCol,
                                                                              hospitalizationDurationAvgHoursCol,
                                                                              globalAvgCol)
    
    table.columns ++= columns
    table.selectionModel.apply.setCellSelectionEnabled(true)
    table.selectionModel.apply.setSelectionMode(SelectionMode.Multiple)
    
    // Dimensions settings
    table.columns.foreach(_.setPrefWidth(Screen.primary.bounds.width / columns.size))
    table.prefHeight = Screen.primary.bounds.height
    
    val menu = new ManagerMenu(
        loadCurrentScheduleListener = _ => ,
        loadPastSurgeriesListener = ,
        loadProfitListener = ,
        loadDoctorsIDMappingListener = ,
        loadSurgeryIDMappingListener = ,
        radioBasicInformationListener = ,
        radioImprovementInformationAverageListener = ,
        radioImprovementInformationByOperationListener =
    )
    val vBox = new VBox()
    vBox.children = List(menu, table)
    root = vBox
}

trait TableSceneBaseMappers
{
    import TableSceneBaseMappers._
    
    def profitAvgColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double]
    
    def surgeryDurationAvgMinutesColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double]
    
    def restingDurationAvgMinutesColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double]
    
    def hospitalizationDurationAvgHoursColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double]
    
    def globalAvgColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double]
    
    def double2digits(double : Double) : Double = df.format(double).toDouble
}

object TableSceneBaseMappers
{
    val df : DecimalFormat = new DecimalFormat("#.##")
}

trait TableSceneNormalMappers extends TableSceneBaseMappers
{
    def profitAvgColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] = ObjectProperty(double2digits(sdf.value.profitAvg))
    
    def surgeryDurationAvgMinutesColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] = ObjectProperty(double2digits(sdf.value.surgeryDurationAvgMinutes))
    
    def restingDurationAvgMinutesColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] = ObjectProperty(double2digits(sdf.value.restingDurationAvgMinutes))
    
    def hospitalizationDurationAvgHoursColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] = ObjectProperty(double2digits(sdf.value.hospitalizationDurationAvgHours))
    
    def globalAvgColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] = ObjectProperty(double2digits(sdf.value.globalAvg))
}

trait TableSceneImprovementMappers extends TableSceneNormalMappers
{
    this : TableScene =>
    
    val surgeryGeneralAvgByDoctorMap : Map[Int, SurgeryAvgInfo] = doctorsBaseStatistics.par.map(doctorStatistics => doctorStatistics.id -> createSurgeryGeneralAvgByDoctor(doctorStatistics)).toList.toMap
    
    def createSurgeryGeneralAvgByDoctor(doctorStatistics : DoctorStatistics) : SurgeryAvgInfo =
    {
        val doctorSurgeries = surgeryAvgInfoByDoctorMap.getOrElse(doctorStatistics.id, Seq()).map(_.operationCode)
        val filteredSurgeryAvgInfoList = surgeryAvgInfoList.filter(surg => doctorSurgeries.contains(surg.operationCode))
        val totalSurgeries = filteredSurgeryAvgInfoList.map(_.amountOfData).sum.toDouble
        val surgeryDurationAvgMinutes = filteredSurgeryAvgInfoList.map(surg => surg.amountOfData * surg.surgeryDurationAvgMinutes).sum / totalSurgeries
        val restingDurationAvgMinutes = filteredSurgeryAvgInfoList.map(surg => surg.amountOfData * surg.restingDurationAvgMinutes).sum / totalSurgeries
        val hospitalizationDurationAvgHours = filteredSurgeryAvgInfoList.map(surg => surg.amountOfData * surg.hospitalizationDurationAvgHours).sum / totalSurgeries
    
        SurgeryAvgInfo(0, totalSurgeries.toInt, surgeryDurationAvgMinutes, restingDurationAvgMinutes, hospitalizationDurationAvgHours)
    }
    
    override def surgeryDurationAvgMinutesColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] = ObjectProperty(improvement(sdf.value, _.surgeryDurationAvgMinutes, _.surgeryDurationAvgMinutes))
    
    override def restingDurationAvgMinutesColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] = ObjectProperty(improvement(sdf.value, _.restingDurationAvgMinutes, _.restingDurationAvgMinutes))
    
    override def hospitalizationDurationAvgHoursColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] = ObjectProperty(improvement(sdf.value, _.hospitalizationDurationAvgHours, _.hospitalizationDurationAvgHours))
    
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
    trait TableSceneImprovementMappersBySurgery extends TableSceneNormalMappers
    {
        this : TableScene =>
    
        val operationCode : Double
    
        override def surgeryDurationAvgMinutesColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] = ObjectProperty(improvement(sdf.value, _.surgeryDurationAvgMinutes, _.surgeryDurationAvgMinutes))
    
        override def restingDurationAvgMinutesColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] = ObjectProperty(improvement(sdf.value, _.restingDurationAvgMinutes, _.restingDurationAvgMinutes))
    
        override def hospitalizationDurationAvgHoursColMapper(sdf : CellDataFeatures[DoctorStatistics, Double]) : ObservableValue[Double, Double] = ObjectProperty(improvement(sdf.value, _.hospitalizationDurationAvgHours, _.hospitalizationDurationAvgHours))
    
        private def improvement(doctor : DoctorStatistics,
                                doctorSurgeryField : SurgeryAvgInfoByDoctor => Double,
                                surgeryField : SurgeryAvgInfo => Double,
                                globalMinusPerson : Boolean = false) : Double =
        {
            val surgeryAvgInfoByDoctorOption = surgeryAvgInfoByDoctorMap.get(doctor.id).flatMap(_.find(_.doctorId == doctor.id))
            val surgeryAvgInfoOption = surgeryAvgInfoList.find(_.operationCode == operationCode)
    
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
