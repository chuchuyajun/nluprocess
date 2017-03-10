package coc.agent.engine;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * A nifty graphical Rete Network viewer for Jess. To use, simply create an instance
 * and call Rete.addUserpackage. The command name is "view".
 * <P> 
 */

public class ViewFunctions implements Userpackage, Serializable
{
  /**
   * Called by a Rete object when you add this to an engine.
   */
  public void add(Rete engine)
  {
    engine.addUserfunction(new View());
    engine.addUserfunction(new Matches());
  }
}

class Matches implements Userfunction, Serializable
{
  public String getName() { return "matches"; }

  public Value call(ValueVector vv, Context context) throws ReteException
  {
    PrintWriter pw = context.getEngine().getOutStream();
    HasLHS r = context.getEngine().findDefrule(vv.get(1).stringValue(context));
    if (r == null)
      throw new ReteException("matches", "No such rule or query",
                              vv.get(1).stringValue(context));
    Vector v = r.getNodes();
    for (int i=0; i< v.size(); i++)
      {
        Node n = (Node) v.elementAt(i);
        if (! (n instanceof NodeTest))
          continue;
        
        pw.println(n);
        if (n instanceof Node2)
          pw.println(((Node2) n).displayMemory());

        pw.println();
      }
    return Funcall.TRUE;
  }
}




/**
 * Actual Userfunction class: now just a trivial shell around Graph
 */
class View implements Userfunction, Serializable
{
  public String getName() { return "view"; }

  public Value call(ValueVector vv, Context context) throws ReteException
  {
    HasLHS r = null;
    if (vv.size() > 1)
      {
        r = context.getEngine().findDefrule(vv.get(1).stringValue(context));
        if (r == null)
          throw new ReteException("view", "No such rule or query",
                                  vv.get(1).stringValue(context));
      }

    // Main view frame and panel
    final Frame f = new Frame("Network View");
    final Graph g = new Graph(context.getEngine(), r);
    f.add(g, "Center");
    f.setSize(500,500);

    Panel p = new Panel();
    // Button to kill window
    Button b = new Button("Quit");
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
          f.setVisible(false);
          f.dispose();
      }
    });    
    p.add(b);

    b = new Button("Home");
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
          g.init();
      }
    });    
    p.add(b);

    f.add("South", p);
    f.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent we) {
        f.setVisible(false);
        f.dispose();
      }
    });
    

    // Show frame
    f.validate();
    f.setVisible(true);   
    return Funcall.TRUE;
  }
}


/**
 * One node in the Rete Network view
 */
class VNode
{
  int m_x, m_y;
  Node m_node;
  Color m_c;

  /**
   * @param x 
   * @param y 
   * @param c 
   * @param node 
   */
  VNode(int x, int y, Color c, Node node)
  {
    m_x = x;
    m_y = y;
    m_node = node;
    m_c = c;
  }
}

/**
 * One inter-node link in the Rete Network view
 */
class VEdge
{
  int m_from, m_to;
  Color m_c;

  /**
   * @param from 
   * @param to 
   * @param c 
   */
  VEdge(int from, int to, Color c)
  {
    m_from = from;
    m_to = to;
    m_c = c;
  }
}

/**
 * The display panel itself
 */
class Graph extends Panel implements MouseListener, MouseMotionListener, JessListener
{  
  int m_nVNodes;
  VNode m_VNodes[];
  int m_nVEdges;
  VEdge m_VEdges[];

  VNode m_pick;
  Node m_show;
  Image m_offscreen;
  Dimension m_offscreensize;
  Graphics m_offgraphics;
  Rete m_engine;

  static final Color m_selectColor = Color.pink;
  static final Color m_VEdgeColor = Color.black;
  static final int WIDTH=20, HW = 20/2, HEIGHT=20, HH = 20/2;
  
  long m_lastMD;

  // 100 rows of nodes ... that'd be some rulebase!
  int [] m_nextSlot = new int[100];

