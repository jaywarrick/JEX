package rtools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;

import logs.Logs;
import miscellaneous.DirectoryManager;

public class ScriptRepository {
	
	public static void startRServe()
	{
		ScriptRepository rep = new ScriptRepository();
		File scriptfile = null;
		String resource = "rtools/StartRserve.R";
		URL res = rep.getClass().getClassLoader().getResource(resource);
		if(res.toString().startsWith("jar:"))
		{ // Need to copy the script out of the jar to create a useable file path for the system command.
			try
			{
				InputStream input = rep.getClass().getClassLoader().getResourceAsStream(resource);
				scriptfile = File.createTempFile("StartRserve", ".R", new File(DirectoryManager.getTempFolderPath()));
				OutputStream out = new FileOutputStream(scriptfile);
				int read;
				byte[] bytes = new byte[1024];
				
				while ((read = input.read(bytes)) != -1)
				{
					out.write(bytes, 0, read);
				}
				
				out.close();
				scriptfile.deleteOnExit();
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		}
		else
		// there is a useable file path already for use in the system command
		{
			try
			{
				scriptfile = new File(res.toURI());
			}
			catch (URISyntaxException e)
			{
				e.printStackTrace();
			}
			
		}
		
		if(scriptfile != null && scriptfile.exists())
		{
			runSysCommand(new String[] { "R", "CMD", "Rscript", scriptfile.getAbsolutePath() });
		}
		else
		{
			Logs.log("Couldn't find the script file to start R! This is where I tried to look: " + scriptfile.getAbsolutePath(), ScriptRepository.class);
		}
		
	}
	
	public static void runSysCommand(String[] cmds)
	{
		try
		{
			Process p = Runtime.getRuntime().exec(cmds);
			
			// // Debug code only... causes process to stall waiting for more
			// lines to be read from the process.
			// BufferedReader stdInput = new BufferedReader(new
			// InputStreamReader(p.getInputStream()));
			// BufferedReader stdError = new BufferedReader(new
			// InputStreamReader(p.getErrorStream()));
			//
			// // read the output from the command
			// JEXStatics.logManager.log("Here is the standard output of the command (if any):\n",
			// 0, ScriptRepository.class.getSimpleName());
			// while ((s = stdInput.readLine()) != null)
			// {
			// JEXStatics.logManager.log(s, 0,
			// ScriptRepository.class.getSimpleName());
			// }
			//
			// // read any errors from the attempted command
			// JEXStatics.logManager.log("Here is the standard error of the command (if any):\n",
			// 0, ScriptRepository.class.getSimpleName());
			// while ((s = stdError.readLine()) != null)
			// {
			// JEXStatics.logManager.log(s, 0,
			// ScriptRepository.class.getSimpleName());
			// }
			//
			// stdInput.close();
			// stdError.close();
			
			p.waitFor();
		}
		catch (IOException e)
		{
			System.out.println("exception happened - here's what I know: ");
			e.printStackTrace();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
}
