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
import miscellaneous.CSVList;
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
		name="HTCondor Submit (Updated)",
		menuPath="R",
		visible=true,
		description="Submit a R job to HTCondor. Follow step 1 of these instructions before running this function to be able to use this function in HTCondor http://chtc.cs.wisc.edu/DAGenv.shtml"
		)
public class HTCondorSubmit_Updated extends JEXPlugin {

	public static boolean firstTimeCalled = true;

	public HTCondorSubmit_Updated()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="R Script", type=MarkerConstants.TYPE_FILE, description="This is your R script .R file that R will run in condor", optional=true)
	JEXData rScript;

	@InputMarker(uiOrder=2, name="Input File 1", type=MarkerConstants.TYPE_ANY, description="JEXData (File, Image, or ROI) to be passed to R to run in Condor", optional=true)
	JEXData file1;

	@InputMarker(uiOrder=3, name="Input File 2", type=MarkerConstants.TYPE_ANY, description="JEXData (File, Image, or ROI) to be passed to R to run in Condor", optional=true)
	JEXData file2;

	@InputMarker(uiOrder=4, name="Input File 3", type=MarkerConstants.TYPE_ANY, description="JEXData (File, Image, or ROI) to be passed to R to run in Condor", optional=true)
	JEXData file3;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Username", description="This is your condor user name, which is the same is your Wisc user name, if you don't have one, you can't really use this.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="bbadger")
	String username;

	@ParameterMarker(uiOrder=2, name="Password", description="This is your condor password , which is the same is your Wisc password, if you don't have one, you can't really use this.", ui=MarkerConstants.UI_PASSWORD, defaultText="Quit peaking")
	String password;

	@ParameterMarker(uiOrder=3, name="Condor Submit Node", description="Which submit node are you using for HTCondor?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="submit-5.chtc.wisc.edu")
	String host;

	@ParameterMarker(uiOrder=4, name="Submit Node Folder", description="Folder on submit node to run this job from", ui=MarkerConstants.UI_TEXTFIELD, defaultText="CHTCRun")
	String submitFolder;

	@ParameterMarker(uiOrder=5, name="# of CPUs", description="# of CPUs to request", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int cpus;

	@ParameterMarker(uiOrder=6, name="RAM [GB]", description="Amount of RAM to request", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2")
	int memory;

	@ParameterMarker(uiOrder=7, name="Disk Space [MB]", description="Amount of disk space to request", ui=MarkerConstants.UI_TEXTFIELD, defaultText="4000")
	int disk;

	/////////// Define Outputs ///////////
	//No outputs here

	@Override
	public int getMaxThreads()
	{
		return 1; // R doesn't like multiple threads
	}

	private static File localDatasetDir = null;
	private static File submitFile = null;
	private static File shFile = null;

	private File getDirPath(JEXEntry optionalEntry)
	{
		if(localDatasetDir == null)
		{
			localDatasetDir = new File(JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath(null) + File.separator + StringUtility.removeAllWhitespace(optionalEntry.getEntryExperiment()));
		}
		return localDatasetDir;
	}

	private static String datasetName()
	{
		return localDatasetDir.getName();
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		if(!isInputValid(rScript, JEXData.FILE))
		{
			JEXDialog.messageDialog("A proper file object for the R script was not found but is required. Aborting for this entry: " + optionalEntry.getEntryExperiment() + " - " + optionalEntry.getTrayX() + "," + optionalEntry.getTrayY());
			return false;
		}
		try {

			File dir = this.getDirPath(optionalEntry);

			if(firstTimeCalled)
			{
				// Prepare the folder structure
				this.prepareFolderStructure(dir);
				firstTimeCalled = false;
			}

			// Generate the .sh file to be executed for each entry
			shFile = this.genScript();

			// Now add the items specific to this entry into our prepared folder structure
			CSVList filesToR = new CSVList();
			CSVList objectsToR = new CSVList();
			filesToR.add(readObjectToFilePathTable(rScript).firstEntry().getValue());
			objectsToR.add(StringUtility.removeAllWhitespace(rScript.getTypeName().getName()));
			if(isInputValid(file1, JEXData.ANY))
			{
				filesToR.add(readObjectToFilePathTable(file1).firstEntry().getValue());
				objectsToR.add(StringUtility.removeAllWhitespace(file1.getTypeName().getName()));
			}
			if(isInputValid(file2, JEXData.ANY))
			{
				filesToR.add(readObjectToFilePathTable(file2).firstEntry().getValue());
				objectsToR.add(StringUtility.removeAllWhitespace(file2.getTypeName().getName()));
			}
			if(isInputValid(file3, JEXData.ANY))
			{
				filesToR.add(readObjectToFilePathTable(file3).firstEntry().getValue());
				objectsToR.add(StringUtility.removeAllWhitespace(file3.getTypeName().getName()));
			}

			// Generate the .sub submit file to be submitted to condor.
			submitFile = this.genSubmit(filesToR, objectsToR);

			// Copy each file to the temp folder as ..<DB>/temp/<Dataset>/<ID>/<File>
			for(int i = 0; i < filesToR.size(); i++)
			{
				File fileToProcess = new File(filesToR.get(i));
				File dstFile = new File(dir.getAbsolutePath() + File.separator + optionalEntry.getEntryID() + File.separator + StringUtility.removeAllWhitespace(objectsToR.get(i) + "." + FileUtility.getFileNameExtension(filesToR.get(i))));
				dstFile.getParentFile().mkdirs();
				FileUtils.copyFile(fileToProcess, dstFile);
			}

			// transferFolder(new File(dir.getAbsolutePath() + File.separator + optionalEntry.getEntryID()), submitFolder + "/" + StringUtility.removeAllWhitespace(optionalEntry.getEntryExperiment()) + File.separator + optionalEntry.getEntryID());
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

	public void runSSHCommands(String... commands)
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

	public File genSubmit(CSVList filesToR, CSVList objectsToR)
	{

		CSVList inputs = new CSVList();
		for(int i = 0; i < objectsToR.size(); i++)
		{
			String objectToR = objectsToR.get(i);
			String fileToR = filesToR.get(i);
			inputs.add(FileUtility.getFileNameWithExtension(objectToR) + "." + FileUtility.getFileNameExtension(fileToR));
		}

		LSVList submitCode = new LSVList();
		submitCode.add("universe = vanilla\nlog = jex_$(Cluster).log");
		submitCode.add("error = jex_$(Cluster).err");
		submitCode.add("executable = script_" + datasetName() + ".sh");
		submitCode.add("arguments = jex_$(Process)");
		submitCode.add("output = jex_$(Cluster).out");
		submitCode.add("should_transfer_files = YES");
		submitCode.add("when_to_transfer_output = ON_EXIT");
		submitCode.add("transfer_input_files = " + inputs.toString());
		submitCode.add("request_cpus = " + cpus ); // 1
		submitCode.add("request_memory = " + memory + "GB"); // 4GB
		submitCode.add("request_disk = " + disk + "MB"); // 4000MB
		submitCode.add("+WantFlocking = true");
		submitCode.add("+WantGlideIn = true");
		submitCode.add("initialdir=$(directory)");
		submitCode.add("queue directory matching " + datasetName() + "/*");
		submitCode.add("");

		File subFile = new File(JEXWriter.saveText(submitCode.toString(), "sub"));

		return subFile;
	}

	public File genScript()
	{

		LSVList submitCode = new LSVList();
		submitCode.add("#!/bin/bash");
		submitCode.add("wget http://proxy.chtc.wisc.edu/SQUID/tedegroot/R.tar.gz");
		submitCode.add("tar -xzvf R.tar.gz");
		submitCode.add("export PATH=$(pwd)/R/bin:$PATH");
		//		submitCode.add("unzip ParticleTracking_0.1.0.zip -d R/library");
		submitCode.add("R CMD BATCH rScript.R");
		submitCode.add("rm R.tar.gz");
		submitCode.add("");

		String shFile = JEXWriter.saveText(submitCode.toString(), "sh");

		return new File(shFile);
	}

	public void transferFile(File src, String dstFolder, String dstFileName)
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
			channelSftp.cd(dstFolder);
			channelSftp.put(new FileInputStream(src), dstFileName);

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
			String cmd1 = "cd " + this.submitFolder;
			String cmd2 = "rm -rf " + StringUtility.removeAllWhitespace(datasetName());
			String cmd3 = "rm -rf " + StringUtility.removeAllWhitespace(datasetName()) + "OUT";
			String cmd4 = "rm -rf " + StringUtility.removeAllWhitespace(datasetName()) + "Results";
			this.runSSHCommands(cmd1, cmd2, cmd3, cmd4);

			// Get the RScript
			//			File rScriptFile = FileReader.readFileObjectToFile(rScript);
			//			if(!rScriptFile.exists())
			//			{
			//				JEXDialog.messageDialog("Couldn't find the file: " + rScriptFile.getAbsolutePath(), this);
			//				return false;
			//			}

			// Get the Rlibs
			//			File rLibsFile = FileReader.readFileObjectToFile(rLibs);
			//			if(!rLibsFile.exists())
			//			{
			//				JEXDialog.messageDialog("Couldn't find the file: " + rLibsFile.getAbsolutePath(), this);
			//				return false;
			//			}

			// Get a temp folder to put stuff in
			if(dir.exists())
			{
				FileUtils.deleteDirectory(dir);
			}

			dir.mkdir();
			// File dstRScript = new File(dir.getAbsolutePath() + File.separator + "rScript.R");
			// File dstRLibs = new File(dir.getAbsolutePath() + File.separator + "shared/sl6-RLIBS.tar.gz");
			//FileUtils.copyFile(rScriptFile, dstRScript);
			// FileUtils.copyFile(rLibsFile, dstRLibs);

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
		if(data.getTypeName().getType().matches(JEXData.ROI))
		{
			String dataFolder = data.getDetachedRelativePath();
			dataFolder = JEXWriter.getDatabaseFolder() + File.separator + dataFolder;
			result.put(new DimensionMap("File=1"), dataFolder);
		}
		else
		{
			JEXDataSingle ds = data.getFirstSingle();
			String dataFolder = FileReader.readToFile(ds).getParent(); 			
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
		// File dir = this.getDirPath(ticket.getOutputList().firstEntry().getKey());
		// String cmd1 = "cd "+ JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getTempFolderName();
		// String cmd2 = "zip -r " + "zipfile.zip" +" " + R.sQuote(StringUtility.removeAllWhitespace(ticket.getOutputList().firstEntry().getKey().getEntryExperiment()));
		String cmd2 = "tar -zcvf " + "tarfile.tar.gz" +" " + R.sQuote(localDatasetDir.getName());
		ScriptRepository.runSysCommandApache(new String[]{"sh", "-c", cmd2}, localDatasetDir.getParent()); // Compress the files

		this.runSSHCommands("mkdir -p " + submitFolder); // Make sure the directory exists
		transferFile(new File(localDatasetDir.getParent() + File.separator + "tarfile.tar.gz"), submitFolder, "tarfile.tar.gz");
		//this.runCommands("cd ChtcRun", "unzip zipfile.zip", "rm zipfile.zip","./mkdag --data="+StringUtility.removeAllWhitespace(ticket.getOutputList().firstEntry().getKey().getEntryExperiment())+" --outputdir="+StringUtility.removeAllWhitespace(ticket.getOutputList().firstEntry().getKey().getEntryExperiment())+"OUT --resultdir=" +StringUtility.removeAllWhitespace(ticket.getOutputList().firstEntry().getKey().getEntryExperiment()) + "Results --cmdtorun=rScript.R --pattern="+outFile+" --type=R --version=R-3.2.0", "cd "+StringUtility.removeAllWhitespace(ticket.getOutputList().firstEntry().getKey().getEntryExperiment())+"OUT", "condor_submit_dag mydag.dag" );
		this.runSSHCommands("cd " + this.submitFolder, "tar -zxvf tarfile.tar.gz", "rm tarfile.tar.gz");
		transferFile(submitFile, submitFolder, "submit_" + datasetName() + ".sub");
		transferFile(shFile, submitFolder,  "script_" + datasetName() + ".sh");
		this.runSSHCommands("cd " + this.submitFolder, "dos2unix script_" + datasetName() + ".sh", "chmod +x script_" + datasetName() + ".sh", "condor_submit submit_" + datasetName() +".sub");
		// this.runCommands("cd " + submitFolder + "/" + StringUtility.removeAllWhitespace(ticket.getOutputList().firstEntry().getKey().getEntryExperiment()) +"/shared", "chmod 664 sl6-RLIBS.tar.gz");
		// this.runCommands("cd ChtcRun", "./mkdag --data="+StringUtility.removeAllWhitespace(ticket.getOutputList().firstEntry().getKey().getEntryExperiment())+" --outputdir="+StringUtility.removeAllWhitespace(ticket.getOutputList().firstEntry().getKey().getEntryExperiment())+"OUT --resultdir=" +StringUtility.removeAllWhitespace(ticket.getOutputList().firstEntry().getKey().getEntryExperiment()) + "Results --cmdtorun=rScript.R --pattern="+outFile+" --type=R --version=R-3.2.0", "cd "+StringUtility.removeAllWhitespace(ticket.getOutputList().firstEntry().getKey().getEntryExperiment())+"OUT", "condor_submit_dag mydag.dag" );

		firstTimeCalled = true;
		localDatasetDir = null;
		submitFile = null;
		shFile = null;
	}


}
