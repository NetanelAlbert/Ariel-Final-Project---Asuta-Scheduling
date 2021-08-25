package view.common

import akka.actor.{ActorRef, Props}
import model.actors.MyActor
import view.mangerStatistics.{StatisticsMainWindowActions, StatisticsWindowManagerActor}
import view.schduling.{SchedulingMainWindowActions, SchedulingWindowManagerActor}
import work.{GetDoctorsStatisticsWork, TellAboutSettingsActorWork, WorkFailure, WorkSuccess}

object ViewManager
{
    def props(m_controller : ActorRef, mainWindow : MainWindowActions) : Props = Props(new ViewManager(m_controller, mainWindow))
}

class ViewManager(m_controller : ActorRef, mainWindow : MainWindowActions) extends MyActor
{
    val m_windowManagerActor = mainWindow match
    {
        case window : StatisticsMainWindowActions => context.actorOf(StatisticsWindowManagerActor.props(m_controller, window), "StatisticsWindowManager")
        
        case window : SchedulingMainWindowActions => context.actorOf(SchedulingWindowManagerActor.props(m_controller, window), "SchedulingWindowManager")
    }
    
    override def receive =
    {
        case work => m_windowManagerActor ! work
    }
    
}
