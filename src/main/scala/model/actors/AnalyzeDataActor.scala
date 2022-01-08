package model.actors


import akka.actor.{ActorRef, Props}
import model.DTOs._
import model.probability.IntegerDistribution
import org.joda.time.{DateTime, Duration, LocalDate, LocalDateTime, LocalTime}
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
    
        val startDateTime = date.toDateTime(startTime)
        // The distributions of the number of used resting/hospitalize beds,
        //  in some points from the start of this block
        val plannedSurgeriesRestingBedsDistributions = plannedSurgeriesRestingDistributions(settings, startDateTime, plannedSurgeries, plannedSurgeryStatistics)
        val plannedSurgeriesHospitalizeBedsDistributions = plannedSurgeriesHospitalizeDistributions(settings, startDateTime, plannedSurgeries, plannedSurgeryStatistics)
        
        val windowSizeMinutes = (endTime.getMillisOfDay - startTime.getMillisOfDay).millis.toMinutes.toInt
        // The collections of suggested surgeries, per available doctor
        val doctorsSacks = doctorsWithSurgeries.values.par.map(getDoctorSurgeriesSuggestions(windowSizeMinutes, settings))
        
        if(doctorsSacks.exists(_.nonEmpty))
        {
            // The collections above, with scores for each one (e.g. the chance of resting beds shortage)
            val options = doctorsSacks.filter(_.nonEmpty).par.map
            {
                getBlockFillingOptionMapper(settings, surgeryStatistics, doctorMapping, plannedSurgeriesRestingBedsDistributions, plannedSurgeriesHospitalizeBedsDistributions)
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
    
    def plannedSurgeriesRestingDistributions(settings : Settings, startDateTime : DateTime, plannedSurgeries : Seq[FutureSurgeryInfo], plannedSurgeryStatistics : Seq[SurgeryStatistics]) : Map[Int, IntegerDistribution] =
    {
        plannedSurgeriesDistributions(_.restingDistribution, _.toMinutes)(settings, startDateTime, plannedSurgeries, plannedSurgeryStatistics)
    }
    
    def plannedSurgeriesHospitalizeDistributions(settings : Settings, startDateTime : DateTime, plannedSurgeries : Seq[FutureSurgeryInfo], plannedSurgeryStatistics : Seq[SurgeryStatistics]) : Map[Int, IntegerDistribution] =
    {
        plannedSurgeriesDistributions(_.hospitalizationDistribution, _.toHours)(settings, startDateTime, plannedSurgeries, plannedSurgeryStatistics)
    }
    
    def plannedSurgeriesDistributions(distribution : SurgeryStatistics => IntegerDistribution, timeUnit : FiniteDuration => Long)
                                     (settings : Settings, startDateTime : DateTime, plannedSurgeries : Seq[FutureSurgeryInfo], plannedSurgeryStatistics : Seq[SurgeryStatistics]) : Map[Int, IntegerDistribution] =
    {
        if(plannedSurgeries.nonEmpty)
        {
            val numberOfSteps = settings.numberOfPointsToLookForShortage
            val distributionMaxLength = settings.distributionMaxLength
            val maxValue = plannedSurgeryStatistics.map(distribution(_).expectation).max.toInt + numberOfSteps
        
            // The distribution for each surgery x, that the patient is still resting / hospitalizing
            val distributions = plannedSurgeries.map(surgery =>
            {
                 val surgeryStartTime = surgery.plannedStart.toDateTime
                 val diffDuration = new Duration(startDateTime, surgeryStartTime).getMillis.millis
                 val diff = timeUnit(diffDuration).toInt // Hours / Minutes
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
        else
        {
            m_logger.warning(s"Got an empty plannedSurgeries list for getOptionsForFreeBlockWork")
            Map(1 -> IntegerDistribution.empty())
        }
    }
    
    def getDoctorSurgeriesSuggestions(windowSizeMinutes : Int, settings : Settings)(surgeries : Seq[SurgeryAvgInfoByDoctor]) : Seq[SurgeryAvgInfoByDoctor] =
    {
        val allSurgeriesLength = surgeries.map(surg => surg.amountOfData * surg.surgeryDurationAvgMinutes).sum
        val proportionalAmountEachSurgery = surgeries.flatMap
        {
            surg =>
            {
                val surgeryLengthMulTimes = surg.amountOfData * surg.surgeryDurationAvgMinutes
                val proportion = surgeryLengthMulTimes / allSurgeriesLength
                val minutesForThisSurg = proportion * windowSizeMinutes
                val repeats = (minutesForThisSurg / surg.durationIncludePrepareTime(settings)).toInt
                Seq.fill(repeats)(surg)
            }
        }
        
        val leftTime = windowSizeMinutes - proportionalAmountEachSurgery.map(_.durationIncludePrepareTime(settings)).sum
        val surgeriesForLeftTime = knapsack(surgeries, leftTime, settings)
        
        proportionalAmountEachSurgery ++ surgeriesForLeftTime
    }
    
    def knapsack(surgeries : Seq[SurgeryAvgInfoByDoctor], W : Int, settings : Settings) : Seq[SurgeryAvgInfoByDoctor] =
    {
        if(surgeries.isEmpty) return Nil
        
        val m = Array.fill[Int](W + 1)(0)
        val sacks = Array.fill[Seq[SurgeryAvgInfoByDoctor]](W + 1)(Nil)
        
        for(w <- 1 to W)
        {
            val smallEnoughSurgeries = surgeries.filter(_.durationIncludePrepareTime(settings) <= w)
            if(smallEnoughSurgeries.nonEmpty)
            {
                val options = smallEnoughSurgeries.map(surg => surg.value + m(w - surg.durationIncludePrepareTime(settings)) -> surg).toMap
                val best = options(options.keys.max)
                m(w) = best.value + m(w - best.durationIncludePrepareTime(settings))
                sacks(w) = sacks(w - best.durationIncludePrepareTime(settings)) :+ best
            }
        }
        
        sacks(W)
    }
    
    def getBlockFillingOptionMapper(settings : Settings, surgeryStatistics : Seq[SurgeryStatistics], doctorMapping : Map[Int, String], plannedSurgeriesRestingBedsDistributions : Map[Int, IntegerDistribution], plannedSurgeriesHospitalizeBedsDistributions : Map[Int, IntegerDistribution]) : Seq[SurgeryAvgInfoByDoctor] => BlockFillingOption =
    {
        val operationToSurgeryBasicInfoMapping = surgeryStatistics.map(surgery => surgery.operationCode -> surgery.basicInfo).toMap
        val operationToProfitMapping = surgeryStatistics.map(surgery => surgery.operationCode -> surgery.profit).toMap
        val operationToRestingDistributionMapping = surgeryStatistics.map(surgery => surgery.operationCode -> surgery.restingDistribution).toMap
        val operationToHospitalizationDistributionMapping = surgeryStatistics.map(surgery => surgery.operationCode -> surgery.hospitalizationDistribution).toMap
        
        val distributionMaxLength = settings.distributionMaxLength
        val totalNumberOfRestingBeds = settings.totalNumberOfRestingBeds
        val totalNumberOfHospitalizeBeds = settings.totalNumberOfHospitalizeBeds
    
        def getBlockFillingOption(surgeryAvgInfoByDoctorSeq : Seq[SurgeryAvgInfoByDoctor]) : BlockFillingOption =
        {
            val newSurgeriesRestingDistributions = surgeryAvgInfoByDoctorSeq.map(surgery => operationToRestingDistributionMapping(surgery.operationCode))
            val totalRestingBedsDistribution = plannedSurgeriesRestingBedsDistributions.map
            {
                case (i, distribution) =>
                {
                    val newSurgeriesRestingBedsDistribution = IntegerDistribution.sumAndTrim(newSurgeriesRestingDistributions.map(_.indicatorLessThenEq(i).opposite), distributionMaxLength)
                    IntegerDistribution.sumAndTrim(distributionMaxLength)(distribution, newSurgeriesRestingBedsDistribution)
                }
            }
            val chanceForRestingShort = totalRestingBedsDistribution.map(_.indicatorLessThenEq(totalNumberOfRestingBeds).no).max
    
            val newSurgeriesHospitalizationDistributions = surgeryAvgInfoByDoctorSeq.map(surgery => operationToHospitalizationDistributionMapping(surgery.operationCode))
            val totalHospitalizeBedsDistribution = plannedSurgeriesHospitalizeBedsDistributions.map
            {
                case (i, distribution) =>
                {   // NOTE -->                                                                                                        // was _.+(i).indicator...
                    val newSurgeriesHospitalizeBedsDistribution = IntegerDistribution.sumAndTrim(newSurgeriesHospitalizationDistributions.map(_.indicatorLessThenEq(i).opposite), distributionMaxLength)
                    IntegerDistribution.sumAndTrim(distributionMaxLength)(distribution, newSurgeriesHospitalizeBedsDistribution)
                }
            }
            val chanceForHospitalizeShort = totalHospitalizeBedsDistribution.map(_.indicatorLessThenEq(totalNumberOfHospitalizeBeds).no).max
    
            val expectedProfitTry = Try
            {
                surgeryAvgInfoByDoctorSeq.map(surgery => operationToProfitMapping(surgery.operationCode).orElse(settings.avgSurgeryProfit).get).sum
            }
    
            val firstSurgery = surgeryAvgInfoByDoctorSeq.head
            BlockFillingOption(
                doctorId = firstSurgery.doctorId,
                doctorName = doctorMapping.get(firstSurgery.doctorId),
                surgeries = surgeryAvgInfoByDoctorSeq.map(_.operationCode).map(operationToSurgeryBasicInfoMapping(_)),
                chanceForRestingShort = round(chanceForRestingShort, 2),
                chanceForHospitalizeShort = round(chanceForHospitalizeShort, 2),
                expectedProfit = expectedProfitTry.toOption
                )
        }
    
        getBlockFillingOption
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
