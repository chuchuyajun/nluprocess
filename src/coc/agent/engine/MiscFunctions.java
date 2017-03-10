package coc.agent.engine;
import java.io.*;
import java.net.*;
import java.util.*;
import coc.agent.engine.factory.Factory;

/** **********************************************************************
 * Some miscellaneous functions.
 * <P>
 * To use one of these functions from Jess, simply register the
 * package class in your Java mainline:
 * <PRE>
 *     engine.addUserpackage(new MiscFunctions());
 * </PRE>
 * See the README for the list of functions in this package.
 * <P>
 ********************************************************************** */

public class MiscFunctions implements Userpackage, Serializable
{
  public void add(Rete engine) 
  {
    engine.addUserfunction(new SetStrategy());
    engine.addUserfunction(new JessSocket());
    engine.addUserfunction(new JessFormat());
    engine.addUserfunction(new JessSystem());
    engine.addUserfunction(new LoadPkg());
    engine.addUserfunction(new LoadFn());
    engine.addUserfunction(new Time());

    // These two are the same for now!
    engine.addUserfunction(new Build("build"));
    engine.addUserfunction(new Build("eval"));

    engine.addUserfunction(new ListFunctions());
    engine.addUserfunction(new RunQuery(RunQuery.RUN));
    engine.addUserfunction(new RunQuery(RunQuery.COUNT));
    engine.addUserfunction(new Agenda());

    engine.addUserfunction(new Bits(Bits.AND));
    engine.addUserfunction(new Bits(Bits.OR));
    engine.addUserfunction(new Bits(Bits.NOT));

    // setgen added by Win Carus (9.19.97)
    engine.addUserfunction(new Setgen());

    engine.addUserfunction(new ResetGlobals(ResetGlobals.SET));
    engine.addUserfunction(new ResetGlobals(ResetGlobals.GET));

    engine.addUserfunction(new EvalSalience(EvalSalience.SET));
    engine.addUserfunction(new EvalSalience(EvalSalience.GET));

    engine.addUserfunction(new SetNodeIndexing());
    engine.addUserfunction(new SetFactory());

    engine.addUserfunction(new JessLong());
  }
}

class SetStrategy implements Userfunction, Serializable
{
  public String getName() { return "set-strategy"; }
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    Strategy s = null;
    String name = vv.get(1).stringValue(context);
    
    try
      {
        s = (Strategy)
          Class.forName("coc.agent.engine." + name).newInstance();
      }
    catch(Throwable t)
      {
        try
          {
            s = (Strategy) Class.forName(name).newInstance();
          }
        catch (Throwable tt)
          {
            throw new ReteException("set-strategy", "Strategy class not found:", name);
          }
      }

    // return name of old strategy
    String rv = context.getEngine().setStrategy(s);
    return new Value(rv, RU.ATOM);
  }
}

class Build implements Userfunction, Serializable
{
  private String m_name;
  Build(String name) { m_name = name; }
  public String getName() { return m_name;}
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String argument = vv.get(1).stringValue(context);
    return context.getEngine().executeCommand(argument);
  }
}

class Bits implements Userfunction, Serializable
{
  private String m_name;
  final static String AND = "bit-and", OR = "bit-or", NOT = "bit-not";

  Bits(String name) { m_name = name; }
  public String getName() { return m_name;}
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    int rv = vv.get(1).intValue(context);

    if (m_name.equals(AND))
      {
        for (int i=2; i<vv.size(); i++)
          rv &= vv.get(i).intValue(context);
      }
    else if (m_name.equals(OR))
      {        
        for (int i=2; i<vv.size(); i++)
          rv |= vv.get(i).intValue(context);
      }
    else // not
      {        
        rv = ~rv;
      }
    return new Value(rv, RU.INTEGER);
  }
}



class ListFunctions implements Userfunction, Serializable
{
  public String getName() { return "list-function$";}
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    ValueVector rv = new ValueVector(100);

    Enumeration e = context.getEngine().listFunctions();
    while (e.hasMoreElements() )
      rv.add(new Value(((Userfunction) e.nextElement()).getName(), RU.ATOM));
    
    // Bubble-sort the names.
    int swaps;
    do 
      {
        swaps = 0;
        for (int i=0; i< rv.size() -1; i++)
          {
            Value v1 = rv.get(i);
            Value v2 = rv.get(i+1);
            if (v1.stringValue(context).compareTo(v2.stringValue(context)) > 0)
              {
                ++swaps;
                rv.set(v2, i);
                rv.set(v1, i +1);
              }
          }

      }
    while (swaps > 0);

