package view.mangerStatistics.windowElements

import common.Utils.doctorStatisticsSeqToMapByID
import model.DTOs.{DoctorStatistics, OperationCodeAndName, Settings, SurgeryAvgInfo, SurgeryAvgInfoByDoctor}
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.scene.control.TableColumn
import view.common.ComparableOptionWithFallbackToString
import scalafx.scene.control.TableCell
import view.common.UiUtils.Double2digitsMapper


class Columns(
    doctorsStatistics : Seq[DoctorStatistics],
    surgeryAvgInfoByDoctorMap : Map[Int, Seq[SurgeryAvgInfoByDoctor]],
    surgeryAvgInfoList : Seq[SurgeryAvgInfo],
)
{
    // avg per doctor surgeries mapper
    private val m_avgByDoctorSurgeries = generateAvgByDoctorSurgeries()
    private val m_avgByDoctorSurgeriesMap = doctorStatisticsSeqToMapByID(m_avgByDoctorSurgeries)
    private val m_avgByDoctorSurgeriesAvgMinusPersonal = doctorsStatisticsDiff(m_avgByDoctorSurgeries, doctorsStatistics)
    private val m_avgByDoctorSurgeriesAvgMinusPersonalMap = doctorStatisticsSeqToMapByID(m_avgByDoctorSurgeriesAvgMinusPersonal)
    private val m_avgByDoctorSurgeriesPersonalAvgDiffColumnMapper = PersonalAvgDiffColumnMapper(
        avgMapper = stat => m_avgByDoctorSurgeriesMap(stat.id),
        diffMapper = stat => m_avgByDoctorSurgeriesAvgMinusPersonalMap(stat.id),
        )
    val m_avgByDoctorColumnsMappers = new TableSceneNormalMappers(m_avgByDoctorSurgeriesAvgMinusPersonal, m_avgByDoctorSurgeriesPersonalAvgDiffColumnMapper)
    
    //simple mapper
    private val m_simpleSurgeryAvgInfo = generateSimpleAVGSurgeries()
    private val m_simpleAVGSurgeriesAvgMinusPersonal = doctorsStatisticsDiff(Seq.fill(doctorsStatistics.size)(m_simpleSurgeryAvgInfo), doctorsStatistics)
    private val m_simpleAVGSurgeriesAvgMinusPersonalMap = doctorStatisticsSeqToMapByID(m_simpleAVGSurgeriesAvgMinusPersonal)
    private val m_simpleAVGSurgeriesPersonalAvgDiffColumnMapper = PersonalAvgDiffColumnMapper(
        avgMapper = _ => m_simpleSurgeryAvgInfo, // It's a simple average so it's the same for all doctors
        diffMapper = stat => m_simpleAVGSurgeriesAvgMinusPersonalMap(stat.id),
        )
    val m_simpleAvgColumnsMappers = new TableSceneNormalMappers(m_simpleAVGSurgeriesAvgMinusPersonal, m_simpleAVGSurgeriesPersonalAvgDiffColumnMapper)
    
    //columns
    private val nameCol = new TableColumn[DoctorStatistics, String]()
    private val profitAvgCol = new TableColumn[DoctorStatistics, ComparableOptionWithFallbackToString[Int]]()
    private val surgeryDurationAvgMinutesCol = new PersonalAvgDiffColumn(_.surgeryDurationAvgMinutes, m_avgByDoctorSurgeriesPersonalAvgDiffColumnMapper)
    private val restingDurationAvgMinutesCol = new PersonalAvgDiffColumn(_.restingDurationAvgMinutes, m_avgByDoctorSurgeriesPersonalAvgDiffColumnMapper)
    private val hospitalizationDurationAvgHoursCol = new PersonalAvgDiffColumn(_.hospitalizationDurationAvgHours, m_avgByDoctorSurgeriesPersonalAvgDiffColumnMapper)
    private val globalAvgCol = new TableColumn[DoctorStatistics, Int]()
    globalAvgCol.cellFactory = _ => Cells.trafficLightsBackgroundCell
 
    
    val columns = List[javafx.scene.control.TableColumn[DoctorStatistics, _]](nameCol,
                                                                              profitAvgCol,
                                                                              surgeryDurationAvgMinutesCol,
                                                                              restingDurationAvgMinutesCol,
                                                                              hospitalizationDurationAvgHoursCol,
                                                                              globalAvgCol)
    
    setColumnsNames(ColumnsNormalNames)
    
    def setColumnsMappers(mapper : TableSceneBaseMappers, settings : Settings)
    {
        nameCol.cellValueFactory = cdf => StringProperty(cdf.value.nameOrId)
        profitAvgCol.cellValueFactory = mapper.profitAvgColMapper
        surgeryDurationAvgMinutesCol.mapper = mapper.personalAvgDiffColumnMapper
        restingDurationAvgMinutesCol.mapper = mapper.personalAvgDiffColumnMapper
        hospitalizationDurationAvgHoursCol.mapper = mapper.personalAvgDiffColumnMapper
        globalAvgCol.cellValueFactory = mapper.globalAvgColMapper(settings)
    }
    
    def setColumnsNames(names : ColumnsNames)
    {
        nameCol.text = names.DOC_NAME
        profitAvgCol.text = names.AVG_PROFIT
        surgeryDurationAvgMinutesCol.text = names.AVG_SURGERY
        restingDurationAvgMinutesCol.text = names.AVG_RESTING
        hospitalizationDurationAvgHoursCol.text = names.AVG_HOSPITALIZATION
        globalAvgCol.text = names.GLOBAL_AVG
    }
    
    def generateSimpleAVGSurgeries() : DoctorStatistics =
    {
        val totalSurgeries = doctorsStatistics.map(_.amountOfData).sum
    
        val surgeryDurationAvgMinutes = doctorsStatistics.map(surg => surg.amountOfData * surg.surgeryDurationAvgMinutes).sum / totalSurgeries
        val restingDurationAvgMinutes = doctorsStatistics.map(surg => surg.amountOfData * surg.restingDurationAvgMinutes).sum / totalSurgeries
        val hospitalizationDurationAvgHours = doctorsStatistics.map(surg => surg.amountOfData * surg.hospitalizationDurationAvgHours).sum / totalSurgeries
    
        DoctorStatistics(
            id = 0,
            name = None,
            amountOfData = totalSurgeries,
            profit = None,
            surgeryDurationAvgMinutes = surgeryDurationAvgMinutes,
            restingDurationAvgMinutes = restingDurationAvgMinutes,
            hospitalizationDurationAvgHours = hospitalizationDurationAvgHours,
            )
    }
    
    def generatePersonalAvgDiffColumnMapperByOperationCode(operationCode : Double) : PersonalAvgDiffColumnMapper =
    {
        val doctorStatisticsMap = surgeryAvgInfoByDoctorMap.values.flatten.filter(_.operationCode == operationCode).map
        {
            case SurgeryAvgInfoByDoctor(_, doctorId, amountOfData, surgeryDurationAvgMinutes, restingDurationAvgMinutes, hospitalizationDurationAvgHours) =>
            {
                doctorId -> DoctorStatistics(doctorId, None, amountOfData, None, surgeryDurationAvgMinutes, restingDurationAvgMinutes, hospitalizationDurationAvgHours)
            }
        }.toMap
    
        val averageStatistics = surgeryAvgInfoList.find(_.operationCode == operationCode).map
        {
            case SurgeryAvgInfo(_, amountOfData, surgeryDurationAvgMinutes, restingDurationAvgMinutes, hospitalizationDurationAvgHours) =>
            {
                DoctorStatistics(-1, None, amountOfData, None, surgeryDurationAvgMinutes, restingDurationAvgMinutes, hospitalizationDurationAvgHours)
            }
        }.get
    
        val diffStatisticsMap = doctorsStatisticsDiff(Seq.fill(doctorStatisticsMap.size)(averageStatistics), doctorStatisticsMap.values.toSeq).map
        {
            stat => stat.id -> stat
        }.toMap
        
        PersonalAvgDiffColumnMapper(
            personalMapper = stat => doctorStatisticsMap(stat.id),
            avgMapper = _ => averageStatistics,
            diffMapper = stat => diffStatisticsMap(stat.id),
            )
    }
    
    def generateAvgByDoctorSurgeries() : Seq[DoctorStatistics] =
    {
        doctorsStatistics.map
        {
            doctorStatistics =>
            {
                val doctorSurgeries = surgeryAvgInfoByDoctorMap.getOrElse(doctorStatistics.id, Seq()).map(_.operationCode)
                
                val filteredSurgeryAvgInfoList = surgeryAvgInfoList.filter(surg => doctorSurgeries.contains(surg.operationCode))
                val totalSurgeries = filteredSurgeryAvgInfoList.map(_.amountOfData).sum.toDouble
                
                val surgeryDurationAvgMinutes = filteredSurgeryAvgInfoList.map(surg => surg.amountOfData * surg.surgeryDurationAvgMinutes).sum / totalSurgeries
                val restingDurationAvgMinutes = filteredSurgeryAvgInfoList.map(surg => surg.amountOfData * surg.restingDurationAvgMinutes).sum / totalSurgeries
                val hospitalizationDurationAvgHours = filteredSurgeryAvgInfoList.map(surg => surg.amountOfData * surg.hospitalizationDurationAvgHours).sum / totalSurgeries
    
                doctorStatistics.copy(
                    surgeryDurationAvgMinutes = surgeryDurationAvgMinutes,
                    restingDurationAvgMinutes = restingDurationAvgMinutes,
                    hospitalizationDurationAvgHours = hospitalizationDurationAvgHours,
                    profit = None,
                    )
            }
        }
    }
    
    def doctorsStatisticsDiff(doctorsStatistics1 : Seq[DoctorStatistics], doctorsStatistics2 : Seq[DoctorStatistics]) : Seq[DoctorStatistics] =
    {
        doctorsStatistics1.sortBy(_.id).zip(doctorsStatistics2.sortBy(_.id)).map
        {
            case (avgStatistics, personalStatistics) =>
            {
                DoctorStatistics(
                    personalStatistics.id,
                    personalStatistics.name,
                    personalStatistics.amountOfData,
                    profit = None,
                    avgStatistics.surgeryDurationAvgMinutes - personalStatistics.surgeryDurationAvgMinutes,
                    avgStatistics.restingDurationAvgMinutes - personalStatistics.restingDurationAvgMinutes,
                    avgStatistics.hospitalizationDurationAvgHours - personalStatistics.hospitalizationDurationAvgHours,
                    )
            }
        }
    }
}


