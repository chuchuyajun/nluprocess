package coc.agent.engine;

import java.io.*;
import java.util.*;
import java.applet.*;
import coc.agent.engine.factory.*;
import coc.agent.engine.awt.TextReader;

/** **********************************************************************
 * The reasoning engine. Executes the built Rete network, and coordinates many
 * other activities.
 * <P>
 */

public class Rete implements Serializable
{

  /**
    If we're embedded in an applet, this is non-null
   */
  transient private Applet m_applet;

  /**
   * Returns the applet this Rete is installed in. Returns null if none.   
   * @return The applet
   */
  public Applet getApplet() { return m_applet; }

  /**
   * Associates this Rete with an applet so that, for instance, the (batch) commands
   * will look for scripts using the applet's document base URL.
   * @param a The applet
   */
  public void setApplet(Applet a) { m_applet = a; }


  /**
    Current conflict resolution strategy
   */
  private Strategy m_strategy;


  /**
    Context for executing global functions
    */

  private Context m_globalContext;

  /**
   * Fetch the global execution context.
   * @return The global execution context.
   */
  public final Context getGlobalContext() { return m_globalContext; }

  transient private boolean m_resetGlobals = true;
  /**
   * When resetGlobals is true, the initializers of global variables are evaluated when
   * (reset) is executed.
   * @param reset The value of this property
   */
  final public void setResetGlobals(boolean reset)
  { m_resetGlobals = reset; }

  /**
   * When resetGlobals is true, the initializers of global variables are evaluated when
   * (reset) is executed.
   * @return The value of this property
   */
  final public boolean getResetGlobals()
  { return m_resetGlobals; }

  public static final int INSTALL=0, ACTIVATE=1, EVERY_TIME=2;
  transient private int m_evalSalience = INSTALL;

  /**
   * Set the salience evaluation behaviour. The behaviour can be one of INSTALL,
   * ACTIVATE, or EVERY_TIME; the default is INSTALL. When the behaviour is INSTALL,
   * a rule's salience is evulated once when the rule is compiled. If it is ACTIVATE, it is
   * computed each time the rule is activated. If it is EVERY_TIME, salience evaluations
   * are done for all rules each time the next rule on the agenda is to be chosen.
   * @param method One of the acceptable values
   * @exception ReteException If something goes wrong
   */
  final public void setEvalSalience(int method) throws ReteException
  {
    if (method < INSTALL || method > EVERY_TIME)
      throw new ReteException("Rete.setEvalSalience", "Invalid value", String.valueOf(method));
    m_evalSalience = method;
  }

  /**
   * Fetch the salience evaluation behaviour
   * @return The salience evaluation behaviour
   */
  final public int getEvalSalience()
  { return m_evalSalience; }
  

  /**
    Successively incremented ID for asserted facts.
    */
  private Object m_idLock = new String("LOCK");
  transient private int m_nextFactId;

  int nextFactId()
  {
    synchronized (m_idLock)
      {
        return m_nextFactId++;
      }
  }
  
  /**
    Successively incremented ID for new rules.
    */

  transient private int m_nextRuleId;
  int nextRuleId() { synchronized (m_idLock) {return m_nextRuleId++;}}

  /**
    Deftemplates are unique to each interpreter.
    */

  private Hashtable m_deftemplates = new Hashtable(101);

  /**
    Deffacts are unique to each interpreter.
    */

  private Hashtable m_deffacts = new Hashtable();

  /**
    Defglobals are unique to each interpreter.
    */

  private Hashtable m_defglobals = new Hashtable();

  private Hashtable m_functions = new Hashtable(101);

  private transient Fact m_initialFact, m_clearFact, m_nullFact;
  Fact getNullFact() { return m_nullFact; }

  private static Deftemplate s_uberTemplate = new Deftemplate("__fact", "Parent template");
  static Deftemplate getParentTemplate() { return s_uberTemplate; }

  /**
    Routers are kept in two hashtables: input ones and output ones.
    Names that are read-write are kept in both tables as separate entries.
    This means we don't need a special 'Router' class.

    Every input router is wrapped in a BufferedReader so we get reliable
    treatment of end-of-line. We need to keep track of the association, so
    we keep the original stream paired with the wrapper in m_inWrappers.

    Console-like streams act differently than file-like streams under
    read and readline , so when you cerate a router, you need to specify
    how it should act.
    */

  private transient Hashtable m_outRouters;
  private transient Hashtable m_inRouters;
  private transient Hashtable m_inWrappers;
  private transient Hashtable m_inModes;

  /**
   * @param s 
   * @param is 
   * @param consoleLike 
   */
  public void addInputRouter(String s, Reader is, boolean consoleLike)
  { 
    synchronized (m_inRouters)
      {
        synchronized (m_inWrappers)
          {
            Tokenizer t = (Tokenizer) m_inWrappers.get(is);
            if (t == null)
              t = new Tokenizer(is);
            
            m_inRouters.put(s, is);
            m_inWrappers.put(is, t);
            m_inModes.put(s, new Boolean(consoleLike));
          }
      }
  }

  /**
   * @param s 
   * @param os 
   */
  public void addOutputRouter(String s, Writer os)
  { m_outRouters.put(s, os); }

  /**
   * @param s 
   */
  public void removeInputRouter(String s)
  { m_inRouters.remove(s); }
  /**
   * @param s 
   */
  public void removeOutputRouter(String s)
  { m_outRouters.remove(s); }

  /**
   * @param s 
   * @return 
   */
  public Reader getInputRouter(String s)
  { return (Reader) m_inRouters.get(s);}

