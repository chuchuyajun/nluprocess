package coc.agent.engine;
import java.io.*;

/** **********************************************************************
 * A class to represent a Jess function call stored in a Value.
 * It is 'self-resolving' using Context
 * <P>
 ********************************************************************** */

public class FuncallValue extends Value implements Serializable
{
  /**
   * @param value 
   * @param type 
   * @exception ReteException 
   */

  public FuncallValue(Funcall f) throws ReteException 
  {
    super(f, RU.FUNCALL);
  }

  public Value resolveValue(Context c) throws ReteException
  {
    if (c == null)
      throw new ReteException("FuncallValue.resolveValue",
                              "Null context for",
                              funcallValue(c).toStringWithParens());

    else
      return funcallValue(c).execute(c);
  }

  public final Object externalAddressValue(Context c) throws ReteException 
  {
    return resolveValue(c).externalAddressValue(c);
  }

  public final Fact factValue(Context c) throws ReteException 
  {
    return resolveValue(c).factValue(c);
  }

  public final ValueVector listValue(Context c) throws ReteException 
  {
    return resolveValue(c).listValue(c);
  }

  public final int intValue(Context c) throws ReteException 
  {
    return resolveValue(c).intValue(c);
  }

  public final double floatValue(Context c) throws ReteException 
  {
    return resolveValue(c).floatValue(c);
  }

  public final double numericValue(Context c) throws ReteException 
  {
    return resolveValue(c).numericValue(c);
  }

  public final String atomValue(Context c) throws ReteException
  {
    return resolveValue(c).atomValue(c);
  }

  public final String variableValue(Context c) throws ReteException
  {
    return resolveValue(c).variableValue(c);
  }

  public final String stringValue(Context c) throws ReteException 
  {
    return resolveValue(c).stringValue(c);
  }

  public final int factIDValue(Context c) throws ReteException 
  {
    return resolveValue(c).factIDValue(c);
  }

}

