package coc.agent.engine;

import java.io.*;
import java.net.*;

/**
 * ********************************************************************** A
 * class for parsing, assembling, and interpreting function calls.
 * <P>
 ********************************************************************** */

public class Funcall extends ValueVector implements Serializable {

	/**
	 * Formats a Funcall as a String	 * 
	 * @return The pretty-print form of this Funcall
	 */
	public String toString() {
		try {
			if (get(0).equals("assert")) {
				List l = new List("assert");
				for (int i = 1; i < size(); i++)
					l.add(get(i).factValue(null));
				return l.toString();
			} else if (get(0).equals("assertp")) {
				List l = new List("assertp");
				for (int i = 1; i < size(); i++)
					l.add(get(i).factValue(null));
				return l.toString();
			}

			else if (get(0).equals("modify")) {
				List l = new List("modify");
				l.add(get(1));
				for (int i = 2; i < size(); i++) {
					ValueVector vv = get(i).listValue(null);
					List ll = new List(vv.get(0).atomValue(null));
					for (int j = 1; j < vv.size(); j++)
						ll.add(vv.get(j));
					l.add(ll);
				}

				return l.toString();
			}
			return new List(super.toString()).toString();
		} catch (ReteException re) {
			return re.toString();
		}
	}

	/** The object representing the value TRUE */
	public static Value TRUE;

	/** The object representing the value FALSE */
	public static Value FALSE;

	/** The object representing the value NIL */
	public static Value NIL;

	/** An object representing an empty list. */
	public static Value NILLIST;

	/** The object representing end-of-file */
	public static Value EOF;

	static Value s_else;

	static Value s_then;

	static Value s_do;

	static {
		try {
			TRUE = new Value("TRUE", RU.ATOM);
			FALSE = new Value("FALSE", RU.ATOM);
			NIL = new Value("nil", RU.ATOM);
			NILLIST = new Value(new ValueVector(), RU.LIST);
			EOF = new Value("EOF", RU.ATOM);
			s_else = new Value("else", RU.ATOM);
			s_then = new Value("then", RU.ATOM);
			s_do = new Value("do", RU.ATOM);
		} catch (ReteException re) {
			System.out.println("*** FATAL ***: Can't instantiate constants");
			System.exit(0);
		}
	}

	/**
	 * Load in all the intrinsic functions
	 */
	static void loadIntrinsics(Rete engine) throws ReteException {
		String[] intlist = { "Assert", "Assertp", "Retract", "RetractString",
				"DoBackwardChaining", "Open", "Close", "Foreach", "Read",
				"Readline", "GensymStar", "While", "If", "Bind", "Modify",
				"And", "Or", "Not", "Eq", "EqStar", "Equals", "NotEquals",
				"Gt", "Lt", "GtOrEq", "LtOrEq", "Neq", "Mod", "Plus", "Times",
				"Minus", "Divide", "SymCat", "LoadFacts", "SaveFacts",
				"AssertString", "UnDefrule", "Batch" };

		int i = 0;

		try {
			for (i = 0; i < intlist.length; i++) {
				Userfunction uf = (Userfunction) Class.forName(
						"coc.agent.engine." + intlist[i]).newInstance();
				engine.addUserfunction(uf);
			}

			engine.addUserfunction(new JessVersion(JessVersion.NUMBER));
			engine.addUserfunction(new JessVersion(JessVersion.STRING));

			engine.addUserfunction(new HaltEtc(HaltEtc.HALT));
			engine.addUserfunction(new HaltEtc(HaltEtc.EXIT));
			engine.addUserfunction(new HaltEtc(HaltEtc.CLEAR));
			engine.addUserfunction(new HaltEtc(HaltEtc.RUN));
			engine.addUserfunction(new HaltEtc(HaltEtc.RESET));
			engine.addUserfunction(new HaltEtc(HaltEtc.RETURN));

			Watch w = new Watch();
			engine.addUserfunction(w);
			engine.addUserfunction(new Watch(w));

			engine.addUserfunction(new StoreFetch(StoreFetch.STORE));
			engine.addUserfunction(new StoreFetch(StoreFetch.FETCH));
			engine.addUserfunction(new StoreFetch(StoreFetch.CLEAR_STORAGE));

			engine.addUserfunction(new FactDuplication(FactDuplication.SET));
			engine.addUserfunction(new FactDuplication(FactDuplication.GET));

			engine.addUserfunction(new Defadvice(Defadvice.ADVICE));
			engine.addUserfunction(new Defadvice(Defadvice.UNADVICE));

			engine.addUserfunction(new TryCatchThrow(TryCatchThrow.TRY));
			engine.addUserfunction(new TryCatchThrow(TryCatchThrow.THROW));

			Printout p = new Printout(Printout.PRINTOUT);
			engine.addUserfunction(p);
			engine.addUserfunction(new Printout(Printout.SETMULTI, p));
			engine.addUserfunction(new Printout(Printout.GETMULTI, p));

			// This package is now REQUIRED
			engine.addUserpackage(new coc.agent.engine.ReflectFunctions());
			engine.addUserpackage(new coc.agent.engine.StringFunctions());
			engine.addUserpackage(new coc.agent.engine.PredFunctions());
			engine.addUserpackage(new coc.agent.engine.MultiFunctions());
			engine.addUserpackage(new coc.agent.engine.MiscFunctions());
			engine.addUserpackage(new coc.agent.engine.MathFunctions());
		} catch (Throwable t) {
			throw new ReteException("Funcall.loadIntrisics",
					"Missing intrinsic (non-optional) function class", t);
		}
	}

	FunctionHolder m_funcall;

