package model.actors

import akka.Done
import akka.pattern.ask
import akka.util.Timeout
import model.DTOs.Settings
import work.{GiveMeSettingsWork, SetSettingsWork}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

case class MismatchException(info : String) extends Throwable(info)

trait SettingsAccess
{
    this : MyActor =>
    
    implicit def ec : ExecutionContext
    implicit val m_timeOut = Timeout(1 second)
    
    val m_settingActor = context.actorOf(SettingsActor.props(), "SettingsActor")
    
    def getSettings : Future[Settings] =
    {
        val settingsFuture = m_settingActor ? GiveMeSettingsWork()
        settingsFuture.transform
        {
            case Success(settings : Settings) => Success(settings)
    
            case Success(value) =>
            {
                m_logger.info(s"expected ${Success.getClass} but got ${value.getClass}")
                Failure(MismatchException(s"Expected Settings but found $value"))
            }
            
            case Failure(exception) => Failure(exception)
        }
    }
    
    def setSettings(settings: Settings) : Future[Done] =
    {
        val setSettingsFuture = m_settingActor ? SetSettingsWork(settings)
        setSettingsFuture.map(_ => Done)
    }
}
