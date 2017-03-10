
package coc.agent.engine;

import java.io.*;
import java.util.zip.*;
import java.util.*;

/**
 * Implements the Jess functions <tt>bload</tt> and <tt>bsave</tt>.
 * <p>
 */
public class DumpFunctions implements Userpackage, Serializable
{

  /**
   * Add this collection of functions to a Rete engine
   * @param engine The Rete object to add the functions to
   */
  public void add(Rete engine)
  {
    engine.addUserfunction(new Dumper(Dumper.DUMP, this));
    engine.addUserfunction(new Dumper(Dumper.RESTORE, this));
  }

  transient private Hashtable m_listeners = new Hashtable();
  /**
   * Register a listener that want to know when a bload/bsave has occurred.
   * @param jel The listener
   */
  public void addJessListener(JessListener jel) { m_listeners.put(jel, jel); }
  /**
   * Remove a listener that no longer wants to know when a bload/bsave has occurred.
   * @param jel The listener
   */
  public void removeJessListener(JessListener jel) { m_listeners.remove(jel); }

  void broadcastEvent(int type, Object data) throws ReteException
  {
    Enumeration e = m_listeners.elements();
    if (e.hasMoreElements())
      {
        JessEvent event = new JessEvent(this, type, data);
        while (e.hasMoreElements())
          ((JessListener) e.nextElement()).eventHappened(event);
      }
  }

}

class Dumper implements Userfunction, Serializable
{
  public static final int DUMP=0, RESTORE=1;
  private int m_cmd;
  private DumpFunctions m_df;

  public Dumper(int cmd, DumpFunctions df) { m_cmd = cmd; m_df = df;}
  public String getName() { return m_cmd == DUMP ? "bsave" : "bload";}

  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String filename = vv.get(1).stringValue(context);
    try
      {            
        switch (m_cmd)
          {
          case DUMP:
            {
              ObjectOutputStream s = new ObjectOutputStream(
                                     new GZIPOutputStream(
                                     new FileOutputStream(filename), 10000));
              Rete r = context.getEngine();
              r.removeFacts();
              s.writeObject(r);
              s.flush();
              s.close();
              return Funcall.TRUE;
            }
          default:        
            {
              ObjectInputStream s = new ObjectInputStream(
                                    new GZIPInputStream(
                                    new FileInputStream(filename), 10000));
              Rete r = (Rete) (s.readObject());
              m_df.broadcastEvent(JessEvent.BLOAD, r);
              return Funcall.TRUE;
            }
          }
      }
    catch (IOException ioe)
      {
        ioe.printStackTrace();
        throw new ReteException(m_cmd == DUMP ? "bsave" : "bload", "IO Exception", ioe);
      }
    catch (ClassNotFoundException cnfe)
      {
        throw new ReteException(m_cmd == DUMP ? "bsave" : "bload", "Class Not Found", cnfe);
      }
    
  }

}


