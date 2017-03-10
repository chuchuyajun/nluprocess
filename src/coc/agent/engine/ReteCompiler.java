/** **********************************************************************
 * Generates a pattern network
 *
 * See the paper
 * "Rete: A Fast Algorithm for the Many Pattern/ Many Object Pattern
 * Match Problem", Charles L.Forgy, Artificial Intelligence 19 (1982), 17-37.
 *
 * The implementation used here does not follow this paper; the present
 * implementation models the Rete net more literally as a set of networked Node
 * objects with interconnections.
 *
 * $Id: ReteCompiler.java,v 1.1 2012/10/02 21:06:07 Buildadmin Exp $
 ********************************************************************** */

package coc.agent.engine;

import java.util.*;
import java.io.*;

/**
 */
class ReteCompiler implements Serializable
{

  private Rete m_engine;

  private int m_hashkey = 101;
  public int getHashKey() { return m_hashkey; }
  public void setHashKey(int h) { m_hashkey = h; }
  
  /**
    The roots of the pattern network
    */

  private Vector m_roots = new Vector();
  /**
   * @return 
   */
  public final Vector roots() { return m_roots; }

  /**
    Have any rules been added?
    */

  private boolean m_dirty = false;

  /**
   * Constructor
   * @param r 
   */
  public ReteCompiler(Rete r) 
  {
    m_engine = r;
  }

  /**
   * Evaluate a funcall, replacing all variable references with the int[]
   * mini-bindings used by the nodes for predicate funcall evaluation.
   * Bind each variable to the first occurrence in rule
   * @param table 
   * @param v 
   * @param dr 
   * @exception ReteException 
   * @return 
   */
  private Value eval(Hashtable table, Value v, HasLHS dr)
       throws ReteException 
  {
    if (v.type() != RU.FUNCALL)
      return v;
    
    Funcall vv = (Funcall) v.funcallValue(null).clone();
    for (int i=0; i<vv.size(); i++) 
      {
        Value val = vv.get(i);
        if (val instanceof Variable)
          {
            int[] binding = new int[4];
            String name = val.variableValue(null);
            binding[0] = -1;
            Binding b = (Binding) table.get(name);
            if (b != null)
              {
                binding[0] = b.m_factIndex;
                binding[1] = b.m_slotIndex;
                binding[2] = b.m_subIndex;
                binding[3] = b.m_type;
                vv.set(new IntArrayValue(binding, name), i);
              }
            else
              {
                // Defglobal
                if (name.charAt(0) != '*')
                  compilerError("eval", "Unbound variable found in funcall: " +
                                name + " parsing defrule " + dr.getName());
              }
          }
        else if (val.type() ==  RU.FUNCALL) 
          {
            // nested funcalls
            vv.set(eval(table, val, dr), i);
          }
      }

    return new FuncallValue(vv);
  }

  /**
   * Call this on a funcall value AFTER eval has modified it.
   * it returns true iff this funcall contains binding to patterns other
   * than the one named by index.
   * @param v 
   * @param index 
   * @exception ReteException 
   * @return 
   */
  private boolean checkForMultiPattern(Value v, int index)
       throws ReteException 
  {
    ValueVector vv = v.funcallValue(null);

    if (vv.get(0).equals("java-test"))
      return true;

    for (int i=1; i<vv.size(); i++) 
      {
        if (vv.get(i).type() ==  RU.INTARRAY &&
            vv.get(i).intArrayValue(null)[0] != index) 
          {
            return true;
          }
        
        else if (vv.get(i).type() ==  RU.FUNCALL &&
                 checkForMultiPattern(vv.get(i), index)) 
          {
            return true;
          }
      }
    return false;

  }

  void freeze() throws ReteException
  {
    if (m_dirty) 
      {
        for (int i=0; i< m_roots.size(); i++) 
          {
            ((Node) m_roots.elementAt(i)).freeze();
          }
      }
    m_dirty = false;
  }

