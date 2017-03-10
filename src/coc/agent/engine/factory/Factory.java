package coc.agent.engine.factory;

import coc.agent.engine.*;

/**
 * Allows extensions that get into the guts of Jess
 * @version 1.0
 */

public interface Factory  
{
  Token newToken(Fact firstFact, int tag) throws ReteException;
  Token newToken(Token t, Fact newFact) throws ReteException;
  Token newToken(Token lt, Token rt) throws ReteException;
  Token newToken(Token t) throws ReteException;    
}
