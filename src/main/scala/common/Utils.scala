package common

import model.DTOs.DoctorStatistics
import org.joda.time.format.DateTimeFormat

object Utils
{
    val dateTimeFormat = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm")
    val dateFormat = DateTimeFormat.forPattern("dd/MM/yyyy")
    val javaDateFormat = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormat = DateTimeFormat.forPattern("HH:mm")
    val roomsStart = 85000
    
    def constantNormalizer(constant : Double = 0)(element : Any) : Double = constant
    
    def getNormalizer[T](iterable : Iterable[T])(implicit ordering: Ordering[T], toDouble : T => Double) : T => Double =
    {
        if(iterable.isEmpty || iterable.forall(_ == iterable.head))
        {
            constantNormalizer()
        }
        else
        {
            val mim = iterable.min
            val max = iterable.max
            val diff = max - mim
    
            element => (element - mim) / diff
        }
    }
    
    def doctorStatisticsSeqToMapByID(doctorsStatistics : Seq[DoctorStatistics]) : Map[Int, DoctorStatistics] =
    {
        seqToMap[Int, DoctorStatistics](doctorsStatistics, _.id)
    }
    
    def seqToMap[K, V](seq : Seq[V], mapBy : V => K) : Map[K, V] =
    {
        seq.map(value => mapBy(value) -> value).toMap
    }
    
    def cache[T](cacheOption : Option[T], setCache : T => Unit)(generateValue : => T) : T =
    {
        cacheOption match
        {
            case Some(value) => value
            
            case None =>
            {
                val newValue = generateValue
                setCache(newValue)
                newValue
            }
        }
    }
}
