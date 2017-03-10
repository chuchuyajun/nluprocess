package coc.agent.engine;
import java.util.EventListener;

/**
 * JessListener is a notification interface for Jess events. Objects that wish to
 * be notified of significant happenings in a Jess engine should implement this
 * interface, then register themselves with a Rete object using Rete.addJessListener().
 *
 * <P>
 * @see JessEvent
 * @see Rete#addJessListener
 * @see Rete#removeJessListener
 */

public interface JessListener extends EventListener
{
  /**
   * Called by a JessEvent source when something interesting happens.
   * The typical implementation of eventHappened will switch on the return value
   * of je.getType().
   *
   * @param je an event object describing the event.
   * @exception ReteException if any problem occurs during event handling.
   */

  void eventHappened(JessEvent je) throws ReteException;
}
