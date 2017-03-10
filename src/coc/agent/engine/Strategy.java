
package coc.agent.engine;
import java.util.Vector;
import java.io.*;

/** **********************************************************************
 * An interface for conflict resolution strategies. Implement this interface, then
 * pass the class name to (set-strategy).
 *<P>
 ********************************************************************** */

public interface Strategy
{
  /**
   * To implement your own conflict resolution strategy, you write this method. It should
   * insert the Activation argument in the proper order within the given vector of
   * activations. Salience should be respected.
   *
   * @param a A new rule activation. Place this in the proper order in v. It is your job to
   *          respect rule salience!
   * @param v The vector of all activations, which will fire in increasing order
   *          
   * @see coc.agent.engine.RU#bsearchVector
   * @exception ReteException if something goes wrong
   */

  void addActivation(Activation a, Vector v) throws ReteException;

  /**
   * To implement your own conflict resolution strategy, you write this method. It should
   * find any activation due to this token within the given vector of
   * activations and return the index. If none, should return -1.
   *
   * @param t A Token
   * @param d A rule
   * @param v A vector of Activations
   *          
   * @see coc.agent.engine.RU#bsearchVector
   * @exception ReteException if something goes wrong
   */

  int findActivation(Token t, Defrule d, Vector v) throws ReteException;

  /**
   * Return the name of this strategy
   * @return a display name for this strategy
   */

  String getName();
}

class breadth implements Strategy, Serializable
{

  public int findActivation(Token t, Defrule d, Vector v) throws ReteException
  {
    synchronized (v)
      {
        int size = v.size();
        int time = t.getTime();
        int idx = findActivation0(t,d,v);
        for (int i=idx; i <size; i++)
          {            
            Activation  a = (Activation) v.elementAt(i);
            
            Token tt = a.getToken();
            if (a.getRule() == d && tt.dataEquals(t))
              {
                return i;
              }
            else if (tt.getTime() > time)
              break;
          }    
        return -1;
      }
  }

  /** This version returns the start of the search region for finding. */
  private int findActivation0(Token t, Defrule d, Vector v) throws ReteException
  {
    int start = 0;
    int end = v.size();
    
    // Special cases for first activation
    if (end == 0)
      return 0;
    
    // Special-case for single-salience agenda - faster!
    int sala = d.getSalience();
    int sal0 = ((Activation)v.elementAt(0)).getRule().getSalience();
    int saln = ((Activation)v.elementAt(v.size() - 1)).getRule().getSalience();
    
    if (sala != sal0)
      start = RU.bsearchVector(sala, v, 0, v.size(),
                               RU.fetchActSalience, RU.compareLTE);
    
    // All salience values are greater
    if (start == -1)
      {
        return end;
      }
    
    if (sala != saln)
      end = RU.bsearchVector(sala, v, 0, v.size(),
                             RU.fetchActSalience, RU.compareLT);
    
    // No salience values are smaller
    if (end == -1)
      end = v.size();
    
    else if (start == end)
      {
        int salse = ((Activation) v.elementAt(start)).getRule().getSalience();
        if (sala <= salse && v.size() > end)
          return ++start;
        else
          return start;
      }
    
    
    int idx = RU.bsearchVector(t.getTime(), v, start, end,
                               RU.fetchActTime, RU.compareGTE);
    if (idx == -1)
      return end;
    else
      return idx;
  }

  public void addActivation(Activation a, Vector v) throws ReteException
  {
    synchronized (v)
      {
        int idx = findActivation0(a.getToken(), a.getRule(), v);
        v.insertElementAt(a, idx);
      }
    return;
  }

  public String getName() { return "breadth"; }
}

class depth implements Strategy, Serializable
{
  public int findActivation(Token t, Defrule d, Vector v) throws ReteException
  {
    synchronized (v)
      {
        int size = v.size();
        int time = t.getTime();
        int idx = findActivation0(t,d,v);
        for (int i=idx; i<size; i++)
          {            
            Activation  a = (Activation) v.elementAt(i);
            
            Token tt = a.getToken();
            if (a.getRule() == d && tt.dataEquals(t))
              {
                return i;
              }
            else if (tt.getTime() < time)
              break;
          }    
        return -1;
      }
  }

  private int findActivation0(Token t, Defrule d, Vector v) throws ReteException
  {

    int start = 0;
    int end = v.size();

    // Special cases for first activation
    if (end == 0)
      return 0;
    
    // Special-case for single-salience agenda - faster!
    int sala = d.getSalience();
    int sal0 = ((Activation)v.elementAt(0)).getRule().getSalience();
    int saln = ((Activation)v.elementAt(v.size() - 1)).getRule().getSalience();
    
    if (sala != sal0)
      start = RU.bsearchVector(sala, v, 0, v.size(),
                               RU.fetchActSalience, RU.compareLTE);
    
    // All salience values are greater
    if (start == -1)
      {
        return end;
      }
    
    if (sala != saln)
      end = RU.bsearchVector(sala, v, 0, v.size(),
                             RU.fetchActSalience, RU.compareLT);
    
    // No salience values are smaller
    if (end == -1)
      end = v.size();
    
    
    else if (start == end)
      {
        int salse = ((Activation) v.elementAt(start)).getRule().getSalience();
        if (sala <= salse && v.size() > end)
          return ++start;
        else
          return start;
      }
    
    int idx = RU.bsearchVector(t.getTime(), v, start, end,
                               RU.fetchActTime, RU.compareLTE);
    if (idx == -1)
      return end;
    return idx;
  }

  public void addActivation(Activation a, Vector v) throws ReteException
  {
    synchronized (v)
      {
        int idx = findActivation0(a.getToken(), a.getRule(), v);
        v.insertElementAt(a, idx);
      }
    return;
  }

  public String getName() { return "depth"; }
}











