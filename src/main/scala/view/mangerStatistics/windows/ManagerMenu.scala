package view.mangerStatistics.windows

import javafx.event.{ActionEvent, EventHandler}
import scalafx.scene.control.{Menu, MenuBar, MenuItem, RadioMenuItem, ToggleGroup}
import scalafx.stage.Screen

class ManagerMenu(loadCurrentScheduleListener : EventHandler[ActionEvent],
                  loadPastSurgeriesListener : EventHandler[ActionEvent],
                  loadProfitListener : EventHandler[ActionEvent],
                  loadDoctorsIDMappingListener : EventHandler[ActionEvent],
                  loadSurgeryIDMappingListener : EventHandler[ActionEvent],

                  radioBasicInformationListener : EventHandler[ActionEvent],
                  radioImprovementInformationAverageListener : EventHandler[ActionEvent],
                  radioImprovementInformationByOperationListener : EventHandler[ActionEvent],
                 ) extends MenuBar
{
    val fileMenu = new Menu("File")
    
    val loadDataMenu = new Menu("Load Data")
    loadDataMenu.items = List(menuItem("Current Schedule", loadCurrentScheduleListener),
                              menuItem("Past Surgeries", loadPastSurgeriesListener),
                              menuItem("Profit", loadProfitListener),
                              menuItem("Doctors ID Mapping", loadDoctorsIDMappingListener),
                              menuItem("Surgery ID Mapping", loadSurgeryIDMappingListener))
    
    fileMenu.items = List(loadDataMenu)
    
    val modeMenu = new Menu("Mode")
    val menuToggleGroup = new ToggleGroup()
    modeMenu.items = List(radioItem("Basic Information", radioBasicInformationListener, menuToggleGroup, true),
                          radioItem("Improvement Information - Average", radioImprovementInformationAverageListener, menuToggleGroup),
                          radioItem("Improvement Information By Operation", radioImprovementInformationByOperationListener, menuToggleGroup))
    
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
