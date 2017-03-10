package coc.agent.engine;

import java.io.*;
import java.awt.*;
import java.awt.event.*;

/** **********************************************************************
 * A basic graphical console for Jess.
 * Basically just run the class coc.agent.engine.Main in a window.
 * <P>
 ********************************************************************** */

public class Console extends Frame implements Serializable
{
  ConsolePanel m_panel;
  Rete m_rete;

  /**
   * Create a Console. This constructor creates a new Rete object, which you can
   * get ahold of with getEngine().
   * @param title The title for the Frame.
   */

  public Console(String title)
  {
    this(title, new Rete());
  }

  /**
   * Create a Console, using a prexisting Rete object.
   * @param title The title for the Frame.
   * @param r A prexisting Rete object.
   */
 
  public Console(String title, Rete r)
  {
    super(title);
    m_rete = r;
    m_panel = new ConsolePanel(r);
    
    add("Center",m_panel);
    Button b;
    add("South", b = new Button("Quit"));
    b.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        dispose();
        System.exit(0);
      }
    });
    validate();
    setSize(500,300);
    //modified by zhongshan for GUIEventCleanUp
	setVisible(true);
	toFront();
  }

  /**
   * Return the Rete engine being used by this Console.
   * @return The Rete object used by this console.
   */
  public Rete getEngine() { return m_rete; }  

  /**
   * Pass the argument array on to an instance of coc.agent.engine.Main connected to this
   * Console, and call Main.execute().
   * @param argv Arguments for coc.agent.engine.Main.initialize().
   */

  public void execute(String [] argv)
  {
    Main m = new Main();
    m.initialize(argv, m_rete);
    m_panel.setFocus();
    while (true)
      m.execute(true);
  }


  /**
   * Trivial main() to display this frame
   * @param argv Arguments passed to execute().
   */
  
  public static void main(String[] argv) 
  {
    new Console("Jess Console").execute(argv);
  }
}