  // Map node types onto node colors.
  Hashtable m_colors = new Hashtable();

  {
    m_colors.put(Node1.class, Color.red);
    m_colors.put(Node1LTR.class, Color.orange);
    m_colors.put(Node1RTL.class, Color.orange);
    m_colors.put(Node2.class, Color.green);
    m_colors.put(NodeNot2.class, Color.yellow);
    m_colors.put(NodeTest.class, Color.blue);
    m_colors.put(Defrule.class, Color.cyan);
  }

  Color [] m_edgeColors = { Color.green, Color.blue };

  HasLHS m_haslhs;

  /**
   * @param r 
   */
  Graph(Rete r, HasLHS dr)
  {
    m_engine = r;
    addMouseListener(this);
    addMouseMotionListener(this);
    setSize(500,500);
    m_haslhs = dr;
    init(); 
  }
  
  /**
   * @param n 
   * @param depth 
   * @return 
   */
  int findVNode(Node n, int depth)
  {
    for (int i = 0 ; i < m_nVNodes ; i++)
      {
        if (m_VNodes[i].m_node == n)
          {
            return i;
          }
      }
    return addVNode(n, depth);
  }

  /**
   * @param n 
   * @return 
   */
  private Color getNodeColor(Node n)
  {
    Color c = (Color) m_colors.get(n.getClass());
    if (c != null)
      return c;

    else if (n instanceof Node1)
      return (Color) m_colors.get(Node1.class);

    else
      return Color.black;
  }

  /**
   * @param callType 
   * @return 
   */
  private Color getEdgeColor(Node n)
  {
    int calltype = (n instanceof NodeTest) ? Node.LEFT : Node.RIGHT;
    if (n instanceof Node1RTL)
      calltype = Node.LEFT;
    
    if (calltype < 0 || calltype > m_edgeColors.length)
      return Color.black;
    else
      return m_edgeColors[calltype];
  }

  /**
   * @param node 
   * @param depth 
   * @return 
   */
  int addVNode(Node node, int depth)
  {
    VNode n = new VNode(++m_nextSlot[depth] * (WIDTH + HW),
                        depth  * (HEIGHT + HH),
                        getNodeColor(node), node);

    if (m_nVNodes == m_VNodes.length)
      {
        VNode[] temp = new VNode[m_nVNodes * 2];
        System.arraycopy(m_VNodes, 0, temp, 0, m_nVNodes);
        m_VNodes = temp;
      }

    m_VNodes[m_nVNodes] = n;
    return m_nVNodes++;
  }

  /**
   * @param from 
   * @param to 
   * @param depth 
   * @param c 
   */
  void addVEdge(Node from, Node to, int depth, Color c)
  {
    VEdge e = new VEdge(findVNode(from, depth), findVNode(to, depth+1), c);
    if (m_nVEdges == m_VEdges.length)
      {
        VEdge[] temp = new VEdge[m_nVEdges * 2];
        System.arraycopy(m_VEdges, 0, temp, 0, m_nVEdges);
        m_VEdges = temp;
      }
    m_VEdges[m_nVEdges++] = e;
  }
  
  /**
   * @param g 
   * @param n 
   */
  public void paintVNode(Graphics g, VNode n)
  {
    int x = n.m_x;
    int y = n.m_y;
    g.setColor((n == m_pick) ? m_selectColor : n.m_c);
    int w = WIDTH;
    int h = HEIGHT;
    g.fillRect(x - w/2, y - h / 2, w, h);
    g.setColor(Color.black);
    g.drawRect(x - w/2, y - h / 2, w-1, h-1);
  }
  
