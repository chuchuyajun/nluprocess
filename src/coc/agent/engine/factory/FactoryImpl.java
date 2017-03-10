package coc.agent.engine.factory;

import java.io.Serializable;
import coc.agent.engine.*;

/**
 * Standard factory implementation
 * @version 1.0
 */

public class FactoryImpl implements Factory, Serializable 
{
  public  Token newToken(Fact firstFact, int tag) throws ReteException
  {
    return new Token(firstFact, tag);
  }

  public Token newToken(Token t, Fact newFact) throws ReteException
  {
    return new Token(t, newFact);
  }

  public Token newToken(Token lt, Token rt) throws ReteException
  {
    return new Token(lt, rt);
  }

  public Token newToken(Token t) throws ReteException
  {
    return new Token(t);
  }
}
