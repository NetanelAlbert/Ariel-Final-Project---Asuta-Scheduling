package model.probability

import model.liatAlgorithm.{BellmanFord, MutableGraph}

import scala.collection.mutable


case class IntegerDistribution(m_distribution: Map[Int, Double]) extends RandomVariable[Int]
{
    import IntegerDistribution._
    require(Math.abs(m_distribution.values.sum - 1) < EPSILON, s"The sum of all probabilities have to be 1 but it is ${m_distribution.values.sum}")
    
    override def support : Set[Int] =
    {
        m_distribution.keySet
    }
    
    override def p(element : Int) : Double =
    {
        val result = m_distribution.getOrElse(element, 0d)
        if(result < 0 && (- EPSILON) < result)
        {
            0
        }
        else if(1 < result && result < 1 + EPSILON)
        {
            1
        }
        else
        {
            result
        }
    }
    
    override def cumulativeP(element : Int) : Double=
    {
        m_distribution.filterKeys(_ <= element).values.sum
    }
    
    def indicatorLessThenEq(element : Int) : Indicator = new Indicator(cumulativeP(element))
    
    override def cumulativePRangeExclude(begin : Int, end : Int) : Double=
    {
        m_distribution.filterKeys(x => {begin < x && x < end}).values.sum
    }
    
    override def cumulativePRangeExcludeEnd(begin : Int, end : Int) : Double =
    {
        m_distribution.filterKeys(x => {begin <= x && x < end}).values.sum
    }
    
    override def expectation : Double=
    {
        m_distribution.map
        {
            case (k, v) => k * v
        }.sum
    }
    
    def getMin : Int = support.min
    
    def trim(m : Int) : IntegerDistribution =
    {
        if(support.size <= m) return this
        
        val graph = MutableGraph(this)
        val source = getMin
    
        BellmanFord(graph, m, source) // the result is in the graph nodes params
        
        createProb(graph, m, source)
    }
    
    def createProb(graph: MutableGraph,
                   m : Int,
                   source : Int,
                   distribution : mutable.HashMap[Int, Double] = mutable.HashMap(),
                   s : Int = Int.MaxValue,
                   i : Int = 0) : IntegerDistribution =
    {
        if(i == 0)
        {
            distribution.sizeHint(m)
        }
        
        if(s == source)
        {
            return IntegerDistribution(distribution.toMap)
        }
        
        val s_ = graph.parentOf(s, m-i).get
        distribution(s_) = cumulativePRangeExcludeEnd(s_, s)
        
        createProb(graph = graph,
                   m = m,
                   source = source,
                   distribution = distribution,
                   s = s_,
                   i = i + 1)
    }
    
    /**
     * Does NOT trim the result
     */
    override def +(that : RandomVariable[Int]) : IntegerDistribution =
    {
        val map = mutable.HashMap[Int, Double]()
        for(v1 <- support ; v2 <- that.support)
        {
            val v = v1 + v2
            val oldP : Double = map.getOrElse(v, 0)
            val newP : Double = p(v1) * that.p(v2)
            map(v) = oldP + newP
        }
        IntegerDistribution(map.toMap)
    }
    
    /**
     * Create a new IntegerDistribution with adding a constant value to each of the distribution values without chang the probabilities.
     * @param valueToAdd - the value to add.
     */
    def + (valueToAdd : Int) : IntegerDistribution =
    {
        if(valueToAdd == 0)
        {
            this
        }
        else
        {
            val newDistribution = m_distribution.map
            {
                case (oldValue, p) => (oldValue + valueToAdd) -> p
            }
            IntegerDistribution(newDistribution)
        }
    }
}

object IntegerDistribution
{
    val EPSILON = 0.0001
    
    def apply(data : Iterable[Int]) : IntegerDistribution =
    {
        val distribution = data.groupBy(identity).mapValues(_.size.toDouble/data.size)
        new IntegerDistribution(distribution)
    }
    
    def empty() = new IntegerDistribution(Map(0 -> 1))
    
    def sumAndTrim(vars: Seq[IntegerDistribution], length : Int) : IntegerDistribution =
    {
        vars.par.aggregate(IntegerDistribution.empty())(sumAndTrim(length), sumAndTrim(length))
    }
    
    def sumAndTrim(m : Int)(dist1 : IntegerDistribution, dist2 : IntegerDistribution): IntegerDistribution =
    {
        (dist1 + dist2).trim(m)
    }
}

class Indicator(p : Double) extends IntegerDistribution(Map(1 -> p,
                                                            0 -> (1 - p)))
{
    def opposite = new Indicator(no)
    
    def yes = p(1)
    
    def no = p(0)
}
