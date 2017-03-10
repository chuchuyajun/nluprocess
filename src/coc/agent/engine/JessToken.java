/** **********************************************************************
 * A packet of info  about a token in the input stream.
 *
 * $Id: JessToken.java,v 1.1 2012/10/02 21:06:08 Buildadmin Exp $
 ********************************************************************** */

package coc.agent.engine;
import java.io.*;

/**
 */
final class JessToken implements Serializable
{
  String m_sval;
  double m_nval;
  int m_lineno;
  int m_ttype;

  /**
   * @param context 
   * @exception ReteException 
   * @return 
   */
  Value tokenToValue(Context context) throws ReteException
  {
    // Turn the token into a value.
    switch (m_ttype) 
      {
      case RU.FLOAT:
        return new Value(m_nval, RU.FLOAT);
      case RU.INTEGER:
        return new Value(m_nval, RU.INTEGER);
      case RU.STRING:
        return new Value(m_sval, RU.STRING);
      case RU.VARIABLE: case RU.MULTIVARIABLE:
        if (context != null)
          {
            Binding b = context.findBinding(m_sval);
            if (b != null)
              {
                return b.m_val;
              }
            else
              throw new ReteException("JessToken.tokenToValue",
                                      "Unbound variable:", m_sval);
          }
        else          
        return new Value("?" + m_sval, RU.ATOM);

      case RU.ATOM:
        return new Value(m_sval, RU.ATOM);

      case RU.NONE:
        if ("EOF".equals(m_sval))
          return Funcall.EOF;
        // FALL THROUGH

      default:
        {
          return new Value("" + (char) m_ttype, RU.STRING);
        }
      }    
  }

  /**
   * @return 
   */
  boolean isBlankVariable()
  {
    return (m_sval != null && m_sval.startsWith(Tokenizer.BLANK_PREFIX));
  }

  /**
   * @return 
   */
  public String toString() 
  {
    if (m_ttype == RU.VARIABLE)
      return "?" + m_sval;
    else if (m_ttype == RU.MULTIVARIABLE)
      return "$?" + m_sval;
    else if (m_ttype == RU.STRING)
      return "\"" + m_sval + "\"";
    else if (m_sval != null)
      return m_sval;
    else if (m_ttype == RU.FLOAT)
      return "" + m_nval;
    else if (m_ttype == RU.INTEGER)
      return "" + (int) m_nval;
    else return "" +  (char) m_ttype;
  }

}
  



