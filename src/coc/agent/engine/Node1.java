
package coc.agent.engine;

import java.io.*;

/** **********************************************************************
 * Single-input nodes of the pattern network
 * <P>
 ********************************************************************** */

abstract class Node1 extends Node implements Serializable
{
  /**
   * Do the business of this node.
   * The input token of a Node1 should only be single-fact tokens.
   *
   * RU.CLEAR means flush two-input ndoe memories; we just pass these along.
   * All one-input nodes must call this and just immediately return *false*
   * if it returns true!
   */

  boolean callNodeRight(Token t) throws ReteException
  {
    broadcastEvent(JessEvent.RETE_TOKEN + RIGHT, t.topFact()); 
    if (t.m_tag == RU.CLEAR)
      {
        passAlong(t);
        return true;
      }
    return false;
  }

  // Nodes that will store values should call this to clean them up.
  Value cleanupBindings(Value v) throws ReteException 
  {
    if (v.type() == RU.INTARRAY)
      v.intArrayValue(null)[0] = 0;

    else if (v.type() == RU.FUNCALL)
      {
        ValueVector vv = v.funcallValue(null);
        for (int i=0; i<vv.size(); i++) 
          cleanupBindings(vv.get(i));
      }
    return v;
  }

  void passAlong(Token t) throws ReteException
  {        
    t = t.prepare();
    Node [] sa = m_localSucc;
    for (int j=0; j<m_nsucc; j++) 
      {
        Node s = sa[j];
        s.callNodeRight(t);
      }
  }

  /**
   * callNode can call this to print debug info
   */
  void debugPrint(Token t, boolean result) throws ReteException
  {
    
    System.out.println(this + " " + t.topFact() + " => " + result);
  }
    
}




