package coc.agent.engine;

import java.util.Vector;
import java.io.Serializable;
/**
 * The defadvice command defines deffunction-like entities that can later
 * be used to modify the behaviour of other functions.
 * <P>
 * The call should look like this:
 * (defadvice before|after (multifield of function names or ALL)
 *    [(function-call)+)
 */

class Defadvice implements Userfunction, Serializable
{
  static final String BEFORE = "before", AFTER = "after",
    ADVICE = "defadvice", UNADVICE = "undefadvice";
  private String m_name;

  public String getName() { return m_name; }
  
  Defadvice(String name) { m_name = name; }

  private ValueVector functionList(Value v, Rete e) throws ReteException
  {
    switch (v.type())
      {
      case RU.LIST:
        return v.listValue(null);
      case RU.ATOM:
        if (v.equals("ALL"))
          return e.executeCommand("(list-function$)").listValue(null);
        // FALL THROUGH
      default:
        ValueVector functions = new ValueVector();
        functions.add(v);
        return functions;
      }
    
  }

  public Value call(ValueVector vv, Context context) throws ReteException
  {
    Rete rete = context.getEngine();
    if (m_name.equals(ADVICE))
      {
        String kind = vv.get(1).stringValue(context);
        ValueVector functions = functionList(vv.get(2).resolveValue(context), rete);
        
        
        for (int i=0; i<functions.size(); i++)
          {            
            FunctionHolder fh = 
              rete.findFunctionHolder(functions.get(i).atomValue(context));

            if (fh == null)
              throw new ReteException("Defadvice.call",
                                      "Cannot advice a function before defining it", m_name);
            
            Userfunction uf = fh.stripAdvice();

            if (uf == null)
              throw new ReteException("Defadvice.call",
                                      "Cannot advice a function before defining it", m_name);
            
            Advice a = ((kind.equals(BEFORE)) ?
                        (Advice) new BeforeAdvice(uf) : (Advice) new AfterAdvice(uf));
            
            for (int j=3; j<vv.size(); j++)
              a.addAction(vv.get(j));
            
            rete.addUserfunction(a);
            
          }
      }
    else // unadvice
      {
        ValueVector functions = functionList(vv.get(1).resolveValue(context), rete);
        for (int i=0; i<functions.size(); i++)
          {

            FunctionHolder fh = 
              rete.findFunctionHolder(functions.get(i).atomValue(context));

            if (fh == null)
              throw new ReteException("Defadvice.call",
                                      "Cannot advice a function before defining it", m_name);
            
            Userfunction uf = fh.stripAdvice();

            if (uf == null)
              throw new ReteException("Defadvice.call",
                                      "Cannot advice a function before defining it", m_name);

            rete.addUserfunction(uf);        
          }        
      }
    return Funcall.TRUE;
  }
}


class BeforeAdvice implements Advice
{
  private Userfunction m_uf;
  private Vector m_actions = new Vector(3);

  BeforeAdvice(Userfunction uf){ m_uf = uf; }

  public Userfunction getFunction() { return m_uf; }

  public void addAction(Value v) { m_actions.addElement(v); }

  public String getName() { return m_uf.getName(); }

  public Value call(ValueVector vv, Context context) throws ReteException
  {
    boolean oldInAdvice = context.getInAdvice();
    try
      {
        context.setInAdvice(true);
        context.setVariable("argv", new Value(vv, RU.LIST));

        Value v = null;
        for (int i=0; i<m_actions.size(); i++)
          v = ((Value) m_actions.elementAt(i)).resolveValue(context);
        
        if (context.returning())
          {
            context.clearReturnValue();
            return v;
          }

        vv = context.findBinding("argv").m_val.listValue(context);
        return m_uf.call(vv, context);
      }
    catch (ReteException re)
      {
        re.addContext("advice");
        throw re;
      }
    finally
      {
        context.setInAdvice(oldInAdvice);
      }
  }
  
}

class AfterAdvice implements Advice
{
  private Userfunction m_uf;
  private Vector m_actions = new Vector(3);

  AfterAdvice(Userfunction uf)
  {
    m_uf = uf;
  }

  public Userfunction getFunction() { return m_uf; }

  public void addAction(Value v) { m_actions.addElement(v); }

  public String getName() { return m_uf.getName(); }

  public Value call(ValueVector vv, Context context) throws ReteException
  {
    boolean oldInAdvice = context.getInAdvice();
    try
      {
        context.setInAdvice(true);
        Value retval = m_uf.call(vv, context);

        context.setVariable("retval", retval);

        Value v = null;
        for (int i=0; i<m_actions.size(); i++)
          v = ((Value) m_actions.elementAt(i)).resolveValue(context);
        
        if (context.returning())
          {
            context.clearReturnValue();
            return v;
          }
        else
          return retval;
      }
    catch (ReteException re)
      {
        re.addContext("advice");
        throw re;
      }
    finally
      {
        context.setInAdvice(oldInAdvice);
      }
  }
}

