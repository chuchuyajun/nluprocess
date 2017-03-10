package coc.agent.engine;

import java.io.*;
import java.util.*;

/**
 * Parser functionality for Jess.
 * <P>
 */
public class Jesp implements Serializable
{
  private final static String JAVACALL = "call";

  /**
    Stream where input comes from
    */
  private JessTokenStream m_jts;
  private Rete m_engine;

  /**
   * Construct a Jesp object.
   * The reader will be looked up in the Rete object's router tables, and any wrapper
   * found there will be used.
   * @param is The Reader from which this Jesp should get its input
   * @param e The engine that the parsed commands go to
   */

  public Jesp(Reader is, Rete e) 
  {
    // We retrieve the official wrapper, if any.
    Tokenizer t = e.getInputWrapper(is);

    if (t == null)
      {
        t = new Tokenizer(is);
      }
    
    m_jts = new JessTokenStream(t);
    m_engine = e;
  }  

  /**
   * Parses an input file. 
   * Argument is true if a prompt should be printed (to the ReteObject's standard
   * output), false for no prompt.
   * @param prompt True if a prompt should be printed.
   * @exception ReteException If anything goes wrong.
   * @return The result of the last parsed entity (often TRUE or FALSE).
   */
  /**
   * TODO
   * changed: 'synchronized' is removed. Don't know if there is any potential problem.
   */
  public synchronized Value parse(boolean prompt) throws ReteException 
  //public Value parse(boolean prompt) throws ReteException 
  {
    Value val = Funcall.TRUE, oldval = val;

    if (prompt)
      {
        m_engine.getOutStream().print("Jess> ");
        m_engine.getOutStream().flush();
      }

    while (!val.equals(Funcall.EOF))
      {
        oldval = val;
        val = parseSexp();

        if (prompt)
          {
            if (!val.equals(Funcall.NIL))
              {
                if (val.type() == RU.LIST)
                  // Add parens to list
                  m_engine.getOutStream().print('(');

                m_engine.getOutStream().print(val);

                if (val.type() == RU.LIST)
                  m_engine.getOutStream().print(')');

                m_engine.getOutStream().println();
              }
            m_engine.getOutStream().print("Jess> ");
            m_engine.getOutStream().flush();
          }
      }
    return oldval;
  }

  /**
   * Flush any partially-parsed information, probably to the next ')'. Useful in
   * error recovery.
   */

  public void clear()
  {
    m_jts.clear();
  }

  /**
   * Change to a new engine: called during bload
   */

  //void setEngine(Rete e)
  public void setEngine(Rete e)
  {
    clear();
    m_engine = e;
  }

  /**
   * Parses an input file containing only facts, asserts each one.
   * @exception ReteException 
   * @return 
   */
  Value loadFacts() throws ReteException 
  {
    JessToken jt = m_jts.nextToken();

    while (jt.m_ttype != RU.NONE)
      {
        m_jts.pushBack(jt);
        Fact f = parseFact();
        m_engine.assertFact(f);
        jt = m_jts.nextToken();
      }

    return Funcall.TRUE;
  }

  /**
   * parseSexp
   * 
   * Syntax:
   * ( -Something- )
   * @exception ReteException 
   * @return 
   */
  private Value parseSexp() throws ReteException
  {
    try
      {
        JessToken jt = m_jts.nextToken();
        switch (jt.m_ttype)
          {
          case RU.ATOM: case RU.STRING: case RU.INTEGER: case RU.FLOAT:
          case RU.VARIABLE: case RU.MULTIVARIABLE:
            return jt.tokenToValue(m_engine.getGlobalContext());
          case '(':
            m_jts.pushBack(jt);
            break;
          case RU.NONE:
            if ("EOF".equals(jt.m_sval))
              return Funcall.EOF;
            // FALL THROUGH
          default:
            throw new ReteException("Jesp.parseSexp",
                                    "Expected a '(', constant, or global variable",
                                    jt.toString());
          }

        String head = m_jts.head();
        
        if (head.equals("defrule"))
          return parseDefrule();

        else if (head.equals("defquery"))
          return parseDefquery();
        
        else if (head.equals("deffacts"))
          return parseDeffacts();
        
        else if (head.equals("deftemplate"))
          return parseDeftemplate();
        
        else if (head.equals("deffunction"))
          return parseDeffunction();
        
        else if (head.equals("defglobal"))          
          return parseDefglobal();
        //delete by stony
//        else if (head.equals("assertp"))          
//          return parseAssertp();
          

        else if (head.equals("EOF"))
          return Funcall.EOF;
        
        else 
          return parseAndExecuteFuncall(null);
      }
    catch (ReteException re)
      {
        if (re instanceof ParseException)
          throw re;
        else
          {
            re.setLineNumber(m_jts.lineno());
            re.setProgramText(m_jts.toString());
            m_jts.clear();
            throw re;
          }
      }

  }
  
  /**
   * parseAssertp
   * 
   * Syntax:
   * (assertp (fact))
   * @exception ReteException 
   * @return Value
   */
  private Value parseAssertp() throws ReteException 
  {
    /* ****************************************
       '(assertp (fact))'
       **************************************** */
      
     JessToken tok = null;
     if ((m_jts.nextToken().m_ttype != '(') ||
          !(m_jts.nextToken().m_sval.equals("assertp")) ||
         ((tok = m_jts.nextToken()).m_ttype != '('))
     parseError("parseAssertp", "Expected (assertp");
     
     m_jts.pushBack(tok);
     Fact fact = parseFact();
     tok = m_jts.nextToken();
     return new Value(m_engine.assertp(fact), RU.INTEGER);
  }

