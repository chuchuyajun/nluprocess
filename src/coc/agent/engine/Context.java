package coc.agent.engine;

import java.util.*;
import java.io.*;

/** **********************************************************************
 * An execution context for Funcalls.
 * <P>
 ********************************************************************** */
public class Context implements Serializable
{
  private Hashtable m_bindings;
  private Context m_parent;  
  private boolean m_return;
  private Value m_retval;
  private Rete m_engine;

  Token m_token;
  Fact m_fact;

  /**
   * If this context represents a join network node from a rule LHS,
   * this will return the left input of the node.
   * @return The Token
   */

  public final Token getToken() { return m_token; }
  final void setToken(Token t) { m_token = t; }

  /**
   * If this context represents a join network node from a rule LHS,
   * this will return the right input of the node.
   * @return The ValueVector form of a fact
   */

  public final Fact getFact() { return m_fact; }
  final void setFact(Fact f) { m_fact = f; }


  private boolean m_inAdvice;
  
  boolean getInAdvice() {return m_inAdvice;}
  void setInAdvice(boolean  v) {m_inAdvice = v;}
  

  /**
   * @param c 
   */
  Context(Context c) 
  {
    m_engine = c.m_engine;
    m_parent = c;
  }

  /**
   * @param engine 
   */
  Context(Rete engine)
  {
    m_engine = engine;
    m_parent = null;
  }
  
  /**
   * @return 
   */
  final boolean returning() 
  {
    return m_return;
  }

  /**
   * @param val 
   * @return 
   */
  final Value setReturnValue(Value val) 
  {
    m_return = true;
    m_retval = val;
    return val;
  }

  /**
   * @return 
   */
  final Value getReturnValue() 
  {
    return m_retval;
  }

  final void clearReturnValue() 
  {
    m_return = false;
    m_retval = null;
  }

  /**
   * @return 
   */
  final int nBindings()
  {
    if (m_bindings == null)
      return 0;
    else
      return m_bindings.size();
  }

  /**
   * @return 
   */
  final Hashtable getBindings()
  {
    if (m_bindings == null)
      m_bindings = new Hashtable(10);

    return m_bindings;
  }

  /**
   * @return 
   */
  public final Rete getEngine() 
  {
    return m_engine; 
  }

  /**
   * @return 
   */
  Context push() 
  {    
    return new Context(this);
  }
  
  /**
   * @return 
   */
  Context pop() 
  {
    if (m_parent != null)
      {
        m_parent.m_return = m_return;
        m_parent.m_retval = m_retval;        
        return m_parent;
      }
    else
      return this;
  }

  /**
   * Find a variable table entry by 'chaining' upwards
   * @param key the name of the variable
   * @return the variable's value
   */
  Binding findBinding(String key) 
  {
    Context c = this;
    while (c != null)
      {
        Binding b = (Binding) c.getBindings().get(key);
        if (b != null)
          return b;
        else
          c = c.m_parent;
      }
    return null;
  }
  
  synchronized void removeNonGlobals()
  {
    if (m_bindings == null)
      return;

    Hashtable ht = new Hashtable(10);    
    for (Enumeration e = m_bindings.keys(); e.hasMoreElements();)
      {
        String s = (String) e.nextElement();
        if (s.startsWith("*"))
          ht.put(s, m_bindings.get(s));
      }
    m_bindings = ht;
  }


  /*
   * Make note of a variable binding during parsing,
   * so it can be used at runtime; the fact and slot indexes are for the use
   * of the subclasses. Defrules use factIndex and slotIndex to indicate where in
   * a fact to get data from; Deffunctions just use factIndex to indicate which
   * element of the argument vector to pull their information from.
   * @param name 
   * @param factIndex 
   * @param slotIndex 
   * @param subIndex 
   * @return 
   */
  final Binding addBinding(String name, int factIndex, int slotIndex,
                           int subIndex, int type) 
  {
    Binding b = findBinding(name);
    if (b == null)
      {
        b = new Binding(name, factIndex, slotIndex, subIndex, type);
        m_bindings.put(name, b);
      }
    return b;
  }

  /**
   * Get the value of a variable
   * @param name The name of the variable with no leading '?' or '$' characters
   * @exception ReteException If the variable is undefined
   */
  public Value getVariable(String name) throws ReteException
  {
    Binding b = findBinding(name);
    if (b == null || b.m_val == null)
      throw new ReteException("Context.getVariable", "No such variable", name);
    return b.m_val.resolveValue(this);
  }

  /**
   * Set a (possibly new) variable to some type and value
   * @param name Name of the variable
   * @param value The value of the variable
   */
  public void setVariable(String name, Value value) throws ReteException
  {
    Binding b = findBinding(name);
    if (b == null)
      {
        int type = (value == null) ? RU.NONE : value.type();
        b = new Binding(name, RU.LOCAL, RU.LOCAL, -1, type);
        getBindings().put(name, b);
      }
    b.m_val = value;
  }

  /**
   * Return binding from highest Context on chain
   * @param key 
   * @return 
   */
  Binding findGlobalBinding(String key)
  {
    Context c = this;
    while (c.m_parent != null)
      c = c.m_parent;

    return (Binding) c.getBindings().get(key);
  }

  /**
   * Add binding to highest Context on chain
   * @param name 
   * @param value 
   * @return 
   */
  Binding addGlobalBinding(String name, Value value) throws ReteException
  {
    Binding b;
    if ((b = findGlobalBinding(name)) != null) 
      {
        b.m_val = value;
      }
    else
      {
        b = new Binding(name, value);
        Context c = this;
        while (c.m_parent != null)
          c = c.m_parent;
        c.getBindings().put(name, b);
            
      }
    return b;
  }

  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append("[Context, " + nBindings() + " bindings: ");
    for (Enumeration e = getBindings().keys(); e.hasMoreElements(); )
      {
        Object o = e.nextElement();
        sb.append(o + "=" + m_bindings.get(o) + ";");
      }
    sb.append("]");
    return sb.toString();
  }

}


