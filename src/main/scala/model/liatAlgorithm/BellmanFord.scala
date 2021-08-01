package model.liatAlgorithm

object BellmanFord
{
    def apply(graph : MutableGraph, length : Int, source : Int)
    {
        def relax(node : Int, k : Int)
        {
            for (ng <- graph.neighborsOf(node))
            {
                val maxi = Math.max(graph.distanceOf(node, k-1), graph.weightOf(node, ng))
                if (maxi < graph.distanceOf(ng, k))
                {
                    graph.setDistance(ng, k, maxi)
                    graph.setParent(ng, k, node)
                }
            }
        }
        
        graph.resetNodes()
        graph.setDistance(source, 0, 0)
        
        val order = graph.topologicalOrder()
        
        for (i <- 1 to length ; node <- order)
        {
            relax(node, i)
        }
    }
}
