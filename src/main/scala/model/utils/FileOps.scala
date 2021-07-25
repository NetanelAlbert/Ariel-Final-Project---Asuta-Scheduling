package model.utils

import org.apache.poi.ss.usermodel.{DataFormatter, Row, Sheet, WorkbookFactory}
import spray.json.DefaultJsonProtocol
import spray.json._
import DefaultJsonProtocol._

import java.io.File
import java.io.PrintWriter
import collection.JavaConversions._
import model.DTOs.FormattingProtocols._
import model.DTOs.{DoctorStatistics, PastSurgeryInfo}

import java.sql.{Date, Timestamp}
import java.util.concurrent.TimeUnit
import scala.util.Try


object FileOps
{
    val SURGEON_STATISTICS_JSON_PATH = "SURGEON_STATISTICS"
    
    def createSurgeryAndDoctorData(newDataPath : String)
    {
        //val doctorInfoList = getAllSurgeryInfo(newDataPath)
        
        
        //saveDoctorInfo(doctorInfoList)
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
    
    def saveDoctorInfo(doctorInfoList : List[DoctorStatistics])
    {
        saveJsonToFile(doctorInfoList.toJson, SURGEON_STATISTICS_JSON_PATH)
    }
    
    def getDoctorInfo : List[DoctorStatistics] =
    {
        val source = scala.io.Source.fromFile("file.txt")
    
        try
        {
            source.mkString.toJson.convertTo[List[DoctorStatistics]]
        } finally source.close
    }
    
    def deleteFile(filePath : String) : Boolean =
    {
        new File(filePath).delete()
    }
}