	/**
	 * Create a Funcall given the name. The Funcall's arguments must then be
	 * added using methods inherited from ValueVector.
	 * 
	 * @param name
	 *            The name of the function
	 * @param engine
	 *            The Rete engine where the function is defined
	 * @exception ReteException
	 *                If something goes wrong.
	 */
	public Funcall(String name, Rete engine) throws ReteException {
		add(new Value(name, RU.ATOM));
		m_funcall = engine.findFunctionHolder(name);
	}

	Funcall(int size) {
		super(size);
	}

	/**
	 * Copies a Funcall
	 * 
	 * @return A copy of the Funcall
	 */
	public Object clone() {
		return cloneInto(new Funcall(size()));
	}

	/**
	 * Makes the argument into a copy of this Funcall.
	 * 
	 * @param vv
	 *            The FUncall into which the copy should be made
	 * @return The argument
	 */
	public Funcall cloneInto(Funcall vv) {
		super.cloneInto(vv);
		vv.m_funcall = m_funcall;
		return vv;
	}

	/**
	 * Execute a funcall in a particular context.
	 * 
	 * @param vv
	 *            The Funcall to execute
	 * @param context
	 *            An execution context for the function
	 * @exception ReteException
	 *                If something goes wrong
	 * @return The result of the function call
	 */
	public final Value execute(Context context) throws ReteException {
		try {
			if (m_funcall == null) {
				String name = get(0).stringValue(context);

				if ((m_funcall = context.getEngine().findFunctionHolder(name)) == null)
					throw new ReteException("Funcall.execute",
							"Unimplemented function", name);
			}
			context.getEngine().broadcastEvent(JessEvent.USERFUNCTION_CALLED,
					m_funcall);

			return m_funcall.call(this, context);
		} catch (ReteException re) {
			re.addContext(toStringWithParens());
			throw re;
		} catch (Exception e) {
			ReteException re = new ReteException("Funcall.execute",
					"Error during execution", e);
			re.addContext(toStringWithParens());
			throw re;
		}
	}

	/**
	 * Calls add(v), then returns this object
	 * 
	 * @param a
	 *            Value
	 * @return This Funcall
	 * @see coc.agent.engine.ValueVector#add
	 */
	public Funcall arg(Value v) {
		add(v);
		return this;
	}
}

/**
 * *** assert ***
 */
class Assert implements Userfunction, Serializable {
	public String getName() {
		return "assert";
	}

	public Value call(ValueVector vvec, Context context) throws ReteException {
		int result = -1;
		Rete engine = context.getEngine();
		for (int i = 1; i < vvec.size(); i++) {
			Fact fact = vvec.get(i).factValue(context);
			result = engine.expandAndAssert(fact, context);
		}
		if (result != -1)
			return new Value(result, RU.FACT_ID);
		else
			return Funcall.FALSE;
	}
}

class Assertp implements Userfunction, Serializable {
	public String getName() {
		return "assertp";
	}

	public Value call(ValueVector vvec, Context context) throws ReteException {
		int result = -1;
		Rete engine = context.getEngine();
		for (int i = 1; i < vvec.size(); i++) {
			Fact fact = vvec.get(i).factValue(context);
			result = engine.expandAndAssertp(fact, context);
		}
		if (result != -1)
			return new Value(result, RU.FACT_ID);
		else
			return Funcall.FALSE;
	}
}

/**
 * *** retract ***
 */
class Retract implements Userfunction, Serializable {
	public String getName() {
		return "retract";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		Value v = vv.get(1).resolveValue(context);
		if (v.type() == RU.ATOM && v.equals("*")) {
			context.getEngine().removeFacts();
		} else {
			Rete engine = context.getEngine();
			for (int i = 1; i < vv.size(); i++) {
				synchronized (engine.getCompiler()) {
					Fact f = engine.retract(vv.get(i).factIDValue(context));

					if (f != null && f.isShadow()) {
						// Undefinstance
						Value ov = f.getSlotValue("OBJECT");
						new Funcall("undefinstance", engine).arg(ov).execute(
								context);
						continue;
					}
				}
			}
		}
		return Funcall.TRUE;
	}
}

/**
 * *** printout ***
 */
class PrintThread extends Thread {
	private static PrintThread s_printThread;

	static {
		s_printThread = new PrintThread();
		s_printThread.setDaemon(true);
		s_printThread.start();
	}

	static PrintThread getPrintThread() {
		return s_printThread;
	}

	private Writer m_os;

	synchronized void assignWork(Writer os) {
		m_os = os;
		notify();
	}

	public synchronized void run() {
		while (true) {
			try {
				while (m_os == null)
					wait();
				try {
					m_os.flush();
				} catch (IOException ioe) {
				} finally {
					m_os = null;
				}
			} catch (InterruptedException ie) {
				break;
			}
			notifyAll();
		}
	}

	// Must return a value so it is not inlined and optimized away!
	synchronized int waitForCompletion() {
		return 1;
	}
}

class Printout implements Userfunction, Serializable {
	private boolean m_multithreadedIO = false;

	private int m_name;

	private Printout m_printout;

	static final int PRINTOUT = 0, SETMULTI = 1, GETMULTI = 2;

	private static final String[] s_names = new String[] { "printout",
			"set-multithreaded-io", "get-multithreaded-io" };

	Printout(int name) {
		m_name = name;
	}

	Printout(int name, Printout p) {
		m_name = name;
		m_printout = p;
	}

