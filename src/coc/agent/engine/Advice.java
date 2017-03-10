package coc.agent.engine;

/**
 * Actual function advice classes implement this
 */

interface Advice extends Userfunction
{
  Userfunction getFunction();
  void addAction(Value v);

}
