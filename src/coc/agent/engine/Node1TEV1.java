package coc.agent.engine;

/** **********************************************************************
 * Test that two slots in the same fact have the same type and value.
 *
 ********************************************************************** */
class Node1TEV1 extends Node1
{
  private int m_idx1, m_idx2, m_subidx1, m_subidx2;

  Node1TEV1(int idx1, int subidx1, int idx2, int subidx2)
  {
    m_idx1 = idx1;
    m_subidx1 = subidx1;
    m_idx2 = idx2;
    m_subidx2 = subidx2;
  }
  
  boolean callNodeRight(Token t) throws ReteException
  {
    if (super.callNodeRight(t))
      return false;

    try
      {
        boolean result = false;
        
        Value v1, v2;
        ValueVector slot;
        Fact fact = t.topFact();
        // make sure both facts are big enough
        
        if (m_subidx1 != -1)  // i.e., first variable is in a multislot
          v1 = fact.get(m_idx1).listValue(null).get(m_subidx1);
        else 
          v1 = fact.get(m_idx1);
        
        if (m_subidx2 != -1)  // i.e., first variable is in a multislot
          v2 = fact.get(m_idx2).listValue(null).get(m_subidx2);
        else 
          v2 = fact.get(m_idx2);
        
        result = (v1.equals(v2));
        
        if (result)
          passAlong(t);
        
        //debugPrint(token, callType, fact, result);
        return result;
      }
    catch (ReteException re)
      {
        re.addContext("rule LHS (TEV1)");
        throw re;
      }
    catch (Exception e)
      {
        ReteException re = new ReteException("Node1TEV1.call",
                                             "Error during LHS execution",
                                             e);
        re.addContext("rule LHS (TEV1)");
        throw re;
                                             
      }    
  }

  public boolean equals(Object o)
  {
    if (o instanceof Node1TEV1)
      {
        Node1TEV1 n = (Node1TEV1) o;
        return (m_idx1 == n.m_idx1 && m_idx2 == n.m_idx2 &&
                m_subidx1 == n.m_subidx1 && m_subidx2 == n.m_subidx2);
                
      }
    else
      return false;
  }  

  public String toString()
  {
    StringBuffer sb = new StringBuffer("[Test that slot #");
    sb.append(m_idx1);
    if (m_subidx1 != -1)
      {
        sb.append(", subindex ");
        sb.append(m_subidx1);        
      }
    sb.append(" == slot#");
    sb.append(m_idx2);
    if (m_subidx2 != -1)
      {
        sb.append(", subindex ");
        sb.append(m_subidx2);        
      }
    sb.append("]");
    return sb.toString();
  }

}

