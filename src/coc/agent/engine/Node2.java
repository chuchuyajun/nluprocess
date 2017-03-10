package coc.agent.engine;

import java.util.*;
import java.awt.*;
import java.io.*;

/**
 * A non-negated, two-input node of the Rete network.
 * Each tests in this node tests that a slot from a fact from the left input
 * and one from the right input have the same value and type.
 *
 * $Id: Node2.java,v 1.1 2012/10/02 21:06:06 Buildadmin Exp $
 */

class Node2 extends NodeTest implements Serializable
{
  
  /**
   * The left and right token memories
   * They are binary trees of Tokens
   */

  transient TokenTree m_left;
  transient TokenTree m_right;


  /**
   * Fact index used for tree indexing in right memory
   */

  private int m_rightIdx = -1;
  private int m_rightSubIdx = -1;
  private int m_leftIdx = -1;
  private int m_leftSubIdx = -1;
  private int m_tokenIdx = 0;

  /**
   * The key to use when creating token trees
   */
    
  private int m_hashkey;

  /**
   * True if the unique CE was applied
   */

  private boolean m_unique;

  /**
   * Only non-null when I should backwards-chain
   */
  Pattern m_pattern;

  /**
   * Only non-null when I should backwards-chain
   */

  Defrule m_defrule;

  /**
   * Used to count pattern-matches; if zero, may backwards-chain.
   */

  public int m_matches = 0;


  /**
   * True if we can do short-cut testing
   */
  boolean m_blessed = false;


  /**
   * Constructor
   * @param engine The Rete engine the node will be installed in
   * @param unique True if the 'unique' CE was applied
   * @param hashkey Hashkey to use for creating TokenTrees
   */

  Node2(Rete engine, boolean unique, int hashkey) 
  {
    super(engine);
    m_unique = unique;
    m_hashkey = hashkey;
    // System.out.println("m_hashkey = " + hashkey);
    
  }

  /**
   * Add a test to this node.
   * @param test EQ or NEQ
   * @param token_idx Which fact in the token
   * @param left_idx Which slot in the left fact
   * @param right_idx Which slot in the right fact
   */

  void addTest(int test, int token_idx, int left_idx, int right_idx) 
  { 
    addTest(test, token_idx, left_idx, -1, right_idx, -1);
  }

  /**
   * Add a test to this node.
   * @param test EQ or NEQ
   * @param token_idx Which fact in the token
   * @param left_idx Which slot in the left fact
   * @param leftSub_idx Which subslot in the left slot
   * @param right_idx Which slot in the right fact
   * @param rightSub_idx Which subslot in the right slot
   */
  void addTest(int test, int token_idx, int left_idx, int leftSub_idx,
               int right_idx, int rightSub_idx) {

    Test t;
    if (leftSub_idx == -1 && rightSub_idx == -1)
      m_tests.addElement(t = new Test2Simple(test, token_idx,
                                             left_idx, right_idx));
    else
      m_tests.addElement(t = new Test2Multi(test, token_idx, left_idx, 
                                            leftSub_idx, right_idx,
                                            rightSub_idx));
  }


  /**
   * Do the business of this node.
   * The 2-input nodes, on receiving a token, have to do several things,
   * and their actions change based on whether it's an ADD or REMOVE,
   * and whether it's the right or left input!
   * <PRE>
   * 
   * For ADDs, left input:
   * 1) Look for this token in the left memory. If it's there, do nothing;
   * If it's not, add it to the left memory.
   * 
   * 2) Perform all this node's tests on this token and each of the right-
   * memory tokens. For any right token for which they succeed:
   * 
   * 3) a) append the right token to a copy of this token. b) do a
   * CallNode on each of the successors using this new token.
   * 
   * For ADDs, right input:
   * 
   * 1) Look for this token in the right memory. If it's there, do nothing;
   * If it's not, add it to the right memory.
   * 
   * 2) Perform all this node's tests on this token and each of the left-
   * memory tokens. For any left token for which they succeed:
   * 
   * 3) a) append this  token to a copy of the left token. b) do a
   * CallNode on each of the successors using this new token.
   * 
   * For REMOVEs, left input:
   * 
   * 1) Look for this token in the left memory. If it's there, remove it;
   * else do nothing.
   * 
   * 2) Perform all this node's tests on this token and each of the right-
   * memory tokens. For any right token for which they succeed:
   * 
   * 3) a) append the right token to a copy of this token. b) do a
   * CallNode on each of the successors using this new token.
   * 
   * For REMOVEs, right input:
   * 
   * 1) Look for this token in the right memory. If it's there, remove it;
   * else do nothing.
   * 
   * 2) Perform all this node's tests on this token and each of the left-
   * memory tokens. For any left token for which they succeed:
   * 
   * 3) a) append this token to a copy of the left token. b) do a
   * CallNode on each of the successors using this new token.
   * 
   * </PRE>
   */