  /**
   * @param g 
   */
  public synchronized void update(Graphics g) { paint(g); }
  /**
   * @param g 
   */
  public synchronized void paint(Graphics g)
  {
    Dimension d = getSize();
    if ((m_offscreen == null) ||
        (d.width != m_offscreensize.width) || (d.height != m_offscreensize.height))
      {
        m_offscreen = createImage(d.width, d.height);
        m_offscreensize = d;
        m_offgraphics = m_offscreen.getGraphics();
        m_offgraphics.setFont(getFont());
      }
    
    m_offgraphics.setColor(getBackground());
    m_offgraphics.fillRect(0, 0, d.width, d.height);
    for (int i = 0 ; i < m_nVEdges ; i++)
      {
        VEdge e = m_VEdges[i];
        int x1 = m_VNodes[e.m_from].m_x;
        int y1 = m_VNodes[e.m_from].m_y;
        int x2 = m_VNodes[e.m_to].m_x;
        int y2 = m_VNodes[e.m_to].m_y;
        m_offgraphics.setColor(e.m_c) ;
        m_offgraphics.drawLine(x1, y1, x2, y2);
      }
    
    for (int i = 0 ; i < m_nVNodes ; i++)
      {
        paintVNode(m_offgraphics, m_VNodes[i]);
      }

    FontMetrics fm = m_offgraphics.getFontMetrics();
    
    if (m_show != null)
      {
        m_offgraphics.setColor(Color.black) ;
        String s = m_show.toString();
	int h = fm.getHeight();
	m_offgraphics.drawString(s, 10, (d.height - h) + fm.getAscent());
      }

    g.drawImage(m_offscreen, 0, 0, null);
  }
  
  /**
   * @param e 
   */
  public void mouseClicked(MouseEvent e)
  {
  }
  
  /**
   * @param e 
   */
  public void mousePressed(MouseEvent e) 
  {
    int bestdist = Integer.MAX_VALUE;
    int x = e.getX();
    int y = e.getY();
    for (int i = 0 ; i < m_nVNodes ; i++)
      {
        VNode n = m_VNodes[i];
        int dist = (n.m_x - x) * (n.m_x - x) + (n.m_y - y) * (n.m_y - y);
        if (dist < bestdist)
          {
            m_pick = n;
            bestdist = dist;
          }
      }
    
    if (bestdist > 200)
      m_pick = null;
    else
      {
        m_pick.m_x = x;
        m_pick.m_y = y;
      }
    repaint();
    e.consume();
  }
    
  /**
   * @param e 
   */
  public void mouseReleased(MouseEvent e)
  {
    try
      {
        long interval = System.currentTimeMillis() - m_lastMD;
        if (interval < 500)
          {
            new NodeViewer(m_pick.m_node, m_engine);
            m_lastMD = 0;
            return;
          }
        
        else if (m_pick != null)
          {
            m_pick.m_x = e.getX();
            m_pick.m_y = e.getY();
            m_lastMD = System.currentTimeMillis();
          }
      }
    finally
      {
        m_pick = null;
        repaint();
        e.consume();    
      }
  }
  
  /**
   * @param e 
   */
  public void mouseEntered(MouseEvent e)
  {
  }
  
  /**
   * @param e 
   */
  public void mouseExited(MouseEvent e)
  {
  }
  
  /**
   * @param e 
   */
  public void mouseDragged(MouseEvent e)
  {
    if (m_pick != null)
      {
        m_pick.m_x = e.getX();
        m_pick.m_y = e.getY();
        repaint();
      }
    e.consume();
  }
    
  /**
   * @param e 
   */
  public void mouseMoved(MouseEvent e)
  {
    int bestdist = Integer.MAX_VALUE;
    int x = e.getX();
    int y = e.getY();
    Node over = null;
    for (int i = 0 ; i < m_nVNodes ; i++)
      {
        VNode n = m_VNodes[i];
        int dist = (n.m_x - x) * (n.m_x - x) + (n.m_y - y) * (n.m_y - y);
        if (dist < bestdist)
          {
            over = n.m_node;
            bestdist = dist;
          }
      }
    
    if (bestdist > 200)
      m_show = null;
        
    else
      m_show = over;

    repaint();
  }
    
