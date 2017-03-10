package coc.agent.engine;

import java.util.*;
import java.io.*;

/**
 * Parent class of Defrules and Defqueries.
 * <P>
 */
public abstract class HasLHS extends Node implements Serializable
{
  Rete m_engine;
  String m_name;
  String m_docstring = "";
  private Vector m_nodes = new Vector();
  private Hashtable m_bindings = new Hashtable();
  Vector m_patts;
  int m_nodeIndexHash = 0;
  transient private StringBuffer m_compilationTrace;
  boolean m_new = true;
  boolean m_frozen = false;

  HasLHS(String name, String docstring, Rete engine) throws ReteException
  {
    m_engine = engine;
    m_name = name;
    m_docstring = docstring;
    m_patts = new Vector();

    if (s_initialFact == null)
      s_initialFact = new Pattern("initial-fact", engine, 0);  
  }

  void freeze() throws ReteException
  {
    // No patterns for this construct; we will fire on "initial-fact".
    if (m_patts.size() == 0) 
      addPattern(s_initialFact);
    m_frozen = true;
  }

  void insertPatternAt(Pattern pattern, int index) throws ReteException
  {
    Vector patterns = m_patts;
    m_bindings = new Hashtable();    
    m_patts = new Vector();
    patterns.insertElementAt(pattern, index);
    for (int i=0; i<patterns.size(); i++)
      addPattern((Pattern) patterns.elementAt(i));
  }

