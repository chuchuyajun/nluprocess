
package coc.agent.engine;

/** **********************************************************************
 * JessEventAdapter
 * A Jess Event Adapter that lets you write JessEvent handlers in Jess.
 * <P>
 ********************************************************************** */

public class JessEventAdapter implements JessListener
{
  private Funcall m_fc;
  private Rete m_engine;

  /**
   * Create an adapter. Normally you'll call this from Jess code using reflection.
   * The first argument is the name of a function to call when a JessEvent occurs;
   * The second is the engine to attach to
   * @param uf The name of a Jess function
   * @param engine The engine to field events from
   * @exception ReteException If anything goes wrong.
   */
  public JessEventAdapter(String uf, Rete engine) throws ReteException
  {
    m_engine = engine;
    m_fc = new Funcall(uf, engine);
    m_fc.setLength(2);
  }      

  /**
   * Called when a JessEvent occurs. The function specified in the constructor is called, 
   * with the event object as the only argument. The function can examine the event using
   * reflection.
   * @param e The event
   */
  public final void eventHappened(JessEvent e)
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
