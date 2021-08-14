package unit.dbTests

import model.DTOs.DoctorAvailability
import model.database.{DBConnection, DoctorAvailabilityTable}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import unit.dbTests.Utils._

import scala.concurrent.ExecutionContext
import scala.language.postfixOps


class DoctorAvailabilityTableTest extends FlatSpec with Matchers with BeforeAndAfterAll
{
    import ExecutionContext.Implicits.global
    
    val db = DBConnection.get(test = true)
    val table = new DoctorAvailabilityTable(db)
    
    val objects = Set(
        DoctorAvailability(1, 2),
        DoctorAvailability(1, 1),
        DoctorAvailability(1, 7),
        DoctorAvailability(2,2),
        DoctorAvailability(2,4),
        DoctorAvailability(2,6),
        DoctorAvailability(3,2),
        DoctorAvailability(3,5),
        DoctorAvailability(3,7),
        DoctorAvailability(4,3),
        DoctorAvailability(4,1),
        DoctorAvailability(4,1),
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
        
        waitFor(insertFuture) shouldEqual objects.size
    }
    
    it should "select the rows" in
    {
        val allData = waitFor(table.selectAll())
    
        allData.length shouldBe objects.size
        allData.toSet shouldBe objects
    }
    
    it should "select the doctors works in Monday" in
    {
        val doctorsIDs = waitFor(table.getAvailableDoctorsIDs(2))

        doctorsIDs.toSet shouldBe objects.filter(_.day == 2).map(_.doctorId)
    }
    
    it should "clear the table" in
    {
        waitFor(table.clear()) shouldBe objects.size
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