  /**
   * Add a pattern to this construct
   */
  void addPattern(Pattern pattern) throws ReteException 
  {
    Deftemplate dt = pattern.getDeftemplate();
    Hashtable substitutes = new Hashtable();
    String varname = pattern.getBoundName();

    if (m_patts.size() == 0 && (pattern.getNegated() != 0 ||
                                pattern.getName().equals("test") ||
                                pattern.getDeftemplate().getBackwardChaining()))
      addPattern(s_initialFact);
    
    if (pattern.getName().startsWith(RU.BACKCHAIN_PREFIX))
      {
        if (varname == null)
          varname = RU.gensym("__factidx");
      }

    if (varname != null)
      {
        addBinding(varname, m_patts.size(), RU.PATTERN, -1, RU.FACT_ID);
        pattern.setBoundName(varname);
      }

    m_patts.addElement(pattern);

    // **********************************************************************
    // Look for variables in the fact, and create bindings for new ones
    // We will also rename variables introduced in (not) CEs to have unique names
    // across the construct
    // **********************************************************************

    for (int i=0; i< pattern.getNSlots(); i++)
      {
        if (pattern.getNTests(i) == 0)
          continue;
        for (int j=0; j< pattern.getNTests(i); j++) 
          {
            Test1 test = pattern.getTest(i, j);
            Value val = test.m_slotValue;
            boolean eq = (test.m_test == Test1.EQ);
            if (val instanceof Variable)
              {
                int type = dt.getSlotDataType(i);
                String name = val.variableValue(null);
                if (m_bindings.get(name) == null)
                  {
                    // Test for defglobal
                    if (name.startsWith("*"))
                      ; 

                    else if (j != 0 && test.getMultislotIndex() == -1)
                      throw new ReteException("HasLHS.addPattern",
                                              "Variable referenced before definition:",
                                              val.toString());
                    
                    else if (pattern.getNegated() != 0 &&
                             !name.startsWith(Tokenizer.BLANK_PREFIX))

                      // "Disguise" this variable name; include patt #
                      // so patterns are independent
                      {
                        String oldName = name;
                        name = "_" + m_patts.size() + "_" + name;
                        test.m_slotValue = new Variable(name, val.type());
                        substitutes.put(oldName, name);
                      }

                    else if (!eq) 
                      {
                        throw new ReteException("HasLHS.addPattern",
                                                "First use of variable negated",
                                                name);
                      }

                    m_bindings.put(name, new Binding(name, m_patts.size() - 1,
                                                     i, test.m_subIdx,
                                                     type)); 

                  }
              }
          }
      }

    // Now go back and rename any variables that need renaming
    Context gc = m_engine.getGlobalContext();
    for (int i=0; i< pattern.getNSlots(); i++)
      {
        if (pattern.getNTests(i) == 0)
          continue;
        for (int j=0; j< pattern.getNTests(i); j++) 
          {
            Value val = pattern.getTest(i, j).m_slotValue;
            if (val.type() == RU.FUNCALL)
              substFuncall(val.funcallValue(gc), substitutes, gc);
          }
      }


    // **********************************************************************
    // Now handle '|' conjunctions by transforming tests into (or) Funcalls
    // **********************************************************************
    for (int i=0; i< pattern.getNSlots(); i++)
      {
        int nTests = pattern.getNTests(i);

        if (nTests == 0)
          continue;
                                  
        // Rearrange the tests
        Vector tests = new Vector();
            
        int currentSubIndex = pattern.getTest(i, 0).m_subIdx;
        int doneIdx = 0;

        // This is a loop over sub-indexes in the test array. doneIdx will be
        // incremented inside the loop, and some constructs will break out to this
        // label.
      subIdxLoop:
        while (doneIdx < nTests)
          {
            // Find out if there are any ORs on this subslot
            boolean hasOrs = false;
            for (int j=doneIdx; j< nTests; j++) 
              {
                Test1 aTest = pattern.getTest(i, j);
                if (aTest.m_subIdx != currentSubIndex)
                  break;
                else if (aTest.m_conjunction == RU.OR)
                  {
                    hasOrs = true;
                    break;
                  }
              }
            
            // If no ORs on this subslot, just copy tests into Vector
            if (!hasOrs)
              {                
                Test1 aTest = null;
                for (int j=doneIdx; j< nTests; j++) 
                  {
                    aTest = pattern.getTest(i, j);
                    if (aTest.m_subIdx != currentSubIndex)
                      {
                        currentSubIndex = aTest.m_subIdx;
                        continue subIdxLoop;
                      }
                    else
                      {
                        tests.addElement(aTest);
                        ++doneIdx;                        
                      }
                  }                    
                continue subIdxLoop;
              }
            
            
            // First find a variable to represent this (sub)slot; we may have to
            // create one
            Value var;
            Test1 firstTest = pattern.getTest(i, doneIdx);
            
            if (firstTest.m_slotValue.type() == RU.VARIABLE)
              {
                var = firstTest.m_slotValue;
                ++doneIdx;
              }
            else
              {
                String name = RU.gensym(Tokenizer.BLANK_PREFIX);
                var = new Variable(name, RU.VARIABLE);
                m_bindings.put(name, new Binding(name, m_patts.size() - 1,
                                                 i, currentSubIndex,
                                                 dt.getSlotDataType(i)));                 
              }
            
            tests.addElement(new Test1(Test.EQ, currentSubIndex, var));
            
            // We're going to build up this function call
            Funcall or = new Funcall("or", m_engine);
            
            // Count how many tests until an OR, so we can omit the AND if not needed
            while (true)
              {
                int andCount=1;
                for (int j=doneIdx+1; j<nTests; j++)
                  {
                    Test1 aTest = pattern.getTest(i, j);
                    if (aTest.m_conjunction == RU.OR || aTest.m_subIdx != currentSubIndex)
                      break;
                    else 
                      ++andCount;
                  }
                
                if (andCount == 1)
                  {
                    or.add(testToFuncall(pattern.getTest(i, doneIdx), var));
                  }
                else
                  {
                    Funcall and = new Funcall("and", m_engine);
                    for (int j=doneIdx; j < doneIdx+andCount; j++)
                      and.add(testToFuncall(pattern.getTest(i, j), var));
                    or.add(new FuncallValue(and));
                  }
                
                doneIdx += andCount;

                if (doneIdx == nTests)
                  break;
                else if (pattern.getTest(i, doneIdx).m_subIdx != currentSubIndex)
                  {
                    break;
                  }
              }
            tests.addElement(new Test1(Test.EQ, currentSubIndex, new FuncallValue(or)));

            if (doneIdx < nTests && pattern.getTest(i, doneIdx).m_subIdx != currentSubIndex)
              currentSubIndex = pattern.getTest(i, doneIdx).m_subIdx;

          }

        Test1[] testArray = new Test1[tests.size()];
        for (int j=0; j<testArray.length; j++)
          testArray[j] = (Test1) tests.elementAt(j);
        pattern.replaceTests(i, testArray);
      }


    // **********************************************************************
    // Add a 'not' so we don't fire if the fact exists during backwards chaining
    // **********************************************************************
    if (pattern.getName().startsWith(RU.BACKCHAIN_PREFIX))
      {
        addPattern(new Pattern(pattern,
                               pattern.getName().substring(RU.BACKCHAIN_PREFIX.length()), 1));

        
      }          
  }

