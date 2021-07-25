package dbTests

import DTOs.SurgeryStatistics
import db.Utils._
import db.slick.DBConnection
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext
import scala.language.postfixOps


class SurgeryStatisticsTableTest extends FlatSpec with Matchers with BeforeAndAfterAll
{
    implicit val ec = ExecutionContext.global
    
    val db = DBConnection.get(test = true)
    val table = new slick.SurgeryStatisticsTable(db)
    
    val objects = List(
        SurgeryStatistics(1.2, new EnumeratedIntegerDistribution(Array(1, 1, 2)), new EnumeratedIntegerDistribution(Array(3, 4, 5)), 122),
        SurgeryStatistics(1.4, new EnumeratedIntegerDistribution(Array(2, 1, 2)), new EnumeratedIntegerDistribution(Array(3, 3, 3)), 159),
        SurgeryStatistics(3.66, new EnumeratedIntegerDistribution(Array(5, 5, 9)), new EnumeratedIntegerDistribution(Array(6, 2, 1)), 72.4)
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
        allData.head.restingDistribution.probability(1) shouldBe objects.head.restingDistribution.probability(1)
        allData.last.hospitalizationDistribution.probability(3) shouldBe objects.last.hospitalizationDistribution.probability(3)
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