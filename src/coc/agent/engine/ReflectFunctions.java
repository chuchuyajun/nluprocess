package coc.agent.engine;

import java.lang.reflect.*;
import java.beans.*;
import java.util.*;
import java.io.*;

/** **********************************************************************
 * Java Reflection for Jess.
 * <P>
 * This stuff is suprisingly powerful! Right now we don't handle
 * multi-dimensional arrays, but I think we don't miss anything else.
 * <P>
 ********************************************************************** */

public class ReflectFunctions implements Userpackage, Serializable
{
  /**
   * Actually add the package to the Rete object.
   * @param engine The Rete object to add this package to.
   */

  public void add(Rete engine) 
  {
    JessImport ji;
    engine.addUserfunction(ji=new JessImport());
    engine.addUserfunction(new JessNew(ji));
    engine.addUserfunction(new Engine());
    engine.addUserfunction(new FetchContext());
    engine.addUserfunction(new Call(ji));
    engine.addUserfunction(new JessField("set-member", ji));
    engine.addUserfunction(new JessField("get-member", ji));
    engine.addUserfunction(new Set(ji));
    engine.addUserfunction(new Get(ji));

    Defclass dc = new Defclass(ji);
    engine.addUserfunction(dc);
    engine.addJessListener(dc);

    Definstance di = new Definstance(engine, dc);
    engine.addUserfunction(di);
    engine.addJessListener(di);

    engine.addUserfunction(new UnDefinstance(di));

  }

  /**
   * ******************************
   * Return a Java argument derived from the Value which matches the
   * Class object as closely as possible. Throws an exception if no match.
   * ******************************
   */
  
  static Object valueToObject(Class clazz, Value value, Context context)
       throws IllegalArgumentException, ReteException
  {
    return valueToObject(clazz, value, context, true);
  }

  static Object valueToObject(Class clazz, Value value, Context context, boolean strict)
       throws IllegalArgumentException, ReteException
  {
    value = value.resolveValue(context);
    switch (value.type())
      {

      case RU.EXTERNAL_ADDRESS:
        {
          if (clazz.isInstance(value.externalAddressValue(context)))
            return value.externalAddressValue(context);
          else
            throw new IllegalArgumentException();
        }

      case RU.ATOM:
      case RU.STRING:
        {
          String s = value.stringValue(context);

          if (!clazz.isPrimitive() && s.equals(Funcall.NIL.stringValue(context)))
            return null;

          else if (clazz.isAssignableFrom(String.class))
            return s;

          else if (clazz == Character.TYPE)
            {
              if (s.length() == 1)
                return new Character (s.charAt(0));
              else
                throw new IllegalArgumentException();
            }

          else if (clazz == Boolean.TYPE)
            {
              if (s.equals(Funcall.TRUE.stringValue(context)))
                return Boolean.TRUE;
              if (s.equals(Funcall.FALSE.stringValue(context)))
                return Boolean.FALSE;
              else
                throw new IllegalArgumentException();
            }

          else
            throw new IllegalArgumentException();
        }
      
      case RU.LONG:
      case RU.INTEGER:
        {

          if (clazz == Long.TYPE || clazz == Long.class)
            return new Long(value.longValue(context));

          int i = value.intValue(context);

          if (clazz == Integer.TYPE || clazz == Integer.class)
            return new Integer(i);

          else if (clazz == Short.TYPE || clazz == Short.class)
            return new Short((short) i);

          else if (clazz == Character.TYPE || clazz == Character.class)
            return new Character( (char) i);

          else if (clazz == Byte.TYPE || clazz == Byte.class)
            return new Byte( (byte) i);

          else if (!strict && clazz == String.class)
            return String.valueOf(i);
          
          else
            throw new IllegalArgumentException();

        }
        
      case RU.FLOAT:
        {
          double d = value.floatValue(context);

          if (clazz == Double.TYPE || clazz == Double.class)
            return new Double(d);

          else if (clazz == Float.TYPE || clazz == Float.class)
            return new Float((float) d);

          else if (!strict && clazz == String.class)
            return String.valueOf(d);

          else
            throw new IllegalArgumentException();
          
        }

      // Turn lists into arrays.
      case RU.LIST:
        {
          if (clazz.isArray())
            {
              Class elemType = clazz.getComponentType();
              ValueVector vv = value.listValue(context);
              Object array = Array.newInstance(elemType, vv.size());
              for (int i=0; i<vv.size(); i++)
                Array.set(array, i,
                          valueToObject(elemType, vv.get(i), context, false));
              return array;
            }
          else
            throw new IllegalArgumentException();
        }
      default:
        throw new IllegalArgumentException();
      }
    
  }

