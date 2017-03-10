package coc.agent.engine;
import java.io.*;

/**
 * Binding binds variables to values; internally, also binds
 * variables to slots in facts. Since it's just a bag of properties with no behaviour,
 * member variables are public.
 * <P>
 *
 */

public class Binding implements Cloneable, Serializable
{
  /** The name of the variable. */
  public String m_name;

  /** The fact within a token where this variable should be loaded from. */
  public int m_factIndex;
  /** The slot within a fact where this variable should be loaded from. */
  public int m_slotIndex;
  /** The subslot within a slot where this variable should be loaded from. */
  public int m_subIndex;
  /** The type of the variable */
  public int m_type;  
  /** The value of the variable */
  public Value m_val;
  
  /**
   * Create a binding, setting initial values. This constructor is frequently used by
   * Jess internally, but onlt Accelerator implementors will use it outside the Jess
   * package.
   * @param name The name of the variable.
   * @param factIndex Which fact in a token it is matched to.
   * @param slotIndex Which slot in the fact.
   * @param subIndex Which subslot in a multislot or -1
   * @param type One of the type constants in coc.agent.engine.RU.
   */

  public Binding(String name, int factIndex, int slotIndex, int subIndex, int type) 
  {
    m_name = name;
    m_factIndex = factIndex;
    m_slotIndex = slotIndex;
    m_subIndex = subIndex;
    m_type = type;
    m_val = null;    
  }

  /**
   * Create a binding, bound to a Value.
   * @param name The name of the variable.
   * @param val The value of the variable.
   */

  public Binding(String name, Value val) throws ReteException
  {
    m_name = name;
    m_factIndex = RU.LOCAL;
    m_slotIndex = RU.LOCAL;
    m_subIndex = -1;
    m_val = val;
    m_type = val.type();
  }

  /**
   * Make a copy of the variable
   * @return The copy.
   */

  public Object clone() 
  {
    Binding b = new Binding(m_name, m_factIndex, m_slotIndex, m_subIndex, m_type);
    b.m_val = m_val;
    return b;
  }

  /**
   * Produce a string representation of this object for debugging.
   * @return The string representation.
   */

  public String toString()
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("[Binding: ");
    sb.append(m_name);
    sb.append(";factIndex=" + m_factIndex);
    sb.append(";slotIndex=" + m_slotIndex);
    sb.append(";subIndex=" + m_subIndex);
    sb.append(";type=" + m_type);
    sb.append(";val=" + m_val);
    sb.append("]");
    return sb.toString();
  }

}
