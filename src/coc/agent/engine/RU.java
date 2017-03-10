package coc.agent.engine;

import java.util.*;
import java.io.*;

/**
 * General utilities for Jess. All fields and methods in this class are static, and
 * there is no constructor.
 * <P>
 */

public class RU implements Serializable
{
  private RU() {}

  /** Relative index of slot name within a deftemplate's slots */
  final public static int DT_SLOT_NAME      = 0;
  /** Relative index of slot default value within a deftemplate's slots */
  final public static int DT_DFLT_DATA      = 1;
  /** Relative index of slot data type within a deftemplate's slots */
  final public static int DT_DATA_TYPE      = 2;
  /** Size of a slot in a deftemplate */
  final public static int DT_SLOT_SIZE      = 3;

  /** Data type of "no value" */
  final public static int NONE             = 0;
  /** Data type of atom */
  final public static int ATOM             = 1 <<  0;
  /** Data type of string */
  final public static int STRING           = 1 <<  1;
  /** Data type of integer */
  final public static int INTEGER          = 1 <<  2;
  /** Data type of a variable */
  final public static int VARIABLE         = 1 <<  3;
  /** Data type of a fact id */
  final public static int FACT_ID          = 1 <<  4;
  /** Data type of float */
  final public static int FLOAT            = 1 <<  5;
  /** Data type of function call stored in a value */
  final public static int FUNCALL          = 1 <<  6;
  /** Data type of fact stored in a value */
  final public static int FACT             = 1 <<  8;
  /** Data type of a list stored in a value */
  final public static int LIST             = 1 <<  9;
  /** Data type of external address */
  final public static int EXTERNAL_ADDRESS = 1 << 11;
  /** Data type of integer array stored in value (internal use) */
  final public static int INTARRAY         = 1 << 12;
  /** Data type of multivariable */
  final public static int MULTIVARIABLE    = 1 << 13;
  /** Data type of slot name stored in a value */
  final public static int SLOT             = 1 << 14;
  /** Data type of multislot name stored in a value */
  final public static int MULTISLOT        = 1 << 15;
  /** Data type of Java long*/
  final public static int LONG             = 1 << 16;

  private static Hashtable m_typeNames = new Hashtable();
  static
  {
    m_typeNames.put(String.valueOf(NONE), "NONE");
    m_typeNames.put(String.valueOf(ATOM), "ATOM");
    m_typeNames.put(String.valueOf(STRING), "STRING");
    m_typeNames.put(String.valueOf(INTEGER), "INTEGER");
    m_typeNames.put(String.valueOf(VARIABLE), "VARIABLE");
    m_typeNames.put(String.valueOf(FACT_ID), "FACT_ID");
    m_typeNames.put(String.valueOf(FLOAT), "FLOAT");
    m_typeNames.put(String.valueOf(FUNCALL), "FUNCALL");
    m_typeNames.put(String.valueOf(FACT), "FACT");
    m_typeNames.put(String.valueOf(LIST), "LIST");
    m_typeNames.put(String.valueOf(EXTERNAL_ADDRESS), "EXTERNAL_ADDRESS");
    m_typeNames.put(String.valueOf(INTARRAY), "INTARRAY");
    m_typeNames.put(String.valueOf(MULTIVARIABLE), "MULTIVARIABLE");
    m_typeNames.put(String.valueOf(SLOT), "SLOT");
    m_typeNames.put(String.valueOf(MULTISLOT), "MULTISLOT");
  }


  /**
   * Given a type constant (ATOM, STRING, INTEGER, etc.) return a String version of
   * the name of that type ("ATOM", "STRING", "INTEGER", etc.)
   *
   * @param type One of the type constants in this class
   * @return The String name of this type, or null if the constant is out of range.
   */

  public static String getTypeName(int type)
  {
    return (String) m_typeNames.get( String.valueOf(type));
  }


  /** Add this token to the Rete network (internal use) */
  final static int ADD       = 0;
  /** Remove this token from the Rete network (internal use) */
  final static int REMOVE    = 1;
  /** Update this token in the Rete network (internal use) */
  final static int UPDATE    = 2;
  /** Clear the Rete network (internal use) */
  final static int CLEAR     = 3;

  /*
    Constants specifying that a variable is bound to a fact-index
    or is created during rule execution
    */

  /** Variable contains a fact index */
  final static int PATTERN = -1;
  /** Variable is local to a defrule or deffunction */
  final static int LOCAL   = -2;
  /** Variable is global */
  final static int GLOBAL  = -3;
  
  /*
    Constants specifying connective constraints
  */

  /** Test is anded with previous */
  final static int AND = 1;

  /** Test is ored with previous */
  final static int OR = 2;
     

