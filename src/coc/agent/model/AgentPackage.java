//## //## Source file:  c:/coc/agent/model/AgentPackage.java
//## //## Subsystem:  coc.agent.model
//## //## Module: AgentPackage

//##begin module.cm preserve=no
/*   %X% %Q% %Z% %W% */
//##end module.cm

//##begin module.cp preserve=no
//##end module.cp

package coc.agent.model;

//##begin module.additionalImports preserve=no
//##end module.additionalImports

//##begin module.imports preserve=yes


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import coc.Agent;
import coc.agent.engine.Context;
import coc.agent.engine.Funcall;
import coc.agent.engine.PredFunctions;
import coc.agent.engine.RU;
import coc.agent.engine.Rete;
import coc.agent.engine.ReteException;
import coc.agent.engine.StringFunctions;
import coc.agent.engine.Userfunction;
import coc.agent.engine.Userpackage;
import coc.agent.engine.Value;
import coc.agent.engine.ValueVector;

public class AgentPackage implements Userpackage
{
    private Rete m_rete;
    private Agent m_agent;

    public void add( Rete rete )
    {
        m_rete = rete;
        initBasicObjects();
    }

    public AgentPackage( Agent agent )
    {
        m_agent = agent;
    }

    public void initBasicObjects()
    {
        m_rete.addUserfunction( new StartsWith() );
        m_rete.addUserfunction( new StrNum() );
        m_rete.addUserfunction( new NumStr() );
        m_rete.addUserfunction( new IS() );
        m_rete.addUserfunction( new SubString() );
		m_rete.addUserfunction( new SubLastString() );
		m_rete.addUserfunction( new ReplaceString() );
		m_rete.addUserfunction( new NLUTest() );
		m_rete.addUserfunction( new KnownUnitType() );
		m_rete.addUserfunction( new Regexp() );
		
        new PredFunctions().add( m_rete );
        new StringFunctions().add( m_rete );
    }
}



/**
 * syntax: (startswith ?s1 ?s2 ?pos)
 * return TRUE/FALSE
 */
class StartsWith implements Userfunction
{
    String _name = "startswith";
    /**
     * (startswith ?str ?prefix ?pos)
     */
    public StartsWith()
    {
        super();
    }

    public String getName()
    {
        return _name;
    }

    public Value call( ValueVector vv, Context context ) throws ReteException
    {
        if( vv.size() < 3 )
        {
            return Funcall.FALSE;
        }
        String str = vv.get( 1 ).stringValue( context );
        String s = vv.get( 2 ).stringValue( context );
        int pos = 0;
        if( vv.size() > 3 )
        {
            pos = vv.get( 3 ).intValue( context );
        }
        if( str.startsWith( s, pos ) )
        {
            return Funcall.TRUE;
        }
        else
        {
            return Funcall.FALSE;
        }
    }
}

class StrNum implements Userfunction
{
    String _name = "str-num";
    /**
     * (str-num ?str)
     */
    public StrNum()
    {
        super();
    }

    public String getName()
    {
        return _name;
    }

    public Value call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( Integer.parseInt( vv.get( 1 ).stringValue( context ) ),
                          RU.INTEGER );
    }
}

class NumStr implements Userfunction
{
    String _name = "num-str";
    /**
     * (num-str ?num)
     */
    public NumStr()
    {
        super();
    }

    public String getName()
    {
        return _name;
    }

    public Value call( ValueVector vv, Context context ) throws ReteException
    {
        return new Value( vv.get( 1 ).stringValue( context ), RU.STRING );
    }
}

class IS implements Userfunction
{
    String _name = "is";

    /**
     * (is ?1 ?2)
     */
    public IS()
    {
        super();
    }

    public String getName()
    {
        return _name;
    }

    public Value call( ValueVector vv, Context context ) throws ReteException
    {
        if( vv.get( 1 ).stringValue( context ).indexOf( vv.get( 2 ).stringValue( context ) ) >= 0 )
        {
            return Funcall.TRUE;
        }
        return Funcall.FALSE;
    }
}

/**
 * This will duplicate the function defined in StringFunction.java
 * But since this is loaded after StringFunction.java, it shall
 * overwrite sub-string defined there.
 * Bonamy is using the 0-based index instead of the 1-based index
 * in our current rule.
 *
 * (sub-str ?begin ?end ?str)
 */
class SubString implements Userfunction, Serializable
{
    public String getName()
    {
        return "sub-str";
    }

