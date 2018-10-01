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

	@ParameterMarker(uiOrder=2, name="XY String Token(s)", description="Use a CSV list to define what set of characters in each filename denotes the file index/x-y/row-col location for the acquisition? (e.g., 'xy', or 'x,y' or 'R,C')", ui=MarkerConstants.UI_TEXTFIELD, defaultText="xy")
	String xyTokenString;

	@ParameterMarker(uiOrder=3, name="Time String Token", description="What set of characters in each filename denotes the Time index?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="t")
	String timeToken;
	
	@ParameterMarker(uiOrder=4, name="Old Token Names", description="List dimension names parsed from the file names that should be changed?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="t,xy,c,z")
	String oldTokens;
	
	@ParameterMarker(uiOrder=5, name="New Token Names", description="What should the parsed dimension names that are going to be renamed be changed to for saving in the database?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="T,XY,Channel,Z")
	String newTokens;

	@ParameterMarker(uiOrder=6, name="Distribution Starting Point", description="In what corner of the entry array should the first image be dealt?", ui=MarkerConstants.UI_DROPDOWN, choices={"UL", "UR", "LL", "LR"}, defaultChoice=0)
	String startPt;

	@ParameterMarker(uiOrder=7, name="Distribute Horizontally First?", description="Distribute split objects into the entry array horizontally first?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean horizontal;

	@ParameterMarker(uiOrder=8, name="Distribute in Snaking Pattern?", description="Distribute split objects into the entry array using a snaking pattern?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean snaking;

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
	public boolean run(JEXEntry optionalEntry) {

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
				dt = new DimTable(files);
				this.trimFilesAndDT();
			}
			else { // one file
				JEXDialog.messageDialog("A directory must be specified instead of a file. Aborting");
				return false;
			}
		}

		inputDatas.put(optionalEntry, input);

		return true;
	}

	public void finalizeTicket(Ticket ticket)
	{
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
		for(String xyToken : xyTokens)
		{
			Dim xyDim = dt.getDimWithName(xyToken);
			if(xyDim == null)
			{
				JEXDialog.messageDialog("The XY string token '" + xyToken + "' did not match any of the parsed dimensions from the filenames. Aborting. Dimensions = " + dt.toString());
				// Remember to reset the static variable again so this data isn't carried over to other function runs
				files = null;
				dt = null;
				inputDatas.clear();
				return;
			}
		}
		
		for (Entry<JEXEntry,JEXData> entry : inputDatas.entrySet())
		{
			JEXData input = entry.getValue();
			if(input == null)
			{
				JEXDialog.messageDialog("No input data found for entry " + entry.getKey().getEntryID() + ". Skipping");
				continue;
			}
			
			// Get a dimensionMap to filter the files for saving
			String templateFilePath = FileReader.readToPath(input.getFirstSingle());
			DimensionMap filterTemplate = StringUtility.getMapFromPath(templateFilePath);
			DimensionMap filter = new DimensionMap();
			for(String xyToken : xyTokens)
			{
				filter.put(xyToken, filterTemplate.get(xyToken));
			}
			if(!dt.testUnderdefinedMap(filter))
			{
				JEXDialog.messageDialog("The parsed map of the files in the input object " + filter + " do not adequately match the parsed dimension maps of the new input files " + dt + ". Aborting", this);
				// Remember to reset the static variable again so this data isn't carried over to other function runs
				files = null;
				dt = null;
				inputDatas.clear();
				return;
			}
			
			TreeMap<DimensionMap,String> subsetOfData = new TreeMap<>();
			
			// Just grab the files in the list that match the xyTokens for this entry
			int totFilesToGet = dt.getSubTable(filter).mapCount(); // All times, but just this location
			int filesGotten = 0;
			for(DimensionMap mapToGet : dt.getMapIterator(filter))
			{
				DimensionMap toSave = mapToGet.copy();
				for(String xyToken : xyTokens)
				{
					toSave.remove(xyToken);
				}
				String pathToGet = files.get(mapToGet);
				if(pathToGet == null)
				{
					Logs.log("A file was expected for " + mapToGet.toString() + ". Skipping.", this);
					continue;
				}
				subsetOfData.put(toSave, pathToGet);
				filesGotten = filesGotten + 1;
			}
			if(filesGotten != totFilesToGet)
			{
				// The filter is based upon the input data.
				// The dt is based upon the files in the directory
				// Thus, if these numbers don't line up, we probably have an issue to contend with.
				JEXDialog.messageDialog("Warning. Not all expected files were found for entry " + entry.getKey().getEntryID() + ".", this);
			}

			// Figure out what part of the filtered file list is new compared to what is in the database already
			// and put the new files into a map
			TreeMap<DimensionMap,String> newFiles = new TreeMap<>();
			TreeMap<DimensionMap,String> alreadySavedFiles = ImageReader.readObjectToImagePathTable(input);
			for(DimensionMap map : subsetOfData.keySet())
			{
				// First get a database version of the dimension map for the incoming file
				DimensionMap renamedMap = this.getRenamedDimensionMap(map);
				if(renamedMap == null)
				{
					JEXDialog.messageDialog("The list of old and new tokens could not be used successfully to rename the parsed dimension names. Please check the old tokens exist in each file name and that the old and new tokens lists are the same size.", this);
					// Remember to reset the static variable again so this data isn't carried over to other function runs
					files = null;
					dt = null;
					inputDatas.clear();
					return;
				}
				
				// Get a version of the database map for the incoming data that is without time.
				DimensionMap testMap = map.copy();
				testMap.remove(this.timeToken);
				testMap = this.getRenamedDimensionMap(testMap);				
				
				// If the timeless incoming databased map doesn't fit the existing dimension of the already saved data, we have a problem.
				if(!input.getDimTable().testUnderdefinedMap(testMap))
				{
					// This means that the input object dimensions don't match the dimensions of the new object to be made
					JEXDialog.messageDialog("There is a dimension mismatch between the imported files with renamed dimension names, and the dimension names in existing input data. Aborting.", this);
					// Remember to reset the static variable again so this data isn't carried over to other function runs
					files = null;
					dt = null;
					inputDatas.clear();
					return;
				}
				
				// So far so good. If the incoming databased map doesn't exist in the saved data, then put it into newFiles
				// Save it according to the database map but use the parsed filename map to get it from the file subset.
				if(alreadySavedFiles.get(renamedMap) == null)
				{
					newFiles.put(renamedMap, subsetOfData.get(map));
				}
			}
			
			if(newFiles.size() == 0)
			{
				continue;
			}
			
			Logs.log("Found new files to merge.", this);
			for(DimensionMap map : newFiles.keySet())
			{
				Logs.log(newFiles.get(map), this);
			}
			// Create a chunk with the new files
			JEXData chunkData = ImageWriter.makeImageStackFromPaths(input.getDataObjectName(), newFiles, true, ticket);
			if(chunkData == null)
			{
				Logs.log("No new data found for the object in Entry " + entry.getKey() + ". Skipping.", this);
				continue;
			}
			
			// Set the flavor of the chunk to virtual chunk
			chunkData.setDataObjectType(new Type(chunkData.getTypeName().getType().getType(), JEXData.FLAVOR_VIRTUAL + " " + JEXData.FLAVOR_CHUNK));

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

	public void trimFilesAndDT()
	{
		// First trim the data table to "contain" the files.
		int mapCount = dt.mapCount();
		while(mapCount > files.size() && dt.getDimWithName(this.timeToken).size() > 0)
		{
			Dim tDim = dt.getDimWithName(this.timeToken);
			tDim.dimValues.remove(tDim.size()-1);
			tDim.updateDimValueSet();
			mapCount = dt.mapCount();
		}

		// Then trim the files to match the trimmed data.table
		Vector<DimensionMap> toRemove = new Vector<>();
		for(DimensionMap map : files.keySet())
		{
			if(!DimTable.hasKeyValues(dt, map))
			{
				toRemove.add(map);
			}
		}
		for(DimensionMap map : toRemove)
		{
			files.remove(map);
		}
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
