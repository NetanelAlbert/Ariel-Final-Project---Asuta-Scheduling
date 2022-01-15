package view.common

import model.DTOs.{Settings, SettingsObject}
import scalafx.Includes.jfxDialogPane2sfx
import scalafx.geometry.Insets
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.ButtonBar.ButtonData
import scalafx.scene.control.TextFormatter.Change
import scalafx.scene.control.{Alert, ButtonType, Dialog, Label, ListCell, ListView, TextField, TextFormatter, Tooltip}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.GridPane
import scalafx.stage.Stage
import scalafx.util.StringConverter
import view.common.SettingEditorDialog.SettingsGridPane
import scala.util.Try

class SettingEditorDialog(stage : Stage, settings : Settings) extends Dialog[Settings]
{
    initOwner(stage)
    title = "Setting Editor"
    
    
    val settingsGridPane = new SettingsGridPane(settings)
    dialogPane().setContent(settingsGridPane)
    
    val resetButtonType = new ButtonType("Reset", ButtonData.Other)
    dialogPane().buttonTypes = Seq(ButtonType.OK, resetButtonType, ButtonType.Cancel)
    
    resultConverter =
    {
        dialogButton =>
        {
            if (dialogButton == ButtonType.OK)
            {
                settingsGridPane.getSettings
            }
            else if (dialogButton == resetButtonType)
            {
                UiUtils.showAlertAndPerform(
                    stage,
                    "Reset Settings",
                    "Are you sure you want to reset settings to default? \nThis can't be undone.",
                    )
                {
                    SettingsObject.default
                }.orNull
            }
            else
            {
                null
            }
        }
    }
}

object SettingEditorDialog
{
    class SettingsGridPane(settings : Settings) extends GridPane
    {
        hgap = 10
        vgap = 10
        padding = Insets(20, 10, 10, 10)
    
        val doctorRankingGrid = new GridPaneWithRows("Doctor Ranking")
        val doctorRankingProfitWeightField = doctorRankingGrid.createIntRow("Profit Weight", settings.doctorRankingProfitWeight)
        val doctorRankingSurgeryTimeWeightField = doctorRankingGrid.createIntRow("Surgery Time Weight", settings.doctorRankingSurgeryTimeWeight)
        val doctorRankingRestingTimeWeightField = doctorRankingGrid.createIntRow("Resting Time Weight", settings.doctorRankingRestingTimeWeight)
        val doctorRankingHospitalizationTimeWeightField = doctorRankingGrid.createIntRow("Hospitalization Time Weight", settings.doctorRankingHospitalizationTimeWeight)
        
        val blockOptionsGrid = new GridPaneWithRows("Block Filling Algorithm")
        val blockOptionsRestingShortWeightField = blockOptionsGrid.createIntRow("Resting Short Weight", settings.blockOptionsRestingShortWeight)
        val blockOptionsHospitalizeShortWeightField = blockOptionsGrid.createIntRow("Hospitalize Short Weight", settings.blockOptionsHospitalizeShortWeight)
        val blockOptionsProfitWeightField = blockOptionsGrid.createIntRow("Profit Weight", settings.blockOptionsProfitWeight)
        
        val distributionMaxLengthField = blockOptionsGrid.createIntRow("Distribution Max Length", settings.distributionMaxLength)
        val numberOfPointsToLookForShortageField = blockOptionsGrid.createIntRow("Number Of Points To Look For Shortage", settings.numberOfPointsToLookForShortage)
        
        val surgeriesForBedCalculationDaysBeforeField = blockOptionsGrid.createIntRow("Days Before - Bed Calculation", settings.surgeriesForBedCalculationDaysBefore)
        val surgeriesForBedCalculationDaysAfterField = blockOptionsGrid.createIntRow("Days After - Bed Calculation", settings.surgeriesForBedCalculationDaysAfter)
        
        val prepareTimeGrid = new GridPaneWithRows("Surgery times")
        val longSurgeryDefinitionMinutesField = prepareTimeGrid.createIntRow("Long Surgery Definition (minutes)", settings.longSurgeryDefinitionMinutes)
        val shortSurgeryPrepareTimeMinutesField = prepareTimeGrid.createIntRow("Short Surgery Prepare Time (minutes)", settings.shortSurgeryPrepareTimeMinutes)
        val longSurgeryPrepareTimeMinutesField = prepareTimeGrid.createIntRow("Long Surgery Prepare Time (minutes)", settings.longSurgeryPrepareTimeMinutes)
    
        val roomsAndBedsGrid = new GridPaneWithRows("Rooms & Beds")
        val totalNumberOfRestingBedsField = roomsAndBedsGrid.createIntRow("Total Number Of Resting Beds", settings.totalNumberOfRestingBeds)
        val totalNumberOfHospitalizeBedsField = roomsAndBedsGrid.createIntRow("Total Number Of Hospitalize Beds", settings.totalNumberOfHospitalizeBeds)
        val numberOfOperationRoomsField = roomsAndBedsGrid.createIntRow("Number Of Operation Rooms", settings.numberOfOperationRooms)
        
        val doctorsAvailabilityGrid = new GridPaneWithRows("Doctors Availability")
        val doctorAvailabilityMonthsToGoBackField = doctorsAvailabilityGrid.createIntRow("Months To Go Back", settings.doctorAvailabilityMonthsToGoBack, Some("Doctors availability is calculated based the days the worked in the last x months, as defined here."))
        
