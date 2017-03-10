package coc.agent.engine.awt;
import java.awt.*;
import java.awt.event.*;
import coc.agent.engine.*;

/* **********************************************************************
 * JessAWTListener
 * An AWT Event Adapter for Jess.
 * This class is used to implement all the other listeners in this package.
 *
 */

class JessAWTListener
{
  private Funcall m_fc;
  private Rete m_engine;

  /**
   * @param uf 
   * @param engine 
   * @exception ReteException 
   */
  JessAWTListener(String uf, Rete engine) throws ReteException
  {
    m_engine = engine;
    m_fc = new Funcall(uf, engine);
    m_fc.setLength(2);
  }      

  /**
   * @param e 
   */
  final void receiveEvent(AWTEvent e)
  {
    try
      {
        m_fc.set(new Value(e), 1);
        m_fc.execute(m_engine.getGlobalContext());
      }
    catch (ReteException re)
      {
        m_engine.getErrStream().println(re);
      }
  }
}

