package coc.agent.engine;

import java.util.*;
import java.io.*;

/** **********************************************************************
 * Class used to parse, print and represent deftemplates.
 * <P>
 ********************************************************************** */
public class Deftemplate implements Serializable
{  
  private boolean m_backchain;
  private String m_name;
  private String m_docstring = "";
  private Deftemplate m_parent;
  private ValueVector m_data = new ValueVector();

  /**
   * return the parent of this deftemplate. The parent is another deftemplate this one
   * extends, or null.
   * @return The parent deftemplate. 
   */
  public Deftemplate getParent() { return m_parent; }

  /**
   * Sever the link with this deftemplate's parent. useful when creating similar, but
   * unrelated deftemplates.
   */
  public void forgetParent() { m_parent = null; }

  // Type objects for 'type' qualifiers in Deftemplates.

  private static Hashtable s_types = new Hashtable();
  private static String [] s_typenames = { "ANY",
                                           "INTEGER", "FLOAT", "NUMBER",
                                           "ATOM", "STRING", "LEXEME",
                                           "OBJECT"};
  private static int[] s_typevals = { -1,
                                      RU.INTEGER, RU.FLOAT, RU.INTEGER | RU.FLOAT,
                                      RU.ATOM, RU.STRING, RU.ATOM | RU.STRING,
                                      RU.EXTERNAL_ADDRESS};
  static
  {
    try
      {
        for (int i = 0; i < s_typenames.length; i++)
          s_types.put(s_typenames[i], new Value(s_typevals[i], RU.INTEGER));
      }
    catch (ReteException re) { /* can't happen */ }
  }


  /**
   * Get the name of this deftemplate
   * @return The name of this deftemplate
   */
  public final String getName() { return m_name; }

  /**
   * Get the docstring of this deftemplate
   * @return The docstring
   */
  public final String getDocstring() { return m_docstring; }

  /**
   * Make this deftemplate backwards-chaining reactive.
   */
  public final void doBackwardChaining() { m_backchain = true; }

  /**
   * Get the backchaining reactivity of this deftemplate.
   * @return True if this deftemplate can stimulate backwards chaining.
   */
  public final boolean getBackwardChaining() { return m_backchain; }

  /**
   * Constructor.
   * @param name The deftemplate name
   * @param docstring The deftemplate's documentation string
   */
  public Deftemplate(String name, String docstring)
  {
    m_name = name;
    m_docstring = docstring;
    if (!name.equals(RU.PARENT_DEFTEMPLATE))
      m_parent = Rete.getParentTemplate();
  }
  
  /**
   * Constructor.
   * Create a deftemplate 'derived from' another one
   * @param name The deftemplate name
   * @param docstring The deftemplate's documentation string
   * @param dt The 'parent' of this deftemplate
   */

  public Deftemplate(String name, String docstring, Deftemplate dt) throws ReteException
  {    
    m_name = name;
    m_parent = dt;
    m_docstring = docstring;

    for (int i=0; i<dt.m_data.size(); i++)
      m_data.add(dt.m_data.get(i));
    
  }

  /**
   * Create a new slot in this deftemplate. If the slot already exists, just
   * change the default value. 
   * @param name Name of the slot
   * @param value default value for the slot
   * @param typename Type of the slot: INTEGER, FLOAT, ANY, etc.
   * @exception ReteException If something goes wrong
   */
  public void addSlot(String name, Value value, String typename) throws ReteException 
  {
    int idx = 0;
    Value type = (Value) s_types.get(typename.toUpperCase());
    if (type == null)
      throw new ReteException("Deftemplate.addSlot", "Bad slot type:", typename);

    // Just set default if duplicate
    if ((idx = getSlotIndex(name)) != -1)
      {
        m_data.set(value, (idx * RU.DT_SLOT_SIZE) + RU.DT_DFLT_DATA);
        m_data.set(type, (idx * RU.DT_SLOT_SIZE) + RU.DT_DATA_TYPE);
        return;
      }
    else
      {
        int start = m_data.size();
        m_data.setLength(start + RU.DT_SLOT_SIZE);
        m_data.set(new Value(name, RU.SLOT), start + RU.DT_SLOT_NAME);
        m_data.set(value, start + RU.DT_DFLT_DATA);
        m_data.set(type, start + RU.DT_DATA_TYPE);
      }

  }

