package model.probability

trait RandomVariable[T]
{
    def support : Set[T]
    
    def p(element : T) : Double
    
    def cumulativeP(element : T) : Double
    
    def cumulativePRangeExclude(begin : T, end : T) : Double
    
    def expectation : Double
    
    def + (that : RandomVariable[T]) : RandomVariable[T]
}
