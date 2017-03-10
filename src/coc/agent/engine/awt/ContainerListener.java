package coc.agent.engine.awt;
import java.awt.*;
import java.awt.event.*;
import coc.agent.engine.*;

/** **********************************************************************
 * ContainerListener
 * An AWT Event Adapter for Jess.
 * <P>
 */
public class ContainerListener extends JessAWTListener
                               implements java.awt.event.ContainerListener
{
  /**
   * Connect the Jess function specified by name to this event handler object. When this
   * handler receives an AWT event, the named function will be invoked in the given
   * engine.
   * @param uf The name of a Jess function
   * @param engine The Jess engine to execute the function in
   * @exception ReteException If anything goes wrong.
   */
  public ContainerListener(String uf, Rete engine) throws ReteException
  {
    super(uf, engine);
  }      

  /**
   * An event-handler method. Invokes the function passed to the constructor with the
   * received event as the argument.
   * @param e The event
   */
  public void componentAdded(ContainerEvent e) { receiveEvent(e); }
  /**
   * An event-handler method. Invokes the function passed to the constructor with the
   * received event as the argument.
   * @param e The event
   */
  public void componentRemoved(ContainerEvent e) { receiveEvent(e); }

}
