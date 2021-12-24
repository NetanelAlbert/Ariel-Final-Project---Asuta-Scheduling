package model.actors


import akka.actor.{ActorRef, Props}
import model.DTOs._
import model.probability.IntegerDistribution
import org.joda.time.{Duration, LocalDate, LocalDateTime, LocalTime}
import work.{BlockFillingOption, GetOptionsForFreeBlockWork, ReadPastSurgeriesExcelWork, WorkFailure, WorkSuccess}

import scala.concurrent.duration.{DurationInt, DurationLong, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object AnalyzeDataActor
{
    def props(m_controller : ActorRef, m_modelManager : ActorRef, m_databaseActor : ActorRef)(implicit ec : ExecutionContext) : Props = Props(new AnalyzeDataActor(m_controller, m_modelManager, m_databaseActor))
}

class AnalyzeDataActor(m_controller : ActorRef,
                       m_modelManager : ActorRef,
                       m_databaseActor : ActorRef)(implicit override val ec : ExecutionContext) extends MyActor with SettingsAccess
{
    
    override def receive =
    {
        case work : ReadPastSurgeriesExcelWork => readPastSurgeriesExcelWork(work, work.pasteSurgeries)
        
        case work @ GetOptionsForFreeBlockWork(startTime, endTime, date, Some(doctorsWithSurgeries), Some(doctorMapping), Some(surgeryStatistics), Some(surgeryAvgInfo), Some(plannedSurgeries), Some(plannedSurgeryStatistics), _) => getOptionsForFreeBlockWork(work, doctorsWithSurgeries, doctorMapping, surgeryStatistics, surgeryAvgInfo, plannedSurgeries, plannedSurgeryStatistics, startTime, endTime, date)
    }
    
    def getOptionsForFreeBlockWork(work : GetOptionsForFreeBlockWork,
                                   doctorsWithSurgeries : Map[Int, Seq[SurgeryAvgInfoByDoctor]],
                                   doctorMapping : Map[Int, String],
                                   surgeryStatistics : Seq[SurgeryStatistics],
                                   surgeryAvgInfo : Seq[SurgeryAvgInfo],
                                   plannedSurgeries : Seq[FutureSurgeryInfo],
                                   plannedSurgeryStatistics : Seq[SurgeryStatistics],
                                   startTime : LocalTime,
                                   endTime : LocalTime,
                                   date : LocalDate)
    {
        // TODO remove Await - use for comprehension for all the function
        val settings = Await.result(getSettings, 5 second)
        val startFunctionTime = new LocalTime()
        m_logger.info("Starting AnalyzeDataActor.getOptionsForFreeBlockWork")
        val totalNumberOfRestingBeds = settings.totalNumberOfRestingBeds
        val totalNumberOfHospitalizeBeds = settings.totalNumberOfHospitalizeBeds
        val distributionMaxLength = settings.distributionMaxLength
        val numberOfJumps = settings.numberOfPointsToLookForShortage
    
        def plannedSurgeriesDistributions(maxValue : Int, numberOfSteps : Int, distribution : SurgeryStatistics => IntegerDistribution, unit : FiniteDuration => Long) : Map[Int, IntegerDistribution] =
        {
            val startDateTime = date.toDateTime(startTime)
            val distributions = plannedSurgeries.map(surgery =>
            {
                val diffMillis = new Duration(startDateTime, surgery.plannedStart.toDateTime).getMillis.millis
                val diff = unit(diffMillis).toInt
                val timelessDistribution = plannedSurgeryStatistics.find(_.operationCode == surgery.operationCode).map(distribution).get
                timelessDistribution + diff
            })
            val step = maxValue / numberOfSteps
            (1 to maxValue by step).par.map(i =>
            {
                val indicatorsStillInBed = distributions.map(_.indicatorLessThenEq(i).opposite)
                i -> IntegerDistribution.sumAndTrim(indicatorsStillInBed, distributionMaxLength)
            }).toList.toMap
        }
        
        val plannedSurgeriesRestingBedsDistributions = if(plannedSurgeries.nonEmpty)
        {
            val maxAvgResting = surgeryAvgInfo.map(_.restingDurationAvgMinutes).max.toInt + numberOfJumps
            plannedSurgeriesDistributions(maxAvgResting, numberOfJumps, _.restingDistribution, _.toMinutes)
        }
        else
        {
            m_logger.warning(s"Got an empty plannedSurgeries list for getOptionsForFreeBlockWork $work")
            Map(1 -> IntegerDistribution.empty())
        }
        
        val plannedSurgeriesHospitalizeBedsDistributions = if(plannedSurgeries.nonEmpty)
        {
            val maxAvgHospitalize = surgeryAvgInfo.map(_.hospitalizationDurationAvgHours).max.toInt + numberOfJumps
            plannedSurgeriesDistributions(maxAvgHospitalize, numberOfJumps, _.hospitalizationDistribution, _.toHours)
        }
        else
        {
            Map(1 -> IntegerDistribution.empty())
        }
        
        val windowSizeMinutes = (endTime.getMillisOfDay - startTime.getMillisOfDay).millis.toMinutes.toInt
        val doctorsSacks = doctorsWithSurgeries.values.par.map(knapsack(_, windowSizeMinutes, settings))
    
        val operationToSurgeryBasicInfoMapping = surgeryStatistics.map(surgery => surgery.operationCode -> surgery.basicInfo).toMap
        val operationToProfitMapping = surgeryStatistics.map(surgery => surgery.operationCode -> surgery.profit).toMap
        val operationToRestingDistributionMapping = surgeryStatistics.map(surgery => surgery.operationCode -> surgery.restingDistribution).toMap
        val operationToHospitalizationDistributionMapping = surgeryStatistics.map(surgery => surgery.operationCode -> surgery.hospitalizationDistribution).toMap
        
        
        if(doctorsSacks.exists(_.nonEmpty))
        {
            val options = doctorsSacks.filter(_.nonEmpty).par.map
            {
                surgeryAvgInfoByDoctorSeq =>
                {
                    val newSurgeriesRestingDistributions = surgeryAvgInfoByDoctorSeq.map(surgery => operationToRestingDistributionMapping(surgery.operationCode))
//                    val newSurgeriesAvgRestingMinutes = surgeryAvgInfoByDoctorSeq.map(_.restingDurationAvgMinutes).sum.toInt
                    val totalRestingBedsDistribution = plannedSurgeriesRestingBedsDistributions.map
                    {
                        case (i, distribution) =>
                        {
                            val newSurgeriesRestingBedsDistribution = IntegerDistribution.sumAndTrim(newSurgeriesRestingDistributions.map(_.+(i).indicatorLessThenEq(i).opposite), distributionMaxLength)
                            IntegerDistribution.sumAndTrim(distributionMaxLength)(distribution, newSurgeriesRestingBedsDistribution)
                        }
                    }
                    val chanceForRestingShort = totalRestingBedsDistribution.map(_.indicatorLessThenEq(totalNumberOfRestingBeds).no).max
    
                    val newSurgeriesHospitalizationDistributions = surgeryAvgInfoByDoctorSeq.map(surgery => operationToHospitalizationDistributionMapping(surgery.operationCode))
//                    val newSurgeriesAvgHospitalizeHours = surgeryAvgInfoByDoctorSeq.map(_.hospitalizationDurationAvgHours).sum.toInt
                    val totalHospitalizeBedsDistribution = plannedSurgeriesHospitalizeBedsDistributions.map
                    {
                        case (i, distribution) =>
                        {
                            val newSurgeriesHospitalizeBedsDistribution = IntegerDistribution.sumAndTrim(newSurgeriesHospitalizationDistributions.map(_.+(i).indicatorLessThenEq(i).opposite), distributionMaxLength)
                            IntegerDistribution.sumAndTrim(distributionMaxLength)(distribution, newSurgeriesHospitalizeBedsDistribution)
                        }
                    }
                    val chanceForHospitalizeShort = totalHospitalizeBedsDistribution.map(_.indicatorLessThenEq(totalNumberOfHospitalizeBeds).no).max
    
                    val expectedProfitTry = Try
                    {
                        surgeryAvgInfoByDoctorSeq.map(surgery => operationToProfitMapping(surgery.operationCode).get).sum
                    }
    
                    val firstSurgery = surgeryAvgInfoByDoctorSeq.head
                    BlockFillingOption(
                        doctorId = firstSurgery.doctorId,
                        doctorName = doctorMapping.get(firstSurgery.doctorId),
                        surgeries = surgeryAvgInfoByDoctorSeq.map(surgery =>
                        {
                            operationToSurgeryBasicInfoMapping(surgery.operationCode)
                        }),
                        chanceForRestingShort = round(chanceForRestingShort, 2),
                        chanceForHospitalizeShort = round(chanceForHospitalizeShort, 2),
                        expectedProfit = expectedProfitTry.toOption
                        )
                }
            }.toList
    
            m_controller ! WorkSuccess(work.copy(topOptions = Some(options)), Some(s"Found the top options for $startTime - $endTime"))
        }
        else
        {
            m_controller ! WorkFailure(work, None, Some("All of the doctors optional surgeries was empty"))
        }
        
        //todo remove duration - for debugging
        val duration = new Duration(startFunctionTime.toDateTimeToday, new LocalTime().toDateTimeToday)
        m_logger.info("Ending AnalyzeDataActor.getOptionsForFreeBlockWork")
        m_logger.info(s"Duration: $duration")
    }
    
    def knapsack(surgeries : Seq[SurgeryAvgInfoByDoctor], W : Int, settings: Settings) : Seq[SurgeryAvgInfoByDoctor] =
    {
        if(surgeries.isEmpty) return Nil
        
        val m = Array.fill[Int](W + 1)(0)
        val sacks = Array.fill[Seq[SurgeryAvgInfoByDoctor]](W + 1)(Nil)
        
        for(w <- 1 to W)
        {
            val smallEnoughSurgeries = surgeries.filter(_.weight(settings) <= w)
            if(smallEnoughSurgeries.nonEmpty)
            {
                val options = smallEnoughSurgeries.map(surg => surg.value + m(w - surg.weight(settings)) -> surg).toMap
                val best = options(options.keys.max)
                m(w) = best.value + m(w - best.weight(settings))
                sacks(w) = sacks(w - best.weight(settings)) :+ best
            }
        }
        
        sacks(W)
    }
    
    
    def getDoctorAvailability(pasteSurgeries : Iterable[PastSurgeryInfo]) : Future[Set[DoctorAvailability]] =
    {
        for
        {
            settings <- getSettings
            monthsToGoBack = settings.doctorAvailabilityMonthsToGoBack
            threshold = LocalDateTime.now().minusMonths(monthsToGoBack)
            doctorAvailability = pasteSurgeries.filter(_.blockStart.isAfter(threshold)).map(surgery => DoctorAvailability(surgery.doctorId, surgery.blockStart.getDayOfWeek)).toSet
        } yield  doctorAvailability
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
                case Success(workCopy) =>
                {
                    m_databaseActor ! workCopy
                }
                
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
                    
                    DoctorStatistics(
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
    
    def round(value : Double, places : Int) : Double =
    {
        if (places < 0)
            throw new IllegalArgumentException
        
        val factor = Math.pow(10, places).toLong
        val factoredValue = value * factor
        val tmp = factoredValue.round
        tmp.toDouble / factor
    }
}
