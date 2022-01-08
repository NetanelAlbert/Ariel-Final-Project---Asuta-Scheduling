package view.schduling.windowElements

import common.Utils
import model.DTOs.{Settings, SurgeryBasicInfo}
import org.joda.time.{LocalDate, LocalTime}
import scalafx.Includes.jfxDialogPane2sfx
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, VPos}
import scalafx.scene.control._
import scalafx.scene.layout.{Background, GridPane}
import scalafx.stage.Stage
import view.common.ComparableOptionWithFallbackToString
import view.common.UiUtils.double2digits
import work.BlockFillingOption

class ShowOptionsBlocksDialog(
    stage : Stage,
    startTime : LocalTime,
    endTime : LocalTime,
    date : LocalDate,
    topOptions : Seq[BlockFillingOption],
    settings : Settings,
) extends Dialog[Nothing]
{
    initOwner(stage)
    title = "Block Filling Suggestions"
    val info = s"Showing options for window: ${date.toString("dd/MM/yyyy")} ${startTime.toString("hh:mm")} - ${endTime.toString("hh:mm")}"

    dialogPane().buttonTypes = Seq(ButtonType.OK)
    
    val surgeriesList = getSurgeriesList
    val optionsTable = getOptionsTable
    
    val grid = new GridPane()
    {
        hgap = 10
        vgap = 10
        padding = Insets(20, 10, 10, 10)
        add(new Label(info), 0, 0)
        
        add(new Label("Options details:"), 0, 1)
        add(optionsTable, 0, 2)
        
        add(new Label("Surgeries chosen for marked option:"), 1, 1)
        add(surgeriesList, 1, 2)
//        prefWidth = 1000
    }
    
    
    this.dialogPane().setContent(grid)
    
    def getOptionsTable =
    {
        new TableView[BlockFillingOption](ObservableBuffer.empty[BlockFillingOption] ++= topOptions)
        {
            columns ++= getColumns
            columns.foreach(_.setStyle("-fx-alignment: center;"))
        
            //        // Dimensions settings
            //        columns.foreach(_.setPrefWidth(Screen.primary.bounds.width / Columns.columns.size))
            //        prefHeight = Screen.primary.bounds.height
        
            selectionModel().selectedItemProperty.addListener(_ =>
            {
                val option = selectionModel().getSelectedItem
                surgeriesList.items = ObservableBuffer.empty[SurgeryBasicInfo] ++= option.surgeries
            })
            topOptions.headOption.foreach(selectionModel().select)
            prefWidth = 800
        }
    }
    
    def getSurgeriesList =
    {
        new ListView[SurgeryBasicInfo](Nil)
        {
            cellFactory = _ =>
            {
                new ListCell[SurgeryBasicInfo]
                {
                    item.onChange
                    {
                        (_, _, option) =>
                        {
                            if (Option(option).nonEmpty)
                            {
                                text = option.nameOrCode
                            }
                        }
                    }
                }
            }
        }
    }
    
    def getColumns =
    {
        val doctorNameCol = new TableColumn[BlockFillingOption, String]("Doctor Name")
        {
            cellValueFactory = cdf => StringProperty(cdf.value.nameOrID)
        }
        
        val chanceForRestingShortCol = new TableColumn[BlockFillingOption, Double]("Chance for resting beds short")
        {
            cellValueFactory = cdf => ObjectProperty(double2digits(cdf.value.chanceForRestingShort))
        }
        
        val chanceForHospitalizeShortCol = new TableColumn[BlockFillingOption, Double]("Chance for hospitalize beds short")
        {
            cellValueFactory = cdf => ObjectProperty(double2digits(cdf.value.chanceForHospitalizeShort))
        }
        
        val expectedProfitCol = new TableColumn[BlockFillingOption, ComparableOptionWithFallbackToString[Int]]("Expected profit")
        {
            cellValueFactory = cdf => ObjectProperty(ComparableOptionWithFallbackToString(cdf.value.expectedProfit))
        }
        
        val globalScoreCol = new TableColumn[BlockFillingOption, Double]("Global score")
        {
            val profitNormalizer = Utils.getNormalizer(topOptions.flatMap(_.expectedProfit))
            cellValueFactory = cdf => ObjectProperty(double2digits(settings.blockOptionGlobalScore(cdf.value, profitNormalizer)))
        }
        
        List[javafx.scene.control.TableColumn[BlockFillingOption, _]](
            doctorNameCol,
            globalScoreCol,
            chanceForRestingShortCol,
            chanceForHospitalizeShortCol,
            expectedProfitCol,
            )
    }
}
