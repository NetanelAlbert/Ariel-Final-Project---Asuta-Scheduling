package view.schduling.windowElements

import akka.Done
import common.Utils
import javafx.scene.input.MouseButton
import model.DTOs.Priority.Priority
import model.DTOs.{Priority, Settings, SurgeryBasicInfo}
import org.joda.time.{LocalDate, LocalTime}
import scalafx.Includes.jfxDialogPane2sfx
import scalafx.beans.property.ObjectProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.GridPane
import scalafx.stage.Stage
import view.common.UiUtils
import work.BlockFillingOption

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * cons by Icons8
 */
object ShowOptionsBlocksDialog
{
    val FULL_STAR = new Image("icons/full-star-50.png")
    val EMPTY_STAR = new Image("icons/empty-star-50.png")
    val HIDE = new Image("icons/hide-50.png")
}

class ShowOptionsBlocksDialog(
    stage : Stage,
    startTime : LocalTime,
    endTime : LocalTime,
    date : LocalDate,
    topOptions : Seq[BlockFillingOption],
    settings : Settings,
    m_updateDoctorPriority : (Int, Priority) => Future[Done],
)(implicit ec : ExecutionContext) extends Dialog[Nothing]
{
    import ShowOptionsBlocksDialog._
    
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
                val itemOption = Option(selectionModel().getSelectedItem)
                itemOption match
                {
                    case Some(item) => surgeriesList.items = ObservableBuffer.empty[SurgeryBasicInfo] ++= item.surgeries
                    case None => surgeriesList.items = ObservableBuffer.empty[SurgeryBasicInfo]
                }
                
            })
            topOptions.headOption.foreach(selectionModel().select)
            prefWidth = 800
            sortOrder.add(columns.delegate.get(0))
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
        def getColumn[T](name : String)(valueMapper : BlockFillingOption => T)(implicit ev : T => Ordered[T]) : TableColumn[BlockFillingOption, PrioritizedValue[T]] =
        {
            new TableColumn[BlockFillingOption, PrioritizedValue[T]](name)
            {
                cellValueFactory = cdf =>
                {
                    val prioritizedValue = PrioritizedValue(valueMapper(cdf.value), cdf.value.doctorPriority, sortType())
                    ObjectProperty(prioritizedValue)
                }
            }
        }
        
        val doctorNameCol = getColumn("Doctor Name")(_.nameOrID)
        val chanceForRestingShortCol = getColumn("Chance for resting beds short")(_.chanceForRestingShort)
        val chanceForHospitalizeShortCol = getColumn("Chance for hospitalize beds short")(_.chanceForHospitalizeShort)
        val expectedProfitCol = getColumn("Expected profit")(_.expectedProfit)
        val globalScoreCol = getColumn("Global score")
        {
            val profitNormalizer = Utils.getNormalizer(topOptions.flatMap(_.expectedProfit))
            blockFillingOption => blockFillingOption.getTotalScore(settings, profitNormalizer)
        }
        
        val iconCol = new TableColumn[BlockFillingOption, Image]("Priority")
        {
            sortable = false
            cellValueFactory = cdf =>
            {
                val icon = cdf.value.doctorPriority match
                {
                    case Priority.STAR => FULL_STAR
                    case Priority.NORMAL => EMPTY_STAR
                    case Priority.HIDDEN => HIDE
                }
                ObjectProperty(icon)
            }
            
            cellFactory = _ =>
            {
                val imageView = new ImageView()
                {
                    fitWidth = 23
                    fitHeight = 23
                }
                
                val javaCell = new javafx.scene.control.TableCell[BlockFillingOption, Image]
                {
                    setGraphic(imageView)
                    setOnMouseClicked(event =>
                    {
                        val index = getIndex
                        val blockFillingOption = getTableView.getItems.get(index)
                        if(event.getButton == MouseButton.PRIMARY)
                        {
                            val newPriority = blockFillingOption.doctorPriority match
                            {
                                case Priority.STAR => Priority.NORMAL
                                case Priority.NORMAL => Priority.STAR
                                case Priority.HIDDEN => Priority.HIDDEN
                            }
                            updateDoctorPriority(blockFillingOption.doctorId, newPriority, index)
                        }
                        else if(event.getButton == MouseButton.SECONDARY)
                        {
                            UiUtils.showAlertAndPerform(
                                stage,
                                "Hide doctor?",
                                s"""Do you want to hide ${blockFillingOption.nameOrID}?
                                   |It will not be shown in this list any more.
                                   |You can change it in the 'file' menu -> 'Doctors Priorities'""".stripMargin
                                )
                            {
                                updateDoctorPriority(blockFillingOption.doctorId, Priority.HIDDEN, index)
                            }
                        }
                    })
                    override def updateItem(item : Image, empty : Boolean)
                    {
                        super.updateItem(item, empty)
                        if (Option(item).nonEmpty || !empty)
                        {
                            imageView.image = item
                        }
                    }
                }
    
                new TableCell(javaCell)
            }
            
            def updateDoctorPriority(doctorID : Int, newPriority : Priority, index : Int)
            {
                m_updateDoctorPriority(doctorID, newPriority).onComplete
                {
                    case Success(_) =>
                    {
                        if(newPriority == Priority.HIDDEN)
                        {
                            tableView().getItems.remove(index)
                        }
                        else
                        {
                            val newRow = tableView().getItems.get(index).copy(doctorPriority = newPriority)
                            tableView().getItems.set(index, newRow)
                            tableView().sort()
                            val newIndex = tableView().getItems.indexOf(newRow)
                            tableView().scrollTo(newIndex)
                            tableView().getSelectionModel.select(newIndex)
                        }
                    }
                    case Failure(exception) =>
                    {
                        UiUtils.showFailDialog(Some(exception), Some(s"Failed to update priority to $newPriority"))
                    }
                }
            }
        }
        
        List[javafx.scene.control.TableColumn[BlockFillingOption, _]](
            doctorNameCol,
            iconCol,
            globalScoreCol,
            chanceForRestingShortCol,
            chanceForHospitalizeShortCol,
            expectedProfitCol,
            )
    }
    
    object PrioritizedValue
    {
        def apply[T](value : T, priority : Priority, getSortType : => javafx.scene.control.TableColumn.SortType)(implicit ev : T => Ordered[T]) : PrioritizedValue[T] =
        {
            new PrioritizedValue[T](value, priority, getSortType)
        }
    }
    
    class PrioritizedValue[T](value : T, priority : Priority, getSortType : => javafx.scene.control.TableColumn.SortType)(implicit ev : T => Ordered[T]) extends Ordered[PrioritizedValue[T]]
    {
        private val m_value = value
        private val m_priority = priority
        private val m_getSortType = getSortType
        override def toString = value.toString
        
        def ascending = javafx.scene.control.TableColumn.SortType.ASCENDING
        
        override def compare(that : PrioritizedValue[T]) : Int =
        {
            if(this.m_priority != that.m_priority)
            {
                if(m_getSortType == ascending)
                {
                    this.m_priority.compare(that.m_priority)
                }
                else
                {
                    that.m_priority.compare(this.m_priority)
                }
            }
            else
            {
                this.m_value.compare(that.m_value)
            }
        }
    }
}
