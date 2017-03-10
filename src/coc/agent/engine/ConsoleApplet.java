package coc.agent.engine;

import java.awt.*;
import java.applet.*;
import java.util.*;
import java.io.*;

/**
 * 
 *    An Applet which uses ConsolePanel. It could
 *    serve as the basis for any number of 'interview' style Expert System
 *    GUIs.
 * <P>
 *    Applet Parameters:
 * <UL>
 * <LI> INPUT: if present, the applet will find the named document relative
 *    to the applet's document base and interpret the file in batch mode,
 *    then fall into a parse loop when the file completes.
 * </UL>
 * <P>
 */

public class ConsoleApplet extends Applet implements Runnable, Serializable
{
  // The display panel
  private ConsolePanel m_panel;
  // The inference engine
  private Rete m_rete;
  // Thread in which the parse loop runs
  private Thread m_thread;
  // Main object used to drive Rete
  private Main m_main;


  public void init()
  {
    setLayout(new BorderLayout());
    m_rete = new Rete(this);
    m_panel = new ConsolePanel(m_rete);
    add("Center", m_panel);
    add("South", new Label());

    String [] argv = new String[] {};
    // Process Applet Parameters
    String appParam = getParameter("INPUT");    
    if (appParam != null)
      argv = new String[] {appParam};
    
    m_main = new Main();
    m_main.initialize(argv, m_rete);
  }

  public synchronized void run()
  {
    do
      {
        try
          {
            m_panel.setFocus();
            while (true)
              m_main.execute(true);
          }
        catch (Throwable t)
          {
            m_thread = null;
          }
      }
    while (m_thread != null);
  }

  public void start()
  {
    if (m_thread == null)
      {
        m_thread = new Thread(this);
        m_thread.start();
      }
  }

  public void stop()
  {
    m_thread = null;
  }

}

