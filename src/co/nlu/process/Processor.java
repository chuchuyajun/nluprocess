package co.nlu.process;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.MDC;

import coc.Agent;
import coc.agent.engine.Fact;
import coc.convertion.VelocityEngineUtils;

import co.jwi.WordNetAgent;
import co.nlu.models.Dep;
import co.nlu.models.Dependent;
import co.nlu.models.Governor;
import co.nlu.models.Sentence;
import co.nlu.models.Token;
import co.nlu.utils.FileUtil;
import co.nlu.utils.Log;
import co.nlu.utils.XMLElement;

/**
 * @author Min Xia
 * @date Oct 2, 2012
 */

public class Processor implements ProcessorImpl{
	
	static{
		WordNetAgent.setWNPath("C:/Work/WorkSpace/workspace_NLP/co-nlu-ontology/cyberobject/apps/co-nlu-ontology/profile/wn-dict");
	}

	@Override
	public void process(String text) {
		XMLElement el = null;
		Map<Integer, Sentence> setnsRet = new HashMap<Integer, Sentence>();
		
		if (StringUtils.isNotBlank(text)) {
			Log.debug("Start to process input\n" + text);
			MDC.put("msgId", "coctest");
			el = XMLElement.parseXML(text);
			setnsRet = parseSentences(el);
			generateFacts(setnsRet);
			setnsRet.clear();
			MDC.remove("msgId");
		}
		else
			Log.debug("The input text is empty.");
	}

	@Override
	public void process(File file) {
		String text = "";
		try {
			text = FileUtil.readFileToString(file);
			process(text);
		} catch (Exception e) {
			Log.debug("Processor.process(file) exception" + e.getLocalizedMessage());
		}
	}
	
	protected Map<Integer, Sentence> parseSentences(XMLElement el){
		Map<Integer, Sentence> sentret = new HashMap<Integer, Sentence>();
		if(null == el){
			return null;
		}
		List<XMLElement> sentenceElements = el.getElements("//document/sentences/sentence");
		for(Iterator<XMLElement> it = sentenceElements.iterator();it.hasNext();){
			XMLElement sentence = (XMLElement)it.next();
			Sentence sent = new Sentence();
			int ind = Integer.parseInt(sentence.getAttribute("id"));
			Map<Integer, Token> tokenret = parseToken(sentence);
			List depret = parseDep(sentence);
			List normldepret = filterDep(tokenret, depret);
			sent.setIndex(ind);
			sent.setTokenRet(tokenret);
			sent.setDepRet(normldepret);
			String text = restoreSentence(sentence);
			sent.setText(text);
			sentret.put(ind, sent);
		}
		
		return sentret;
	}

	private List filterDep(Map<Integer, Token> tokenret, List depret) {
		// TODO Auto-generated method stub
		List filterDepRet = new ArrayList();
		for(Iterator it = depret.iterator();it.hasNext();){
			Dep dep = (Dep)it.next();
			if(checkNormalizedToken(tokenret,dep))
				continue;
			filterDepRet.add(dep);
		}
		
		return filterDepRet;
	}

	private boolean checkNormalizedToken(Map<Integer, Token> tokenret,
			Dep dep) {
		// TODO Auto-generated method stub
		if(tokenret.get(dep.getGov().getIdx()).isHasNormalNer() && 
				tokenret.get(dep.getDepend().getIdx()).isHasNormalNer())
			return true;
		return false;
	}

	private String restoreSentence(XMLElement sentence) {
		// TODO Auto-generated method stub
		StringBuilder sb = new StringBuilder();
		List wordList = sentence.getElements("tokens/token/word");
		for(Iterator it = wordList.iterator();it.hasNext();){
			sb.append(((XMLElement)it.next()).getText());
			sb.append(" ");
		}
		return sb.toString();
	}

	private List parseDep(XMLElement sentence) {
		List depRet = new ArrayList();
		List depList = sentence.getElements("collapsed-ccprocessed-dependencies/dep");
		for(Iterator it = depList.iterator();it.hasNext();){
			XMLElement depel = (XMLElement)it.next();
			Dep dep = new Dep();
			Governor gov = new Governor();
			Dependent depend = new Dependent();
			gov.setIdx(Integer.parseInt(depel.getElement("governor").getAttribute("idx")));
			gov.setValue(depel.getChildText("governor").toLowerCase());
			depend.setIdx(Integer.parseInt(depel.getElement("dependent").getAttribute("idx")));
			depend.setValue(depel.getChildText("dependent").toLowerCase());
			dep.setDep(depel.getAttribute("type"));
			dep.setGov(gov);
			dep.setDepend(depend);
			depRet.add(dep);
		}
		
		return depRet;
	}

