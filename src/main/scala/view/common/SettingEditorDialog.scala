package view.common

import model.DTOs.Settings
import scalafx.Includes.jfxDialogPane2sfx
import scalafx.geometry.Insets
import scalafx.scene.control.{ButtonType, Dialog, Label, TextField, TextFormatter}
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
    dialogPane().buttonTypes = Seq(ButtonType.Cancel, ButtonType.OK)
    
    resultConverter =
    {
        dialogButton =>
        {
            if (dialogButton == ButtonType.OK)
            {
                settingsGridPane.getSettings
            } else
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
//        prefWidth = 1000
        padding = Insets(20, 10, 10, 10)
        
        private var row = 0
        def createRow(text : String, defaultValue : Int) : TextField =
        {
            val textField = new TextField()
            
            textField.textFormatter = new TextFormatter[Int](
                StringConverter[Int](_.toInt, _.toString),
                defaultValue,
                floatFilter)
            
//            textField.text = defaultValue.toString
            add(new Label(text), 0, row)
            add(textField, 1, row)
            row += 1
            textField
        }
    
        val doctorRankingProfitWeightField = createRow("doctorRankingProfitWeight", settings.doctorRankingProfitWeight)
        val doctorRankingSurgeryTimeWeightField = createRow("doctorRankingSurgeryTimeWeight", settings.doctorRankingSurgeryTimeWeight)
        val doctorRankingRestingTimeWeightField = createRow("doctorRankingRestingTimeWeight", settings.doctorRankingRestingTimeWeight)
        val doctorRankingHospitalizationTimeWeightField = createRow("doctorRankingHospitalizationTimeWeight", settings.doctorRankingHospitalizationTimeWeight)
        val shortSurgeryPrepareTimeMinutesField = createRow("shortSurgeryPrepareTimeMinutes", settings.shortSurgeryPrepareTimeMinutes)
        val longSurgeryPrepareTimeMinutesField = createRow("longSurgeryPrepareTimeMinutes", settings.longSurgeryPrepareTimeMinutes)
        val longSurgeryDefinitionMinutesField = createRow("longSurgeryDefinitionMinutes", settings.longSurgeryDefinitionMinutes)
        val totalNumberOfRestingBedsField = createRow("totalNumberOfRestingBeds", settings.totalNumberOfRestingBeds)
        val totalNumberOfHospitalizeBedsField = createRow("totalNumberOfHospitalizeBeds", settings.totalNumberOfHospitalizeBeds)
        val numberOfOperationRoomsField = createRow("numberOfOperationRooms", settings.numberOfOperationRooms)
        val distributionMaxLengthField = createRow("distributionMaxLength", settings.distributionMaxLength)
        val numberOfPointsToLookForShortageField = createRow("numberOfPointsToLookForShortage", settings.numberOfPointsToLookForShortage)
        val doctorAvailabilityMonthsToGoBackField = createRow("doctorAvailabilityMonthsToGoBack", settings.doctorAvailabilityMonthsToGoBack)
        val surgeriesForBedCalculationDaysBeforeField = createRow("surgeriesForBedCalculationDaysBefore", settings.surgeriesForBedCalculationDaysBefore)
        val surgeriesForBedCalculationDaysAfterField = createRow("surgeriesForBedCalculationDaysAfter", settings.surgeriesForBedCalculationDaysAfter)
        
        
        def getSettings : Settings =
        {
            Settings(
                doctorRankingProfitWeight = doctorRankingProfitWeightField.text().toInt,
                doctorRankingSurgeryTimeWeight = doctorRankingSurgeryTimeWeightField.text().toInt,
                doctorRankingRestingTimeWeight = doctorRankingRestingTimeWeightField.text().toInt,
                doctorRankingHospitalizationTimeWeight = doctorRankingHospitalizationTimeWeightField.text().toInt,
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
            )
        }
        
        def integerFilter = change => filter(change, _.toInt, "0")
    
        def floatFilter = change => filter(change, _.toDouble, "0.0")
        
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
    }
}