object ColumnsNormalNames extends ColumnsNames

trait ColumnsNames
{
    def DOC_NAME = "Doctor Name"
    def AVG_PROFIT = "Profit"
    def AVG_SURGERY = "Surgery Duration (minutes)"
    def AVG_RESTING = "Resting Duration (minutes)"
    def AVG_HOSPITALIZATION = "Hospitalization (hours)"
    def GLOBAL_AVG = "Score"
}


object Cells
{
    def trafficLightsBackgroundCell : TableCell[DoctorStatistics, Int] = new TableCell(new StyledTableCell[Int](Styles.trafficLightsBackground))
    
    def redGreenTextCell : TableCell[DoctorStatistics, Double] = new TableCell(new StyledTableCell[Double](Styles.redGreenText))
    
    private class StyledTableCell[T](getStyle : T => String, valueMapper : T => String = (_ : T).toString) extends javafx.scene.control.TableCell[DoctorStatistics, T]()
    {
        override def updateItem(item : T, empty : Boolean)
        {
            // Always invoke super.
            super.updateItem(item, empty)
            
            if (Option(item).isEmpty || empty)
            {
                setText(null)
            }
            else
            {
                setText(valueMapper(item))
                this.setStyle(getStyle(item))
            }
        }
    }
}

object Styles
{
    val CENTER_BOLD = """-fx-alignment: center;
                        |-fx-font-weight: bolder;""".stripMargin
    
