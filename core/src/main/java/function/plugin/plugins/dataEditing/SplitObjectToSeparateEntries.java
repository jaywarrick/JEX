package function.plugin.plugins.dataEditing;

import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataReader.ImageReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import cruncher.Ticket;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import function.plugin.plugins.imageTools.ImageStackStitcher;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import miscellaneous.Pair;
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

	@ParameterMarker(uiOrder=2, name="Num Rows", description="Total number of rows of the array of entries to distribute object into. (0 causes autocalculation of the number of rows based upon the selection)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	int rows;

	@ParameterMarker(uiOrder=3, name="Num Cols", description="Total number of cols of the array of entries to distribute object into. (0 causes autocalculation of the number of columns based upon the selection)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	int cols;

	@ParameterMarker(uiOrder=4, name="Starting Point", description="In what corner is the first image of each image group to be stitched.", ui=MarkerConstants.UI_DROPDOWN, choices={"UL", "UR", "LL", "LR"}, defaultChoice=0)
	String startPt;

	@ParameterMarker(uiOrder=5, name="Distribute Horizontally First?", description="Distribute split objects into the entry array horizontally first?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean horizontal;

	@ParameterMarker(uiOrder=6, name="Distribute in Snaking Pattern?", description="Distribute split objects into the entry array using a snaking pattern?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean snaking;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Split Object", type=MarkerConstants.TYPE_FILE, flavor="", description="The resultant compiled table", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	public static JEXData theData = null;

	@Override
	public boolean run(JEXEntry optionalEntry)
	{		
		// Validate the input data
		if(myData == null || !(myData.getTypeName().getType().equals(JEXData.FILE) || myData.getTypeName().getType().equals(JEXData.IMAGE)))	
		{
			return false;
		}
		else
		{
			theData = myData;
		}

		// Return status
		return true;
	}

	public Pair<Integer,Integer> getSelectionRowsAndCols(Ticket t)
	{
		TreeMap<JEXEntry,Set<JEXData>> outputs = t.getOutputList();
		int maxRow = 0;
		int maxCol = 0;

		for(Entry<JEXEntry, Set<JEXData>> e : outputs.entrySet())
		{
			maxRow = Math.max(e.getKey().getTrayY(), maxRow);
			maxCol = Math.max(e.getKey().getTrayX(), maxCol);
		}

		return(new Pair<Integer,Integer>(maxRow, maxCol));
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
		Pair<Integer,Integer> selection = getSelectionRowsAndCols(ticket);
		if(rows < 1)
		{
			rows = selection.p1 + 1; // Selection Rows and Cols are 0 indexed so add 1
		}
		if(cols < 1)
		{
			cols = selection.p2 + 1; // Selection Rows and Cols are 0 indexed so add 1
		}

		// Get the row column index look up table.
		TreeMap<DimensionMap,Integer> lut = getLookUpTable(startPt, rows, cols, horizontal, snaking);

		// Save the appropriate images into each entry
		DimTable dt = theData.getDimTable();
		Dim dim = dt.getDimWithName(dimToSplit);
		for (Entry<JEXEntry,Set<JEXData>> e : ticket.getOutputList().entrySet())
		{
			DimensionMap rc = new DimensionMap("R=" + e.getKey().getTrayY() + ",C=" + e.getKey().getTrayX());
			DimensionMap filter = new DimensionMap(dimToSplit + "=" + dim.dimValues.get(lut.get(rc)));
			TreeMap<DimensionMap,String> subsetOfData = new TreeMap<>();
			for(DimensionMap mapToGet : dt.getMapIterator(filter))
			{
				subsetOfData.put(mapToGet, files.get(mapToGet));
			}
			// Make the data object to save
			JEXData temp = null;
			if(theData.getTypeName().getType().equals(JEXData.IMAGE))
			{
				temp = ImageWriter.makeImageStackFromPaths(ticket.getOutputNames()[0].getName(), subsetOfData);
			}
			else
			{
				temp = FileWriter.makeFileObject(ticket.getOutputNames()[0].getName(), null, subsetOfData);
			}
			
			// Add the data to the set.
			Set<JEXData> data = e.getValue();
			data.clear();
			data.add(temp);
			
			// Update the status bar
			count = count + 1;
			percentage = 100 * count / total;
			JEXStatics.statusBar.setProgressPercentage((int) percentage);
		}
		
		// Remember to reset the static variable again so this data isn't carried over to other function runs
		theData = null;
	}

	public TreeMap<DimensionMap,Integer> getLookUpTable(String startPt, int rows, int cols, boolean horizontal, boolean snaking)
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