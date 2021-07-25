package dbTests

import model.DTOs.SurgeonStatisticsAutoAvg
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import Utils._
import model.database.DBConnection


class SurgeonStatisticsTableTest extends FlatSpec with Matchers with BeforeAndAfterAll
{
    implicit val ec = ExecutionContext.global
    
    val db = DBConnection.get(test = true)
    val table = new slick.SurgeonStatisticsTable(db)
    
    val objects = List(
        SurgeonStatisticsAutoAvg(1, "David", 450, 2.543, 6453.34, .3, 54.656),
        SurgeonStatisticsAutoAvg(2, "Avi", 99540, 4.5643, 6.3441, 3643.8, 11.3356),
        SurgeonStatisticsAutoAvg(3, "Dan", 451, 264.5, 613.3534, 33.782, 4.6)
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