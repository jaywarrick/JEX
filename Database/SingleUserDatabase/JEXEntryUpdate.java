package Database.SingleUserDatabase;

import java.io.File;

import Database.DBObjects.JEXEntry;

public class JEXEntryUpdate implements Update {
	
	public JEXEntry entry;
	public File temp1, temp2;
	
	public JEXEntryUpdate(JEXEntry entry)
	{
		this.entry = entry;
	}
	
	public void startUpdate() throws Exception
	{
		// Here we are in charge of moving the attached folders for the
		// experiments folders and Array folders
		// This is only called if an experiment or array name actually changed.
		
		// NO LONGER SUPPORTING ATTACHED INFO
		// // First get the src folders
		// String src1 = JEXWriter.getAttachedFolderPath(entry,
		// JEXEntry.EXPERIMENT, true, false);
		// String src2 = JEXWriter.getAttachedFolderPath(entry, JEXEntry.TRAY,
		// true, false);
		//
		// // Secure the temp folder names
		// String tempPath1 = JEXWriter.getDatabaseFolder() + File.separator +
		// JEXWriter.getTempFolderName() + File.separator +
		// FileUtility.getFileNameWithoutExtension(JEXWriter.getUniqueRelativeTempPath("duh"));
		// String tempPath2 = JEXWriter.getDatabaseFolder() + File.separator +
		// JEXWriter.getTempFolderName() + File.separator +
		// FileUtility.getFileNameWithoutExtension(JEXWriter.getUniqueRelativeTempPath("duh"));
		//
		// // Create and remember the temp Files
		// this.temp1 = new File(tempPath1);
		// this.temp2 = new File(tempPath2);
		//
		// // Delete any directories in the temp folder with the same name if
		// they exist
		// JEXStatics.fileManager.deleteDirectory(this.temp1);
		// JEXStatics.fileManager.deleteDirectory(this.temp2);
		//
		// // Move the directories to the temp folder
		// JEXStatics.fileManager.session().moveFile(new File(src1),
		// this.temp1);
		// JEXStatics.fileManager.session().moveFile(new File(src2),
		// this.temp2);
		
	}
	
	public void finishUpdate() throws Exception
	{
		// // Delete any directories in the temp folder with the same name if
		// they exist
		// JEXStatics.fileManager.deleteDirectory(this.temp1);
		// JEXStatics.fileManager.deleteDirectory(this.temp2);
		//
		// // Move the directories to the temp folder
		// JEXStatics.fileManager.session().moveFile(new File(src1),
		// this.temp1);
		// JEXStatics.fileManager.session().moveFile(new File(src2),
		// this.temp2);
		//
		// // Get the dst folders
		// this.dst1 = JEXWriter.getAttachedFolderPath(entry,
		// JEXEntry.EXPERIMENT, false, false);
		// this.dst2 = JEXWriter.getAttachedFolderPath(entry, JEXEntry.TRAY,
		// false, false);
		
	}
	
	public void finalizeUpdate()
	{
		this.entry.loadTimeExperimentName = this.entry.getEntryExperiment();
		this.entry.loadTimeTrayName = this.entry.getEntryTrayName();
	}
	
}