    def centerBoldSize(size : Int) : String =
        s"""$CENTER_BOLD
           |-fx-font-size: $size;""".stripMargin
    
    val TRANSPARENT_GREEN = "rgba(0, 200 ,0 , 0.4)"
    val TRANSPARENT_ORANGE = "rgba(244, 164, 96, 0.4)"
    val TRANSPARENT_RED = "rgba(255, 50, 71, 0.4)"
    def trafficLightsBackground(value : Int) : String =
    {
        val color =
            if(value > 66) TRANSPARENT_GREEN
            else if(value > 33) TRANSPARENT_ORANGE
            else TRANSPARENT_RED
        
        s"""-fx-background-color: $color;
           | -fx-alignment: center;
           | -fx-opacity: 0.7;
           | -fx-font-weight: bolder;""".stripMargin
    }
    
    def redGreenText[T](value : T)(implicit toDouble : T => Double) : String =
    {
        val color =
            if(value >= 0) "forestgreen"
            else "firebrick"
        
        s"-fx-text-fill: $color; -fx-alignment: center;"
    }
}

object ValueMappers
{
    def addPlusSign(value : Double) : String =
    {
        if(value > 0) s"+$value"
        else value.toString
    }
}

class PersonalAvgDiffColumn(valueMapper : DoctorStatistics => Double, initialMapper : PersonalAvgDiffColumnMapper) extends TableColumn[DoctorStatistics, Unit]("")
{
    private var m_personalMapper : DoctorStatistics => DoctorStatistics = identity
    private var m_avgMapper : DoctorStatistics => DoctorStatistics = identity
    private var m_diffMapper : DoctorStatistics => DoctorStatistics = identity

