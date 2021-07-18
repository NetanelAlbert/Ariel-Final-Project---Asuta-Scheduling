package db

import DTOs.SurgeonStatistics

import java.sql.{DriverManager, ResultSet}
import scala.concurrent.Future

class SurgeonStatisticsTable(tableLocation : String = "") extends Table[SurgeonStatistics]
{
    import SurgeonStatisticsTable._
    val connection = DriverManager.getConnection(s"jdbc:hsqldb:file:$tableLocation/database/db", "SA", "")
    
    
    override def tableName : String = "SurgeonStatistics"
    
    override def columns : String =
    {
        s"""$id,
            $name,
            $amountOfData,
            $surgeryDurationAvgMinutes,
            $restingDurationAvgMinutes,
            $hospitalizationDurationAvgHours,
            $globalAvg"""
    }
    
    override def columnsDefinition : String =
    {
        s"""$id $idDefinition,
            $name $nameDefinition,
            $amountOfData $amountOfDataDefinition,
            $profitAvg $profitAvgDefinition,
            $surgeryDurationAvgMinutes $surgeryDurationAvgMinutesDefinition,
            $restingDurationAvgMinutes $restingDurationAvgMinutesDefinition,
            $hospitalizationDurationAvgHours $hospitalizationDurationAvgHoursDefinition,
            $globalAvg $globalAvgDefinition"""
    }
    
    override def primaryKey : String = id
    
    override implicit def mapToObject(resultSet : ResultSet) : SurgeonStatistics =
    {
        SurgeonStatistics(
            resultSet.getInt(id),
            resultSet.getString(name),
            resultSet.getInt(amountOfData),
            resultSet.getDouble(profitAvg),
            resultSet.getDouble(surgeryDurationAvgMinutes),
            resultSet.getDouble(restingDurationAvgMinutes),
            resultSet.getDouble(hospitalizationDurationAvgHours),
            resultSet.getDouble(globalAvg)
        )
    }
    
    override def insert(element : SurgeonStatistics) : Future[Int] = element match
    {
        case SurgeonStatistics(id, name, amountOfData, profitAvg, surgeryDurationAvgMinutes, restingDurationAvgMinutes, hospitalizationDurationAvgHours, globalAvg) =>
        {
            val query = insertPrefix + "(?, ?, ?, ?, ?, ?, ?, ?) ;"
            val stmt = connection.prepareStatement(query)
            stmt.setInt(1, id)
            stmt.setString(2, name)
            stmt.setInt(3, amountOfData)
            stmt.setDouble(4, profitAvg)
            stmt.setDouble(5, surgeryDurationAvgMinutes)
            stmt.setDouble(6, restingDurationAvgMinutes)
            stmt.setDouble(7, hospitalizationDurationAvgHours)
            stmt.setDouble(8, globalAvg)
            executeUpdate(stmt)
        }
    }
}

object SurgeonStatisticsTable
{
    val id = "id"
    val idDefinition = "INTEGER NOT NULL UNIQUE"
    
    val name = "name"
    val nameDefinition = "VARCHAR(45)"
    
    val amountOfData = "amountOfData"
    val amountOfDataDefinition = "INTEGER"
    
    val profitAvg = "profitAvg"
    val profitAvgDefinition = "REAL"
    
    val surgeryDurationAvgMinutes = "surgeryDurationAvgMinutes"
    val surgeryDurationAvgMinutesDefinition = "REAL"
    
    val restingDurationAvgMinutes = "restingDurationAvgMinutes"
    val restingDurationAvgMinutesDefinition = "REAL"
    
    val hospitalizationDurationAvgHours = "hospitalizationDurationAvgHours"
    val hospitalizationDurationAvgHoursDefinition = "REAL"
    
    val globalAvg = "globalAvg"
    val globalAvgDefinition = "REAL"
}
