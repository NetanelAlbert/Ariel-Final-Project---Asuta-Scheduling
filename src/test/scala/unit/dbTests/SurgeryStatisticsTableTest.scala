package unit.dbTests

import model.DTOs.SurgeryStatistics
import model.database.{DBConnection, SurgeryStatisticsTable}
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import Utils._
import model.probability.IntegerDistribution


class SurgeryStatisticsTableTest extends FlatSpec with Matchers with BeforeAndAfterAll
{
    implicit val ec = ExecutionContext.global
    
    val db = DBConnection.get(test = true)
    val table = new SurgeryStatisticsTable(db)
    
    val objects = List(
        SurgeryStatistics(1.2, Some("aaa"), IntegerDistribution(Seq(1, 1, 2)), IntegerDistribution(Seq(3, 4, 5)), Some(122), 432),
        SurgeryStatistics(1.4, Some("bbb"), IntegerDistribution(Seq(2, 1, 2)), IntegerDistribution(Seq(3, 3, 3)), Some(159), 4334),
        SurgeryStatistics(3.66, Some("ccc"), IntegerDistribution(Seq(5, 5, 9)), IntegerDistribution(Seq(6, 2, 1)), None, 23)
        )
    
    it should "initial table" in
    {
        val createTableFuture = table.create()
        waitFor(createTableFuture, 1000)
    }
    
    it should "be empty" in
    {
        val allData = waitFor(table.selectAll())
        allData.length shouldBe 0
    }
    
    it should "insert some rows" in
    {
        val insertFuture = table.insertAll(objects)
        
        waitFor(insertFuture) shouldEqual Some(objects.length)
    }
    
    it should "select the rows" in
    {
        val allData = waitFor(table.selectAll())
    
        allData.length shouldBe objects.length
        allData shouldBe objects
    }
    
    it should "clear the table" in
    {
        waitFor(table.clear()) shouldBe objects.length
    }
    
    it should "select nothing" in
    {
        val allData = waitFor(table.selectAll())
    
        allData.length shouldBe 0
    }
    
    it should "delete nothing" in
    {
        waitFor(table.clear()) shouldBe 0
    }
    
    it should "close connection" in
    {
        db.close()
    }
}