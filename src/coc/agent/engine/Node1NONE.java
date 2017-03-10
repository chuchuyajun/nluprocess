
package coc.agent.engine;

/** **********************************************************************
 * A test that always fails
 ********************************************************************** */

class Node1NONE extends Node1
{    
  boolean callNodeRight(Token t) throws ReteException
  {
    super.callNodeRight(t);
    return false;
  }

  public boolean equals(Object o)
  {
    return (o instanceof Node1NONE);
  }
}