  /**
   * @param is 
   * @return 
   */
  Tokenizer getInputWrapper(Reader is)
  { return (Tokenizer) m_inWrappers.get(is);}

  /**
   * @param s 
   * @return 
   */
  public boolean getInputMode(String s)
  { return ((Boolean) m_inModes.get(s)).booleanValue();}

  /**
   * @param s 
   * @return 
   */
  public Writer getOutputRouter(String s)
  { return (Writer) m_outRouters.get(s);}
  
  /**
    The fact-list is unique to each interpreter.
    */
  
  transient private Vector m_facts;

  /**
    Facts generated by LHS actions, not asserted until RHS time.
    */
  
  transient private Vector m_factsToAssert, m_factsToRetract;

  void setPendingFact(Fact fact, boolean assertFact)
  {
    if (assertFact)
      m_factsToAssert.addElement(fact);

    else
      {
        m_factsToRetract.addElement(fact);
      }
  }

  // Process any facts that were asserted by rule LHS processing -- i.e.,
  // for backwards chaining.
  void processPendingFacts() throws ReteException
  {
    synchronized (m_factsToAssert) 
      {
        while (m_factsToAssert.size() > 0)
          {
            _assert((Fact) m_factsToAssert.elementAt(0));
            m_factsToAssert.removeElementAt(0);
          }
      }

    synchronized (m_factsToRetract) 
      {
        while (m_factsToRetract.size() > 0)
          {
            _retract((Fact) m_factsToRetract.elementAt(0));
            m_factsToRetract.removeElementAt(0);
          }
      }
  }

  /**
    The rule base is unique to each interpreter.
    */

  private Hashtable m_rules = new Hashtable();

  /**
    The agenda is unique to each interpreter.
    */

  private transient Vector m_activations;

  /**
    Each interpreter has its own compiler object
    */

  private ReteCompiler m_compiler = new ReteCompiler(this);

  /**
   * Fetch the ReteCompiler object used by the engine. You probabably shouldn't
   * use this for anything!
   *
   * @return the Compiler object
   */
  final ReteCompiler getCompiler() { return m_compiler; }

  /**
    Flag for (halt) function
    */

  transient private boolean m_halt;

  /**
   * Flag to allow duplicate facts
   */

  private boolean m_factDuplication = false;
  
  /**
     * Returns true if duplicate facts are allowed, false otherwise.
     * @return Value of factDuplication.
     */
  public boolean getFactDuplication() {return m_factDuplication;}
  
  /**
   * Turn fact-duplication on or off. 
   * @param v  Value to assign to factDuplication.
   */
  public void setFactDuplication(boolean  v) {m_factDuplication = v;}  

  /**
    Stuff to help us parse Jess code
    */

  private transient TextReader m_tis;
  private transient Jesp  m_jesp;


  /**
   * Stuff to help us communicate between Jess and Java
   * Things stored with store() are accessible from Jess with (fetch),
   * and vice-versa!
   */

  transient private Hashtable m_storage;

  /**
   * Store a value in the engine under a given name for later retrieval by fetch.
   * @ see Rete#fetch
   * @param name A key under which to file the value
   * @param val The value to store
   * @return Any old value stored under this name, or null.
   */

  public Value store(String name, Value val)
  {
    if (val == null)
      return (Value) m_storage.remove(name);
    else
      return (Value) m_storage.put(name, val);
  }

  /**
   * Store a value in the engine under a given name for later retrieval by fetch. The
   * Object is first wrapped in a new coc.agent.engine.Value object.
   * @ see Rete#fetch
   * @param name A key under which to file the value
   * @param val The value to store
   * @return Any old value stored under this name, or null.
   */

  public Value store(String name, Object val)
  { 
    if (val == null)
      {
        return (Value) m_storage.remove(name);
      }
    else
      return (Value) m_storage.put(name, new Value(val));
  }

  /**
   * Retrieve an object previously stored with store().
   * @see Rete#store
   * @param name The key under which to find an object
   * @return The object, or null if not found.
   */

  public Value fetch(String name)
  { return (Value)  m_storage.get(name); }

  /**
   * Clear the storage used by store() and fetch().
   */

  public void clearStorage() { m_storage.clear(); }
  

  private int m_time = 0;
  int getTime() { return m_time; }


  /*
    Factory stuff
  */
  private static Factory m_factory = new FactoryImpl();
  public static Factory getFactory() { return m_factory; }
  public static void setFactory(Factory f) { m_factory = f; }

  /**
    Constructors
    */

  /**
   * Now throws RuntimeException if any intrinsic functions fail to load.
   */
  public Rete()
  {
    this(null);
  }

  /**
   * Now throws RuntimeException if any intrinsic functions fail to load.
   * @param a If this Rete object is being created inside an applet,
   * pass it as an argument.
   */
  public Rete(Applet a)
  {
    m_applet = a;
    m_globalContext = new Context(this);

    initTransientMembers();

    try
      {
        m_strategy = (Strategy) Class.forName("coc.agent.engine.depth").newInstance();
        Funcall.loadIntrinsics(this);
      }
    catch (ReteException re)
      {
        StringWriter sw = new StringWriter();
        if (re.getNextException() != null)
          {
            sw.write(re.toString());                    
            sw.write("\n");
            sw.write("\nNested exception is:\n");    
            re.getNextException().printStackTrace(new PrintWriter(sw, true));
          }
        else
          re.printStackTrace(new PrintWriter(sw, true));
        
        throw new RuntimeException(sw.toString());
      }    
    catch (Exception e)
      {
        throw new RuntimeException(e.toString());
      }
  }
  
