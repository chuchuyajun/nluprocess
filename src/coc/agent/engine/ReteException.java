package coc.agent.engine;
import java.io.*;

/**
 * ReteException is the parent type of all exceptions thrown by public methods
 * in the Jess library.
 * <P>
 */

public class ReteException extends Exception implements Serializable
{

  /**
   * Constructs a ReteException containing a descriptive message.
   * The three separate arguments make it easy to 
   * assemble a message from pieces.
   * @param routine the name of the routine this exception occurred in.
   * @param msg an informational message.
   * @param data usually some data; appended to msg after a space.
   */

  public ReteException(String routine, String msg, String data)
  {    
    m_routine = routine;
    m_message = msg;
    m_data = data;
  }

  /**
   * Constructs a ReteException containing a nested exception.
   * @param routine the name of the routine this exception occurred in.
   * @param msg an informational message.
   * @param t another type of exception that this object wraps
   */

  public ReteException(String routine, String msg, Throwable t)
  {    
    m_routine = routine;
    m_message = msg;
    m_nextException = t;
  }

  
  private Throwable m_nextException;
  
  /**
     * Get any nested exception objects
     * @return Value of nextException.
     */
  public Throwable getNextException() {return m_nextException;}
  
  /**
     * Set a nested exception object
     * @param v  Value to assign to nextException.
     */
  public void setNextException(Throwable  v) {m_nextException = v;}
  

  private int m_lineNumber = -1;
  /**
   * Get the program line number where the error occurred.
   * @return Value of lineNumber.
   */
  public int getLineNumber() {return m_lineNumber;}
  
  /**
   * Set the program line number where the error occurred.
   * @param v  Value to assign to lineNumber.
   */
  public void setLineNumber(int  v) {m_lineNumber = v;}
  
  
  private String m_message;
  
  /**
   * Get the error message.
   * @return Value of message.
   */
  public String getMessage() {return m_message;}
  
  /**
   * Set the error message.
   * @param v  Value to assign to message.
   */
  public void setMessage(String  v) {m_message = v;}
  

  private String m_routine;
  
  /**
   * Get the Java routine name where this error occurred
   * @return Value of routine.
   */
  public String getRoutine() {return m_routine;}
  
  /**
   * Set the Java routine name where this error occurred
   * @param v  Value to assign to routine.
   */
  public void setRoutine(String  v) {m_routine = v;}
  
  private String m_data;
  
  /**
   * Get the extra error data.
   * @return Value of data.
   */
  public String getData() {return m_data;}
  
  /**
   * Set the value of the extra error data.
   * @param v  Value to assign to data.
   */
  public void setData(String  v) {m_data = v;}
    
  private String m_programText;
  
  /**
     * Get the Jess program fragment that led to this exception
     * @return Value of programText.
     */
  public String getProgramText() {return m_programText;}
  
  /**
     * Set the Jess program fragment that led to this exception.
     * @param v  Value to assign to programText.
     */
  public void setProgramText(String  v) {m_programText = v;}
  

  private StringBuffer m_context;

  /**
   * Adds information about where an error happened to a ReteException.
   * Contexts are tracked cumulatively, and the toString message will show all
   * contexts that have been added.
   * @param s a description of an execution context: 'defrule Foo', for example.
   * @see coc.agent.engine.ReteException#toString
   */
  public void addContext(String s)
  {
    if (m_context == null)
      m_context = new StringBuffer();
    m_context.append(" while executing ");
    m_context.append(s);
  }
  
  /**
   * Get the context information for this error.
   * @return The context
   */ 
  public String getContext() { return m_context.toString(); }


  /**
   * Returns a String representation of this ReteException. The String includes the
   * routine name, message, and any contexts that have been added.
   * @return a string containing this information.
   */

  public String toString()
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("Jess reported an error in routine ");
    sb.append(m_routine);
    if (m_context != null)
      sb.append(m_context);
    sb.append(".\n");
    sb.append("  Message: ");
    sb.append(m_message);

    if (m_data != null)
      {
        sb.append(" ");
        sb.append(m_data);
      }

    sb.append(".");

    if (m_programText != null)
      {
        sb.append("\n  Program text: ");
        sb.append(m_programText);
        if (m_lineNumber != -1)
          {
            sb.append(" at line ");
            sb.append(m_lineNumber);
            sb.append(".");
          }
      }
    
    return sb.toString();
  }


}
