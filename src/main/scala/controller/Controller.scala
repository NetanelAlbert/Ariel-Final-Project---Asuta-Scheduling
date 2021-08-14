package controller

import akka.actor.Props
import model.actors.{ModelManager, MyActor}
import view.mangerStatistics.actors.ViewManager
import view.mangerStatistics.windows.MainWindowActions
import work.{GetDoctorsStatisticsWork, ReadPastSurgeriesExcelWork, WorkSuccess}

import scala.concurrent.ExecutionContext

object Controller
{
    def props(mainWindow : MainWindowActions) : Props = Props(new Controller(mainWindow))
}

class Controller(mainWindow : MainWindowActions) extends MyActor
{
    import ExecutionContext.Implicits.global
    
    val m_viewManager = context.actorOf(ViewManager.props(self, mainWindow), "viewManager")
    val m_modelManager = context.actorOf(ModelManager.props(self), "modelManager")
    
    override def receive : Receive =
    {
//        case message : WorkSuccess =>
//            m_logger.debug(s"Received message : $message from Model Manager")
//            m_viewManager ! message
//            m_modelManager ! GetDoctorsStatisticsWork()
            
        case message if sender.path.toString.contains("modelManager") =>
            m_logger.debug(s"Received message : $message from Model Manager")
            m_viewManager ! message
        case message if sender.path.toString.startsWith("viewManager") =>
            m_logger.debug(s"Received message : $message from View Manager")
            m_modelManager ! message
    }
    
//    m_modelManager ! ReadPastSurgeriesExcelWork("SurgeriesData.xlsx")
    m_modelManager ! GetDoctorsStatisticsWork()
    
}
