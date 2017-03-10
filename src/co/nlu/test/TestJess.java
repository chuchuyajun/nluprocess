package co.nlu.test;

import coc.Agent;

/**
 * @author Min Xia
 * @date Oct 2, 2012
 */
public class TestJess {
	public static void main(String[] a) throws Exception
    {
		Agent agent = new Agent();
		agent.assertFact("(ner John 30)");
		agent.inference();
		Agent.log("Test cpl finished!");
    }
}
