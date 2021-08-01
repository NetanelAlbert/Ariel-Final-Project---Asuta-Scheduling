package unit

import model.probability.IntegerDistribution
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Ignore, Matchers}

import scala.util.Random

class IntegerDistributionTest extends FlatSpec with Matchers with BeforeAndAfterAll
{
    "IntegerDistribution" should "Create an empty distribution" in
    {
        val dist = IntegerDistribution()
        
        dist.support shouldBe Set(0)
        dist.p(0) shouldBe 1
        dist.expectation shouldBe 0
    }
    
    it should "Create distribution from bunch of elements" in
    {
        val elements = Random.shuffle(Seq(1, 1, 1, 2, 2, 2, 3, 3))
        val dist = IntegerDistribution(elements)
        
        dist.support shouldBe Set(1, 2, 3)
        
        dist.p(0) shouldBe 0
        dist.p(1) shouldBe 3.0 / 8
        dist.p(2) shouldBe 3.0 / 8
        dist.p(3) shouldBe 2.0 / 8
        
        dist.cumulativeP(2) shouldBe 6.0 / 8
        dist.cumulativePRangeExclude(1, 3) shouldBe 3.0 / 8
        dist.cumulativePRangeExcludeEnd(1, 5) shouldBe 1
        
        dist.expectation shouldBe 1 * 3.0 / 8 + 2 * 3.0 / 8 + 3 * 2.0 / 8
        
        dist.getMin shouldBe 1
    }
    
    it should "Trim successfully" in
    {
        val map = Map(1 -> 1.0 / 4,
                      2 -> 1.0 / 8,
                      3 -> 1.0 / 8,
                      4 -> 1.0 / 8,
                      5 -> 1.0 / 4,
                      6 -> 1.0 / 8)
        val dist = IntegerDistribution(map)
        
        val trimmed = dist.trim(3)
        trimmed shouldBe IntegerDistribution(Map(1 -> 3.0 / 8,
                                                 3 -> 1.0 / 4,
                                                 5 -> 3.0 / 8))
    }
    
    it should "Sum with empty IntegerDistribution" in
    {
        val map = Map(1 -> 3.0 / 8,
                      2 -> 1.0 / 4,
                      3 -> 3.0 / 8)
        val dist = IntegerDistribution(map)
        
        val sumWithEmpty = dist + IntegerDistribution()
        sumWithEmpty shouldBe dist
    }
    
    it should "Sum IntegerDistribution with itself" in
    {
        val map = Map(1 -> 3.0 / 8,
                      2 -> 1.0 / 4,
                      3 -> 3.0 / 8)
        val dist = IntegerDistribution(map)
        
        implicit class And(x : Int)
        {
            def and(y : Int) : Double = map(x) * map(y)
        }
        
        val probOf4 = (2 and 2) + (1 and 3) + (3 and 1)
        
        val sumWithSelfMap = Map(2 -> (1 and 1), // (1+1)
                                 3 -> 2 * (1 and 2), // (1+2, 2+1)
                                 4 -> probOf4, // (2+2, 3+1, 1+3)
                                 5 -> 2 * (2 and 3), // (2+3, 3+2)
                                 6 -> (3 and 3)) // (3+3)
        
        val sumWithSelf = dist + dist
        
        sumWithSelf shouldBe IntegerDistribution(sumWithSelfMap)
    }
    
    it should "Sum two IntegerDistributions" in
    {
        val map1 = Map(20 -> 1.0 / 4,
                       30 -> 3.0 / 4)
        val dist1 = IntegerDistribution(map1)
        
        val map2 = Map(1 -> 3.0 / 8,
                       2 -> 1.0 / 4,
                       3 -> 3.0 / 8)
        val dist2 = IntegerDistribution(map2)
        
        implicit class And(x : Int)
        {
            def and(y : Int) : Double = map1(x) * map2(y)
        }
        
        val sumMap = Map(21 -> (20 and 1),
                         22 -> (20 and 2),
                         23 -> (20 and 3),
                         31 -> (30 and 1),
                         32 -> (30 and 2),
                         33 -> (30 and 3))
        
        dist1 + dist2 shouldBe IntegerDistribution(sumMap)
    }
    
    ignore should "sumAndTrim" in
    {
        // TODO test sumAndTrim() for bunch of distributions
    }
}
