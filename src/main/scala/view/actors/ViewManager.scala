package view.actors

import akka.actor.{ActorRef, Props}
import model.actors.MyActor
import view.mangerStatistics.MainWindowActions
import work.{GetDoctorsStatisticsWork, TellAboutSettingsActorWork, WorkFailure, WorkSuccess}

class ViewManager(m_controller : ActorRef, mainWindow : MainWindowActions) extends MyActor
{
    val m_windowManagerActor = context.actorOf(WindowManagerActor.props(m_controller, mainWindow), "windowManagerActor")
    
    override def receive =
    {
        //case message => m_windowManagerActor ! message
        case WorkSuccess(work : GetDoctorsStatisticsWork, _) => m_windowManagerActor ! work

        case work : TellAboutSettingsActorWork => m_windowManagerActor ! work
        
        case status @ WorkSuccess(work, message) => mainWindow.showSuccessDialog(message.getOrElse("Action succeed"))
        
        case status @ WorkFailure(work, cause, message) =>
        {
            val messageToShow = message.getOrElse(cause.map(_.getMessage).getOrElse("Action failed"))
            mainWindow.showFailDialog(messageToShow)
        }
    }
    
}

object ViewManager
{
    def props(m_controller : ActorRef, mainWindow : MainWindowActions) : Props = Props(new ViewManager(m_controller, mainWindow))
}
