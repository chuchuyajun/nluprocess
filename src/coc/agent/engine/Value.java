package coc.agent.engine;
import java.io.*;

/** **********************************************************************
 * A class to represent a Jess typed value.  Does some 'type conversions'.
 * Subclasses of this are used to represent Variables and other special types.
 * <P>
 ********************************************************************** */

public class Value implements Serializable
{
  static final int STRING_TYPES = RU.ATOM | RU.STRING | RU.VARIABLE |
    RU.MULTIVARIABLE | RU.SLOT | RU.MULTISLOT;

  static final int NUM_TYPES = RU.INTEGER | RU.FLOAT;
  static final int FACT_TYPES = RU.FACT_ID | NUM_TYPES;

  private int             m_type;
  private int            m_intval;
  private double       m_floatval;
  private Object      m_objectval;

  /**
   * Contruct a value of integral type. Allowed type values are NONE,
   * INTEGER, and FACT_ID.
   * @param value The value
   * @param type The type
   * @exception ReteException If the value and type don't match.
   */
  public Value(int value, int type) throws ReteException 
  {
    m_type = type;
    switch (m_type) 
      {
      case RU.NONE:
      case RU.INTEGER:
      case RU.FACT_ID:
        m_intval = value; break;


      default:
        throw typeError("Value", "Not an integral type", type);
      }
  }
  
  /**
   * Contruct a value that is a copy of another Value.
   * @param v Value to copy
   */
  public Value(Value v) 
  {
    m_type = v.m_type;
    m_intval = v.m_intval;
    m_floatval = v.m_floatval;
    m_objectval = v.m_objectval;
  }

  /**
   * Contruct a value of String type. Allowed type values are 
   * RU.ATOM, RU.STRING, RU.SLOT and RU.MULTISLOT.
   * @param value The value
   * @param type The type
   * @exception ReteException If the value and type don't match.
   */

  public Value(String s, int type) throws ReteException 
  {
    if (!(this instanceof Variable) && (type == RU.VARIABLE ||
                                        type == RU.MULTIVARIABLE))
      throw new ReteException("Value.Value",
                              "Cannot use coc.agent.engine.Value to represent variable " + s + ".",
                              "You must use class coc.agent.engine.Variable");

    if ((type & STRING_TYPES) == 0)
      throw typeError("Value", "Not a string type", type);

    m_type = type; 
    m_objectval = s;
  }

  /**
   * Contruct a value of list type. Allowed type values are FACT and LIST.
   * @param value The value
   * @param type The type
   * @exception ReteException If the value and type don't match.
   */

  public Value(ValueVector f, int type) throws ReteException 
  {
    if (!(this instanceof FuncallValue) && type == RU.FUNCALL)
      throw new ReteException("Value.Value",
                              "Cannot use coc.agent.engine.Value to represent the function call " +
                              f.toStringWithParens(),
                              "You must use class coc.agent.engine.FuncallValue");

    if (type != RU.FUNCALL && type != RU.FACT && type != RU.LIST)
      throw typeError("Value", "Not a vector type", type);

    m_type = type;
    m_objectval = f;
  }

  /**
   * Contruct a value of floating-point type. Allowed type values are FLOAT,
   * INTEGER, and FACT_ID.
   * @param value The value
   * @param type The type
   * @exception ReteException If the value and type don't match.
   */

  public Value(double d, int type) throws ReteException 
  {
    if (type != RU.FLOAT && type != RU.INTEGER  && type != RU.FACT_ID && type != RU.LONG)
      throw typeError("Value", "Not a float type", type);

    m_type = type;
    if (type == RU.FLOAT || type == RU.LONG)
      m_floatval = d;
    else
      m_intval = (int) d;
    
  }

  /**
   * Contruct a value of external address type.
   * @param value The value
   */

  public Value(Object o) 
  {
    m_type = RU.EXTERNAL_ADDRESS;
    m_objectval = o;
  }

  Value(int[] a) throws ReteException
  {
    if (!(this instanceof IntArrayValue))
      throw new ReteException("Value.Value",
                              "Cannot use coc.agent.engine.Value to represent int[]s.",
                              "You must use class coc.agent.engine.IntArrayValue");

      
    m_type = RU.INTARRAY;
    m_objectval = a;
  }

  /**
   * Returns the contents of this value, as an int[]
   * @exception ReteException If this value does not contain an int[]
   * @return The int[]
   */
  public int[] intArrayValue(Context c) throws ReteException 
  {
    if (m_type == RU.INTARRAY)
      return (int[]) m_objectval;
    throw typeError("intArrayValue", "Not an int[]");
  }

  /**
   * Returns the contents of this value, as an external adress object
   * @exception ReteException If this value does not contain an object
   * @return The external address object
   */
  public Object externalAddressValue(Context c) throws ReteException 
  {
    switch (m_type)
      {
      case RU.EXTERNAL_ADDRESS:
      case RU.STRING:
      case RU.ATOM:
        return m_objectval;
      }
    throw typeError("externalAddressValue", "Not an external address");
  }

  /**
   * Returns the contents of this value, as a function call.
   * @exception ReteException If this value does not contain a function call
   * @return The function call object
   */