	public String getName() {
		return s_names[m_name];
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		switch (m_name) {
		case SETMULTI:
			boolean tmp = m_printout.m_multithreadedIO;
			m_printout.m_multithreadedIO = !(vv.get(1).equals(Funcall.FALSE));
			return tmp ? Funcall.TRUE : Funcall.FALSE;

		case GETMULTI:
			return m_printout.m_multithreadedIO ? Funcall.TRUE : Funcall.FALSE;

		case PRINTOUT:
		default:

			String routerName = vv.get(1).stringValue(context);
			Writer os = context.getEngine().getOutputRouter(routerName);
			if (os == null)
				throw new ReteException("printout", "printout: bad router",
						routerName);

			StringBuffer sb = new StringBuffer(100);
			for (int i = 2; i < vv.size(); i++) {
				Value v = vv.get(i).resolveValue(context);
				switch (v.type()) {
				case RU.ATOM:
					if (v.equals("crlf")) {
						sb.append("\n");
						break;
					}

					// FALL THROUGH
				case RU.STRING:
					sb.append(v.stringValue(context));
					break;
				case RU.INTEGER:
					sb.append(v.intValue(context));
					break;
				case RU.FLOAT:
					sb.append(v.numericValue(context));
					break;
				case RU.FACT_ID:
					sb.append("<Fact-");
					sb.append(v.factIDValue(context));
					sb.append(">");
					break;
				case RU.LIST:
					sb.append(v.listValue(context).toStringWithParens());
					break;
				case RU.EXTERNAL_ADDRESS:
					sb.append(v.toString());
					break;
				default:
					sb.append(v.toString());
				}
			}
			try {
				os.write(sb.toString());
				if (m_multithreadedIO)
					PrintThread.getPrintThread().assignWork(os);
				else
					os.flush();
			} catch (IOException ioe) {
				throw new ReteException("printout", "I/O Exception", ioe);
			}
			return Funcall.NIL;
		}

	}
}

/**
 * *** open ***
 */
class Open implements Userfunction, Serializable {
	public String getName() {
		return "open";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		Rete engine = context.getEngine();

		// Obtain parameters
		String filename = vv.get(1).stringValue(context);
		String router = vv.get(2).stringValue(context);
		String access = "r";
		if (vv.size() > 3)
			access = vv.get(3).stringValue(context);

		try {
			if (access.equals("r")) {
				engine.addInputRouter(router, new BufferedReader(
						new FileReader(filename)), false);
			} else if (access.equals("w")) {
				engine.addOutputRouter(router, new BufferedWriter(
						new FileWriter(filename)));

			} else if (access.equals("a")) {
				RandomAccessFile raf = new RandomAccessFile(filename, "rw");
				raf.seek(raf.length());
				FileWriter fos = new FileWriter(raf.getFD());
				engine.addOutputRouter(router, new BufferedWriter(fos));
			} else
				throw new ReteException("open", "Unsupported access mode",
						access);
		} catch (IOException ioe) {
			throw new ReteException("open", "I/O Exception", ioe);
		}
		return new Value(router, RU.ATOM);
	}
}

/**
 * *** close ***
 */
class Close implements Userfunction, Serializable {
	public String getName() {
		return "close";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		Rete engine = context.getEngine();
		if (vv.size() > 1)
			for (int i = 1; i < vv.size(); i++) {
				Writer os;
				Reader is;
				String router = vv.get(i).stringValue(context);
				try {
					if ((os = engine.getOutputRouter(router)) != null) {
						os.close();
						engine.removeOutputRouter(router);
					}
				} catch (IOException ioe) {
				}
				try {
					if ((is = engine.getInputRouter(router)) != null) {
						is.close();
						engine.removeInputRouter(router);
					}
				} catch (IOException ioe) {
				}
			}
		else
			throw new ReteException("close", "Must close files by name", "");

		return Funcall.TRUE;
	}
}

/**
 * *** read ***
 */
class Read implements Userfunction, Serializable {
	public String getName() {
		return "read";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {

		// Find input source
		String routerName = "t";

		if (vv.size() > 1)
			routerName = vv.get(1).stringValue(context);

		Rete engine = context.getEngine();
		Tokenizer t = engine.getInputWrapper(engine.getInputRouter(routerName));

		if (t == null)
			throw new ReteException("read", "bad router", routerName);
		JessToken jt = t.nextToken();

		// Console-like streams read a token, then throw away to newline.
		if (engine.getInputMode(routerName))
			t.discardToEOL();

		return jt.tokenToValue(null);
	}

}

/**
 * *** readline ***
 */
class Readline implements Userfunction, Serializable {
	public String getName() {
		return "readline";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		String routerName = "t";

		if (vv.size() > 1)
			routerName = vv.get(1).stringValue(context);

		Rete engine = context.getEngine();
		Tokenizer t = engine.getInputWrapper(engine.getInputRouter(routerName));

		String line = t.readLine();
		if (line == null)
			return Funcall.EOF;
		else
			return new Value(line, RU.STRING);
	}
}

/**
 * *** gensym* ***
 */

class GensymStar implements Userfunction, Serializable {
	public String getName() {
		return "gensym*";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		return new Value(RU.gensym("gen"), RU.ATOM);
	}
}

/**
 * *** while ***
 */

class While implements Userfunction, Serializable {
	public String getName() {
		return "while";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		// This accepts a superset of the correct syntax...
		Value result = vv.get(1).resolveValue(context);

		// Skip optional do
		int sawDo = 0;
		if (vv.get(2).equals(Funcall.s_do))
			++sawDo;

		outer_loop: while (!result.equals(Funcall.FALSE)) {
			for (int i = 2 + sawDo; i < vv.size(); i++) {
				result = vv.get(i).resolveValue(context);
				if (context.returning()) {
					result = context.getReturnValue();
					break outer_loop;
				}

			}

			result = vv.get(1).resolveValue(context);

		}
		return result;
	}
}

/**
 * *** if ***
 */
class If implements Userfunction, Serializable {
	public String getName() {
		return "if";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		// This accepts a superset of the correct syntax...

		if (!vv.get(2).equals(Funcall.s_then))
			throw new ReteException("if", "Expected 'then':", vv.get(2)
					.toString());

		// check condition
		Value result = vv.get(1).resolveValue(context);

		if (!(result.equals(Funcall.FALSE))) {
			// do 'then' part
			result = Funcall.FALSE;
			for (int i = 3; i < vv.size(); i++) {
				Value val = vv.get(i).resolveValue(context);

				if (val.equals(Funcall.s_else))
					break;

				if (context.returning()) {
					result = context.getReturnValue();
					break;
				}

				result = val;
			}
			return result;
		} else {
			// first find the 'else'
			result = Funcall.FALSE;
			boolean seen_else = false;
			for (int i = 3; i < vv.size(); i++) {
				if (!seen_else) {
					if (vv.get(i).equals(Funcall.s_else))
						seen_else = true;

					continue;
				}

				Value val = vv.get(i).resolveValue(context);

				if (context.returning()) {
					result = context.getReturnValue();
					break;
				}

				result = val;
			}

			return result;
		}
	}
}

/**
 * *** bind ***
 */
class Bind implements Userfunction, Serializable {
	public String getName() {
		return "bind";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		Value rv = vv.get(2).resolveValue(context);
		context.setVariable(vv.get(1).variableValue(context), rv);
		return rv;
	}
}

/**
 * *** foreach ***
 */
class Foreach implements Userfunction, Serializable {

