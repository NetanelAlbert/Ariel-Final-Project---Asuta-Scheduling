package model.liatAlgorithm

import model.probability.RandomVariable

import java.awt.Color
import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
import scala.collection.mutable

class MutableGraph(nodes : Seq[Int])
{
    private val m_nodes = new mutable.HashMap[Int, Node]() ++
        nodes.map(node => node -> Node(node))
    
    def this(){
        this(Seq())
    }
    
    def += (num : Int)
    {
        m_nodes += num -> Node(num)
    }
    
    def vertices = m_nodes.keySet
    
    def connect(src : Int, dest : Int, weight : Double)
    {
        m_nodes(src).m_neighbors += dest -> weight
    }
    
    def isConnected(src : Int, dest : Int) =
    {
        m_nodes(src).m_neighbors.contains(dest)
    }
    
    def weightOf(src : Int, dest : Int) : Double =
    {
        m_nodes(src).m_neighbors(dest)
    }
    
    def neighborsOf(node : Int) =
    {
        m_nodes(node).m_neighbors.keySet
    }
    
    def setParent(node : Int, index :Int, parent : Int)
    {
        if(isConnected(parent, node))
            m_nodes(node).m_parents(index) = parent
        else
            throw new RuntimeException("No such element: "+parent)
    }
    
    def parentOf(node : Int, index :Int) : Option[Int] =
    {
        m_nodes(node).m_parents.get(index)
    }
    
    def setDistance(node : Int, index :Int, distance : Double)
    {
            m_nodes(node).m_distances(index) = distance
    }
    
    def distanceOf(node : Int, index :Int) : Double =
    {
        m_nodes(node).m_distances.getOrElse(index, Double.PositiveInfinity)
    }
    
    def setState(node : Int, state : Color)
    {
        m_nodes(node).m_state = state
    }
    
    def stateOf(node : Int) =
    {
        m_nodes(node).m_state
    }
    
    def reset(node : Node)
    {
        node.m_parents.clear()
        node.m_distances.clear()
        node.m_state = Color.WHITE
    }
    
    def resetNodes()
    {
        m_nodes.values.foreach(reset)
    }

    /*
        todo - ask Liat why do we need this.
         We know how the graph will look like - and the order will be by the node number.
         because each number pointing to every number greater the it.
     */
    def topologicalOrder() : Seq[Int] =
    {
        val order = new java.util.ArrayDeque[Int](m_nodes.size)
        val enter = vertices.to[mutable.Set]
        
        def dfs(node : Int)
        {
            setState(node, Color.GRAY)
            for(child <- neighborsOf(node))
            {
                val state = stateOf(child)
                if(state == Color.GRAY)
                    throw new RuntimeException("There is a cycle")
                if(state == Color.WHITE){ // same as != black
                    enter.remove(child)
                    dfs(child)
                }
            }
            order.addFirst(node)
            setState(node, Color.BLACK)
        }
        
        while (enter.nonEmpty)
        {
            val head = enter.head
            dfs(head)
            enter.remove(head)
        }
        
        order.toSeq
    }
    
    private case class Node (m_id : Int) extends Ordered[Node]
    {
        val m_neighbors = mutable.Map[Int, Double]() // (Node, weight)
        val m_parents : mutable.Map[Int, Int] = mutable.Map()
        val m_distances : mutable.Map[Int, Double] = mutable.Map() // or destination?
        var m_state = Color.WHITE
        
        // for the sort in the start of topological sort (maybe unnecessary)
        override def compare(that: Node) = this.m_neighbors.size compare that.m_neighbors.size
    }
    
}

object MutableGraph
{
    def apply(prob : RandomVariable[Int]) : MutableGraph =
    {
        val nodes = prob.support.toSeq.sorted :+ Int.MaxValue
        val graph = new MutableGraph(nodes)
        
        for(node <- nodes ; ng <- nodes.filter(node < _))
        {
            val weight = prob.cumulativePRangeExclude(node, ng)
            graph.connect(node, ng, weight)
        }
        
        graph
    }
}