    return new Value(rv, RU.LIST);
  }
}

class Agenda implements Userfunction, Serializable
{
  public String getName() { return "agenda";}
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    Rete r = context.getEngine();
    PrintWriter outStream = r.getOutStream();
    int i = 0;
    for (Enumeration e = r.listActivations(); e.hasMoreElements();)
      {
        Activation a = (Activation) e.nextElement();
        if (!a.isInactive())
          {
            outStream.println(a);
            ++i;
          }
      }
    outStream.print("For a total of ");
    outStream.print(i);
    outStream.println(" activations.");
    return Funcall.NIL;
  }
}

class JessSystem implements Userfunction, Serializable
{
  public String getName() { return "system";}
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    try
      {
        boolean async = false;
        int size = vv.size();
        if (vv.get(size - 1).stringValue(context).equals("&"))
            {
              async = true;
              --size;
            }

        String[] words = new String[size - 1];
        for (int i=1; i<size; i++)
          words[i-1] = vv.get(i).stringValue(context);
        Process p = Runtime.getRuntime().exec(words);
        Thread t1 = new ReaderThread(p.getInputStream(),
                                     context.getEngine().getOutStream());
        t1.start();
        Thread t2 = new ReaderThread(p.getErrorStream(),
                                     context.getEngine().getErrStream());
        t2.start();

        if (!async)
          {
            try
              {
                p.waitFor();
                t1.join();
                t2.join();
              }
            catch (InterruptedException ie)
              {
                /* Nothing */
              }
          }
        return new Value(p);
      }
    catch (IOException ioe)
      {
        throw new ReteException("system", vv.toStringWithParens(), ioe);
      }
    catch (SecurityException se)
      {
        throw new ReteException("system", vv.toStringWithParens(), se);
      }
  }

  // Read outputs of subprocess, send to terminal
  private class ReaderThread extends Thread
  {
    InputStream m_is;
    Writer m_os;
    
    ReaderThread(InputStream is, Writer os)
    {
      m_is = is;
      m_os = os;
      setDaemon(true);
    }
    
    public void run()
    {
      try
        {
          int i;
          while (true)
            {
              if ((i = m_is.read()) != -1)
                m_os.write((char) i);
              else
                break;
            }
        }
      catch (Exception e) { /* quietly exit */ }
    }
  }
}

class LoadPkg implements Userfunction, Serializable
{
  public String getName() { return "load-package";}
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String clazz = vv.get(1).stringValue(context);
    try
      {
        Userpackage up = (Userpackage) Class.forName(clazz).newInstance();
        context.getEngine().addUserpackage(up);
      }
    catch (ClassNotFoundException cnfe)
      {
        throw new ReteException("load-package", "Class not found", clazz);
      }
    catch (IllegalAccessException iae)
      {
        throw new ReteException("load-package", "Class is not accessible",
                                clazz);
      }
    catch (InstantiationException ie)
      {
        throw new ReteException("load-package", "Class cannot be instantiated",
                                clazz);
      }
    catch (ClassCastException cnfe)
      {
        throw new ReteException("load-package",
                                "Class must inherit from UserPackage", clazz);
      }
    return Funcall.TRUE;
  }
}

class LoadFn implements Userfunction, Serializable
{
  public String getName() { return "load-function";}
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String clazz = vv.get(1).stringValue(context);
    try
      {
        Userfunction uf = (Userfunction) Class.forName(clazz).newInstance();
        context.getEngine().addUserfunction(uf);
      }
    catch (ClassNotFoundException cnfe)
      {
        throw new ReteException("load-function", "Class not found", clazz);
      }
    catch (IllegalAccessException iae)
      {
        throw new ReteException("load-function", "Class is not accessible",
                                clazz);
      }
    catch (InstantiationException ie)
      {
        throw new ReteException("load-function",
                                "Class cannot be instantiated",
                                clazz);
      }
    catch (ClassCastException cnfe)
      {
        throw new ReteException("load-function",
                                "Class must inherit from UserFunction", clazz);
      }
      
    return Funcall.TRUE;
  }
}

class Time implements Userfunction, Serializable
{
  public String getName() { return "time";}

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return new Value( System.currentTimeMillis()/1000, RU.FLOAT );
  }
}

