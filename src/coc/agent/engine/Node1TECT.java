package coc.agent.engine;

/** **********************************************************************
 * Test class type
 ********************************************************************** */
class Node1TECT extends Node1 
{
  private String m_name;
  Node1TECT(String name)
  {
    m_name = name;
  }

  boolean callNodeRight(Token t) throws ReteException
  {
    if (super.callNodeRight(t))
      return false;

    try
      {
        boolean result = false;
        Fact fact = t.topFact();
        // If this fact is of this class, we're done.
        if (result = m_name.equals(fact.getName()))
          ;
        else
          {
            Deftemplate dt = fact.getDeftemplate();
            dt = dt.getParent();
            while (dt != null)
              {
                if (result = m_name.equals(dt.getName()))
                  break;
                dt = dt.getParent();
              }
          }

        // debugPrint(fact, result);

        if (result)  
          passAlong(t);
        
        return result;
      }
    catch (ReteException re)
      {
        re.addContext("rule LHS (TECT)");
        throw re;
      }
    catch (Exception e)
      {
        ReteException re = new ReteException("Node1TECT.call",
                                             "Error during LHS execution",
                                             e);
        re.addContext("rule LHS (TECT)");
        throw re;
                                             
      }    
  }
  
  public String toString()
  {
    return "[Test that fact class is " + m_name + " or a subclass of type " + m_name + "]";
  }

  public boolean equals(Object o)
  {
    if (o instanceof Node1TECT)
      {
        Node1TECT n = (Node1TECT) o;
        return (m_name.equals(n.m_name));
      }
    else
      return false;
  }  

}

