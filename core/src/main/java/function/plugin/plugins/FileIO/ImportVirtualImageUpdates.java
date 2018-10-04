package function.plugin.plugins.FileIO;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Type;
import Database.SingleUserDatabase.JEXWriter;
import cruncher.Ticket;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.ParameterMarker;
import function.plugin.plugins.imageTools.ImageStackStitcher;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.FileUtility;
import miscellaneous.LSVList;
import miscellaneous.SimpleFileFilter;
import miscellaneous.StringUtility;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;

@Plugin(
		type = JEXPlugin.class,
		name="Import Virtual Image Updates",
		menuPath="File IO",
		visible=true,
		description="Use an existing virtual image object to research the image file directory for new files that can be" +
				"auto-imported and merged with the existing data. This will create a data 'chunk' that can be used to then" +
				"update subsequent objects in a workflow."
		)
public class ImportVirtualImageUpdates extends JEXPlugin {

	public ImportVirtualImageUpdates() {}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Image or File Object (optional)", type=MarkerConstants.TYPE_ANY, description="An object to split on a dimension.", optional=false)
	JEXData input;

	/////////// Define Parameters ///////////

	//	@ParameterMarker(uiOrder=0, name="Input Directory (if no input Object)", description="Location of the images if there is no input object provided. Must be a directory.", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	String inDir;

	//	@ParameterMarker(uiOrder=1, name="File Extension", description="The type of file that is being imported if there is no input object provided. Default is tif.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="tif")
	String fileExtension;

//	@ParameterMarker(uiOrder=1, name="Next timepoint only?", description="Get all new files (unchecked) or just the next timepoint only (checked)?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean nextOnly = true;

	@ParameterMarker(uiOrder=2, name="XY String Token(s)", description="Use a CSV list to define what set of characters in each filename denotes the file index/x-y/row-col location for the acquisition? (e.g., 'xy', or 'x,y' or 'R,C')", ui=MarkerConstants.UI_TEXTFIELD, defaultText="xy")
	String xyTokenString;

