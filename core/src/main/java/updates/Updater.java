package updates;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.DirectoryManager;
import miscellaneous.FileUtility;
import miscellaneous.LSVList;
import miscellaneous.Pair;
import miscellaneous.StringUtility;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;

import preferences.OS;

public class Updater {
	
	private static boolean runningFromJar = false;
	//	public static String currentVersion = "Version-0.0"; // Keep this syntax to make it download every time (change number to current number to only download if needed)
	
	public static final String RELEASE_DEVELOPMENT="Development", RELEASE_OFFICIAL="Official", CONNECTION="Connection";
	
	public static void attemptJEXUpdate()
	{		
		JEXStatics.statusBar.setStatusText("Attempting to update JEX...");
		// Check to see if we are running from an executable version of JEX
		
		if(!runningFromJar)
		{
			Logs.log("The -fromJar flag was not detected upon launching JEX to mark this as running from a jar. Therefore, assuming we are not running from a jar. Aborting attempt to update.", Logs.ERROR, Updater.class);
			JEXStatics.statusBar.setStatusText("Not running from a *.jar file... can't update jar file. Aborting update.");
			return;
		}
		
		String pathOfJEXExecutables = getPathOfJEXExecutablesFolder();
		
		// Force update to ensure latest version is being used.
		int version = JEXDialog.getChoice("Update JEX...", "Which version would you like?", new String[]{"Development Version","Last Official Release"}, 0);
		Logs.log("Forcing update to latest download from sourceforge file repository.\n\n", Updater.class);
		JEXStatics.statusBar.setStatusText("Updating. Step 1 of 1 - Downloading...");
		TreeMap<String,Object> connectionResults = connectToServerFile(version == 0);
		HttpURLConnection httpConn = (HttpURLConnection) connectionResults.get(CONNECTION);
		
		// Download JEX
		String download = downloadJEXDistribution(httpConn);
		
		if(download == null)
		{
			Logs.log("Unable to update. We'll try nextTime.", Updater.class);
			JEXStatics.statusBar.setStatusText("Download failed. Update aborted. Please try again later.");
			return;
		}
		
		// Replace old JEX
		JEXStatics.statusBar.setStatusText("Updating. Step 2 of 3 - Replacing old JEX with new JEX...");
		boolean success = updateJEXFilesWithThisZip(download, pathOfJEXExecutables);
		if(!success)
		{
			Logs.log("Unable to update. We'll try nextTime.", Updater.class);
			JEXStatics.statusBar.setStatusText("Couldn't replace old JEX files. Update aborted.");
			return;
		}
		
		// Restart JEX
		JEXStatics.statusBar.setStatusText("Updating. Step 3 of 3 - Restarting JEX in 1 second...");
		try
		{
			Thread.sleep(1000);
			restartJEX(pathOfJEXExecutables);
		}
		catch (InterruptedException e)
		{
			JEXStatics.statusBar.setStatusText("?!?! Couldn't successfully autorestart ?!?! Manually closing and reopening JEX.");
			e.printStackTrace();
		}
		
	}
	
