package coc.agent.engine;

import java.util.*;
import java.io.*;

/** **********************************************************************
 * Class used to represent deffacts. Note that you can create Deffacts objects
 * and add them to a Rete engine using Rete.addDeffacts().
 * <P>
 ********************************************************************** */

public class Deffacts implements JessListener, Serializable
{
  
  private String m_name;
  private Vector m_facts;
  private String m_docstring = "", m_string = "";

  /**
   * Fetch the name of thie deffacts
   * @return the name
   */
  public final String getName() { return m_name; }

  /**
   * Fetch the documentation comment, if any, for this deffacts
   * @return the documentation string
   */
  public final String getDocstring() { return m_docstring; }

  /**
   * Create a deffacts
   * @param name The name of the deffacts
   * @param docstring A documentation string
   */

  public Deffacts(String name, String docstring) 
  {
    m_name = name;
    m_facts = new Vector();
    m_docstring = docstring;
  }
  
  /**
   * Add a fact to this deffacts
   * @param fact The fact to add
   */
  
  public void addFact(Fact fact) 
  {
    m_facts.addElement(fact);
  }

  /**
   * Fetch the number of facts in this deffacts
   * @return the number of facts
   */  

  public int getNFacts() { return m_facts.size(); }

  /**
   * Fetch a single Fact from this deffacts
   * @param idx the o-based index of the desired fact
   * @return the idx'th fact
   */  

  public ValueVector getFact(int idx) { return (ValueVector) m_facts.elementAt(idx); }

  /**
   * Assert my facts into engine. Called in response to an event, not directly by you.
   * A Deffacts will also remove itself from the engine in response to a CLEAR event.
   * @param je An event in the engine; 
   * @exception ReteException If anything goes wrong.
   */

  public void eventHappened(JessEvent je) throws ReteException
  {    
    switch((int) je.getType())
    {
      case (int) JessEvent.RESET:    
      {        
        try
          {
            Rete engine = (Rete) je.getSource();
            Context gc = engine.getGlobalContext();
            for (int j=0; j<m_facts.size(); j++)
              {                
                engine.expandAndAssert((Fact) m_facts.elementAt(j), gc);
              }
            break;
          }
        catch (ReteException re)
          {
            re.addContext("assert from deffacts " + m_name);
            throw re;
          }        
      }
      case (int) JessEvent.CLEAR:
      {
        ((Rete) je.getSource()).removeJessListener(this);
        break;
      }
    }
  }

  /**
   * Describe myself
   * @return A string representation of this deffacts
   */

  public String toString() 
  {
    List l = new List("deffacts", m_name);
    l.addQuoted(m_docstring);

    for (Enumeration e = m_facts.elements(); e.hasMoreElements();)
      l.add((Fact) e.nextElement());

    return l.toString();
  }

}