class JessSocket implements Userfunction, Serializable
{
  public String getName() { return "socket";}

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    String host = vv.get(1).stringValue(context);
    int port = vv.get(2).intValue(context);
    String router = vv.get(3).stringValue(context);

    try
      {
        Socket sock = new Socket(host, port);
        Rete engine = context.getEngine();
        engine.addInputRouter(router,
                              new InputStreamReader(sock.getInputStream()),
                              false);
        engine.addOutputRouter(router,
                               new PrintWriter(sock.getOutputStream()));
        return vv.get(3);
      }
    catch (IOException ioe)
      {
        throw new ReteException("socket", "I/O Exception", ioe);
      }
  }
}

class Setgen implements Userfunction, Serializable
{
  public String getName() { return "setgen" ;}

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    int i = vv.get( 1 ).intValue(context );
    RU.s_gensymIdx = (i > RU.s_gensymIdx) ? i : (RU.s_gensymIdx + 1);

    return Funcall.TRUE;
  }
}

class ResetGlobals implements Userfunction, Serializable
{
  public static final int SET=0, GET=1;
  private int m_cmd;

  public ResetGlobals(int cmd) { m_cmd = cmd; }
  public String getName()
  { return m_cmd == SET ? "set-reset-globals" : "get-reset-globals"; }
  
  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    switch (m_cmd)
      {
      case SET:
        {
          Value v = null;
          Value v1 = vv.get(1); 
          if (v1.equals(Funcall.NIL) || v1.equals(Funcall.FALSE))
            {
              context.getEngine().setResetGlobals(false);
              v = Funcall.FALSE;
            }
          else
            {
              context.getEngine().setResetGlobals(true);
              v = Funcall.TRUE;
            }        
          return v;
        }

      default:
        return context.getEngine().getResetGlobals() ?
          Funcall.TRUE : Funcall.FALSE;         
      }
  }
}

class EvalSalience implements Userfunction, Serializable
{
  public static final int SET=0, GET=1;
  private static final String [] s_values = {"when-defined", "when-activated",
                                             "every-cycle"};
  private int m_cmd;

  public EvalSalience(int cmd) { m_cmd = cmd; }
  public String getName()
  { return m_cmd == SET ? "set-salience-evaluation"
      : "get-salience-evaluation"; }
  
  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    switch (m_cmd)
      {
      case SET:
        {
          int old = context.getEngine().getEvalSalience();
          String s = vv.get(1).stringValue(context);
          for (int i=0; i<s_values.length; i++)
            if (s.equals(s_values[i]))
              {
                context.getEngine().setEvalSalience(i);
                return new Value(s_values[old], RU.ATOM);
              }          
          throw new ReteException("set-eval-salience", "Invalid value: " + s,
                                  "(valid values are when-defined, " +
                                  "when-activated, every-cycle)");
        }
      case GET:
      default: 
        {
          return new Value(s_values[context.getEngine().getEvalSalience()],
                           RU.ATOM);                    
        }
      }
  }
}

class SetNodeIndexing implements Userfunction, Serializable
{
  public String getName() { return "set-node-index-hash"; }
  
  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    int hash = vv.get(1).intValue(context);

    context.getEngine().getCompiler().setHashKey(hash);
    return Funcall.TRUE;
  }
}

class SetFactory implements Userfunction, Serializable
{
  public String getName() { return "set-factory"; }
  
  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    Factory newF = 
      (coc.agent.engine.factory.Factory) vv.get(1).externalAddressValue(context);

    Factory oldF = context.getEngine().getFactory();
    context.getEngine().setFactory(newF);
        
    return new Value(oldF);
  }
}

class RunQuery implements Userfunction, Serializable
{
  public static final int RUN=0, COUNT=1;
  private int m_cmd;
  RunQuery(int cmd) { m_cmd = cmd; }

  public String getName()
  { return m_cmd == RUN ? "run-query": "count-query-results"; }
  
  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    String queryName = vv.get(1).atomValue(context);
    HasLHS lhs = context.getEngine().findDefrule(queryName);
    if (lhs == null || ! (lhs instanceof Defquery))
      throw new ReteException("run-query", "No such query:", queryName);
    
    Defquery dq = (Defquery) lhs;
    Rete engine = context.getEngine();

