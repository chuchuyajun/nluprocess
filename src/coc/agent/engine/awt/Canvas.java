package coc.agent.engine.awt;
import coc.agent.engine.*;

import java.awt.Graphics;

/** **********************************************************************
 * A GUI adapter for Jess. Lets you do graphics from Jess code!
 * <P>
 * Construct one of these with the name of a 'painting' Userfunction as
 * the first argument (a deffunction, usually.) The function should expect a
 * Component as its first argument, and a java.awt.Graphics object
 * as its second argument (both as EXTERNAL_OBJECTs)
 * <P>
 ********************************************************************** */

public class Canvas extends java.awt.Canvas
{
  private Funcall m_fc;
  private Rete m_engine;

  /**
   * Will generally be called from Jess language code via reflection.
   * @param uf The name of a Jess function to call when a paint() message
   *           arrives
   * @param engine The engine that should run the function
   * @exception ReteException If something goes wrong.
   */
  public Canvas(String uf, Rete engine) throws ReteException
  {
    m_engine = engine;
    m_fc = new Funcall(uf, engine);
    m_fc.add(new Value(this));
    m_fc.setLength(3);
  }      

  /**
   * The paint() method that all AWT components have. This implementation calls the
   * Jess function named in the constructor argument. When that function is called, its
   * first argument will be this Canvas object, and the second will be the graphics
   * context.
   * @param g A graphics context 
   */

  public void paint(java.awt.Graphics g)
  {
    try
      {
        m_fc.set(new Value(g), 2);
        m_fc.execute(m_engine.getGlobalContext());
      }
    catch (ReteException re)
      {
        m_engine.getErrStream().println(re);
      }
  }
}
