package coc.agent.engine;

import java.util.Vector;

//import coc.agent.view.ISmart2;
//import coc.agent.event.AgentEvent;

public interface IReteSession
{
	public void retract(Fact f);
	public void pushf(Fact f);
	public void pushpf(Fact f);
//	public void pushAE(AgentEvent e);
	public void reset(Rete engine) throws ReteException;
//	public void setSmart2(ISmart2 smart2);
//        public ISmart2 getSmart2();
//        public co.adaptive.AdaptiveFrame getFrame();
	public boolean isAdaptedFrame(Vector cdFacts);
        public String getSid();
}