  private void initTransientMembers()
  {
    try
      {
        m_outRouters = new Hashtable(13);
        m_inRouters = new Hashtable(13);
        m_inWrappers = new Hashtable(13);
        m_inModes = new Hashtable(13);
        
        m_tis = new TextReader(true);
        m_jesp = new Jesp(m_tis, this);
        
        addInputRouter("t", new InputStreamReader(System.in), true);
        addOutputRouter("t", new PrintWriter(System.out, false));
        addInputRouter("WSTDIN", getInputRouter("t"), true);
        addOutputRouter("WSTDOUT", getOutputRouter("t"));
        addOutputRouter("WSTDERR", getOutputRouter("t"));
        
        m_facts = new Vector();
        m_factsToAssert = new Vector();
        m_factsToRetract = new Vector();
        m_activations = new Vector();
        m_storage = new Hashtable();
        
        addDeftemplate(s_uberTemplate);
        m_initialFact = new Fact("initial-fact", this);
        m_clearFact = new Fact("__clear", this);
        m_nullFact = new Fact("__not_or_test_CE", this);

        m_theEvent = new JessEvent(this, 0, null);
        setEventMask(0);

      }
    catch (ReteException re)
      {
        System.out.println("Rete.initTransientMembers: " + re);
        System.exit(-1);
      }
  }

  /**
   * @return 
   */
  public PrintWriter getErrStream()
  {
    synchronized (m_outRouters)
      {
        // Coerce to PrintWriter;
        PrintWriter ps;
        Writer os = getOutputRouter("WSTDERR");
        if (os instanceof PrintWriter)
          ps = (PrintWriter) os;
        else
          {
            ps = new PrintWriter(os);
            addOutputRouter("WSTDERR", ps);
          }
        return ps;
      }
  }

  /**
   * @return 
   */
  public PrintWriter getOutStream()
  {
    synchronized (m_outRouters)
      {
        // Coerce to PrintWriter;
        PrintWriter ps;
        Writer os = getOutputRouter("WSTDOUT");
        if (os instanceof PrintWriter)
          ps = (PrintWriter) os;
        else
          {
            ps = new PrintWriter(os);
            addOutputRouter("WSTDOUT", ps);
          }
        return ps;
      }
  }

  /**
   * Print all fact(assert/retract) information 
   */
  public PrintWriter getOutFACTStream()
  {
    synchronized (m_outRouters)
      {
        // Coerce to PrintWriter;
        PrintWriter ps;
        Writer os = getOutputRouter("WSTDOUT_FACT");
        if (os instanceof PrintWriter)
          ps = (PrintWriter) os;
        else
          {
            ps = new PrintWriter(os);
            addOutputRouter("WSTDOUT_FACT", ps);
          }
        return ps;
      }
  }
  
  /**
   * Reinitialize engine
   * Thanks to Karl Mueller for idea
   * @exception ReteException 
   */
  public synchronized void clear() throws ReteException
  {
    m_halt = false;
    m_nextFactId = m_nextRuleId = 0;
    m_deftemplates.clear();
    clearStorage();
    m_globalContext = new Context(this);

    m_facts.removeAllElements();
    m_factsToAssert.removeAllElements();
    m_factsToRetract.removeAllElements();
    m_rules.clear();
    m_activations.removeAllElements();

    // Undefine Deffunctions, but nothing else
    Hashtable temp = new Hashtable(100);

    for (Enumeration ee = m_functions.keys(); ee.hasMoreElements();)
      {
        Object o = ee.nextElement();
        Userfunction uf = ((FunctionHolder) m_functions.get(o)).getFunction();
        if (! (uf instanceof Deffunction))
          temp.put(o, new FunctionHolder(uf));
      }
    m_functions=temp;   

    m_compiler = new ReteCompiler(this);

    m_deffacts.clear();
    m_defglobals.clear();

    setEventMask(0);
    broadcastEvent(JessEvent.CLEAR, this);

    // Redo these to reload the deftemplates
    m_initialFact = new Fact("initial-fact", this);
    m_clearFact = new Fact("__clear", this);
//    System.gc();

  }

  /**
   * Reset the interpreter. Remove all facts, flush the network,
   * remove all activations.
   * @exception ReteException 
   */
  void removeFacts() throws ReteException
  {
    synchronized (m_compiler)
      {
        // remove all existing facts
        // This Token tag is a special command. All 1-input nodes
        // just pass it along; all two-input nodes clear both memories.
        

        m_clearFact.setFactId(0);
        processToken(RU.CLEAR, m_clearFact);
        m_facts.setSize(0);
        // System.gc();
      }
  }

  /**
   * Reset the Rete engine. Remove all facts, activations, etc. Clear all non-globals from
   * the global scope. Assert (initial-fact). Broadcasts a JessEvent of type RESET. 
   *
   * @exception ReteException  If anything goes wrong.
   */
  public void reset() throws ReteException 
  {
    
    synchronized (m_compiler)
      {
        IReteSession sess = getSession();
        if (sess != null)
        {
            sess.reset(this);
        }
        
        removeFacts();
        m_globalContext.removeNonGlobals();
        m_activations.removeAllElements();
        m_nextFactId = 0;
        m_time = 0;
        assertFact(m_initialFact);
        broadcastEvent(JessEvent.RESET, this);
        
        /*
        IReteSession sess = getSession();
        if (sess == null) // original
        {
            removeFacts();
            m_globalContext.removeNonGlobals();
            m_activations.removeAllElements();
            m_nextFactId = 0;
            m_time = 0;
            assert(m_initialFact);
            broadcastEvent(JessEvent.RESET, this);
        }
        else // changes for session
        {
            sess.reset(this);
            assert(m_initialFact);
        }
        */
      }
  }

