package function.plugin.plugins.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXReader;
import Database.SingleUserDatabase.JEXWriter;
import cruncher.Ticket;
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
		name="HTCondor Collect_Fast",
		menuPath="R",
		visible=true,
		description="Collect a R job from HTCondor."
		)
public class HTCondorCollect_Fast extends JEXPlugin {

	public static boolean firstTimeCalled = true;

	public HTCondorCollect_Fast()
	{}

	/////////// Define Inputs ///////////

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Username", description="This is your condor user name, which is the same is your Wisc user name, if you don't have one, you can't really use this.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="bbadger")
	String username;

	@ParameterMarker(uiOrder=2, name="Password", description="This is your condor password , which is the same is your Wisc password, if you don't have one, you can't really use this.", ui=MarkerConstants.UI_PASSWORD, defaultText="Quit peaking")
	String password;

	@ParameterMarker(uiOrder=3, name="Condor Submit Node", description="Which submit node are you using for HTCondor?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="submit-5.chtc.wisc.edu")
	String host;

	@ParameterMarker(uiOrder=4, name="Submit Folder To Collect From", description="Which submit node folder are you collecting data from?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="CHTCRun")
	String submitFolder;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Output Files", type=MarkerConstants.TYPE_ANY, flavor="", description="The files from the Condor results.", enabled=true)
	Vector<JEXData> output = new Vector<JEXData>();

	@Override
	public int getMaxThreads()
	{
		return 1; // R doesn't like multiple threads
	}

	public static TreeSet<String> sExptNames = new TreeSet<>();
	public static TreeMap<String,TreeSet<String>> sIds = new TreeMap<>();

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// For this entry id and experiment, store the needed information for zipping and unpacking later
		sExptNames.add(StringUtility.removeAllWhitespace(optionalEntry.getEntryExperiment()));
		TreeSet<String> temp = sIds.get(StringUtility.removeAllWhitespace(optionalEntry.getEntryExperiment()));
		if(temp == null || temp.size() == 0)
		{
			temp = new TreeSet<String>();
		}
		temp.add(optionalEntry.getEntryID());
		sIds.put(StringUtility.removeAllWhitespace(optionalEntry.getEntryExperiment()), temp);

		return true;
	}

	public void runCommands(LSVList cmdList)
	{
		try {
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

	public void finalizeTicket(Ticket ticket)
	{
		LSVList commandsToRun = new LSVList();

		TreeMap<JEXEntry, Set<JEXData>> outputList = ticket.getOutputList();

		for(String exptName : sExptNames)
		{
			
			// Zip the file contents
			String cmd1 = "cd " + R.sQuote(submitFolder);
			String cmd2 = "zip -r " + exptName + ".zip " + exptName + "/"; 
			commandsToRun.add(cmd1);
			commandsToRun.add(cmd2);
		}

		this.runCommands(commandsToRun);

		// Transfer the zip to the temp folder and umpack the files
		String uniqueFolder = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath(null);

		for(String exptName : sExptNames)
		{
			try {
				Thread.sleep(30000);                
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			};
			this.transferFile(this.submitFolder, exptName + ".zip", uniqueFolder);

			
			
			//String cmd3 = "scp " + username + "@" + host + ":" + R.sQuote("ChtcRun/" + exptName + "/" + id + ".zip") + " " + R.sQuote(uniqueFolder + File.separator + id + ".zip");
			String cmd3 = "unzip -d " + R.dQuote(uniqueFolder) + " " + R.dQuote(uniqueFolder + File.separator + exptName + ".zip"); //for Windows dQuote(), for Unix sQuote()
			String cmd4 = "rm -rf " + R.dQuote(uniqueFolder + File.separator + exptName + ".zip");
			ScriptRepository.runSysCommandApache(new String[]{cmd3});
			//ScriptRepository.runSysCommandApache(new String[]{"ls", "unzip 2016-04-22MMAdhesionPt617.zip"}, "D:/Teddy/Jex/Brightfield Adhesion/temp/JEXData0000000000");
			ScriptRepository.runSysCommandApache(new String[]{cmd4});
		}

		for(JEXEntry entry : outputList.keySet())
		{
			Set<JEXData> datas = outputList.get(entry);
			if(datas == null || datas.size() == 0)
			{
				datas = new HashSet<JEXData>();
			}
			datas.clear();
			// Import the files as JEXData
			List<File> files = FileUtility.getSortedFileList((new File(uniqueFolder + File.separator + StringUtility.removeAllWhitespace(entry.getEntryExperiment()) + File.separator + entry.getEntryID())).listFiles());
			for(File f : files)
			{
				if(f.isFile() && !f.getName().equals(entry.getEntryID() + ".zip"))
				{
					if(FileUtility.getFileNameExtension(f.getName()).equals("jxd"))
					{
						JEXData roi = JEXReader.readFileToJEXData(f.getAbsolutePath(), new TypeName(JEXData.ROI, f.getName()));
						datas.add(roi);
					}
					else
					{
						datas.add(FileWriter.makeFileObject(f.getName(), null, f));
					}
				}
			}
			outputList.put(entry, datas);
		}
	}

}
