package view.mangerStatistics.windows

import javafx.event.{ActionEvent, EventHandler}
import scalafx.scene.control.{Menu, MenuBar, MenuItem, RadioMenuItem, ToggleGroup}
import scalafx.stage.Screen

class ManagerMenu extends MenuBar
{
    val fileMenu = new Menu("File")
    
    val loadDataMenu = new Menu("Load Data")
    loadDataMenu.items = List(menuItem("Current Schedule",      _ => println("Current Schedule")),
                              menuItem("Past Surgeries",        _ => println("Past Surgeries")),
                              menuItem("Profit",                _ => println("Profit")),
                              menuItem("Doctors ID Mapping",    _ => println("Doctors ID Mapping")),
                              menuItem("Surgery ID Mapping",    _ => println("Surgery ID Mapping")))
    
    fileMenu.items = List(loadDataMenu)
    
    val modeMenu = new Menu("Mode")
    val menuToggleGroup = new ToggleGroup()
    modeMenu.items = List(radioItem("Basic information", _ => println("Basic information"), menuToggleGroup, true),
                          radioItem("Improvement information - Average", _ => println("Improvement information - Average"), menuToggleGroup),
                          radioItem("Improvement information By Operation", _ => println("Improvement information By Operation"), menuToggleGroup))
    
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

