// A Java event class for notifications of various kinds.
package coc.agent.engine;

import java.util.EventObject;

/**
 * JessEvents are used by JessEvent sources (like the Rete class) to convey 
 * information about interesting things that happen to registered event listeners.
 * <P>
 * @see JessListener
 * @see Rete#addJessListener
 * @see Rete#removeJessListener
 */
public class JessEvent extends EventObject
{
  /** A defrule has been added or removed */
  public static final int DEFRULE             = 1 << 0;

  /** A defrule has been fired */
  public static final int DEFRULE_FIRED       = 1 << 1;

  /** A defrule has been activated or deactivated */
  public static final int ACTIVATION          = 1 << 2;

  /** A deffacts has been added or removed */
  public static final int DEFFACTS            = 1 << 3;

  /** A fact has been asserted or retracted */
  public static final int FACT                = 1 << 4;

  /** A definstance has been added or removed */
  public static final int DEFINSTANCE         = 1 << 5;

  /** A deftemplate has been added or removed */
  public static final int DEFTEMPLATE         = 1 << 6;

  /** A defclass has been added or removed */
  public static final int DEFCLASS            = 1 << 7;

  /** A defglobal has been added or removed */
  public static final int DEFGLOBAL           = 1 << 8;

  /** A userfunction has been added or removed */
  public static final int USERFUNCTION        = 1 << 9;

  /** A userpackage has been added or removed */
  public static final int USERPACKAGE         = 1 << 10;

  /** A (clear) has been executed */
  public static final int CLEAR               = 1 << 11;

  /** A (reset) has been executed */
  public static final int RESET               = 1 << 12;

  /** A (run) has been executed */
  public static final int RUN                 = 1 << 13;

  /** A (bload) has been executed */
  public static final int BLOAD               = 1 << 14;

  /** A Rete node has been reached by a token. Deliberately equal to RETE_TOKEN_LEFT */
  public static final int RETE_TOKEN          = 1 << 15;

  /** A Rete node has been reached by a token, calltype left */
  public static final int RETE_TOKEN_LEFT     = 1 << 15;

  /** A Rete node has been reached by a token, calltype right */
  public static final int RETE_TOKEN_RIGHT    = 1 << 16;

  /** A userfunction has been called */
  public static final int USERFUNCTION_CALLED = 1 << 19;

  /** Added to other event-related flags to indicate removal of construct */
  public static final int REMOVED             = 1 << 31;

  Object m_obj;
  int m_type;

  /**
   * Construct a JessEvent containing the given information.
   * @param source the object (usually an instance of Rete) generating the event.
   * @param type one of the manifest constants in this class.
   * @param obj data relevant to the specific type of this event.
   */
  public JessEvent(Object source, int type, Object obj)
  {
    super(source);
    m_type = type;
    m_obj = obj;
  }

  void reset(int type, Object obj) { m_type = type; m_obj = obj; }

  /**
   * Gets the type of this event. The type should be one of the manifest constants in
   * this class.
   * @return the event's type.
   */
  public int getType() { return m_type; }

  /**
   * Gets any optional data associated with this event. The type of this data depends
   * on the type of the event object.
   * @see JessEvent#getType
   * @return the optional data.
   */
  public Object getObject() { return m_obj; }

  /**
   * Return a string suitable for debugging.
   * @return Something like [JessEvent: type=37].
   */
  
  public String toString()
  {
    return "[JessEvent: type=" + m_type + "]";
  }
}
