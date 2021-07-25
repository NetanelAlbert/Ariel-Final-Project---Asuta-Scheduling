package dbTests

import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._

object Utils
{
    def waitFor[T](future: Future[T], mills : Int = 100)(implicit ec : ExecutionContext) : T =
    {
        Await.result(future, mills millis)
        
    }
    
}