  /**
   * parseDefglobal
   * 
   * Syntax:
   * (defglobal ?x = 3 ?y = 4 ... )
   * @exception ReteException 
   * @return 
   */
  private Value parseDefglobal() throws ReteException 
  {
    /* ****************************************
       '(defglobal'
       **************************************** */

    if (  (m_jts.nextToken().m_ttype != '(') ||
          ! (m_jts.nextToken().m_sval.equals("defglobal")) )
      parseError("parseDefglobal", "Expected (defglobal...");


    /* ****************************************
       varname = value sets
       **************************************** */

    JessToken name, value;
    while ((name = m_jts.nextToken()).m_ttype != ')') 
      {

        if (name.m_ttype != RU.VARIABLE)
          parseError("parseDefglobal", "Expected a variable name");
      
        // Defglobal names must start and end with an asterisk!
        if (name.m_sval.charAt(0) != '*' ||
            name.m_sval.charAt(name.m_sval.length() -1) != '*')
          parseError("parseDefglobal", "Defglobal names must start and " +
                      "end with an asterisk!");

        if (m_jts.nextToken().m_ttype != '=')
          parseError("parseDefglobal", "Expected =");

        value = m_jts.nextToken();

        switch (value.m_ttype) 
          {

          case RU.ATOM: case RU.STRING: 
            m_engine.addDefglobal(new Defglobal(name.m_sval,
                                  new Value(value.m_sval, value.m_ttype)));
            break;

          case RU.VARIABLE:
            m_engine.addDefglobal(new Defglobal(name.m_sval,
                                  new Variable(value.m_sval, value.m_ttype)));
            break;

          case RU.FLOAT:
          case RU.INTEGER:
            m_engine.addDefglobal(new Defglobal(name.m_sval,
                               new Value(value.m_nval, value.m_ttype)));
            break;

          case '(': 
            {
              m_jts.pushBack(value);
              Funcall fc = parseFuncall();
              m_engine.addDefglobal(new Defglobal(name.m_sval,
                                 new FuncallValue(fc)));
            }            
            break;

          default:
            parseError("parseDefglobal", "Bad value");
          }
      }

    return Funcall.TRUE;

  }

  /**
   * parseFuncall
   * 
   * Syntax:
   * (functor field2 (nested funcall) (double (nested funcall)))
   * 
   * Trick: If the functor is a variable, we insert the functor 'call'
   * and assume we're going to make an outcall to Java on the object in
   * the variable!
   * @exception ReteException 
   * @return 
   */
  private Funcall parseFuncall() throws ReteException 
  {
    JessToken tok;
    String name = null;
    Funcall fc;
    
    if (m_jts.nextToken().m_ttype != '(')
      parseError("parseFuncall", "Expected '('");

    /* ****************************************
       functor
       **************************************** */
    tok = m_jts.nextToken();
    switch (tok.m_ttype) 
      {

      case RU.ATOM:
        name = tok.m_sval;
        break;

      case '=':
        // special functors
        name = "=".intern();
        break;

      case RU.VARIABLE:
        // insert implied functor
        name = JAVACALL;
        break;

      default:
        parseError("parseFuncall", "Bad functor");
      }
    fc = new Funcall(name, m_engine);

    if (tok.m_ttype == RU.VARIABLE)
      fc.add(new Variable(tok.m_sval, RU.VARIABLE));

    /* ****************************************
       arguments
       **************************************** */
    tok = m_jts.nextToken();
    while (tok.m_ttype != ')') 
      {

        switch (tok.m_ttype) 
          {

            // simple arguments
          case RU.ATOM: case RU.STRING:
            fc.add(new Value(tok.m_sval, tok.m_ttype)); break;

          case RU.VARIABLE: case RU.MULTIVARIABLE:
            fc.add(new Variable(tok.m_sval, tok.m_ttype)); break;

          case RU.FLOAT:
          case RU.INTEGER:
            fc.add(new Value(tok.m_nval, tok.m_ttype)); break;

            // nested funcalls
          case '(':
            m_jts.pushBack(tok);
            if (name.equals("assert") || name.equals("assertp")) 
              {
                Fact fact = parseFact();
                fc.add(new Value(fact, RU.FACT));
                break;
          
              } 
            else if (name.equals("modify")) 
              {
                ValueVector pair = parseValuePair();
                fc.add(new Value(pair, RU.LIST));
                break;

              }
            else 
              {
                Funcall fc2 = parseFuncall();
                fc.add(new FuncallValue(fc2));
                break;
              }

          case RU.NONE:
            // EOF during eval
            parseError("parseFuncall", "Unexpected EOF");
            break;
            
          default:
            fc.add(new Value(String.valueOf((char) tok.m_ttype), RU.STRING));
            break;

          } // switch tok.m_ttype
        tok = m_jts.nextToken();
      } // while tok.m_ttype != ')'

    return fc;
  }

