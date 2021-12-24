package work

import akka.actor.ActorRef
import model.DTOs.{DoctorAvailability, Settings}

trait SettingsWork extends Work

case class AddDoctorAvailabilityWork(doctorAvailability : DoctorAvailability) extends SettingsWork

case class RemoveDoctorAvailabilityWork(doctorAvailability : DoctorAvailability) extends SettingsWork

case class GiveMeSettingsWork(responseTo : Option[ActorRef] = None, settingsOption: Option[Settings] = None) extends SettingsWork

case class SetSettingsWork(settings: Settings) extends SettingsWork