    val avgCol = new Column("Average", m_avgMapper)
    val personalCol = new Column("Personal", m_personalMapper)
    val diffCol = new Column("Diff", m_diffMapper)
    {
        cellFactory = _ => Cells.redGreenTextCell
    }
    
    mapper = initialMapper
    
    columns.addAll(
        avgCol,
        personalCol,
        diffCol,
        )
    
    def mapper = PersonalAvgDiffColumnMapper(m_personalMapper, m_avgMapper, m_diffMapper)
    def mapper_=(mapper : PersonalAvgDiffColumnMapper)
    {
        val PersonalAvgDiffColumnMapper(personalMapper, avgMapper, diffMapper) = mapper
        m_personalMapper = personalMapper
        m_avgMapper = avgMapper
        m_diffMapper = diffMapper
        refreshCellsValueFactory()
    }
    
    def refreshCellsValueFactory()
    {
        Seq(avgCol, personalCol, diffCol).foreach(_.refreshCellValueFactory())
    }
    
    class Column(name : String, mapper : => (DoctorStatistics => DoctorStatistics)) extends TableColumn[DoctorStatistics, Double](name)
    {
        style = "-fx-alignment: center;"
        prefWidth = 100
        refreshCellValueFactory()
        
        def refreshCellValueFactory()
        {
            cellValueFactory = cdf => ObjectProperty(valueMapper(mapper(cdf.value)).double2digits)
        }
    }
}


case class PersonalAvgDiffColumnMapper
(
    personalMapper : DoctorStatistics => DoctorStatistics = identity,
    avgMapper : DoctorStatistics => DoctorStatistics = identity,
    diffMapper : DoctorStatistics => DoctorStatistics = identity
)
