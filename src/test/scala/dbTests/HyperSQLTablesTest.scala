package dbTests

import model.db.Table
import model.DTOs.{SurgeonStatistics, SurgeonStatisticsAutoAvg, SurgeryStatistics}
import model.db.Table.testDBLocation
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import model.utils.FileOps

import java.sql.SQLSyntaxErrorException
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

class SurgeryStatisticsTableTest123 extends HyperSQLTablesTest[SurgeryStatistics] with TableInfo[SurgeryStatistics]
{
    override val table = new SurgeryStatisticsTable(testDBLocation)
    override val objects = List(
        SurgeryStatistics(1.2, new EnumeratedIntegerDistribution(Array(1, 1, 2)), new EnumeratedIntegerDistribution(Array(3, 4, 5)), 122),
        SurgeryStatistics(1.4, new EnumeratedIntegerDistribution(Array(2, 1, 2)), new EnumeratedIntegerDistribution(Array(3, 3, 3)), 159),
        SurgeryStatistics(3.66, new EnumeratedIntegerDistribution(Array(5, 5, 9)), new EnumeratedIntegerDistribution(Array(6, 2, 1)), 72.4)
        )
    
    override def assertEqual(list1 : List[SurgeryStatistics], list2 : List[SurgeryStatistics])
    {
        list1.head.restingDistribution.probability(1) shouldBe list2.head.restingDistribution.probability(1)
        list1.last.hospitalizationDistribution.probability(3) shouldBe list2.last.hospitalizationDistribution.probability(3)
    }
}

class SurgeonStatisticsTableTest123 extends HyperSQLTablesTest[SurgeonStatistics] with TableInfo[SurgeonStatistics]
{
    override val table = new SurgeonStatisticsTable(testDBLocation)
    override val objects = List(
        SurgeonStatisticsAutoAvg(1, "David", 450, 2.543, 6453.34, .3, 54.656),
        SurgeonStatisticsAutoAvg(2, "Avi", 99540, 4.5643, 6.3441, 3643.8, 11.3356),
        SurgeonStatisticsAutoAvg(3, "Dan", 451, 264.5, 613.3534, 33.782, 4.6)
    )
//        SurgeryStatistics(1.2, new EnumeratedIntegerDistribution(Array(1, 1, 2)), new EnumeratedIntegerDistribution(Array(3, 4, 5)), 122),
//        SurgeryStatistics(1.4, new EnumeratedIntegerDistribution(Array(2, 1, 2)), new EnumeratedIntegerDistribution(Array(3, 3, 3)), 159),
//        SurgeryStatistics(3.66, new EnumeratedIntegerDistribution(Array(5, 5, 9)), new EnumeratedIntegerDistribution(Array(6, 2, 1)), 72.4)
//        )
    
    override def assertEqual(list1 : List[SurgeonStatistics], list2 : List[SurgeonStatistics])
    {
        list1 shouldBe list2
    }
}

trait TableInfo[T]
{
    val table : Table[T]
    
    val objects : List[T]
    
    def assertEqual(list1 : List[T], list2 : List[T])
}

trait HyperSQLTablesTest[T] extends FlatSpec with Matchers with BeforeAndAfterAll//todo? with TimeLimitedTests
{
    m_self : TableInfo[T] =>
    
    implicit val ec = ExecutionContext.global
    
    it should "initial table" in
    {
        val createTableFuture = table.createTableIfNotExist()
        Await.result(createTableFuture, 100 millis) shouldEqual 0
    }
    
    it should "be empty" in
    {
        val selectAllFuture = table.selectAll()
        val selectAllRes = Await.result(selectAllFuture, 100 millis)
        selectAllRes.length shouldBe 0
    }
    
    it should "insert some rows" in
    {
        val insertFuture = table.insertAll(objects)
        
        Await.result(insertFuture, 100 millis) shouldEqual objects.length
    }
    
    it should "select the rows" in
    {
        val selectAllFuture = table.selectAll()
        val selectAllRes = Await.result(selectAllFuture, 100 millis)
        
        selectAllRes.length shouldBe objects.length
        assertEqual(selectAllRes, objects)
    }
    
    it should "clear the table" in
    {
        val deleteFuture = table.clearTable()
        Await.result(deleteFuture, 100 millis) shouldBe objects.length
    }
    
    it should "select nothing" in
    {
        val selectAllFuture = table.selectAll()
        val selectAllRes = Await.result(selectAllFuture, 100 millis)
        selectAllRes.length shouldBe 0
    }
    
    it should "delete nothing" in
    {
        val deleteFuture = table.clearTable()
        Await.result(deleteFuture, 100 millis) shouldBe 0
    }
    
    it should "close connection" in
    {
        table.closeConnection()
    }
}
