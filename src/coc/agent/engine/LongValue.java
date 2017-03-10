
package coc.agent.engine;
import java.io.*;

/** **********************************************************************
 * A class to represent a Java long. 
 * <P>
 ********************************************************************** */

public class LongValue extends Value implements Serializable
{
  private long m_long;

  /**
   * Create a LongValue
   * @param l The long value
   * @exception ReteException If the type is invalid
   */

  public LongValue(long l) throws ReteException 
  {
    super((double) l, RU.LONG);
    m_long = l;
  }

  public final long longValue(Context c) throws ReteException 
  {
    return m_long;
  }

  public final double numericValue(Context c) throws ReteException 
  {
    return (double) m_long;
  }

  public final int intValue(Context c) throws ReteException 
  {
    return (int) m_long;
  }

  public final String stringValue(Context c) throws ReteException 
  {
    return toString();
  }

  public final String toString()
  {
    return new Long(m_long).toString();
  }


  public final boolean equals(Value v)
  {
    if (v.type() != RU.LONG)
      return false;
    else
      return m_long == ((LongValue) v).m_long;
  }
}