    // Create the query-trigger fact
    Fact f = new Fact(RU.QUERY_TRIGGER + queryName, engine);
    ValueVector qv = new ValueVector();
    if ((vv.size() - 2) != dq.getNVariables())
      throw new ReteException("run-query", "Wrong number of variables for query", queryName);
    
    for (int i = 2; i< vv.size(); i++)
      qv.add(vv.get(i).resolveValue(context));

    f.setSlotValue(RU.DEFAULT_SLOT_NAME, new Value(qv, RU.LIST));

    // Assert the fact, blocking access to other queries; then return the
    // results, which clears the query
    synchronized(dq)
      {
        dq.clearResults();
        engine.assertFact(f);
        Value v;
        if (m_cmd == RUN)
          {
            Enumeration e = dq.getResults();
            v = new Value(e);
          }
        else
          v = new Value(dq.countResults(), RU.INTEGER);

        dq.clearResults();
        engine.retract(f);
        return v;
      }
  }
}

class JessFormat implements Userfunction, Serializable
{
  public String getName() { return "format" ;}

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    // individual formats in here
    StringBuffer fmtbuf = new StringBuffer(20);

    // The eventual return value
    StringBuffer outbuf = new StringBuffer(100);

    // The router where things go
    Value router = vv.get(1);

    // The format command string    
    String fmt = vv.get(2).stringValue(context);
    // an index into the above
    int ptr = 0;

    // index over arguments
    int validx = 3;

    char c = 0;
    while (ptr < fmt.length())
      {
        // Any leading non-format stuff
        while (ptr < fmt.length() && (c = fmt.charAt(ptr++)) != '%')
          outbuf.append(c);
        
        if (ptr >= fmt.length())
          break;

        // copy format to fmtbuf
        fmtbuf.setLength(0);
        fmtbuf.append(c);
        while (ptr < fmt.length() && (c = fmt.charAt(ptr++)) != '%'
                && !Character.isLetter(c))
          fmtbuf.append(c);
        if (c == 'n')
          {
            outbuf.append('\n');
            break;
          }
        else if (c == '%')
          {
            outbuf.append('%');
            break;
          }
        else
          fmtbuf.append(c);
        
        Format f = new Format(fmtbuf.toString());
        Value v;
        switch (f.fmt)
          {
          case 'd': case 'i': case 'o': case 'x': case 'X':
            v = vv.get(validx++);
            outbuf.append(f.form(v.intValue(context))); break;
          case 'f': case 'e': case 'E': case 'g': case 'G':
            v = vv.get(validx++);
            outbuf.append(f.form(v.floatValue(context))); break;
          case 'c':
            v = vv.get(validx++);
            switch (v.type())
              {
              case RU.ATOM: case RU.STRING:
                outbuf.append(f.form(v.stringValue(context).charAt(0))); break;
              default:
                outbuf.append(f.form((char) v.intValue(context))); break;
              }
            break;
          case 's':
            v = vv.get(validx++);                
            outbuf.append(f.form(v.stringValue(context))); break;
            
          default:
            throw new ReteException("format", "Unknown format", fmtbuf.toString());
          }
      }

    String s = outbuf.toString();
    if (!router.equals(Funcall.NIL))
      {
        String routerName = router.stringValue(context);
        Writer os = context.getEngine().getOutputRouter(routerName);
        if (os == null)
          throw new ReteException("format", "Bad router", routerName);
        try
          {
            for (int i=0; i<s.length(); i++)
              os.write(s.charAt(i));
            os.flush();
          }
        catch (IOException ioe)
          {
            throw new ReteException("format", "I/O Exception", ioe);
          }
      }

    return new Value(s, RU.STRING);
  }
}

class JessLong implements Userfunction, Serializable
{
  public String getName() { return "long"; }
  
  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    long l = 0;
    Value v = vv.get(1).resolveValue(context);
    switch (v.type())
      {
      case RU.STRING:
      case RU.ATOM:
        try
          {
            l = new Long(v.stringValue(context)).longValue();
            break;
          }
        catch (NumberFormatException nfe)
          {
            throw new ReteException("long", "Invalid number format", v.toString());
          }
      case RU.INTEGER:
      case RU.FLOAT:
      case RU.FACT_ID:
        l = (long) v.numericValue(context);
        break;
      default:
        throw new ReteException("long", "Illegal argument", v.toString());
      }

    return new LongValue(l);
  }
}



