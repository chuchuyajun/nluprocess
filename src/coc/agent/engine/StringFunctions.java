
package coc.agent.engine;
import java.io.*;

/** **********************************************************************
 * Implements String handling functions.
 * <P>
 */
public class StringFunctions implements Userpackage, Serializable
{

  /**
   * Add these functions to a Rete object
   * @param engine The Rete object
   */
  public void add( Rete engine )
  {
	engine.addUserfunction( new StrMerge() );
    engine.addUserfunction( new StrCat( ) );
    engine.addUserfunction( new StrCompare( ) );
    engine.addUserfunction( new StrIndex( ) );
    engine.addUserfunction( new SubString( ) );
    engine.addUserfunction( new StrSimple(StrSimple.LENGTH) );
    engine.addUserfunction( new StrSimple(StrSimple.UPCASE) );
    engine.addUserfunction( new StrSimple(StrSimple.LOWCASE) );
    engine.addUserfunction( new GetTokenIndex( ));
    engine.addUserfunction( new LowerCase( ));
  }
}

//The return value ignore "
class StrMerge implements Userfunction, Serializable
{
  public String getName() { return "str-merge" ;}

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    Value v = vv.get(1).resolveValue(context);
    
    if (vv.size() == 2 && v.type() == RU.STRING)
	{
		return v;
	}

    StringBuffer buf = new StringBuffer( "" );
      
    for ( int i = 1; i < vv.size( ); i++ )
      {
        v = vv.get(i).resolveValue(context);
		if (v.type() == RU.STRING)
			buf.append( v.stringValue(context));
        else
			buf.append (v.toString());
      }
      
    return new Value( buf.toString(), RU.DT_DFLT_DATA );
      
  }
}

class StrCat implements Userfunction, Serializable
{
  public String getName() { return "str-cat" ;}

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    Value v = vv.get(1).resolveValue(context);
    
    if (vv.size() == 2 && v.type() == RU.STRING)
	{
		return v;
	}

    StringBuffer buf = new StringBuffer( "" );
      
    for ( int i = 1; i < vv.size( ); i++ )
      {
        v = vv.get(i).resolveValue(context);
		if (v.type() == RU.STRING)
			buf.append( v.stringValue(context));
        else
			buf.append (v.toString());
      }
      
    return new Value( buf.toString(), RU.STRING );
      
  }
}

class GetTokenIndex implements Userfunction, Serializable
{
  public String getName() { return "getTokenIndex" ;}

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    Value v = vv.get(1).resolveValue(context);

    StringBuffer buf = new StringBuffer( "" );
    
    int index = v.stringValue(context).lastIndexOf("_");
    String value = v.stringValue(context).substring(index+1);
      
    return new Value( value, RU.STRING );
    
  }
}

class LowerCase implements Userfunction, Serializable
{
  public String getName() { return "lowercase" ;}

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
	  return new Value(vv.get(1).stringValue(context).toLowerCase(), RU.DT_DFLT_DATA);
  }
}


class StrCompare implements Userfunction, Serializable
{
  public String getName() { return "str-compare" ;}

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return new Value( vv.get( 1 ).stringValue(context ).compareTo( vv.get( 2 ).stringValue(context ) ), RU.INTEGER );
  }
}

class StrIndex implements Userfunction, Serializable
{
  public String getName() { return "str-index" ;}

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    int rv = vv.get( 2 ).stringValue(context ).indexOf( vv.get( 1 ).stringValue(context ) );
    return rv == -1 ? Funcall.FALSE : new Value( rv + 1, RU.INTEGER );
  }
}

class SubString implements Userfunction, Serializable
{
  public String getName() { return "sub-string" ;}
  
  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    int begin = (int) vv.get( 1 ).numericValue(context ) -1;
    int end = (int) vv.get( 2 ).numericValue(context );
    String s = vv.get( 3 ).stringValue(context );

    if (begin < 0 || begin > s.length() - 1 ||
        end > s.length() || end <= 0)
      throw new ReteException("sub-string",
                              "Indices must be between 1 and " +
                              s.length(), "");
    return new Value(s.substring(begin, end), RU.STRING); 
  }
}

class StrSimple implements Userfunction, Serializable
{
  public static final int LENGTH=0, UPCASE=1, LOWCASE=2;
  public static final String m_names[] = {"str-length", "upcase", "lowcase"};
  
  private int m_name;
  StrSimple(int n) { m_name = n; }

  public String getName() { return m_names[m_name] ;}

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    switch(m_name)
      {
      case UPCASE:
        return new Value(vv.get(1).stringValue(context).toUpperCase(), RU.STRING );
      case LOWCASE:
        return new Value(vv.get(1).stringValue(context).toLowerCase(), RU.STRING );
      case LENGTH:
        return new Value(vv.get(1).stringValue(context).length(), RU.INTEGER );
      default:
        return Funcall.NIL;
      }
  }
}

