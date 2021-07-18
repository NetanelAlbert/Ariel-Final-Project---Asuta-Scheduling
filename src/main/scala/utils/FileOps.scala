package utils

import org.apache.poi.ss.usermodel.{DataFormatter, Row, Sheet, WorkbookFactory}
import spray.json.DefaultJsonProtocol
import spray.json._
import DefaultJsonProtocol._

import java.io.File
import java.io.PrintWriter
import collection.JavaConversions._
import DTOs.FormattingProtocols._
import DTOs.{SurgeonStatistics, SurgeryInfo}

import java.sql.{Date, Timestamp}
import java.util.concurrent.TimeUnit
import scala.util.Try


object FileOps
{
    val SURGEON_STATISTICS_JSON_PATH = "SURGEON_STATISTICS"
    
    def createSurgeryAndSurgeonData(newDataPath : String)
    {
        //val surgeonInfoList = getAllSurgeryInfo(newDataPath)
        
        
        //saveSurgeonInfo(surgeonInfoList)
    }
    
    def getSheet(path : String, index : Int = 0) : Sheet =
    {
        val file = new File(path)
        val workbook = WorkbookFactory.create(file)
        workbook.getSheetAt(index)
    }
    
    //TODO :: handle fail?
    def saveJsonToFile(jsValue : JsValue, path : String)
    {
        new PrintWriter(path)
        {
            try
            {
                write(jsValue.toString)
            } finally close()
        }
    }
    
    def saveSurgeonInfo(surgeonInfoList : List[SurgeonStatistics])
    {
        saveJsonToFile(surgeonInfoList.toJson, SURGEON_STATISTICS_JSON_PATH)
    }
    
    def getSurgeonInfo : List[SurgeonStatistics] =
    {
        val source = scala.io.Source.fromFile("file.txt")
    
        try
        {
            source.mkString.toJson.convertTo[List[SurgeonStatistics]]
        } finally source.close
    }
    
    def deleteFile(filePath : String) : Boolean =
    {
        new File(filePath).delete()
    }
}
