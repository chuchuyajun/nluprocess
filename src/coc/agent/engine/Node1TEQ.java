package coc.agent.engine;

/** **********************************************************************
 * Test slot value. 
 ********************************************************************** */
class Node1TEQ extends Node1 
{
  private Value m_value;
  private int m_idx;
  private Context m_context;
  
  Node1TEQ(int idx, Value val, Rete r) throws ReteException
  {
    m_value = cleanupBindings(val);
    m_idx = idx;
    m_context = new Context(r.getGlobalContext());
  }
  
  boolean callNodeRight(Token t) throws ReteException
  {    
    if (super.callNodeRight(t))
      return false;

    boolean result = false;
    Fact fact = t.topFact();
    try
      {        
        if (m_value.type() == RU.FUNCALL) 
          {
            m_context.setFact(fact);
            m_context.setToken(t);
            
            if (!m_value.resolveValue(m_context).equals(Funcall.FALSE))                  
              result = true;
          }
        else if (fact.get(m_idx).equals(m_value.resolveValue(m_context)))
          {
            result = true;
          }

        if (result)
          passAlong(t);
        
        //debugPrint(token, callType, fact, result);
        return result;
      }
    catch (ReteException re)
      {
        re.addContext("rule LHS (TEQ)");
        throw re;
      }
    catch (Exception e)
      {
        ReteException re = new ReteException("Node1TEQ.call",
                                             "Error during LHS execution",
                                             e);
        re.addContext("rule LHS (TEQ)");
        throw re;
                                             
      }    
  }

  public String toString()
  {
    return "[Test that slot at index " + m_idx + " equals " + m_value + "]";
  }

  public boolean equals(Object o)
  {
    if (o instanceof Node1TEQ)
      {
        Node1TEQ n = (Node1TEQ) o;
        return (m_idx == n.m_idx && m_value.equals(n.m_value));
      }
    else
      return false;
  }  

}

