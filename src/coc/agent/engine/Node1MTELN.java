package coc.agent.engine;

/** **********************************************************************
 * Test multislot length.
 ********************************************************************** */

class Node1MTELN extends Node1 
{
  private int m_idx, m_len;

  Node1MTELN(int idx, int len)
  {
    m_idx = idx;
    m_len = len;
  }
  
  boolean callNodeRight(Token t) throws ReteException
  {
    if (super.callNodeRight(t))
      return false;

    try
      {
        boolean result = false;
        Fact fact = t.topFact();
        Value s;
        if ((s = fact.get(m_idx)).type() == RU.LIST) 
          {
            ValueVector vv = s.listValue(null);
            if (vv.size() == m_len)
              result = true;
          }
        
        // debugPrint(fact, result);
        
        if (result)
          passAlong(t);
        
        return result;
      }
    catch (ReteException re)
      {
        re.addContext("rule LHS (MTELN)");
        throw re;
      }
    catch (Exception e)
      {
        ReteException re = new ReteException("Node1MTELN.call",
                                             "Error during LHS execution",
                                             e);
        re.addContext("rule LHS (MTELN)");
        throw re;
                                             
      }    
  }

  public String toString()
  {
    return "[Test that the multislot at index " + m_idx + " is " + m_len + " items long]";
  }

  public boolean equals(Object o)
  {
    if (o instanceof Node1MTELN)
      {
        Node1MTELN n = (Node1MTELN) o;
        return (m_idx == n.m_idx && m_len == n.m_len);
      }
    else
      return false;
  }  
}




