package model.database

import scala.concurrent.Future

trait BaseDB[T]
{
    def create() : Future[Unit]
    
    def insert(element : T) : Future[Int]
    
    def insertAll(elements : Seq[T]) : Future[Int] =
    {
        import scala.concurrent.ExecutionContext.Implicits.global
        val insertFutureSeq = elements.map(insert)
        Future.sequence(insertFutureSeq).map(_.sum)
    }
    
    def selectAll() : Future[Seq[T]]
    
    def clear() : Future[Int]
}