  /**
   * parseValuePair
   * These are used in (modify) funcalls and salience declarations
   * 
   * Syntax:
   * (ATOM VALUE)
   * @exception ReteException 
   * @return 
   */
  private ValueVector parseValuePair() throws ReteException 
  {
    ValueVector pair = new ValueVector(2);
    JessToken tok = null;

    /* ****************************************
       '(atom'
       **************************************** */

    if (m_jts.nextToken().m_ttype != '(' ||
        (tok = m_jts.nextToken()).m_ttype != RU.ATOM) 
      {
        parseError("parseValuePair", "Expected '( <atom>'");
      }

    pair.add(new Value(tok.m_sval, RU.ATOM));

    /* ****************************************
       value
       **************************************** */
    do
      {
        switch ((tok = m_jts.nextToken()).m_ttype) 
          {            
          case RU.ATOM: case RU.STRING:
            pair.add(new Value(tok.m_sval, tok.m_ttype)); break;

          case RU.VARIABLE: case RU.MULTIVARIABLE:
            pair.add(new Variable(tok.m_sval, tok.m_ttype)); break;
            
          case RU.FLOAT:
          case RU.INTEGER:
            pair.add(new Value(tok.m_nval, tok.m_ttype)); break;
            
          case '(':
            m_jts.pushBack(tok);
            Funcall fc = parseFuncall();
            pair.add(new FuncallValue(fc)); break;
            
          case ')':
            break;

          default:
            parseError("parseValuePair", "Bad argument");
          }
      }
    while (tok.m_ttype != ')');

    return pair;
  }
  

  /**
   * parseDeffacts
   * 
   * Syntax:
   * (deffacts <name> ["comment"] (fact) [(fact)...])
   * @exception ReteException 
   * @return 
   */
  private Value parseDeffacts() throws ReteException 
  {
    Deffacts df = null;
    JessToken tok = null;

    /* ****************************************
       '(deffacts'
       **************************************** */

    if (m_jts.nextToken().m_ttype != '(' ||
        (tok = m_jts.nextToken()).m_ttype != RU.ATOM ||
        !tok.m_sval.equals("deffacts")) 
      {
        parseError("parseDeffacts", "Expected '( deffacts'");
      }

    /* ****************************************
       deffacts name
       **************************************** */

    if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
      parseError("parseDeffacts", "Expected deffacts name");
    String name = tok.m_sval;

    tok = m_jts.nextToken();

    /* ****************************************
       optional comment
       **************************************** */

    String docstring = "";
    if (tok.m_ttype == RU.STRING) 
      {
        docstring = tok.m_sval;
        tok = m_jts.nextToken();
      } 

    df = new Deffacts(name, docstring);

    /* ****************************************
       list of facts
       **************************************** */

    while (tok.m_ttype == '(') 
      {
        m_jts.pushBack(tok);
        Fact f = parseFact();
        df.addFact(f);
        tok = m_jts.nextToken();
      }

    /* ****************************************
       closing paren
       **************************************** */

    if (tok.m_ttype != ')')
      parseError("parseDeffacts", "Expected ')'");

    m_engine.addDeffacts(df);
    return Funcall.TRUE;

  }

  /**
   * parseFact
   * 
   * This is called from the parse routine for Deffacts and from the
   * Funcall parser for 'assert'; because of this latter, it can have
   * variables that need expanding.
   * 
   * Syntax:
   * ordered facts: (atom field1 2 "field3")
   * NOTE: We now turn these into unordered facts with a single slot "__data"
   * unordered facts: (atom (slotname value) (slotname value2))
   * @exception ReteException 
   * @return 
   */
  Fact parseFact() throws ReteException 
  {
    String name, slot=RU.DEFAULT_SLOT_NAME;
    int slot_type;
    Fact f;
    JessToken tok = null;

    /* ****************************************
       '( atom'
       **************************************** */

    if (m_jts.nextToken().m_ttype != '(' ||
        (tok = m_jts.nextToken()).m_ttype != RU.ATOM)
      parseError("parseFact", "Expected '( <atom>'");
    
    name = tok.m_sval;
  
    /* ****************************************
       slot data
       What we do next depends on whether we're parsing
       an ordered or unordered fact. We can determine this very easily:
       If there is a deftemplate, use it; if the first slot is named "__data", this
       is unordered, else ordered. If there is no deftemplate, assume ordered.
       **************************************** */

    // get a deftemplate if one already exists.
    boolean ordered = false;

    Deftemplate deft = m_engine.createDeftemplate(name);
    if (deft.getSlotIndex(RU.DEFAULT_SLOT_NAME) == 0)
      ordered = true;

    /* ****************************************
       SLOT DATA 
       **************************************** */
    f = new Fact(name, m_engine);
    tok = m_jts.nextToken();
    
    while (tok.m_ttype != ')') 
      {
        
        if (!ordered)
          {
            // Opening parenthesis
            if (tok.m_ttype != '(')
              parseError("parseFact", "Expected '('");
            
            // Slot name
            if  ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
              parseError("parseFact", "Bad slot name");
            slot = tok.m_sval;
            tok = m_jts.nextToken();
          }
        
        // Is this a slot or a multislot?
        slot_type = deft.getSlotType(deft.getSlotIndex(slot));
        
        switch (slot_type) 
          {
            
            // Data in normal slot
          case RU.SLOT:
            switch (tok.m_ttype) 
              {
                
              case RU.ATOM:
              case RU.STRING:
                f.setSlotValue(slot, new Value(tok.m_sval, tok.m_ttype)); break;
                
              case RU.VARIABLE: case RU.MULTIVARIABLE:
                f.setSlotValue(slot, new Variable(tok.m_sval, tok.m_ttype)); break;
                
              case RU.FLOAT:
              case RU.INTEGER:
                f.setSlotValue(slot, new Value(tok.m_nval, tok.m_ttype)); break;
                
              case '=':
                tok = m_jts.nextToken();
                if (tok.m_ttype != '(')
                  throw new ReteException("Jesp.parseFact",
                                          "'=' cannot appear as an " +
                                          "atom within a fact", "");
                // FALLTHROUGH
              case '(': 
                {
                  m_jts.pushBack(tok);
                  Funcall fc = parseFuncall();
                  f.setSlotValue(slot, new FuncallValue(fc)); break;
                }
                
              default:
                parseError("parseFact", "Bad slot value");
              }
            
            if  ((tok = m_jts.nextToken()).m_ttype != ')')
              parseError("parseFact", "Expected ')'");
            break;
            
          case RU.MULTISLOT:
            // Data in multislot
            // Code is very similar, but bits of data are added to a multifield
            ValueVector slot_vv = new ValueVector();

            while (tok.m_ttype != ')') 
              {
                switch (tok.m_ttype) 
                  {
                    
                  case RU.ATOM:
                  case RU.STRING:
                    slot_vv.add(new Value(tok.m_sval, tok.m_ttype)); break;

                  case RU.VARIABLE: case RU.MULTIVARIABLE:
                    slot_vv.add(new Variable(tok.m_sval, tok.m_ttype)); break;
                    
                  case RU.FLOAT:
                  case RU.INTEGER:
                    slot_vv.add(new Value(tok.m_nval, tok.m_ttype)); break;
                    
                  case '=':
                    tok = m_jts.nextToken();
                    if (tok.m_ttype != '(')
                      throw new ReteException("Jesp.parseFact",
                                              "'=' cannot appear as an " +
                                              "atom within a fact", "");
                    // FALLTHROUGH
                  case '(': 
                    {
                      m_jts.pushBack(tok);
                      Funcall fc = parseFuncall();
                      slot_vv.add(new FuncallValue(fc)); break;
                    }
                    
                  default:
                    parseError("parseFact", "Bad slot value");
                  }
                
                tok = m_jts.nextToken();
                
              }
            f.setSlotValue(slot, new Value(slot_vv, RU.LIST));          
            break;
            
          default:
            parseError("parseFact", "No such slot in deftemplate");
          }          
        
        if (!ordered)
          {
            // hopefully advance to next ')'
            tok = m_jts.nextToken();
          }
        else
          break;        
      }
    
    if (tok.m_ttype != ')')
      parseError("parseFact", "Expected ')'");

    return f;
    
  }

