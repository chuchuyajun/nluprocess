package coc.agent.engine;

import java.util.*;
import java.io.*;

/**
 * Class used to represent Defrules. These are constructed by the parser.
 * <P>
 */

public class Defrule extends HasLHS implements Serializable
{
  private Hashtable m_activations = new Hashtable();
  private Funcall[] m_localActions;
  transient private Vector m_actions = new Vector();
  private int m_id;
  private int m_salience;
  private Value m_salienceVal;

  Defrule(String name, String docstring, Rete engine) throws ReteException
  {
    super(name, docstring, engine);
    m_id = engine.nextRuleId();
    m_salience = 0;
    m_salienceVal = new Value(0, RU.INTEGER);
  }

  /**
   * Fetch the salience setting of this rule
   * @return The salience of this defrule
   */

  public final int getSalience() { return m_salience; }

  void setSalience(Value i) throws ReteException
  { m_salienceVal = i; evalSalience();}

  /**
   * Evaluate the salience of this rule. If the salience was set to a Funcall value
   * during parsing, then this function may return different values over time. If
   * the salience is constant, this is equivalent to getSalience.
   * @param engine The Rete engine the rule belongs to
   * @exception ReteException If something goes wrong
   * @return The evaluated salience
   */
  public int evalSalience() throws ReteException
  {
    Context gc = m_engine.getGlobalContext().push();
    try
      {
        m_salience = m_salienceVal.intValue(gc);
      }
    finally
      {
        gc.pop();    
      }
    return m_salience;
  }


  /**
   * Fetch the unique id of this rule.
   * @return The id.
   */
  public int getId() { return m_id; }


  private void doAddCall(Token token) throws ReteException
  {
    Activation a = new Activation(token, this);
    m_engine.addActivation(a);
    m_activations.put(token, a);
  }

  private void possiblyDoAddCall(Token token) throws ReteException
  {
    // We're not new, so updates don't affect us
    if (!m_new)
      return;

    // We've already got this one
    if (m_activations.get(token) != null)
      return;

    // Add a new activation
    doAddCall(token);
  }

  /**
   * All we need to do is create or destroy the appropriate Activation
   * object, which contains enough info to fire a rule.
   * @param token 
   * @param callType 
   * @exception ReteException 
   * @return 
   */
  public boolean callNodeLeft(Token token)
       throws ReteException 
  {
    broadcastEvent(JessEvent.RETE_TOKEN + LEFT, token);     

    switch (token.m_tag) 
      {
      
      case RU.ADD:
        {
          doAddCall(token);
          break;
        }

      case RU.REMOVE:
        {
          Activation a = (Activation) m_activations.remove(token);
          if (a != null)
            m_engine.removeActivation(a, false);
          break;
        }

      case RU.UPDATE:
        {
          possiblyDoAddCall(token);
          break;
        }

      case RU.CLEAR:
        {
          m_activations.clear();
          break;
        }


      }
    return true;
  }

  /**
   * @param fact_input 
   * @param c 
   * @exception ReteException 
   */
  private void ready(Token fact_input, Context c) throws ReteException 
  {

    Fact fact;
    // set up the variable table    
    for (Enumeration e = getBindings().elements(); e.hasMoreElements();) 
      {
        Binding b = (Binding) e.nextElement();
        
        if (b.m_slotIndex == RU.LOCAL)
          // no default binding for locals
          continue;

        // all others variables need info from a fact
        // if this is a not CE, skip it;
        fact = fact_input.fact(b.m_factIndex);
        try 
          {
            if (b.m_slotIndex == RU.PATTERN) 
              {
                b.m_val = new Value(fact.getFactId(), RU.FACT_ID);
              } 
            else 
              {
                if (b.m_subIndex == -1)
                  {
                    b.m_val = fact.get(b.m_slotIndex);
                  }
                
                else 
                  {
                    ValueVector vv = fact.get(b.m_slotIndex).listValue(c);
                    b.m_val = vv.get(b.m_subIndex);
                  }
            
              }
            c.setVariable(b.m_name, b.m_val);
          }
        catch (Throwable t) 
          {
            // bad binding. These can come from unused bindings in not CE's.
          }     
      }
    return;
  }
  

