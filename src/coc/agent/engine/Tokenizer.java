
package coc.agent.engine;
import java.io.*;


/**
 * A simple Tokenizer for the CLIPS language
*/

class Tokenizer implements Serializable
{
  private transient PushbackReader m_ios;
  private int m_line = 0;
  private boolean m_nl = false;
  public static final String BLANK_PREFIX = "_blank_";
  public static final String BLANK_MULTI = "_blank_mf";

  /**
   * Constructor - use large buffer for FileReader, no buffering
   * for everything else
   * @param ios 
   */
  public Tokenizer(Reader ios)
  {
    if (ios instanceof PushbackReader)
      m_ios = (PushbackReader) ios;

    else if (ios instanceof FileReader)
      m_ios = new PushbackReader(new BufferedReader(ios, 512));
    else
      m_ios = new PushbackReader(ios);
  }

  public void reportNewlines(boolean b) { m_nl = b; }

  StringBuffer m_sb = new StringBuffer(5);

  public synchronized String readLine() throws ReteException
  {
    int c;
    m_sb.setLength(0);
    while ((c = nextChar()) != '\n' && c != -1)
      m_sb.append((char) c);
    
    if (c == -1 && m_sb.length() == 0)
      return null;

    ++m_line;
    return m_sb.toString();
  }

  public synchronized JessToken nextToken() throws ReteException
  {
    m_sb.setLength(0);
    int c;

    // ******************************
    // Eat any leading whitespace

    try
      {
      whiteloop:
        do
          {
            c = nextChar();
            
            switch (c)
              {
              case -1:
                {
                  return finishToken(-1, m_sb);                  
                }
                
              case '\n':
                // new line
                ++m_line;
                if (m_nl)
                  return finishToken('\n', m_sb);
                // else keep going
                break;
                
              case ' ': case '\t': 
                // keep going
                break;
                
              default:
                // OK, no more whitespace
                m_ios.unread(c);
                break whiteloop;
              }
          }
        while (c != -1);
        
        // ******************************
        // First character of a token - treat delimiters differently
        
        c = nextChar();
        switch (c)
          {
          case -1:
            return finishToken(-1, m_sb);
            
            // comment
          case ';':
            discardToEOL();
            return nextToken();
            
            // quoted string
          case '"':
            readString(m_sb);
            return finishToken('"', m_sb);
            
            // single-character tokens
          case '(': case ')': case '&': case '~': case '|': case '=':
            // m_sb.append((char)c);
            return finishToken(c, m_sb);
            
            // anything else
          default:
            m_sb.append((char)c);
            break;
          }
        
        // ******************************
        // second and later characters of a multi-character token
        do
          {
            c = nextChar();
            if (Character.isWhitespace((char)c))
              {
                m_ios.unread(c);
                return finishToken(0, m_sb);
              }
            
            switch(c)
              {
                // end of file
              case -1:
                return finishToken(-1, m_sb);
                
                // separators
              case '(': case ')': case '&': case '~': case '|': case '<':
              case ';': case '"':
                {
                  m_ios.unread(c);
                  return finishToken(0, m_sb);
                } 
              
              // character escape
              case '\\':
                c = nextChar();
                if (c == -1)
                  return finishToken(-1, m_sb);
                else
                  m_sb.append((char) c);
                break;
                
                // ordinary chars
              default: 
                m_sb.append((char)c);
                break;
              }
          }
        while (true);
      }
    catch (IOException ioe)
      {
        throw new ReteException("Tokenizer.nextToken",
                                "I/O Exception",
                                ioe);
      }
    // NOT REACHED
  }

  /**
   * Returns the next character from the stream; CR, LF, or CRLF are
   * each returned as the single character '\n'.
   */
  private int nextChar() throws ReteException
  {
    try
      {
        int c = m_ios.read();
        if (c == '\r')
          {
            if (m_ios.ready())
              {
                c = m_ios.read();
                if (c != '\n')
                  m_ios.unread(c);
              }
            c = '\n';
          }
        return c;
      }
    catch (IOException ioe)
      {
        throw new ReteException("Tokenizer.nextChar",
                                "Error on input stream",
                                ioe);
      }
  }

