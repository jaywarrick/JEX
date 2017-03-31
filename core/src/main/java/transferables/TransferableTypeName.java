//
//  TransferableButtonObject.java
//  MicroFluidicHT_Tools
//
//  Created by erwin berthier on 8/26/08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//
package transferables;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.TreeSet;

import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FileUtility;
import miscellaneous.LSVList;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;


public class TransferableTypeName implements Transferable {
	
	public static DataFlavor jexDataFlavor = new DataFlavor(TypeName.class, "A TypeName object");
	
	protected static DataFlavor[] supportedFlavors = { jexDataFlavor, DataFlavor.stringFlavor };
	
	TypeName tn;
	
	public TransferableTypeName(TypeName tn)
	{
		this.tn = tn;
	}
	
	public DataFlavor[] getTransferDataFlavors()
	{
		return supportedFlavors;
	}
	
	public boolean isDataFlavorSupported(DataFlavor flavor)
	{
		if(flavor.equals(jexDataFlavor) || flavor.equals(DataFlavor.stringFlavor))
			return true;
		return false;
	}
	
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
	{
		if(flavor.equals(jexDataFlavor))
		{
			Logs.log("Transfering typename", 1, this);
			return tn;
		}
		else if(flavor.equals(DataFlavor.stringFlavor))
		{
			// This is where we should get the directories of the selected object in the selected entries.
			TreeSet<JEXEntry> entries = JEXStatics.jexManager.getSelectedEntries();
			String CompleteObjectDirList = ""; // a lsv list of each file within a JEXData object
			for(JEXEntry e : entries)
			{
				// Get the object with matching type name
				JEXData d = e.getData(tn);
				
				if(d == null)
				{
					continue;
				}
				
				// Put together the absolute path of the object's database directory
				String dirPath = FileUtility.getFileParent(JEXWriter.getDatabaseFolder() + File.separator + d.getDetachedRelativePath());
				
				// Compile a single string with dir1 + File.pathSeparator + dir2 + File.separator ... that includes every file in the directory (including subfolders)
				File[] files = new File(dirPath).listFiles();
				LSVList pathList = (LSVList) FileUtility.getAllAbsoluteFilePaths(files, new LSVList());
				
				CompleteObjectDirList += pathList.toString() + "\n";
			}
			Logs.log("Transfering string value " + tn.toString(), 1, this);
			String result = CompleteObjectDirList; // Change what we return...
			return result;
		}
		else
		{
			throw new UnsupportedFlavorException(flavor);
		}
	}
}