  boolean callNodeLeft(Token token) throws ReteException
  {  
    try
      {
        broadcastEvent(JessEvent.RETE_TOKEN + Node.LEFT, token); 
        
        m_matches = 0;
        
        switch (token.m_tag) 
          {
            
          case RU.ADD:
          case RU.UPDATE:

            m_left.add(token, token.m_tag == RU.UPDATE);
            runTestsVaryRight(token, m_right);
            askForBackChain(token);
            
            break;
            
          case RU.REMOVE:
            if (m_left.remove(token))                  
              {
                runTestsVaryRight(token, m_right);
              }
            break;
            
          case RU.CLEAR:
            // This is a special case. If we get a 'clear', we flush our memories,
            // then notify all our successors and return.            
            initTransientMembers();
            passAlong(token);
            return false;
            
          default:
            throw new ReteException("Node2.callNode",
                                    "Bad tag in token",
                                    String.valueOf(token.m_tag));
          } // switch token.tag
        
        return true;
      }
    catch (ReteException je)
      {
        je.addContext("rule LHS (Node2)");
        throw je;        
      }
  }

  boolean callNodeRight(Token t) throws ReteException
  {
    try
      {        
        int tag = t.m_tag;
        broadcastEvent(JessEvent.RETE_TOKEN + Node.RIGHT, t); 
        
        switch (tag) 
          {
            
          case RU.UPDATE:
          case RU.ADD:
            m_right.add(t, tag == RU.UPDATE);
            runTestsVaryLeft(t, m_left);
            break;
            
          case RU.REMOVE:
            if (m_right.remove(t))
              {
                runTestsVaryLeft(t, m_left);
              }
            break;
            
          case RU.CLEAR:
            return false;

          default:
            throw new ReteException("Node2.callNode",
                                    "Bad tag in token",
                                    String.valueOf(tag));
          } // switch tag
        
        return true;
      }
    catch (ReteException je)
      {
        je.addContext("rule LHS (Node2)");
        throw je;        
      }
  }

  /**
   * Node2.callNode can call this to produce debug info.
   * @param token 
   * @param callType 
   * @exception ReteException 
   */

  void debugPrint(Token token, int callType) throws ReteException 
  {
    System.out.println("TEST " + toString() + "(" + hashCode() + ");calltype=" + callType
                       + ";tag=" + token.m_tag + ";class=" +
                       token.fact(0).getName());
  }

  /**
   * Run all the tests on a given (left) token and every token in the
   * right memory. For the true ones, assemble a composite token and
   * pass it along to the successors.
   *
   * @param lt 
   * @param th 
   * @exception ReteException 
   * @return 
   */

  boolean runTestsVaryRight(Token lt, TokenTree th) throws ReteException 
  {
    if (m_blessed)
      {
        Value v;
        if (m_leftSubIdx == -1)
          v = lt.fact(m_tokenIdx).get(m_leftIdx);
        else
          v = lt.fact(m_tokenIdx).get(m_leftIdx).listValue(null).get(m_leftSubIdx);

        TokenVector fv;
        if ((fv = th.findCodeInTree(v.hashCode(), false)) == null)
          return false;
        
        else
          return doRunTestsVaryRight(lt, fv);     
      }
    else
      return doRunTestsVaryRight(lt, th);     
  }


  boolean doRunTestsVaryRight(Token lt, TokenTree th) throws ReteException 
  {
    if (th == null)
      return false;

    int hash = th.m_hash;
    TokenVector[] facts = th.m_tokens;

    for (int j=0; j<hash; j++)
      if (doRunTestsVaryRight(lt, facts[j]))
        return true;

    return false;
  }
    

  boolean doRunTestsVaryRight(Token lt, TokenVector v) throws ReteException
  {
    if (v != null)
      {
        int size = v.size();
        int ntests = m_localTests.length;
        
        m_context.setToken(lt);
        int tag = lt.m_tag;
        for (int i=0; i<size; i++)
          {
            Token rt = v.elementAt(i);
            m_context.setFact(rt.topFact());

            if (ntests == 0 || runTests(ntests)) 
              {
                if (tag != RU.REMOVE)
                  ++m_matches;
                
                passAlong(Rete.getFactory().newToken(lt, rt));
                if (m_unique && tag != RU.REMOVE && tag != RU.CLEAR)
                  return true;
              }
          }
      }
    return false;
  }
  
