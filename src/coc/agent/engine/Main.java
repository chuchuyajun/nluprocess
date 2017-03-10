

package coc.agent.engine;

import java.io.*;
import java.net.*;

/** **********************************************************************
 * A command-line interface for Jess; also displayed in a window by the
 * Console classes.
 * <P>
********************************************************************** */

public class Main implements Serializable, JessListener
{
  private Rete m_rete;
  private transient Reader m_fis;
  private Jesp m_j;
  private boolean m_readStdin = true;
  private static final String LIBRARY_NAME = "jess/scriptlib.clp";


  public static void main(String[] argv) 
  {
    Main m = new Main();
    m.initialize(argv, new Rete());
    m.execute(true);
  }


  /**
   * Display the Jess startup banner on the Rete object's standard output, something like
   *
   * <PRE>
   * Jess Version 5.0a5 5/27/99
   * </PRE>
   *
   */


  public void showLogo()
  {
    if (m_rete != null && m_rete.getOutStream() != null)
      {
        m_rete.getOutStream().println("\nJess, the Java Expert System Shell");
                                      //+ " and the Sandia Corporation");
        try
          {
            m_rete.executeCommand("(printout t (jess-version-string) crlf crlf)");
          } 
        catch(ReteException re) {}
      }
  }
  
  private class LibraryLoader implements Serializable, JessListener
  {
    public void eventHappened(JessEvent je) {
      if (je.getType() == JessEvent.CLEAR)
        {
          try
            {
              m_rete.executeCommand("(batch " + LIBRARY_NAME + ")");            
            }
          catch (ReteException re) {}
        }
    }
  }

  interface SerializableJessListener extends JessListener, Serializable {}

  /**
   * Set a Main object up for later execution.
   * @param argv Command-line arguments
   * @param r An initialized Rete object, with routers set up
   */
  public Main initialize(String [] argv, Rete r)
  {
    m_rete = r;        

    // **********************************************************************
    // Process any command-line switches
    int argIdx = 0;
    boolean doLogo = true;
    if (argv.length > 0)
      while(argIdx < argv.length && argv[argIdx].startsWith("-"))
      {
        if (argv[argIdx].equals("-nologo"))
          doLogo = false;
        argIdx++;
      }

    // **********************************************************************
    // Print banner
    if (doLogo)
        showLogo();
                
    // **********************************************************************
    // Open a file if requested
    m_fis = m_rete.getInputRouter("t");
    String name = argv.length <= argIdx ? null : argv[argIdx];

    try
      {
        if (name != null)
          {
            if (m_rete.getApplet() == null)
              m_fis = new BufferedReader(new FileReader(name));
            else
              {
                URL url
                  = new URL(m_rete.getApplet().getDocumentBase(),
                            name);          
                m_fis = new BufferedReader(new InputStreamReader(url.openStream())); 
              }         
            m_readStdin = false;
          }
      }
    catch (IOException ioe) 
      {
        m_rete.getErrStream().println("File not found or cannot open file:" +
                                 ioe.getMessage());
        m_rete.getErrStream().flush();
        System.exit(0);
      } 

    loadOptionalFunctions();
    return this ;           
  }
  
  private void loadOptionalFunctions()
  {
    // **********************************************************************
    // Load in optional packages, but don't fail if any are missing.
    String [] packages = { "coc.agent.engine.BagFunctions",
                           "coc.agent.engine.ViewFunctions" };

    for (int i=0; i< packages.length; i++)
      {
        try
          {
            m_rete.addUserpackage((Userpackage)
                                Class.forName(packages[i]).newInstance());
          }
        catch (Throwable t) { /* Optional package not present, OK */ }
      }

    // **********************************************************************
    // Arrange for handling of the 'bsave' & 'bload' commands
    
    try
      {
        DumpFunctions df = new DumpFunctions();
        m_rete.addUserpackage(df);
        df.addJessListener(this);

    // **********************************************************************
    // Read script library file, if one exists; arrange to have it reloaded on CLEAR

        m_rete.executeCommand("(batch " + LIBRARY_NAME + ")");
        m_rete.addJessListener(new LibraryLoader());
      }
    catch (ReteException re)
      {
        m_rete.getErrStream().println("Error processing script library file:");
        re.printStackTrace(m_rete.getErrStream());
        if (re.getNextException() != null)
          {
            m_rete.getErrStream().println("Nested exception is:");
            re.getNextException().printStackTrace(m_rete.getErrStream());
          }
        m_rete.getErrStream().flush();
      }
  }

  /**
   * Repeatedly parse and excute commands, from location determined during initialize().
   * @param doPrompt True if a prompt should be printed, false otherwise.
   * Prompts will never be printed during a (batch) command.
   */

  public void execute(boolean doPrompt)
  {
    // **********************************************************************
    // Process input from file or keyboard
    
    if (m_fis != null) 
      {
        m_j = new Jesp(m_fis, m_rete);
        do 
          {
            try
              {
                // Argument is 'true' for prompting, false otherwise
                m_j.parse(doPrompt && m_readStdin);
              }                        
            catch (ReteException re) 
              {
                if (re.getNextException() != null)
                  {
                    m_rete.getErrStream().write(re.toString());                    
                    m_rete.getErrStream().write("\nNested exception is:\n");    
                    m_rete.getErrStream().println(re.getNextException().getMessage());
                    re.getNextException().printStackTrace(m_rete.getErrStream());
                  }
                else
                  re.printStackTrace(m_rete.getErrStream());
              }
            catch (Exception e) 
              {
                m_rete.getErrStream().println("Unexpected exception:");
                e.printStackTrace(m_rete.getErrStream());
              }
            finally
              {
                m_rete.getErrStream().flush();
                m_rete.getOutStream().flush();                
              }
          }
        // Loop if we're using the command line
        while (m_readStdin);
      }
    // If called again, read stdin, not batch file    
    m_readStdin = true;
    m_fis = m_rete.getInputRouter("t");
  }

  /**
   * @param je 
   */
  public void eventHappened(JessEvent je) throws ReteException
  {
    if (je.getType() == JessEvent.BLOAD)
      {
        // Reattach parser to engine
        m_rete = (Rete) je.getObject();
        m_j.setEngine(m_rete);

        // Reinstall things not loaded by Rete constructor
        loadOptionalFunctions();
      }
  }
}




