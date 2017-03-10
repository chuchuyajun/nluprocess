package coc.agent.engine;

/** **********************************************************************
 * Interface for a collection of functions, user-defined or otherwise.
 * <P>
 ********************************************************************** */

public interface Userpackage
{

  /**
   * Add this package of functions to the given engine by calling addUserfunction
   * some number of times.
   * @see coc.agent.engine.Rete#addUserfunction
   * @see coc.agent.engine.Rete#addUserpackage
   * @param engine 
   */
  void add(Rete engine);

}