	public String getName() {
		return "foreach";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		String variable = vv.get(1).variableValue(context);
		ValueVector items = vv.get(2).listValue(context);
		Value v = Funcall.NIL;

		for (int i = 0; i < items.size(); i++) {
			context.setVariable(variable, items.get(i).resolveValue(context));
			for (int j = 3; j < vv.size(); j++) {
				v = vv.get(j).resolveValue(context);
				if (context.returning()) {
					v = context.getReturnValue();
					return v;
				}
			}
		}
		return v;
	}
}

/**
 * *** try, catch, throw ***
 */

class TryCatchThrow implements Userfunction, Serializable {
	public static final String TRY = "try", THROW = "throw";

	private String m_name;

	TryCatchThrow(String s) {
		m_name = s;
	}

	public String getName() {
		return m_name;
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		if (m_name.equals(THROW)) {
			Throwable t = (Throwable) vv.get(1).externalAddressValue(context);
			t.fillInStackTrace();

			if (t instanceof ReteException)
				throw (ReteException) t;
			else {
				throw new ReteException("throw",
						"Exception thrown from Jess language code", t);
			}
		}

		// Else name is "Try." First find catch
		int handler = -1;

		for (int j = 1; j < vv.size(); j++) {
			if (vv.get(j).type() == RU.ATOM && vv.get(j).equals("catch")) {
				handler = j;
				break;
			}
		}

		if (handler == -1)
			throw new ReteException("try", "No catch block in try expression",
					"");

		Value v = null;
		try {
			for (int j = 1; j < handler; j++) {
				v = vv.get(j).resolveValue(context);
				if (context.returning()) {
					v = context.getReturnValue();
					break;
				}
			}
		} catch (Throwable t) {
			v = Funcall.FALSE; // so we can have empty handlers
			context.setVariable("ERROR", new Value(t));

			for (int j = ++handler; j < vv.size(); j++) {
				v = vv.get(j).resolveValue(context);

				if (context.returning()) {
					v = context.getReturnValue();
					break;
				}
			}
		} finally {
			return v;
		}
	}
}

/**
 * *** modify ***
 */
class Modify implements Userfunction, Serializable {
	public String getName() {
		return "modify";
	}

	private String getSlotName(ValueVector svp, Context c) throws ReteException {
		return svp.get(0).stringValue(c);
	}

	Value getSlotValue(ValueVector svp, Context c, int type)
			throws ReteException {
		if (type == RU.SLOT) {
			Value v = svp.get(1).resolveValue(c);
			while (v.type() == RU.LIST)
				v = v.listValue(c).get(0).resolveValue(c);

			return v;
		} else // MULTISLOT
		{
			ValueVector vv = new ValueVector();
			for (int i = 1; i < svp.size(); i++) {
				Value listItem = svp.get(i).resolveValue(c);
				if (listItem.type() == RU.LIST) {
					ValueVector sublist = listItem.listValue(c);
					for (int j = 0; j < sublist.size(); j++)
						vv.add(sublist.get(j).resolveValue(c));
				} else
					vv.add(listItem);
			}
			return new Value(vv, RU.LIST);
		}
	}