  /**
   * Assert a fact, as a String
   * @param s 
   * @exception ReteException 
   * @return 
   */
  public int assertString(String s) throws ReteException 
  {
    StringReader sbis;
    try
      {
        synchronized (m_tis)
          {
            m_tis.clear();
            m_jesp.clear();
            m_tis.appendText(s);
            Fact f = m_jesp.parseFact();
            return assertFact(f);
          }
      }
    catch (Exception t)
      {
        throw new ReteException("Rete.assertString", s, t);
      }
  }
  
  /**
   * Clone the fact, expand any variable references in the clone, 
   * then call assert().
   * @exception ReteException If anything goes wrong.
   * @return The fact ID of the asserted fact, or -1.
   */

  int expandAndAssert(Fact f, Context context) throws ReteException
  {
    Fact fact = (Fact) f.clone();
    for (int j=0; j<fact.size(); j++) 
      {
        Value current = fact.get(j).resolveValue(context);
        if (current.type() == RU.LIST)
          {
            ValueVector vv = new ValueVector();
            ValueVector list = current.listValue(context);
            for (int k=0; k<list.size(); k++)
              {
                Value listItem = list.get(k).resolveValue(context);
                if (listItem.type() == RU.LIST)
                  {
                    ValueVector sublist = listItem.listValue(context);
                    for (int m=0; m<sublist.size(); m++)
                      vv.add(sublist.get(m).resolveValue(context));
                  }
                else
                  vv.add(listItem);
              }
            current = new Value(vv, RU.LIST);
          }
        fact.set(current, j);            
      }
    
    return assertFact(fact);
  }    

  int expandAndAssertp(Fact f, Context context) throws ReteException
  {
    Fact fact = (Fact) f.clone();
    for (int j=0; j<fact.size(); j++) 
      {
        Value current = fact.get(j).resolveValue(context);
        if (current.type() == RU.LIST)
          {
            ValueVector vv = new ValueVector();
            ValueVector list = current.listValue(context);
            for (int k=0; k<list.size(); k++)
              {
                Value listItem = list.get(k).resolveValue(context);
                if (listItem.type() == RU.LIST)
                  {
                    ValueVector sublist = listItem.listValue(context);
                    for (int m=0; m<sublist.size(); m++)
                      vv.add(sublist.get(m).resolveValue(context));
                  }
                else
                  vv.add(listItem);
              }
            current = new Value(vv, RU.LIST);
          }
        fact.set(current, j);            
      }
    
    return assertp(fact);
  }    


  /**
   * Assert a fact
   * @param f A Fact object. This fact becomes the property of Jess
   * after calling assert() -- don't change any of its fields until
   * the fact is retracted!
   *
   * @exception ReteException If anything goes wrong
   * @return The fact ID on success, or -1.
   */
  public int assertFact(Fact f) throws ReteException 
  {
    int i = _assert(f);
    if (i != -1)
      processPendingFacts();
    return i;
  }
  
  private Object m_activationSemaphore = "ACTIVATION LOCK";

  /**
   * The monitor of the object returned from this method will be signalled
   * whenever an activation appears. Thus a run-loop could wait on 
   * this monitor when idle.
   */

  public Object getActivationSemaphore() { return m_activationSemaphore; }

  /**
   * Waits on the activation lock until a rule is activated. Can be called
   * in a run-loop to wait for more rules to fire.
   * @see getAssertLock
   */

  public void waitForActivations()
  {
    try
      {
        synchronized (m_activationSemaphore) { m_activationSemaphore.wait(); }
      }
    catch (InterruptedException ie) { /* FALL THROUGH */ }
  }

  private int _assert(Fact f) throws ReteException
  {
    synchronized (m_compiler)
      {
        // find any old copy
        if (doPreAssertionProcessing(f) != 0)
          return -1;
        
        // insert the new fact
        f.setFactId(nextFactId());
        broadcastEvent(JessEvent.FACT, f);        
        
        ++m_time;
        f.updateTime(this);
        m_facts.addElement(f);
        
        // Send it to the Rete network
        processToken(RU.ADD, f);
        
        /**
         * added code below for mutilple sessions
         */
        IReteSession sess = getSession();
        if (sess != null)
        {
            //sess.getFrame().pushf(f);
			sess.pushf(f);
        }
        /**
         * added code ends
         */
        return f.getFactId();
      }
  }
  
  /**
   * assert protected facts
   */
  public int assertp(Fact f) throws ReteException 
  {
    int i = _assertp(f);
    if (i != -1)
      processPendingFacts();
    return i;
  }

  private int _assertp(Fact f) throws ReteException
  {
    synchronized (m_compiler)
      {
        // find any old copy
        if (doPreAssertionProcessing(f) != 0)
          return -1;
        
        // insert the new fact
        f.setFactId(nextFactId());
        broadcastEvent(JessEvent.FACT, f);        
        
        ++m_time;
        f.updateTime(this);
        m_facts.addElement(f);
        
        // Send it to the Rete network
        processToken(RU.ADD, f);
        
        /**
         * added code below for mutilple sessions
         */
        IReteSession sess = getSession();
        if (sess != null)
        {
            //sess.getFrame().pushpf(f);
            sess.pushpf(f);
        }
        /**
         * added code ends
         */
        return f.getFactId();
      }
  }

