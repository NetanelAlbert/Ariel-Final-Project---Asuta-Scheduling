package work

import model.DTOs.{DoctorAvailability, Settings}

trait SettingsWork extends Work

case class AddDoctorAvailabilityWork(doctorAvailability : DoctorAvailability) extends SettingsWork

case class RemoveDoctorAvailabilityWork(doctorAvailability : DoctorAvailability) extends SettingsWork

case class GetSettingsWork(settingsOption: Option[Settings]) extends SettingsWork

case class SetSettingsWork(settings: Settings) extends SettingsWork