	private static Value m_rewrite;
	static {
		try {
			m_rewrite = new Value("modify-n", RU.ATOM);
		} catch (ReteException re) { /* can't happen */
		}
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		Rete engine = context.getEngine();
		Fact fact;
		int factId = vv.get(1).factIDValue(context);

		if (vv.get(0).equals(m_rewrite)) {
			// We know this is not a shadow fact, just a simple modify.
			// We also know the slot names now hold slot numbers. Note that
			// we're retracting the fact to get a copy of it - this saves us one
			// lookup vs. the way it'd donefor the other cases.

			if ((fact = engine.retract(factId)) == null)
				throw new ReteException("modify", "no such fact", String
						.valueOf(factId));

			for (int i = 2; i < vv.size(); i++) {
				ValueVector svp = vv.get(i).listValue(context);
				int[] ia = svp.get(0).intArrayValue(context);
				fact.set(getSlotValue(svp, context, ia[1]), ia[0]);
			}

			// and assert the new fact
			int id = engine.assertFact(fact);

			return new Value(id, RU.FACT_ID);
		}

		// OK, we've never done this one before, or it's a shadow fact.
		// First, get the fact, and the Deftemplate.

		if ((fact = engine.findFactByID(factId)) == null)
			throw new ReteException("modify", "no such fact", String
					.valueOf(factId));

		Deftemplate dt = fact.getDeftemplate();

		// First find out of this is a LHS object, not an ordinary fact
		// If so, call the appropriate mutators...

		if (fact.isShadow()) {
			Value ov = fact.getSlotValue("OBJECT");
			Funcall fc = new Funcall("set", engine).arg(ov).arg(
					new Value("set", RU.STRING));
			fc.setLength(4);

			for (int i = 2; i < vv.size(); i++) {

				// fetch the slot, value subexp, stored as a List
				ValueVector svp = vv.get(i).listValue(context);
				String slotName = getSlotName(svp, context);

				fc.set(new Value(slotName, RU.ATOM), 2);

				int type = dt.getSlotType(dt.getSlotIndex(slotName));
				fc.set(getSlotValue(svp, context, type), 3);

				fc.execute(engine.getGlobalContext());
			}
			return Funcall.TRUE;
		} else {
			engine.retract(fact);
			// This is just an ordinary fact.
			// now change the values. For each argument...

			for (int i = 2; i < vv.size(); i++) {

				// fetch the slot, value subexp, stored as a List
				ValueVector svp = vv.get(i).listValue(context);
				String slotName = getSlotName(svp, context);
				int idx = dt.getSlotIndex(slotName);
				int type = dt.getSlotType(idx);

				// Set the value in the fact
				fact.setSlotValue(slotName, getSlotValue(svp, context, type));

				// modify the funcall to contain the index and type
				svp.set(new IntArrayValue(new int[] { idx, type }, "?__data"),
						0);
			}

			// and assert the new fact
			int id = engine.assertFact(fact);

			// We got this far, so we were able to recast everything as a
			// modify-n.
			// Rename the function call!
			vv.set(m_rewrite, 0);

			return new Value(id, RU.FACT_ID);
		}
	}
}

/**
 * *** and ***
 */
class And implements Userfunction, Serializable {

	public String getName() {
		return "and";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		for (int i = 1; i < vv.size(); i++) {
			Value v = vv.get(i).resolveValue(context);

			if (v.equals(Funcall.FALSE))
				return Funcall.FALSE;
		}

		return Funcall.TRUE;
	}
}

/**
 * *** or ***
 */
class Or implements Userfunction, Serializable {
	public String getName() {
		return "or";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		for (int i = 1; i < vv.size(); i++) {
			Value v = vv.get(i).resolveValue(context);

			if (!v.equals(Funcall.FALSE))
				return Funcall.TRUE;
		}
		return Funcall.FALSE;
	}
}

/**
 * *** not ***
 */
class Not implements Userfunction, Serializable {
	public String getName() {
		return "not";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		if (vv.get(1).resolveValue(context).equals(Funcall.FALSE))
			return Funcall.TRUE;
		else
			return Funcall.FALSE;
	}
}

/**
 * *** eq ***
 */
class Eq implements Userfunction, Serializable {
	public String getName() {
		return "eq";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		Value first = vv.get(1).resolveValue(context);
		for (int i = 2; i < vv.size(); i++) {
			// if (!vv.get(i).resolveValue(context).equals(first))
			/**
			 * CyberObject customization ---
			 */
			if (!vv.get(i).resolveValue(context).equalsWithoutType(first))
				return Funcall.FALSE;
		}
		return Funcall.TRUE;
	}
}

/**
 * *** eq* ***
 */
class EqStar implements Userfunction, Serializable {
	public String getName() {
		return "eq*";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		Value first = vv.get(1).resolveValue(context);
		for (int i = 2; i < vv.size(); i++) {
			if (!vv.get(i).resolveValue(context).equalsStar(first))
				return Funcall.FALSE;
		}
		return Funcall.TRUE;
	}
}

/**
 * *** = ***
 */
class Equals implements Userfunction, Serializable {
	public String getName() {
		return "=";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		for (int i = 2; i < vv.size(); i++) {
			if (!(vv.get(i).numericValue(context) == vv.get(1).numericValue(
					context)))
				return Funcall.FALSE;
		}
		return Funcall.TRUE;
	}
}

/**
 * *** <> ***
 */
class NotEquals implements Userfunction, Serializable {
	public String getName() {
		return "<>";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		for (int i = 2; i < vv.size(); i++) {
			if (vv.get(i).numericValue(context) == vv.get(1).numericValue(
					context))
				return Funcall.FALSE;
		}
		return Funcall.TRUE;
	}
}

/**
 * *** > ***
 */
class Gt implements Userfunction, Serializable {
	public String getName() {
		return ">";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		for (int i = 1; i < vv.size() - 1; i++) {
			double value1 = vv.get(i).numericValue(context);
			double value2 = vv.get(i + 1).numericValue(context);

			if (!(value1 > value2))
				return Funcall.FALSE;
		}
		return Funcall.TRUE;
	}
}

/**
 * *** < ***
 */
class Lt implements Userfunction, Serializable {
	public String getName() {
		return "<";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		for (int i = 1; i < vv.size() - 1; i++) {
			double value1 = vv.get(i).numericValue(context);
			double value2 = vv.get(i + 1).numericValue(context);

			if (!(value1 < value2))
				return Funcall.FALSE;
		}
		return Funcall.TRUE;
	}
}

/**
 * *** >= ***
 */

class GtOrEq implements Userfunction, Serializable {
	public String getName() {
		return ">=";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		for (int i = 1; i < vv.size() - 1; i++) {
			double value1 = vv.get(i).numericValue(context);
			double value2 = vv.get(i + 1).numericValue(context);

			if (!(value1 >= value2))
				return Funcall.FALSE;
		}
		return Funcall.TRUE;
	}
}

/**
 * *** <= ***
 */
class LtOrEq implements Userfunction, Serializable {
	public String getName() {
		return "<=";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		for (int i = 1; i < vv.size() - 1; i++) {
			double value1 = vv.get(i).numericValue(context);
			double value2 = vv.get(i + 1).numericValue(context);

			if (!(value1 <= value2))
				return Funcall.FALSE;
		}
		return Funcall.TRUE;
	}
}

/**
 * *** neq ***
 */
class Neq implements Userfunction, Serializable {

