package Database.SingleUserDatabase;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DataReader.FileReader;
import Database.Definition.Type;

import java.io.File;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FileUtility;

import org.xadisk.bridge.proxies.interfaces.Session;

import tables.DimensionMap;

public class JEXDataUpdate implements Update {
	
	public JEXData data;
	public JEXData updatedData;
	public String tempJXDFilePath;
	public boolean hasReferencedFilesToCopy = false;
	
	public JEXDataUpdate(JEXData data)
	{
		this.data = data;
		this.updatedData = new JEXData(data);
	}
	
	public void startUpdate() throws Exception
	{
		// THIS ONLY GETS CALLED IF THIS DATA OBJECT WAS LOADED (NEW OBJECTS AND
		// VIEWED OBJECTS) (updateData IS CALLED FROM DatabaseObjectToXData
		// which is called upon call to saveDB)
		// OR IF THE ENTRY THIS BELONGS TO NOW BELONGS TO AN EXPERIMENT OR TRAY
		// WITH A DIFFERENT NAME
		// IN WHICH CASE WE NEED TO LOAD THE JEXDATA dataMap
		if(!data.isLoaded())// THIS IS JUST IN CASE
		{
			data.getDataMap();
		}
		
		// /////// Force move of any referenced files to temp folder if needed
		// ////////////
		// We do this because it avoids overwriting files in the midst of
		// multiple object name changes
		// Object name changes result in moving files to a different folder.
		// Certain scenarios of renaming
		// would cause loss of data if we wrote directly to the final
		// destination. You need an intermediate
		// destination for all changes to ensure that the destination can be
		// overwritten.
		Type type = data.getTypeName().getType();
		Session session = JEXStatics.fileManager.session();
		String dbFolder = JEXWriter.getDatabaseFolder();
		String dataFolderRelativePath = JEXWriter.getDataFolder(data, true);
		if(type.equals(JEXData.FILE) || type.equals(JEXData.IMAGE) || type.equals(JEXData.SOUND) || type.equals(JEXData.MOVIE) || type.equals(JEXData.TRACK) || type.equals(JEXData.WORKFLOW))
		{
			// The data has referenced files so those need to be moved along
			// with the jxd
			// Get the datamap of JEXDataSingles for moving the data
			TreeMap<DimensionMap,JEXDataSingle> datamap = updatedData.getDataMap();
			for (DimensionMap map : datamap.keySet())
			{
				// For each JEXDataSingle, move the referenced file to the temp
				// folder if needed (i.e. if JEXData DataFolder changes)
				JEXDataSingle ds = datamap.get(map);
				String dstRelativePath = dataFolderRelativePath + File.separator + JEXDataIO.createReferencedFileName(ds);
				String dst = dbFolder + File.separator + dstRelativePath;
				File src = new File(FileReader.readToPath(ds));
				this.hasReferencedFilesToCopy = session.fileExists(src) && !src.getPath().equals(dst); // Then
				// we
				// for
				// sure
				// have
				// something
				// to
				// copy
				// over
				if(this.hasReferencedFilesToCopy && !src.getParent().equals(dbFolder + File.separator + JEXWriter.getTempFolderName())) // Don't
				// uselessy
				// recopy
				// to
				// the
				// temp
				// folder
				{
					String tempRelativePath = JEXWriter.getUniqueRelativeTempPath(FileUtility.getFileNameExtension(src.getPath()));
					String temp = dbFolder + File.separator + tempRelativePath;
					File tempFile = new File(temp);
					if(session.fileExists(tempFile))
					{
						session.deleteFile(tempFile);
					}
					Logs.log("Moving file from: " + src.getPath() + " to: " + temp, 0, this);
					session.moveFile(src, tempFile);
					
					// Update the datasingle with current file location in
					// updated JEXData to allow finishing of update
					ds.put(JEXDataSingle.RELATIVEPATH, tempRelativePath);
				}
				else
				// If it is already in the tempfolder just leave the path of the
				// datasingle in the updated data alone and don't move any files
				{
					// Don't do anything
				}
			}
		}
	}
	
	public void finishUpdate() throws Exception
	{
		// Force copy of referenced files from temp to dst
		String dbFolder = JEXWriter.getDatabaseFolder();
		String dataFolderRelativePath = JEXWriter.getDataFolder(data, true);
		Session session = JEXStatics.fileManager.session();
		if(this.hasReferencedFilesToCopy)
		{
			// Let's move them from the current location in the temp folder to
			// their final destination.
			// Use the updatedData JEXData here so we can update the datasingles
			// with final destinations.
			TreeMap<DimensionMap,JEXDataSingle> datamap = updatedData.getDataMap();
			for (DimensionMap map : datamap.keySet())
			{
				// For each JEXDataSingle, move the referenced file to the temp
				// folder if needed (i.e. if JEXData DataFolder changes)
				JEXDataSingle ds = datamap.get(map);
				String dstRelativePath = dataFolderRelativePath + File.separator + JEXDataIO.createReferencedFileName(ds);
				String dst = dbFolder + File.separator + dstRelativePath;
				String tempFolder = dbFolder + File.separator + JEXWriter.getTempFolderName();
				File src = new File(FileReader.readToPath(ds));
				boolean needToCopyReferencedFiles = src.getParent().equals(tempFolder); // At
				// this
				// point
				// everything
				// that
				// needs
				// copying
				// over
				// should
				// be
				// in
				// the
				// temp
				// folder
				if(needToCopyReferencedFiles)
				{
					File dstFile = new File(dst);
					if(session.fileExists(dstFile))
					{
						session.deleteFile(dstFile);
					}
					Logs.log("Moving file from: " + src.getPath() + " to: " + dst, 0, this);
					session.moveFile(src, dstFile);
					
					// Update the datasingle with current file location in
					// updated JEXData to allow finishing of update
					ds.put(JEXDataSingle.RELATIVEPATH, dstRelativePath);
				}
			}
		}
		
		// Referenced files are now in their correct final destinations
		// Now we can create the jxd file and move it to the final destination
		String tempJxdFilePath = JEXDataIO.createJXD(updatedData, JEXDataIO.DETACHED_FILEEXTENSION, false);
		File tempJxdFile = new File(tempJxdFilePath);
		if(!session.fileExists(tempJxdFile))
		{
			throw new Exception("Couldn't create jxd file.");
		}
		String jxdFileName = JEXDataIO.createJXDFileName(data);
		File dataFolder = new File(dbFolder + File.separator + dataFolderRelativePath);
		if(!session.fileExists(dataFolder))
		{
			session.createFile(dataFolder, true);
		}
		File jxdFile = new File(dbFolder + File.separator + dataFolderRelativePath + File.separator + jxdFileName);
		if(session.fileExists(jxdFile))
		{
			session.deleteFile(jxdFile);
		}
		session.moveFile(tempJxdFile, jxdFile);
		
		// Put the new jxd file path into the updated JEXData
		updatedData.setDetachedRelativePath(dataFolderRelativePath + File.separator + jxdFileName);
		
		// The updated Data should now have all the right information and is
		// ready to be added to the database in place of the old JEXData
	}
	
	public void finalizeUpdate()
	{
		this.data.setDetachedRelativePath(JEXWriter.getDataFolder(data, true) + File.separator + JEXDataIO.createJXDFileName(data));
		this.data.unloadObject();
		this.data = null;
		this.updatedData.unloadObject();
		this.updatedData = null;
	}
	
}
