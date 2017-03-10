
package coc.agent.engine;

/** **********************************************************************
 * A test that always passes, but makes calls with calltype LEFT instead of RIGHT.
 ********************************************************************** */

class Node1RTL extends Node1
{    
  boolean callNodeRight(Token t) throws ReteException
  {
    super.callNodeRight(t);
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
        s.callNodeLeft(t);
      }
  }

  public boolean equals(Object o)
  {
    return (o instanceof Node1RTL);
  }

  public String toString() { return "[Left input adapter]"; }

}

