
package coc.agent.engine;
import java.io.*;

/** **********************************************************************
 * A class to represent an int[], used internally. It is 'self-resolving' using Context
 * <P>
 ********************************************************************** */

class IntArrayValue extends Value implements Serializable
{
  /**
   * @param value 
   * @param type 
   * @exception ReteException 
   */

  private String m_name;
  public IntArrayValue(int[] ia, String name) throws ReteException 
  {
    super(ia);
    m_name = name;
  }

  public Value resolveValue(Context c) throws ReteException
  {
    if (c == null)
      throw new ReteException("IntArrayValue.resolveValue",
                              "Null context ", "");

    Token t = c.getToken();
    Fact f;

    int[] binding = super.intArrayValue(c);
    
    Value var;
    if (t == null || binding[0] == t.size())
      f = c.getFact();
    else
      f = t.fact(binding[0]);

    if (binding[1] == -1) // -1 here means fact-id
      return new Value(f.getFactId(), RU.FACT_ID);
    else
      var = f.get(binding[1]);
    
    if (binding[2] == -1) // -1 here means no subfield
      return var;

    else
      {
        ValueVector subv = var.listValue(null);
        return subv.get(binding[2]);
      }
    
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
    return super.stringValue(c);
  }

  public final String stringValue(Context c) throws ReteException 
  {
    return resolveValue(c).stringValue(c);
  }

  public final int factIDValue(Context c) throws ReteException 
  {
    return resolveValue(c).factIDValue(c);
  }

  public String toString()
  {
    return "?" + m_name;
  }

  /*
    This overrides the overloaded equals() in Value.
  */

  public boolean equals(Value o)
  {
    if (! (o instanceof IntArrayValue))
      return false;

    try
      {
        int [] ia = intArrayValue(null);
        int [] oia = ((IntArrayValue) o).intArrayValue(null);

        if (ia.length != oia.length)
          return false;
        
        for (int i=0; i<ia.length; i++)
          if (ia[i] != oia[i])
            return false;
        return true;

      }
    catch (ReteException je)
      {
        return false;
      }
  }

}


