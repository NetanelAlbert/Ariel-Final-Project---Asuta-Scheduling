package liatAlgorithm

import java.awt.Color
import scala.collection.mutable

class MutableGraph(nodes : Set[Int])
{
    private val m_nodes = new mutable.HashMap[Int, Node]() ++
        nodes.map(node => node -> Node(node))
    
    def this(){
        this(Set())
    }
    
    def += (num : Int)
    {
        m_nodes += num -> Node(num)
    }
    
    def vertices = m_nodes.keySet
    
    def connect(src : Int, dest : Int, weight : Double)
    {
        m_nodes(src).m_neighbors += dest -> Edge(src, dest, weight)
    }
    
    def isConnected(src : Int, dest : Int) =
    {
        m_nodes(src).m_neighbors.contains(dest)
    }
    
    def weightOf(src : Int, dest : Int) =
    {
        m_nodes(src).m_neighbors(dest).m_weight
    }
    
    def neighborsOf(node : Int) =
    {
        m_nodes(node).m_neighbors.keySet
    }
    
    def setParent(node : Int, parent : Int)
    {
        if(isConnected(parent, node))
            m_nodes(node).m_parent = Some(parent)
        else
            throw new RuntimeException("No such element: "+parent)
    }
    def removeParent(node : Int)
    {
        m_nodes(node).m_parent = None
    }
    
    def parentOf(node : Int) =
    {
        m_nodes(node).m_parent
    }
    
    def setDistance(node : Int, distance : Double)
    {
            m_nodes(node).m_distance = distance
    }
    
    def distanceOf(node : Int) =
    {
        m_nodes(node).m_distance
    }
    
    def setState(node : Int, state : Color)
    {
        m_nodes(node).m_state = state
    }
    
    def stateOf(node : Int) =
    {
        m_nodes(node).m_state
    }

    /*
        todo - ask Liat why do we need this.
         We know how the graph will look like - and the order will be by the node number.
         because each number pointing to every number greater the it.
     */
    def topologicalOrder() =
    {
        val order = new java.util.ArrayDeque[Int](m_nodes.size)
        // enter = set(graph)  todo translate
        def dfs(node : Node)
        {
            node.m_state = Color.GRAY
            for(child <- node.m_neighbors.keySet)
            {
                val state = stateOf(child)
                if(state == Color.GRAY)
                    throw new RuntimeException("There is a cycle")
                if(state == Color.WHITE){
                    //enter.discard(k) todo translate
                    dfs(m_nodes(child))
                }
            }
            order.addFirst(node.m_id)
            node.m_state = Color.BLACK
        }
        // while enter: dfs(enter.pop()) todo translate
        order
    }
    
    private case class Node (m_id : Int) extends Ordered[Node]
    {
        val m_neighbors = mutable.HashMap[Int, Edge]()
        var m_parent : Option[Int] = None
        var m_distance = Double.PositiveInfinity // or destination?
        var m_state = Color.WHITE
        
        // for the sort in the start of topological sort (maybe unnecessary)
        override def compare(that: Node) = this.m_neighbors.size compare that.m_neighbors.size
    }
    
    private case class Edge (m_src : Int, m_dest : Int, m_weight : Double)
}
