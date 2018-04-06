package function.plugin.plugins.FileIO;

import java.io.File;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FileUtility;
import miscellaneous.SimpleFileFilter;

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
		name="Find And Replace In File Names",
		menuPath="File IO",
		visible=true,
		description="Function that allows you to specify rules for changing the name of a group of files in a folder."
		)
public class FindAndReplaceInFileNames extends JEXPlugin {

	public FindAndReplaceInFileNames()
	{}

	// ----------------------------------------------------
	// --------- INFORMATION ABOUT THE FUNCTION -----------
	// ----------------------------------------------------

	/////////// Define Inputs ///////////

	@OutputMarker(uiOrder=1, name="Folder (optional)", type=MarkerConstants.TYPE_FILE, flavor="", description="Folder where files are held (not recursive).", enabled=true)
	JEXData folderData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Test Run", description="Should this just be a test run and simply output proposed changes to the console?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean test;

	@ParameterMarker(uiOrder=1, name="Input Directory/File", description="Location of the multicolor TIFF images", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	String inDir;

	@ParameterMarker(uiOrder=2, name="File extension", description="Changes only those with a matching file extension (excluding '.' character)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="tif")
	String extension;

	@ParameterMarker(uiOrder=3, name="Split String", description="What string of characters should be used to split up the filename first? (leave blank to skip). Set to blank to add prefix or suffix.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String splitString;

	@ParameterMarker(uiOrder=4, name="Split Index", description="If split, which split should be searched to perform find/replace? (0 is the index of the first split, then 1, ...)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	int splitIndex;

	@ParameterMarker(uiOrder=5, name="Skip/Keep first N characters (prefix)", description="The number of characters to skip/keep at beginning of name during find/replace. Use '*' to skip/keep all. This takes precedence over skipping all end characters. Set to 0 to add prefix. Set to * to add suffix.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="*")
	String N;

	@ParameterMarker(uiOrder=6, name="Skip/Keep last M characters (suffix)", description="The number of characters to skip/keep at end of name(or portion of the split file name) during find/replace. Use '*' to skip/keep all end characters unless '*' is specified for the skip/keep parameter for the beginnning of the String. Set to * to add prefix. Set to 0 to add suffix.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="*")
	String M;

	@ParameterMarker(uiOrder=7, name="Old String", description="What string of characters should be found? Set to * to add prefix or suffix. Use '*' to replace all characters between the specified beginning and ending characters.", ui=MarkerConstants.UI_FILECHOOSER, defaultText="_")
	String oldMiddle;

	@ParameterMarker(uiOrder=8, name="New String", description="If found, what string of characters should be used to replace the old string of characters? (Also used for adding prefix or suffix)", ui=MarkerConstants.UI_FILECHOOSER, defaultText=".")
	String newMiddle;

	/////////// Define Outputs ///////////

	// None.

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	/**
	 * This file renaming function will not overwrite a file that exists.
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry)
	{
		// Collect the inputs
		File folder = null;
		if(folderData.getDataMap().isEmpty())
		{
			folder = new File(this.inDir);
		}
		else
		{
			folder = FileReader.readFileObjectToFile(folderData);
		}
		if(folder == null || !folder.isDirectory())
		{
			JEXDialog.messageDialog("Could not find the specified directory - " + this.inDir, this);
			return false;
		}

		File[] files = folder.listFiles(new SimpleFileFilter(new String[] { extension }));
		if(files == null || files.length == 0)
			return false;
		int count = 0;
		int total = files.length;
		int n = 0, m = 0;
		for (File file : files)
		{
			// Get the filename without the extension
			String fileName = file.getName();
			fileName = FileUtility.getFileNameWithoutExtension(fileName);

			// Split the string into the desired parts
			String[] parts = new String[]{fileName};
			if(!splitString.equals(""))
			{
				String splitStringReg = "\\" + this.splitString;
				parts = fileName.split(splitStringReg);
			}
			else
			{
				this.splitIndex = 0;
			}

			// Grab the specified part
			if(this.splitIndex >= parts.length)
			{
				Logs.log("Skipping file named '" + fileName + "." + this.extension + "'. Inappropriate split index '" + this.splitIndex + "' when splitting using '" + this.splitString + "' on this filename.", this);
				continue;
			}
			String part = parts[this.splitIndex];

			// Define n
			if(this.N.equals("*"))
			{
				n = part.length();
			}
			else if(this.M.equals("*"))
			{
				n = 0;
			}
			else
			{
				n = Integer.parseInt(this.N);
			}

			// Define m
			if(this.N.equals("*"))
			{
				m = 0;
			}
			else if(this.M.equals("*"))
			{
				m = part.length();
			}
			else
			{
				m = Integer.parseInt(this.M);
			}
			if(fileName.length() <= n + m)
			{
				Logs.log("Skipping file named: " + fileName + "." + this.extension + " - inappropriate number of characters to skip in the specified substring: " + part, this);
				continue;
			}

			// Grab the beginning, middle, and end of the specific part.
			String beginning = part.substring(0, n);
			String middle = part.substring(n, part.length() - m);
			String end = part.substring(part.length() - m);

			// Fix the middle
			if(this.oldMiddle.equals("*"))
			{
				// Replace the whole middle
				middle = this.newMiddle;
			}
			else
			{
				String tempMiddle = middle + "_";
				if(N.equals("0") && M.equals("0"))
				{
					// Then replacing the whole thing.
					middle = middle.replace(this.oldMiddle, this.newMiddle);
				}
				else
				{
					while(!tempMiddle.equals(middle))
					{
						if(this.isCanceled())
						{
							return false;
						}
						tempMiddle = middle;
						// Search and replace within the middle
						middle = middle.replace(this.oldMiddle, this.newMiddle);
					}
				}
			}

			// Remake the filename
			parts[this.splitIndex] = beginning + middle + end;
			String newPath = this.remakeParts(parts);
			newPath = newPath + "." + this.extension;
			if(this.test)
			{
				Logs.log("Test of file renaming - OLD:" + file.getName() + "\t\t NEW:" + newPath, this);
			}
			else
			{
				newPath = folder.getPath() + File.separator + newPath;
				if(!(new File(newPath)).exists())
					this.renameFile(file, newPath);
			}
			
			count = count + 1;
			int percentage = (int) (100 * ((double) (count) / (double) total));
			JEXStatics.statusBar.setProgressPercentage(percentage);

		}

		// Return status
		return true;
	}

	private void renameFile(File toBeRenamed, String toFile)
	{
		if(!toBeRenamed.exists() || toBeRenamed.isDirectory())
		{

			Logs.log("Can't rename. File does not exist: " + toBeRenamed, 0, this);
			return;
		}

		File newFile = new File(toFile);

		// Rename
		if(!toBeRenamed.renameTo(newFile))
			Logs.log("Error renmaing file" + toBeRenamed + " to " + newFile, 0, this);
		else
			Logs.log("Renamed OLD:" + toBeRenamed.getName() + " to\t NEW:" + toFile, this);
	}

	private String remakeParts(String[] parts)
	{
		// Using a string builder is REDICULOUSLY faster. Writing the points for a big roi went from 5 mins to <1 sec !!!!
		if(parts.length > 0)
		{
			StringBuilder ret = new StringBuilder();
			ret.append(parts[0]);
			for (int i = 1; i < parts.length; i++)
			{
				ret.append(this.splitString);
				ret.append(parts[i]);
			}
			return ret.toString();
		}
		return "";
	}

}

// class AdjustImageHelperFunction implements GraphicalCrunchingEnabling,
// ImagePanelInteractor{
// ImagePanel imagepanel ;
// GraphicalFunctionWrap wrap ;
// DimensionMap[] dimensions ;
// String[] images ;
// ImagePlus im ;
// FloatProcessor imp ;
// int index = 0 ;
// int atStep = 0 ;
//
// boolean auto = false;
// double oldMin = 0;
// double oldMax = 1000;
// double newMin = 0;
// double newMax = 100;
// double gamma = 0.5;
// int depth = 16;
//
// ParameterSet params;
// JEXData imset;
// JEXEntry entry;
// String[] outputNames;
// public JEXData output;
//
// AdjustImageHelperFunction(JEXData imset, JEXEntry entry, String[]
// outputNames, ParameterSet parameters){
// // Pass the variables
// this.imset = imset;
// this.params = parameters;
// this.entry = entry;
// this.outputNames = outputNames;
//
// ////// Get params
// auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
// oldMin = Double.parseDouble(params.getValueOfParameter("Old Min"));
// oldMax = Double.parseDouble(params.getValueOfParameter("Old Max"));
// newMin = Double.parseDouble(params.getValueOfParameter("New Min"));
// newMax = Double.parseDouble(params.getValueOfParameter("New Max"));
// gamma = Double.parseDouble(params.getValueOfParameter("Gamma"));
// depth = Integer.parseInt(params.getValueOfParameter("Output Bit Depth"));
//
// TreeMap<DimensionMap,JEXDataSingle> map = imset.getDataMap();
// int length = map.size();
// images = new String[length];
// dimensions = new DimensionMap[length];
// int i = 0;
// for (DimensionMap dim: map.keySet()){
// JEXDataSingle ds = map.get(dim);
// String path = ds.get(JEXDataSingle.FOLDERNAME) + File.separator +
// ds.get(JEXDataSingle.FILENAME);
// dimensions[i] = dim;
// images[i] = path;
// i ++;
// }
//
// // Prepare the graphics
// imagepanel = new ImagePanel(this,"Adjust image");
// displayImage(index);
// wrap = new GraphicalFunctionWrap(this,params);
// wrap.addStep(0, "Select roi", new String[]
// {"Automatic","Old Min","Old Max","New Min","New Max","Output Bit Depth"});
// wrap.setInCentralPanel(imagepanel);
// wrap.setDisplayLoopPanel(true);
// }
//
// private void displayImage(int index){
// ImagePlus im = new ImagePlus(images[index]);
// imagepanel.setImage(im);
// }
//
// /**
// * Run the function and open the graphical interface
// * @return the ROI data
// */
// public JEXData doit(){
// if (!auto){
// wrap.start();
// }
// else {
// finishIT();
// }
//
// return output;
// }
//
// public void runStep(int index) {
// // Get the new parameters
// auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
// oldMin = Double.parseDouble(params.getValueOfParameter("Old Min"));
// oldMax = Double.parseDouble(params.getValueOfParameter("Old Max"));
// newMin = Double.parseDouble(params.getValueOfParameter("New Min"));
// newMax = Double.parseDouble(params.getValueOfParameter("New Max"));
// gamma = Double.parseDouble(params.getValueOfParameter("Gamma"));
// depth = Integer.parseInt(params.getValueOfParameter("Output Bit Depth"));
//
// // prepare the images for calculation
// ImagePlus im = new ImagePlus(images[index]);
// imagepanel.setImage(im);
// imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a
// float processor
//
// adjustImage();
//
// imagepanel.setImage(new ImagePlus("",imp));
// }
// public void runNext(){
// atStep = atStep+1;
// if (atStep > 0) atStep = 0;
// }
// public void runPrevious(){
// atStep = atStep-1;
// if (atStep < 0) atStep = 0;
// }
// public int getStep(){ return atStep;}
//
// public void loopNext(){
// index = index + 1;
//
// if (index >= images.length-1) index = images.length-1;
// if (index < 0) index = 0;
//
// runStep(index);
// }
// public void loopPrevious(){
// index = index - 1;
//
// if (index >= images.length-1) index = images.length-1;
// if (index < 0) index = 0;
//
// runStep(index);
// }
// public void recalculate(){}
//
// public void startIT() {
// wrap.displayUntilStep();
// }
// /**
// * Apply the roi to all other images
// */
// public void finishIT() {
// auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
// oldMin = Double.parseDouble(params.getValueOfParameter("Old Min"));
// oldMax = Double.parseDouble(params.getValueOfParameter("Old Max"));
// newMin = Double.parseDouble(params.getValueOfParameter("New Min"));
// newMax = Double.parseDouble(params.getValueOfParameter("New Max"));
// gamma = Double.parseDouble(params.getValueOfParameter("Gamma"));
// depth = Integer.parseInt(params.getValueOfParameter("Output Bit Depth"));
//
// output = new DefaultJEXData(JEXData.IMAGE,outputNames[0],"Adjusted image");
//
// // Run the function
// TreeMap<DimensionMap,JEXDataSingle> map = imset.getDataMap();
// int count = 0;
// int total = map.size();
// JEXStatics.statusBar.setProgressPercentage(0);
// for (DimensionMap dim: map.keySet()){
// JEXDataSingle ds = map.get(dim);
// String imagePath = ds.get(JEXDataSingle.FOLDERNAME) + File.separator +
// ds.get(JEXDataSingle.FILENAME);
// File imageFile = new File(imagePath);
// String imageName = imageFile.getName();
//
// // get the image
// im = new ImagePlus(imagePath);
// imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a
// float processor
//
// ////// Begin Actual Function
// adjustImage();
// ////// End Actual Function
//
// ////// Save the results
// String localDir = JEXStatics.jexManager.getLocalFolder(entry);
// String newFileName = FunctionUtility.getNextName(localDir, imageName, "A");
// String newImagePath = localDir + File.separator + newFileName;
// FunctionUtility.imSave(imp, "false", depth, newImagePath);
//
// JEXDataSingle outputds = new DefaultJEXDataSingle();
// outputds.put(JEXDataSingle.FOLDERNAME, localDir);
// outputds.put(JEXDataSingle.FILENAME, newFileName);
// output.addData(dim,outputds);
// Logs.log("Finished processing " + count + " of " + total +
// ".",1,this);
// count++;
//
// // Status bar
// int percentage = (int) (100 * ((double) count/ (double)map.size()));
// JEXStatics.statusBar.setProgressPercentage(percentage);
// }
//
//
// }
//
// private void adjustImage(){
// FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
// }
//
//
// public void clickedPoint(Point p) {}
// public void pressedPoint(Point p) {}
// public void mouseMoved(Point p){}
//
// }

