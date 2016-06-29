package rtools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import Database.SingleUserDatabase.JEXWriter;
import jex.statics.JEXDialog;
import logs.Logs;
import miscellaneous.DirectoryManager;

public class ScriptRepository {

	public static String R_COMMON = "RCommon.R", LAPJV = "LAPJV", MAXIMA = "Maxima", MAXIMA_LIST = "MaximaList", PACKAGE_FUNCTIONS = "PackageFunctions", START_R_SERVE = "StartRServe", TRACK = "Track", TRACK_FILTERS = "TrackFilters", TRACK_FITTING = "TrackFitting", TRACKING = "Tracking", TRACK_LIST = "TrackList";

	public static void sourceGitHubFile(String user, String repository, String branch, String fileName)
	{
		// https://github.com/user/repository/raw/branch/filename
		String fileToGet = "https://github.com/" + user + "/" + repository + "/raw/" + branch + "/" + fileName;
		URL toGet;
		try
		{
			toGet = new URL(fileToGet);
			Logs.log("Attempting to source: " + fileToGet, ScriptRepository.class);
			String freePath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("R");
			File freeFile = new File(freePath);
			FileUtils.copyURLToFile(toGet, freeFile, 10000, 10000);
			R.eval("1");
			R.source(freeFile.getAbsolutePath());
		}
		catch(MalformedURLException e)
		{
			JEXDialog.messageDialog("Couldn't create a proper URL for source: " + fileToGet + ".  Check GitHub user, repository, branch, and filename are well constructed.", ScriptRepository.class);
			e.printStackTrace();
		}
		catch(IOException e)
		{
			JEXDialog.messageDialog("Couldn't source: " + fileToGet + ".  Check file exists at this url and you have a connection to the internet.", ScriptRepository.class);
			e.printStackTrace();
		}
	}
	
	public static void runSysCommandApache(String[] cmds)
	{
		runSysCommandApache(cmds, null, true);
	}
	
	public static void runSysCommandApache(String[] cmds, String directory)
	{
		runSysCommandApache(cmds, directory, true);
	}

	public static void runSysCommandApache(String[] cmds, String directory, boolean wait)
	{
		try
		{
			DefaultExecutor executor = new DefaultExecutor();
			if(directory != null )
			{
				File dir = new File(directory);
				if(dir.isDirectory() && dir.exists())
				{
					executor.setWorkingDirectory(new File(directory));
				}
				else
				{
					throw new IOException("Couldn't find the specified working directory.");
				}
			}

			for(String cmd : cmds)
			{
				DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
				executor.setStreamHandler(streamHandler);
				executor.execute(CommandLine.parse(cmd), EnvironmentUtils.getProcEnvironment(), resultHandler);
				if(wait)
				{
					resultHandler.waitFor();
					if(resultHandler.getException() != null)
					{
						Logs.log(resultHandler.getException().getMessage(), Logs.ERROR, ScriptRepository.class);
					}
					else
					{
						Logs.log(outputStream.toString(), ScriptRepository.class);
					}
				}
			}
		}
		catch(ExecuteException e){
			e.printStackTrace();
		}
		catch(IOException e2){
			e2.printStackTrace();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	public static void runRScript(String[] scriptFilePaths)
	{
		String[] cmds = new String[2*scriptFilePaths.length + 1];
		cmds[0] = "Rscript";
		int j = 0;
		for(int i = 1; i < cmds.length; i++)
		{
			if(i % 2 == 1)
			{
				cmds[i] = "-e";
			}
			else
			{
				cmds[i] = scriptFilePaths[j];
				j++;
			}
		}
		runSysCommandApache(cmds);
	}

	private static File getScriptFile(String resourcePath)
	{
		// e.g., resourcePath = "rtools/StartRserve.R";
		String resource = resourcePath;
		ScriptRepository rep = new ScriptRepository();
		File scriptfile = null;
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
				return scriptfile;
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
			return null;
		}
		else
			// there is a useable file path already for use in the system command
		{
			try
			{
				scriptfile = new File(res.toURI());
				return scriptfile;
			}
			catch (URISyntaxException e)
			{
				e.printStackTrace();
			}
			return null;
		}
	}

	public static void startRServe()
	{
		File scriptfile = getScriptFile("rtools/StartRserve.R");
		if(scriptfile != null && scriptfile.exists())
		{
			runSysCommandApache(new String[] { "R CMD Rscript " + R.dQuote(scriptfile.getAbsolutePath()) }, null, false);
		}
		else
		{
			Logs.log("Couldn't find the script file to start R! This is where I tried to look: " + scriptfile.getAbsolutePath(), ScriptRepository.class);
		}

	}

}
