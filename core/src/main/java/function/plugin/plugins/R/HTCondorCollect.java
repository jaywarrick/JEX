package function.plugin.plugins.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.FileWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXDialog;
import logs.Logs;
import miscellaneous.FileUtility;
import miscellaneous.LSVList;
import miscellaneous.StringUtility;
import rtools.R;
import rtools.ScriptRepository;


/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 * 
 */

@Plugin(
		type = JEXPlugin.class,
		name="HTCondor Collect",
		menuPath="R",
		visible=true,
		description="Collect a R job from HTCondor."
		)
public class HTCondorCollect extends JEXPlugin {

	public static boolean firstTimeCalled = true;

	public HTCondorCollect()
	{}

	/////////// Define Inputs ///////////

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Username", description="This is your condor user name, which is the same is your Wisc user name, if you don't have one, you can't really use this.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="bbadger")
	String username;

	@ParameterMarker(uiOrder=2, name="Password", description="This is your condor password , which is the same is your Wisc password, if you don't have one, you can't really use this.", ui=MarkerConstants.UI_PASSWORD, defaultText="Quit peaking")
	String password;

	@ParameterMarker(uiOrder=3, name="Condor Submit Node", description="Which submit node are you using for HTCondor?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="submit-5.chtc.wisc.edu")
	String host;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Output Files", type=MarkerConstants.TYPE_ANY, flavor="", description="The files from the Condor results.", enabled=true)
	Vector<JEXData> output = new Vector<JEXData>();

	@Override
	public int getMaxThreads()
	{
		return 1; // R doesn't like multiple threads
	}

	private String id = null;
	private String exptName = null;

	@Override
	public boolean run(JEXEntry optionalEntry)
	{

		// For this entry id and experiment, grab all the files in a particular folder
		id = optionalEntry.getEntryID();
		exptName = StringUtility.removeAllWhitespace(optionalEntry.getEntryExperiment());

		// Zip the file contents
		String cmd1 = "cd " + R.sQuote("ChtcRun/" + exptName+"Results");
		String cmd2 = "zip -r " + id + ".zip " + id + "/"; 
		this.runCommands(cmd1, cmd2);

		// Transfer the zip to the temp folder and umpack the files
		String uniqueFolder = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath(null);
		this.transferFile("ChtcRun/" + exptName + "Results", id + ".zip", uniqueFolder);
		//String cmd3 = "scp " + username + "@" + host + ":" + R.sQuote("ChtcRun/" + exptName + "/" + id + ".zip") + " " + R.sQuote(uniqueFolder + File.separator + id + ".zip");
		String cmd3 = "unzip -d " + R.sQuote(uniqueFolder) + " " + R.sQuote(uniqueFolder + File.separator + id + ".zip");
		String cmd4 = "rm -rf " + R.sQuote(uniqueFolder + File.separator + id + ".zip");
		ScriptRepository.runSysCommand(new String[]{"sh", "-c", cmd3});
		ScriptRepository.runSysCommand(new String[]{"sh", "-c", cmd4});
		
		
		
		// Import the files as JEXData
		List<File> files = FileUtility.getSortedFileList((new File(uniqueFolder)).listFiles());
		for(File f : files)
		{
			if(f.isFile() && !f.getName().equals(id + ".zip"))
			{
				output.addElement(FileWriter.makeFileObject(f.getName(), null, f));
			}
		}

		return true;
	}

	public void runCommands(String... commands)
	{
		try {
			LSVList cmdList = new LSVList();
			for(String command : commands)
			{
				cmdList.add(command);
			}
			String commandString = cmdList.toString();

			Logs.log("Attempting ssh commands...", this);
			System.out.println(commandString);

			JSch ssh = new JSch();
			Session session = ssh.getSession(username, host, 22);
			session.setPassword(password);
			java.util.Properties config = new java.util.Properties(); 
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect(30000);
			ChannelExec channel= (ChannelExec) session.openChannel("exec");

			//		channel.setInputStream(System.in);
			//		/*
			//	      // a hack for MS-DOS prompt on Windows.
			//	      channel.setInputStream(new FilterInputStream(System.in){
			//	          public int read(byte[] b, int off, int len)throws IOException{
			//	            return in.read(b, off, (len>1024?1024:len));
			//	          }
			//	        });
			//		 */
			//
			//		channel.setOutputStream(System.out);

			channel.setCommand(commandString);
			BufferedReader in=new BufferedReader(new InputStreamReader(channel.getInputStream()));
			channel.connect();


			String msg=null;
			while((msg=in.readLine())!=null){
				Logs.log(msg, this);
			}

			channel.disconnect();
			session.disconnect();
		}
		catch (JSchException e)
		{
			e.printStackTrace();
			JEXDialog.messageDialog("Couldn't login to the HTCondor");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void transferFile(String srcDir, String fileToCopy, String dstDirPath)
	{
		try{
			JSch ssh = new JSch();
			Session session = ssh.getSession(username, host, 22);
			session.setPassword(password);
			java.util.Properties config = new java.util.Properties(); 
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect(30000);
			Channel channel = session.openChannel("sftp");
			channel.connect();
			ChannelSftp channelSftp = (ChannelSftp)channel;
			channelSftp.cd(srcDir);
			//channelSftp.put(new FileInputStream(fileToCopy), src.getName());
			
			byte[] buffer = new byte[1024];
			BufferedInputStream bis = new BufferedInputStream(channelSftp.get(fileToCopy));
			File newFile = new File(dstDirPath+File.separator+fileToCopy);
			newFile.getParentFile().mkdirs();
			OutputStream os = new FileOutputStream(newFile);
			BufferedOutputStream bos = new BufferedOutputStream(os);
			int readCount;
			//System.out.println("Getting: " + theLine);
			while( (readCount = bis.read(buffer)) > 0)
			{
				bos.write(buffer, 0, readCount);
			}
			bis.close();
			bos.close();

			channel.disconnect();
			session.disconnect();		

		}catch(Exception ex){
			ex.printStackTrace();
			JEXDialog.messageDialog("Couldn't transfer the files", this);
		}

	}
}