  public int doPreAssertionProcessing(Fact f) throws ReteException
  {
    if (!m_factDuplication)
      {
        Fact of  = findFact(f);
        
        if (of != null)
          {
            return -1;
          }            
      }
    return 0;
  }
  

  /**
   * Karl Mueller NASA/GSFC Code 522.2 
   * (Karl.R.Mueller@gsfc.nasa.gov)
   * 27.January.1998
   * 
   * Retract a fact as a string
   * @param s 
   * @exception ReteException 
   */
  public Fact retractString(String s) throws ReteException 
  {
    try
      {
        synchronized (m_tis)
          {
            m_tis.clear();
            m_jesp.clear();
            m_tis.appendText(s);
            Fact f = m_jesp.parseFact();
            return retract(f);
          }
      }
    catch (Exception t)
      {
        throw new ReteException("Rete.retractString", s, t);
      }
  }
  

  /**
   * Retract a fact. 
   * @param f A Fact object. Doesn't need to be the actual object that appears on
   * the fact-list; can just be a Fact that could compare equal to one.
   * @exception ReteException If anything goes wrong.
   */
  public Fact retract(Fact f) throws ReteException 
  {
    synchronized (m_compiler)
      {
        synchronized (m_facts)
          {
            int idx;
            if ((idx = findFactIdx(f)) != -1) 
              {
                Fact ff = _retract(idx);
                processPendingFacts();
                return ff;
              }
            else
              return null;
          }
      }
  }
  

  /**
   * Retract a fact by ID, used by rule RHSs.
   * @param id The fact-id of a fact
   * @exception ReteException If anything goes wrong.
   */
  public Fact retract(int id) throws ReteException 
  {
    synchronized (m_compiler)
      {
        synchronized (m_facts)
          {
            int idx;
            if ((idx = findFactIdxByID(id)) != -1)
              {
                Fact ff = _retract(idx);
                processPendingFacts();
                return ff;
              }
            else
              return null;
          }
      }
  }

  /**
   * Retract a fact by index within fact-list. Better be calling this
   * from within a block locked on m_facts!
   * @param idx The physical index in m_facts to retract
   * @exception ReteException If anything goes wrong
   */
  private Fact _retract(int idx) throws ReteException
  {
    synchronized (m_compiler)
      {
        synchronized (m_facts)
          {
            
            Fact f = (Fact) m_facts.elementAt(idx);          
            broadcastEvent(JessEvent.FACT | JessEvent.REMOVED, f);
            m_facts.removeElementAt(idx);          
            
            ++m_time;
            f.updateTime(this);
            
            processToken(RU.REMOVE, f); 
            /**
             * changes for multiple sessions
             */
            IReteSession sess = getSession();
            //if (sess != null) sess.getFrame().retract(f);
            if (sess != null) sess.retract(f);
            //System.out.println("retract: "+f.toStringWithParens());    
            return f;
            
          }        
      }
  }

  /**
   * Like the public retract(Fact), but doesn't call processPendingFacts()
   */
  private Fact _retract(Fact f) throws ReteException 
  {
    synchronized (m_compiler)
      {
        synchronized (m_facts)
          {
            int idx;
            if ((idx = findFactIdx(f)) != -1) 
              {
                return _retract(idx);
              }
            else
              return null;
          }
      }
  }


  
  private RU.Fetch fetchFactID =
    new RU.Fetch() { public int fetch(Object o) { return ((Fact) o).getFactId(); }};
  
  /**
   * This 'find' is used by the retract that rules use. Consider the returned Fact
   * to be READ-ONLY!
   * @param id The fact-id
   * @exception ReteException If something goes wrong
   * @return The fact, or null if none
   */

  public Fact findFactByID(int id) throws ReteException 
  {
    synchronized (m_facts)
      {
        int idx = findFactIdxByID(id);
        if (idx != -1)
          return (Fact) m_facts.elementAt(idx);
        else
          return null;
      }
  }

  /**
   * Always call these and use the result from within a block synchronized
   * on m_facts!
   */
  private int findFactIdxByID(int id) throws ReteException 
  {
    synchronized (m_facts)
      {        
        int size = m_facts.size();
        if (size > 500)
          {
            int idx = 
              RU.bsearchVector(id, m_facts, 0, size, fetchFactID, RU.compareGTE);
            if (idx != -1)
              {
                Fact f = (Fact) m_facts.elementAt(idx);
                if (f.getFactId() == id)
                  return idx;
              }
          }
        else
          {
            
            for (int idx=0; idx<size; idx++)
              {
                Fact f =  (Fact) m_facts.elementAt(idx);
                if (f.getFactId() == id)
                  return idx;
              }
          }
        return -1;
      }
  }

  /**
   * Does a given fact exist? (We're looking for identical
   * data, but the ID can differ)
   */
  private Fact findFact(Fact f) throws ReteException 
  {
    synchronized(m_facts)
      {
        int idx = findFactIdx(f);
        if (idx != -1)
          return (Fact) m_facts.elementAt(idx);
        else
          return null;
      }
  }

  /**
   * Use this if the fact doesn't have a valid ID
   */
   
