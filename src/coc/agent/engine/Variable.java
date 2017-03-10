
package coc.agent.engine;
import java.io.*;

/** **********************************************************************
 * A class to represent a Jess variable. It is 'self-resolving' using Context.
 * <P>
 ********************************************************************** */

public class Variable extends Value implements Serializable
{
  /**
   * Create a Variable
   * @param name The nameof the variable
   * @param type RU.VARIABLE or RU.MULTIVARIABLE
   * @exception ReteException If the type is invalid
   */

  public Variable(String name, int type) throws ReteException 
  {
    super(name, type);
    if (name.indexOf("?") != -1 || name.indexOf("$") != -1)
      throw new ReteException("Variable.Variable",
                              "Variable name cannot contain '?' or '$'",
                              name);
        
  }

  /**
   * Will resolve the variable (return the value it represents.)
   * @param c An evaluation context. Cannot be null!
   * @return The value of this variable
   * @exception ReteException If the variable is undefined
   */

  public Value resolveValue(Context c) throws ReteException
  {
    Binding b;
    if (c == null)
      throw new ReteException("Variable.resolveValue",
                              "Null context for",
                              variableValue(c));

    else if ((b = c.findBinding(variableValue(c))) == null)
      throw new ReteException("Variable.resolveValue",
                              "Undefined variable (no binding)",
                              variableValue(c));

    else if (b.m_val == null)
      throw new ReteException("Variable.resolveValue",
                              "Undefined variable (no value)",
                              variableValue(c));
    else
      return b.m_val;
  }

  /**
   * Resolves the variable, then returns the value as an external address.
   */

  public final Object externalAddressValue(Context c) throws ReteException 
  {
    return resolveValue(c).externalAddressValue(c);
  }

  /**
   * Resolves the variable, then returns the value as a Fact
   */

  public final Fact factValue(Context c) throws ReteException 
  {
    return resolveValue(c).factValue(c);
  }

  /**
   * Resolves the variable, then returns the value as a list
   */

  public final ValueVector listValue(Context c) throws ReteException 
  {
    return resolveValue(c).listValue(c);
  }
 
  /**
   * Resolves the variable, then returns the value as an int
   */

  public final int intValue(Context c) throws ReteException 
  {
    return resolveValue(c).intValue(c);
  }

  /**
   * Resolves the variable, then returns the value as a float
   */

  public final double floatValue(Context c) throws ReteException 
  {
    return resolveValue(c).floatValue(c);
  }

  /**
   * Resolves the variable, then returns the value as a float
   */

  public final double numericValue(Context c) throws ReteException 
  {
    return resolveValue(c).numericValue(c);
  }

  /**
   * Resolves the variable, then returns the value as an atom
   */

  public final String atomValue(Context c) throws ReteException
  {
    return resolveValue(c).atomValue(c);
  }

  /**
   * Returns the name of this variable
   */

  public final String variableValue(Context c) throws ReteException
  {
    return super.stringValue(c);
  }

  /**
   * Resolves the variable, then returns the value as a string
   */

  public final String stringValue(Context c) throws ReteException 
  {
    return resolveValue(c).stringValue(c);
  }

  /**
   * Resolves the variable, then returns the value as a fact-ID
   */

  public final int factIDValue(Context c) throws ReteException 
  {
    return resolveValue(c).factIDValue(c);
  }

}