  /** String prepended to deftemplate names to form backwards chaining goals */
  final static String BACKCHAIN_PREFIX = "need-";

  /** String prepended to deftemplate names to form backwards chaining goals */
  final static String QUERY_TRIGGER = "__query-trigger-";

  /** Special multislot name used for ordered facts */  
  final static String DEFAULT_SLOT_NAME = "__data";

  /**  The name of the ultimate parent of all deftemplates */  
  final static String PARENT_DEFTEMPLATE = "__fact";

  /*
    A number used in quickly generating semi-unique symbols.
    */
  
  static int s_gensymIdx = 0;

  /**
   * Generate a pseudo-unique symbol starting with "prefix"
   * @param prefix The alphabetic part of the symbol
   * @return The new symbol
   */
  public static synchronized String gensym(String prefix) 
  {
    String sym = prefix + s_gensymIdx++;    
    return sym;
  }

  /**
   * Get a property, but return null on SecurityException
   * @param prop The property name to get
   * @return The value of the property, or null if none or security problem
   */
  public static String getProperty(String prop)
  {
    try
      {
        return System.getProperty(prop);
      }
    catch (SecurityException se)
      {
        return null;
      }
  }

  /**
   * A general-purpose helper routine for use in implementing coc.agent.engine.Strategy classes.   
   * Find the appropriate Object in a Vector of Objects, between the given indexes,
   * using the given Fetch and Compare objects. For example, if you pass in 
   * fetchActTime and compareLTE,  this function will find the first Activation in 
   * the Vector with timestamp less than or equal to 'value'.
   *
   * @param value The reference value to compare to what fetch returns.
   * @param v The vector to search
   * @param start The index in v of the first element to consider
   * @param end The index in v of the last element to consider, plus one.
   * @param ff An RU.Fetch object, used to compute a value for comparison
   * @param ff An RU.Compare object, used to compare fetch values to 'value'.
   *
   * @see coc.agent.engine.Strategy
   */

  public static int bsearchVector(int value, Vector v, int start, int end,
                                  Fetch ff, Compare cc)
  {
    synchronized (v)
      {
        int limit = end;
        int mid;
        while ((end - start) > 10)
          {
            mid = (end - start)/2 + start;
            Object o = v.elementAt(mid);
            int t = ff.fetch(o);
            if (!cc.compare(t, value))
              start = mid;
            else
              end = mid;
          }

        if (++end > limit)
          end = limit;

        for (int i=start; i<end; i++)
          {
            Object o = v.elementAt(i);
            if (cc.compare(ff.fetch(o), value))
              return i;
          }
        return -1;
      }
  }

  
  /**
   * A helper interface used by RU.bsearchVector. 
   * @see RU#bsearchVector
   */

  public interface Fetch extends Serializable
  {
    /**
     * Given an Object, a call to fetch() will
     * return an integer retrieved or compputed from the object in some way.
     * @return the int
     * @see RU#bsearchVector
     */
    int fetch(Object obj);
  }

  /**
   * A helper interface used by RU.bsearchVector.
   * @see RU#bsearchVector
   */
  
  public interface Compare extends Serializable
  {
    /**
     * Given two integers, return true or false depending on some comparison.
     * @param i1 the first int
     * @param i2 the second int
     * @return the comparison
     * @see RU#bsearchVector
     */
    boolean compare(int i1, int i2);
  }

  /** A Fetch that retrieves time stamps from coc.agent.engine.Activation objects. */

  public static final Fetch fetchActTime = new Fetch()
  { public int fetch(Object obj) { return ((Activation) obj).getToken().getTime(); }};

  /** A Fetch that retrieves salience values from coc.agent.engine.Activation objects. */
  public static final Fetch fetchActSalience = new Fetch()
  { public int fetch(Object obj) { return ((Activation) obj).getRule().getSalience();}};

  /** Comparison that returns true if the first int is less than or equal to the second. */
  public static final Compare compareLTE = new Compare()
  { public boolean compare(int i1, int i2) { return  i1 <= i2; }};

  /** Comparison that returns true if the first int is greater than or equal to the second. */
  public static final Compare compareGTE = new Compare()
  { public boolean compare(int i1, int i2) { return  i1 >= i2; }};

  /** Comparison that returns true if the first int is greater than the second. */
  public static final Compare compareGT = new Compare()
  { public boolean compare(int i1, int i2) { return  i1 > i2; }};

  /** Comparison that returns true if the first int is less than the second. */
  public static final Compare compareLT = new Compare()
  { public boolean compare(int i1, int i2) { return  i1 < i2; }};

  /** Comparison that returns true if the first int is equal to the second. */
  public static final Compare compareEQ = new Compare()
  { public boolean compare(int i1, int i2) { return  i1 == i2; }};


}





