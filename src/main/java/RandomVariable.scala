import scala.collection.immutable.HashMap
// todo find a way to make it generic and be able to multiply in expectation()
class RandomVariable(m_distribution: HashMap[Int, Double])
{
    if(Math.abs(m_distribution.values.sum-1) > 0.0001)
        throw new RuntimeException(
            "The sum of all probabilities has to be 1 bu it is "+m_distribution.values.sum)
    
    def support : Set[Int] =
    {
        m_distribution.keySet
    }
    
    def p(element : Int) : Double =
    {
        m_distribution(element)
    }
    
    def cumulativeP(element : Int)=
    {
        m_distribution.filter(_._1 <= element).values.sum
    }
    
    def cumulativePRangeExclude(begin : Int, end : Int)=
    {
        m_distribution.filter(x => {begin < x._1 && x._1 < end}).values.sum
    }
    
    def expectation()=
    {
        m_distribution.map(x => x._1*x._2).sum
    }
    
    def + (that: RandomVariable) : RandomVariable = ???
    
    
    
    
    
}
object RandomVariable
{
    def sum (vars: Seq[RandomVariable]) : RandomVariable = ???   // maybe need for summing a lot at once
}