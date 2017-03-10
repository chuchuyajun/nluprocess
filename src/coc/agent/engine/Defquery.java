package coc.agent.engine;

import java.util.*;
import java.io.*;

/**
 * Class used to represent Defqueries. These are constructed by the parser.
 * <P>
 */

public class Defquery extends HasLHS implements Serializable
{
  private Vector m_results = new Vector();
  private Vector m_queryVariables = new Vector();

  Defquery(String name, String docstring, Rete engine) throws ReteException
  {
    super(name, docstring, engine);
  }

  /**
   * Recieve satisfied queries
   * @param token 
   * @param callType 
   * @exception ReteException 
   * @return 
   */
  public synchronized boolean callNodeLeft(Token token)
       throws ReteException 
  {
    broadcastEvent(JessEvent.RETE_TOKEN + LEFT, token);         

    if (token.m_tag == RU.ADD || m_new && token.m_tag == RU.UPDATE)
      m_results.addElement(token);
    
    else if (token.m_tag == RU.REMOVE)
      m_results.removeElement(token);

    else if (token.m_tag == RU.CLEAR)
      m_results.removeAllElements();

    return true;
  }

  /**
   * Get any query results
   */

  synchronized Enumeration getResults()
  {    
    Enumeration e = m_results.elements();
    return e;
  }

  synchronized void clearResults()
  {    
    m_results = new Vector();
  }

  synchronized int countResults()
  {    
    return m_results.size();
  }

  /**
   * Tell this rule to set the LHS up for faster execution
   * @exception ReteException 
   */
  void freeze() throws ReteException
  {
    if (m_frozen)
      return;
    else
      {
        // Build and install query pattern here
        Pattern p = new Pattern(RU.QUERY_TRIGGER + m_name, m_engine, 0);
        int i = 0;
        for (Enumeration e = m_queryVariables.elements(); e.hasMoreElements();i++)          
          p.addTest(RU.DEFAULT_SLOT_NAME, new Test1(Test.EQ, i, (Variable) e.nextElement()));
        
        insertPatternAt(p, 0);
        
        super.freeze();
      }
  }
  
  void addQueryVariable(Variable v)
  {
    m_queryVariables.addElement(v);
  }

  int getNVariables() { return m_queryVariables.size(); }

  /**
   * Pretty-print this rule. The intent is that the output of this function can be
   * reparsed to recreate the rule. 
   * @return Pretty-printed text of rule.
   */

  public String toString() 
  {
    if (!m_frozen)
      throw new IllegalStateException("Can't pretty-print an incomplete defquery");

    List l = new List("defquery", m_name);
    l.indent("   ");
    l.newLine();
    l.addQuoted(m_docstring);
    l.newLine();

    for (Enumeration e = m_patts.elements(); e.hasMoreElements();)
      {
        l.add(e.nextElement());
        l.newLine();
      }
    return l.toString();
  }

}



