package model.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import work.{Work, WorkFailure}

trait MyActor extends Actor with ActorLogging
{
    implicit val m_logger = log
    
    //    def failureResponseActor : ActorRef
    
    
    final override def unhandled(message : Any)
    {
        val errorInfo = s"unhandled message: $message. from: $sender"
        
        message match {
//            case work : Work => //TODO
//                failureResponseActor ! WorkFailure(work, None, Some(errorInfo))

            case _ => super.unhandled(message)
        }
    }
}
