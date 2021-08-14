package view.mangerStatistics.actors

import akka.actor.{ActorRef, Props}
import model.actors.MyActor
import view.mangerStatistics.windows.MainWindowActions
import work.{WorkFailure, WorkSuccess}

class ViewManager(m_controller : ActorRef, mainWindow : MainWindowActions) extends MyActor
{
    val m_windowManagerActor = context.actorOf(WindowManagerActor.props(m_controller, mainWindow), "windowManagerActor")
    
    override def receive =
    {
        //case message => m_windowManagerActor ! message

        case status @ WorkSuccess(work, _) => m_windowManagerActor ! work
        
        case status @ WorkFailure(work, _, _) => println(s"! ! ! ViewManager got WorkFailure: ${work.getClass}")
    }
    
}

object ViewManager
{
    def props(m_controller : ActorRef, mainWindow : MainWindowActions) : Props = Props(new ViewManager(m_controller, mainWindow))
}