	public String getName() {
		return "neq";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		Value first = vv.get(1).resolveValue(context);
		for (int i = 2; i < vv.size(); i++) {
			if (vv.get(i).resolveValue(context).equals(first))
				return Funcall.FALSE;
		}
		return Funcall.TRUE;
	}
}

/**
 * *** mod ***
 */

class Mod implements Userfunction, Serializable {
	public String getName() {
		return "mod";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		int d1 = (int) vv.get(1).numericValue(context);
		int d2 = (int) vv.get(2).numericValue(context);

		return new Value(d1 % d2, RU.INTEGER);
	}
}

/**
 * *** + ***
 */
class Plus implements Userfunction, Serializable {
	public String getName() {
		return "+";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		double sum = 0;
		int type = RU.INTEGER;
		int size = vv.size();
		for (int i = 1; i < size; i++) {
			Value arg = vv.get(i).resolveValue(context);
			sum += arg.numericValue(context);
			if (arg.type() == RU.FLOAT)
				type = RU.FLOAT;

		}
		return new Value(sum, type);
	}
}

/**
 * *** * ***
 */
class Times implements Userfunction, Serializable {
	public String getName() {
		return "*";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		double product = 1;
		int type = RU.INTEGER;
		int size = vv.size();
		for (int i = 1; i < size; i++) {
			Value arg = vv.get(i).resolveValue(context);
			product *= arg.numericValue(context);
			if (arg.type() == RU.FLOAT)
				type = RU.FLOAT;
		}

		return new Value(product, type);
	}
}

/**
 * *** - ***
 */
class Minus implements Userfunction, Serializable {
	public String getName() {
		return "-";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		Value arg = vv.get(1).resolveValue(context);
		int type = arg.type();
		double diff = arg.numericValue(context);
		int size = vv.size();
		for (int i = 2; i < size; i++) {
			arg = vv.get(i).resolveValue(context);
			diff -= arg.numericValue(context);
			if (arg.type() == RU.FLOAT)
				type = RU.FLOAT;
		}

		return new Value(diff, type);
	}
}

class Divide implements Userfunction, Serializable {
	public String getName() {
		return "/";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		double quotient = vv.get(1).numericValue(context);
		int size = vv.size();
		for (int i = 2; i < size; i++) {
			quotient /= vv.get(i).numericValue(context);
		}
		return new Value(quotient, RU.FLOAT);

	}
}

/**
 * *** sym-cat ***
 */
class SymCat implements Userfunction, Serializable {
	public String getName() {
		return "sym-cat";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {

		StringBuffer buf = new StringBuffer("");
		for (int i = 1; i < vv.size(); i++) {
			Value val = vv.get(i).resolveValue(context);
			if (val.type() == RU.STRING)
				buf.append(val.stringValue(context));
			else
				buf.append(val.toString());
		}

		return new Value(buf.toString(), RU.ATOM);
	}
}

/**
 * *** store, fetch **
 */
class StoreFetch implements Userfunction, Serializable {
	static final int STORE = 0, FETCH = 1, CLEAR_STORAGE = 2;

	static final String[] s_names = { "store", "fetch", "clear-storage" };

	private int m_name;

	StoreFetch(int name) {
		m_name = name;
	}

	public String getName() {
		return s_names[m_name];
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		Value v;
		switch (m_name) {
		case STORE:
			Value val = vv.get(2).resolveValue(context);
			if (val.equals(Funcall.NIL))
				val = null;
			v = context.getEngine().store(vv.get(1).stringValue(context), val);

			if (v != null)
				return v;
			else
				return Funcall.NIL;

		case CLEAR_STORAGE:
			context.getEngine().clearStorage();
			return Funcall.TRUE;

		case FETCH:
		default:
			v = context.getEngine().fetch(vv.get(1).stringValue(context));
			if (v != null)
				return v.resolveValue(context);
			else
				return Funcall.NIL;
		}
	}
}

/**
 * *** HaltEtc ***
 */
class HaltEtc implements Userfunction, Serializable {
	static final int HALT = 0, EXIT = 1, CLEAR = 2, RUN = 3, RESET = 4,
			RETURN = 5;

	static final String[] s_names = { "halt", "exit", "clear", "run", "reset",
			"return" };

	private int m_name;

	HaltEtc(int name) {
		m_name = name;
	}

	public String getName() {
		return s_names[m_name];
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		switch (m_name) {
		case HALT:
			context.getEngine().halt();
			break;
		case EXIT:
			PrintThread.getPrintThread().waitForCompletion();

			System.exit(0);
			break;
		case CLEAR:
			context.getEngine().clear();
			break;
		case RUN:
			if (vv.size() == 1)
				return new Value(context.getEngine().run(), RU.INTEGER);
			else
				return new Value(context.getEngine().run(
						vv.get(1).intValue(context)), RU.INTEGER);
		case RETURN: {
			if (vv.size() > 1)
				return context.setReturnValue(vv.get(1).resolveValue(context));
			else
				return context.setReturnValue(Funcall.NIL);
		}
		case RESET:
			context.getEngine().reset();
			break;
		}
		return Funcall.TRUE;
	}
}

/**
 * *** watch ***
 */
class Watch implements Userfunction, JessListener, Serializable {
	private boolean m_facts, m_rules, m_compilations, m_activations;