  private int findFactIdx(Fact f) throws ReteException 
  {
    int size = m_facts.size();
    int fsize = f.size();
  outer_loop:
    for (int i=0; i < size; i++) 
      {
        Fact tf = (Fact) m_facts.elementAt(i);
        if (fsize != tf.size())
          continue;
        if (!f.getName().equals(tf.getName()))
          continue;
        for (int j=0; j < fsize; j++) 
          {
            if (!f.get(j).equals(tf.get(j)))
              continue outer_loop;
          }
        return i;
      }
    return -1;
  }

  /**
   * Return the pretty print forms of all facts, as a big string
   * @param name 
   * @return 
   */
  public String ppFacts(String name) 
  {
    StringBuffer sb = new StringBuffer(m_facts.size() * 60);
    for (int i=0; i<m_facts.size(); i++) 
      {
        Fact f = (Fact) m_facts.elementAt(i);
        if (!f.getName().equals(name))
          continue;
        sb.append(f.toList().toStringBuffer());
        sb.append("\n");
      }
    return sb.toString();
  }

  /**
   * @exception ReteException 
   * @return 
   */
  public String ppFacts()
  {
    StringBuffer sb = new StringBuffer(m_facts.size() * 60);
    for (int i=0; i<m_facts.size(); i++) 
      {
        Fact f = (Fact) m_facts.elementAt(i);
        sb.append(f.toList().toStringBuffer());
        sb.append("\n");
      }
    return sb.toString();
  }

  /**
   * Return an Enumeration of all the deffacts in this engine.
   */
  public Enumeration listDeffacts()
  { return m_deffacts.elements(); }

  /**
   * Return an Enumeration of all the deftemplates in this engine, both explicit and
   * implied.
   */
  public Enumeration listDeftemplates()
  { return m_deftemplates.elements(); }

  /**
   * Return an Enumeration of all the defrules in this engine.
   */
  public Enumeration listDefrules()
  { return m_rules.elements(); }

  /**
   * Return an Enumeration of all the facts currently on the fact-list
   */
  public Enumeration listFacts()
  { return m_facts.elements(); }

  /**
   * Return an Enumeration of all the activiations - i.e, the agenda.
   */
  public Enumeration listActivations()
  { return m_activations.elements(); }

  /**
   * Return an Enumeration of all the defglobals in this engine.
   */
  public Enumeration listDefglobals()
  { return m_defglobals.elements(); }

  /**
   * Return an Enumeration of all the functions in this engine: built-in, user, and 
   * deffunctions.
   */
  public Enumeration listFunctions()
  { 
    // Strip advice and FunctionHolders here.
    Vector v = new Vector();
    for (Enumeration e = m_functions.keys(); e.hasMoreElements();)
      v.addElement(findUserfunction((String) e.nextElement()));

    return v.elements();
  }
  
  /**
   * Process a Token which represents a fact being added or removed.
   * Eventually we should set this up so that the token gets dispatched
   * to only one root of the pattern net - right now it presented to all of
   * them!
   */
  private boolean processTokenOneNode(Token t, Node n) throws ReteException
  {
    synchronized (m_compiler)
      {
        return n.callNodeRight(t);
      }
  }
       
  private boolean processToken(int tag, Fact fact) throws ReteException 
  {
    boolean result = false;
    synchronized (m_compiler)
      {
        Vector v = m_compiler.roots();
        
        // make sure the network is optimized
        m_compiler.freeze();
        int size = v.size();    
        Token t = Rete.getFactory().newToken(fact, tag);
        for (int i=0; i<size; i++)
          {
            Node1 n = (Node1) v.elementAt(i);            
            if (processTokenOneNode(t, n))
              result = true;
          }
        return result;
      }
  }

  /**
   * Present all the facts on the agenda to a single Node.
   * @param n 
   * @exception ReteException 
   */
  void updateNodes(Hashtable n) throws ReteException 
  {    

    if (m_facts.size() == 0)
      return;

    m_compiler.freeze();
    for (Enumeration e = m_facts.elements(); e.hasMoreElements();)
      {
        Fact fact = (Fact) e.nextElement();
        Token t = Rete.getFactory().newToken(fact, RU.UPDATE);
        for (Enumeration nodes = n.elements(); nodes.hasMoreElements();)
          processTokenOneNode(t, (Node) nodes.nextElement());
      }
    processPendingFacts();
  }

  /**
   * Find a defrule object with a certain name
   * @param name 
   * @return 
   */
  public final HasLHS findDefrule(String name) 
  {
    return (HasLHS) m_rules.get(name);
  }

  /**
   * Find a deftemplate object with a certain name
   * @param name 
   * @return 
   */
  public Deftemplate findDeftemplate(String name) 
  {
    return ((Deftemplate) m_deftemplates.get(name));
  }

  /**
   * find the deftemplate, if there is one, or create implied dt.
   * @param name 
   * @exception ReteException 
   * @return 
   */
  Deftemplate createDeftemplate(String name)
       throws ReteException 
  {

    Deftemplate deft = findDeftemplate(name);
    if (deft == null)
      {
        // this is OK. Create an implied deftemplate
        deft = addDeftemplate(new Deftemplate(name, "(Implied)"));
        deft.addMultiSlot(RU.DEFAULT_SLOT_NAME, Funcall.NILLIST);
        
      }
    return deft;
  }



  /**
   * Creates a new deftemplate in this object. 
   * Ensure that every deftemplate has a unique class name; silently
   * ignore redefinitions!
   * @param dt 
   * @exception ReteException 
   * @return 
   */
  public Deftemplate addDeftemplate(Deftemplate dt)
       throws ReteException 
  {
    synchronized (m_deftemplates)
      {
        String name = dt.getName();
        if (m_deftemplates.get(name) == null) 
          {
            broadcastEvent(JessEvent.DEFTEMPLATE, dt);

            m_deftemplates.put(name, dt);
          }
        return dt;
      }
  }

