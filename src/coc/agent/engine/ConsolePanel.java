package coc.agent.engine;

import java.awt.*;
import coc.agent.engine.awt.TextReader;
import coc.agent.engine.awt.TextAreaWriter;
import java.awt.event.*;
import java.util.*;
import java.io.*;

/**
 * A basic question-and-answer dialog GUI.
 * This class is a Panel containing input and output text areas for a Rete to use.
 * It uses the TextReader and TextAreaWriter classes to turn textual data into I/O streams.
 * The Console and ConsoleApplet classes both display an instance of this.
 *
 * <P>
 */

public class ConsolePanel extends Panel implements Serializable
{
  // Members used for presenting output
  private TextAreaWriter m_taw;

  // Members used for getting input
  private TextField m_tf;

  // TextReader is in the jess package
  private TextReader m_in;

  // We need the engine to fire events on...
  Rete m_rete;
  /**
   * Lay out the Panel; also attach the Rete object to
   * the input and output text components.
   * @param r A Rete engine
   */
  
  public ConsolePanel(Rete r)
  {
    // Remember the engine
    m_rete = r;

    // Set up the GUI elements
    final TextArea ta = new TextArea(10, 40);
    m_tf = new TextField(50);
    ta.setEditable(false);
    Button bClear = new Button("Clear Window");
    Panel p = new Panel();
    p.setLayout(new BorderLayout());

    // Set up I/O streams
    m_taw = new TextAreaWriter(ta);
    m_in = new TextReader(false);
    
    // Assemble the GUI
    setLayout(new BorderLayout());
    add("Center", ta);
    p.add("Center", m_tf);
    p.add("East", bClear);
    add("South", p);
    
    m_tf.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae)
        {
          synchronized (ta)
            {  
              try
                {
                  m_taw.write(m_tf.getText());
                  m_taw.write('\n');
                  m_taw.flush();
                }
              catch (IOException ioe)
                {
                  // Can't really happen
                }
            }
          
          m_in.appendText(m_tf.getText() + "\n");
          m_tf.setText("");    
        }
    });
    
    bClear.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae)
        {
          m_taw.clear();
          m_tf.setText("");
        }
    });
    
    // Configure the Rete object
    PrintWriter pw = new PrintWriter(m_taw, true);
    r.addInputRouter("t", m_in, true);
    r.addOutputRouter("t", pw);
    r.addInputRouter("WSTDIN", m_in, true);
    r.addOutputRouter("WSTDOUT", pw);
    r.addOutputRouter("WSTDERR", pw);
  }

  /**
   * Move focus to the input area. Helps to call this whenever a button is clicked, etc.
   */

  final public void setFocus() { m_tf.requestFocus(); }

}

