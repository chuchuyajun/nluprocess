/** **********************************************************************
 * Parent class of all nodes of the pattern network
 *
 * $Id: Node.java,v 1.1 2012/10/02 21:06:08 Buildadmin Exp $
 ********************************************************************** */

package coc.agent.engine;
import java.util.*;
import java.io.*;

/**
 */
public abstract class Node implements Serializable
{
  
  public final static int LEFT     = 0;
  public final static int RIGHT    = 1;

  /**
    How many rules use me?
    */

  public int m_usecount = 0;


  /**
    Succ is the list of this node's successors
    */

  private Vector m_succ;

  public final Vector succ() { return m_succ; }
  Node [] m_localSucc;
  int m_nsucc;

  /**
   * Constructor
   */

  Node()
  {
    m_succ = new Vector();
  }

  /**
    Move the successors into an array
    */
  void freeze() throws ReteException
  {
    synchronized (m_succ)
      {
        m_nsucc = m_succ.size();
        boolean needsWork = (m_localSucc == null || m_localSucc.length != m_nsucc);
        
        if (needsWork)
          m_localSucc = new Node[m_nsucc];
        
        for (int i=0; i< m_nsucc; i++)
          {
            if (needsWork)
              m_localSucc[i] = (Node) m_succ.elementAt(i);
            m_localSucc[i].freeze();
          }
      }   
  }

  /**
   * Cannot use removeElement here. Some Nodes compare equal using equals(),
   * but aren't the same physical node. We only want to remove the ones that
   * are physically equal, not just conceptually.
   */
  void removeSuccessor(Node s)
  {
    for (int i=0; i<m_succ.size(); i++)
      if (s == m_succ.elementAt(i))
        {
          m_succ.removeElementAt(i);
          return;
        }
  }

  /**
   * Do the business of this node.
   */

  boolean callNodeLeft(Token token) throws ReteException
  {
    throw new ReteException("callNodeLeft", "Undefined in class", getClass().getName());
  }

  boolean callNodeRight(Token t) throws ReteException
  {
    throw new ReteException("callNodeRight", "Undefined in class", getClass().getName());
  }

  private Hashtable m_listeners;

  public void addJessListener(JessListener jel)
  {
    if (m_listeners == null)
      m_listeners = new Hashtable();

    m_listeners.put(jel, jel);
  }

  /**
   * @param jel 
   */
  public void removeJessListener(JessListener jel)
  {
    if (m_listeners == null)
      return;

    m_listeners.remove(jel);
    if (m_listeners.size() == 0)
      m_listeners = null;
  }

  void broadcastEvent(int type, Object data) throws ReteException
  {
    if (m_listeners != null && m_listeners.size() != 0)
      {
        Enumeration e = m_listeners.elements();
        JessEvent event = new JessEvent(this, type, data);
        while (e.hasMoreElements())
          {
            ((JessListener) e.nextElement()).eventHappened(event);
          }
      }
  }

  private void readObject(ObjectInputStream stream)
       throws IOException, ClassNotFoundException
  {
    stream.defaultReadObject();
  }

}






