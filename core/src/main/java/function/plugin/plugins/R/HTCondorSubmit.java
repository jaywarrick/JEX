package function.plugin.plugins.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.scijava.plugin.Plugin;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.SingleUserDatabase.JEXWriter;
import cruncher.Ticket;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXDialog;
import logs.Logs;
import miscellaneous.FileUtility;
import miscellaneous.LSVList;
import miscellaneous.StringUtility;
import rtools.R;
import rtools.ScriptRepository;
import tables.DimensionMap;


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
		name="HTCondor Submit",
		menuPath="R",
		visible=true,
		description="Submit a R job to HTCondor. Follow step 1 of these instructions before running this function to be able to use this function in HTCondor http://chtc.cs.wisc.edu/DAGenv.shtml"
		)
public class HTCondorSubmit extends JEXPlugin {

	public static boolean firstTimeCalled = true;

	public HTCondorSubmit()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Files to R", type=MarkerConstants.TYPE_ANY, description="JEXData to be passed as a data.frame and called data1 for use in the provided R command.", optional=true)
	JEXData files2Process;

	@InputMarker(uiOrder=2, name="R Scripts", type=MarkerConstants.TYPE_FILE, description="JEXData to be passed as a data.frame and called data1 for use in the provided R command.", optional=true)
	JEXData rScripts;