        val profitGrid = new GridPaneWithRows("Profit")
        val avgSurgeryProfitField = profitGrid.createOptionalIntRow("Surgery Average Profit", settings.avgSurgeryProfit)
        val avgDoctorProfitField = profitGrid.createOptionalIntRow("Doctor Average Profit", settings.avgDoctorProfit)
        
        def gridsList = List(
            roomsAndBedsGrid,
            blockOptionsGrid,
            prepareTimeGrid,
            doctorsAvailabilityGrid,
            doctorRankingGrid,
            profitGrid,
            )
        
        private var currentPane : Option[GridPaneWithRows] = None
        def setGrid(grid : GridPaneWithRows)
        {
            currentPane.foreach(children.remove(_))
            currentPane = Some(grid)
            add(grid, 1, 0)
        }
        
        val sectionsListView = new ListView[GridPaneWithRows](gridsList)
        {
            selectionModel().selectedItemProperty.addListener(_ =>
            {
              val option = selectionModel().getSelectedItem
              setGrid(option)
            })
    
            cellFactory = _ =>
            {
                new ListCell[GridPaneWithRows]
                {
                    item.onChange{ (_, _, option) =>
                    {
                        if(Option(option).nonEmpty)
                        {
                            text = option.name
                        }
                    }}
                }
            }
        }
        
        add(sectionsListView, 0, 0)
        setGrid(roomsAndBedsGrid)
        
        def getSettings : Settings =
        {
            Settings(
                doctorRankingProfitWeight = doctorRankingProfitWeightField.text().toInt,
                doctorRankingSurgeryTimeWeight = doctorRankingSurgeryTimeWeightField.text().toInt,
                doctorRankingRestingTimeWeight = doctorRankingRestingTimeWeightField.text().toInt,
                doctorRankingHospitalizationTimeWeight = doctorRankingHospitalizationTimeWeightField.text().toInt,

                blockOptionsRestingShortWeight = blockOptionsRestingShortWeightField.text().toInt,
                blockOptionsHospitalizeShortWeight = blockOptionsHospitalizeShortWeightField.text().toInt,
                blockOptionsProfitWeight = blockOptionsProfitWeightField.text().toInt,
                
                shortSurgeryPrepareTimeMinutes = shortSurgeryPrepareTimeMinutesField.text().toInt,
                longSurgeryPrepareTimeMinutes = longSurgeryPrepareTimeMinutesField.text().toInt,
                longSurgeryDefinitionMinutes = longSurgeryDefinitionMinutesField.text().toInt,
                totalNumberOfRestingBeds = totalNumberOfRestingBedsField.text().toInt,
                totalNumberOfHospitalizeBeds = totalNumberOfHospitalizeBedsField.text().toInt,
                numberOfOperationRooms = numberOfOperationRoomsField.text().toInt,
                distributionMaxLength = distributionMaxLengthField.text().toInt,
                numberOfPointsToLookForShortage = numberOfPointsToLookForShortageField.text().toInt,
                doctorAvailabilityMonthsToGoBack = doctorAvailabilityMonthsToGoBackField.text().toInt,
                surgeriesForBedCalculationDaysBefore = surgeriesForBedCalculationDaysBeforeField.text().toInt,
                surgeriesForBedCalculationDaysAfter = surgeriesForBedCalculationDaysAfterField.text().toInt,
                avgSurgeryProfit = intOption(avgSurgeryProfitField.text()),
                avgDoctorProfit = intOption(avgSurgeryProfitField.text()),
            )
        }
    }
    
    def intOption(string : String) = Try(string.toInt).toOption
    
    def integerFilter = change => filter(change, _.toInt, "0")
    
    def integerOptionFilter = change => filter(change, {str => if(str.nonEmpty) str.toInt}, "")
    
    def floatFilter = change => filter(change, _.toDouble, "0")
    
    def filter(change : TextFormatter.Change, actionToTry : String => Unit, defaultValue : String) : TextFormatter.Change =
    {
        Try
        {
            actionToTry(change.controlNewText)
        }.failed.foreach
        {
            _ => change.text = ""
        }
        
        if(change.controlNewText.isEmpty)
        {
            change.text = defaultValue
        }
        
        change
    }
    
    class GridPaneWithRows(val name : String) extends GridPane
    {
        hgap = 10
        vgap = 10
        //        prefWidth = 1000
        padding = Insets(20, 10, 10, 10)
    
        private var row = 0
    
        def createIntRow(text : String, defaultValue : Int, info : Option[String] = None) =
        {
            createRow[Int](text, defaultValue, _.toInt, _.toString, integerFilter, info)
        }
    
        def createOptionalIntRow(text : String, defaultValue : Option[Int], info : Option[String] = None) =
        {
            createRow[Option[Int]](text, defaultValue, intOption, _.map(_.toString).getOrElse(""), integerOptionFilter, info)
        }
    
        def createRow[T](text : String, defaultValue : T, toT : String => T, TToString : T => String, filter : Change => Change, info : Option[String] = None) : TextField =
        {
            val textField = new TextField()
            {
                textFormatter = new TextFormatter[T](
                    StringConverter[T](toT, TToString),
                    defaultValue,
                    filter)
            }
    
            add(new Label(text), 0, row)
            add(textField, 1, row)
            info.foreach(info =>
            {
                val imageView = new ImageView(infoImage)
                {
                    pickOnBounds = true
                    fitWidth = 20
                    fitHeight = 20
                    Tooltip.install(this, info)
                }
                add(imageView, 2, row)
            })
            row += 1
            textField
        }
    }
    
    val infoImage = new Image("icons/info-icon-50.png")
}
