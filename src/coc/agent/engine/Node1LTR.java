
package coc.agent.engine;

/** **********************************************************************
 * A test that always passes, and makes calls with calltype RIGHT.
 * Used to build nested NOTs
 ********************************************************************** */

class Node1LTR extends Node
{    
  boolean callNodeLeft(Token t) throws ReteException
  {
    broadcastEvent(JessEvent.RETE_TOKEN + LEFT, t.topFact()); 
    passAlong(t);
    return true;
  }

  void passAlong(Token t) throws ReteException
  {    
    t = t.prepare();
    Node [] sa = m_localSucc;
    for (int j=0; j<m_nsucc; j++) 
      {
        Node s = sa[j];
        // System.out.println(this + " " +  t);
        s.callNodeRight(t);
      }
  }

  public boolean equals(Object o)
  {
    return (o instanceof Node1LTR);
  }

  public String toString() { return "[Right input adapter]"; }

}

