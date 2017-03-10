package coc.agent.engine;
import java.util.*;
import java.io.*;


/** **********************************************************************
 * An accelerator generates Java versions of rule LHSs, compiles them and returns
 * new Test objects to execute them.
 * <P>
 ********************************************************************** */

public interface Accelerator
{

  /**
   * Given the function call, return a Test object.
   * @param f A function call to translate
   * @return A coc.agent.engine.Test object that performs equivalently to the Funcall.
   */

  Test speedup(Funcall f) throws ReteException;

}











