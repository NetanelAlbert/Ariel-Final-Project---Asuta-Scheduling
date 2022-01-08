package view.common

case class ComparableOptionWithFallbackToString[T](value : Option[T])(implicit ev : T => Ordered[T]) extends Ordered[ComparableOptionWithFallbackToString[T]]
{
    override def toString = value.map(_.toString).getOrElse("Unknown")
    
    override def compare(that : ComparableOptionWithFallbackToString[T]) : Int =  (this.value, that.value) match
    {
        case (None, None) => 0
        
        case (Some(_), None) => 1
        
        case (None, Some(_)) => - 1
        
        case (Some(thisV), Some(thatV)) => thisV.compare(thatV)
    }
}

object ComparableOptionWithFallbackToString
{
    object Empty extends ComparableOptionWithFallbackToString[Int](None)
    {
        override def toString = "Unknown"
    
        override def compare(that : ComparableOptionWithFallbackToString[Int]) : Int =  0
    }
}
