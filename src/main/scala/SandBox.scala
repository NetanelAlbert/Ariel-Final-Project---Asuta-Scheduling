
import akka.actor.{Actor, ActorSystem, Props}
import akka.util.Timeout
import model.DTOs.{HourOfWeek, Priority}
import model.database.DoctorStatisticsTable
import model.actors.{AnalyzeDataActor, FileActor}
import org.joda.time.{Days, LocalDate, LocalTime}
import scalafx.application.JFXApp3
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.effect.DropShadow
import scalafx.scene.layout.HBox
import scalafx.scene.paint.Color._
import scalafx.scene.paint.{Color, LinearGradient, Stops}
import scalafx.scene.text.Text
import spray.json.JsValue
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import scala.util.{Random, Try}
import slick.jdbc.HsqldbProfile.api._
import slick.util.AsyncExecutor


object SandBox
{
    
    case class Bla(s : String, i : Int, n : Long)
    implicit val aa = jsonFormat3(Bla)
    
    def main(args : Array[String]) : Unit =
    {
        val l = List[Int]()
        // returns the largest number from the collection
        println(l.reduceOption((x, y) => x max y))
    }
    
    
    def startAsk
    {
        val actorSystem = ActorSystem("SandBox")
        val askActor = actorSystem.actorOf(Props[AskActor])
        askActor ! "Start"
    }
    
    import scala.concurrent.duration._
    import akka.pattern.ask
    
    import scala.concurrent.ExecutionContext.Implicits.global
    
    class TellActor extends Actor {
        
        val recipient = context.actorOf(Props[ReceiveActor])
        
        def receive = {
            case "Start" =>
                recipient ! "Hello" // equivalent to recipient.tell("hello", self)
            
            case reply => println(reply)
        }
    }
    
    class AskActor extends Actor {
        
        val recipient = context.actorOf(Props[ReceiveActor])
        
        def receive = {
            case "Start" =>
                implicit val timeout : Timeout = 3 seconds
                val replyF = recipient ? "Hello" // equivalent to recipient.ask("Hello")
                replyF.foreach{
                    println
                }

            case m => println(s"AskActor received ${m}")
        }
    }
    
    class ReceiveActor extends Actor {
        
        def receive = {
            case "Hello" => sender ! "And Hello to you!"
        }
    }
    
    def printLocalTime(time : LocalTime) = println(s"hours: ${time.getHourOfDay} '\nminutes: ${time.getMinuteOfHour}")
    
    def runWithTime(procedure : => Unit)
    {
        val start = System.currentTimeMillis()
        
        procedure
    
        val end = System.currentTimeMillis()
    
        val diff = end - start
        val second = 1000
        val minute = 60*second
        println(s"\nRun in ${diff/minute} minutes, ${(diff % minute)/second} seconds and ${diff % second} millis (or $diff millis)")
    }
    
    def readFromExcel()
    {
        val start = System.currentTimeMillis()
    
//        val surgeries = AnalyzeDataActor.getAllSurgeryFromExcel("SurgeriesData.xlsx")
//        //val doctors = AnalyzeData.getDoctorStatisticsFromSurgeryInfo(surgeries)
//
//        val surgeriesStatistic = AnalyzeDataActor.getSurgeryStatisticsFromSurgeryInfo(surgeries)
        val end = System.currentTimeMillis()
//        surgeriesStatistic.foreach(println)
    
        val diff = end - start
        val second = 1000
        val minute = 60*second
        println(s"\nRun in ${diff/minute} minutes, ${(diff % minute)/second} seconds and ${diff % second} millis (or $diff millis)")
    }
    
    def vla
    {
        val db = Database.forURL("jdbc:hsqldb:file:database/model.db", "SA", "", executor = AsyncExecutor.default("DbExecutor", 3))
        //val table = new DoctorStatisticsTable(db)
    }

}

object MyApp extends JFXApp3
{
    override def start()
    {
        stage = new JFXApp3.PrimaryStage
        {
            //    initStyle(StageStyle.Unified)
            title = "Menu Demo"
            scene = new Scene
            {
                //content = List(new ManagerMenu)
                maximized = true
            }
        }
    }
}

object JFXApp3Demo extends JFXApp3
{
    override def start()
    {
        stage = new JFXApp3.PrimaryStage {
            //    initStyle(StageStyle.Unified)
            title = "ScalaFX Hello World"
            scene = new Scene {
                fill = Color.rgb(38, 38, 38)
                content = new HBox {
                    padding = Insets(50, 80, 50, 80)
                    children = Seq(
                        new Text {
                            text = "Scala"
                            style = "-fx-font: normal bold 100pt sans-serif"
                            fill = new LinearGradient(
                                endX = 0,
                                stops = Stops(Red, DarkRed))
                        },
                        new Text {
                            text = "FX"
                            style = "-fx-font: italic bold 100pt sans-serif"
                            fill = new LinearGradient(
                                endX = 0,
                                stops = Stops(White, DarkGray)
                                )
                            effect = new DropShadow {
                                color = DarkGray
                                radius = 15
                                spread = 0.25
                            }
                        }
                        )
                }
            }
        }
    }
}
