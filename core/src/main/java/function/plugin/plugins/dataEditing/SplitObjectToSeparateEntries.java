package function.plugin.plugins.dataEditing;

import java.util.Map.Entry;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataReader.ImageReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import cruncher.Ticket;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import function.plugin.plugins.imageTools.ImageStackStitcher;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import miscellaneous.FileUtility;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 * @author jaywarrick
 * 
 */
@Plugin(
		type = JEXPlugin.class,
		name="Split Object To Separate Entries",
		menuPath="Data Editing",
		visible=true,
		description="Split an object on one of its dimensions and distribute the separate objects across entries. (Good for splitting a multi-point image set of separate wells)"
		)
public class SplitObjectToSeparateEntries extends JEXPlugin {

	public SplitObjectToSeparateEntries()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Image or File Object", type=MarkerConstants.TYPE_ANY, description="An object to split on a dimension.", optional=false)
	JEXData myData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Dimension to Split", description="Name of the dimension to split.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Loc")
	String dimToSplit;

	@ParameterMarker(uiOrder=2, name="Perform Virtual Split?", description="Virtual splitting just moves files while non-virtual makes a copy. Should a virtual split be performed?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean virtual;

	@ParameterMarker(uiOrder=3, name="Starting Point", description="In what corner is the first image of each image group to be stitched.", ui=MarkerConstants.UI_DROPDOWN, choices={"UL", "UR", "LL", "LR"}, defaultChoice=0)
	String startPt;

	@ParameterMarker(uiOrder=4, name="Distribute Horizontally First?", description="Distribute split objects into the entry array horizontally first?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean horizontal;

	@ParameterMarker(uiOrder=5, name="Distribute in Snaking Pattern?", description="Distribute split objects into the entry array using a snaking pattern?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean snaking;

	@ParameterMarker(uiOrder=6, name="Delete Split Dimension?", description="After splitting the object, should the resultant singleton dimension be deleted from the object?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean deleteSingleton;

	int rows = 0;
	int cols = 0;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Split Object", type=MarkerConstants.TYPE_FILE, flavor="", description="The resultant compiled table", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	public static JEXData theData = null;
	public static JEXEntry theEntry = null;

	@Override
	public boolean run(JEXEntry optionalEntry)
	{		
		if(myData != null && myData.getDimTable().getDimWithName(this.dimToSplit) == null)
		{
			JEXDialog.messageDialog("The dimension " + this.dimToSplit + " could not be found in the data provided as input. Aborting.", this);
			return false;
		}
		// Validate the input data
		if(myData != null && !(myData.getTypeName().getType().matches(JEXData.FILE) || myData.getTypeName().getType().matches(JEXData.IMAGE)))	
		{
			JEXDialog.messageDialog("Support for splitting File and Image objects is only available at this time. Aborting.", this);
			return false;
		}
		if(myData == null)
		{
			return false;
		}
		else
		{
			theData = myData;
			theEntry = optionalEntry;
		}

		// Return status
		return true;
	}

	public Integer[] getSelectionRowsAndCols(Ticket t)
	{
		TreeMap<JEXEntry,Set<JEXData>> outputs = t.getOutputList();
		int maxRow = 0;
		int maxCol = 0;
		int minRow = Integer.MAX_VALUE;
		int minCol = Integer.MAX_VALUE;

		if(outputs.size()==0)
		{
			return null;
		}

		for(Entry<JEXEntry, Set<JEXData>> e : outputs.entrySet())
		{
			maxRow = Math.max(e.getKey().getTrayY(), maxRow);
			maxCol = Math.max(e.getKey().getTrayX(), maxCol);
			minRow = Math.min(e.getKey().getTrayY(), minRow);
			minCol = Math.min(e.getKey().getTrayX(), minCol);
		}

		Integer[] ret = new Integer[] {minRow, minCol, maxRow, maxCol};
		return(ret);
	}

	public void finalizeTicket(Ticket ticket)
	{
		double count = 0;
		double total = ticket.getOutputList().size();
		double percentage = 0;

		// Gather all the files to combine and split them up by the split dimension.
		TreeMap<DimensionMap,String> files = null;
		if(theData.getTypeName().getType().matches(JEXData.FILE))
		{
			files = FileReader.readObjectToFilePathTable(theData);
		}
		else
		{
			files = ImageReader.readObjectToImagePathTable(theData);
		}
		if(files == null)
		{
			JEXDialog.messageDialog("Couldn't read the files from the provided object. Make sure it is an Image or a File object.", this);
		}

		// Get the number of rows
		Integer[] selection = getSelectionRowsAndCols(ticket);
		if(selection == null)
		{
			JEXDialog.messageDialog("No entries were selected. Aborting.", this);
			return;
		}
		rows = (selection[2] - selection[0]) + 1;
		cols = (selection[3] - selection[1]) + 1;

		// Get the row column index look up table.
		TreeMap<DimensionMap,Integer> lut = getLookUpTable(startPt, rows, cols, horizontal, snaking);
		TreeMap<Integer,JEXEntry> ilut = new TreeMap<>();
		// Only use lut entries that are associated with selected entries and order them in the order they should be filled using a TreeMap
		TreeMap<JEXEntry,Set<JEXData>> outputs = ticket.getOutputList();
		for(Entry<JEXEntry, Set<JEXData>> e : outputs.entrySet())
		{
			DimensionMap key = new DimensionMap("R=" + (e.getKey().getTrayY() - selection[0]) + ",C=" + (e.getKey().getTrayX() - selection[1]));
			Integer i = lut.get(key);
			ilut.put(i, e.getKey());
		}

		// Save the appropriate data into each entry
		DimTable dt = theData.getDimTable();
		Dim dim = dt.getDimWithName(dimToSplit);
		int i = 0;
		for (Entry<Integer,JEXEntry> e : ilut.entrySet())
		{
			DimensionMap filter = new DimensionMap(dimToSplit + "=" + dim.dimValues.get(i));
			TreeMap<DimensionMap,String> subsetOfData = new TreeMap<>();
			for(DimensionMap mapToGet : dt.getMapIterator(filter))
			{
				DimensionMap toSave = mapToGet.copy();
				if(this.deleteSingleton)
				{
					toSave.remove(this.dimToSplit);
				}
				subsetOfData.put(toSave, files.get(mapToGet));
			}
			
			// Make the data object to save
			JEXData temp = null;
			if(virtual)
			{
				// Move the data to a new JEXData
				temp = new JEXData(theData);
				temp.clearData();
				for(Entry<DimensionMap, String> eFile : subsetOfData.entrySet())
				{
					JEXDataSingle ds = this.createJEXDataSingle(eFile.getValue());
					temp.addData(eFile.getKey(), ds);
					if(deleteSingleton)
					{
						temp.setDimTable(theData.getDimTable().getSubTable(this.dimToSplit));
					}
					else
					{
						temp.setDimTable(theData.getDimTable().getSubTable(filter));
					}
				}
			}
			else if(theData.getTypeName().getType().matches(JEXData.IMAGE))
			{
				temp = ImageWriter.makeImageStackFromPaths(ticket.getOutputNames()[0].getName(), subsetOfData);
			}
			else
			{
				temp = FileWriter.makeFileObject(ticket.getOutputNames()[0].getName(), null, subsetOfData);
			}

			// Add the data to the set.
			Set<JEXData> data = ticket.getOutputList().get(e.getValue());
			data.clear();
			data.add(temp);

			i = i + 1;

			// Update the status bar
			count = count + 1;
			percentage = 100 * count / total;
			JEXStatics.statusBar.setProgressPercentage((int) percentage);
		}
		
//		// Remove the old JEXData object
//		if(virtual && !theData.hasVirtualFlavor())
//		{
//			JEXStatics.jexDBManager.removeDataFromEntry(theEntry, theData);
//		}

		// Remember to reset the static variable again so this data isn't carried over to other function runs
		theData = null;
		theEntry = null;
	}

	public JEXDataSingle createJEXDataSingle(String path)
	{
		JEXDataSingle ds = new JEXDataSingle();
		

		// I the incoming object was a virtual object, then keep the file in its current location
		if(theData.hasVirtualFlavor())
		{
			// Make a new JEXDataSingle using the relative path from the database folder to the file.
			ds.put(JEXDataSingle.RELATIVEPATH, path);
		}
		else
		{
			// Get a new file name in the temp folder
			String extension = FileUtility.getFileNameExtension(path);
			String relativePath = JEXWriter.getUniqueRelativeTempPath(extension);
			File tempFile = new File(JEXWriter.getDatabaseFolder() + File.separator + relativePath);
			
			// MOVE the file to the database temp folder
			try
			{
				FileUtils.moveFile(FileUtils.getFile(path), FileUtils.getFile(tempFile));
			}
			catch(IOException e)
			{
				e.printStackTrace();
				return null;
			}
			// Make a new JEXDataSingle using the relative path from the database folder to the file.
			ds.put(JEXDataSingle.RELATIVEPATH, relativePath);
		}
		
		// Return the datasingle
		return ds;
	}

	public static TreeMap<DimensionMap,Integer> getLookUpTable(String startPt, int rows, int cols, boolean horizontal, boolean snaking)
	{
		TreeMap<DimensionMap,Integer> ret = new TreeMap<>();
		Vector<Integer> myRows = ImageStackStitcher.getIndices(rows, false);
		Vector<Integer> myRowsRev = ImageStackStitcher.getIndices(rows, true);
		Vector<Integer> myCols = ImageStackStitcher.getIndices(cols, false);
		Vector<Integer> myColsRev = ImageStackStitcher.getIndices(cols, true);

		Vector<Integer> theRows = myRows;
		Vector<Integer> theCols = myCols;

		if(startPt.equals("LL") || startPt.equals("LR"))
		{
			theRows = myRowsRev;
		}
		if(startPt.equals("UR") || startPt.equals("LR"))
		{
			theCols = myColsRev;
		}

		int i = 0;
		if(!snaking && horizontal)
		{
			for (int r : theRows)
			{
				for (int c : theCols)
				{
					ret.put(new DimensionMap("R=" + r + ",C=" + c), i);
					i = i + 1;
				}
			}
		}
		else if(!snaking && !horizontal)
		{
			for (int c : theCols)
			{
				for (int r : theRows)
				{
					ret.put(new DimensionMap("R=" + r + ",C=" + c), i);
					i = i + 1;
				}
			}
		}
		else if(snaking && horizontal)
		{
			for (int r : theRows)
			{
				for (int c : theCols)
				{
					ret.put(new DimensionMap("R=" + r + ",C=" + c), i);
					i = i + 1;
				}
				if(theCols == myCols)
				{
					theCols = myColsRev;
				}
				else
				{
					theCols = myCols;
				}
			}
		}
		else if(snaking && !horizontal)
		{
			for (int c : theCols)
			{
				for (int r : theRows)
				{
					ret.put(new DimensionMap("R=" + r + ",C=" + c), i);
					i = i + 1;
				}
				if(theRows == myRows)
				{
					theRows = myRowsRev;
				}
				else
				{
					theRows = myRows;
				}
			}
		}
		return ret;
	}
}