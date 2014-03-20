package jex.statics;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import logs.Logs;

import org.xadisk.additional.XAFileOutputStreamWrapper;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.filesystem.exceptions.DirectoryNotEmptyException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;
import org.xadisk.filesystem.exceptions.FileUnderUseException;
import org.xadisk.filesystem.exceptions.InsufficientPermissionOnFileException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

import Database.SingleUserDatabase.JEXWriter;

public class JEXDBFileManager {
	
	public static final String managerFolder = "File Manager Docs";
	public static final String managerID = "JEXDBFileManager";
	
	public StandaloneFileSystemConfiguration config = null;
	public XAFileSystem fs = null;
	protected Session session = null;
	
	public void initialize()
	{
		// Get the current database folder.
		String databaseFolder = JEXWriter.getDatabaseFolder();
		
		// Return if the current databaseFolder is null.
		if(databaseFolder == null)
		{
			return;
		}
		
		this.config = new StandaloneFileSystemConfiguration(databaseFolder + File.separator + managerFolder, managerID);
		
		this.fs = XAFileSystemProxy.bootNativeXAFileSystem(this.config);
		
		Logs.log("Booting JEXDBFileManager on database folder: " + databaseFolder, 0, this);
		
		try
		{
			this.fs.waitForBootup(-1);
			Logs.log("JEXDBFileManager is now available for use on database folder.", 0, this);
		}
		catch (Throwable t)
		{
			t.printStackTrace();
			
			// Shutdown whatever possible
			this.shutdown();
			Logs.log("JEXDBFileManager could not be initialized on " + databaseFolder, 0, this);
		}
	}
	
	public boolean startNewSession()
	{
		if(this.session != null)
		{
			try
			{
				this.fs.shutdown();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			if(this.session != null)
			{
				this.session = null;
			}
		}
		if(this.fs == null)
		{
			this.initialize();
		}
		this.session = this.fs.createSessionForLocalTransaction();
		return this.session != null;
	}
	
	public Session session()
	{
		if(this.fs == null)
		{
			return null;
		}
		return this.session;
	}
	
	public void shutdown()
	{
		if(fs != null)
		{
			try
			{
				this.fs.shutdown();
				this.fs = null;
				this.session = null;
			}
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
	}
	
	public boolean saveText(String fullPath, String toSave, boolean overwrite) throws Exception
	{
		PrintWriter writer = null;
		try
		{
			File outFile = new File(fullPath);
			File folder = outFile.getParentFile();
			if(!this.session.fileExists(folder))
			{
				this.session.createFile(folder, true);
			}
			
			if(this.session.fileExists(outFile))
			{
				if(overwrite)
				{
					this.session.deleteFile(outFile);
				}
				else
				{
					return false;
				}
			}
			this.session.createFile(outFile, false);
			XAFileOutputStream output = this.session.createXAFileOutputStream(outFile, true);
			XAFileOutputStreamWrapper outputWrapper = new XAFileOutputStreamWrapper(output);
			writer = new PrintWriter(new BufferedOutputStream(outputWrapper));
			writer.print(toSave);
			writer.flush();
			writer.close();
			return true;
		}
		catch (Exception e)
		{
			if(writer != null)
			{
				writer.close();
			}
			throw e;
		}
	}
	
	/**
	 * Deletes all files and subdirectories under dir. Returns true if all deletions were successful. If a deletion fails, the method stops attempting to delete and returns false.
	 * 
	 * @param dir
	 * @return
	 * @throws InterruptedException
	 * @throws FileNotExistsException
	 * @throws InsufficientPermissionOnFileException
	 * @throws NoTransactionAssociatedException
	 * @throws LockingFailedException
	 * @throws FileUnderUseException
	 * @throws DirectoryNotEmptyException
	 */
	public boolean deleteDirectory(File folder) throws Exception
	{
		if(this.session.fileExistsAndIsDirectory(folder))
		{
			String[] children = this.session.listFiles(folder);
			for (int i = 0; i < children.length; i++)
			{
				boolean success = this.deleteDirectory(new File(folder, children[i]));
				if(!success)
				{
					return false;
				}
			}
		}
		
		// The directory is now empty so delete it
		this.session.deleteFile(folder);
		return !this.session.fileExists(folder);
	}
	
}