  private Hashtable m_doneVars = new Hashtable();
  /**
   * Add a rule's patterns to the network
   * @param r 
   * @exception ReteException 
   */  
  public synchronized void addRule(HasLHS r) throws ReteException 
  {
    r.appendCompilationTrace(r.getName() + ": ");

    int hash = r.getNodeIndexHash();
    if (hash == 0)
      hash = m_hashkey;

    // Tell the rule to fix up its binding table; it must be complete now.
    r.freeze();

    // Fetch the rule's binding table; no more makeVarTable!
    Hashtable table = r.getBindings();

    // 'terminals' will be where we hold onto the final links in the
    // chain of nodes built during the first pass for each pattern
    Node[] terminals = new Node[r.getNPatterns()];
    Node[] ruleRoots = new Node[r.getNPatterns()];
    
    /* *********
       FIRST PASS
       ********* */

    // In the first pass, we just create some of the one-input
    // nodes for each pattern.
    // These one-input nodes compare a certain slot in a fact (token) with a
    // fixed, typed, piece of data. Each pattern gets one special
    // one-input node, the TECT node, which checks the class type
    // and class name.


    // iterate over each of the rule's patterns
    for (int i=0; i<r.getNPatterns(); i++) 
      {

        // get this pattern

        Pattern p = (Pattern) r.getPattern(i);

        // If this is a 'test' CE, we have to treat it slightly differently
        boolean isTest = (p.getName().equals("test"));

          
        ////////////////////////////////////////////////////////////
        // Every pattern must have a definite class name
        // Therefore, the first node in a chain always
        // checks the class name of a token (except for test CEs)
        ////////////////////////////////////////////////////////////

        Node last = null;
        
        last =
          createSuccessor(m_roots,
                          isTest? (Node1) new Node1NONE() : (Node1) new Node1TECT(p.getName()),
                          r);
        
        ruleRoots[i] = last;

        // First we have to find all the multifields, because these change the
        // 'shape' of the facts as they go through the net. 

        for (int j=0; !isTest && j<p.getNSlots(); j++) 
          {

            // any tests on this slot?
            if (p.getNTests(j) == 0)
              continue;

            int testIdx = j;
        
            for (int k=0; k<p.getNTests(j); k++) 
              {
                Test1 test = p.getTest(j, k);
                
                if (test.m_slotValue.type() == RU.MULTIVARIABLE) 
                  {
                    // Split this multislot into many multislot facts
                    last = createSuccessor(last.succ(),
                                           new Node1MTMF(testIdx, test.m_subIdx),
                                           r);
                  } 
          
              }

          }


        ////////////////////////////////////////////////////////////
        // Good time to check size, now that it's deinitely set.
        ////////////////////////////////////////////////////////////

        // Test multislot sizes...
        for (int j=0; !isTest && j<p.getNSlots(); j++) 
          {
        
            // if multislot, size is determinate now (splitting done above)
            if (p.getSlotLength(j) != -1) 
              last = createSuccessor(last.succ(),
                                     new Node1MTELN(j, p.getSlotLength(j)),
                                     r);

          }

        ////////////////////////////////////////////////////////////
        // Simplest basic tests are done here
        ////////////////////////////////////////////////////////////

        for (int j=0; !isTest && j<p.getNSlots(); j++) 
          {

            // any tests on this slot?
            if (p.getNTests(j) == 0)
              continue;

            int testIdx = j;
        
            for (int k=0; k<p.getNTests(j); k++) 
              {
                Test1 test = p.getTest(j, k);
                Value slotValue = test.m_slotValue;


                if (slotValue instanceof Variable)
                  {
                    // Single tests against only global variables
                    String name = slotValue.variableValue(null);
                    if (!(name.startsWith("*") && name.endsWith("*")))
                      {
                        continue;
                      }
                  }
                
                Node1 node = null;
                
                // expand variable references in funcalls to index, slot, subslot triples 
                Value v =  eval(table, slotValue, r);
                if (slotValue.type() != RU.FUNCALL || !checkForMultiPattern(v, i))
                  {
                    switch(test.m_test)
                      {
                        
                        //NOTE: These constructors are destructive... they may mangle v.
                      case Test1.EQ:
                        switch (test.m_subIdx)
                          {
                          case -1:
                            node = new Node1TEQ(testIdx, v, m_engine);
                            break;
                          default:
                            node = new Node1MTEQ(testIdx, test.m_subIdx, v, m_engine);
                            break;
                          }
                        break;
                      default:
                        switch (test.m_subIdx)
                          {
                          case -1:
                            node = new Node1TNEQ(testIdx, v, m_engine);
                            break;
                          default:
                            node = new Node1MTNEQ(testIdx, test.m_subIdx, v, m_engine);
                            break;
                          }
                        break;
                      }
                    
                    last = createSuccessor(last.succ(), node, r);                    
                  }
              }
          }
        terminals[i] = last;
      }

    /* *********
       SECOND PASS
       ********* */

    // In this pass, we are looking for variables which must be
    // instantiated the same way twice in one fact. IE, the pattern look like
    // (foo ?X foo ?X), and we're looking for facts like (foo bar foo bar).
    // NOT versions are handled as well.

    // iterate over each of the rule's patterns
    for (int i=0; i<r.getNPatterns(); i++) 
      {
          
        // get this pattern
        Pattern p = (Pattern) r.getPattern(i);

        // If this is a 'test' CE, we have to treat it slightly differently
        if (p.getName().equals("test"))
          continue;

        // workspace to track variables that have been done.
        m_doneVars.clear();

        // find a variable slot, if there is one. If one is found,
        // look at the rest of
        // the fact for another one with the same name.
        // If one is found, create the
        // appropriate node and put it in place.

        // NOTs make things a bit more complex.
        // There are a few cases for a varname
        // appearing twice in a pattern:

        // ?X ?X        ->        generate a TEV1 node.
        // ?X ~?X       ->        generate a TNEV1 node.
        // ~?X ?X       ->        generate a TNEV1 node.
        // ~?X ~?X      ->        (DO NOTHING!)


        // look for a slot in the pattern containing a variable

        for (int j= 0; j < p.getNSlots(); j++) 
          {
            // any tests for this slot?
            if (p.getNTests(j) == 0)
              continue;

            for (int k= 0; k < p.getNTests(j); k++) 
              {
                Test1 test_jk = p.getTest(j, k);                
                if (!(test_jk.m_slotValue instanceof Variable))
                  continue;
                
                // see if we've done this one before.
                String varName = test_jk.m_slotValue.variableValue(null);
                if (m_doneVars.get(varName) != null)
                  continue;
                
                // no, we haven't. Find each other occurrence.
                // We start the search at the same slot since it might be a
                // multislot!
                for (int n=j; n < p.getNSlots(); n++) 
                  {
                    if (p.getNTests(n) == 0)
                      continue;
                    for (int o= 0; o < p.getNTests(n); o++) 
                      {
                        Test1 test_no = p.getTest(n, o);                

                        // This can happen since we're researching the same slot.
                        if (test_no == test_jk)
                          continue;
                        if (test_no.m_slotValue instanceof Variable &&
                            test_no.m_slotValue.equals(test_jk.m_slotValue))
                          {
                            // we've identified another slot with the same variable.
                            // Do what's described in the table above.
                            int slot1 = j;
                            int slot2 = n;
                            if (test_jk.m_test == Test.EQ) 
                              {
                                if (test_no.m_test == Test.EQ)
                                  terminals[i]
                                    = createSuccessor(terminals[i].succ(),
                                                      new Node1TEV1(slot1, test_jk.m_subIdx,
                                                                    slot2, test_no.m_subIdx),
                                                      r);
                                else
                                  terminals[i]
                                    = createSuccessor(terminals[i].succ(),
                                                      new Node1TNEV1(slot1, test_jk.m_subIdx,
                                                                    slot2, test_no.m_subIdx),
                                                      r);
                                
                              }
                            else 
                              {
                                if (test_no.m_test == Test.EQ)
                                  terminals[i]
                                    = createSuccessor(terminals[i].succ(),
                                                      new Node1TNEV1(slot1, test_jk.m_subIdx,
                                                                    slot2, test_no.m_subIdx),
                                                      r);
                                else
                                  ;
                              }
                          }
                      }
                  }
                m_doneVars.put(varName, varName);
              }
          }
        
      } // end of second pass
    
    // Attach a NodeRTL to the very first tail, so this one will do a LEFT calltype
    // instead of a RIGHT one.

    terminals[0] = createSuccessor(terminals[0].succ(), new Node1RTL(), r);

    /* *********
       THIRD PASS
       ********* */

    // Now we start making some two-input nodes. These nodes check that
    // a variable with the same name in two patterns is instantiated the
    // same way in each of two facts; or not, in the case of negated
    // variables. An important thing to remember: the first instance of a
    // variable can never be negated. We'll check that here and throw
    // an exception if it's violated.  We can compare every other instance
    // of the variable in this rule against this first one - this simplifies
    // things a lot! We'll use simplified logic which will lead to a few
    // redundant, but correct tests.

    // Two-input nodes can contain many tests, so they are rather more
    // complex than the one-input nodes. To share them, what we'll do is build
    // a new node, then compare this new one to all possible shared ones.
    // If we can share, we just throw the new one out. The inefficiency is
    // gained back in spades at runtime, both in memory and speed. Note that
    // NodeNot2 type nodes cannot be shared.

    /*
      The number of two-input nodes that we create is *determinate*: it is
      always one less than the number of patterns. For example, w/ 4 patterns,
      numbered 0,1,2,3, and the following varTable:
      (Assuming RU.SLOT_SIZE = 2)

      <PRE>

      X  Y  N
      0  1  2
      2  4  4

      </PRE>
      generated from the following rule LHS:

      <PRE>
      (foo ?X ?X)
      (bar ?X ?Y)
      (Goal (Type Simplify) (Object ?N))
      (Expression (Name ?N) (Arg1 0) (Op +) (Arg2 ~?X))
      </PRE>

      Would result in the following nodes being generated
      (Assuming SLOT_DATA == 0, SLOT_TYPE == 1, SLOT_SIZE == 2):

      <PRE>
        0     1
         \   /
      ___L___R____
      |          |            2
      | 0,2 = 2? |           /
      |          |          /
      ------------ \0,1    /
                   _L______R__                3
                  |          |               /
                  | NO TEST  |              /
                  |          |             /
                  ------------ \0,1,2     /
                                L_______R__
                                | 0,2 != 8?|
                                | 2,4 = 2? |
                                |          |
                                ------------
                                     |0,1,2,3
                                     |
                                (ACTIVATE)

      <PRE>

      Where the notation 2,4 = 8? means that this node tests tbat index 4 of
      fact 2 in the left token is equal to index 8 in the right
      token's single fact. L and R indicate Left and Right inputs.
      */

    // for each pattern, starting with the second one

    for (int i=1; i < (r.getNPatterns()); i++) 
      {

        // get this pattern
        Pattern p = (Pattern) r.getPattern(i);

        // Keep track of which variables we've tested already for this pattern.
        m_doneVars.clear();

        // If this is a 'test' CE, we have to treat it slightly differently
        boolean isTest = (p.getName().equals("test"));

        // construct an appropriate 2 input node...
        NodeTest n2;
        
        if (isTest)
          n2 = new NodeTest(m_engine);
        else if (p.getNegated() != 0)
          n2 = new NodeNot2(m_engine, hash);
        else
          n2 = new Node2(m_engine, p.getUnique(), hash);
        
        // now tell the node what tests to perform

        // for each field in this pattern
        for (int j=0; j< p.getNSlots(); j++) 
          {

            // any tests for this slot?
            if (p.getNTests(j) == 0)
              continue;

            // for every test on this slot..
            for (int k=0; k< p.getNTests(j); k++) 
              {
                Test1 test_jk = p.getTest(j, k);
                // if this test is against a variable...
                if (test_jk.m_slotValue instanceof Variable) 
                  {
                    // find this variable in the table
                    String name = p.getTest(j, k).m_slotValue.variableValue(null);

                    if (m_doneVars.get(name) != null)
                      continue;

                    Binding b = (Binding) table.get(name);
                    if (b == null)
                      compilerError("addRule",
                                    "Corrupted VarTable: var " +
                                    name + " not in table");


                    // if this is the first appearance, no test.
                    else if (b.m_factIndex == i)
                      continue;

                    if (test_jk.m_test == Test.EQ)
                      n2.addTest(Test.EQ,
                                 b.m_factIndex,
                                 b.m_slotIndex,
                                 b.m_subIndex,
                                 j,
                                 test_jk.m_subIdx);
                    else
                      n2.addTest(Test.NEQ,
                                 b.m_factIndex,
                                 b.m_slotIndex,
                                 b.m_subIndex,
                                 j,
                                 test_jk.m_subIdx);

                    m_doneVars.put(name, name);

                    // if this test is a function call
                  }
                else if (test_jk.m_slotValue.type() == RU.FUNCALL) 
                  {
                    // This simplifies things somewhat!
                    if (p.getDeftemplate().getBackwardChaining())
                      throw new ReteException("ReteCompiler.addRule",
                                              "Can't use funcalls in backchained patterns",
                                              p.getName());
                    
                    // expand the variable references to index, slot pairs
                    // we do this again even though we did it in pass one
                    // we don't want to destroy the patterns themselves
                    // Tell Eval to bind variables to first occurrence in Rule.
                    Value v = eval(table, test_jk.m_slotValue, r);

                    // if other facts besides this one are mentioned, generate a test

                    if (isTest || checkForMultiPattern(v, i)) 
                      {
                        if (test_jk.m_test == Test.EQ)
                          n2.addTest(Test.EQ, j,
                                     test_jk.m_subIdx, v);
                        else
                          n2.addTest(Test.NEQ, j,
                                     test_jk.m_subIdx, v);
                      }
                  }
              }
          }
      
        // search through the successors of this pattern and the next one.
        // Do they have any in common, and if so, are they equivalent to the
        // one we just built? If so, we don't need to add the new one!
        // Don't share Node2's.
 
        boolean newNode = true;

        Vector left = terminals[i-1].succ();
        Vector right = terminals[i].succ();
        
      j_loop:
        for (int j=0; j<left.size(); j++) 
          {
            Node jnode = (Node) left.elementAt(j);
            if (!n2.equals(jnode))
              continue;
            for (int k=0; k<right.size(); k++) 
              {
                if (jnode == right.elementAt(k))
                  {
                    n2 = (NodeTest) jnode; 
                    newNode = false;
                    break j_loop;
                  }
              }
          }
      
        if (newNode) 
          {
            // attach it to the tails of the node chains from
            // this pattern and the next
        
            left.addElement(n2);
            right.addElement(n2);
       
            if (r instanceof Defrule &&
                p.getDeftemplate().getBackwardChaining() && ! p.getExplicit())
              {
                // n2 can't be a NodeTest, since do-backwards-chaining would
                // have rejected the call. Explicit CEs are those for which we should
                // never do backwards chaining
                ((Node2) n2).setBackchainInfo(p, (Defrule) r);
              }

            // OK, it's done; speed it up!
            n2.complete();
            r.appendCompilationTrace("+2");

          }
        else 
          r.appendCompilationTrace("=2");
        
        r.addNode(n2);
                    
        // Handling nested nots. We have already built the innermost "not".
        // Now we add the outer ones. Each not feeds into the -right- input of
        // the next outermost one. Only the innermost has any tests in it.
        if (p.getNegated() > 1)
          {
            for (int nested_nots=1; nested_nots < p.getNegated(); nested_nots++)
              {
                Node nltr = new Node1LTR();  // A left-to-right input adapter
                NodeTest not = new NodeNot2(m_engine, hash);   // outer not
                not.complete();              // must call this after all tests added
                n2.succ().addElement(nltr);  // the inner not feeds into the adapter
                nltr.succ().addElement(not); // the adapter feeds into the outer not
                left.addElement(not);        // the inner not's left input also goes into outer
                r.addNode(nltr);
                r.addNode(not);
                n2 = not;
              }
            
          }

        // Advance the tails
        terminals[i-1] = terminals[i] = n2;

      }
    
    /* ************
       FOURTH PASS
       ************ */

    // All that's left to do is to create the terminal node.
    // This is very easy.

    terminals[r.getNPatterns() - 1].succ().addElement(r);
    r.appendCompilationTrace("+t");
    r.addNode(r);

    // we need freezing.
    m_dirty = true;

    //Tell the engine to update this rule if the fact list isn't empty
    Hashtable uniqueRoots = new Hashtable();
    for (int i=0; i<ruleRoots.length; i++)
      uniqueRoots.put(ruleRoots[i], ruleRoots[i]);
    m_engine.updateNodes(uniqueRoots);
    r.setOld();
  }

  Node createSuccessor(Vector amongst, Node1 n, HasLHS r) 
       throws ReteException
  {
    for (int i=0; i< amongst.size(); i++) 
      {
        Node test = (Node) amongst.elementAt(i);
        if (n.equals(test))
          {
            r.appendCompilationTrace("=1");
            r.addNode(test);
            return test;
          }
      }
    // No match found

    amongst.addElement(n);
    r.appendCompilationTrace("+1");
    r.addNode(n);
    return n;
  }

  private void compilerError(String routine, String message)
       throws ReteException 
  {
    throw new ReteException("ReteCompiler." + routine, message, "");
  }

}