  /**
   * ******************************
   * Create a Jess Value object out of a Java Object. Primitive types get
   * special treatment.
   * ******************************
   */
  
  static Value objectToValue(Class c, Object obj) throws ReteException
  {
    if (obj == null && !c.isArray())
      return Funcall.NIL;

    if (c == Void.class)
      return Funcall.NIL;

    if (obj instanceof Value)
      return (Value) obj;

    if (c == String.class || obj instanceof String)
      return new Value( obj.toString(), RU.STRING);

    if (c.isArray())
      {
        int length = 0;
        if (obj != null)
          length = Array.getLength(obj);
        ValueVector vv = new ValueVector(length);
        
        for (int i=0; i<length; i++)
          vv.add(objectToValue(c.getComponentType(), Array.get(obj, i)));
        
        return new Value(vv, RU.LIST);
      }

    if (c == Boolean.TYPE || obj instanceof Boolean)
      return ((Boolean) obj).booleanValue() ? Funcall.TRUE : Funcall.FALSE;

    if (c == Byte.TYPE || c == Short.TYPE ||
        c == Integer.TYPE ||
        obj instanceof Byte || obj instanceof Short ||
        obj instanceof Integer)
      return new Value (( (Number) obj).intValue(), RU.INTEGER);

    if (c == Long.TYPE || obj instanceof Long)
      return new LongValue(((Long) obj).longValue());
    
    if (c == Double.TYPE || c == Float.TYPE || obj instanceof Double ||
        obj instanceof Float)
      return new Value (( (Number) obj).doubleValue(), RU.FLOAT);

    if (c == Character.TYPE || obj instanceof Character)
      return new Value (obj.toString(), RU.ATOM);

    return new Value(obj);
  }

  private static Hashtable s_descriptors = new Hashtable();
  static PropertyDescriptor[] getPropertyDescriptors(Class c)
    throws ReteException, IntrospectionException
  {
    
    PropertyDescriptor[] pds;
    if ((pds = (PropertyDescriptor[]) s_descriptors.get(c)) != null)
      return pds;

    BeanInfo bi = Introspector.getBeanInfo(c);
    if (bi.getBeanDescriptor().getBeanClass() != c)
      throw new ReteException("ReflectFunctions.getPropertyDescriptors",
                              "Introspector returned bogus BeanInfo object for class ",
                              bi.getBeanDescriptor().getBeanClass().getName());
    
    pds = bi.getPropertyDescriptors();
    s_descriptors.put(c, pds);
    return pds;
  }
}

class Engine implements Userfunction, Serializable 
{
  public String getName() { return "engine";}

  public Value call(ValueVector vv, Context context) throws ReteException
  {
    return new Value(context.getEngine());
  }
}

class FetchContext implements Userfunction, Serializable 
{
  public String getName() { return "context";}

  public Value call(ValueVector vv, Context context) throws ReteException
  {
    return new Value(context);
  }
}

/**
 * **********************************************************************
 * Call a Java method from Jess. First argument is EITHER an external-address
 * object, or the name of a class. The latter works only for Static methods, of
 * course. Later arguments are the contructor arguments. We pick methods based
 * on a first-fit algorithm, not necessarily a best-fit. If you want to be super
 * selective, you can disambiguate by wrapping basic types in object wrappers.
 * If it absolutely won't work, well, you can always write a Java Userfunction
 * as a wrapper!
 * **********************************************************************
 */

