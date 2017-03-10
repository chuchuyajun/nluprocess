/** **********************************************************************
 * Specialized two-input nodes for negated patterns
 *
 * NOTE: CLIPS behaves in a surprising way which I'm following here.
 * Given this defrule:
 * <PRE>
 * (defrule test-1
 *  (bar)       
 *  (not (foo))
 *  =>
 *  (printout t "not foo"))
 *  
 * CLIPS behaves this way:
 * 
 * (watch activations)
 * (assert (bar))
 * ==> Activation 0 test-1
 * (assert (foo))
 * <== Activation 0 test-1
 * (retract (foo))
 * ==> Activation 0 test-1
 * 
 * This is not surprising yet. Here's the funky part
 * 
 * (run)
 * "not foo"
 * (assert (foo))
 * (retract (foo))
 * ==> Activation 0 test-1
 *
 * The rule fires,  and all that's required to fire it again is for the
 * "not-ness" to be removed and replaced; the (bar) fact does not need to
 * be replaced. This obviously falls out of the implementation; it makes things
 * easy!
 *
 * </PRE>
*
 * $Id: NodeNot2.java,v 1.1 2012/10/02 21:06:04 Buildadmin Exp $
 ********************************************************************** */

package coc.agent.engine;
import java.io.*;
import java.util.Vector;

/**
 */
class NodeNot2 extends Node2 implements Serializable
{
  /**
   * @param engine 
   */
  NodeNot2(Rete engine, int hashkey) throws ReteException
  {
    super(engine, false, hashkey);
  }

  boolean callNodeLeft(Token token) throws ReteException
  {  
    if (token.m_tag == RU.ADD || token.m_tag == RU.UPDATE)
      token = Rete.getFactory().newToken(token);

    return super.callNodeLeft(token);
  }
    
  /**
   * Run all the tests on a given (left) token and every token in the
   * right memory. Every time a right token *passes* the tests, increment 
   * the left token's negation count; at the end, if the
   * left token has a zero count, pass it through.
   * 
   * The 'nullToken' contains a fact used as a placeholder for the 'not' CE.
   * @param lt 
   * @param th 
   * @exception ReteException 
   * @return 
   */
  boolean runTestsVaryRight(Token lt, TokenTree th) throws ReteException 
  {
    if (lt.m_tag != RU.REMOVE)      
      super.runTestsVaryRight(lt, th);
    
    if (lt.m_negcnt == 0)
      {
        Token nt = Rete.getFactory().newToken(lt, m_engine.getNullFact());
        nt.updateTime(m_engine);
        m_matches++;
        passAlong(nt);
      }
    return false;
  }

  /**
   * @param lt 
   * @param th 
   * @exception ReteException 
   */
  boolean doRunTestsVaryRight(Token lt, TokenTree th)
       throws ReteException
  {
    if (th == null)
      return false;
    
    for (int j=0; j<th.m_hash; j++)
      doRunTestsVaryRight(lt, th.m_tokens[j]);

    return false;
  }
  
  boolean doRunTestsVaryRight(Token lt, TokenVector v) throws ReteException
  {
    if (v != null)
    {
      int size = v.size();
      if (size > 0)
        {
          int ntests = m_localTests.length;
          m_context.setToken(lt);
          
          for (int i=0; i<size; i++)
            {
              m_context.setFact(v.elementAt(i).topFact());
              
              if (ntests == 0 || runTests(ntests))
                lt.m_negcnt++;
            }
        }
    }
    return false;

  }

  /**
   * Run all the tests on a given (right) token and every token in the
   * left memory. For the true ones, increment (or decrement) the appropriate
   * negation counts. Any left token which transitions to zero gets passed
   * along.
   * @param rt 
   * @param th 
   * @exception ReteException 
   * @return 
   */

  boolean doRunTestsVaryLeft(Token rt, TokenTree th) throws ReteException 
  {
    if (th == null)
      return false;

    for (int j=0; j<th.m_hash; j++)
      {
        TokenVector v = th.m_tokens[j];
        doRunTestsVaryLeft(rt, v);
      }

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
            int tag = rt.m_tag;

            m_context.setFact(rt.topFact());
            for (int i=0; i<size; i++)
              {
                Token lt = v.elementAt(i);
                m_context.setToken(lt);
                
                if (ntests == 0 || runTests(ntests)) {
                  if (tag == RU.ADD || tag == RU.UPDATE) 
                    {
                      // retract any activation due to the left token
                      Token nt2 = Rete.getFactory().newToken(lt, m_engine.getNullFact());
                      nt2.updateTime(m_engine);
                      nt2.m_tag = RU.REMOVE;
                      passAlong(nt2);
                      lt.m_negcnt++;
                    } 
                  else if (--lt.m_negcnt == 0) 
                    {
                      // pass along the revitalized left token
                      Token nt2 = Rete.getFactory().newToken(lt, m_engine.getNullFact());
                      nt2.updateTime(m_engine);
                      passAlong(nt2);
                    }
                  else if (lt.m_negcnt < 0)
                    throw new ReteException("NodeNot2.RunTestsVaryLeft",
                                            "Corrupted Negcnt (< 0)",
                                            "");
                }
              }
          }
      }
    return false;
  }
  /**
   * Describe myself
   * @return 
   */
  public String toString() 
  {
    StringBuffer sb = new StringBuffer(256);
    sb.append("[NodeNot2 ntests=");
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
