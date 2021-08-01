package model.actors

import akka.actor.{Actor, ActorLogging}

trait MyActor extends Actor with ActorLogging
{
    implicit val m_logger = log
}
