package model.actors


import akka.actor.{ActorRef, Props}
import model.DTOs.SurgeryStatisticsImplicits.SurgeryStatisticsToSurgeryBasicInfo
import model.DTOs._
import model.probability.IntegerDistribution
import org.joda.time.{LocalDate, LocalDateTime}
import work.{BlockFillingOption, GetOptionsForFreeBlockWork, ReadPastSurgeriesExcelWork, WorkFailure, WorkSuccess}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object AnalyzeDataActor
{
    def props(m_controller : ActorRef, m_modelManager : ActorRef, m_databaseActor : ActorRef)(implicit ec : ExecutionContext) : Props = Props(new AnalyzeDataActor(m_controller, m_modelManager, m_databaseActor))
}

class AnalyzeDataActor(m_controller : ActorRef,
                       m_modelManager : ActorRef,
                       m_databaseActor : ActorRef)(implicit c : ExecutionContext) extends MyActor
{
    
    override def receive =
    {
        case work : ReadPastSurgeriesExcelWork => readPastSurgeriesExcelWork(work, work.pasteSurgeries)
        
        case work @ GetOptionsForFreeBlockWork(_, _, _, Some(doctorsWithSurgeries), Some(doctorMapping), Some(surgeryStatistics), Some(surgeryAvgInfo), Some(plannedSurgeries), _) => getOptionsForFreeBlockWork(work, doctorsWithSurgeries, doctorMapping, surgeryStatistics, surgeryAvgInfo, plannedSurgeries)
    }
    
    def getOptionsForFreeBlockWork(work : GetOptionsForFreeBlockWork, doctorsWithSurgeries : Map[Int, Seq[SurgeryAvgInfoByDoctor]], doctorMapping : Map[Int, String], surgeryStatistics : Seq[SurgeryStatistics], surgeryAvgInfo : Seq[SurgeryAvgInfo], plannedSurgeries : Seq[FutureSurgeryInfo])
    {
        // TODO implement real algorithm ! ! ! !
        //  !
        //  !
        //  !
        val doc1 = doctorsWithSurgeries.keySet.head
        val option1 = BlockFillingOption(doc1,
                                         doctorMapping.get(doc1),
                                         doctorsWithSurgeries(doc1).flatMap(surg =>
                                                                                surgeryStatistics.find(_.operationCode == surg.operationCode)
                                                                                                 .map(_.basicInfo)),
                                         1, 1, 1, 1, 1)
        val doc2 = doctorsWithSurgeries.keySet.tail.head
        val option2 = BlockFillingOption(doc2,
                                         doctorMapping.get(doc2),
                                         doctorsWithSurgeries(doc2).flatMap(surg =>
                                                                                surgeryStatistics.find(_.operationCode == surg.operationCode)
                                                                                                 .map(_.basicInfo)),
                                         .1, .1, .1, .1, 5)
        
        val workCopy = work.copy(topOptions = Some(Seq(option1, option2)))
        
        m_controller ! WorkSuccess(workCopy, Some("!!! Not real data !!!"))
        
        // real flow:
        val options = doctorsWithSurgeries.values.map(knapsack(_, surgeryStatistics, surgeryAvgInfo))
        
    }
    
    case class KnapsackSurgery(operationCode : Double, expectedDurationMinutes : Int, amountOfData : Int, profit : Int)
    {
        def weight = expectedDurationMinutes
        
        def price = amountOfData // todo - profit might be irrelevant and might pun weights on them
    }
    
    def knapsack(doctorSurgeries : Seq[SurgeryAvgInfoByDoctor], surgeryStatistics : Seq[SurgeryStatistics], surgeryAvgInfo : Seq[SurgeryAvgInfo]) : Seq[BlockFillingOption] =
    {
        Seq() // TODO
    }
    
    
    def getDoctorAvailability(pasteSurgeries : Iterable[PastSurgeryInfo]) : Future[Set[DoctorAvailability]] =
    {
        Future
        {
            val threshold = LocalDateTime.now().minusYears(2)
            pasteSurgeries.filter(_.blockStart.isAfter(threshold)).map(surgery => DoctorAvailability(surgery.doctorId, surgery.blockStart.getDayOfWeek)).toSet
        }
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
            val doctorAvailabilitiesFuture = getDoctorAvailability(pasteSurgeries)
            
            val copyWorkFuture = for
            {
                surgeryStatisticsIterable <- surgeryStatisticsIterableFuture
                surgeryAvgInfoIterable <- surgeryAvgInfoIterableFuture
                surgeryAvgInfoByDoctorIterable <- surgeryAvgInfoByDoctorIterableFuture
                doctorStatisticsIterable <- doctorStatisticsIterableFuture
                doctorAvailabilities <- doctorAvailabilitiesFuture
            } yield readPastSurgeriesExcelWork.copy(
                surgeryStatistics = Some(surgeryStatisticsIterable),
                surgeryAvgInfo = Some(surgeryAvgInfoIterable),
                surgeryAvgInfoByDoctor = Some(surgeryAvgInfoByDoctorIterable),
                doctorStatistics = Some(doctorStatisticsIterable),
                doctorAvailabilities = Some(doctorAvailabilities),
                pasteSurgeries = None
                )
            
            copyWorkFuture.onComplete
            {
                case Success(workCopy) => m_databaseActor ! workCopy
                
                case Failure(exception) =>
                {
                    m_controller ! WorkFailure(readPastSurgeriesExcelWork, Some(exception), Some(s"Can't generate DB objects from Excel File: ${readPastSurgeriesExcelWork.file.getPath}"))
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
                    
                    val amountOfData = iterable.size
                    
                    val (surgery, resting, hospitalization) = iterable.aggregate((0, 0, 0))(addSurgeryInfoToTuple, sumTwoTuples)
                    
                    val surgeryDurationAvgMinutes = surgery.toDouble / amountOfData
                    val restingDurationAvgMinutes = resting.toDouble / amountOfData
                    val hospitalizationDurationAvgHours = hospitalization.toDouble / amountOfData
                    
                    DoctorStatisticsAutoAvg(
                        id,
                        name = None,
                        amountOfData,
                        profit = None,
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
                                      profit = None,
                                      list.size)
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
        case ((a1, b1, c1), (a2, b2, c2)) => (a1 + a2, b1 + b2, c1 + c2)
    }
    
    def average(iterable : Iterable[Int]) : Double = iterable.sum.toDouble / iterable.size
}
