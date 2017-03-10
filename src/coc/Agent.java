
package coc;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Enumeration;

import co.nlu.utils.Log;
import coc.agent.engine.DumpFunctions;
import coc.agent.engine.Fact;
import coc.agent.engine.Jesp;
import coc.agent.engine.JessEvent;
import coc.agent.engine.JessListener;
import coc.agent.engine.Rete;
import coc.agent.engine.ReteException;
import coc.agent.engine.Userpackage;
import coc.agent.model.AgentPackage;
import coc.utils.PrintWriterWithTime;

public class Agent implements Serializable, JessListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7953475097421576774L;
	private String rulefilename = "rule/nlu.clp";
	private static String LIBRARY_NAME = "rule/scriptlib.clp";
	private boolean reteRunning = false;
	private Jesp jesp;
	private Rete rete;
	private static Agent _self = null;
	
	public Agent(){
		PrintWriter pw;
		PrintWriter pw_fact;
		try {
			pw = new PrintWriterWithTime(new FileWriter("logs/agent.log" , true));
			pw_fact = new PrintWriterWithTime(new FileWriter("logs/allfacts.log" , true));
		} catch (Exception ex) {
			pw = new PrintWriter(System.out);
			pw_fact = new PrintWriter(System.out);
		}
		
		rete = new Rete();
		
		rete.addOutputRouter("t", pw);
		rete.addOutputRouter("WSTDOUT", pw);
		rete.addOutputRouter("WSTDERR", pw);
		
		rete.addOutputRouter("WSTDOUT_FACT", pw_fact);
		/*
		 * if Only use jess.jar and do not want to change engine core code.
		 * you can new a class(XXXFunctions) which implements Userpackage and bundle it with Engine through ".add(rete)"
		 */
		new AgentPackage(this).add(rete);//Add Cyber defined function to this package
		try {
			this.initRuleBase(rulefilename);
		} catch (Exception ex) {
			ex.printStackTrace();
			log(" failed to load rule from " + rulefilename + " | " + ex.getMessage());
		}
	}

	public synchronized static Agent singleton(){
		if(_self == null){
			_self = new Agent();
		}
		return _self;
	}
	
	/**
	 * Loading rules from a file or from a URL
	 */
	public final void initRuleBase(String rulefile) throws ReteException,
			IOException {
		if (rulefile == null) {
			return;
		}
		Log.debug("agent is currently running ... ");
		log("agent is currently running ... ");

		this.rulefilename = rulefile;
		this.executeCommand("(reset)");
		this.executeCommand("(clear)");
		this.inference();
		Log.debug("Loading " + rulefile + "... ");
		log("Loading " + rulefile + "... ");

		//loadOptionalFunctions();

		BufferedReader rfr = new BufferedReader(new FileReader(rulefile));

		// Process input from file or keyboard
		this.jesp = new Jesp(rfr, this.rete);
		do {
			jesp.parse(false);
		} while (rfr.ready());
		rfr.close();
		Log.debug("Rules have been loaded Successfully.");
		log("Rules have been loaded Successfully.");
	}
	
	//DUMP function will trigger it
	private void loadOptionalFunctions() throws ReteException,
			FileNotFoundException, IOException {
		String[] packages = { "coc.agent.engine.BagFunctions",
				"coc.agent.engine.ViewFunctions" };

		for (int i = 0; i < packages.length; i++) {
			try {
				rete.addUserpackage((Userpackage) Class.forName(packages[i])
						.newInstance());
			} catch (Throwable t) {
			}
		}

		DumpFunctions df = new DumpFunctions();
		rete.addUserpackage(df);
		df.addJessListener(this);

		if (LIBRARY_NAME != null) {
			BufferedReader bfr = new BufferedReader(new FileReader(LIBRARY_NAME));
			jesp = new Jesp(bfr, rete);
			do {
				jesp.parse(false);
			} while (bfr.ready());

			bfr.close();
		}

		rete.addJessListener(new LibraryLoader());
	}

	private class LibraryLoader implements Serializable, JessListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 9076039427290731030L;

		public void eventHappened(JessEvent je) {
			if (je.getType() == JessEvent.CLEAR) {
				if (LIBRARY_NAME == null) {
					return;
				}
				try {
					BufferedReader bfr = new BufferedReader(new FileReader(LIBRARY_NAME));
					jesp = new Jesp(bfr, rete);
					do {
						jesp.parse(false);
					} while (bfr.ready());

					bfr.close();
				} catch (ReteException re) {
					re.printStackTrace();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * @param je
	 */
	public void eventHappened(JessEvent je) throws ReteException {
		if (je.getType() == JessEvent.BLOAD) {
			// Reattach parser to engine
			rete = (Rete) je.getObject();
			jesp.setEngine(rete);

			// Reinstall things not loaded by Rete constructor
			try {
				loadOptionalFunctions();
			} catch (FileNotFoundException fnf) {
				throw new ReteException("eventHappend", "", fnf);
			} catch (IOException ioe) {
				throw new ReteException("eventHappend", "", ioe);
			}
		}
	}
	
	public void assertFact(String fact){
		executeCommand("(assert " + fact + ")");
	}
	
	public void executeCommand(String cmd){
		try {
			rete.executeCommand(cmd);
		} catch (ReteException re) {
			re.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void inference() {
		if (reteRunning) {
			/**
			 * if it is already running, do not run again
			 */
			return;
		}

		reteRunning = true;

		try {
			rete.run();
		} catch (ReteException re) {
			re.printStackTrace();
		} finally {
			reteRunning = false;
		}
	}
	
	public void clear() {
		if (rete != null) {
			rete.halt();
		}
		
		this.reteRunning = false;
	}
	
	//TODO
	public static void log(String info){
		System.out.println(info);
	}
	
	public Rete getEngine() {
		return rete;
	}
	
	public boolean isExistFact(String fact) {
		boolean ret = false;
		try {
			Rete rete = getEngine();
			Enumeration fen = rete.listFacts();
			Fact mfact = null;
			String factstr = null;
			while (fen.hasMoreElements()) {
				mfact = (Fact) fen.nextElement();
				factstr = mfact.toStringWithParens();
				if (factstr.equals(fact)) {
					ret = true;
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public String generateOWL(){
		String owlcontent = "";
		
		Enumeration fen = rete.listFacts();
		
		return owlcontent;
	}
	
	public static void main(String[] args){
		Agent agent = new Agent();
		agent.assertFact("(ner John 30)");
		agent.inference();
		Agent.log("Test cpl finished!");
	}
	
}
