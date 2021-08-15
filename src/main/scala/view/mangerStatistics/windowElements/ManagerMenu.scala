package view.mangerStatistics.windowElements

import javafx.event.{ActionEvent, EventHandler}
import scalafx.scene.control._
import scalafx.stage.Screen
import view.mangerStatistics.windowElements.ManagerMenu.Modes._

class ManagerMenu(loadPastSurgeriesListener : EventHandler[ActionEvent],
                  loadProfitListener : EventHandler[ActionEvent],
                  loadDoctorsIDMappingListener : EventHandler[ActionEvent],
                  loadSurgeryIDMappingListener : EventHandler[ActionEvent],

                  radioBasicInformationListener : EventHandler[ActionEvent],
                  radioImprovementInformationAverageListener : EventHandler[ActionEvent],
                  radioImprovementInformationByOperationListener : EventHandler[ActionEvent],
                 ) extends MenuBar
{
    val fileMenu = new Menu("File")
    
    val loadDataMenu = new Menu("Load Configurations")
    loadDataMenu.items = List(menuItem("Past Surgeries", loadPastSurgeriesListener),
                              menuItem("Profit", loadProfitListener),
                              menuItem("Doctors ID Mapping", loadDoctorsIDMappingListener),
                              menuItem("Surgery ID Mapping", loadSurgeryIDMappingListener))
    
    fileMenu.items = List(loadDataMenu)
    
    val modeMenu = new Menu("Mode")
    val menuToggleGroup = new ToggleGroup()
    modeMenu.items = List(radioItem(BASIC, radioBasicInformationListener, menuToggleGroup, true),
                          radioItem(IMPROVE_AVG, radioImprovementInformationAverageListener, menuToggleGroup),
                          radioItem(IMPROVE_BY_OP, radioImprovementInformationByOperationListener, menuToggleGroup))
    
    menus = List(fileMenu, modeMenu)
    prefWidth = Screen.primary.bounds.width
    
    def menuItem(name : String, action : EventHandler[ActionEvent]) : MenuItem =
    {
        val item = new MenuItem(name)
        item.onAction = action
        item
    }
    
    def radioItem(name : String, action : EventHandler[ActionEvent], toggleGroup : ToggleGroup, select : Boolean = false) : RadioMenuItem =
    {
        val item = new RadioMenuItem(name)
        item.onAction = action
        item.toggleGroup = toggleGroup
        item.selected = select
        item
    }
}

object ManagerMenu
{
    object Modes
    {
        val BASIC = "Basic Information"
        val IMPROVE_AVG = "Improvement Information - Average"
        val IMPROVE_BY_OP = "Improvement Information By Operation"
    }
}