  /**
   * Creates a new deffacts in this object
   * @param df 
   * @exception ReteException 
   * @return 
   */
  public Deffacts addDeffacts(Deffacts df) throws ReteException 
  {    
    broadcastEvent(JessEvent.DEFFACTS, df);

    Deffacts old = (Deffacts) m_deffacts.put(df.getName(), df);
    if (old != null)
      removeJessListener(old);
    addJessListener(df);
    return df;
  }

  /**
   * Creates a new Defglobal in this object. Trick it into resetting 
   * right now, regardless of the setting of resetGlobals.
   * @param dg 
   * @exception ReteException 
   * @return 
   */
  public Defglobal addDefglobal(Defglobal dg) throws ReteException 
  {
    broadcastEvent(JessEvent.DEFGLOBAL, dg);

    Defglobal old = (Defglobal) m_defglobals.put(dg.getName(), dg);
    if (old != null)
      removeJessListener(old);
    
    boolean oldReset = m_resetGlobals;
    try
      {
        m_resetGlobals = true;
        dg.eventHappened(new JessEvent(this, JessEvent.RESET, dg));
      }
    finally
      {
        m_resetGlobals = oldReset;
      }

    addJessListener(dg);
    return dg;
  }

  /**
   * @param name 
   * @return 
   */
  public Defglobal findDefglobal(String name)
  {
    return (Defglobal) m_defglobals.get(name);
  }

  /**
   * Creates a new function in this object
   * Will happily destroy an old one.
   * @param uf A new USerfunction
   * @return The parameter, or null if call rejected by event handler
   */
  public Userfunction addUserfunction(Userfunction uf)
  {
    try
      {
        broadcastEvent(JessEvent.USERFUNCTION, uf);
      }
    catch (ReteException je)
      {
        return null;
      }
    
    FunctionHolder fh;
    if ((fh = (FunctionHolder) m_functions.get(uf.getName())) != null)
      fh.setFunction(uf);
    else
      fh = new FunctionHolder(uf);
    m_functions.put(uf.getName(), fh);
    return uf;
  }

  /**
   * Add a Userpackage to this engine. A package generally calls addUserfunction
   * lots of times. 
   * @param up The package object
   * @return The package object, or null if call rejected by event handler
   */
  public Userpackage addUserpackage(Userpackage up)
  {
    try
      {
        broadcastEvent(JessEvent.USERPACKAGE, up);
      }
    catch (ReteException je)
      {
        return null;
      }

    up.add(this);
    return up;
  }

  /**
   * Find a userfunction, if there is one.
   * @param name The name of the function
   * @return The Userfunction object, if there is one.
   */
  public final Userfunction findUserfunction(String name) 
  {
    FunctionHolder fh = (FunctionHolder) m_functions.get(name);
    if (fh != null)
      {
        Userfunction f = fh.getFunction();
        return f;
      }
    else
      return null;
  }

  /**
   * Find a userfunction, if there is one.
   * @param name The name of the function
   * @return The Userfunction object, if there is one.
   */
  final FunctionHolder findFunctionHolder(String name) 
  {
    return (FunctionHolder) m_functions.get(name);
  }

  /**
   * Creates a new defrule in this object
   * @param dr 
   * @exception ReteException 
   * @return 
   */
  public final HasLHS addDefrule(HasLHS dr) throws ReteException 
  {
    synchronized (m_compiler)
      {
        unDefrule(dr.getName());
        
        m_compiler.addRule(dr);
        m_rules.put(dr.getName(), dr);
        broadcastEvent(JessEvent.DEFRULE, dr);


        return dr;
      }
  }

  /**
   * @param name 
   * @exception ReteException 
   * @return 
   */
  public final Value unDefrule(String name) throws ReteException 
  {
    synchronized (m_compiler)
      {
        HasLHS odr = findDefrule(name);
        if (odr != null)
          {
            broadcastEvent(JessEvent.DEFRULE | JessEvent.REMOVED, odr);

            odr.remove(m_compiler.roots());
            m_rules.remove(name);
            if (odr instanceof Defrule)
              for (Enumeration e=m_activations.elements(); e.hasMoreElements();)
                {
                  Activation a = (Activation) e.nextElement();
                  if (a.getRule() == odr)
                    {
                      removeActivation(a, false);
                    }
                }
            
            return Funcall.TRUE;
          }
      }

    return Funcall.FALSE;
  }

  /**
   * Info about a rule to fire.
   * @param a 
   * @exception ReteException 
   */
  void addActivation(Activation a) throws ReteException 
  {

    broadcastEvent(JessEvent.ACTIVATION, a);

    if (m_evalSalience != Rete.INSTALL)
      a.getRule().evalSalience();

    m_strategy.addActivation(a, m_activations);
    synchronized (m_activationSemaphore) { m_activationSemaphore.notify();}
  }

  /**
   * @param s 
   * @exception ReteException 
   * @return 
   */
  public String setStrategy(Strategy s) throws ReteException
  {
    synchronized (m_activations)
      {
        String rv = m_strategy.getName();
        m_strategy = s;
        Vector v = m_activations;
        m_activations = new Vector();
        for (Enumeration e = v.elements(); e.hasMoreElements();)
          {
            Activation a = (Activation) e.nextElement(); 
            if (!a.isInactive())
              addActivation(a);
          }
        return rv;
      }
  }