  boolean runTestsVaryLeft(Token token, TokenTree th) throws ReteException 
  {
    if (th == null)
      return false;

    if (m_blessed)
      {
        Fact fact = token.topFact();
        Value v;
        if (m_rightSubIdx == -1)
          v = fact.get(m_rightIdx);
        else
          v = fact.get(m_rightIdx).listValue(null).get(m_rightSubIdx);
          
        
        TokenVector fv;
        if ((fv = th.findCodeInTree(v.hashCode(), false)) == null)          
          return false;
        
        else
          return doRunTestsVaryLeft(token, fv);   
      }
    else
      return doRunTestsVaryLeft(token, th);     
  }

  boolean doRunTestsVaryLeft(Token rt, TokenTree th) throws ReteException 
  {    
    if (th == null)
      return false;


    for (int j=0; j<th.m_hash; j++)
      doRunTestsVaryLeft(rt, th.m_tokens[j]);

    return false;
  }

  boolean doRunTestsVaryLeft(Token rt, TokenVector v) throws ReteException
  {
    if (v != null)
      {
        int size = v.size();
        if (size > 0)
          {
            int ntests = m_localTests.length;
            
            m_context.setFact(rt.topFact());

            for (int i=0; i<size; i++)
              {
                Token lt = (Token) v.elementAt(i);
                m_context.setToken(lt);

                if (ntests == 0 || runTests(ntests)) 
                  {
                    // the new token has the *left* token's tag at birth...
                    
                    Token nt = Rete.getFactory().newToken(lt, rt);
                    nt.m_tag = rt.m_tag;
                    passAlong(nt);
                  }
              }
          }
      }
    return false;
  }

  final boolean runTests(int ntests)
       throws ReteException 
  {
    Test[] theTests = m_localTests;
    for (int i=0; i < ntests; i++) 
      {
        if (!theTests[i].doTest(m_context))
          return false;
      }        

    return true;
  }

  /**
   * Beginning of backward chaining. This is very slow; we need to do more
   * of the work at compile time.
   */
  void setBackchainInfo(Pattern p, Defrule d)
  {
    m_pattern = p;
    m_defrule = d;
  }

  private void askForBackChain(Token token) throws ReteException
  {
    // In theory, we could allow m_matches != 0 and use this to retract need-x facts.
    // I can't wuite figure out how to do this, though.

    if (m_pattern == null || m_matches != 0)
      return;
    
    Fact f = new Fact(RU.BACKCHAIN_PREFIX + m_pattern.getName(), m_engine);

    // We just want the stubby beginnings of this fact.
    Fact vv = f;
    
    // For each slot in the pattern...
    for (int i=0; i<m_pattern.getNSlots(); i++)
      {
        int type = m_pattern.getDeftemplate().getSlotType(i);

        // This is the slot value, which we're looking to set to something useful...        
        Value val = Funcall.NIL;

        ValueVector slot = null;
        if (type == RU.MULTISLOT)
          slot = new ValueVector();

        // Look at every test

        for (int j=0; j<m_pattern.getNTests(i); j++)
          {
            Test1 test = m_pattern.getTest(i, j);

            // only consider EQ tests, not NEQ
            if (test.m_test != Test.EQ)
              continue;


            // If this is a variable, and we can pull a value out of the token, 
            // we're golden; but if this is the first occurrence, forget it.
            else if (test.m_slotValue instanceof Variable)
              {
                Binding b =
                  (Binding) m_defrule.getBindings().get(test.m_slotValue.variableValue(null));

                if (b.m_factIndex < token.size())
                  {
                    val = token.fact(b.m_factIndex).get(b.m_slotIndex);
                    
                    if (b.m_subIndex != -1)
                      val = val.listValue(null).get(b.m_subIndex);
                  }
                
                if (type == RU.SLOT)
                  break;
              }

            // Otherwise, it's a plain value, and this is what we want!
            else
              {
                val = test.m_slotValue;

                if (type == RU.SLOT)
                  break;
              }

            // Add something to this multislot.
            if (type == RU.MULTISLOT)
              {
                if (slot.size() < (test.m_subIdx + 1))
                  slot.setLength(test.m_subIdx + 1);
                slot.set(val, test.m_subIdx);
                val = Funcall.NIL;
              }
          }

        if (type == RU.MULTISLOT)
          {
            for (int ii=0; ii<slot.size(); ii++)
              if (slot.get(ii) == null)
                slot.set(Funcall.NIL, ii);
            
            val = new Value(slot, RU.LIST);
          }

        vv.set(val, i);
        val = Funcall.NIL;
      }
    
    // The engine will assert or retract this after the current LHS cycle.
    m_engine.setPendingFact(vv, m_matches == 0);
  }


