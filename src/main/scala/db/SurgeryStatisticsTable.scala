package db

import DTOs.SurgeryStatistics
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution

import java.sql.{DriverManager, ResultSet}
import scala.concurrent.Future

class SurgeryStatisticsTable(tableLocation : String = "") extends Table[SurgeryStatistics]
{
    val connection = DriverManager.getConnection(s"jdbc:hsqldb:file:$tableLocation/database/db", "SA", "")
    
    import SurgeryStatisticsTable._
    
    override def tableName : String = "SurgeryStatistics"
    
    override def columns : String =
    {
        s"""
            $surgeonId,
            $operationCode,
            $restingDistribution,
            $hospitalizationDistribution,
            $profit
           """
    }
    
    override def columnsDefinition : String =
    {
        s"""
            $surgeonId $surgeonIdDefinition,
            $operationCode $operationCodeDefinition,
            $restingDistribution $restingDistributionDefinition,
            $hospitalizationDistribution $hospitalizationDistributionDefinition,
            $profit $profitDefinition
           """
    }
    
    override def primaryKey : String = operationCode
    
    override implicit def mapToObject(resultSet : ResultSet) : SurgeryStatistics =
    {
        val restingDistributionObject = resultSet.getObject(restingDistribution).asInstanceOf[EnumeratedIntegerDistribution]
        val hospitalizationDistributionObject  = resultSet.getObject(hospitalizationDistribution).asInstanceOf[EnumeratedIntegerDistribution]
        SurgeryStatistics(resultSet.getDouble(operationCode),
                          restingDistributionObject,
                          hospitalizationDistributionObject,
                          resultSet.getDouble(profit))
    }
    
    override def insert(element : SurgeryStatistics) : Future[Int] = element match
    {
        case SurgeryStatistics(operationCode, restingDistribution, hospitalizationDistribution, avgProfit) =>
        {
            val query = insertPrefix + "(?, ?, ?, ?) ;"
            val stmt = connection.prepareStatement(query)
            stmt.setDouble(1, operationCode)
            stmt.setObject(2, restingDistribution)
            stmt.setObject(3, hospitalizationDistribution)
            stmt.setDouble(4, avgProfit)
            
            executeUpdate(stmt)
        }
    }
}

object SurgeryStatisticsTable
{
    val surgeonId = "surgeonId"
    val surgeonIdDefinition = "INT NOT NULL UNIQUE"
    
    val operationCode = "operationCode"
    val operationCodeDefinition = "REAL NOT NULL UNIQUE"
    
    val restingDistribution = "restingDistribution"
    val restingDistributionDefinition = "OBJECT"
    
    val hospitalizationDistribution = "hospitalizationDistribution"
    val hospitalizationDistributionDefinition = "OBJECT"
    
    val profit = "profit"
    val profitDefinition = "REAL"
}