  /**
   * @param n 
   * @param depth 
   */
  private void buildNetwork(Node n, int depth)
  {
    if (m_haslhs != null && !ruleContains(n))
      return;

    for (int i=0; i<n.succ().size(); i++)
      {
        Node s = (Node) n.succ().elementAt(i);
        if (m_haslhs != null && !ruleContains(s))
          continue;
        addVEdge(n, s, depth, getEdgeColor(n));
        buildNetwork(s, depth + 1);
      }
  }
      
  private boolean ruleContains(Node n)
  {    
    Vector v = m_haslhs.getNodes();
    for (int i=0; i<v.size(); i++)
      if (n == v.elementAt(i))
        return true;
    return false;
  }


  public void init()
  {
    m_VNodes = new VNode[10];
    m_VEdges = new VEdge[10];
    m_nVNodes = m_nVEdges = 0;
    m_pick = null;
    m_show = null;
    
    for (int i=0; i<m_nextSlot.length; i++)
      m_nextSlot[i] = 0;

    for (int i=0; i<m_engine.getCompiler().roots().size(); i++)        
      buildNetwork((Node) m_engine.getCompiler().roots().elementAt(i), 1);

    repaint();
  }
    
  /**
   * @param je 
   */
  public void eventHappened(JessEvent je)
  {
    if ((je.getType() & JessEvent.DEFRULE) != 0
        || je.getType() == JessEvent.CLEAR)
      {
        init();
      }
  }

}

/**
 * The detail viewer
 */
class NodeViewer extends Frame implements JessListener
{
  Node m_node;
  Rete m_engine;
  TextArea m_view, m_events;
  /**
   * @param n 
   * @param r 
   */
  NodeViewer(Node n, Rete r)
  {
    super(n.toString());
    m_node = n;
    m_engine = r;
    m_engine.store("NODE", m_node);
    
    m_view = new TextArea(40, 20);;
    m_events = new TextArea(40, 20);;
    m_view.setEditable(false);
    m_events.setEditable(false);

    Panel p = new Panel();
    p.setLayout(new GridLayout(2, 1));
    p.add(m_view);
    p.add(m_events);
    
    Panel p2 = new Panel();
    // Execute debug code
    final TextField tf = new TextField(50);
    p2.add(tf);
    tf.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae)
        {
          try
            {
              m_engine.store("NODE", m_node);
              m_engine.executeCommand(tf.getText());
              tf.setText("");
            }
          catch (ReteException re)
            {
              tf.setText(re.toString());
            }
        }
    });
    Button b = new Button("Clear");
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        tf.setText("");
        m_events.setText("");
      }
    });

    p2.add(b);
    b = new Button("Quit");
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        dispose();
        m_node.removeJessListener(NodeViewer.this);
        
      }
    });
    p2.add(b);
    
    add(p2, "South");
    add(p, "Center");
    describeNode();
    setSize(600, 600);
    validate();
    //modified by zhongshan for GUIEventCleanUp
	setVisible(true);
	toFront();
    m_node.addJessListener(this);
    m_engine.addJessListener(this);
  }


  void describeNode()
  {
    StringBuffer sb = new StringBuffer();
    if (m_node instanceof Node1)    
      {
        sb.append(m_node);
      }
    else if (m_node instanceof Node2)
      {
        sb.append(m_node);
        sb.append(((Node2) m_node).displayMemory());
      }
    else if (m_node instanceof NodeTest)
      {
        sb.append(m_node);
      }
    else if (m_node instanceof HasLHS)
      {
        sb.append(m_node);
      }

    m_view.setText(sb.toString());
  }

  /**
   * @param je 
   * @exception ReteException 
   */
  public void eventHappened(JessEvent je) throws ReteException
  {
    Object o = je.getSource();
    if (o == m_node)
      {
        Token t = (Token) je.getObject();    
        int type = je.getType();
        if (m_view != null)
          m_events.append(type + ": " + t + "\n");
      }
    else if (o == m_engine)
      {
        describeNode();
      }

  }
}


