
package coc.agent.engine;
import java.io.*;

/** **********************************************************************
 * Predicate functions (is X of type Y?).
 *
 * <P>
 */

public class PredFunctions implements Userpackage, Serializable
{
  /**
   * Add these functions to a Rete object.
   * @param engine The Rete object
   */
  public void add(Rete engine) 
  {
    engine.addUserfunction(new EvenP());
    engine.addUserfunction(new OddP());
    engine.addUserfunction(new TypeP("lexemep", RU.ATOM | RU.STRING));
    engine.addUserfunction(new TypeP("numberp", RU.INTEGER | RU.FLOAT | RU.LONG));
    engine.addUserfunction(new TypeP("longp", RU.LONG));
    engine.addUserfunction(new TypeP("floatp", RU.FLOAT));
    engine.addUserfunction(new TypeP("integerp", RU.INTEGER));
    engine.addUserfunction(new TypeP("stringp", RU.STRING));
    engine.addUserfunction(new TypeP("symbolp", RU.ATOM));
    engine.addUserfunction(new TypeP("multifieldp", RU.LIST));
    engine.addUserfunction(new TypeP("external-addressp",
                                     RU.EXTERNAL_ADDRESS));
  }
}


class EvenP implements Userfunction, Serializable
{
  public String getName() { return "evenp";}
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {

    boolean b = ((((int) vv.get(1).numericValue(context)) % 2) == 0);
    return b ? Funcall.TRUE : Funcall.FALSE;
  }
}

class OddP implements Userfunction, Serializable
{
  public String getName() { return "oddp";}
  
  public Value call(ValueVector vv, Context context) throws ReteException 
  {

    boolean b = ((((int) vv.get(1).numericValue(context)) % 2) == 0);
    return b ? Funcall.FALSE : Funcall.TRUE;
  }
}

class TypeP implements Userfunction, Serializable
{
  private String m_name;
  private int m_type;
  TypeP(String name, int type) {m_name = name; m_type = type;}
  public String getName() { return m_name;}
  
  public Value call(ValueVector vv, Context context) throws ReteException 
  {
    return ((vv.get(1).resolveValue(context).type() & m_type) != 0) ?
      Funcall.TRUE : Funcall.FALSE;
  }
}






