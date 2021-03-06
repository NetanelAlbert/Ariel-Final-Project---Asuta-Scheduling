package model.actors

import akka.actor.{ActorRef, Props}
import work.{FileWork, GetDataWork, GetOptionsForFreeBlockWork}

import scala.concurrent.ExecutionContext

class ModelManager(m_controller : ActorRef)(implicit override val ec : ExecutionContext) extends MyActor with SettingsAccess
{
    val m_databaseActor = context.actorOf(DatabaseActor.props(m_controller, self), "DatabaseActor")
    val m_analyzeDataActor = context.actorOf(AnalyzeDataActor.props(m_controller, self, m_databaseActor), "AnalyzeDataActor")
    val m_fileActor = context.actorOf(FileActor.props(m_controller, self, m_databaseActor, m_analyzeDataActor), "FileActor")
    
    override def receive : Receive =
    {
        case work : FileWork => m_fileActor ! work
        
        case work : GetOptionsForFreeBlockWork if work.surgeryStatistics.nonEmpty => m_analyzeDataActor ! work
        
        case work : GetDataWork => m_databaseActor ! work
    }
    
}

object ModelManager
{
    def props(m_controller : ActorRef)(implicit ec : ExecutionContext) : Props = Props(new ModelManager(m_controller))
}
