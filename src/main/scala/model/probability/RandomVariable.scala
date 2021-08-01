package model.probability

trait RandomVariable[T >: Int]
{
    def support : Set[T]
    
    def p(element : T) : Double
    
    def cumulativeP(element : T) : Double
    
    def cumulativePRangeExclude(begin : T, end : T) : Double
    
    def cumulativePRangeExcludeEnd(begin : Int, end : Int) : Double
    
    def expectation : Double
    
    def getMin : Int
    
    def + (that : RandomVariable[T]) : RandomVariable[T]
}