class Call implements Userfunction, Serializable 
{

  String m_name = "call";

  public String getName() { return m_name; }
  
  private JessImport m_ji;
  Call(JessImport ji) { m_ji = ji; }


  private static Hashtable s_methods = new Hashtable();
  static Method[] getMethods(Class c)
  {
    if (s_methods.get(c) != null)
      return (Method[]) s_methods.get(c);
    else
      {
        Method[] m = c.getMethods();
        s_methods.put(c, m);
        return m;
      }
  }


  public Value call(ValueVector vv, Context context) throws ReteException
  {

    String method = vv.get(2).stringValue(context);

    Class c = null;
    try
      {
        Object target = null;

        Value v = vv.get(1).resolveValue(context);
        if (v.type() == RU.STRING || v.type() == RU.ATOM)
          {
            try
              {
                c = m_ji.findClass(v.stringValue(context));
              }
            catch (Exception cnfe)
              {
                // Maybe we're supposed to call the method
                // on the string object itself...
              }
          }
        if (c == null)
          {
            target = v.externalAddressValue(context);
            c = target.getClass();
          }        

        /*
         * Build argument list
         */
        
        int nargs = vv.size() - 3;
        Object args[] = new Object[nargs];

        Method [] methods = Call.getMethods(c);
        Object rv = null;
        int i;
        for (i=0; i< methods.length; i++)
          {
            try
              {
                Method m = methods[i];
                Class[] argTypes = m.getParameterTypes();
                if (!m.getName().equals(method) || nargs != argTypes.length)
                  continue;
                
                // OK, found a method. Problem is, it might be a public
                // method of a private class. We'll check for this, and
                // if so, we have to find a more appropriate method
                // descriptor. Can't believe we have to do this.

                if (!Modifier.isPublic(c.getModifiers()))
                  {
                    m = null;
                  escape:
                    while (c != null)
                      {
                        Class [] interfaces = c.getInterfaces();
                        for (int ii =0; ii< interfaces.length; ii++)
                          {
                            try
                              {
                                m = interfaces[ii].getMethod(method, argTypes);
                                break escape;
                              }
                            catch (NoSuchMethodException nsme) {}
                          }
                        c = c.getSuperclass();
                        if (c != null && Modifier.isPublic(c.getModifiers()))
                          {
                            try
                              {
                                m = c.getMethod(method, argTypes);
                                break escape;
                              }
                            catch (NoSuchMethodException nsme) {} 
                          }
                      }
                    if (m == null)
                      throw new ReteException("call",
                                              "Method not accessible",
                                              method);
                  }

                // Now give it a try!
                              
                for (int j=0; j<nargs; j++)
                  {
                    args[j]                    
                      = ReflectFunctions.valueToObject(argTypes[j],
                                                       vv.get(j+3),
                                                       context);
                    
                  }

                

                rv = m.invoke(target, args);

                return ReflectFunctions.objectToValue(m.getReturnType(), rv);

              }
            catch (IllegalArgumentException iae)
              {
                // Try the next one!
              }
          }

        throw new NoSuchMethodException(method);

      }
    catch (NoSuchMethodException nsm)
      {
        if (!hasMethodOfName(c, method))
            throw new ReteException("call", "No method named '" + method + "' found",
                                    "in class " + c.getName());

        else          
          throw new ReteException("call", "No overloading of method '" + method + "'",
                                  "in class " + c.getName() +
                                  " I can call with these arguments: " +
                                  vv.toStringWithParens());
        
      }
    catch (InvocationTargetException ite)
      {
        throw new ReteException("call", "Called method threw an exception",
                                ite.getTargetException()); 
      }
    catch (IllegalAccessException iae)
      {
        throw new ReteException("call", "Method is not accessible", iae);
      }
    catch (IllegalArgumentException iae)
      {
        throw new ReteException("call", "Invalid argument to " + method, iae);
      }
  }