  private void substFuncall(Funcall f, Hashtable substitutes, Context context)
    throws ReteException
  {
    for (int i=1; i<f.size(); i++)
      {      
        Value current = f.get(i);
        if (current instanceof Variable)
          {
            String s = (String) substitutes.get(current.variableValue(context));
            if (s != null)
              f.set(new Variable(s, current.type()), i);
          }
        else if (current instanceof FuncallValue)
          substFuncall(current.funcallValue(context), substitutes, context);
      }
  }


  /**
   *  Given a test, create an implied function call that does it
   */
  private Value testToFuncall(Test1 t, Value var) throws ReteException
  {
    Value v = t.m_slotValue;
    switch (t.m_slotValue.type())
      {
      case RU.FUNCALL:
        {
          if (t.m_test == Test.NEQ)
            return new FuncallValue(new Funcall("not", m_engine).arg(v));
          else
            return v;
        }
      default:        
        return new FuncallValue(new Funcall(t.m_test==Test.EQ ? "eq" : "neq", m_engine).arg(v).arg(var));
      }
  }

  /**
   * Fetch the number of patterns on the LHS of this construct.
   * @return The number of patterns
   */
  public int getNPatterns() { return m_patts.size(); }
  /**
   * Return the idx-th Pattern on this construct's LHS.
   * @param idx The zero-based index of the desired pattern
   * @return the pattern
   */
  public Pattern getPattern(int idx) { return (Pattern) m_patts.elementAt(idx); }

  /**
   * @param name 
   * @param factidx 
   * @param slotidx 
   * @param subidx 
   */
  private void addBinding(String name, int factidx, int slotidx, int subidx, int type) 
  {
    m_bindings.put(name, new Binding(name, factidx, slotidx, subidx, type));
  }

  /**
   * @return 
   */
  Hashtable getBindings() { return m_bindings; }

  /**
   * Return a string (useful for debugging) describing all the Rete network
   * nodes connected to this construct.
   * @return A textual description of all the nodes used by this construct
   */

  public String listNodes()
  {
    StringBuffer sb = new StringBuffer(100);
    for (int i=0; i< m_nodes.size(); i++)
      {
        sb.append(m_nodes.elementAt(i));
        sb.append("\n");
      }
    return sb.toString();
  }

  Vector getNodes() { return m_nodes; }
  
  void addNode(Node n) throws ReteException
  {
    if (n == null)
      new ReteException("HasLHS.addNode", "Compiler fault", "null Node added");
    ++n.m_usecount;
    m_nodes.addElement(n);
  }


  /**
   * Completely remove this construct from the Rete network, including
   * removing any internal nodes that are only used by this construct.
   * @param roots The roots of the Rete network where this construct lives.
   */
  
  void remove(Vector roots) throws ReteException
  {
    Enumeration e = m_nodes.elements();
    while (e.hasMoreElements())
      {
        Node s = (Node) e.nextElement();
        if (--s.m_usecount <= 0)
          {
            roots.removeElement(s);
            Enumeration e2 = m_nodes.elements();
            while (e2.hasMoreElements())
              {
                Node n = (Node) e2.nextElement();
                n.removeSuccessor(s);
              }
          }
      }
    // Straighten out the nodes we disturbed.
    e = m_nodes.elements();
    while (e.hasMoreElements())
      {
        Node s = (Node) e.nextElement();
        s.freeze();
      }
    m_nodes.removeAllElements();
  }  

  void appendCompilationTrace(String s)
  {
    
    if (m_compilationTrace == null)
      m_compilationTrace = new StringBuffer();
    m_compilationTrace.append(s);
  }

  StringBuffer getCompilationTrace() { return m_compilationTrace; }

  private static Pattern s_initialFact;

  /**
   * Set the node-index-hash of this construct. The node-index-hash value effects the
   * indexing efficiency of the join nodes for this construct. Larger values will make
   * constructs with many partial matches faster (to a point). Must be set before construct is
   * added to engine (so is typically set during parsing via the equivalent
   * Jess command.
   * @param h The node index hash value
   */

  public void setNodeIndexHash(int h) { m_nodeIndexHash = h; }


  /**
   * Get the node-index-hash setting of this construct.
   * @return The node-index-hash of this construct
   */
  public int getNodeIndexHash() { return m_nodeIndexHash; }

  /**
   * Fetch the name of this construct
   * @return The name of this construct
   */
  public final String getName() { return m_name; }

  /**
   * Get the documentation string for this construct.
   * @return The docstring for this construct
   */
  public final String getDocstring() { return m_docstring; }

  // Compiler calls this after we've had initial update
  void setOld() { m_new = false;}

}



