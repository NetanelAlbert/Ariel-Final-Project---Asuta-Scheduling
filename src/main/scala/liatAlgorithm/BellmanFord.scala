package liatAlgorithm

import probability.IntegerDistribution

object BellmanFord
{
    def initialize(distribution : IntegerDistribution, source : Int) =
    {
        val graph = new MutableGraph(distribution.support + Int.MaxValue)
        val nodes = graph.vertices.toSeq.sorted
        for (i <- nodes.indices)
        {
            val nodeI = nodes(i)
            for (j <- i + 1 until nodes.size)
            {
                val nodeJ = nodes(j)
                graph.connect(nodeI, nodeJ, distribution.cumulativePRangeExclude(nodeI, nodeJ))
            }
        }
        graph.setDistance(source, 0)
        graph
    }
    
    
    def bellman_ford(graph : MutableGraph, source : Int, length : Int) : Seq[Int] =
    {
        def relax(node : Int)
        {
            for (ng <- graph.neighborsOf(node))
            {
                val maxi = Math.max(graph.distanceOf(node), graph.weightOf(node, ng))
                if (maxi < graph.distanceOf(ng))
                {
                    graph.setDistance(ng, maxi)
                    graph.setParent(ng, node)
                }
            }
        }
        
        //g = topological(g)
        // todo - do we need topological? I think we will get the natural (numbering) order
        val nodes = graph.vertices.toSeq.sorted
        for (_ <- 1 to length; node <- nodes)
        {
            relax(node)
        }
        
        
        def traceFromSource(node : Int) : Seq[Int] =
        {
            val parent = graph.parentOf(node)
            parent match
            {
                case None => Seq(node)
                case Some(num) => traceFromSource(num) :+ node
            }
        }
        
        val lastNode = graph.parentOf(Int.MaxValue)
        lastNode match
        {
            case None => throw new RuntimeException("Infinity node is not reached")
            case Some(num) => traceFromSource(num)
        }
    }
    
}
