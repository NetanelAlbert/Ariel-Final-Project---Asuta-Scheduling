package db


import java.io.{ByteArrayOutputStream, ObjectOutputStream, Serializable}
import java.sql.{Blob, Connection, DriverManager, PreparedStatement, ResultSet}
import javax.sql.rowset.serial.SerialBlob
import scala.concurrent.{ExecutionContext, Future}

trait Table[T]
{
    import db.Table._
    implicit val ec = ExecutionContext.global
    
    val connection : Connection
    
    def tableName : String
    
    def columns : String
    
    def columnsDefinition : String
    
    def primaryKey : String
    
    implicit def mapToObject(resultSet: ResultSet) : T
    
    def createTableIfNotExist() : Future[Int] =
    {
        println(s"creating table '$tableName'")
        val query = s"CREATE TABLE IF NOT EXISTS $tableName ($columnsDefinition) ;"
        executeUpdate(query)
    }
    
    def selectAll(orderBy : Option[String] = None, descend : String = "") : Future[List[T]] =
    {
        val queryOrder = orderBy match
        {
            case Some(order) => s"$ORDER_BY $order $descend"

            case _ => ""
        }
        val query = s"$SELECT $ALL $FROM $tableName $queryOrder ;"
        
        executeQuery(query).map(_.map[T])
    }
    
    def insertPrefix : String = {s"INSERT INTO $tableName VALUES "}
    
    def insert(element : T) : Future[Int]
    
    def insertAll(elements : List[T]) : Future[Int] =
    {
        Future.sequence(elements.map(insert)).map(_.sum)
    }
    
    def executeQuery(query : String) : Future[ResultSet] =
    {
        val pst = connection.prepareStatement(query)
        executeQuery(pst)
    }
    
    def executeQuery(pst : PreparedStatement) : Future[ResultSet] =
    {
        Future
        {
            pst.clearParameters()
            pst.executeQuery()
        }
    }
    
    def executeUpdate(query : String) : Future[Int] =
    {
        val pst = connection.prepareStatement(query)
        executeUpdate(pst)
    }
    
    def executeUpdate(pst : PreparedStatement) : Future[Int] =
    {
        Future
        {
            //pst.clearParameters()
            //println(s"executeUpdate pst = $pst")
            pst.executeUpdate()
        }
    }
    
    def clearTable() : Future[Int] =
    {
        val query = s"DELETE FROM $tableName;"
        executeUpdate(query)
    }
    
    def closeConnection()
    {
        connection.close()
    }
    
//    def blob[B <: Serializable](obj : B) : Blob =
//    {
//        val bos = new ByteArrayOutputStream()
//        val out = new ObjectOutputStream(bos)
//        out.writeObject(obj)
//        out.flush()
//        new SerialBlob(bos.toByteArray)
//    }
}

object Table
{
    val testDBLocation = "src/test/resources"
    //val connection = DriverManager.getConnection(s"jdbc:hsqldb:file:$tableLocation/db", "SA", "")
    
    val SELECT = "SELECT"
    val FROM = "FROM"
    val ALL = "*"
    val ORDER_BY = "ORDER BY"
    val DESCEND = "DESC"
    
    implicit class IterableResultSet(resultSet: ResultSet)
    {
        def map[A](implicit mapping : ResultSet => A) : List[A] =
        {
            val list = collection.mutable.ListBuffer[A]()
            while (resultSet.next())
            {
                list += mapping(resultSet)
            }
            list.toList
        }
    }
}