  public Funcall funcallValue(Context c) throws ReteException 
  {
    if (m_type == RU.FUNCALL)
      return (Funcall) m_objectval;
    throw typeError("funcallValue", "Not a function call");        
  }

  /**
   * Returns the contents of this value, as a fact
   * @exception ReteException If this value does not contain a fact
   * @return The fact
   */

  public Fact factValue(Context c) throws ReteException 
  {
    if (m_type == RU.FACT)
      return (Fact) m_objectval;
    throw typeError("factValue", "Not a fact");            
  }

  /**
   * Returns the contents of this value, as a list
   * @exception ReteException If this value does not contain a list
   * @return The list
   */

  public ValueVector listValue(Context c) throws ReteException 
  {
    if (m_type == RU.LIST)
      return (ValueVector) m_objectval;
    throw typeError("listValue", "Not a list");
  }

  /**
   * Returns the contents of this value, as a number
   * @exception ReteException If this value does not contain any kind of number
   * @return The number as a double
   */

  public double numericValue(Context c) throws ReteException 
  {
    Value v = resolveValue(c);
    switch (v.m_type)
      {
      case RU.FLOAT:
        return v.m_floatval;
      case RU.FACT_ID:
      case RU.INTEGER:
        return  v.m_intval;
      case RU.STRING:
      case RU.ATOM:
        try
          {
            return Double.valueOf((String) m_objectval).doubleValue();
          }
        catch (NumberFormatException nfe)
          {
            /* FALL THROUGH */
          }
      default:
        throw typeError("numericValue", "Not a number");
      }
  }

  /**
   * Returns the contents of this value, as an int
   * @exception ReteException If this value does not contain any kind of number
   * @return The number as an int
   */

  public int intValue(Context c) throws ReteException 
  {
    switch (m_type)
      {
      case RU.FLOAT:
        return (int) m_floatval;
      case RU.FACT_ID:
      case RU.INTEGER:
        return  m_intval;
      case RU.STRING:
      case RU.ATOM:
        try
          {
            return Integer.parseInt((String) m_objectval);
          }
        catch (NumberFormatException nfe)
          {
            /* FALL THROUGH */
          }
      default:
        throw typeError("intValue", "Not an integer");
      }
  }

  /**
   * Returns the contents of this value, as a long
   * @exception ReteException If this value does not contain any kind of number
   * @return The number as a long
   */

  public long longValue(Context c) throws ReteException 
  {
    return (long) numericValue(c);
  }


  /**
   * Returns the contents of this value, as a number
   * @exception ReteException If this value does not contain any kind of number
   * @return The number as a double
   */

  public double floatValue(Context c) throws ReteException 
  {
    return numericValue(c);
  }

  /**
   * Returns the contents of this value, as an atom
   * @exception ReteException If this value does not contain any kind of String
   * @return The atom
   */

  public String atomValue(Context c) throws ReteException
  {
    return stringValue(c);
  }

  /**
   * Returns the contents of this value, as a String (a variable name)
   * @exception ReteException If this value does not contain a variable
   * @return The name of the variable
   */

  public String variableValue(Context c) throws ReteException
  {
    if (m_type != RU.VARIABLE && m_type != RU.MULTIVARIABLE)
      throw typeError("variableValue", "Not a variable");
    return stringValue(c);
  }

  /**
   * Returns the contents of this value, as a String 
   * @exception ReteException If this value does not contain any kind of String
   * @return The string
   */

  public String stringValue(Context c) throws ReteException 
  {
    switch (m_type)
      {
      case RU.ATOM:
      case RU.STRING:
      case RU.VARIABLE:
      case RU.MULTIVARIABLE:
      case RU.SLOT:
      case RU.MULTISLOT:
        return (String) m_objectval;
      case RU.INTEGER:
        return String.valueOf(m_intval);
      case RU.FLOAT:
        return String.valueOf(m_floatval);
      case RU.EXTERNAL_ADDRESS:
        return m_objectval.toString();
      default:
        throw typeError("stringValue", "Not a string");
      }
  }

  /**
   * Returns the contents of this value, as a fact-id (an int)
   * @exception ReteException If this value does not contain a fact-id
   * @return The fact-id as an int
   */

  public int factIDValue(Context c) throws ReteException 
  {
    if ((m_type & FACT_TYPES) != 0)
      return m_intval;
    throw typeError("factIDValue", "Not a Fact-ID");
  }
  
  private ReteException typeError(String routine, String msg)
  {
    return typeError(routine, msg, m_type);
  }

  private ReteException typeError(String routine, String msg, int type)
  {
    return new ReteException("Value." + routine,
                             msg + ": \"" + toString() + "\"",
                             "(type = " + RU.getTypeName(type) + ")");
  }

  private String escape(String s)
  {
    if (s.indexOf('"') == -1)
      return s;
    else
      {
        StringBuffer sb = new StringBuffer();
        for (int i=0; i< s.length(); i++)
          {
            char c = s.charAt(i);
            if (c == '"' || c == '\\')
              sb.append('\\');
            sb.append(c);
          }
        return sb.toString();
      }
  }
  
