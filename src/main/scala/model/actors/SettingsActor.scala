package model.actors

import akka.actor.{ActorRef, Props}
import model.DTOs._
import model.database._
import work._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object SettingsActor
{
    def props(m_controller : ActorRef, m_modelManager : ActorRef)(implicit ec : ExecutionContext) : Props = Props(new SettingsActor(m_controller, m_modelManager))
}

class SettingsActor(m_controller : ActorRef, m_modelManager : ActorRef)(implicit ec : ExecutionContext) extends MyActor
{
    val m_db = DBConnection.get()
    
    val doctorAvailabilityTable = new DoctorAvailabilityTable(m_db)
    val settingsTable = new SettingsTable(m_db)
    
    override def receive =
    {
        case work @ AddDoctorAvailabilityWork(doctorAvailability) => addDoctorAvailabilityWork(work, doctorAvailability)
        
        case work @ RemoveDoctorAvailabilityWork(doctorAvailability) => removeDoctorAvailabilityWork(work, doctorAvailability)
        
        case work @ GiveMeSettingsWork(responseTo, _) => giveMeSettingsWork(work, responseTo.getOrElse(sender()))
        
        case work @ SetSettingsWork(settings) => setSettingsWork(work, settings)
    }
    
    def addDoctorAvailabilityWork(work : AddDoctorAvailabilityWork, doctorAvailability : DoctorAvailability)
    {
        doctorAvailabilityTable.insertIfNotExist(doctorAvailability).onComplete
        {
            case Success(0) => m_controller ! WorkSuccess(work, Some("I already knew that"))
    
            case Success(_) => m_controller ! WorkSuccess(work, Some("Thank for the new info"))
    
            case Failure(exception) => m_controller ! WorkFailure(work, Some(exception), Some("Error while trying to save working info"))
        }
    }
    
    def removeDoctorAvailabilityWork(work : RemoveDoctorAvailabilityWork, doctorAvailability : DoctorAvailability)
    {
        doctorAvailabilityTable.deleteRow(doctorAvailability).onComplete
        {
            case Success(_) => m_controller ! WorkSuccess(work, Some("Thank for the new info. It removed."))
    
            case Failure(exception) => m_controller ! WorkFailure(work, Some(exception), Some("Error while trying to remove working info"))
        }
    }
    
    def giveMeSettingsWork(work : GiveMeSettingsWork, responseTo : ActorRef)
    {
        settingsTable.get.onComplete
        {
            case Success(settings) => responseTo ! WorkSuccess(work.copy(settingsOption = Some(settings)), None)
    
            case Failure(exception) => responseTo ! WorkFailure(work, Some(exception), Some("Can't fetch settings from DB"))
        }
    }
    
    def setSettingsWork(work : SetSettingsWork, settings : Settings)
    {
        settingsTable.set(settings).onComplete
        {
            case Success(_) => m_controller ! WorkSuccess(work, None)
        
            case Failure(exception) => m_controller ! WorkFailure(work, Some(exception), Some("Can't set new settings in DB"))
        }
    }
}
