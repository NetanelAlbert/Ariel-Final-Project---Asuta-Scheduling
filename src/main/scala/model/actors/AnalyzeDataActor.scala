package model.actors


import akka.actor.{ActorRef, Props}
import model.DTOs.Priority.Priority
import model.DTOs._
import model.probability.IntegerDistribution
import org.joda.time._
import work._

import scala.concurrent.duration.{DurationInt, DurationLong, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

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
        
        case work @ GetOptionsForFreeBlockWork(startTime, endTime, date, Some(doctorsWithSurgeries), Some(doctorMapping), Some(surgeryStatistics), Some(plannedSurgeriesAvgInfo), Some(plannedSurgeries), Some(plannedSurgeryStatistics), Some(doctorsPriorityMap), _) =>
        {
            getOptionsForFreeBlockWork(work, doctorsWithSurgeries, doctorMapping, surgeryStatistics, plannedSurgeriesAvgInfo, plannedSurgeries, plannedSurgeryStatistics, doctorsPriorityMap, startTime, endTime, date)
        }
    }
    
    def getOptionsForFreeBlockWork(work : GetOptionsForFreeBlockWork,
                                   doctorsWithSurgeries : Map[Int, Seq[SurgeryAvgInfoByDoctor]],
                                   doctorMapping : Map[Int, String],
                                   surgeryStatistics : Seq[SurgeryStatistics],
                                   plannedSurgeriesAvgInfo : Seq[SurgeryAvgInfo],
                                   plannedSurgeries : Seq[FutureSurgeryInfo],
                                   plannedSurgeryStatistics : Seq[SurgeryStatistics],
                                   doctorsPriorityMap : Map[Int, Priority],
                                   startTime : LocalTime,
                                   endTime : LocalTime,
                                   date : LocalDate)
    {
        getSettings.onComplete
        {
            case Success(settings) =>
            {
                val startFunctionTime = new LocalTime()
                m_logger.info("Starting AnalyzeDataActor.getOptionsForFreeBlockWork")
    
                val startDateTime = date.toDateTime(startTime)
                // The distributions of the number of used resting/hospitalize beds,
                //  in some points from the start of this block
                val plannedSurgeriesRestingBedsDistributions = plannedSurgeriesRestingDistributions(settings, startDateTime, plannedSurgeries, plannedSurgeryStatistics, plannedSurgeriesAvgInfo)
                val plannedSurgeriesHospitalizeBedsDistributions = plannedSurgeriesHospitalizeDistributions(settings, startDateTime, plannedSurgeries, plannedSurgeryStatistics, plannedSurgeriesAvgInfo)
    
                val windowSizeMinutes = (endTime.getMillisOfDay - startTime.getMillisOfDay).millis.toMinutes.toInt
                // The collections of suggested surgeries, per available doctor
                val doctorsSacks = doctorsWithSurgeries.values.par.map(getDoctorSurgeriesSuggestions(windowSizeMinutes, settings))
    
                if (doctorsSacks.exists(_.nonEmpty))
                {
                    val blockFillingOptionMapper = getBlockFillingOptionMapper(settings, surgeryStatistics, doctorMapping, plannedSurgeriesRestingBedsDistributions, plannedSurgeriesHospitalizeBedsDistributions, doctorsPriorityMap)
                    // The collections above, with scores for each one (e.g. the chance of resting beds shortage)
                    val options = doctorsSacks.filter(_.nonEmpty).par.map
                    {
                        blockFillingOptionMapper
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
    
            case Failure(exception) =>
            {
                m_controller ! WorkFailure(work, Some(exception), Some("Failed to fetch settings"))
            }
        }
    }
    
    def addTimes(settings : Settings)(sack : Seq[SurgeryAvgInfoByDoctor]) : Seq[(SurgeryAvgInfoByDoctor, Int)] =
    {
        var time = 0
        sack.map
        {
            surg =>
            {
                time += surg.durationIncludePrepareTime(settings)
                surg -> time
            }
        }
    }
    
    def plannedSurgeriesRestingDistributions(settings : Settings, startDateTime : DateTime, plannedSurgeries : Seq[FutureSurgeryInfo], plannedSurgeryStatistics : Seq[SurgeryStatistics], plannedSurgeriesAvgInfo : Seq[SurgeryAvgInfo]) : Map[Int, IntegerDistribution] =
    {
        plannedSurgeriesDistributions(_.restingDistribution, _.toMinutes, plannedSurgeriesAvgInfo)(settings, startDateTime, plannedSurgeries, plannedSurgeryStatistics)
    }
    
    def plannedSurgeriesHospitalizeDistributions(settings : Settings, startDateTime : DateTime, plannedSurgeries : Seq[FutureSurgeryInfo], plannedSurgeryStatistics : Seq[SurgeryStatistics], plannedSurgeriesAvgInfo : Seq[SurgeryAvgInfo]) : Map[Int, IntegerDistribution] =
    {
        plannedSurgeriesDistributions(_.hospitalizationDistribution, _.toHours, plannedSurgeriesAvgInfo)(settings, startDateTime, plannedSurgeries, plannedSurgeryStatistics)
    }
    
    def plannedSurgeriesDistributions(distribution : SurgeryStatistics => IntegerDistribution, timeUnit : FiniteDuration => Long, plannedSurgeriesAvgInfo : Seq[SurgeryAvgInfo])
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
                val surgeryLength = plannedSurgeriesAvgInfo.find(_.operationCode == surgery.operationCode).map(_.surgeryDurationAvgMinutes).get
                timelessDistribution + (diff + surgeryLength.toInt)
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
                val options = smallEnoughSurgeries.map(surg =>
                {
                    val newValue = surg.value + m(w - surg.durationIncludePrepareTime(settings))
                    newValue -> surg
                }).toMap
                val best = options(options.keys.max)
                m(w) = best.value + m(w - best.durationIncludePrepareTime(settings))
                sacks(w) = sacks(w - best.durationIncludePrepareTime(settings)) :+ best
            }
        }
        
        sacks(W)
    }
    
    def getBlockFillingOptionMapper(
        settings : Settings,
        surgeryStatistics : Seq[SurgeryStatistics],
        doctorNameMapping : Map[Int, String],
        plannedSurgeriesRestingBedsDistributions : Map[Int, IntegerDistribution],
        plannedSurgeriesHospitalizeBedsDistributions : Map[Int, IntegerDistribution],
        doctorsPriorityMap : Map[Int, Priority]
    ) : Seq[SurgeryAvgInfoByDoctor] => BlockFillingOption =
    {
        val operationToSurgeryBasicInfoMapping = surgeryStatistics.map(surgery => surgery.operationCode -> surgery.basicInfo).toMap
        val operationToProfitMapping = surgeryStatistics.flatMap
        {
            surgery => surgery.profit.map(profit => surgery.operationCode -> profit)
        }.toMap
        val operationToRestingDistributionMapping = surgeryStatistics.map(surgery => surgery.operationCode -> surgery.restingDistribution).toMap
        val operationToHospitalizationDistributionMapping = surgeryStatistics.map(surgery => surgery.operationCode -> surgery.hospitalizationDistribution).toMap
        
        val distributionMaxLength = settings.distributionMaxLength
        val totalNumberOfRestingBeds = settings.totalNumberOfRestingBeds
        val totalNumberOfHospitalizeBeds = settings.totalNumberOfHospitalizeBeds
    
        def getBlockFillingOption(surgeryAvgInfoByDoctorSeq : Seq[SurgeryAvgInfoByDoctor]) : BlockFillingOption =
        {
            val surgeryAvgInfoByDoctorsWithTimes = addTimes(settings)(surgeryAvgInfoByDoctorSeq)
            
            val newSurgeriesRestingDistributions = surgeryAvgInfoByDoctorsWithTimes.map
            {
                case(surgery, minutes) => operationToRestingDistributionMapping(surgery.operationCode) + minutes
            }
            val totalRestingBedsDistribution = plannedSurgeriesRestingBedsDistributions.map
            {
                case (i, distribution) =>
                {
                    
                    val newSurgeriesRestingIndicators = newSurgeriesRestingDistributions.map(_.indicatorLessThenEq(i).opposite)
                    val newSurgeriesRestingBedsDistribution = IntegerDistribution.sumAndTrim(newSurgeriesRestingIndicators, distributionMaxLength)
                    IntegerDistribution.sumAndTrim(distributionMaxLength)(distribution, newSurgeriesRestingBedsDistribution)
                }
            }
            val chanceForRestingShort = totalRestingBedsDistribution.map(_.indicatorLessThenEq(totalNumberOfRestingBeds).no).max
    
            val newSurgeriesHospitalizationDistributions = surgeryAvgInfoByDoctorsWithTimes.map
            {
                case (surgery, minutes) => operationToHospitalizationDistributionMapping(surgery.operationCode) + minutes / 60
            }
            val totalHospitalizeBedsDistribution = plannedSurgeriesHospitalizeBedsDistributions.map
            {
                case (i, distribution) =>
                {   // NOTE -->                                                                                                        // was _.+(i).indicator...
                    val newSurgeriesHospitalizeBedsDistribution = IntegerDistribution.sumAndTrim(newSurgeriesHospitalizationDistributions.map(_.indicatorLessThenEq(i).opposite), distributionMaxLength)
                    IntegerDistribution.sumAndTrim(distributionMaxLength)(distribution, newSurgeriesHospitalizeBedsDistribution)
                }
            }
            val chanceForHospitalizeShort = totalHospitalizeBedsDistribution.map(_.indicatorLessThenEq(totalNumberOfHospitalizeBeds).no).max
    
            val expectedProfitOption = settings.avgSurgeryProfit.map
            {
                avgSurgeryProfit => surgeryAvgInfoByDoctorSeq.map(surgery => operationToProfitMapping.getOrElse(surgery.operationCode, avgSurgeryProfit)).sum
            }
    
            val firstSurgery = surgeryAvgInfoByDoctorSeq.head
            BlockFillingOption(
                doctorId = firstSurgery.doctorId,
                doctorName = doctorNameMapping.get(firstSurgery.doctorId),
                surgeries = surgeryAvgInfoByDoctorSeq.map(_.operationCode).map(operationToSurgeryBasicInfoMapping(_)),
                chanceForRestingShort = round(chanceForRestingShort, 2),
                chanceForHospitalizeShort = round(chanceForHospitalizeShort, 2),
                expectedProfit = expectedProfitOption,
                doctorPriority = doctorsPriorityMap(firstSurgery.doctorId)
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
            threshold = LocalDateTime.now().minusMonths(monthsToGoBack).toDate.getTime
            doctorAvailability = pasteSurgeries.filter(_.blockStartMillis > threshold).map(surgery => DoctorAvailability(surgery.doctorId, new LocalDateTime(surgery.blockStartMillis).getDayOfWeek)).toSet
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
    
                hospital = surgeryStatisticsIterable.map(_.hospitalizationDistribution.support.size)
                hospitalMax = hospital.max
                hospitalAVG = hospital.sum / hospital.size.toDouble
                resting = surgeryStatisticsIterable.map(_.restingDistribution.support.size)
                restingMax = resting.max
                restingAVG = resting.sum / resting.size.toDouble
                _ = println(s"hospitalMax = $hospitalMax \nrestingMax = $restingMax")
                _ = println(s"hospitalAVG = $hospitalAVG \nrestingAVG = $restingAVG")
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
        case PastSurgeryInfo(_, _, surgeryDurationMinutes, restingMinutes, hospitalizationHours, _) =>
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
