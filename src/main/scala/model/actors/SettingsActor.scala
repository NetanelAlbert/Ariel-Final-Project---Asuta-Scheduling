package model.actors

import akka.Done
import akka.actor.{ActorRef, Props}
import model.DTOs.Priority.Priority
import model.DTOs._
import model.actors.SettingsActor.SettingsException
import model.database._
import work._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object SettingsActor
{
    def props()(implicit ec : ExecutionContext) : Props = Props(new SettingsActor())
    
    case class SettingsException(cause : Throwable, info : String) extends Throwable
}

class SettingsActor()(implicit ec : ExecutionContext) extends MyActor
{
    val m_db = DBConnection.get()
    
    val doctorAvailabilityTable = new DoctorAvailabilityTable(m_db)
    val settingsTable = new SettingsTable(m_db)
    val doctorStatisticsTable = new DoctorStatisticsTable(m_db)
    
    override def receive =
    {
        case work @ AddDoctorAvailabilityWork(doctorAvailability) => addDoctorAvailabilityWork(work, doctorAvailability)
        
        case work @ RemoveDoctorAvailabilityWork(doctorAvailability) => removeDoctorAvailabilityWork(work, doctorAvailability)
        
        case work @ GiveMeSettingsWork(responseTo, _) => giveMeSettingsWork(work, responseTo)
        
        case work @ SetSettingsWork(settings) => setSettingsWork(work, settings)

        case UpdateDoctorPriority(doctorID, priority) => updateDoctorPriority(doctorID, priority)

        case UpdateDoctorsPriority(doctorsPriority) => updateDoctorsPriority(doctorsPriority)

        case GetDoctorPriorityAndName => getDoctorPriorityAndName()
    }
    
    def addDoctorAvailabilityWork(work : AddDoctorAvailabilityWork, doctorAvailability : DoctorAvailability)
    {
        val responseActor = sender()
    
        doctorAvailabilityTable.insertIfNotExist(doctorAvailability).onComplete
        {
            case Success(0) => responseActor ! Success(work, Some("I already knew that"))
    
            case Success(_) => responseActor ! Success(work, Some("Thank for the new info"))
    
            case Failure(exception) => responseActor ! WorkFailure(work, Some(exception), Some("Error while trying to save working info"))
        }
    }
    
    def removeDoctorAvailabilityWork(work : RemoveDoctorAvailabilityWork, doctorAvailability : DoctorAvailability)
    {
        val responseActor = sender()
    
        doctorAvailabilityTable.deleteRow(doctorAvailability).onComplete
        {
            case Success(_) => responseActor ! Success(work, Some("Thank for the new info. It removed."))
    
            case Failure(exception) => responseActor ! WorkFailure(work, Some(exception), Some("Error while trying to remove working info"))
        }
    }
    
    def giveMeSettingsWork(work : GiveMeSettingsWork, responseTo : Option[ActorRef])
    {
        val responseActor = responseTo.getOrElse(sender())
        
        settingsTable.get.onComplete
        {
            case Success(settings) => responseActor ! settings
    
            case Failure(exception) => responseActor ! akka.actor.Status.Failure(WorkFailure(work, Some(exception), Some("Can't fetch settings from DB")))
        }
    }
    
    def setSettingsWork(work : SetSettingsWork, settings : Settings)
    {
        val responseActor = sender()
    
        settingsTable.set(settings).onComplete
        {
            case Success(_) => responseActor ! Success(work)
        
            case Failure(exception) => responseActor ! Failure(SettingsException(exception, "Can't set new settings in DB"))
        }
    }
    
    def updateDoctorPriority(doctorID : Int, priority : Priority)
    {
        updateDoctorsPriority(Seq(doctorID -> priority))
    }
    
    def updateDoctorsPriority(doctorsPriority : Iterable[(Int, Priority)])
    {
        val responseActor = sender()
        doctorStatisticsTable.setDoctorsPriority(doctorsPriority).onComplete
        {
            case Success(_) => responseActor ! Done
            case Failure(exception) => responseActor ! akka.actor.Status.Failure(exception)
        }
    }
    
    def getDoctorPriorityAndName()
    {
        val responseActor = sender()
        doctorStatisticsTable.getDoctorsPriorityAndNames().onComplete
        {
            case Success(doctorPriorityAndNames) => responseActor ! doctorPriorityAndNames
            case Failure(exception) => responseActor ! akka.actor.Status.Failure(exception)
        }
    }
}