  private boolean hasMethodOfName(Class c, String name)
  {
    try
      {
        Method[] m = Call.getMethods(c);
        for (int i=0; i<m.length; i++)
          if (m[i].getName().equals(name))
            return true;
        return false;
      }
    catch (Exception e)
      {
        return false;
      }
  }

}

class Set extends Call
{
  Set(JessImport ji) { super(ji); m_name = "set";}

  public Value call(ValueVector vv, Context context) throws ReteException
  {
    try
      {
        // Pass this along to 'call'
        // We can't self-modify since we're not copied
        Funcall f = new Funcall("call", context.getEngine());
        for (int i=1; i < vv.size(); i++)
          f.arg(vv.get(i).resolveValue(context));
                
        String propName = f.get(2).stringValue(context);

        PropertyDescriptor [] pd =
          ReflectFunctions.getPropertyDescriptors(f.get(1).externalAddressValue(context).getClass());

        for (int i=0; i<pd.length; i++)
          {
            Method m = null;
            if (pd[i].getName().equals(propName) &&
                (m = pd[i].getWriteMethod()) != null)
              {
                f.set(new Value(m.getName(), RU.STRING), 2);          
                return super.call(f, context);
              }
          }
        throw new ReteException("set", "No such property:", propName);
      }
    catch (IntrospectionException ie)
      {
        throw new ReteException("set", "Introspection Error:", ie);
      }
  }
}

class Get extends Call
{
  Get(JessImport ji) { super(ji); m_name = "get";}
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    try
      {
        // Pass this along to 'call'
        // We can't self-modify since we're not copied
        Funcall f = new Funcall("call", context.getEngine());
        for (int i=1; i < vv.size(); i++)
          f.arg(vv.get(i).resolveValue(context));

        String propName = vv.get(2).stringValue(context);
        // note that these are cached, so all the introspection
        // only gets done once.
        PropertyDescriptor [] pd =
          ReflectFunctions.getPropertyDescriptors(f.get(1).externalAddressValue(context).getClass());
        for (int i=0; i<pd.length; i++)
          {
            Method m = null;
            if (pd[i].getName().equals(propName) &&
                (m = pd[i].getReadMethod()) != null)
              {
                f.set(new Value(m.getName(), RU.STRING), 2);          
                return super.call(f, context);
              }
          }
        throw new ReteException("get", "No such property:", propName);
      }
    catch (IntrospectionException ie)
      {
        throw new ReteException("get", "Introspection Error", ie);
      }
  }
}

class JessImport implements Userfunction, Serializable
{
  private Hashtable m_specific = new Hashtable();
  private Vector m_general = new Vector();

  public String getName() { return "import";}
  
  JessImport() { m_general.addElement("java.lang."); }
  
  Class findClass(String clazz) throws ClassNotFoundException
  {
    if (clazz.indexOf(".") == -1)
      {
        String s = (String) m_specific.get(clazz);
        if (s != null)
          clazz = s;

        else
          {
            for (Enumeration e=m_general.elements(); e.hasMoreElements();)
              {
                s = ((String) e.nextElement()) + clazz;
                try
                  {
                    Class c = Class.forName(s);
                    m_specific.put(clazz, s);
                    return c;
                  }
                catch (ClassNotFoundException ex) { /* Just try again */ }
              }
          }
      }        
    return Class.forName(clazz);
  }

  public Value call(ValueVector vv, Context c) throws ReteException
  {
    String arg = vv.get(1).atomValue(c);
    if (arg.indexOf("*") != -1)
      m_general.addElement(arg.substring(0, arg.indexOf("*")));

    else 
      m_specific.put(arg.substring(arg.lastIndexOf(".")+1, arg.length()),
                     arg);
    return Funcall.TRUE;
  }

}

/**
 * **********************************************************************
 * Create a Java object from Jess
 * The first argument is the full-qualified typename; later arguments are
 * the contructor arguments.  We pick methods based on a first-fit algorithm,
 * not necessarily a best-fit. If you want to be super selective, you can
 * disambiguate by wrapping basic types in object wrappers.
 * **********************************************************************
 */