  /**
   * parseDeftemplate
   * 
   * Syntax:
   * (deftemplate (slot foo (default <value>)) (multislot bar))
   * @exception ReteException 
   * @return 
   */
  private Value parseDeftemplate() throws ReteException 
  {
    Deftemplate dt;
    int slot_type = RU.SLOT;
    Value default_value = null;
    String default_type = null;
    JessToken tok;

    /* ****************************************
       '(deftemplate'
       **************************************** */

    if (  (m_jts.nextToken().m_ttype != '(') ||
          ! (m_jts.nextToken().m_sval.equals("deftemplate")) )
      parseError("parseDeftemplate", "Expected (deftemplate...");

    /* ****************************************
       deftemplate name, optional extends clause
       **************************************** */

    if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
      parseError("parseDeftemplate", "Expected deftemplate name");
    
    String name = tok.m_sval;
    String docstring = "";
    String parent = null;
    
    if ((tok = m_jts.nextToken()).m_ttype == RU.ATOM) 
      {
        if (tok.m_sval.equals("extends"))
          {
            tok = m_jts.nextToken();
            if (tok.m_ttype == RU.ATOM)
              parent = tok.m_sval;
            else
              parseError("parseDeftemplate", "Expected deftemplate name to extend");
          }
        else
          parseError("parseDeftemplate", "Expected '(' or 'extends'");
        tok = m_jts.nextToken();
      }

    if (parent == null)
      dt = new Deftemplate(name, docstring);
    else
      dt = new Deftemplate(name, docstring, m_engine.findDeftemplate(parent));
      
    /* ****************************************
       optional comment
       **************************************** */

    if (tok.m_ttype == RU.STRING) 
      {
        docstring = tok.m_sval;
        tok = m_jts.nextToken();
      }

    /* ****************************************
       individual slot descriptions
       **************************************** */
    
    // ( <slot type>

    while (tok.m_ttype == '(') 
      { // 'slot' 
        if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM ||
            !(tok.m_sval.equals("slot") || tok.m_sval.equals("multislot")))
          parseError("parseDeftemplate", "Bad slot type");

        slot_type = tok.m_sval.equals("slot") ? RU.SLOT : RU.MULTISLOT;
      
        // <slot name>
        if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
          parseError("parseDeftemplate", "Bad slot name");
        name = tok.m_sval;      
      
        // optional slot qualifiers
      
        default_value = (slot_type == RU.SLOT) ?
          Funcall.NIL : Funcall.NILLIST;

        default_type = "ANY";
      
        tok = m_jts.nextToken();
        while (tok.m_ttype == '(') 
          { // slot qualifier
            if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
              parseError("parseDeftemplate", "Slot qualifier must be atom");
        
            // default value qualifier
        
            String option = tok.m_sval;
              
            if (option.equalsIgnoreCase("default") ||
                option.equalsIgnoreCase("default-dynamic"))
              {
                tok = m_jts.nextToken();
                switch (tok.m_ttype) 
                  {

                  case RU.ATOM: case RU.STRING:
                    default_value = new Value(tok.m_sval, tok.m_ttype); break;

                  case RU.FLOAT:
                  case RU.INTEGER:
                    default_value = new Value(tok.m_nval, tok.m_ttype); break;
                    
                  case '(': 
                    if (option.equalsIgnoreCase("default-dynamic"))
                      {
                        m_jts.pushBack(tok);
                        Funcall fc = parseFuncall();
                        default_value = new FuncallValue(fc);
                      }
                    else
                      default_value = parseAndExecuteFuncall(tok);
                    break;
                    
                  default:
                    parseError("parseDeftemplate",
                               "Illegal default slot value");
                  }
              }
            else if (option.equalsIgnoreCase("type")) 
              {
                if (slot_type == RU.MULTISLOT)
                  parseError("parseDeftemplate", 
                             "'type' not allowed for multislots");

                // type is allowed; we save the value, but otherwise ignore it.
                tok = m_jts.nextToken();
                default_type = tok.m_sval;
              }
            else
              parseError("parseDeftemplate", "Unimplemented slot qualifier");
      
            if ((tok = m_jts.nextToken()).m_ttype != ')')
              parseError("parseDeftemplate", "Expected ')'");
      
            tok = m_jts.nextToken();
          }
        if (tok.m_ttype != ')')
          parseError("parseDeftemplate", "Expected ')'");
      
        if (slot_type == RU.SLOT)
          dt.addSlot(name, default_value, default_type);
        else
          {
            if (default_value.type() != RU.LIST)
              parseError("parseDeftemplate", "Default value for multislot " +
                         name + " is not a multifield: " + default_value);
            dt.addMultiSlot(name, default_value);
          }
      
        tok = m_jts.nextToken();
      }
    if (tok.m_ttype != ')')
      parseError("parseDeftemplate", "Expected ')'");