	public static void openWebpage(String address)
	{
		if(Desktop.isDesktopSupported())
		{
			URL page;
			try
			{
				page = new URL(address);
				Desktop.getDesktop().browse(page.toURI());
			}
			catch (MalformedURLException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (URISyntaxException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static boolean runningFromJar()
	{
		return runningFromJar;
	}
	
	public static void setRunningFromJar(boolean runningFromJar)
	{
		Updater.runningFromJar = runningFromJar;
	}
	
	public static void restartJEX(String pathOfJEXExecutables)
	{
		String commandFile = pathOfJEXExecutables + File.separator + "JEX for Windows.bat";
		if(OS.isMacOSX())
		{
			commandFile = pathOfJEXExecutables + File.separator + "JEX for Mac.command";
		}
		
		try
		{
			FileUtility.openFileDefaultApplication(commandFile);
		}
		catch (Exception e)
		{
			Logs.log("Couldn't start newly updated JEX. Aborting restart to keep current JEX open.", Updater.class);
			JEXStatics.statusBar.setStatusText("Couldn't start updated JEX. Shutdown of old JEX aborted.");
			e.printStackTrace();
			return;
		}
		
		// Close JEX through a window event so we don't get an unexecpected close warning upon quiting.
		WindowEvent wev = new WindowEvent(JEXStatics.main, WindowEvent.WINDOW_CLOSING);
		Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
	}
	
	public static Pair<String,LSVList> getVersionInfo(HttpURLConnection httpConn)
	{
		String version = null;
		LSVList lines = new LSVList();
		BufferedReader rd = null;
		try
		{
			// Read the stream from the server, storing the text from the text file as it comes.
			rd = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
			String line = null;
			lines = new LSVList();
			while ((line = rd.readLine()) != null)
			{
				if(line.startsWith("Version-"))
				{
					String temp = line;
					if(temp.endsWith("\n"))
					{
						temp.substring(0, line.length() - 1); // Omit the \n newline character at the end
					}
					else
					{
						temp.substring(0, line.length());
					}
					version = temp;
				}
				lines.add(line);
			}
		}
		catch (Exception e)
		{
			Logs.log("Couldn't download file after successful connection to the server.", Updater.class);
			version = null;
			lines = new LSVList();
		}
		finally
		{
			// Close connections
			if(rd != null)
			{
				try
				{
					rd.close();
				}
				catch (Exception e)
				{}
			}
		}
		
		return new Pair<String,LSVList>(version, lines);
	}
	
	public static HttpURLConnection getConnectionTo(String urlPath)
	{
		HttpURLConnection httpConn = null;
		
		// Check the version on the sourceforge repository
		try
		{
			// FileUtility.openFileDefaultApplication("/Users/jaywarrick/Public/Drop Box/Java/JEX Deployment/JEX Executables/JEX.command");
			// Got the following URL by going to sourceforge, clicking the download link for the Version.txt file and copying the link information on the following page that indicates the "direct link" to the file.
			// Set up the connection
			// ////////////////////////////////////////////////https://downloads.sourceforge.net/project/jextools/Version.txt?r=&ts=1368817297&use_mirror=master
			// Make sure the URL doesn't have the httpS in it, only http. Otherwise the redirect gets us into a neverending loop.
			httpConn = (HttpURLConnection) (new java.net.URL(urlPath).openConnection()); // "https://sourceforge.net/projects/jextools/files/Version.txt/download").openConnection());
			httpConn.setRequestMethod("GET");
			httpConn.setDoOutput(true);
			httpConn.setReadTimeout(20000);
			httpConn.setInstanceFollowRedirects(true);
			
			// Try to connect
			httpConn.connect();
			
			// Check the connection and try again for 10 s if need be
			int responseCode = httpConn.getResponseCode();
			double t = System.currentTimeMillis();
			String newLocationHeader = null;
			while ((responseCode / 100) == 3 && System.currentTimeMillis() < t + 10000)
			{ /* codes 3XX are redirections */
				newLocationHeader = httpConn.getHeaderField("Location");
				Logs.log(newLocationHeader, Updater.class);
				/* open a new connection and get the content for the URL newLocationHeader */
				httpConn.disconnect();
				httpConn.connect();
				responseCode = httpConn.getResponseCode();
				/* do it until you get some code that is not a redirection */
				Logs.log("Getting " + urlPath + " on sourceforge server. Current http response code: " + responseCode + ", Waiting time = " + (System.currentTimeMillis() - t) / 1000 + " s.", Updater.class);
				Thread.sleep(500);
			}
			if(responseCode == 404)
			{
				Logs.log("Couldn't connect to " + urlPath + " on the sourceforge server. Server not available (Http Error Code 404).", Updater.class);
				closeConnection(httpConn);
				return null;
			}
			if((responseCode / 100) == 3)
			{
				Logs.log("Server took too long to redirect to the download url " + newLocationHeader + " for latest download. (Http Error Code " + responseCode + ")", Updater.class);
				closeConnection(httpConn);
				return null;
			}
		}
		catch (Exception e)
		{
			Logs.log("Error while trying to connect to"+ urlPath + " on sourceforge server. Check internet connection.", Updater.class);
			closeConnection(httpConn);
			return null;
		}
		return httpConn;
	}
	
	/**
	 * Connect to sourceforge. Parse the latest download to determine the latest
	 * developmental release (i.e., SNAPSHOT version), official release, and also
	 * return the actuall HttpURLConnection. The keys for each are the static strings,
	 * RELEASE_DEVELOPMENT, RELEASE_OFFICIAL, and CONNECTION.
	 * @return TreeMap<String,Object>
	 */
	public static TreeMap<String,Object> connectToServerFile(boolean developmentalVersion)
	{
		HttpURLConnection httpConn = getConnectionTo("http://sourceforge.net/projects/jextools/files/latest/download?source=files");
		
		if(httpConn != null)
		{
			TreeMap<String,Object> ret = new TreeMap<String,Object>();
			String developmentFileName = httpConn.getURL().getPath();
			String baseFileName = FileUtility.getFileNameWithoutExtension(developmentFileName);
			String[] pieces = baseFileName.split("-");
			String[] versionNums = pieces[1].split(".");
			Integer temp = Integer.parseInt(versionNums[2])-1;
			String officialVersionNum = versionNums[0] + "." + versionNums[1] + "." + temp;
			String officialReleaseFileName = pieces[0] + "-" + officialVersionNum + ".zip";
			ret.put(RELEASE_DEVELOPMENT, developmentFileName);
			ret.put(RELEASE_OFFICIAL, officialReleaseFileName);
			
			if(!developmentalVersion)
			{
				httpConn.disconnect();
				
				// Make sure the URL doesn't have the httpS in it, only http. Otherwise the redirect gets us into a neverending loop.
				httpConn = getConnectionTo("http://sourceforge.net/projects/jextools/files/" + officialReleaseFileName + "/download");
				if(httpConn != null)
				{
					ret.put(CONNECTION, httpConn);
				}
				else
				{
					return null;
				}
			}
			else
			{
				ret.put(CONNECTION, httpConn);
			}
			
			return ret;
		}
		else
		{
			return null;
		}
	}
	
	public static void closeConnection(HttpURLConnection httpConn)
	{
		if(httpConn != null)
		{
			httpConn.disconnect();
		}
	}
	
	/**
	 * Get the most recent JEXDistribution
	 * @param httpConn
	 * @return TreeMap<String,String> containing the "Development" zip file name
	 */
	public static String downloadJEXDistribution(HttpURLConnection httpConn)
	{
		BufferedInputStream from = null;
		FileOutputStream to = null;
		String placeToSave = null;
		try
		{
			// Check the size of the file
			Map<String,List<String>> values = httpConn.getHeaderFields();
			Integer fileSize = 1;
			if(values != null && !values.isEmpty())
			{
				String sLength = values.get("Content-Length").get(0);
				if(sLength != null)
				{
					fileSize = Integer.parseInt(sLength);
				}
			}
			if(fileSize == 1)
			{
				Logs.log("JEX is out of date but we couldn't find a complete distribution file on the server. We'll try next time.", Updater.class);
				return null;
			}
			
			// Open the input stream to the zip file.
			from = new BufferedInputStream(httpConn.getInputStream());
			
			// Set up a filename and buffer for writing the file.
			placeToSave = DirectoryManager.getUniqueAbsoluteTempPath("zip");
			Logs.log("Place to save download: " + placeToSave, Updater.class);
			File placeToSaveFile = new File(placeToSave);
			if(placeToSaveFile.exists())
			{
				placeToSaveFile.delete();
			}
			to = new FileOutputStream(new File(placeToSave)); // Create output stream
			byte[] buffer = new byte[4096]; // To hold file contents
			int bytes_read; // How many bytes in buffer
			
			// Write the file.
			// Read a chunk of bytes into the buffer, then write them out, looping until we reach the end of the file (when read() returns -1).
			double totalRead = 0;
			int currentPercentage = 0;
			JEXStatics.statusBar.setStatusText("Downloading JEX Update...");
			JEXStatics.statusBar.setProgressPercentage(currentPercentage);
			Logs.log("Download progress... " + currentPercentage + "%", Updater.class);
			int lastPercentage = currentPercentage;
			while ((bytes_read = from.read(buffer)) != -1)
			{
				// Read until EOF
				to.write(buffer, 0, bytes_read); // write
				
				// Update the progress bar
				totalRead = totalRead + bytes_read;
				currentPercentage = ((int) ((totalRead / fileSize) * 100.0));
				if(lastPercentage != currentPercentage)
				{
					Logs.log("Download progress... " + currentPercentage + "%", Updater.class);
					JEXStatics.statusBar.setProgressPercentage(currentPercentage);
					lastPercentage = currentPercentage;
				}
			}
			Logs.log("JEX successfully downloaded to " + placeToSave, Updater.class);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			// Always close the streams, even if exceptions were thrown
			if(from != null)
			{
				try
				{
					from.close();
				}
				catch (IOException e)
				{}
			}
			if(to != null)
			{
				try
				{
					to.flush();
					to.close();
				}
				catch (IOException e)
				{}
			}
			if(httpConn != null)
			{
				try
				{
					httpConn.disconnect();
				}
				catch (Exception e)
				{}
			}
		}
		return placeToSave;
	}
	
	public static boolean updateJEXFilesWithThisZip(String pathToDownloadedZip, String pathOfJEXExecutablesFolder)
	{
		boolean ret = true;
		
		try
		{
			// create output directory is not exists
			String outputFolder = DirectoryManager.getTempFolderPath() + File.separator + "UpdateFiles";
			File folder = new File(outputFolder);
			if(!folder.exists())
			{
				folder.mkdir();
			}
			
			ZipFile zf = new ZipFile(pathToDownloadedZip);
			Logs.log("Extracting JEX update from: " + pathToDownloadedZip + ", To: " + outputFolder, Updater.class);
			zf.extractAll(outputFolder);
			Logs.log("Installing JEX update from: " + outputFolder + ", To: " + pathOfJEXExecutablesFolder, Updater.class);
			FileUtils.copyDirectory(new File(outputFolder), new File(pathOfJEXExecutablesFolder));
		}
		catch (ZipException e)
		{
			e.printStackTrace();
			ret = false;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			ret = false;
		}
		return ret;
	}
	
	public static String getPathOfJEXExecutablesFolder()
	{
		if(Updater.runningFromJar())
		{			
			// Then we are running JEX from the JEX.jar
			try
			{
				// String jarFolderPath = Updater.class.getProtectionDomain().getCodeSource().toURI().getPath();
				String jarFolderPath = ClassLoader.getSystemClassLoader().getResource(".").toURI().getPath();
				Logs.log("System JEX.jar path... " + jarFolderPath, Updater.class);
				return jarFolderPath;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				Logs.log("Error occurred while trying to get the path to the JEX executables folder. Aborting and returning null for file path.", Logs.ERROR, Updater.class);
				return null;
			}
		}	
		Logs.log("The -fromJar flag was not detected upon launching JEX to mark this as running from a jar. Therefore, assuming we are not running from a jar. Aborting and returning null for file path to jar.", Logs.ERROR, Updater.class);
		return null;
	}
	
	public static boolean javaVersionIsAtLeast(String requiredVersion)
	{
		String currentVersion = System.getProperty("java.version");
		
		int result = StringUtility.compareString(requiredVersion, currentVersion);
		if(result > 0)
		{
			return false;
		}
		return true;
	}
}