  /**
   * An activation has been cancelled or fired; forget it
   * @param a 
   */

  void removeActivation(Activation a, boolean fired)  throws ReteException
  {
    a.setInactive();
    if (!fired)
      broadcastEvent(JessEvent.ACTIVATION | JessEvent.REMOVED, a);
  }

  /**
   * Return a string describing a list of facts
   * @param t 
   * @exception ReteException 
   * @return 
   */
  static String factList(Token t) throws ReteException 
  {    
    StringBuffer sb = new StringBuffer(100);
    boolean first = true;
    for (int i=0; i<t.size(); i++) 
      {
        if (!first)
          sb.append(",");
        int id = t.fact(i).getFactId();
        if (id != -1)
          {
            sb.append(" f-");
            sb.append(id);
          }
        first = false;
      }
    return sb.toString();
  }

  /**
   * Run the actual engine.
   * @exception ReteException 
   * @return 
   */
  public int run() throws ReteException
  {
    int i=0, j;
    do
      {
        j = run(Integer.MAX_VALUE);
        i += j;
      }
    while (j > 0 && !m_halt);
    return i;
  }

  protected void aboutToFire(Activation a) {}
  protected void justFired(Activation a) {}

  /**
   * @param max 
   * @exception ReteException 
   * @return 
   */
  public synchronized int run(int max) throws ReteException 
  {
    int n = 0;
    int size = 0;
    m_halt = false;
    
    while (m_activations.size() > 0 && !m_halt && n < max) 
      {        
        Thread.yield();
        
        if (m_activations.size() > 0)
          {
            Activation a = null;

            synchronized (m_activations) 
              {
                a = (Activation) m_activations.elementAt(0);              
                m_activations.removeElementAt(0);
              }
            
            if (!a.isInactive())
              {
                a.setSequenceNumber(++n);
                broadcastEvent(JessEvent.DEFRULE_FIRED, a);
                try
                  {
                    aboutToFire(a);
                    a.fire();
                  }
                finally
                  {
                    justFired(a);
                  }
              }
          }

        if (m_evalSalience == EVERY_TIME)
          {
            synchronized (m_activations)
              {
                Vector v = m_activations;
                m_activations = new Vector();

                for (Enumeration e = v.elements(); e.hasMoreElements();)
                  addActivation((Activation) e.nextElement());
              }
          }
      }
    return n;
  }
  
  /**
   * Run until halt() is called. When no rules are active, the calling Thread
   * will be waiting on the activation semaphore.
   */

  public int runUntilHalt() throws ReteException
  {
    int count = 0;
    while (!m_halt)
      {
        count += run();
        if (m_halt)
          break;
        waitForActivations();
      }
    return count;
  }


  /**
   * Stuff to let Java code call functions inside of us.
   * @param cmd 
   * @exception ReteException 
   * @return 
   */
  public Value executeCommand(String cmd) throws ReteException 
  {
  /**
   * TODO
   * changed: 'synchronized' is removed. Don't know if there is any potential problem.
   */
      synchronized (m_tis)
      {
        m_tis.clear();
        m_jesp.clear();
        m_tis.appendText(cmd);
        return m_jesp.parse(false);
      }
  }

  /**
    Jane, stop this crazy thing!
    */

  public void halt() 
  {
    m_halt = true;
    synchronized (m_activationSemaphore) { m_activationSemaphore.notify(); }
  }

  private void readObject(ObjectInputStream stream)
    throws IOException, ClassNotFoundException
  
  {  
    stream.defaultReadObject();
    initTransientMembers();
  }

  private Vector m_listeners = new Vector();

  public Enumeration listJessListeners()
  {
    return m_listeners.elements();
  }

  /**
   * @param jel 
   */
  public void addJessListener(JessListener jel)
  {
    m_listeners.addElement(jel);
  }

  /**
   * @param jel 
   */

  public void removeJessListener(JessListener jel)
  {
    m_listeners.removeElement(jel);
  }

  private transient JessEvent m_theEvent;

  private int m_eventMask = JessEvent.RESET + JessEvent.CLEAR;
  public int getEventMask() { return m_eventMask; }
  public void setEventMask(int i)
  {
    m_eventMask = i  | JessEvent.RESET | JessEvent.CLEAR;
  }


  final void broadcastEvent(int type, Object data) throws ReteException
  {

    // only broadcast active events
    if ((type & m_eventMask) == 0)
      return;

    // We lock this for two reasons. One, it's cheaper than going in and out of
    // the vector methods over and over. Two, it prevents any other thread from
    // messing up our count and triggering an ArrayIndexOutOfBounds exception. Note
    // that we must call size() each time since a handler may remove a listener.
    synchronized (m_listeners)
      {
        if (m_listeners.size() == 0)
          return;
                
        for (int i=0; i<m_listeners.size(); i++)
          {
            try
              {
                m_theEvent.reset(type, data);
                ((JessListener) m_listeners.elementAt(i)).eventHappened(m_theEvent);
              }
            catch (ReteException je)
              {
                throw je;
              }
            catch (Exception e)
              {
                throw new ReteException("Rete.broadcastEvent",
                                        "Event handler threw an exception",
                                        e);                
              }
          }
      }
  }
  
  IReteSession session = null;
  
  public void setSession(IReteSession session) {
      this.session = session;
  }
  
  public IReteSession getSession() {
      return session;
  }
  
  public Hashtable getm_rule(){
	  return this.m_rules;
  }
}
