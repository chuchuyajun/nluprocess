package coc.agent.engine;

import java.util.*;
import java.io.*;

/** **********************************************************************
 * Class used to represent Deffunctions (functions defined in the Jess language).
 * Note that you can create these form Java code and add them to a Rete engine using
 * Rete.addUserfunction. 
 *
 * <P>
 ********************************************************************** */

public class Deffunction implements Userfunction, Serializable
{
  
  private String m_name;
  private String m_docstring = "";
  private int m_nargs;
  private Hashtable m_bindings = new Hashtable();
  private Vector m_argNames = new Vector();
  private Vector m_actions = new Vector();
  private boolean m_hasWildcard = false;

  /**
   * Fetch the name of this Deffunction
   * @return the name
   */
  public final String getName() { return m_name; }

  /**
   * Fetch the documentation string of this Deffunction
   * @return the documentation string
   */
  public final String getDocstring() { return m_docstring; }

  /**
   * Set the documentation string of this Deffunction
   * @param ds The documentation string
   */
  public final void setDocstring(String ds) { m_docstring = ds; }

  /**
   * Create a deffunction
   * @param name the name of the deffunction
   * @param docstring the documentation string
   */
  public Deffunction(String name, String docstring)
  {
    m_name = name;
    m_docstring = docstring;
  }

  /**
   * Add a formal argument to this deffunction. Only the last may be a MULTIVARIABLE.
   * Note that arguments will appear in left-to-right roder acording to the order in which
   * they are added.
   * @param name A name for the variable (without the leading '?')
   * @param type RU.MULTIVARIABLE or RU.VARIABLE
   */
  public void addArgument(String name, int type) throws ReteException
  {
    if (m_hasWildcard)
      throw new ReteException("Deffunction.addArgument", "Deffunction " + m_name +
                              " already has a wildcard argument:", name);
    m_bindings.put(name, new Binding(name, m_argNames.size() + 1, type, -1, RU.NONE));
    m_argNames.addElement(name);
    if (type == RU.MULTIVARIABLE)
      m_hasWildcard = true;
  }

  /**
   * Add an action to this deffunction. The actions and values added to a deffunction
   * will be stored in the order added, and thereby make up the body of the deffunction.
   * @param fc The action
   */
  public void addAction(Funcall fc) throws ReteException
  {
    m_actions.addElement(new FuncallValue(fc));
  }

  /**
   * Add a simple value to this deffunction. The actions and values added to a deffunction
   * will be stored in the order added, and thereby make up the body of the deffunction.
   * @param val The value
   */
  public void addValue(Value val) 
  {
    m_actions.addElement(val);
  }


  /**
   * Execute this deffunction. Evaluate each action or value, in order. If no explicit
   * (return) statement is encountered, the last evaluation result will be returned as the
   * result of this deffunction.
   *
   * @param call The ValueVector form of the function call used to invoke this deffunction.
   * @param c The execution context
   * @exception ReteException If anything goes wrong
   * @return As described above.
   */
  public Value call(ValueVector call, Context context) throws ReteException 
  {
    
    // Clean context
    Context c = new Context(context.getEngine().getGlobalContext());
    c.clearReturnValue();
    Value result = Funcall.NIL;        

    try
      {
        
        if (call.size() < (m_argNames.size() + 1))
          throw new ReteException(m_name,
                                  "Too few arguments to deffunction",
                                  m_name);
               
        // set up the variable table. Note that args are resolved in the parent's context.
        
        for (Enumeration e = m_bindings.elements(); e.hasMoreElements();)
          {
            Binding b = (Binding) e.nextElement();        
            switch (b.m_slotIndex)
              {
                // No default bindings for locals
              case RU.LOCAL:
                if (c.findBinding(b.m_name) == null)
                  c.setVariable(b.m_name, Funcall.NIL);
                continue;
                
                // all others variables come from arguments
              case RU.VARIABLE:            
                c.setVariable(b.m_name, call.get(b.m_factIndex).resolveValue(context));
                break;
                
              case RU.MULTIVARIABLE:
                {
                  ValueVector vv = new ValueVector();              
                  for (int i=b.m_factIndex; i< call.size(); i++)
                    {
                      Value v = call.get(i).resolveValue(context);
                      if (v.type() == RU.LIST)
                        {
                          ValueVector list = v.listValue(context);
                          for (int j=0; j<list.size(); j++)
                            vv.add(list.get(j).resolveValue(context));
                        }
                      else
                        vv.add(v);
                    }
                  c.setVariable(b.m_name, new Value(vv, RU.LIST));
                  break;
                }
              }
          }
        
        // OK, now run the function. For every action...
        int size = m_actions.size();
        for (int i=0; i<size; i++) 
          {
            result = ((Value) m_actions.elementAt(i)).resolveValue(c);
            
            if (c.returning()) 
              {
                result = c.getReturnValue();
                c.clearReturnValue();
                break;
              }
          }
      }
    catch (ReteException re)
      {
        re.addContext("deffunction " + m_name);
        throw re;
      }
    finally
      {
        c.pop();
      }
    return result.resolveValue(c);
  }
  
  /**
   * Describe myself
   * @return a pretty-print representation of this deffunction
   */
  public String toString() 
  {
    List l = new List("deffunction", m_name);
    if (m_argNames.size() > 0)
      {
        List args = new List();
        for (Enumeration e = m_argNames.elements(); e.hasMoreElements();)
          {
            Binding b = (Binding) m_bindings.get(e.nextElement());
            String prefix = b.m_slotIndex == RU.VARIABLE ? "?" : "$?";
            args.add(prefix + b.m_name);
          }
        l.add(args);
      }
    else
      l.add("()");
    l.addQuoted(m_docstring);

    for (Enumeration e = m_actions.elements(); e.hasMoreElements();)
      {
        l.newLine();
        l.add(e.nextElement());
      }
    return l.toString();
  }


}