	@InputMarker(uiOrder=3, name="R Libraries (zipped)", type=MarkerConstants.TYPE_FILE, description="JEXData to be passed as a data.frame and called data1 for use in the provided R command.", optional=true)
	JEXData rLibs;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Username", description="This is your condor user name, which is the same is your Wisc user name, if you don't have one, you can't really use this.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="bbadger")
	String username;

	@ParameterMarker(uiOrder=2, name="Password", description="This is your condor password , which is the same is your Wisc password, if you don't have one, you can't really use this.", ui=MarkerConstants.UI_PASSWORD, defaultText="Quit peaking")
	String password;

	@ParameterMarker(uiOrder=3, name="Condor Submit Node", description="Which submit node are you using for HTCondor?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="submit-5.chtc.wisc.edu")
	String host;

	@ParameterMarker(uiOrder=3, name="Input File Name", description="The name of your infile in the R code", ui=MarkerConstants.UI_TEXTFIELD, defaultText="ROI.jxd")
	String inFile;

	@ParameterMarker(uiOrder=3, name="Output File Pattern", description="Common prefix of output files in the R code", ui=MarkerConstants.UI_TEXTFIELD, defaultText="output")
	String outFile;



	/////////// Define Outputs ///////////
	//No outputs here

	@Override
	public int getMaxThreads()
	{
		return 1; // R doesn't like multiple threads
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		try {
			File dir = new File(JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getTempFolderName() + File.separator + StringUtility.removeAllWhitespace(optionalEntry.getEntryExperiment()));

			if(firstTimeCalled)
			{
				// Prepare the folder structure
				this.prepareFolderStructure(dir);
				firstTimeCalled = false;
			}

			// Now add the items specific to this entry into our prepared folder structure
			String fileToProcessPath = readObjectToFilePathTable(files2Process).firstEntry().getValue();
			File fileToProcess = new File(fileToProcessPath);
			File dstFile = new File(dir.getAbsolutePath() + File.separator + optionalEntry.getEntryID() + File.separator + StringUtility.removeAllWhitespace(inFile));
			dstFile.getParentFile().mkdirs();
			FileUtils.copyFile(fileToProcess, dstFile);

						
			//transferFolder(new File(dir.getAbsolutePath() + File.separator + optionalEntry.getEntryID()), "ChtcRun/" + StringUtility.removeAllWhitespace(optionalEntry.getEntryExperiment()) + File.separator + optionalEntry.getEntryID());
			//		String cmd4 = "scp -r " + R.sQuote(dir.getAbsolutePath()) + " " + username + "@" + host + ":~/ChtcRun/" + StringUtility.removeAllWhitespace(optionalEntry.getEntryExperiment());
			//		String cmd5 = password;
			//	ScriptRepository.runSysCommand(new String[]{cmd4, cmd5});

			// Run commands to submit the job
			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			JEXDialog.messageDialog("Couldn't copy the files to process for Entry X:" + optionalEntry.getTrayX() + " Y:" + optionalEntry.getTrayY(), this);
			return false;
		}
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

	public void transferFile(File src, String dst)
	{
		try{
			JSch ssh = new JSch();
			Session session = ssh.getSession(username, host, 22);
			session.setPassword(password);
			java.util.Properties config = new java.util.Properties(); 
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect(3000);
			Channel channel = session.openChannel("sftp");
			channel.connect();
			ChannelSftp channelSftp = (ChannelSftp)channel;
			channelSftp.cd(dst);
			channelSftp.put(new FileInputStream(src), src.getName());

			channel.disconnect();
			session.disconnect();		

		}catch(Exception ex){
			ex.printStackTrace();
			JEXDialog.messageDialog("Couldn't transfer the files", this);
		}

	}



	public boolean prepareFolderStructure(File dir)
	{
		try {
			// Remove existing directory on CHTC
			String cmd1 = "cd ChtcRun";
			String cmd2 = "rm -r " + StringUtility.removeAllWhitespace(dir.getName());
			String cmd3 = "rm -r " + StringUtility.removeAllWhitespace(dir.getName()) + "OUT";
			this.runCommands(cmd1, cmd2, cmd3);

			// Get the RScript
			File rScriptFile = FileReader.readFileObjectToFile(rScripts);
			if(!rScriptFile.exists())
			{
				JEXDialog.messageDialog("Couldn't find the file: " + rScriptFile.getAbsolutePath(), this);
				return false;
			}

			// Get the Rlibs
			File rLibsFile = FileReader.readFileObjectToFile(rLibs);
			if(!rLibsFile.exists())
			{
				JEXDialog.messageDialog("Couldn't find the file: " + rLibsFile.getAbsolutePath(), this);
				return false;
			}

			// Get a temp folder to put stuff in
			if(dir.exists())
			{
				FileUtils.deleteDirectory(dir);
			}

			dir.mkdir();
			File dstRScript = new File(dir.getAbsolutePath() + File.separator + "shared/rScript.R");
			File dstRLibs = new File(dir.getAbsolutePath() + File.separator + "shared/rLibs.tar.gz");
			FileUtils.copyFile(rScriptFile, dstRScript);
			FileUtils.copyFile(rLibsFile, dstRLibs);

			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			JEXDialog.messageDialog("Couldn't delete/create/fill the directory with Rscript and RLibs:" + dir.getPath() + ". Check that all files inside the directory are not open in an application (e.g., windows).", this);
			return false;
		}
	}
	
	private static TreeMap<DimensionMap,String> readObjectToFilePathTable(JEXData data)
	{
		TreeMap<DimensionMap,String> result = new TreeMap<DimensionMap,String>();
		if(data.getTypeName().getType().equals(JEXData.ROI))
		{
			String dataFolder = data.getDetachedRelativePath();
			dataFolder = JEXWriter.getDatabaseFolder() + File.separator + dataFolder;
			result.put(new DimensionMap("File=1"), dataFolder);
		}
		else
		{
			JEXDataSingle ds = data.getFirstSingle();
			String dataFolder = (new File(FileReader.readToPath(ds))).getParent(); 			
			for (DimensionMap map : data.getDataMap().keySet())
			{
				ds = data.getData(map);
				String path = readToPath(dataFolder, ds);
				result.put(map, path);
			}
		}
		
		return result;
	}
	
	private static String readToPath(String dataFolder, JEXDataSingle ds)
	{
		String fileName = FileUtility.getFileNameWithExtension(ds.get(JEXDataSingle.RELATIVEPATH));
		String result = dataFolder + File.separator + fileName;
		return result;
	}

	public void finalizeTicket(Ticket ticket)
	{
		//String cmd1 = "cd "+ JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getTempFolderName();
		String cmd2 = "zip -r " + "zipfile.zip" +" " + R.sQuote(StringUtility.removeAllWhitespace(ticket.getOutputList().firstEntry().getKey().getEntryExperiment()));
		ScriptRepository.runSysCommand(new String[]{"sh", "-c", cmd2}, JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getTempFolderName());
		transferFile(new File(JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getTempFolderName() + File.separator + "zipfile.zip"), "ChtcRun");
		this.runCommands("cd ChtcRun", "unzip zipfile.zip", "rm zipfile.zip","./mkdag --data="+StringUtility.removeAllWhitespace(ticket.getOutputList().firstEntry().getKey().getEntryExperiment())+" --outputdir="+StringUtility.removeAllWhitespace(ticket.getOutputList().firstEntry().getKey().getEntryExperiment())+"OUT"+" --cmdtorun=rScript.R --pattern="+outFile+" --type=R --version=R-3.2.0", "cd "+StringUtility.removeAllWhitespace(ticket.getOutputList().firstEntry().getKey().getEntryExperiment())+"OUT", "condor_submit_dag mydag.dag" );
		
		firstTimeCalled = true;
	}


}
