package view.mangerStatistics.windowElements

import model.DTOs.DoctorStatistics
import scalafx.beans.property.StringProperty
import scalafx.scene.control.TableColumn

object Columns
{
    val nameCol = new TableColumn[DoctorStatistics, String]("Doctor Name")
    val profitAvgCol = new TableColumn[DoctorStatistics, Double]("Average Profit")
    val surgeryDurationAvgMinutesCol = new TableColumn[DoctorStatistics, Double]("Average Surgery Duration (minutes)")
    val restingDurationAvgMinutesCol = new TableColumn[DoctorStatistics, Double]("Average Resting Duration (minutes)")
    val hospitalizationDurationAvgHoursCol = new TableColumn[DoctorStatistics, Double]("Average Hospitalization Duration (hours)")
    val globalAvgCol = new TableColumn[DoctorStatistics, Double]("Global Average")
    
    def setColumnsMappers(mapper : TableSceneBaseMappers)
    {
        nameCol.cellValueFactory = sdf => StringProperty(sdf.value.nameOrId)
        profitAvgCol.cellValueFactory = mapper.profitAvgColMapper
        surgeryDurationAvgMinutesCol.cellValueFactory = mapper.surgeryDurationAvgMinutesColMapper
        restingDurationAvgMinutesCol.cellValueFactory = mapper.restingDurationAvgMinutesColMapper
        hospitalizationDurationAvgHoursCol.cellValueFactory = mapper.hospitalizationDurationAvgHoursColMapper
        globalAvgCol.cellValueFactory = mapper.globalAvgColMapper
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
    
    val columns = List[javafx.scene.control.TableColumn[DoctorStatistics, _]](nameCol,
                                                                              profitAvgCol,
                                                                              surgeryDurationAvgMinutesCol,
                                                                              restingDurationAvgMinutesCol,
                                                                              hospitalizationDurationAvgHoursCol,
                                                                              globalAvgCol)
    
    setColumnsMappers(new TableSceneNormalMappers)
    setColumnsNames(ColumnsNormalNames)
}


object ColumnsNormalNames extends ColumnsNames

trait ColumnsNames
{
    def DOC_NAME = "Doctor Name"
    def AVG_PROFIT = "Average Profit"
    def AVG_SURGERY = "Average Surgery Duration (minutes)"
    def AVG_RESTING = "Average Resting Duration (minutes)"
    def AVG_HOSPITALIZATION = "Average Hospitalization (hours)"
    def GLOBAL_AVG = "Global Average"
}

object ColumnsAvgNames extends ColumnsNames
{
    val DIFF = "\nDifference from average"
    override def AVG_SURGERY = super.AVG_SURGERY + DIFF
    override def AVG_RESTING = super.AVG_RESTING + DIFF
    override def AVG_HOSPITALIZATION = super.AVG_HOSPITALIZATION + DIFF
    override def GLOBAL_AVG = super.GLOBAL_AVG + DIFF
}