	private Watch m_partner;

	Watch() {
	}

	Watch(Watch w) {
		m_partner = w;
	}

	public String getName() {
		return m_partner == null ? "watch" : "unwatch";
	}

	private boolean installListener(int mask, Rete engine) {
		boolean state = m_facts || m_rules || m_activations || m_compilations;
		mask = engine.getEventMask() | mask;
		engine.setEventMask(mask);
		if (!state)
			engine.addJessListener(this);
		return state;
	}

	private boolean removeListener(int mask, Rete engine) {
		boolean state = m_partner.m_facts || m_partner.m_rules
				|| m_partner.m_activations || m_partner.m_compilations;
		mask = engine.getEventMask() & ~mask;
		engine.setEventMask(mask);
		if (!state)
			engine.removeJessListener(m_partner);
		return state;
	}

	// Note that the ordering of things (when installListener, THEN set flag,
	// but unset, then remove) is carefully orchestrated. Be careful when
	// modifying.
	public Value call(ValueVector vv, Context context) throws ReteException {
		String what = vv.get(1).stringValue(context);
		Rete engine = context.getEngine();

		if (what.equals("rules")) {
			if (m_partner == null) {
				installListener(JessEvent.DEFRULE_FIRED, engine);
				m_rules = true;
			} else {
				m_partner.m_rules = false;
				removeListener(JessEvent.DEFRULE_FIRED, engine);
			}
		}

		else if (what.equals("facts")) {
			if (m_partner == null) {
				installListener(JessEvent.FACT, engine);
				m_facts = true;
			} else {
				m_partner.m_facts = false;
				removeListener(JessEvent.FACT, engine);
			}

		}

		else if (what.equals("activations")) {
			if (m_partner == null) {
				installListener(JessEvent.ACTIVATION, engine);
				m_activations = true;
			} else {
				m_partner.m_activations = false;
				removeListener(JessEvent.ACTIVATION, engine);
			}

		}

		else if (what.equals("compilations")) {
			if (m_partner == null) {
				installListener(JessEvent.DEFRULE, engine);
				m_compilations = true;
			} else {
				m_partner.m_compilations = false;
				removeListener(JessEvent.DEFRULE, engine);
			}

		}

		else if (what.equals("all")) {
			if (m_partner == null) {
				installListener(JessEvent.DEFRULE, engine);
				m_compilations = m_activations = m_rules = m_facts = true;
				installListener(JessEvent.DEFRULE_FIRED, engine);
				installListener(JessEvent.FACT, engine);
				installListener(JessEvent.ACTIVATION, engine);
			} else {
				m_partner.m_compilations = m_partner.m_activations = m_partner.m_rules = m_partner.m_facts = false;
				removeListener(JessEvent.DEFRULE, engine);
				removeListener(JessEvent.DEFRULE_FIRED, engine);
				removeListener(JessEvent.FACT, engine);
				removeListener(JessEvent.ACTIVATION, engine);
			}
		} else
			throw new ReteException("watch", "watch: can't watch/unwatch", what);

		return Funcall.TRUE;
	}

	public void eventHappened(JessEvent je) throws ReteException {
		int type = je.getType();
		boolean remove = (type & JessEvent.REMOVED) != 0;
		Rete engine = (Rete) je.getSource();
		switch (type & ~JessEvent.REMOVED) {
		case JessEvent.CLEAR:
			m_rules = m_facts = m_activations = m_compilations = false;
			engine.removeJessListener(this);
			break;
		case JessEvent.FACT: {
			if (m_facts) {
				Fact f = (Fact) je.getObject();
				PrintWriter pw = engine.getOutStream();
				pw.print(remove ? " <== " : " ==> ");
				pw.print("f-");
				pw.print(f.getFactId());
				pw.print(" ");
				pw.println(f);
				pw.flush();
				//print all fact information into a single log file
				PrintWriter pw_fact = engine.getOutFACTStream();
				pw_fact.print(remove ? " <== " : " ==> ");
				pw_fact.print("f-");
				pw_fact.print(f.getFactId());
				pw_fact.print(" ");
				pw_fact.println(f);
				pw_fact.flush();
			}
			break;
		}
		case JessEvent.DEFRULE_FIRED: {
			if (m_rules)
				((Activation) je.getObject()).debugPrint(engine.getOutStream());
			break;
		}
		case JessEvent.ACTIVATION: {
			if (m_activations) {
				Activation a = (Activation) je.getObject();
				PrintWriter pw = engine.getOutStream();
				pw.print(remove ? "<== " : "==> ");
				pw.print("Activation: ");
				pw.print(a.getRule().getName());
				pw.print(" : ");
				pw.println(engine.factList(a.getToken()));
				pw.flush();
			}
			break;
		}
		case JessEvent.DEFRULE: {
			if (m_compilations & !remove) {
				PrintWriter pw = engine.getOutStream();
				pw.println(((HasLHS) je.getObject()).getCompilationTrace());
				pw.flush();
			}

			break;
		}
		default:
			break;
		}
	}
}

/**
 * *** jess versions ***
 */
class JessVersion implements Userfunction, Serializable {
	static final int NUMBER = 0, STRING = 1;

	static final String[] s_names = { "jess-version-number",
			"jess-version-string" };

	private int m_name;

	JessVersion(int name) {
		m_name = name;
	}

