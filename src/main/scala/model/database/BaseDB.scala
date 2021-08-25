package model.database

import scala.concurrent.Future

trait BaseDB[T]
{
    def create() : Future[Unit]
    
    def insert(element : T) : Future[Int]
    
    def insertAll(elements : Iterable[T]) : Future[Option[Int]]

    def selectAll() : Future[Seq[T]]
    
    def clear() : Future[Int]
}
