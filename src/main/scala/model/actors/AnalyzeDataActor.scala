package model.actors


import akka.actor.{Actor, ActorRef}
import org.apache.poi.ss.usermodel.{DataFormatter, Row}
import model.DTOs.{DoctorStatistics, DoctorStatisticsAutoAvg, PastSurgeryInfo, SurgeryAvgInfo, SurgeryAvgInfoByDoctor, SurgeryStatistics}
import model.probability.IntegerDistribution
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution
import work.{ReadPastSurgeriesExcelWork, WorkFailure}

import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AnalyzeDataActor(m_controller : ActorRef,
                       m_databaseActor : ActorRef,
                       m_modelManager : ActorRef)(implicit c : ExecutionContext) extends MyActor
{
    
    override def receive =
    {
        case work : ReadPastSurgeriesExcelWork => readPastSurgeriesExcelWork(work, work.pasteSurgeries)
    
    
        case _ =>
    }
    
    def readPastSurgeriesExcelWork(readPastSurgeriesExcelWork : ReadPastSurgeriesExcelWork, pasteSurgeriesOption : Option[Iterable[PastSurgeryInfo]]) = pasteSurgeriesOption match
    {
        case None =>
        {
            val info = "Got ReadPastSurgeriesExcelWork without pasteSurgeries"
            m_controller ! WorkFailure(readPastSurgeriesExcelWork, None, Some(info))
        }

        case Some(pasteSurgeries) =>
        {
            val surgeryStatisticsIterableFuture = getSurgeryStatistics(pasteSurgeries)
            val surgeryAvgInfoIterableFuture = getSurgeryAvgInfo(pasteSurgeries)
            val surgeryAvgInfoByDoctorIterableFuture = getSurgeryAvgInfoByDoctor(pasteSurgeries)
            val doctorStatisticsIterableFuture = getDoctorStatistics(pasteSurgeries)
            
            val copyFuture = for
            {
                surgeryStatisticsIterable <- surgeryStatisticsIterableFuture
                surgeryAvgInfoIterable <- surgeryAvgInfoIterableFuture
                surgeryAvgInfoByDoctorIterable <- surgeryAvgInfoByDoctorIterableFuture
                doctorStatisticsIterable <- doctorStatisticsIterableFuture
            } yield readPastSurgeriesExcelWork.copy(
                surgeryStatistics = Some(surgeryStatisticsIterable),
                surgeryAvgInfo = Some(surgeryAvgInfoIterable),
                surgeryAvgInfoByDoctor = Some(surgeryAvgInfoByDoctorIterable),
                doctorStatistics = Some(doctorStatisticsIterable),
                pasteSurgeries = None
            )
            
            copyFuture.onComplete
            {
                case Success(workCopy) => m_databaseActor ! workCopy
    
                case Failure(exception) =>
                {
                    m_controller ! WorkFailure(readPastSurgeriesExcelWork, Some(exception), Some(s"Can't generate DB objects from Excel File: ${readPastSurgeriesExcelWork.filePath}"))
                }
            }
        }
    }
    
    def getSurgeryAvgInfo(surgeryList : Iterable[PastSurgeryInfo]) : Future[Iterable[SurgeryAvgInfo]] =
    {
        Future
        {
            surgeryList.groupBy(_.operationCode).map
            {
                case (operationCode, surgeryListByOp) =>
                {
            
                    val surgeryAvg = average(surgeryListByOp.map(_.surgeryDurationMinutes))
                    val restingAvg = average(surgeryListByOp.map(_.restingMinutes))
                    val hospitalizationAvg = average(surgeryListByOp.map(_.hospitalizationHours))
                    SurgeryAvgInfo(operationCode, surgeryListByOp.size, surgeryAvg, restingAvg, hospitalizationAvg)
                }
            }
        }
    }
    
    def getSurgeryAvgInfoByDoctor(surgeryList : Iterable[PastSurgeryInfo]) : Future[Iterable[SurgeryAvgInfoByDoctor]] =
    {
        Future
        {
            surgeryList.groupBy(_.doctorId).flatMap
            {
                case (doctorId, surgeryListByDoctor) =>
                {
                    surgeryListByDoctor.groupBy(_.operationCode).map
                    {
                        case (operationCode, surgeryListByDoctorByOp) =>
                        {
                            val surgeryAvg = average(surgeryListByDoctorByOp.map(_.surgeryDurationMinutes))
                            val restingAvg = average(surgeryListByDoctorByOp.map(_.restingMinutes))
                            val hospitalizationAvg = average(surgeryListByDoctorByOp.map(_.hospitalizationHours))
                            SurgeryAvgInfoByDoctor(operationCode, doctorId, surgeryListByDoctorByOp.size, surgeryAvg, restingAvg, hospitalizationAvg)
                        }
                    }
                }
            }
        }
    }
    
    
    def getDoctorStatistics(surgeryList : Iterable[PastSurgeryInfo]) : Future[Iterable[DoctorStatistics]] =
    {
        Future
        {
            surgeryList.groupBy(_.doctorId).map
            {
                case (id, iterable) =>
                {
            
                    val name = None // todo get names
                    val amountOfData = iterable.size
                    val profitAvg = 1.11 // todo get profit
            
                    val (surgery, resting, hospitalization) = iterable.aggregate((0, 0, 0))(addSurgeryInfoToTuple, sumTwoTuples)
            
                    val surgeryDurationAvgMinutes = surgery.toDouble / amountOfData
                    val restingDurationAvgMinutes = resting.toDouble / amountOfData
                    val hospitalizationDurationAvgHours = hospitalization.toDouble / amountOfData
            
                    DoctorStatisticsAutoAvg(
                        id,
                        name,
                        amountOfData,
                        profitAvg,
                        surgeryDurationAvgMinutes,
                        restingDurationAvgMinutes,
                        hospitalizationDurationAvgHours)
                }
            }
        }
    }
    
    def getSurgeryStatistics(surgeryList : Iterable[PastSurgeryInfo]) : Future[Iterable[SurgeryStatistics]] =
    {
        Future
        {
            surgeryList.groupBy(_.operationCode).map
            {
                case (operationCode, list) =>
                {
                    val restingDistribution = IntegerDistribution(list.map(_.restingMinutes))
                    val hospitalizationDistribution = IntegerDistribution(list.map(_.hospitalizationHours))
            
                    SurgeryStatistics(operationCode,
                                      operationName = None,
                                      restingDistribution,
                                      hospitalizationDistribution,
                                      profit = None)
                }
            }
        }
    }
    
    def addSurgeryInfoToTuple(tuple : (Int, Int, Int), surgeryInfo : PastSurgeryInfo) : (Int, Int, Int) = surgeryInfo match
    {
        case PastSurgeryInfo(_, _, surgeryDurationMinutes, restingMinutes, hospitalizationHours, _, _) =>
        {
            val pastSurgeryTuple = (surgeryDurationMinutes, restingMinutes, hospitalizationHours)
            sumTwoTuples(tuple, pastSurgeryTuple)
        }
    }
    
    def sumTwoTuples(tuple1 : (Int, Int, Int), tuple2 : (Int, Int, Int)) : (Int, Int, Int) = (tuple1, tuple2) match
    {
        case((a1, b1, c1), (a2, b2, c2)) => (a1 + a2, b1 + b2, c1 + c2)
    }
    
    def average(iterable : Iterable[Int]) : Double = iterable.sum.toDouble/iterable.size
}