class JessNew implements Userfunction, Serializable 
{
  JessImport m_ji;

  JessNew(JessImport ji) { m_ji = ji; }

  public String getName() { return "new";}
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    try
      {
        /*
          Find target class
        */

        String clazz = vv.get(1).stringValue(context);
        Class c = m_ji.findClass(clazz);

        
        ValueVector resolved = new ValueVector();
        for (int i=2; i<vv.size(); i++)
          resolved.add(vv.get(i).resolveValue(context));


        /*
         * Build argument list
         */

        int nargs = vv.size() - 2;
        Object args[] = new Object[nargs];

        Constructor [] cons = c.getConstructors();
        Object rv = null;
        int i;
        for (i=0; i< cons.length; i++)
          {
            try
              {
                Constructor constructor = cons[i];
                Class[] argTypes = constructor.getParameterTypes();
                if (nargs != argTypes.length)
                  continue;

                // Otherwise give it a try!
                for (int j=0; j<nargs; j++)
                  {                    
                    args[j]
                      = ReflectFunctions.valueToObject(argTypes[j],
                                                       resolved.get(j),
                                                       context);
                  }

                rv = constructor.newInstance(args);
                return new Value(rv);

              }
            catch (IllegalArgumentException iae)
              {
                // Try the next one!
              }
          }

        throw new NoSuchMethodException(c.getName());       

      }
    catch (InvocationTargetException ite)
      {
        throw new ReteException("new", "Constructor threw an exception",
                                ite.getTargetException());
      }
    catch (NoSuchMethodException nsm)
      {
        throw new ReteException("new", "Constructor not found: " + vv.toStringWithParens(),
                                nsm);
      }
    catch (ClassNotFoundException cnfe)
      {
        throw new ReteException("new", "Class not found", cnfe);
      }
    catch (IllegalAccessException iae)
      {
        throw new ReteException("new", "Class or constructor is not accessible", iae);
      }
    catch (InstantiationException ie)
      {
        throw new ReteException("new", "Class cannot be instantiated", ie);
      }
  }
}

/**
 * **********************************************************************
 * Set or get a data member of a Java object from Jess
 * **********************************************************************
 */

class JessField implements Userfunction, Serializable 
{

  private String m_name;
  public String getName() { return m_name; }
  
  private JessImport m_ji;
  JessField(String functionName, JessImport ji)
  {
    // name should be get-member or set-member
    m_name = functionName;
    m_ji = ji;
  }


  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String field = vv.get(2).stringValue(context);

    boolean doSet = false;

    if (vv.get(0).stringValue(context).equals("set-member"))
      doSet = true;
    
    Class c = null;
    Object target = null;
    
    Value v = vv.get(1).resolveValue(context);

    if (v.type() == RU.STRING || v.type() == RU.ATOM)
      {
        try
          {
            c = m_ji.findClass(v.stringValue(context));
          }
        catch (ClassNotFoundException ex)
          {
            throw new ReteException(vv.get(0).stringValue(context),
                                    "No such class",
                                    v.stringValue(context));
          }
      }
    if (c == null)
      {
        target = v.externalAddressValue(context);
        c = target.getClass();
      }        

    try
      {        
        Field f = c.getField(field);
        Class argType = f.getType();
        if (doSet)
          {
            Value v2 = vv.get(3).resolveValue(context);
            f.set(target, ReflectFunctions.valueToObject(argType, v2, context));
            return v2;
          }
        else
          {
            Object o = f.get(target);
            return ReflectFunctions.objectToValue(argType, o);
          }
      }
    catch (NoSuchFieldException nsfe)
      {
        throw new ReteException(vv.get(0).stringValue(context),
                                "No such field " + field +
                                " in class ", c.getName());
      }

    catch (IllegalAccessException iae)
      {
        throw new ReteException(vv.get(0).stringValue(context),
                                "Field is not accessible",
                                field);
      }
    catch (IllegalArgumentException iae)
      {
        throw new ReteException(vv.get(0).stringValue(context),
                                "Invalid argument",
                                vv.get(1).toString());
      }
  }
}

