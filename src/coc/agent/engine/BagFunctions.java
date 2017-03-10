package coc.agent.engine;

import java.util.*;
import java.io.*;

/**
 * User-defined functions for manipulating 'bags' of properties.
 * <P>
 * To use one of these functions from your programs, simply register the
 * package class in your Java mainline:
 *
 * <PRE>
 *    engine.addUserpackage(new coc.agent.engine.BagFunctions());
 * </pre>
 * 
 */

public class BagFunctions implements Userpackage, Serializable
{

  /**
   * Actually add the package to the Rete object.
   * @param engine The Rete object to add this package to.
   */

  public void add(Rete engine) 
  {
    engine.addUserfunction(new Bag());
  }
}

class Bag implements Userfunction, Serializable
{
  private Hashtable m_bags = new Hashtable();

  public String getName() { return "bag";}
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String command = vv.get(1).stringValue(context);

    // Create, destroy and find bags.

    if (command.equals("create"))
      {
        String name = vv.get(2).stringValue(context);
        Hashtable bag = (Hashtable) m_bags.get(name);
        if (bag == null)
          {
            bag = new Hashtable();
            m_bags.put(name, bag);
          }
        return new Value(bag);
      }
    else if (command.equals("delete"))
      {
        String name = vv.get(2).stringValue(context);
        m_bags.remove(name);
        return Funcall.TRUE;
      }
    else if (command.equals("find"))
      {
        String name = vv.get(2).stringValue(context);
        Hashtable bag = (Hashtable) m_bags.get(name);
        if (bag != null)
          return new Value(bag);
        else
          return Funcall.NIL;
      }
    else if (command.equals("list"))
      {
        ValueVector rv = new ValueVector();
        Enumeration e = m_bags.keys();
        while (e.hasMoreElements())
          rv.add(new Value( (String) e.nextElement(), RU.STRING));
        return new Value(rv, RU.LIST);
      }

    // Set, check and read properties of bags

    else if (command.equals("set"))
      {
        Hashtable bag = (Hashtable) vv.get(2).externalAddressValue(context);
        String name = vv.get(3).stringValue(context);
        Value val = vv.get(4).resolveValue(context);
        bag.put(name, val);
        return val;
      }
    else if (command.equals("get"))
      {
        Hashtable bag = (Hashtable) vv.get(2).externalAddressValue(context);
        String name = vv.get(3).stringValue(context);
        Value v = (Value) bag.get(name);
        if (v != null)
          return v;
        else
          return Funcall.NIL;
      }
    else if (command.equals("props"))
      {
        Hashtable bag = (Hashtable) vv.get(2).externalAddressValue(context);
        ValueVector rv = new ValueVector();
        Enumeration e = bag.keys();
        while (e.hasMoreElements())
          rv.add(new Value( (String) e.nextElement(), RU.STRING));
        return new Value(rv, RU.LIST);
      }
    
    else
      throw new ReteException("bag", "Unknown command", command);
  }
}
