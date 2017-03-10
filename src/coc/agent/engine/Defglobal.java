package coc.agent.engine;
import java.util.*;
import java.io.*;

/**
 * Class used to represent Defglobals. You can create Defglobals and add them to a
 * Rete engine using Rete.addDefglobal.
 * <P>
 */
public class Defglobal implements JessListener, Serializable
{
  private Binding m_binding;
  
  /**
   * Create a defglobal. Should be added to a Rete object with
   * Rete.addDefglobal. Note that a separate Defglobal object must be created
   * for each global variable, even though one (defglobal) construct may represent
   * more than one such variable.
   *
   * @param name The defglobal's variable name. Note that the name must begin and
   * end with an asterisk.
   * @param val The initial value for the defglobal; can be an RU.FUNCALL value.
   * @exception ReteException If anything goes wrong.
   */
  public Defglobal(String name, Value val) throws ReteException
  {
    m_binding = new Binding(name, val);
  }
  
  /**
   * On a RESET event, reenter myself into engine, but only if the engine thinks it's
   * appropriate (i,e., depending on the current set-reset-globals setting.) On a CLEAR
   * event, unlink myself from the engine.
   * @param je The event
   * @exception ReteException If anything goes wrong.
   */
  public void eventHappened(JessEvent je) throws ReteException
  {
    if (je.getType() == JessEvent.RESET)
      {
        try
          {
            Rete engine = (Rete) je.getSource();
            if (engine.getResetGlobals())
              {
                Value v = m_binding.m_val;  
                Context gc = engine.getGlobalContext();
                Value vv = v.resolveValue(gc);   
                gc.addGlobalBinding(m_binding.m_name, vv);   
              }        
          }
        catch (ReteException re)
          {
            re.addContext("definition for defglobal ?" + m_binding.m_name);
            throw re;
          }
      }
    else if (je.getType() == JessEvent.CLEAR)
      {
        ((Rete) je.getSource()).removeJessListener(this);
      }
  }
  
  /**
   * Get this defglobal's variable name
   * @return The variable name
   */
  public String getName() { return m_binding.m_name; }

  /**
   * Get this defglobal's initialization value. The returned Value may be a
   * simple value, a Variable, or a FuncallValue, so be careful how you
   * interpret it.
   * @return The value this variable was originally initialized to
   */
  public Value getInitializationValue() { return m_binding.m_val; }

  /**
   * Describe myself
   * @return A pretty-printed version of the defglobal, suitable for parsing  
   */
  public String toString()
  {
    List l = new List("defglobal");
    l.add("?" + m_binding.m_name);
    l.add("=");
    l.add(m_binding.m_val);
    return l.toString(); 
  }

}