  /**
   * Describe myself
   * @return A string showing all the tests, etc, in this node.
   */
  public String toString() 
  {
    StringBuffer sb = new StringBuffer(256);
    sb.append("[Node2 ntests=");
    sb.append(m_tests.size());
    sb.append(" ");
    for (int i=0; i<m_tests.size(); i++)
      {
        sb.append(m_tests.elementAt(i).toString());
        sb.append(" ");
      }
    sb.append(";usecount = ");
    sb.append(m_usecount);
    sb.append(";unique = ");
    sb.append(m_unique);
    sb.append("]");
    return sb.toString();
  }


  /**
   * @param stream 
   * @exception IOException 
   * @exception ClassNotFoundException 
   */
  private void readObject(ObjectInputStream stream)
       throws IOException, ClassNotFoundException
  {
    stream.defaultReadObject();
    initTransientMembers();
  }

  // Called from the Constructor and from readObject
  private void initTransientMembers()
  {
    boolean useSortCode = (m_leftIdx == -1);
    if (m_left == null)
      m_left = new TokenTree(m_hashkey, useSortCode, m_tokenIdx, m_leftIdx, m_leftSubIdx);
    else
      m_left.clear();

    if (m_right == null)
      m_right = new TokenTree(m_hashkey, false, 0, m_rightIdx, m_rightSubIdx);
    else
      m_right.clear();
  }

  /*
   * Textural description of memory contents
   */
    
  StringBuffer displayMemory()
  {
    StringBuffer sb = new StringBuffer("\nLeft Memory:\n");
    for (int i=0; i<m_left.m_hash; i++)
      {
        TokenVector tv = m_left.m_tokens[i];
        if (tv == null)
          continue;
        for (int j=0; j< tv.size(); j++)
          {
            sb.append(tv.elementAt(j));
            sb.append("\n");
          }
      }
    sb.append("\nRightMemory:\n");
    for (int i=0; i<m_right.m_hash; i++)
      {
        TokenVector fv = m_right.m_tokens[i];
        if (fv == null)
          continue;
        for (int j=0; j< fv.size(); j++)
          {
            sb.append(fv.elementAt(j));
            sb.append("\n");
          }
      }                                    
    return sb;
  }
  


  /*
   * Move the tests into an array
   * possibly compact the test array
   */

  void complete() throws ReteException
  {
    loadAccelerator();
    
    // Move into array
    super.complete();


    // Try to have a positive Test2Simple first
    for (int i=0; i<m_localTests.length; i++)
      {
        Test t = m_localTests[i];
        if (t instanceof Test2Simple)
          {
            Test2Simple t2s = (Test2Simple) t;
            if (t2s.getTest())
              {
                if (i > 0)
                  {
                    Test tmp = m_localTests[0];
                    m_localTests[0] = t2s;
                    m_localTests[i] = tmp;
                  }
                m_rightIdx = t2s.getRightIndex();
                m_tokenIdx = t2s.getTokenIndex();
                m_leftIdx = t2s.getLeftIndex();
                m_blessed = true;
                break;
              }
          }
      }

    
    // If this fails, try to have a positive Test2Multi first
    if (!m_blessed)
      {
        for (int i=0; i<m_localTests.length; i++)
          {
            Test t = m_localTests[i];
            if (t instanceof Test2Multi)
              {
                Test2Multi t2s = (Test2Multi) t;
                if (t2s.getTest())
                  {
                    if (i > 0)
                      {
                        Test tmp = m_localTests[0];
                        m_localTests[0] = t2s;
                        m_localTests[i] = tmp;
                      }
                    m_rightIdx = t2s.getRightIndex();
                    m_rightSubIdx = t2s.getRightSubIndex();
                    m_tokenIdx = t2s.getTokenIndex();
                    m_leftIdx = t2s.getLeftIndex();
                    m_leftSubIdx = t2s.getLeftSubIndex();
                    m_blessed = true;
                    break;
                  }
              }
          }
      }

    initTransientMembers();
  }

}