	@ParameterMarker(uiOrder=3, name="Time String Token", description="What set of characters in each filename denotes the Time index?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="t")
	String timeToken;

	@ParameterMarker(uiOrder=4, name="Old Token Names", description="List dimension names parsed from the file names that should be changed?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="t,xy,c,z")
	String oldTokens;

	@ParameterMarker(uiOrder=5, name="New Token Names", description="What should the parsed dimension names that are going to be renamed be changed to for saving in the database?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="T,XY,Channel,Z")
	String newTokens;

	/////////// Define Outputs ///////////

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	public static TreeMap<DimensionMap,String> files = null;
	public static DimTable dt = null;
	public static TreeMap<JEXEntry,JEXData> inputDatas = new TreeMap<>();

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		if(input != null && input.getDataObjectType().matches(JEXData.IMAGE) && input.hasVirtualFlavor())
		{
			JEXDataSingle ds = input.getFirstSingle();
			File firstData = FileReader.readToFile(ds, input.hasVirtualFlavor());
			this.inDir = firstData.getParent();
			this.fileExtension = FileUtility.getFileNameExtension(firstData.getAbsolutePath());
		}
		else if(input != null)
		{
			JEXDialog.messageDialog("The object provided was not a Virtual Image object. Aborting.");
		}

		// GATHER DATA FROM PARAMETERS
		// create file object for input directory
		File filePath = new File(inDir);
		if(!filePath.exists())
		{
			JEXDialog.messageDialog("Warning! The file or folder specified could not be found.");
			return false;
		}
		if(files == null)
		{
			if (filePath.isDirectory()) { // multiple files
				// gather and sort the input files
				JEXStatics.statusBar.setStatusText("Sorting files to convert. May take minutes...");
				List<File> pendingImageFiles = FileUtility.getSortedFileList(filePath.listFiles(new SimpleFileFilter(new String[] { fileExtension })));
				if(pendingImageFiles.size() == 0)
				{
					JEXDialog.messageDialog("No files were found in the specified directory. Aborting.");
					return false;
				}
				files = new TreeMap<>();
				for(File f : pendingImageFiles)
				{
					DimensionMap map = StringUtility.getMapFromPath(f.getAbsolutePath());
					files.put(map, f.getAbsolutePath());
				}
				boolean success = this.trimFilesAndDT(input, nextOnly);
				if(!success)
				{
					return false;
				}
			}
			else { // one file
				JEXDialog.messageDialog("A directory must be specified instead of a file. Aborting");
				return false;
			}
		}

		inputDatas.put(optionalEntry, input);
		
		Logs.log("Visited Entrty " + optionalEntry.getEntryID(), this);

		return true;
	}

	public void finalizeTicket(Ticket ticket)
	{
//		files = null;
		if(dt == null)
		{
			// then trimming the file list was unsuccessful.
			return;
		}
		double count = 0;
		double total = ticket.getOutputList().size();
		double percentage = 0;

		if(inputDatas.firstEntry().getValue().hasChunkFlavor())
		{
			int answer = JEXDialog.getChoice("Continue?", "A 'chunk' flavored object of the same name exists and is being used as the input. Typically you don't want this for this function. Should the function continue anyway?", new String[] {"Yes", "No"}, 1);
			if(answer == 1)
			{
				// Remember to reset the static variable again so this data isn't carried over to other function runs
				files = null;
				dt = null;
				inputDatas.clear();
				return;
			}
		}

		// First, get the xyTokens
		CSVList xyTokens = StringUtility.getCSVListAndRemoveWhiteSpaceOnEnds(this.xyTokenString);
		//		// Check that the 
		//		for(String xyToken : xyTokens)
		//		{
		//			Dim xyDim = dt.getDimWithName(xyToken);
		//			if(xyDim == null)
		//			{
		//				JEXDialog.messageDialog("The XY string token '" + xyToken + "' did not match any of the parsed dimensions from the filenames. Aborting. Dimensions = " + dt.toString());
		//				// Remember to reset the static variable again so this data isn't carried over to other function runs
		//				files = null;
		//				dt = null;
		//				inputDatas.clear();
		//				return;
		//			}
		//		}
		
		// Filter files to just those files that pertain to these entries.
		TreeMap<DimensionMap,String> filteredFiles = new TreeMap<>();
		int validEntries = 0;
		for (Entry<JEXEntry,JEXData> entry : inputDatas.entrySet())
		{
			// Check an input data exists for this entry.
			JEXData input = entry.getValue();
			if(input == null)
			{
				JEXDialog.messageDialog("No input data found for entry X=" + entry.getKey().getTrayX() + " Y=" + entry.getKey().getTrayY() + ". Skipping");
				continue;
			}
			validEntries = validEntries + 1;
			
			// Generate a DimTable for this entry and data.
			DimTable entryTable = this.getEntryTable(entry.getKey(), input, xyTokens);
			
			// Use the entryTable to only grab the appropriate files.
			for(DimensionMap map : entryTable.getMapIterator())
			{
				String path = files.get(map);
				if(path == null)
				{
					JEXDialog.messageDialog("No input files found for entry X=" + entry.getKey().getTrayX() + " Y=" + entry.getKey().getTrayY() + " " + map.toString() + ". Skipping");
					continue;
				}
				filteredFiles.put(map, path);
			}
		}
		files = filteredFiles;
		
		// Check if we have a full timepoint.
		if(files.size() != dt.mapCount()*validEntries)
		{
			JEXDialog.messageDialog("We expected " + dt.mapCount()*validEntries + " files but only found " + files.size() + ". Since the timepoint is not complete for these entries, we are aborting and stopping Auto-Updater.", this);
			return;
		}

		// Now for each entry, create the new datas
		for (Entry<JEXEntry,JEXData> entry : inputDatas.entrySet())
		{
			JEXData input = entry.getValue();
			if(input == null)
			{
				continue;
			}
			
			// Generate a DimTable for this entry and data.
			DimTable entryTable = this.getEntryTable(entry.getKey(), input, xyTokens);

			TreeMap<DimensionMap,String> entryFiles = new TreeMap<>();
			for(DimensionMap map : entryTable.getMapIterator())
			{
				entryFiles.put(map, files.get(map)); // possibility of null file path eliminated in loops above.
			}
			
			if(entryFiles.size() == 0)
			{
				JEXDialog.messageDialog("There were no new files for entry X=" + entry.getKey().getTrayX() + " Y=" + entry.getKey().getTrayY() + ".", this);
				continue;
			}
			
			// Convert the dimensionMaps to match the database maps
			TreeMap<DimensionMap,String> temp = new TreeMap<>();
			for(Entry<DimensionMap,String> e : entryFiles.entrySet())
			{
				DimensionMap toConvert = e.getKey().copy();
				for(String xyToken : xyTokens)
				{
					toConvert.remove(xyToken);
				}
				
				DimensionMap convertedMap = this.getRenamedDimensionMap(toConvert);
				temp.put(convertedMap, e.getValue());
			}
			entryFiles = temp;

			Logs.log("Found new files to merge.", this);
			for(DimensionMap map : entryFiles.keySet())
			{
				Logs.log(entryFiles.get(map), this);
			}
			// Create a chunk with the new files
			JEXData chunkData = ImageWriter.makeImageStackFromPaths(input.getDataObjectName(), entryFiles, true, ticket);
			if(chunkData == null)
			{
				Logs.log("No new data found for the object in Entry " + entry.getKey() + ". Skipping.", this);
				continue;
			}

			// Set the flavor of the chunk to virtual chunk
			chunkData.setDataObjectType(new Type(chunkData.getTypeName().getType().getType(), JEXData.FLAVOR_VIRTUAL + " " + JEXData.FLAVOR_UPDATE));

			// Merge the input data and the new virtual chunk
			// Put the 'input' first in the list so its type takes precedence.
			JEXData mergedData = JEXWriter.performVirtualMerge(new JEXData[] {input, chunkData}, entry.getKey(), input.getDataObjectName(), this);

			// Save the output
			HashSet<JEXData> datas = new HashSet<>();
			if(!mergedData.getTypeName().equals(chunkData.getTypeName()))
			{
				datas.add(mergedData);
				datas.add(chunkData);
			}
			else
			{
				datas.add(chunkData);
			}

			// Add the datas to the output for the entry
			ticket.getOutputList().put(entry.getKey(), datas);

			// Update the status bar
			count = count + 1;
			percentage = 100 * count / total;
			JEXStatics.statusBar.setProgressPercentage((int) percentage);
		}

		// Remember to reset the static variable again so this data isn't carried over to other function runs
		files = null;
		dt = null;
		inputDatas.clear();
	}
	
	public DimTable getEntryTable(JEXEntry entry, JEXData input, CSVList xyTokens)
	{
		// Get a DimTable with xyTokens appropriate to the entry to filter the files
		String templateFilePath = FileReader.readToPath(input.getFirstSingle());
		DimensionMap filterTemplate = StringUtility.getMapFromPath(templateFilePath);
		DimTable entryTable = dt.copy();
		for(String xyToken : xyTokens)
		{
			entryTable.add(new Dim(xyToken, filterTemplate.get(xyToken)));
		}
		return entryTable;
	}

	public boolean trimFilesAndDT(JEXData input, boolean nextOnly)
	{		
		CSVList xyTokens = StringUtility.getCSVListAndRemoveWhiteSpaceOnEnds(this.xyTokenString);

		TreeMap<DimensionMap,String> exampleInputFiles = ImageReader.readObjectToImagePathTable(input);
		TreeMap<DimensionMap,String> parsedDimMaps = new TreeMap<>(); 
		for(Entry<DimensionMap,String> e : exampleInputFiles.entrySet())
		{
			DimensionMap map = StringUtility.getMapFromPath(e.getValue());
			parsedDimMaps.put(map, e.getValue());
		}
		DimTable inputDT = new DimTable(parsedDimMaps);
		for(String xyToken : xyTokens)
		{
			inputDT.removeDimWithName(xyToken);
		}


		// grab the DimTable for the directory of files.
		DimTable dirDT = new DimTable(files);
		for(String xyToken : xyTokens)
		{
			dirDT.removeDimWithName(xyToken);
		}

		// Just grab new files
		Dim dirTimes = dirDT.getDimWithName(timeToken);
		Dim inputTimes = inputDT.getDimWithName(timeToken);
		Dim newTimes = Dim.subtract(dirTimes, inputTimes);
		System.out.println();
		System.out.println();
		if(newTimes.size() == 0)
		{
			JEXDialog.messageDialog("New new timepoints detected in directory. Aborting.", this);
			return false;
		}
		if(nextOnly)
		{
			newTimes = new Dim(timeToken + "=" + newTimes.valueAt(0));
		}
		int i = dirDT.indexOf(dirTimes);
		dirDT.remove(i);
		dirDT.add(i, newTimes);

		// Save the filtered dimTable
		dt = dirDT;

		// Then trim the files to match the trimmed data.table
		Vector<DimensionMap> toRemove = new Vector<>();
		for(DimensionMap map : files.keySet())
		{
			DimensionMap mapToRemove = map.copy();
			for(String xyToken : xyTokens)
			{
				mapToRemove.remove(xyToken);
			}
			if(!DimTable.hasKeyValues(dt, mapToRemove))
			{
				toRemove.add(map);
			}
		}
		for(DimensionMap map : toRemove)
		{
			files.remove(map);
		}
		return true;
	}

	public JEXDataSingle createJEXDataSingle(String path)
	{
		JEXDataSingle ds = new JEXDataSingle();

		// Make a new JEXDataSingle using the relative path from the database folder to the file.
		// This will be virtual so just store the whole path as the relative path
		ds.put(JEXDataSingle.RELATIVEPATH, path);

		// Return the datasingle
		return ds;
	}

	public DimensionMap getRenamedDimensionMap(DimensionMap oldMap)
	{
		CSVList daOld = StringUtility.getCSVStringAsCSVListWithoutWhiteSpace(this.oldTokens);
		CSVList daNew = StringUtility.getCSVStringAsCSVListWithoutWhiteSpace(this.newTokens);
		if(daOld.size() != daNew.size())
		{
			return null;
		}
		DimensionMap newMap = oldMap.copy();
		for(String key : oldMap.keySet())
		{
			if(daOld.contains(key))
			{
				int i = daOld.indexOf(key);
				newMap.remove(key);
				newMap.put(daNew.get(i), oldMap.get(key));
			}
			else
			{
				newMap.put(key, oldMap.get(key));
			}
		}
		return newMap;
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


	public Integer[] getSelectionRowsAndCols(TreeMap<JEXEntry,JEXData> inputs)
	{
		int maxRow = 0;
		int maxCol = 0;
		int minRow = Integer.MAX_VALUE;
		int minCol = Integer.MAX_VALUE;

		if(inputs.size()==0)
		{
			return null;
		}

		for(Entry<JEXEntry, JEXData> e : inputs.entrySet())
		{
			maxRow = Math.max(e.getKey().getTrayY(), maxRow);
			maxCol = Math.max(e.getKey().getTrayX(), maxCol);
			minRow = Math.min(e.getKey().getTrayY(), minRow);
			minCol = Math.min(e.getKey().getTrayX(), minCol);
		}

		Integer[] ret = new Integer[] {minRow, minCol, maxRow, maxCol};
		return(ret);
	}

	public LSVList getFileList(List<File> filesToImport)
	{
		LSVList fileList = new LSVList();
		for(File f : filesToImport)
		{
			fileList.add(f.getAbsolutePath());
		}
		return fileList;
	}
}
