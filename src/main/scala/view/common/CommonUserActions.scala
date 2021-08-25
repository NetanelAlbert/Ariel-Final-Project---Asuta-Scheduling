package view.common

import akka.actor.{ActorRef}
import model.actors.MyActor
import work.{ReadDoctorsMappingExcelWork, ReadPastSurgeriesExcelWork, ReadProfitExcelWork, ReadSurgeryMappingExcelWork}

import java.io.File

trait CommonUserActions
{
    this : MyActor =>
    
    val m_controller : ActorRef
    
    def loadPastSurgeriesListener(file : File, keepOldMapping : Boolean) : Unit = m_controller ! ReadPastSurgeriesExcelWork(file, keepOldMapping)
    
    def loadProfitListener(file : File) : Unit = m_controller ! ReadProfitExcelWork(file)
    
    def loadDoctorsIDMappingListener(file : File) : Unit = m_controller ! ReadDoctorsMappingExcelWork(file)
    
    def loadSurgeryIDMappingListener(file : File) : Unit = m_controller ! ReadSurgeryMappingExcelWork(file)
    
    def reloadDefaultData
}