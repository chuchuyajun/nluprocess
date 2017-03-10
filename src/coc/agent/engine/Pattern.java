package coc.agent.engine;

import java.util.*;
import java.io.*;

/** **********************************************************************
 * Pattern represents a single conditional elemebnt on a rule LHS.
 * A Pattern consists mainly of a two-dimensional array of Test1 structures.
 * Each Test1 contains information about a specific characteristic of a slot.
 *
 * <P>
 */

public class Pattern implements Serializable
{

  /**
    The deftemplate corresponding to this pattern
    */

  private Deftemplate m_deft;
  private static final int INITSIZE = 1;

  /**
   * @return The number of slots in this pattern's Deftemplate
   */
  public int size() { return m_deft.getNSlots(); }

  /**
    The Slot tests for this pattern
    */

  private Test1[][] m_tests;
  private int m_slotLengths[];
  
  /**
    Am I in a (not () ) ?
    */
  
  private int m_negated;

  /**
    Am I uniquely determined?
    */
  
  private boolean m_unique;

  /**
    Only match explicitly, no backwards chaining
    */
  
  private boolean m_explicit;

  /**
    Class of fact matched by this pattern
    */

  private String m_class;

  /**
    Bound to a variable if non-null
    */

  private String m_boundName;

  /**
   * Constructor.
   * @param name 
   * @param engine 
   * @param negcnt 
   * @exception ReteException 
   */
  public Pattern(String name, Rete engine, int negcnt)
       throws ReteException 
  {
    m_class = name;
    m_deft = engine.createDeftemplate(name);
    m_negated = negcnt;

    int nvalues = m_deft.getNSlots();
    m_tests = new Test1[nvalues][];
    m_slotLengths = new int[nvalues];
    for (int i=0; i<nvalues; i++)
      m_slotLengths[i] = -1;
  }

  // Creates a new Pattern which shares some data, but with a new name.
  // Used by backchaining stuff.

  private static final int VARIABLE_TYPES = RU.VARIABLE | RU.MULTIVARIABLE;

  /**
   * @param p 
   * @param name 
   * @param negcnt 
   * @exception ReteException 
   */
  Pattern(Pattern p, String name, int negcnt) throws ReteException
  {
    m_class = name;
    m_deft = p.m_deft;
    m_negated = negcnt;

    // We need to copy the tests and replace any blank variables with
    // new blanks (with new names.) 
    m_tests = new Test1[p.m_tests.length][];
    for (int i=0; i<m_tests.length; i++)
      {
        m_tests[i] = (p.m_tests[i] == null) ? null : new Test1[p.m_tests[i].length];
        if (m_tests[i] != null)
          {
            System.arraycopy(p.m_tests[i], 0, m_tests[i], 0, m_tests[i].length);
            for (int j=0; j<m_tests[i].length; j++)
              {
                Value v = m_tests[i][j].m_slotValue;
                if (v instanceof Variable &&
                    v.variableValue(null).startsWith(Tokenizer.BLANK_PREFIX))
                  m_tests[i][j] =
                    new Test1(m_tests[i][j], new Variable(RU.gensym(Tokenizer.BLANK_PREFIX),
                                                          v.type()));
              }
          }
      }

    m_slotLengths = p.m_slotLengths;
  }

  /**
   * set the length of a multislot within a pattern
   * @param slotname 
   * @param length 
   * @exception ReteException 
   */
  public void setMultislotLength(String slotname, int length) 
       throws ReteException 
  {
    int index = m_deft.getSlotIndex(slotname);
    if (index == -1)
      throw new ReteException("Pattern.setMultislotLength",
                              "Attempt to set length of invalid slotname",
                              slotname);

    m_slotLengths[index] = length;
  }

  /**
   * Add a value to this pattern
   * @param slotname 
   * @param aTest
   * @exception ReteException 
   */
  public void addTest(String slotname, Test1 aTest)
       throws ReteException 
  {
    
    // try to find this slotname in the deftemplate
    int idx = m_deft.getSlotIndex(slotname);
    if (idx == -1)
      throw new ReteException("Pattern.addTest",
                              "Attempt to add field with invalid slotname",
                              slotname);

    if (m_tests[idx] == null)
      m_tests[idx] = new Test1[INITSIZE];

    int j=0;
    while (j < m_tests[idx].length && m_tests[idx][j] != null)
      ++j;

    if (j == m_tests[idx].length)
      {
        Test1[] tmp = new Test1[j+1];
        System.arraycopy(m_tests[idx], 0, tmp, 0, j);
        m_tests[idx] = tmp;
      }
    
    // Tests must be added in subslot-order!    
    if (j > 0 && m_tests[idx][j-1].m_subIdx > aTest.m_subIdx)
      throw new ReteException("Pattern.addTest",
                              "Attempt to add out-of-order test: subindex ",
                              m_tests[idx][j-1].m_subIdx + " > " + aTest.m_subIdx);
    
    m_tests[idx][j] = aTest;

  }