  /**
   * Pretty-print this value, without adding parens to any lists
   * @return The formatted string
   */
  public String toString() 
  {
    switch (m_type) 
      {
      case RU.INTEGER:
        return String.valueOf(m_intval);
      case RU.FLOAT:
        return String.valueOf(m_floatval);
      case RU.STRING:
        return "\"" + escape((String) m_objectval) + "\"";
      case RU.ATOM:
      case RU.SLOT:
      case RU.MULTISLOT:
        return (String) m_objectval;
      case RU.VARIABLE:
        return "?" + m_objectval;
      case RU.MULTIVARIABLE:
        return "$?" + m_objectval;
      case RU.FACT_ID:
        return ("<Fact-" + m_intval + ">");
      case RU.FACT:
      case RU.FUNCALL:
      case RU.LIST:
        return m_objectval.toString();
      case RU.INTARRAY: 
        {
          int[] binding = (int[]) m_objectval;
          return "?" + binding[0] + "," + binding[1] + "," + binding[2];
        }
      case RU.EXTERNAL_ADDRESS:
        return "<External-Address:" + m_objectval.getClass().getName() + ">";
      case RU.NONE:
        return Funcall.NIL.toString();
      default:
        return "<UNKNOWN>";
      }
  }

  /**
   * Pretty-print this value, adding parens to any lists
   * @return The formatted string
   */

  public String toStringWithParens() 
  {
    switch (m_type) 
      {
      case RU.FACT:
      case RU.FUNCALL:
      case RU.LIST:
        return ((ValueVector) m_objectval).toStringWithParens();
      default:
        return toString();
      }
  }

  /**
   * Return the type of this variable. Always one of the constants in coc.agent.engine.RU.
   * @return The type
   */
  public int type()
  {
    return m_type;
  }

  /**
   * Compare this value to another object. As a convenience, if the
   * parameter is not a Value, it will be compared to any contained
   * Object inside this Vector (a String or external address object.)
   *
   * @param v The object to compare to. 
   * @return True if the objects are equivalent. 
   */
  public boolean equals(Object v) 
  {
    if (v instanceof Value)
      return equals((Value) v);
    else
      return v.equals(m_objectval);
  }

  /**
   * Compare this value to another value. Believe it or not, using this
   * separate overloaded routine has a measurable impact on performance -
   * since so much time is spent comparing Values.   
   *
   * @param v The Value to compare to.
   * @return True if the Values are equivalent.
   */

  public boolean equals(Value v) 
  {

    if (this == v)
      return true;

    if (v.m_type != m_type)
      return false;

    switch (m_type)
      {
      case RU.INTEGER:
      case RU.FACT_ID:
        return (m_intval == v.m_intval);
        
      case RU.FLOAT:
        return (m_floatval == v.m_floatval);
        
      default:
        return m_objectval.equals(v.m_objectval);
      }
  }
  
  /**
   * Like equals(Value) above, but returns true for 3 == 3.0
   * @param v Value to compare to
   * @return True if the values are loosely equivalent
   */
  
  public boolean equalsStar(Value v)
  {
    if (this == v)
      return true;

    try
      {
        if ((m_type & NUM_TYPES) != 0 && (v.m_type & NUM_TYPES) != 0)
          {
            return (numericValue(null) == v.numericValue(null));
          }
        
        else
          return equals(v);
      }
    catch (ReteException e) { /* can't happen */ return false; }
  }
  
  /**
   * Return a hashcode for the object
   * @return The hashcode
   */

  public int hashCode()
  {
    switch (m_type)
      {
      case RU.NONE:
        return 0;

      case RU.INTEGER: case RU.FACT_ID:
        return m_intval;

      case RU.FLOAT:
        return (int) m_floatval;

      case RU.FACT:
      case RU.FUNCALL:
      case RU.LIST:
        {
          try
            {
              ValueVector vv = (ValueVector) m_objectval;
              int retval = 0;
              for (int i=0; i<vv.size(); i++)
                retval += vv.get(i).hashCode();
              return retval;
            }
          catch (ReteException re) { /* can't happen */ return 0; }
        }
        
      default:
        return (m_objectval != null) ? m_objectval.hashCode() : 0;
      }
  }
  
  /**
   * Given an evaluation context, return the "true value" of this Value.
   * For this class, the true value is always "this". For subclasses, the Context
   * may be used to compute a new Value.
   * @see coc.agent.engine.Variable
   * @see coc.agent.engine.Funcall
   * @param c An execution context. You can pass null if you are sure that you're not
   * calling this method on a subclass that uses the argument.
   * @return This object
   */
  public Value resolveValue(Context c) throws ReteException
  {
    return this;
  }
  
  /**
   * CyberObject --- customized - compare without type
   * @param v
   * @return
   */
  public boolean equalsWithoutType(Value v) 
  {
    if (this == v)
      return true;

    switch (m_type)
      {
      case RU.INTEGER:
      case RU.FACT_ID:
        return (m_intval == v.m_intval);
        
      case RU.FLOAT:
        return (m_floatval == v.m_floatval);
        
      default:
        return m_objectval.equals(v.m_objectval);
      }
  }
}


