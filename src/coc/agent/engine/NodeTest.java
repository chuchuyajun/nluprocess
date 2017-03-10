package coc.agent.engine;

import java.util.*;
import java.io.*;

/** **********************************************************************
 * Node containing an arbitrary list of tests; used for TEST CE's and also
 * the base class for join nodes.
 ********************************************************************** */

class NodeTest extends Node implements Serializable
{
  /*
   * Must be package-visible
   */
     
  static Accelerator m_accelerator;
  
  /**
    The tests this node performs
    */

  Vector m_tests;
  Test[] m_localTests;

  /*
   * Execution context for functions. Because this is a member, only one thread
   * can be in callNode at a time!
   */

  Context m_context;

  Rete m_engine;

  /**
   * Constructor
   */
  NodeTest(Rete engine) 
  {
    m_engine = engine;
    m_context = new Context(engine.getGlobalContext());
    m_tests = new Vector();
  }

  void complete() throws ReteException
  {
    // freeze the tests vector
    m_localTests = new Test[m_tests.size()];
    for (int i=0; i< m_tests.size(); i++)
      m_localTests[i] = (Test) m_tests.elementAt(i); 
  }

  /**
   * Bare NodeTests can only have Test1's in them.
   */
  void addTest(int test, int slot_idx, Value v) 
       throws ReteException
  {
    addTest(test, slot_idx, -1, v); 
  }

  void addTest(int test, int slot_idx, int slot_sidx, Value v) 
       throws ReteException
  {
    Test t = null;

    Funcall f = v.funcallValue(null);
    
    loadAccelerator();
    
    // if we have an accelerator, try to apply it
    if (m_accelerator != null)
      t = m_accelerator.speedup(f);
    
    // if no acceleration, use the standard Test1 class
    if (t == null)
      t = new Test1(test, slot_sidx, v);
    
    m_tests.addElement(t);    
  }

  /**
   * The classic 'you should never inherit from concrete classes' problem!
   */
  
  void addTest(int test, int token_idx, int left_idx, int right_idx) 
       throws ReteException
  {
    throw new ReteException("NodeTest:addtest",
                            "Can't add Test2s to this class", "");

  }

  void addTest(int test, int token_idx, int left_idx, int leftSub_idx,
               int right_idx, int rightSub_idx)
       throws ReteException
  {
    throw new ReteException("NodeTest:addtest",
                            "Can't add Test2s to this class", "");
  }

  /**
   * For our purposes, two Node2's are equal if every test in
   * one has an equivalent test in the other, and if the test vectors are
   * the same size. The subclass NodeNot2 should never be shared, so we'll
   * report unequal always. This routine is used during network compilation,
   * not at runtime.
   */

  public boolean equals(Object o) 
  {
    if (this == o)
      return true;
    
    if (this.getClass() != o.getClass())
      return false;

    NodeTest n = (NodeTest) o;
      
    if (n instanceof NodeNot2 ||
        n.m_tests.size() != m_tests.size())
      return false;

  outer_loop:
    for (int i=0; i<m_tests.size(); i++) 
      {
        Object t1 = m_tests.elementAt(i);
        for (int j=0; j<m_tests.size(); j++) {
          if (t1.equals(n.m_tests.elementAt(j)))
            continue outer_loop;
        }
        return false;
      }
    return true;
  }

  boolean callNodeRight(Token t) throws ReteException
  {    
    if (t.m_tag == RU.CLEAR)
      return false;
    else
      return super.callNodeRight(t);
  }

  boolean callNodeLeft(Token token) throws ReteException
  {
    broadcastEvent(JessEvent.RETE_TOKEN + LEFT, token); 
    if (token.m_tag == RU.CLEAR)
      {
        passAlong(token);
        return true;
      }
    
    int ntests = m_localTests.length;

    boolean result = (ntests == 0) ? true : runTests(token, ntests);

    if (result)
      {
        token = Rete.getFactory().newToken(token.prepare(), m_engine.getNullFact());
        token.updateTime(m_engine);
        passAlong(token);
      }
    return result;
  }

  boolean runTests(Token token, int ntests) throws ReteException
  {
    try
      {
        m_context.setToken(token);
        
        for (int i=0; i < ntests; i++) 
          {
            if (!m_localTests[i].doTest(m_context))
              return false;
          }        
        return true;
      }
    catch (ReteException re)
      {
        re.addContext("'test' CE");
        throw re;
      }
  }

  void loadAccelerator() throws ReteException
  {
    // Try to load accelerator, if needed
    if (m_accelerator == null)
      {
        String classname;
        if ((classname = RU.getProperty("SPEEDUP")) != null)
          {
            try
              {
                m_accelerator = (Accelerator) Class.forName(classname).newInstance();
              }
            catch (Exception e)
              {
                throw new ReteException("NodeTest.addTest",
                                        "Can't load Accelerator class " + classname,
                                        e.getMessage());
              }
          }
      }
  }

  /**
    Two-input nodes always make calls to the left input of other nodes.
  */

  void passAlong(Token t) throws ReteException
  { 
    t = t.prepare();
    Node [] sa = m_localSucc;
    for (int j=0; j<m_nsucc; j++) 
      {
        Node s = sa[j];
        s.callNodeLeft(t);
      }
  }

  /**
   * Describe myself
   * @return 
   */
  public String toString() 
  {
    StringBuffer sb = new StringBuffer(256);
    sb.append("[NodeTest ntests=");
    sb.append(m_tests.size());
    sb.append(" ");
    for (int i=0; i<m_tests.size(); i++)
      {
        sb.append(m_tests.elementAt(i).toString());
        sb.append(" ");
      }
    sb.append(";usecount = ");
    sb.append(m_usecount);
    sb.append("]");
    return sb.toString();
  }

}