    public Value call( ValueVector vv, Context context ) throws ReteException
    {
        int begin = ( int ) vv.get( 1 ).numericValue( context );
        int end = ( int ) vv.get( 2 ).numericValue( context );
        String s = vv.get( 3 ).stringValue( context );

        if( begin < 0 || begin > s.length() ||
            end > s.length() || end < 0 )
        {
            throw new ReteException( "sub-string",
                                     "Indices must be between 0 and " +
                                     s.length(), "" );
        }
        return new Value( s.substring( begin, end ), RU.STRING );
    }
}

/** 
 * get substring from appointed index to end
 * (sub-laststring ?cxNum ?string)
 */
class SubLastString implements Userfunction, Serializable
{
    public String getName()
    {
        return "sub-laststring";
    }

    public Value call( ValueVector vv, Context context ) throws ReteException
    {
        int cxNum = ( int ) vv.get( 1 ).numericValue( context );
        String s = vv.get( 2 ).stringValue( context );

        if( cxNum < 0 )
        {
            throw new ReteException( "sub-laststring",
                                     "Indices must be larger than 0", "" );
        }
        if(s.length()>cxNum){
        	s=s.substring( s.length()-cxNum );
        }
        return new Value( s, RU.STRING );
    }
}

/**
 * string Replace function 
 * to replace ?word in ?text with ?toword
 * (str-replace ?text ?word ?toword)
 */
class ReplaceString implements Userfunction, Serializable
{
    public String getName()
    {
        return "str-replace";
    }

    public Value call( ValueVector vv, Context context ) throws ReteException
    {
        String text = vv.get( 1 ).stringValue( context );
        String word = vv.get( 2 ).stringValue( context );
        String toword = vv.get( 3 ).stringValue( context );

        if(text.equals("") || word.equals(""))
        {
            throw new ReteException( "str-replace",
                                     "text and word can't be blank. ", "" );
        }
        return new Value(StringUtils.replace(text, word, toword), RU.STRING);
    }
}

class KnownUnitType implements Userfunction, Serializable {
	String _name = "isunit";
	
	public String getName() {
		return _name;
	}
	public Value call(ValueVector vv, Context context) throws ReteException {
		//String filename = vv.get(1).stringValue(context);
		String filename = "rule/unit_types.txt";
		String currentType = vv.get(1).stringValue(context);
		
		File file = new File(filename);
		Vector<String> vec = new Vector<String>();
		try {
			BufferedReader bw = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = bw.readLine()) != null) {
				vec.add(line.toLowerCase());
			}
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if(vec.contains(currentType.toLowerCase())){
			return Funcall.TRUE;
		}else{
			return Funcall.FALSE;
		}
	}
}

/**
 * Regular expression
 * syntax: (regexp str reg)
 */
class Regexp implements Userfunction
{
    String _name = "regexp";

    public String getName()
    {
        return _name;
    }

    public Value call( ValueVector vv, Context context ) throws ReteException
    {
        //System.out.println(_name+" -> vv[1].type: "+RU.getTypeName(vv.get(1).type()));
        //System.out.println(_name+" -> "+vv.get(1).stringValue(context)+" : "+vv.get(2).stringValue(context));
        if( vv.get( 1 ).type() == RU.STRING ||
            vv.get( 1 ).type() == RU.ATOM ||
            vv.get( 1 ).type() == RU.INTARRAY ||
            vv.get( 1 ).type() == RU.FUNCALL ||
            vv.get( 1 ).type() == RU.VARIABLE )
        {
            try
            {
            	Pattern p = Pattern.compile( vv.get( 2 ).stringValue( context ) );
            	Matcher match = p.matcher( vv.get( 1 ).stringValue( context ) );

                if( match.find())
                {
                    return Funcall.TRUE;
                }
            }
            catch( Exception ree )
            {
                ree.printStackTrace();
            }
        }
        return Funcall.FALSE;
    }
}

class NLUTest implements Userfunction, Serializable {
	public String getName() {
		return "NLUTest";
	}
	public Value call(ValueVector vv, Context context) throws ReteException {
		String input = "Stanford";
		for (int i = 1; i < vv.size(); i++) {
			if (vv.get(i).resolveValue(context).toString().indexOf(input)>=0) {
				System.err.println("Stanford is a good colleage!");
				return Funcall.NIL;
			}
		}
		System.err.println("What's wrong with you?");
		return Funcall.NIL;
	}
}
