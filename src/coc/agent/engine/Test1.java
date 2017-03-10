package coc.agent.engine;
import java.io.*;

/** **********************************************************************
 * Holds a single test in a Pattern on the LHS of a Rule.
 * <P>
 */
public class Test1 implements Test, Serializable
{
  /**
    What test to do (Test1.EQ, Test1.NEQ, etc)
   */

  int m_test;

  /**
    Which subslot within a multislot (0,1,2...)
   */

  int m_subIdx;

  /**
    The datum to test against
    */

  Value m_slotValue;

  /**
     AND or OR
  */
  int m_conjunction = RU.AND;

  /**
   * Create a single test.
   * @param test Test.EQ or Test.NEQ
   * @param sub_idx The subfield of a multislot, or -1
   * @param slot_value An object test against
   * @param conjunction RU.AND or RU.OR
   * @exception ReteException If something goes wrong
   */
  public Test1(int test, int sub_idx, Value slot_value, int conjunction) 
       throws ReteException
  {
    this(test, sub_idx, slot_value);
    m_conjunction = conjunction;
  }

  Test1(int test, int sub_idx, Value slot_value) 
       throws ReteException
  {
    m_test = test;
    m_subIdx = sub_idx;
    m_slotValue = slot_value;
  }

  Test1(Test1 t, Value slot_value)
       throws ReteException
  {
    m_test = t.m_test;
    m_subIdx = t.m_subIdx;
    m_conjunction = t.m_conjunction;
    m_slotValue = slot_value;
  }

  public int getTest() { return m_test; }
  public Value getValue() { return m_slotValue; }
  public int getMultislotIndex() { return m_subIdx;  }

  public boolean doTest(Context context) throws ReteException
  {
    boolean retval;
    
    retval = m_slotValue.resolveValue(context).equals(Funcall.FALSE);

    switch (m_test) 
      {
      case EQ:
        if (retval)
          return false;
        break;
        
      case NEQ:
        if (!retval)
          return false;
        break;
        
      }
    return true;
  }



  public boolean equals(Object o)
  {
    if (! (o instanceof Test1))
      return false;

    Test1 t = (Test1) o;
    if (m_test != t.m_test)
      return false;

    else if (m_subIdx != t.m_subIdx)
      return false;

    else if (m_conjunction != t.m_conjunction)
      return false;

    else return m_slotValue.equals(t.m_slotValue);
  }



  /**
   * @return 
   */
  public String toString()
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("[Test1: test=");
    sb.append(m_test == NEQ ? "NEQ" : "EQ");
    sb.append(";sub_idx=");
    sb.append(m_subIdx);
    sb.append(";slot_value=");
    sb.append(m_slotValue);
    sb.append(";conjunction=");
    sb.append(m_conjunction);
    sb.append("]");

    return sb.toString();
  } 


}