  /**
   * Do the RHS of this rule.For each action (ValueVector form of a Funcall),
   * do two things:
   * 1) Call ExpandAction to do variable substitution and
   * subexpression expansion
   * 2) call Funcall.Execute on it.
   * 
   * Fact_input is the Vector of ValueVector facts we were fired with.
   */
  synchronized void fire(Token fact_input) throws ReteException 
  {
    Context c = new Context(m_engine.getGlobalContext());
    c.clearReturnValue();
    
    // Pull needed values out of facts into bindings table
    ready(fact_input, c);
    
        
    try
      {
        // OK, now run the rule. For every action...
        int size = m_localActions.length;
        for (int i=0; i<size; i++) 
          {
            m_localActions[i].execute(c);
                        
            if (c.returning()) 
              {
                c.clearReturnValue();
                c.pop();
                return;
              }
          }
      }
    catch (ReteException re)
      {
        re.addContext("defrule " + m_name);
        throw re;
      }

    finally
      {
        c.pop();
        m_activations.remove(fact_input);                 
      }
        
  }
    
  /**
   * Tell this rule to set the actions up for faster execution
   * @exception ReteException 
   */
  void freeze() throws ReteException
  {
    if (m_frozen)
      return;
    else
      super.freeze();

    m_localActions = new Funcall[m_actions.size()];
    for (int i=0; i<m_localActions.length; i++)
      m_localActions[i] = (Funcall) m_actions.elementAt(i);    
  }
  

  void debugPrint(Token facts, int seq, PrintWriter ps) throws ReteException 
  {
    ps.print("FIRE ");
    ps.print(seq);
    ps.print(" ");
    ps.print(m_name);
    for (int i=0; i<facts.size(); i++) 
      {
        Fact f = facts.fact(i);
        if (f.getFactId() != -1)
          ps.print(" f-" + f.getFactId());
        if (i< facts.size() -1)
          ps.print(",");
      }
    ps.println();
    ps.flush();
  }
  
  /**
   * Fetch the number of actions on this rule's RHS
   * @return The number of actions
   */
  public int getNActions() { return m_actions.size(); }

  /**
   * Fetch the idx-th RHS action of this rule
   @ @param idx The zero-based index of the action to fetch
   * @return The action as a Funcall
   */
  public Funcall getAction(int idx) { return (Funcall) m_actions.elementAt(idx); }


  /**
   * Add an action to this deffunction
   * @param fc 
   */
  void addAction(Funcall fc) 
  {
    m_actions.addElement(fc);
  }


  /**
   * Pretty-print this rule. The intent is that the output of this function can be
   * reparsed to recreate the rule. 
   * @return Pretty-printed text of rule.
   */

  public String toString() 
  {
    if (!m_frozen)
      throw new IllegalStateException("Can't pretty-print an incomplete rule");

    List l = new List("defrule", m_name);
    l.indent("   ");
    l.newLine();
    l.addQuoted(m_docstring);
    l.newLine();
    try
      {
        l.add(new List("declare",
                       new List("salience", m_salienceVal)).
              add(new List("node-index-hash", new Value(m_nodeIndexHash, RU.INTEGER))));
      }
    catch (ReteException cantHappen) {}
    l.newLine();
    for (Enumeration e = m_patts.elements(); e.hasMoreElements();)
      {
        l.add(e.nextElement());
        l.newLine();
      }
    l.add("=>");
    for (int i=0; i < m_localActions.length; i++)
      {
        l.newLine();
        l.add(m_localActions[i]);
      }
    return l.toString();
  }

  public Vector getRuleActions(){
	  return this.m_actions;
  } 

  public Vector getRulePatts(){
	  return this.m_patts;
  }
  
  public String getRuleName(){
	  return this.m_name;
  }
}