	private Map<Integer, Token> parseToken(XMLElement sentence) {
		Map<Integer, Token> tokenRet = new HashMap<Integer, Token>();
		List<XMLElement> tList = sentence.getElements("tokens/token");
		for(Iterator<XMLElement> it = tList.iterator();it.hasNext();){
			XMLElement token = (XMLElement) it.next();
			Token tk = new Token();
			int tInd = Integer.parseInt(token.getAttribute("id"));
			String pos =  token.getChildText("POS");
			String ner = token.getChildText("NER");
			String normner = token.getChildText("NormalizedNER");
			if(!ner.equals("O") && (null !=normner)){
				tk.setHasNormalNer(true);
				normner = normner.replace(" ", "-");
				tk.setNorner(normner);
			}else
				tk.setHasNormalNer(false);
			tk.setId(tInd);
			tk.setWord(token.getChildText("word").toLowerCase());
			tk.setPos(pos);
			if(pos.startsWith("JJ") || pos.startsWith("RB")){
				tk.setLemma(WordNetAgent.getInstance().getLemma(token.getChildText("word"), pos));
			}else
				tk.setLemma(token.getChildText("lemma").toLowerCase());
			if(ner.equals("O") && (pos.startsWith("JJ") || pos.startsWith("RB") || pos.startsWith("NN")))
			{
//				ner = WordNetAgent.getInstance().getRelevantParent(tk.getLemma(), pos);
				if(null == ner || ner.trim().equals("")){
					ner = "O";
				}
				tk.setNer(ner);
				Log.debug("Wordnet parent word: " + tk.getLemma() + ", " + pos + " -> " + tk.getNer());
			}else
				tk.setNer(ner);
			
			tokenRet.put(tInd, tk);
		}
		return tokenRet;
	}

	private void generateFacts(Map<Integer, Sentence> setnsRet) {
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
					formatter.format("(%s %s_%sx%s %s)", "_pos", tk.getWord(),stnc.getIndex(),tk.getId(),tk.getPos());
					sb.append("\n");
					formatter.format("(%s %s_%sx%s %s)", "_lema", tk.getWord(),stnc.getIndex(),tk.getId(),tk.getLemma());
					sb.append("\n");
				}
				if(tk.getNer().equals("O"))
					continue;
				formatter.format("(%s %s_%sx%s %s)", "_ner", tk.getWord(),stnc.getIndex(),tk.getId(),tk.getNer());
				sb.append("\n");
				if(tk.isHasNormalNer()){
					formatter.format("(%s %s_%sx%s %s)", "_normner", tk.getWord(),stnc.getIndex(),tk.getId(),tk.getNorner());
					sb.append("\n");
				}
				
				
			}
			List depList = stnc.getDepRet();
			for(Iterator it = depList.iterator();it.hasNext();){
				Dep dep = (Dep) it.next();
				formatter.format("(_td %s %s_%sx%s %s_%sx%s)",dep.getDep(),dep.getGov().getValue(),stnc.getIndex(),dep.getGov().getIdx(),
						dep.getDepend().getValue(), stnc.getIndex(),dep.getDepend().getIdx());
				sb.append("\n");
			}
//			stnc.clear();
		}
		
		System.out.println(sb);
		factslist = sb.toString().split("\\n");
		agent.assertFact("(initial-fact)");
		for(int i = 0; i<factslist.length;i++){
			agent.assertFact(factslist[i]);
			Log.debug("Assert initial fact: "+factslist[i]);
		}

		agent.inference();
		agent.clear();
		Enumeration fen = agent.getEngine().listFacts();
		Vector vec = new Vector();
		Fact mfact;
		//System.out.println("All Facts Begin -->");
		while (fen.hasMoreElements()) {
			mfact = (Fact) fen.nextElement();
			vec.add(mfact);
			//System.out.println("  --->"+ mfact.toStringWithParens());
		}
		//System.out.println("--All Facts End --");
		VelocityEngineUtils.getOutputOWL(vec, setnsRet);
		Log.debug("Test cpl finished!");
		Log.debug("End facts generateion");
	}
}
