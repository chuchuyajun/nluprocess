package coc.agent.engine;

/** **********************************************************************
 * Test multislot value and type for inequality. 
 ********************************************************************** */
class Node1MTNEQ extends Node1 
{

  private int m_idx, m_subidx;
  private Context m_context;
  private Value m_value;

  Node1MTNEQ(int idx, int subidx, Value val, Rete engine) throws ReteException
  {
    m_idx = idx;
    m_subidx = subidx;
    m_value = cleanupBindings(val);
    m_context = new Context(engine.getGlobalContext());
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
            if (vv.size() >= m_subidx) 
              {
                Value subslot = vv.get(m_subidx);
                if (m_value.type() == RU.FUNCALL) 
                  {
                    m_context.setFact(fact);
                    m_context.setToken(t);
                    
                    if (m_value.resolveValue(m_context).equals(Funcall.FALSE))
                      result = true;
                  }
                else
                  if ( !subslot.equals(m_value.resolveValue(m_context)))
                    result = true;
              }
          }
        
        if (result)
          passAlong(t);
        
        //debugPrint(token, callType, fact, result);
        return result;
      }
    catch (ReteException re)
      {
        re.addContext("rule LHS (MTNEQ)");
        throw re;
      }
    catch (Exception e)
      {
        ReteException re = new ReteException("Node1MTNEQ.call",
                                             "Error during LHS execution",
                                             e);
        re.addContext("rule LHS (MTNEQ)");
        throw re;
                                             
      }    
  }

  public String toString()
  {
    return "[Test that subslot " + m_subidx + " of multislot " + m_idx +
      " does not equal " + m_value + "]";
  }

  public boolean equals(Object o)
  {
    if (o instanceof Node1MTNEQ)
      {
        Node1MTNEQ n = (Node1MTNEQ) o;
        return (m_idx == n.m_idx && m_subidx == n.m_subidx && m_value.equals(n.m_value));
      }
    else
      return false;
  }
}