  /**
   * Create a new multislot in this deftemplate. If the slot already exists, just
   * change the default value. Public so reflectfunctions can use.
   * @param name Name of the slot
   * @param value default value for the slot
   * @exception ReteException If something goes wrong
   */
  public void addMultiSlot(String name, Value value) throws ReteException 
  {
    int idx = 0;

    // Just set default if duplicate
    if ((idx = getSlotIndex(name)) != -1)
      {
        m_data.set(value, absoluteIndex(idx) + RU.DT_DFLT_DATA);
        return;
      }
    int start = m_data.size();
    m_data.setLength(start + RU.DT_SLOT_SIZE);
    m_data.set(new Value(name, RU.MULTISLOT), start + RU.DT_SLOT_NAME);
    m_data.set(value, start + RU.DT_DFLT_DATA);
    m_data.set((Value) s_types.get(s_typenames[0]), start + RU.DT_DATA_TYPE);
  }
  
  private int absoluteIndex(int index)
  {
    return index * RU.DT_SLOT_SIZE;
  }

  /**
   * Returns the slot data type (one of the constants in coc.agent.engine.RU) for the slot
   * given by the zero-based index.
   * @param index The zero-based index of the slot (0, 1, 2 ... getNSlots()-1)
   * @return The data type of that slot (RU.INTEGER, RU.ATOM, etc., or RU.NONE)
   * 
   */ 

  public int getSlotDataType(int index)
       throws ReteException 
  {
    return m_data.get(absoluteIndex(index) + RU.DT_DATA_TYPE).intValue(null);
  }

  /**
   * Returns the default value of a slot given by the zero-based index.
   * @param index The zero-based index of the slot (0, 1, 2 ... getNSlots()-1)
   * @return The default value for that slot (can be Funcall.NIL or Funcall.NILLIST for none
   */ 

  public Value getSlotDefault(int index)
       throws ReteException 
  {
    return m_data.get(absoluteIndex(index) + RU.DT_DFLT_DATA);
  }

  /**
   * Returns the slot type (RU.SLOT or RU.MULTISLOT) of the slot in this deftemplate
   * given by the zero-based index.
   * @param index The zero-based index of the slot (0, 1, 2 ... getNSlots()-1)
   * @return The type of that slot (RU.SLOT or RU.MULTISLOT)
   * 
   */ 

  public int getSlotType(int index)
       throws ReteException 
  {
    return m_data.get(absoluteIndex(index) + RU.DT_SLOT_NAME).type();
  }

  /**
   * Return the index (0, 1, 2 ... getNSlots()-1) of the named slot,
   * or -1 if there is no such slot
   * @param slotname The name of the slot
   * @return The zero-based index of the slot
   */
  public int getSlotIndex(String slotname) throws ReteException 
  {

    int n = getNSlots();
    for (int i = 0; i<n; i++)
      if (m_data.get(absoluteIndex(i) + RU.DT_SLOT_NAME).equals(slotname))
        return i;
    
    return -1;
  }

  /**
   * Return the name of a given slot in this deftemplate
   * @param index The zero-based index of the slot (0, 1, 2 ... getNSlots()-1)
   * @return The name of that slot 
   * @exception ReteException If something is horribly wrong
   */
  public String getSlotName(int index) throws ReteException 
  {
    return m_data.get(absoluteIndex(index) + RU.DT_SLOT_NAME).stringValue(null);
  }

  /**
   * Return the number of slots in this deftemplate
   * @return The number of slots in this deftemplate
   */
  public int getNSlots()
  {
    return m_data.size() / RU.DT_SLOT_SIZE;
  }
  
  /**
   * Turn this deftemplate into a String
   * @return a string representation of the Deftemplate
   */
  public String toString() 
  {   
    List l = new List("deftemplate", m_name);
    if (m_parent != null)
      {
        l.add("extends");
        l.add(m_parent.getName());
      }
    
    l.addQuoted(m_docstring);
    for (int i=0; i<m_data.size(); i+=RU.DT_SLOT_SIZE)
      {
        try
          {
            Value val = m_data.get(i + RU.DT_SLOT_NAME);
            List slot = new List(val.type() == RU.SLOT ? "slot" : "multislot", val);
            val = m_data.get(i + RU.DT_DFLT_DATA);        
            if (!val.equals(Funcall.NIL) && !val.equals(Funcall.NILLIST))
              slot.add(new List("default", val));
            val = m_data.get(i + RU.DT_DATA_TYPE);        
            if (val.intValue(null) != -1)
              slot.add(new List("type", val));
            l.newLine();                  
            l.add(slot);
          }
        catch (ReteException re)
          {
            l.add(re.toString());
            break;
          }
      }
    return l.toString(); 
  }
}

