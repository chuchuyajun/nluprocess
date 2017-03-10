package coc.agent.engine;

import java.io.*;

/** **********************************************************************
 * A sort of Hashtable of Tokens kept by sortcode
 *
 * $Id: TokenTree.java,v 1.1 2012/10/02 21:06:02 Buildadmin Exp $
 ********************************************************************** */

class TokenTree implements Serializable
{
  int m_hash;

  TokenVector[] m_tokens;

  boolean m_useSortcode;
  int m_tokenIdx, m_factIdx, m_subIdx;

  TokenTree(int hash, boolean useSortCode, int tokenIdx, int factIdx, int subIdx)
  {
    m_hash = hash;
    m_useSortcode = useSortCode;
    m_factIdx = factIdx;
    m_subIdx = subIdx;
    m_tokenIdx = tokenIdx;
    m_tokens = new TokenVector[m_hash];
  }

  final void clear()
  {
    for (int i=0; i< m_hash; i++)
      if (m_tokens[i] != null)
        m_tokens[i].clear();
  }

  /**
   * @param t 
   * @exception ReteException 
   */
  synchronized boolean add(Token t, boolean update) throws ReteException
  {
    int code;
    if (m_useSortcode) 
      code = t.m_sortcode;
    else if (m_factIdx == -1)
      code = t.fact(m_tokenIdx).getFactId();
    else if (m_subIdx == -1)
      code = t.fact(m_tokenIdx).m_v[m_factIdx].hashCode();
    else
      code = t.fact(m_tokenIdx).m_v[m_factIdx].listValue(null).m_v[m_subIdx].hashCode();
      

    if (code < 0)
      code = -code;

    TokenVector v = findCodeInTree(code, true);

    int size = v.size();
    
    if (update)
      for (int i=0; i< size; i++)
        {
          Token tt = v.elementAt(i);
          if (t.dataEquals(tt))
            {
              return false;
            }
        }

    v.addElement(t);
    return true;
  }  

  /**
   * @param t 
   */
  synchronized boolean remove(Token t) throws ReteException
  {
    int code;
    if (m_useSortcode) 
      code = t.m_sortcode;
    else if (m_factIdx == -1)
      code = t.fact(m_tokenIdx).getFactId();
    else if (m_subIdx == -1)
      code = t.fact(m_tokenIdx).get(m_factIdx).hashCode();
    else
      code = t.fact(m_tokenIdx).get(m_factIdx).listValue(null).get(m_subIdx).hashCode();
    
    if (code < 0)
      code = -code;
    
    TokenVector v = findCodeInTree(code, false);    
    
    if (v == null)
      return false;
    
    int size = v.size();
    
    if (size == 0)
      return false;
    
    for (int i=0; i< size; i++)
      {
        Token tt = v.elementAt(i);
        if (t.dataEquals(tt))
          {
            v.removeElementAt(i);
            return true;
          }
      }
    return false;
  }
  
  /**
   * @param t 
   * @param create 
   * @return 
   */
  synchronized TokenVector findCodeInTree(int code, boolean create) throws ReteException
  {
    code = code % m_hash;
    
    if (code < 0)
      code = -code;
    
    if (create && m_tokens[code] == null)
      return m_tokens[code] = new TokenVector();
    else
      return m_tokens[code];
  }
  
}


  
