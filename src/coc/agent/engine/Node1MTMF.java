package coc.agent.engine;

/** **********************************************************************
 * Not a test at all. Split a fact into lots of possible alternate facts,
 * each representing a possible multifield mapping within a multislot.
 *
 ********************************************************************** */
class Node1MTMF extends Node1 
{
  private int m_idx, m_subidx;
  Node1MTMF(int idx, int subidx)
  {
    m_idx = idx;
    m_subidx = subidx;
  }
  
  boolean callNodeRight(Token t) throws ReteException
  {
    if (super.callNodeRight(t))
      return false;

    
    int i,j;
    int slot_idx = m_idx;
    int sub_idx = m_subidx;

    Fact fact = t.topFact();
    int tag = t.m_tag;
    if (fact.get(slot_idx).equals(Funcall.NIL))
    {
      passAlong(t);
      return  false;
    }
        
    ValueVector mslot = fact.get(slot_idx).listValue(null);
    int slot_size = mslot.size();

    int new_slot_size, multi_size;
    
    for (i=0; i < (slot_size - sub_idx + 1); i++) 
      {
        new_slot_size = sub_idx + 1 + i;
        multi_size = slot_size - new_slot_size + 1;
      
        ValueVector new_slot = new ValueVector(new_slot_size);
        // Start of new and old are the same
        for (j=0; j < sub_idx; j++)
          new_slot.add( mslot.get(j));
      
        // Middle of old moved into special multifield in new
        ValueVector multi = new ValueVector(multi_size);
        new_slot.add( new Value(multi, RU.LIST));
      
        for (j=sub_idx; j < multi_size + sub_idx; j++) 
          multi.add(mslot.get(j));
      
        // End of new and old are the same
        for (j=sub_idx + multi_size; j < new_slot_size - 1 + multi_size; j++)
          new_slot.add( mslot.get(j));
      
        Fact new_fact = (Fact) fact.clone();
        new_fact.set(new Value(new_slot, RU.LIST), slot_idx);
      
        // Handy for debugging
        // System.out.println(new_fact);
      
        // Pass this fact on to all successors.
        passAlong(Rete.getFactory().newToken(new_fact, tag));

      }
    return true;
  }
  
  public String toString()
  {
    return "[Split the multislot at index " + m_idx + ", subindex " + m_subidx + "]";
  }

  public boolean equals(Object o)
  {
    if (o instanceof Node1MTMF)
      {
        Node1MTMF n = (Node1MTMF) o;
        return (m_idx == n.m_idx && m_subidx == n.m_subidx);            
      }
    else
      return false;
  }
}

