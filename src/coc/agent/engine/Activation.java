package coc.agent.engine;
import java.io.*;

/**
 * An activation of a rule. Contains enough info to bind
 * a rule's variables.
 */

public class Activation implements Serializable
{
  /**
    Token is the token that got us fired.
   */
  
  private Token m_token;

  /**
   * Get the Rete network Token that caused this Activation.
   * @return The token.
   */

  public final Token getToken() { return m_token; }

  /**
    Rule is the rule we will fire.
   */

  private Defrule m_rule;

  /**
   * Return the activated rule.
   * @return The rule.
   */

  public final Defrule getRule() { return m_rule; }


  /**
   * True if activation has been cancelled
   */
  private boolean m_inactive;

  Activation(Token token, Defrule rule) 
  {
    m_token = token;
    m_rule = rule;
  }

  /**
   * Returns true if this activation is live, or false if it's been cancelled.
   */

  public boolean isInactive() { return m_inactive; }

  void setInactive() { m_inactive = true; }


  /**
   * Fire the rule
   */

  boolean fire() throws ReteException 
  {
    m_rule.fire(m_token);
    return true;
  }

  private int m_seq;
  void setSequenceNumber(int i) { m_seq = i; }

  void debugPrint(PrintWriter ps) throws ReteException 
  {
    m_rule.debugPrint(m_token, m_seq, ps);
  }

  /**
     Compare this object to another object.
     @param o The object to compare to.
   */

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof Activation))
      return false;
    
    else
      {
        Activation a = (Activation) o;
        return
          this.m_rule == a.m_rule &&
          this.m_token.dataEquals(a.m_token);
      }             
  }


  /**
   * Produce a string representation of this Activation for use in debugging.
   * @return The string representation
   */

  public String toString()
  {
    try
      {
        StringBuffer sb = new StringBuffer(100);
        sb.append("[Activation: ");
        sb.append(m_rule.getName());
        sb.append(" ");
        sb.append(Rete.factList(m_token));
        sb.append(" ; time=");
        sb.append(m_token.getTime());
        sb.append(" ; salience=");
        sb.append(m_rule.getSalience());
        sb.append("]");
        return sb.toString();
      }
    catch (ReteException re) { return re.toString(); }
  }

}

