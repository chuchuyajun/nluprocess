package coc.agent.engine;

import java.io.Serializable;

/**
 * Allows functions to be called indirectly, so that we can 'advice' them.
 * @version 1.0
 */

final class FunctionHolder implements Serializable
{
  private Userfunction m_uf;

  FunctionHolder(Userfunction uf)
  {
    setFunction(uf);
  }
    
  final void setFunction(Userfunction uf)
  {
    m_uf = uf;
  }
  
  final Userfunction getFunction()
  {
    return m_uf;
  }

  final String getName() { return m_uf.getName(); }

  final Value call(Funcall vv, Context c) throws ReteException
  {
    Userfunction uf = m_uf;
    if (c.getInAdvice())
      uf = stripAdvice();
  return uf.call(vv, c);
  }

  // If inside an "advice", we had better not call any other advice functions!
  Userfunction stripAdvice()
  {
    Userfunction uf = m_uf;
    
    while (uf instanceof Advice)
      uf = ((Advice) uf).getFunction();

    return uf;
  }
}
