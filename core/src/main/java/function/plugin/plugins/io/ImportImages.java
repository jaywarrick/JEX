package function.plugin.plugins.io;

import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import tables.DimTable;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.ImageWriter;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;

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
		name="Import images",
		menuPath="Import",
		visible=false,
		description="Internal plugin used to thread and queue import of image files."
		)
public class ImportImages extends JEXPlugin {

	public ImportImages()
	{}
	
	/////////// Define Inputs ///////////
	
	/////////// Define Parameters ///////////
	
	public TreeMap<DimensionMap,String> imageMap = null;
	
	public void setImagesToCopy(TreeMap<DimensionMap,String> imagesToCopy)
	{
		this.imageMap = imagesToCopy;
	}
	
	public DimTable table = null;
	
	public void setOptionalDimTable(DimTable table)
	{
		this.table = table;
	}
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(name="Copied Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData output;
	
	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{		
		if(imageMap != null && imageMap.size() > 0)
		{
			this.output = ImageWriter.makeImageStackFromPaths("temp", imageMap);
			if(this.table != null)
			{
				this.output.setDimTable(this.table);
			}
			return true;
		}
		else
		{
			return false;
		}
	}
}
