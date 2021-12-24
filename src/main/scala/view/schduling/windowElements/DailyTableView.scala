package view.schduling.windowElements

import javafx.collections.{FXCollections, ObservableList}
import model.DTOs.{Block, FutureSurgeryInfo, Settings}
import org.controlsfx.control.spreadsheet.{GridBase, SpreadsheetCell, SpreadsheetCellType, SpreadsheetView}
import org.joda.time.{LocalDate, LocalTime}
import scalafx.stage.Screen

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
import common.Utils._


class DailyTableView(futureSurgeryInfo : Iterable[FutureSurgeryInfo], blocks : Map[LocalDate, Set[Block]], settings : Settings) extends SpreadsheetView
{
    val operationRooms = settings.numberOfOperationRooms
    private var today = LocalDate.now()
    private def todayBlocks = blocks.getOrElse(today, Set[Block]())
    println(s"today = $today")
    println(s"todayBlocks: $todayBlocks")
    println(s"blocks: ${blocks.mkString("\n")}")
    
    val dayStart = timeFormat.parseLocalTime("07:00")
    val dayEnd = timeFormat.parseLocalTime("23:00")
    val dayLength = dayEnd.getHourOfDay - dayStart.getHourOfDay
    val workingHours = (for(hour <- 0 until dayLength) yield dayStart.plusHours(hour)).toList
    
    // Set layout
    val grid = new GridBase(workingHours.size, operationRooms + 1)
    setShowRowHeader(false)
    grid.getColumnHeaders.setAll(ColumnsNames.getColumnsNames(operationRooms).asJava)
    getColumns.foreach(_.setPrefWidth((Screen.primary.bounds.width - 5) / (operationRooms + 1)))
    grid.setRowHeightCallback(_ => (Screen.primary.bounds.height - 150) / workingHours.size)
    
    // Set content
    setContent
    def setContent
    {
        val rows : ObservableList[ObservableList[SpreadsheetCell]] = FXCollections.observableArrayList()
        workingHours.map(getRow).foreach(rows.add)
        
        grid.setRows(rows)
        this.setGrid(grid)
    
        todayBlocks.foreach
        {
            block =>
                val span = spanningForCell(block)
                val row = block.blockStart.getHourOfDay - dayStart.getHourOfDay
                val col = block.operationRoom
                grid.spanRow(span, row, col)
        }
    }
    
    // Set behavior
    this.setEditable(false)
    override def deleteSelectedCells() : Unit = {/* Do nothing */}
    this.setContextMenu(null)
    
    
    private def surgeryForCell(time : LocalTime, room : Int) : Option[Block] =
    {
        todayBlocks.find(block =>
        {
            block.operationRoom == room &&
            ! block.blockStart.isBefore(time) &&
            block.blockStart.isBefore(time.plusHours(1))
           // TODO change last 2 conditions
        })
    }
    
    private def getRow(time : LocalTime) : ObservableList[SpreadsheetCell] =
    {
        val rowNum = time.getHourOfDay - dayStart.getHourOfDay
        val row : ObservableList[SpreadsheetCell] = FXCollections.observableArrayList()
        
        val timeCell = SpreadsheetCellType.STRING.createCell(rowNum, 0, 1, 1, time.toString(timeFormat))
        timeCell.setStyle("""-fx-font-size: 20;
                            |-fx-font-weight: bold;
                            |-fx-alignment: center;""".stripMargin)
        row.add(timeCell)
        for(room <- 1 to operationRooms)
        {
            val cellValue = surgeryForCell(time, room)
            val span = cellValue.map(spanningForCell).getOrElse(1)
            val cellValueString = cellValue.map(_.prettyStringByLines(span)).orNull
            val cell = SpreadsheetCellType.STRING.createCell(rowNum, room, span, 1, cellValueString)
            cellValue.foreach(_ => cell.setStyle(
                """-fx-background-color: gainsboro;
                  |-fx-font-size: 23;
                  |-fx-font-weight: bolder;
                  |-fx-alignment: center;""".stripMargin))
            
            row.add(cell)
        }
        row
    }
    
    private def spanningForCell(block : Block) : Int =
    {
        block.blockEnd.getHourOfDay - block.blockStart.getHourOfDay + 1
    }
    
    def nextDay
    {
        this date_= today.plusDays(1)
    }
    
    def dayBefore
    {
        this date_= today.plusDays(-1)
    }
    
    def date = today
    
    def date_=(date : LocalDate)
    {
        today = date
        this.setContent
    }
}
