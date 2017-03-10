package coc.agent.engine;

/**
 * A generic Rete network test. Different implementations of this represent pattern-network
 * tests, join-network tests, etc.
 * <P>
 */

public interface Test
{
  /** Used by Test constructors to indicate this test is for equality */
  int EQ  = 0;
  /** Used by Test constructors to indicate this test is for inequality */
  int NEQ = 1;

  /**
   * Perform the actual test. The context argument contains all relevant information
   * needed to resolve variables, etc.
   * @param context The execution context in which to evaluate the test
   * @exception ReteException  If anything goes wrong
   * @return The result of the test
   */

  boolean doTest(Context context) throws ReteException;

}





