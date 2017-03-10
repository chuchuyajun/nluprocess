
package coc.agent.engine;
import java.io.*;

/** **********************************************************************
 *  A tiny class to hold an individual test for a 2-input node to perform
 * <P>
 */
class Test2Simple implements Test, Serializable
{

  /**
    What test to do (true is EQ, false is NEQ)
   */

  private boolean m_test;

  boolean getTest() { return m_test; }

  /**
    Which fact within a token (0,1,2...)
   */

  private int m_tokenIdx;
  int getTokenIndex() { return m_tokenIdx; }
  /**
    Which field (absolute index of slot start) from the left memory
   */

  private int m_leftIdx;
  int getLeftIndex() { return m_leftIdx; }

  /**
    Which field (absolute index of slot start) from the right memory
   */

  private int m_rightIdx;
  int getRightIndex() { return m_rightIdx; }

  /**
   * Constructors
   * @param test 
   * @param tokenIdx 
   * @param leftIdx 
   * @param rightIdx 
   */
  Test2Simple(int test, int tokenIdx, int leftIdx, int rightIdx)
  {
    m_test = (test == EQ);
    m_tokenIdx = tokenIdx;
    m_rightIdx = rightIdx;
    m_leftIdx = leftIdx;
  }

  /**
   * @param tt 
   * @return 
   */
  public boolean equals(Object tt)
  {
    if (! (tt instanceof Test2Simple))
      return false;

    Test2Simple t = (Test2Simple) tt;
    return  (m_test == t.m_test &&
             m_tokenIdx == t.m_tokenIdx &&
             m_rightIdx == t.m_rightIdx &&
             m_leftIdx == t.m_leftIdx);
  }

  /**
   * @param lt 
   * @param rf 
   * @param c 
   * @param engine 
   * @exception ReteException 
   * @return 
   */
  public boolean doTest(Context c)
       throws ReteException
  {
    Token lt = c.m_token;
    ValueVector rf = c.m_fact;

    return (m_test == lt.fact(m_tokenIdx).get(m_leftIdx).equals(rf.get(m_rightIdx)));
  }

  /**
   * @return 
   */
  public String toString()
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("[Test2Simple: test=");
    sb.append(m_test ? "EQ" : "NEQ");
    sb.append(";tokenIdx=");
    sb.append(m_tokenIdx);
    sb.append(";leftIdx=");
    sb.append(m_leftIdx);
    sb.append(";rightIdx=");
    sb.append(m_rightIdx);
    sb.append("]");

    return sb.toString();
  }

}


