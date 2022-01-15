package work

import akka.actor.ActorRef
import model.DTOs.Priority.Priority
import model.DTOs.{DoctorAvailability, Settings}

trait SettingsWork extends Work

//TODO use in UI
case class AddDoctorAvailabilityWork(doctorAvailability : DoctorAvailability) extends SettingsWork

//TODO use in UI
case class RemoveDoctorAvailabilityWork(doctorAvailability : DoctorAvailability) extends SettingsWork

case class GiveMeSettingsWork(responseTo : Option[ActorRef] = None, settingsOption: Option[Settings] = None) extends SettingsWork

case class SetSettingsWork(settings: Settings) extends SettingsWork

case class UpdateDoctorPriority(doctorID : Int, priority : Priority) extends SettingsWork

case class UpdateDoctorsPriority(doctorsPriority : Iterable[(Int, Priority)]) extends SettingsWork

case object GetDoctorPriorityAndName extends SettingsWork