  /**
   * The first argument is a hint about what kind of token this is.
   * '0' means an atom or number; -1 means EOF was hit; '"' means a
   * String; '\n' means return CRLF; anything else is a one-character token.
   */
  private JessToken finishToken(int c, StringBuffer s) throws ReteException
  {
    JessToken jt = new JessToken();
    jt.m_lineno = m_line;

    switch (c)
      {
        // quoted string
      case '"':
        jt.m_ttype = RU.STRING;
        jt.m_sval = s.toString();
        break;

        // single-character tokens:
      case '(': 
        jt.m_ttype = '(';
        jt.m_sval = "(";
        break;
        
      case ')':
        jt.m_ttype = ')';
        jt.m_sval = ")";
        break;

      case '&':
        jt.m_ttype = '&';
        jt.m_sval = "&";
        break;

      case '~':
        jt.m_ttype = '~';
        jt.m_sval = "~";
        break;

      case '|':
        jt.m_ttype = c;
        jt.m_sval = "|";
        break;

      case '=':
        jt.m_ttype = '=';
        jt.m_sval = "=";
        break;
        
        // Return newline token
      case '\n':
        jt.m_ttype = RU.ATOM;
        jt.m_sval = "CRLF";
        break;

        // EOF encountered
      case -1:
        if (s.length() == 0)
          {
            jt.m_ttype = RU.NONE;
            jt.m_sval = "EOF";
            break;
          }
        else return finishToken(0, s);

        // everything else
      case 0:
        String sval = s.toString();
        char ch = sval.charAt(0);

        // VARIABLES
        if (ch == '?')
          {
            jt.m_ttype = RU.VARIABLE;
            if (sval.length() > 1)
              jt.m_sval = sval.substring(1);
            else
              jt.m_sval = RU.gensym(BLANK_PREFIX);
            break;
          }

        // MULTIVARIABLES
        else if (ch == '$' && sval.length() > 1 &&
                 sval.charAt(1) == '?')
          {
            jt.m_ttype = RU.MULTIVARIABLE;
            if (sval.length() > 2)
              jt.m_sval = sval.substring(2);
            else
              jt.m_sval = RU.gensym(BLANK_MULTI);
            break;
          }

        // Atoms that look like parts of numbers
        else if (sval.length() == 1 && (ch == '-' || ch == '.' || ch == '+'))
          {
            jt.m_ttype = RU.ATOM;
            jt.m_sval = sval;
            break;
          }

        if (Character.isDigit(ch) || ch == '-' || ch == '.' || ch == '+')
          {
            // INTEGERS
            try
              {
                int i = Integer.parseInt(sval, 10);
                jt.m_ttype = RU.INTEGER;
                jt.m_nval = i;
            break;            
              }
            catch (NumberFormatException nfe) { /* OK, not an integer. */ }
            
            // FLOATS
            try
              {
                double d = Double.valueOf(sval).doubleValue();
                jt.m_ttype = RU.FLOAT;
                jt.m_nval = d;
                break;            
              }
            catch (NumberFormatException nfe) { /* OK, not a float. */ }
          }

          // PLAIN ATOMS
        jt.m_ttype = RU.ATOM;
        jt.m_sval = sval;
        break;
                        
      default:
        throw new ReteException("Tokenizer.finishToken", "Impossible tag:", "" + (char) c);
      }
    return jt;
  }

  public void discardToEOL() throws ReteException
  {
    // ******************************
    // discard all characters up to and including the next CR, LF, or CRLF
    int c = -1;
    do
      {
        c = nextChar();
        if (c == 10)
          {
            ++m_line;
            break;
          }
      }
    while (c != -1);
  }

  private String readString(StringBuffer s) throws ReteException
  {
    int c = -1;
  loop:
    do
      {
        c = nextChar();
        switch (c)
          {
          case -1: return null;

          case '\\':
            {
              c = nextChar();
              s.append((char)c);
              break;
            }

          // this is no longer an error
          case '\n':
            {
              ++m_line;
              s.append((char)c);
              break;
            }
                
          // end of string
          case '"':
            break loop;

          default:
            s.append((char)c); break;
          }        
      }
    while (c != -1);

    return s.toString();
  }

}