  void replaceTests(int slotIndex, Test1[] theTests)
  {
    m_tests[slotIndex] = theTests;
  }


  /**
   * Is this pattern a (not()) CE pattern, possibly nested?
   * @return 
   */
  public int getNegated()
  {
    return m_negated;
  }

  void setUnique()
  {
    m_unique = true;
  }

  /**
   * @return 
   */
  public boolean getUnique()
  {
    return m_unique;
  }

  /**
   * @param exp 
   */
  void setExplicit(boolean exp)
  {
    m_explicit = exp;
  }

  /**
   * @return 
   */
  public boolean getExplicit()
  {
    return m_explicit;
  }

  /**
   * @return 
   */
  public String getName() 
  {
    return m_class;
  }

  /**
   * @param s 
   * @exception ReteException 
   */
  public void setBoundName(String s) throws ReteException
  {
    if ((m_negated != 0 || m_class.equals("test")) && s != null)
      throw new ReteException("Pattern.setBoundName",
                              "Can't bind negated pattern to variable", s);
    m_boundName = s;
  }

  /**
   * @return 
   */
  public String getBoundName() { return m_boundName; }

  /**
   * @return 
   */
  public int getNSlots()
  {
    return m_tests.length;
  }

  /**
   * @param slot 
   * @return 
   */
  public int getNTests(int slot)
  {
    if (m_tests[slot] == null)
      return 0;
    else
      return m_tests[slot].length;
  }

  /**
   * @param slot 
   * @return 
   */
  public int getSlotLength(int slot)
  {
    if (m_slotLengths != null)
      return m_slotLengths[slot];
    else
      return -1;
  }

  /**
   * @param slot 
   * @param test 
   * @return 
   */
  public Test1 getTest(int slot, int test)
  {
    return m_tests[slot][test];
  }

  /**
   * @return 
   */
  public Deftemplate getDeftemplate() { return m_deft; }

  /**
   * Describe myself
   * @return 
   */
  public String toString() 
  {
    List l = new List(m_class);
    
    try
      {
        // Make "Ordered" facts look ordered
        if (getNSlots() == 1 &&
            m_deft.getSlotName(0).equals(RU.DEFAULT_SLOT_NAME))
          {
            if (m_tests != null && m_tests[0] != null && m_tests[0].length  > 0)
                l.add(testsAsString(m_tests[0]));
          }
        else // Not "ordered"
          {
            for (int i=0; i < getNSlots(); i++)
              {
                if( getNTests(i) > 0 )
                  {
                    String data = testsAsString(m_tests[i]);
                    l.add(new List(m_deft.getSlotName(i), data));
                  }
              }
          }
      }
    catch (ReteException re)
      {
        /* Can't happen */
      }

    if (m_unique)
      l = new List("unique", l);

    if (m_explicit)
      l = new List("explicit", l);

    if (m_negated != 0)
      for (int i=0; i<m_negated; i++)
        l = new List("not", l);

    String retval = l.toString();

    if (m_boundName != null)
      retval = "?" + m_boundName + " <- " + retval;

    return retval;
  }

  /**
   * @param tests 
   * @exception ReteException 
   * @return 
   */
  private String testsAsString(Test1 [] tests) throws ReteException
  {
    StringBuffer sb = new StringBuffer();
    if (tests != null)
      {
        int lastSubIdx = -1;
        for (int i=0; i<tests.length; i++)
          {
            Test1 t = tests[i];
            if (i > 0)
              {
                if (t.m_subIdx == lastSubIdx)
                  sb.append('&');
                else
                  sb.append(' ');
              }
            lastSubIdx = t.m_subIdx;

            if (t.m_test == Test1.NEQ)
              sb.append('~');
            
            Value v = t.m_slotValue;
            switch (v.type())
              {
              case RU.FUNCALL:
                if (! m_class.equals("test"))
                    sb.append(':');
                // FALL THROUGH
              default:
                sb.append(v);
                break;
              }
          }
      }

    if (sb.length() == 0)
      {
        if (m_deft.getSlotType(0) == RU.MULTISLOT)
          sb.append(RU.gensym("$?__var"));
        else          
          sb.append(RU.gensym("?__var"));
      }

    return sb.toString();
  }
  
  
}
  