/**
 * **********************************************************************
 * Tell Jess to match on properties of a specific Java object
 * **********************************************************************
 */

class Definstance implements Userfunction, Serializable, PropertyChangeListener, JessListener
{
  Defclass m_defclass;
  Rete m_engine;

  Definstance(Rete engine, Defclass dc)
  {
    m_engine = engine;
    m_defclass = dc;
  }
 
  /**
   * Return the name of this command
   * @return The command name
   */
  public String getName() { return "definstance";}
  
  // Keys are objects to match, elements are the facts that represent them.
  private Hashtable m_facts = new Hashtable(101);
  
  // Keys are objects to match, elements are the Jess class names
  private Hashtable m_jessClasses = new Hashtable(101);

  Enumeration listDefinstances() { return m_facts.keys(); }

  Value undefine(Object o) throws ReteException
  {
    Fact f = (Fact) m_facts.get(o);
    if (f != null)
      m_engine.retract(f);
    m_facts.remove(o);

    try
      {
        Method apcl = o.getClass().getMethod("removePropertyChangeListener",
                                             new Class[] { PropertyChangeListener.class });    
        apcl.invoke( o, new Object[] { this });
      }
    catch (Exception e) { /* whatever */ }

    if (m_jessClasses.remove(o) == null)
      return Funcall.FALSE;
    else
      return Funcall.TRUE;
  }

  public void eventHappened(JessEvent je) throws ReteException
  {
    switch ((int) je.getType())      
      {
      case (int) JessEvent.RESET:
        {
          Enumeration e = m_facts.keys();
          while (e.hasMoreElements())
            createFact(e.nextElement(), null, null, null);

          break;
        }
      case (int) JessEvent.CLEAR:
        m_facts.clear();
        m_jessClasses.clear();      
       break;
 
      default:
        // NOTHING
      }
  }
  
