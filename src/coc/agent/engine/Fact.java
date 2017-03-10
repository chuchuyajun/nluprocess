package coc.agent.engine;

import java.util.*;
import java.io.*;

/** **********************************************************************
 * A Fact is a ValueVector where the entries are the slot data in
 * declaration order. The "head" of the fact, id, etc., are -not- stored in
 * the vector. 
 * <P>
 ********************************************************************** */

public class Fact extends ValueVector implements Serializable
{
  private String m_name;
  public String getName() { return m_name; }
  
  private int m_id = -1;
  /**
   * Returns this Fact's fact-id.
   * @return The fact-id
   */
  public int getFactId() { return m_id; }
  void setFactId(int i) { m_id = i; }
  
  private boolean m_shadow;
  void setShadow(boolean isShadow) { m_shadow = isShadow; }
  /**
   * Indicates whether this Fact is a shadow fact for a matched Bean.
   * @return True is this is a shadow fact
   */
  public boolean isShadow() { return m_shadow; }

  /**
   * Return the deftemplate for this fact.
   * @return The deftemplate for this fact
   */

  public final Deftemplate getDeftemplate()  { return m_deft; }
  private Deftemplate m_deft;

  int getTime() { return m_time; }
  void updateTime(Rete engine) { m_time = engine.getTime(); }
  private int m_time;

  public Value get(int i) throws ReteException
  {
    if (i == -1)
      return new Value(m_id, RU.FACT_ID);
    else
      return super.get(i);
  }


  /**
   * Basic constructor. If name is not a known deftemplate, an implied ordered
   * deftemplate is created. If it is a known unordered deftemplate, default values
   * are copied from the deftemplate.
   * @param name The head or name of the fact
   * @param engine The engine in which to find the deftemplate
   * @exception ReteException If anything goes wrong
   */

  public Fact(String name, Rete engine) throws ReteException 
  {
    if (name.equals("not") || name.equals("test") ||
        name.equals("unique") || name.equals("explicit"))
      throw new ReteException("Fact.Fact",
                              "Illegal fact name:", name);
                

    m_deft = engine.createDeftemplate(name);
    createNewFact();
    m_time = engine.getTime();
  }
  
  /**
   * Starts from another Fact. No default values are
   * filled in; the ValueVector is assumed to already be complete.
   * @param f The ValueVector form of a fact
   * @param engine The engine in which to find the deftemplate
   * @exception ReteException If anything goes wrong.
   */
  public Fact(Fact f) throws ReteException
  {
    m_name = f.m_name;
    m_deft = f.m_deft;
    setLength(f.size());
    for (int i=0; i<size(); i++)
      set(f.get(i), i);
    m_time = f.m_time;
    m_id = f.m_id; 
  }
  

  /**
   * Make a copy of this fact
   * @return The copy
   */
  public Object clone()
  {
    try
      {
        return new Fact(this);
      }
    catch (ReteException re) { /* can't happen */ return null; }
  }

  private void createNewFact() throws ReteException
  {
    int size = m_deft.getNSlots();
    setLength(size);
    m_name = m_deft.getName();
    m_shadow = false;
    
    for (int i=0; i<size; i++)
      set(m_deft.getSlotDefault(i), i);
  }

  private final int findSlot(String slotname) throws ReteException 
  {
    int index = m_deft.getSlotIndex(slotname);
    if (index == -1)
      throw new ReteException("Fact.findSlot",
                              "Attempt to access invalid slotname",
                              slotname);
    return index;
  }

  /**
   * Return the value from the named slot.
   * @param slotname The name of a slot in this fact
   * @exception ReteException If anything goes wrong
   * @return The value
   */
  final public Value getSlotValue(String slotname) throws ReteException
  {
    return get(findSlot(slotname));
  }

  /**
   * Set the value in the named slot.
   * @param slotname The name of the slot
   * @param value The new value for the slot
   * @exception ReteException If anything goes wrong
   */
  final public void setSlotValue(String slotname, Value value) throws ReteException 
  {
    set(value,findSlot(slotname));
  }
  
  
  List toList()
  {
    try 
      {
        List l = new List(m_name);
        
        int nslots = size();
        // Make "Ordered" facts look ordered
        if (nslots == 1 &&
            m_deft.getSlotName(0).equals(RU.DEFAULT_SLOT_NAME))
          {
            if (get(0).type() != RU.LIST)
              {
                l.add(get(0));
                return l;
              }
            else if (get(0).listValue(null).size() == 0)
              return l;
            else
              {
                // Omit slot name and parens
                l.add(get(0));
                return l;
              }
          }
          
        for (int i=0; i< nslots; i++) 
          {
            l.add(new List(m_deft.getSlotName(i), get(i)));
          }
        return l;
      }
    catch (ReteException re) 
      {
        return new List(re.toString());
      }    
  }
  /**
     * Pretty-print this fact into a String. Should always be a parseable fact, except when
   * a slot holds an external-address value.
   * @return The pretty-printed String.
   */
    
  public String toString() 
  {
    return toList().toString();
  }

  public String toStringWithParens() 
  {
    return toList().toString();
  }

  /**
   * The version in ValueVector isn't good enough, since it doesn't compare heads!
   */

  public boolean equals(Object o)
  {
    if (! (o instanceof Fact))
      return false;

    Fact f = (Fact) o;
    if (!m_name.equals(f.m_name))
      return false;

    return super.equals(o);

  }

}









