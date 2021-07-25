package model.probability

import scala.collection.immutable.HashMap
// todo find a way to make it generic and be able to multiply in expectation()
class IntegerDistribution(m_distribution: HashMap[Int, Double]) extends RandomVariable[Int]
{
    if(Math.abs(m_distribution.values.sum-1) > 0.0001)
    {
        throw new RuntimeException("The sum of all probabilities has to be 1 bu it is "+m_distribution.values.sum)
    }
    
    override def support : Set[Int] =
    {
        m_distribution.keySet
    }
    
    override def p(element : Int) : Double =
    {
        m_distribution(element) //TODO - what id the exact value isn't in the map?
    }
    
    override def cumulativeP(element : Int)=
    {
        m_distribution.filterKeys(_ <= element).values.sum
    }
    
    override def cumulativePRangeExclude(begin : Int, end : Int)=
    {
        m_distribution.filterKeys(x => {begin < x && x < end}).values.sum
    }
    
    override def expectation=
    {
        m_distribution.map(x => x._1*x._2).sum
    }
    
    override def +(that : RandomVariable[Int]) : RandomVariable[Int] = ???
}

object IntegerDistribution
{
    def sum (vars: Seq[IntegerDistribution]) : IntegerDistribution = ???   // maybe need for summing a lot at once
}