  /**
   * SYNTAX: (definstance <jess-classname> <external-address> [static | dynamic])
   */
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    try
      {
        String jessTypename = vv.get(1).stringValue(context);

        if (m_defclass.jessNameToJavaName(jessTypename) == null)
          throw new ReteException("definstance", "Unknown object class",
                                  jessTypename);

        Value v = vv.get(2).resolveValue(context);

        if (v.equals(Funcall.NIL))
          throw new ReteException("definstance", "Argument is nil:", v.toString()); 

        // Fetch the object
        Object o = v.externalAddressValue(context);

        // Make sure we're not already matching on this object
        if (m_facts.get(o) != null)
          return new Value(-1, RU.FACT_ID);

        String javaTypename = m_defclass.jessNameToJavaName(jessTypename);
        
        if (!Class.forName(javaTypename).isAssignableFrom(o.getClass()))
          throw new ReteException("definstance", "Object is not instance of",
                                  javaTypename);
    
        if (vv.size() < 4 || vv.get(3).equals("dynamic"))
          {
            // Add ourselves to the object as a PropertyChangeListener 
            
            Class pcl = Class.forName("java.beans.PropertyChangeListener");
            Method apcl = o.getClass().getMethod("addPropertyChangeListener",
                                                 new Class[] { pcl });    
            apcl.invoke( o, new Object[] { this });
          }

        m_jessClasses.put(o, jessTypename);
        int fid = createFact(o, null, null, context);

        return new Value(fid, RU.FACT_ID);
      }
    catch (InvocationTargetException ite)
      {
        throw new ReteException("definstance",
                                "Cannot add PropertyChangeListener",
                                ite.getTargetException());
      }
    catch (NoSuchMethodException nsm)
      {
        throw new ReteException("definstance",
                                "Obj doesn't accept PropertyChangeListeners",
                                nsm);
      }
    catch (ClassNotFoundException cnfe)
      {
        throw new ReteException("definstance", "Class not found", cnfe);
      }
    catch (IllegalAccessException iae)
      {
        throw new ReteException("definstance",
                                "Class or method is not accessible",
                                iae);
      }
  }
  
  private synchronized int createFact(Object o, String changedName, Object newValue,
                                       Context context) 
       throws ReteException
  {
    // Synthesize a fact for this object; remember it for later retraction
    Fact fact = (Fact) m_facts.get(o);
    boolean doGetValue = (newValue == null || changedName == null);
            
    try
      {

        if (fact == null)
          {
            fact = new Fact((String) m_jessClasses.get(o), m_engine);
            fact.setSlotValue("OBJECT", new Value(o));
            fact.setShadow(true);
            m_facts.put(o,fact);
          }
        else
          {
            m_engine.retract(fact);
          }

        
        Deftemplate deft = fact.getDeftemplate();
        Object [] args = new Object[] {};
        boolean changedSomething = false;
        for (int i=0; i<deft.getNSlots(); i++)
          {
            if (deft.getSlotName(i).equals("OBJECT"))
              continue;
            SerializablePropertyDescriptor pd = (SerializablePropertyDescriptor)
              deft.getSlotDefault(i).externalAddressValue(context);
            String name = pd.getName();

            // changedName is null if multiple props changed or
            // completely new fact is desired
            if (changedName != null && !name.equals(changedName))
              continue;
            
            Method m = pd.getReadMethod();
            Class rt = m.getReturnType();

            if (doGetValue)
              newValue = m.invoke(o, args);
           
            Value oldV = fact.getSlotValue(name);
            
            Value newV = ReflectFunctions.objectToValue(rt, newValue);
            if (!oldV.equals(newV))
              {
                fact.setSlotValue(name, newV);
              }

            changedSomething = true;            
          }      
        if (changedName != null && !changedSomething)
          throw new ReteException("Definstance.createFact", "Property not found:",
                                  changedName);
        
      }
    catch (InvocationTargetException ite)
      {
        throw new ReteException("Definstance.createFact", "Called method threw an exception",
                                ite.getTargetException()); 
      }
    catch (IllegalAccessException iae)
      {
        throw new ReteException("Definstance.createFact", "Method is not accessible",
                                iae);
      }
    catch (IllegalArgumentException iae)
      {
        throw new ReteException("Definstance.createFact", "Invalid argument", iae);
      }
    finally
      {
        // Regardless of what happens, we need to assert this now, since
        // retracted it before!
        return m_engine.assertFact(fact);        
      }
  }
  
  
  public synchronized void propertyChange(PropertyChangeEvent pce)
  {
    Object o = pce.getSource();
    
    try
      {
        String s = (String) m_jessClasses.get(o);
        if (s != null)
          createFact(o, pce.getPropertyName(), pce.getNewValue(), null);
        
      }
    catch (ReteException re)
      {
        System.out.println("Async Error: " + re);
        if (re.getNextException() != null)
          re.getNextException().printStackTrace();
      }
  }

}

/**
 */
class UnDefinstance implements Userfunction, Serializable
{
  private Definstance m_di;
  
  UnDefinstance(Definstance di)
  {
    m_di = di;
  }
 
  public String getName() { return "undefinstance";}
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    Value v = vv.get(1).resolveValue(context);
    if (v.type() == RU.EXTERNAL_ADDRESS)
      return m_di.undefine(v.externalAddressValue(context));
    else if (v.equals("*"))
      {
        for (Enumeration e = m_di.listDefinstances(); e.hasMoreElements();)
          m_di.undefine(e.nextElement());
        return Funcall.TRUE;
      }
    else
      throw new ReteException("undefinstance", "Invalid argument", v.toString());
  }
}

/**
 * We use this in deftemplates so that we can serialize an engine containing
 * defclasses.
 */

class SerializablePropertyDescriptor implements Serializable
{
  private String m_class, m_property;
  private transient Method m_get, m_set;

  SerializablePropertyDescriptor(Class c, PropertyDescriptor pd)
  {
    m_class = c.getName();
    m_property = pd.getName();
    m_get = pd.getReadMethod();
    m_set = pd.getWriteMethod();
  }

