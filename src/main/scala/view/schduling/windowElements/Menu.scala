package view.schduling.windowElements

import javafx.event.{ActionEvent, EventHandler}
import scalafx.scene.control._
import scalafx.stage.Screen

class SchedulingMenu(loadPastSurgeriesListener : EventHandler[ActionEvent],
                     loadProfitListener : EventHandler[ActionEvent],
                     loadDoctorsIDMappingListener : EventHandler[ActionEvent],
                     loadSurgeryIDMappingListener : EventHandler[ActionEvent],
                    ) extends MenuBar
{
    prefWidth = Screen.primary.bounds.width
    
    val fileMenu = new Menu("File")
    
    val loadDataMenu = new Menu("Load Configurations")
    loadDataMenu.items = List(menuItem("Past Surgeries", loadPastSurgeriesListener),
                                          menuItem("Profit", loadProfitListener),
                                          menuItem("Doctors ID Mapping", loadDoctorsIDMappingListener),
                                          menuItem("Surgery ID Mapping", loadSurgeryIDMappingListener))
    
    fileMenu.items = List(loadDataMenu)
    
    
    menus = List(fileMenu)
    
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
