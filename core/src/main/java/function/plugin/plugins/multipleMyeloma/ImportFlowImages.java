package function.plugin.plugins.multipleMyeloma;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.ValueWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import ij.ImagePlus;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import miscellaneous.FileUtility;
import tables.DimensionMap;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 */

@Plugin(
		type = JEXPlugin.class,
		name="Import Flow Images",
		menuPath="Multiple Myeloma",
		visible=true,
		description="Import flow images in format <prefix><Cell#>_Ch<ChannelNumber>.ome.tif"
		)
public class ImportFlowImages extends JEXPlugin {

	public ImportFlowImages()
	{}

	/////////// Define Inputs ///////////

	//	@InputMarker(uiOrder=1, name="Data Table (compiled)", type=MarkerConstants.TYPE_FILE, description="Table of (R)ed and (G)reen fluorescence data in arff format.", optional=false)
	//	JEXData fileData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Folder of Images", description="Select the folder that contains the .ome.tif images you would like to import.", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	String folderString;
	
	@ParameterMarker(uiOrder=1, name="File Prefix", description="Select the folder that contains the .ome.tif images you would like to import.", ui=MarkerConstants.UI_FILECHOOSER, defaultText="Image_")
	String prefix;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Images", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Image flow images.", enabled=true)
	JEXData imageData;
	
	@OutputMarker(uiOrder=2, name="Folder", type=MarkerConstants.TYPE_VALUE, flavor="", description="Folder where images were imported from.", enabled=true)
	JEXData folderValueData;

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		File folder = new File(folderString);

		if(!folder.exists())
		{
			JEXDialog.messageDialog("The specified folder was not found: " + folderString, this);
			return false;
		}

		Collection<File> files = Arrays.asList(folder.listFiles());

		int count = 0, total = files.size(), percentage = 0;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}

		TreeMap<DimensionMap,String> imageMap = new TreeMap<>();
		for(File f : files)
		{
			if(this.isCanceled())
			{
				return false;
			}
			if(f.getAbsolutePath().endsWith(".ome.tif") && FileUtility.getFileNameWithExtension(f.getAbsolutePath()).startsWith(prefix))
			{
				String fileName = FileUtility.getFileNameWithoutExtension(f.getAbsolutePath());
				fileName = FileUtility.getFileNameWithoutExtension(fileName); // Remove the .ome subextension
				String[] pieces = fileName.split("_");
				int n = pieces.length;
				String cellNum = pieces[n-2];
				String chString = pieces[n-1];
				chString = chString.substring(2, chString.length());
				ImagePlus im = new ImagePlus(f.getAbsolutePath());
				String path = JEXWriter.saveImage(im);
				imageMap.put(new DimensionMap("Cell=" + cellNum + ",Channel=" + chString), path);
			}
		}
		
		this.imageData = ImageWriter.makeImageStackFromPaths("Duh", imageMap);
		this.folderValueData = ValueWriter.makeValueObject("Duh", this.folderString);

		// Return status
		return true;
	}

}
