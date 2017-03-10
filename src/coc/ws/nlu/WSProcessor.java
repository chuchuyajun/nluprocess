package coc.ws.nlu;

import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import coc.Agent;
import coc.agent.engine.Fact;
import coc.convertion.VelocityEngineUtils;

import co.nlu.models.Dep;
import co.nlu.models.Sentence;
import co.nlu.models.Token;
import co.nlu.process.Processor;
import co.nlu.utils.Log;
import co.nlu.utils.XMLElement;

public class WSProcessor extends Processor{
	
	public String process2(String text) {
		String owlcontent = null;
		XMLElement el = null;
		Map<Integer, Sentence> setnsRet = new HashMap<Integer, Sentence>();
		
		try{
			if (StringUtils.isNotBlank(text)) {
				Log.debug("Start to process input");
				el = XMLElement.parseXML(text);
				setnsRet = parseSentences(el);
				owlcontent = generateFacts(setnsRet);
				setnsRet.clear();
				
				return owlcontent;
			}else{
				Log.debug("The input text is empty.");
			}
		}catch(Exception ex){
			Log.debug("process2() error" + ex.getMessage());
		}
		return "";
	}
	
	private String generateFacts(Map<Integer, Sentence> setnsRet) {
		String owlcontent = null;
		Agent agent = new Agent();
		Log.debug("Start to genearte facts");
		StringBuilder sb = new StringBuilder();
		String[] factslist;
		Sentence stnc;
		Formatter formatter = new Formatter(sb, Locale.US);
		for (Object skey : setnsRet.keySet()) {
			stnc = setnsRet.get(skey);
			for(Object tkey : stnc.getTokenRet().keySet()){
				Token tk = (Token)stnc.getTokenRet().get(tkey);
				if(tk.getPos().length()!=1){
					formatter.format("(%s %s-%sx%s %s)", "_pos", tk.getWord(),stnc.getIndex(),tk.getId(),tk.getPos());
					sb.append("\n");
				}
				if(tk.getNer().equals("O"))
					continue;
				formatter.format("(%s %s-%sx%s %s)", "ner", tk.getWord(),stnc.getIndex(),tk.getId(),tk.getNer());
				sb.append("\n");
				
			}
			List depList = stnc.getDepRet();
			for(Iterator it = depList.iterator();it.hasNext();){
				Dep dep = (Dep) it.next();
				formatter.format("(_td %s %s-%sx%s %s-%sx%s)",dep.getDep(),dep.getGov().getValue(),stnc.getIndex(),dep.getGov().getIdx(),
						dep.getDepend().getValue(), stnc.getIndex(),dep.getDepend().getIdx());
				sb.append("\n");
			}
//			stnc.clear();
		}
		
		System.out.println(sb);
		factslist = sb.toString().split("\\n");
		for(int i = 0; i<factslist.length;i++){
			agent.assertFact(factslist[i]);
			Log.debug("Assert fact: "+factslist[i]);
		}

		agent.inference();
		agent.clear();
		Enumeration fen = agent.getEngine().listFacts();
		Vector vec = new Vector();
		Fact mfact;
		while (fen.hasMoreElements()) {
			mfact = (Fact) fen.nextElement();
			vec.add(mfact);
		}
		owlcontent = VelocityEngineUtils.getOutputOWLContent(vec, setnsRet);
		Log.debug("Test cpl finished!");
		Log.debug("End facts generateion");
		
		return owlcontent;
	}
}