	public String getName() {
		return s_names[m_name];
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		switch (m_name) {
		case NUMBER:
			return new Value(5.1, RU.FLOAT);
		default:
			return new Value("Jess Version 5.1 4/24/2000", RU.STRING);
		}
	}
}

/**
 * *** load-facts ***
 */
class LoadFacts implements Userfunction, Serializable {
	public String getName() {
		return "load-facts";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		Reader f;
		if (context.getEngine().getApplet() == null) {
			try {
				f = new FileReader(vv.get(1).stringValue(context));
			} catch (IOException t) {
				throw new ReteException("load-facts", "I/O Exception", t);
			}

		} else {
			try {
				URL url = new URL(context.getEngine().getApplet()
						.getDocumentBase(), vv.get(1).stringValue(context));
				f = new InputStreamReader(url.openStream());
			} catch (Exception t) {
				throw new ReteException("load-facts", "Network error", t);
			}
		}

		// OK, we have a stream. Now the tricky part!

		Jesp jesp = new Jesp(f, context.getEngine());

		return jesp.loadFacts();
	}
}

class SaveFacts implements Userfunction, Serializable {
	public String getName() {
		return "save-facts";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		String s = "";
		PrintWriter f;
		if (context.getEngine().getApplet() == null) {
			try {
				f = new PrintWriter(new FileWriter(vv.get(1).stringValue(
						context)));
			} catch (IOException t) {
				throw new ReteException("save-facts", "I/O Exception", t);
			}

		} else {
			try {
				URL url = new URL(context.getEngine().getApplet()
						.getDocumentBase(), vv.get(1).stringValue(context));
				URLConnection urlc = url.openConnection();
				urlc.setDoOutput(true);
				f = new PrintWriter(urlc.getOutputStream());
			} catch (Exception t) {
				throw new ReteException("load-facts", "Network error", t);
			}
		}

		// OK, we have a stream. Now the tricky part!
		if (vv.size() > 2) {
			for (int i = 2; i < vv.size(); i++) {
				s += context.getEngine()
						.ppFacts(vv.get(i).stringValue(context));

			}
		} else
			s = context.getEngine().ppFacts();

		f.println(s);
		f.close();
		return Funcall.TRUE;

	}
}

class AssertString implements Userfunction, Serializable {
	public String getName() {
		return "assert-string";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		String fact = vv.get(1).stringValue(context);
		Value rv = new Value(context.getEngine().assertString(fact), RU.FACT_ID);
		return rv;
	}
}

/**
 * Karl Mueller NASA/GSFC Code 522.2 (Karl.R.Mueller@gsfc.nasa.gov)
 * 26.January.1998
 * 
 * *** retract-string *** Added function to retract fact as a string
 */
class RetractString implements Userfunction, Serializable {
	public String getName() {
		return "retract-string";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		for (int i = 1; i < vv.size(); i++) {
			context.getEngine().retractString(vv.get(i).stringValue(context));
		}
		return Funcall.TRUE;
	}
}

class UnDefrule implements Userfunction, Serializable {
	public String getName() {
		return "undefrule";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		String rulename = vv.get(1).stringValue(context);
		return context.getEngine().unDefrule(rulename);
	}
}

/**
 * Do backward-chaining (goal-seeking) for a particular deftemplate.
 */
class DoBackwardChaining implements Userfunction, Serializable {
	public String getName() {
		return "do-backward-chaining";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		String name = vv.get(1).stringValue(context);
		if (name.equals("test") || name.equals("not"))
			throw new ReteException("do-backward-chaining",
					"Can't backchain on special CEs", name);
		Deftemplate dt = context.getEngine().findDeftemplate(name);
		if (dt == null)
			dt = context.getEngine().createDeftemplate(name);

		dt.doBackwardChaining();
		Deftemplate newDt = new Deftemplate(RU.BACKCHAIN_PREFIX + name,
				"Goal seeker for " + name, dt);
		newDt.forgetParent();
		context.getEngine().addDeftemplate(newDt);
		return Funcall.TRUE;
	}
}

class FactDuplication implements Userfunction, Serializable {
	static final String SET = "set-fact-duplication",
			GET = "get-fact-duplication";

	private String m_name;

	FactDuplication(String name) {
		m_name = name;
	}

	public String getName() {
		return m_name;
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		Rete engine = context.getEngine();
		if (m_name.equals(SET)) {

			boolean oldValue = engine.getFactDuplication();
			boolean value = !vv.get(1).resolveValue(context).equals(
					Funcall.FALSE);
			engine.setFactDuplication(value);
			return oldValue ? Funcall.TRUE : Funcall.FALSE;
		} else {
			return engine.getFactDuplication() ? Funcall.TRUE : Funcall.FALSE;
		}
	}
}

class Batch implements Userfunction, Serializable {
	public String getName() {
		return "batch";
	}

	public Value call(ValueVector vv, Context context) throws ReteException {
		String filename = vv.get(1).stringValue(context);
		Value v = Funcall.FALSE;
		Reader fis = null;
		try {
			try {
				if (context.getEngine().getApplet() == null)
					fis = new FileReader(filename);
				else {
					URL url = new URL(context.getEngine().getApplet()
							.getDocumentBase(), vv.get(1).stringValue(context));
					fis = new InputStreamReader(url.openStream());
				}
			} catch (Exception e) {
				// Try to find a resource file, too.
				InputStream is = getClass().getClassLoader()
						.getSystemResourceAsStream(filename);
				if (is == null)
					throw new ReteException("batch", "Cannot open file", e);
				fis = new InputStreamReader(is);
			}
			Jesp j = new Jesp(fis, context.getEngine());
			do {
				v = j.parse(false);
			} while (fis.ready());
		} catch (IOException ex) {
			throw new ReteException("batch", "I/O Exception", ex);
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch (IOException ioe) {
				}
		}
		return v;
	}
}