    m_engine.addDeftemplate(dt);
    return Funcall.TRUE;
  }
  

  /**
   * parseDefrule
   * Wrapper around doParseDefrule
   * We're going to split defrules into multiple rules is we see an (or) CE
   *
   * @exception ReteException 
   * @return 
   */
  private Value parseDefrule() throws ReteException 
  {
    Value v;
    v = doParseDefrule();
    return v;
  }

  private Hashtable m_varnames = new Hashtable();

  /**
   * doParseDefrule
   * synchronized to protect m_varnames
   * 
   * 
   * Syntax:
   * (defrule name
   * [ "docstring...." ]
   * [ (declare [(salience 1)] [(node-index-hash 57)]) ]
   * (pattern 1)
   * ?foo <- (pattern 2)
   * (pattern 3)
   * =>
   * (action 1)
   * (action ?foo)
   * )
   * @exception ReteException 
   * @return 
   */

  private synchronized Value doParseDefrule() throws ReteException 
  {
    Defrule dr;
    JessToken tok, tok2;

    /* ****************************************
       '(defrule'
       **************************************** */

    if (  (m_jts.nextToken().m_ttype != '(') ||
          ! (m_jts.nextToken().m_sval.equals("defrule")) )
      parseError("parseDefrule", "Expected (defrule...");


    /* ****************************************
       defrule name, optional comment
       **************************************** */

    if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
      parseError("parseDefrule", "Expected defrule name");
    String name = tok.m_sval;

    String docstring = "";
    if ((tok = m_jts.nextToken()).m_ttype == RU.STRING) 
      {
        docstring = tok.m_sval;
        tok = m_jts.nextToken();
      }

    dr = new Defrule(name, docstring, m_engine);

    // check for salience declaration
    if (tok.m_ttype == '(')
      {
        if ((tok2 = m_jts.nextToken()).m_ttype == RU.ATOM && tok2.m_sval.equals("declare")) 
          {
            while ((tok2 = m_jts.nextToken()).m_ttype != ')')
              {
                m_jts.pushBack(tok2);
                ValueVector vv = parseValuePair();
                
                String head = vv.get(0).atomValue(null);
                if (head.equals("salience"))
                  dr.setSalience(vv.get(1));
                else if (head.equals("node-index-hash"))
                  dr.setNodeIndexHash(vv.get(1).intValue(m_engine.getGlobalContext()));
                else
                  parseError("parseDefrule", "Expected 'salience' or 'node-index-hash'");
              }

            tok = m_jts.nextToken();
          }
        else 
          { // head wasn't 'declare'
            m_jts.pushBack(tok2);
          }
      }

    // **************************************************
    // We need to keep track of the type of each variable, since
    // CLIPS code lets you omit the second and later '$' before multivars.
    // This only matters when a multivar is actualy matched against, since
    // if the '$' is omitted, a TMF node won't get generated. We'll therefore
    // 'put the $'s back in' as needed. This table is shared across all
    // patterns. in a rule.
    // **************************************************

    m_varnames.clear();

    // now we're looking for just patterns

    while (tok.m_ttype == '(' || tok.m_ttype == RU.VARIABLE) 
      {
        String patternBinding = null;
        switch (tok.m_ttype) 
          {
          case RU.VARIABLE: 
            {
              // pattern bound to a variable
              // These look like this:
              // ?name <- (pattern 1 2 3)

              patternBinding = tok.m_sval;

              if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM ||
                  !tok.m_sval.equals("<-"))
                parseError("parseDefrule", "Expected '<-'");

              // FALL THROUGH
            }
          
          case '(': 
            {
              // pattern not bound to a var
              if (tok.m_ttype == '(')
                m_jts.pushBack(tok);
              Pattern p = parsePattern(0, m_varnames);

              if (patternBinding != null && p.getNegated() != 0)
                parseError("parseDefrule",
                           "'not' and 'test' CE's cannot be bound to variables");

              p.setBoundName(patternBinding);
              dr.addPattern(p);
              break;
            }
          } 
        tok = m_jts.nextToken();
      }

    if (tok.m_ttype != '=' ||
        (tok = m_jts.nextToken()).m_ttype != RU.ATOM ||
        !tok.m_sval.equals(">"))
      {
        parseError("parseDefrule", "Expected '=>'");
      }
    
    tok = m_jts.nextToken();

    while (tok.m_ttype == '(') 
      {
        m_jts.pushBack(tok);
        Funcall f = parseFuncall();
        dr.addAction(f);
        tok = m_jts.nextToken();
      }

    if (tok.m_ttype != ')')
      parseError("parseDefrule", "Expected ')'");

    m_engine.addDefrule(dr);
    return Funcall.TRUE;
  }

  
  /**
   * parsePattern
   * 
   * parse a Pattern object in a Rule LHS context
   * 
   * Syntax:
   * Like that of a fact, except that values can have complex forms like
   * 
   * ~value       (test for not a value)
   * ?X&~red      (store the value in X; fail match if not red)
   * ?X&:(> ?X 3) (store the value in X; fail match if not greater than 3)
   * @param negcnt 
   * @param varnames 
   * @exception ReteException 
   * @return 
   */
  private Pattern parsePattern(int negcnt, Hashtable varnames) throws ReteException 
  {
    String name, slot = RU.DEFAULT_SLOT_NAME;
    Pattern p;
    JessToken tok = null;

    /* ****************************************
       ' ( <atom> '
       **************************************** */

    if (  (m_jts.nextToken().m_ttype != '(') ||
          ! ((tok = m_jts.nextToken()).m_ttype == RU.ATOM))
      parseError("parsePattern", "Expected '( <atom>'");

    name = tok.m_sval;

    /* ****************************************
       Special handling for NOT CEs
       NoTs can now be nested
       **************************************** */

    if (name.equals("not")) 
      {
        // this is a negated pattern; strip off the (not ) and 
        // recursively parse the actual pattern.

        p = parsePattern(negcnt + 1, varnames);
        if (m_jts.nextToken().m_ttype != ')')
          parseError("parsePattern", "Expected ')'");
        return p;
      } 

    /* ****************************************
       Special handling for EXISTS CEs
       Note that these can be nested inside of NOTs.
       **************************************** */

    if (name.equals("exists")) 
      {
        // Strip (exists) and set negcnt to 2 (exists A => not not A)

        p = parsePattern(negcnt + 2, varnames);
        if (m_jts.nextToken().m_ttype != ')')
          parseError("parsePattern", "Expected ')'");
        p.setUnique();
        return p;
      } 

    /* ****************************************
       Special handling for UNIQUE CEs
       Note that these can be nested inside of NOTs.
       **************************************** */

    if (name.equals("unique")) 
      {
        // this is a determined pattern; strip off the (unique ) and 
        // recursively parse the actual pattern.

        p = parsePattern(negcnt, varnames);
        if (m_jts.nextToken().m_ttype != ')')
          parseError("parsePattern", "Expected ')'");
        p.setUnique();
        return p;
      } 

    /* ****************************************
       Special handling for EXPLICIT CEs
       Note that these can be nested inside of NOTs.
       **************************************** */

    if (name.equals("explicit")) 
      {
        // this is a determined pattern; strip off the (explicit ) and 
        // recursively parse the actual pattern.

        p = parsePattern(negcnt, varnames);
        if (m_jts.nextToken().m_ttype != ')')
          parseError("parsePattern", "Expected ')'");
        p.setExplicit(true);
        return p;
      } 

    /* ****************************************
       Special handling for TEST CEs
       Note that these can be nested inside of NOTs.
       **************************************** */

    if (name.equals("test")) 
      {
        // this is a 'test' pattern. We trick up a fake one-slotted
        // pattern which will get treated specially by the compiler.
        p = new Pattern(name, m_engine, 0);

        Funcall f = parseFuncall();
        p.addTest("__data", new Test1((negcnt % 2) == 1 ? Test.NEQ : Test.EQ,
                                      -1, new FuncallValue(f))); 

        if (m_jts.nextToken().m_ttype != ')')
          parseError("parsePattern", "Expected ')'");

        return p;
      } 

    /* ****************************************
       What we do next depends on whether we're parsing
       an ordered or unordered fact. 
       **************************************** */

    boolean ordered = false;
    Deftemplate deft = m_engine.createDeftemplate(name);
    if (deft.getSlotIndex(RU.DEFAULT_SLOT_NAME) == 0)
      ordered = true;
    
    /* ****************************************
       Actual pattern slot data
       **************************************** */
    p = new Pattern(name, m_engine, negcnt);
    tok = m_jts.nextToken();
    while (ordered || tok.m_ttype == '(') 
      {
        
        if (!ordered)
          {
            if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
              parseError("parsePattern", "Bad slot name");
            slot = tok.m_sval;
            tok = m_jts.nextToken();
          }

        boolean multislot = (deft.getSlotType(deft.getSlotIndex(slot)) == RU.MULTISLOT);
        
        int subidx = (multislot ? 0 : -1);
        int nextConjunction = RU.AND;
        Test1 aTest = null;
        while (tok.m_ttype != ')') 
          {
            
            // if this is a '~'  pattern, keep track
            boolean not_slot = false;
            if (tok.m_ttype == '~') 
              {
                not_slot = true;
                tok = m_jts.nextToken();
              }
            
            switch (tok.m_ttype) 
              {
              case RU.VARIABLE: case RU.MULTIVARIABLE:
                // Fix type if necessary - lets you omit the '$' on
                // second and later occurrences of multivars.
                Integer type = (Integer) varnames.get(tok.m_sval);
                if (type == null)
                  varnames.put(tok.m_sval, new Integer(tok.m_ttype));
                else
                  tok.m_ttype = type.intValue();
                
                aTest = new Test1(not_slot ? Test.NEQ : Test.EQ, subidx,
                                  new Variable(tok.m_sval, tok.m_ttype));
                break;
                
              case RU.ATOM: 
                
                if (tok.m_sval.equals(":"))
                  {
                    Funcall f = parseFuncall();
                    aTest = new Test1(not_slot ? Test.NEQ : Test.EQ, subidx,
                                      new FuncallValue(f));
                    break;
                  }
                // FALL THROUGH                    
                
              case RU.STRING:
                aTest = new Test1(not_slot ? Test.NEQ : Test.EQ, subidx,
                                  new Value(tok.m_sval, tok.m_ttype));
                break;
                
              case RU.FLOAT:
              case RU.INTEGER:
                aTest = new Test1(not_slot ? Test.NEQ : Test.EQ, subidx,
                                  new Value(tok.m_nval, tok.m_ttype));
                break;
                
                // We're going to handle these by transforming them into
                // predicate constraints.
                
              case '=':
                {
                  Funcall inner = parseFuncall();
                  
                  // We're building (eq* <this-slot> <inner>)
                  Funcall outer = new Funcall("eq*", m_engine);
                  
                  // We need the variable that refers to this slot
                  Value var = null;
                  int idx = p.getDeftemplate().getSlotIndex(slot);
                  if (p.getNTests(idx) > 0)
                    {
                      Test1 t1 = p.getTest(idx, 0);
                      if (t1.getTest() == Test.EQ)
                        {
                          Value var2 = t1.getValue();
                          if (var2.type() == RU.VARIABLE && t1.m_subIdx == subidx)
                            var = var2;                          
                        }
                    
                    }
                  
                  if (var == null)
                    {
                      var = new Variable(RU.gensym("__jesp"), RU.VARIABLE);
                      p.addTest(slot, new Test1(Test.EQ, subidx, var));
                    }
                  
                  // Finish up the Funcall
                  outer.add(var);
                  outer.add(new FuncallValue(inner));
                  
                  aTest = new Test1(not_slot ? Test.NEQ : Test.EQ, subidx,
                                    new FuncallValue(outer));
                }
                break;
                
              default:
                parseError("parsePattern", "Bad slot value");
              }
            
            tok = m_jts.nextToken();

            aTest.m_conjunction = nextConjunction;
            
            if (tok.m_ttype == '&')
              tok = m_jts.nextToken();
            

            else if (tok.m_ttype == '|')
              {
                nextConjunction = RU.OR;
                tok = m_jts.nextToken();
              }

            else
              if (!multislot && tok.m_ttype != ')')
                parseError("parsePattern", slot + " is not a multislot");
              else
                {
                  ++subidx;
                  nextConjunction = RU.AND;
                }

            p.addTest(slot, aTest);
          }
        
        if (multislot)
          p.setMultislotLength(slot, subidx);
        
        if (!ordered)
          tok = m_jts.nextToken();
        else
          break;
        
      }
    return p;
        
  }

  /**
   * parseDefquery
   * synchronized to protect m_varnames
   * 
   * Syntax:
   * (defquery name
   * [ "docstring...." ]
   * [(declare (variables ?var1 ?var2 ...))]
   * (pattern))
   * @exception ReteException 
   * @return 
   */

  private synchronized Value parseDefquery() throws ReteException 
  {
    Defquery dr;
    JessToken tok, tok2;

    /* ****************************************
       '(defquery'
       **************************************** */

    if (  (m_jts.nextToken().m_ttype != '(') ||
          ! (m_jts.nextToken().m_sval.equals("defquery")) )
      parseError("parseDefquery", "Expected (defquery...");


    /* ****************************************
       defquery name, optional comment
       **************************************** */

    if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
      parseError("parseDefquery", "Expected defquery name");
    String name = tok.m_sval;

    String docstring = "";
    if ((tok = m_jts.nextToken()).m_ttype == RU.STRING) 
      {
        docstring = tok.m_sval;
        tok = m_jts.nextToken();
      }

    dr = new Defquery(name, docstring, m_engine);

    // check for variables declaration
    if (tok.m_ttype == '(')
      if ((tok2 = m_jts.nextToken()).m_ttype == RU.ATOM &&
          tok2.m_sval.equals("declare")) 
        {

          if ((tok2 = m_jts.nextToken()).m_ttype != '(' ||
              (tok2 = m_jts.nextToken()).m_ttype != RU.ATOM)
            {
              parseError("parseDefquery", "Expected (<atom>");
            }

          if (tok2.m_sval.equals("variables"))
            {
              tok2 = m_jts.nextToken();
              // We need at least one variable!
              if(tok2.m_ttype != RU.VARIABLE)
                parseError("parseDefquery", "Expected variable");

              do
                {
                  Variable v = new Variable(tok2.m_sval, tok2.m_ttype) ;
                  dr.addQueryVariable(v) ;
                  tok2 = m_jts.nextToken() ;
                }
              while(tok2.m_ttype == RU.VARIABLE) ;
            }
          else 
            parseError("parseDefquery", "Expected 'variables'");

          if (tok2.m_ttype != ')' || m_jts.nextToken().m_ttype != ')')
            parseError("parseDefrule", "Expected '))('");
          
          tok = m_jts.nextToken();          
        }
      else 
        { // head wasn't 'declare'
          m_jts.pushBack(tok2);
        }


    // **************************************************
    // We need to keep track of the type of each variable, since
    // CLIPS code lets you omit the second and later '$' before multivars.
    // This only matters when a multivar is actualy matched against, since
    // if the '$' is omitted, a TMF node won't get generated. We'll therefore
    // 'put the $'s back in' as needed. This table is shared across all
    // patterns. in a rule.
    // **************************************************

    m_varnames.clear();

    // now we're looking for just patterns

    while (tok.m_ttype == '(' || tok.m_ttype == RU.VARIABLE) 
      {
        String patternBinding = null;
        switch (tok.m_ttype) 
          {
          case RU.VARIABLE: 
            {
              // pattern bound to a variable
              // These look like this:
              // ?name <- (pattern 1 2 3)

              patternBinding = tok.m_sval;

              if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM ||
                  !tok.m_sval.equals("<-"))
                parseError("parseDefquery", "Expected '<-'");

              // FALL THROUGH
            }
          
          case '(': 
            {
              // pattern not bound to a var
              if (tok.m_ttype == '(')
                m_jts.pushBack(tok);
              Pattern p = parsePattern(0, m_varnames);

              // This is still an error for now
              if (p.getNegated() > 1)
                parseError("parseDefquery",
                           "Nested not CEs are not allowed yet.");

              if (patternBinding != null && p.getNegated() != 0)
                parseError("parseDefquery",
                           "'not' and 'test' CE's cannot be bound to variables");

              p.setBoundName(patternBinding);
              dr.addPattern(p);
              break;
            }
          } 
        tok = m_jts.nextToken();
      }

    if (tok.m_ttype != ')')
      parseError("parseDefquery", "Expected ')'");

    m_engine.addDefrule(dr);
    return Funcall.TRUE;
  }


  /**
   * parseDeffunction
   * 
   * Syntax:
   * (deffunction name ["doc-comment"] (<arg1><arg2...) ["doc-comment"]
   * (action)
   * value
   * (action))
   * @exception ReteException 
   * @return 
   */
  private Value parseDeffunction() throws ReteException 
  {
    Deffunction df;
    JessToken tok;

    /* ****************************************
       '(deffunction'
       **************************************** */

    if (  (m_jts.nextToken().m_ttype != '(') ||
          ! (m_jts.nextToken().m_sval.equals("deffunction")) )
      parseError("parseDeffunction", "Expected (deffunction...");


    /* ****************************************
       deffunction name
       **************************************** */

    if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
      parseError("parseDeffunction", "Expected deffunction name");
    String name = tok.m_sval;
    
    /* ****************************************
       optional comment
       **************************************** */

    String docstring = "";
    if ((tok = m_jts.nextToken()).m_ttype == RU.STRING) 
      {
        docstring = tok.m_sval;
        tok = m_jts.nextToken();
      }

    df = new Deffunction(name, docstring);

    /* ****************************************
       Argument list
       **************************************** */

    if (tok.m_ttype != '(') 
      parseError("parseDeffunction", "Expected '('");
    
    while ((tok = m_jts.nextToken()).m_ttype == RU.VARIABLE ||
           tok.m_ttype == RU.MULTIVARIABLE)
      df.addArgument(tok.m_sval, tok.m_ttype);

    if (tok.m_ttype != ')') 
      parseError("parseDeffunction", "Expected ')'");


    /* ****************************************
       optional comment
       **************************************** */

    if ((tok = m_jts.nextToken()).m_ttype == RU.STRING) 
      {
        df.setDocstring(tok.m_sval);
        tok = m_jts.nextToken();
      }

    /* ****************************************
       function calls and values
       **************************************** */

    while (tok.m_ttype != ')') 
      {
        if (tok.m_ttype == '(') 
          {
            m_jts.pushBack(tok);
            Funcall f = parseFuncall();
            df.addAction(f);
          }
        else 
          {
            switch (tok.m_ttype) 
              {
          
              case RU.ATOM: case RU.STRING:
                  df.addValue(new Value(tok.m_sval, tok.m_ttype)); break;

              case RU.VARIABLE: case RU.MULTIVARIABLE:
                df.addValue(new Variable(tok.m_sval, tok.m_ttype)); break;
          
              case RU.FLOAT:
              case RU.INTEGER:
                df.addValue(new Value(tok.m_nval, tok.m_ttype)); break;
          
              default:
                parseError("parseDeffunction", "Unexpected character");
              }
          }
        tok = m_jts.nextToken();
      }

    m_engine.addUserfunction(df);
    return Funcall.TRUE;
  }

  private Value parseAndExecuteFuncall(JessToken tok) throws ReteException
  {
    if (tok != null)
      m_jts.pushBack(tok);
    Funcall fc = parseFuncall();
    Context c = m_engine.getGlobalContext();
    return fc.execute(c);

  }

  /**
   * Make error reporting a little more compact.
   */
  private void parseError(String routine, String msg) throws ReteException 
  {
    try
      {
        ParseException p =  new ParseException("Jesp." + routine, msg);
        p.setLineNumber(m_jts.lineno());
        p.setProgramText(m_jts.toString());
        throw p;
      }
    finally
      {
        m_jts.clear();
      }

  }

}


/**
 */
class ParseException extends ReteException
{
  /**
   * @param s1 
   * @param s2 
   * @param s3 
   */
  ParseException(String s1, String s2) { super(s1, s2, ""); }
}