  private void reload() throws ReteException
  {
    try
      {
        Class c = Class.forName(m_class);
        PropertyDescriptor[] pd = ReflectFunctions.getPropertyDescriptors(c);
        for (int i=0; i<pd.length; i++)
          if (pd[i].getName().equals(m_property))
            {
              m_get = pd[i].getReadMethod();
              m_set = pd[i].getWriteMethod();
              return;
            }
        
      }
    catch (Exception e)
      {
        throw new ReteException("SerializablePropertyDescriptor.reload",
                                "Can't recreate property", e);
      }
  }

  String getName() { return m_property; }


  Method getReadMethod() throws ReteException
  {
    if (m_get == null)
      reload();
    return m_get;
  }

  Method getWriteMethod() throws ReteException
  {
    if (m_set == null)
      reload();
    return m_set;
  }    
}
        
/** **********************************************************************
 * Tell Jess to prepare to match on properties of a Java class
 * Generates a deftemplate from the class
 * 
 ********************************************************************** */

class Defclass implements Userfunction, Serializable, JessListener
{
  private JessImport m_ji;

  Defclass(JessImport ji) { m_ji = ji; }

  public String getName() { return "defclass";}
  
  // Keys are Jess class names; elements are the Java class names
  private Hashtable m_javaClasses = new Hashtable(101);


  public void eventHappened(JessEvent je) throws ReteException
  {
    if (je.getType() == JessEvent.CLEAR)
      m_javaClasses.clear();
  }

  String jessNameToJavaName(String s) { return (String) m_javaClasses.get(s); }

  /**
   * SYNTAX: (defclass <jess-classname> <Java-classname>)
   */
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String jessName = vv.get(1).stringValue(context);
    String clazz = vv.get(2).stringValue(context);
    String parent = vv.size() > 4 ? vv.get(4).stringValue(context) : null;
    if (parent != null && !vv.get(3).equals("extends"))
      throw new ReteException("defclass",
                              "expected 'extends <classname>'",
                              vv.get(3).toString());
    try
      {
        Deftemplate dt;
        if (parent != null)
          {          
            dt = context.getEngine().findDeftemplate(parent);
            if (dt == null) 
              throw new ReteException("defclass",
                                      "extended template does not exist: ",
                                      parent);
            dt = new Deftemplate(jessName, "$JAVA-OBJECT$ " + clazz, dt);
          }
        else
          dt = new Deftemplate(jessName, "$JAVA-OBJECT$ " + clazz);
        
        Class c = m_ji.findClass(clazz);
        m_javaClasses.put(jessName, c.getName());
        
        // Make all the readable 'bean properties' into slots
        PropertyDescriptor [] props = ReflectFunctions.getPropertyDescriptors(c);

        // Sort them first
        for (int i=0; i<props.length-1; i++)
          for (int j=i+1; j<props.length; j++)
            if (props[i].getName().compareTo(props[j].getName()) > 0)
              {
                PropertyDescriptor temp = props[i];
                props[i] = props[j];
                props[j] = temp;
              }


        // TODO: should set proper slot types
        for (int i=0; i<props.length; i++)
          {
            Method m = props[i].getReadMethod();
            if (m == null) continue;
            String name = props[i].getName();
            Class rt = m.getReturnType();
            if (rt.isArray())
              dt.addMultiSlot(name,
                              new Value(new SerializablePropertyDescriptor(c, props[i])));
            else
              dt.addSlot(name,
                         new Value(new SerializablePropertyDescriptor(c, props[i])), "ANY");
          }
        
        // Last slot is special - it holds the active instance
        dt.addSlot("OBJECT", Funcall.NIL, "OBJECT");

        // Install our synthetic deftemplate
        context.getEngine().addDeftemplate(dt);

        return new Value(c.getName(), RU.ATOM);
      }
    catch (ClassNotFoundException cnfe)
      {
        throw new ReteException("defclass", "Class not found:", cnfe);
      }
    catch (IntrospectionException ie)
      {
        throw new ReteException("defclass", "Introspection error:", ie);
      }
  }
}
