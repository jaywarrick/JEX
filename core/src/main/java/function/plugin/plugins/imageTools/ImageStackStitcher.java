package function.plugin.plugins.imageTools;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.LabelReader;
import Database.DataReader.ValueReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import function.plugin.old.JEX_ImageTools_Stitch_Coord;
import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import image.roi.IdPoint;
import image.roi.PointList;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import logs.Logs;
import miscellaneous.CSVList;
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
 * @author erwinberthier
 * 
 */
@Plugin(
		type = JEXPlugin.class,
		name="Image Stack Stitcher",
		menuPath="Image Tools > Stitching",
		visible=true,
		description="Function that allows you to stitch an image STACK into a single image using two image alignment objects."
		)
public class ImageStackStitcher extends JEXPlugin {

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="H-Alignment", type=MarkerConstants.TYPE_VALUE, description="Horizontal alignment (L to R adjacent images) object obtained from the image aligner plugin on the plugins tab of JEX.", optional=false)
	JEXData hData;

	@InputMarker(uiOrder=2, name="V-Alignment", type=MarkerConstants.TYPE_VALUE, description="Vertical alignment (Top to Bottom adjeacent images) object obtained from the image aligner plugin on the plugins tab of JEX.", optional=false)
	JEXData vData;

	@InputMarker(uiOrder=3, name="Images", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;


	/////////// Define Parameters ///////////


	@ParameterMarker(uiOrder=-1, name="Location Dim Name", description="Name of the row dimension in the imageset.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Location")
	String locDimName;

	@ParameterMarker(uiOrder=0, name="Number of Columns", description="Number of columns that make up the stitched image. (Can also specify a label name)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2")
	String colsString;

	@ParameterMarker(uiOrder=1, name="Starting Point", description="In what corner is the first image of each image group to be stitched.", ui=MarkerConstants.UI_DROPDOWN, choices={"UL", "UR", "LL", "LR"}, defaultChoice=0)
	String startPt;

	@ParameterMarker(uiOrder=2, name="Horizontal First Movement?", description="From the start point, which direction is the next image, (checked = horizontal, unchecked = vertical)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean horizontal;

	@ParameterMarker(uiOrder=3, name="Snaking Path?", description="From the start point, which direction is the next image", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean snaking;

	@ParameterMarker(uiOrder=4, name="Size Scale", description="How much to multiply the image size (0.5 reduces image to half its original size)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double scale;

	@ParameterMarker(uiOrder=5, name="Intensity Multiplier", description="How much to multiply the image size (0.5 reduces image to half its original size)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double multiplier;

	@ParameterMarker(uiOrder=6, name="Output Bit Depth", description="Depth of the outputted image", ui=MarkerConstants.UI_DROPDOWN, choices={ "8", "16", "32" }, defaultChoice=1)
	int bitDepth;

	@ParameterMarker(uiOrder=7, name="Stitched Image BG Intensity", description="Intensity to set for the background of the stitched image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	double background;

	@ParameterMarker(uiOrder=8, name="Normalize Intensities Fit Bit Depth", description="Scale intensities to go from 0 to max value determined by new bit depth (\'true\' overrides intensity multiplier).", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean normalize;


	////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Stitched Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant stitched image", enabled=true)
	JEXData output;

	public ImageStackStitcher()
	{}

	// ----------------------------------------------------
	// --------- INFORMATION ABOUT THE FUNCTION -----------
	// ----------------------------------------------------


	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	// ----------------------------------------------------
	// --------- THE ACTUAL MEAT OF THIS FUNCTION ---------
	// ----------------------------------------------------

	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry)
	{
		// Collect the inputs
		if(hData == null || !hData.getTypeName().getType().equals(JEXData.VALUE))
		{
			return false;
		}
		CSVList alignmentInfoHor = new CSVList(ValueReader.readValueObject(hData));
		int horDxImage = Integer.parseInt(alignmentInfoHor.get(0));
		int horDyImage = Integer.parseInt(alignmentInfoHor.get(1));

		if(vData == null || !vData.getTypeName().getType().equals(JEXData.VALUE))
		{
			return false;
		}
		CSVList alignmentInfoVer = new CSVList(ValueReader.readValueObject(vData));
		int verDxImage = Integer.parseInt(alignmentInfoVer.get(0));
		int verDyImage = Integer.parseInt(alignmentInfoVer.get(1));

		// Collect the inputs
		if(imageData == null)
		{
			JEXDialog.messageDialog("No object by that name exists for this entry: X" + entry.getTrayX() + " Y" + entry.getTrayY());
			return false;
		}
		// Collect the inputs
		if(!imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			JEXDialog.messageDialog("The object provided is not an 'Image' object.");
			return false;
		}
		DimTable table = imageData.getDimTable();

		// Get the number of columns
		Integer cols = -1;
		JEXData colsLabel = null;
		try
		{
			cols = Integer.parseInt(colsString);
		}
		catch(NumberFormatException e)
		{
			try
			{
				colsLabel = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.LABEL, colsString), entry);
				if(colsLabel == null)
				{
					JEXDialog.messageDialog("Couldn't parse '" + colsString + "' to a number or to an existing label in this directory");
				}
				cols = Integer.parseInt(LabelReader.readLabelValue(colsLabel));
			}
			catch(NumberFormatException e2)
			{
				JEXDialog.messageDialog("Couldn't parse '" + colsString + "' to a number, so looked and found a corresponding label but couldn't parse the label value to an integer. Label value: " + LabelReader.readLabelValue(colsLabel));
			}
		}

		Dim locDim = table.getDimWithName(locDimName);
		if(locDim == null)
		{
			JEXDialog.messageDialog("No dimension called '" + locDimName + "' in the object for this entry: X" + entry.getTrayX() + " Y" + entry.getTrayY());
			return false;
		}
		int rows = (int)Math.ceil(((double)locDim.size())/((double)cols));

		// Run the function
		PointList imageCoords = getMovements(horDxImage, horDyImage, verDxImage, verDyImage, rows, cols, horizontal, snaking, startPt, scale);

		// Remove the row and col Dim's from the DimTable and iterate through it
		// and stitch.
		table.remove(locDim);
		Map<DimensionMap,String> stitchedImageFilePaths = new HashMap<DimensionMap,String>();
		for (DimensionMap partialMap : table.getDimensionMaps())
		{
			if(this.isCanceled())
			{
				return false;
			}
			try
			{
				List<DimensionMap> mapsToGet = getMapsForStitching(locDim, partialMap);
				File stitchedFile = stitch(entry, imageData, mapsToGet, imageCoords, scale, normalize, multiplier, bitDepth, background, null, null, null);
				stitchedImageFilePaths.put(partialMap, stitchedFile.getAbsolutePath());
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return false;
			}

		}

		output = ImageWriter.makeImageStackFromPaths("temp", stitchedImageFilePaths);

		// Return status
		return true;
	}

	public static List<DimensionMap> getMapsForStitching(Dim locDim, DimensionMap partialMap)
	{
		List<DimensionMap> ret = new Vector<DimensionMap>();
		for (int l = 0; l < locDim.size(); l++)
		{
			DimensionMap imageMap = partialMap.copy();
			imageMap.put(locDim.name(), locDim.valueAt(l));
			ret.add(imageMap);
		}
		return ret;
	}

	public static PointList getMovements(int horDxImage, int horDyImage, int verDxImage, int verDyImage, int rows, int cols, boolean horizontal, boolean snaking, String startPt, double scale)
	{

		PointList ret = new PointList();
		Vector<Integer> myRows = getIndices(rows, false);
		Vector<Integer> myRowsRev = getIndices(rows, true);
		Vector<Integer> myCols = getIndices(cols, false);
		Vector<Integer> myColsRev = getIndices(cols, true);

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

		if(!snaking && horizontal)
		{
			for (int r : theRows)
			{
				for (int c : theCols)
				{
					ret.add(r * verDxImage + c * horDxImage, r * (1* verDyImage) + c * (1 * horDyImage));
				}
			}
		}
		else if(!snaking && !horizontal)
		{
			for (int c : theCols)
			{
				for (int r : theRows)
				{
					ret.add(r * verDxImage + c * horDxImage, r * (1* verDyImage) + c * (1 * horDyImage));
				}
			}
		}
		else if(snaking && horizontal)
		{
			for (int r : theRows)
			{
				for (int c : theCols)
				{
					ret.add(r * verDxImage + c * horDxImage, r * (1* verDyImage) + c * (1 * horDyImage));
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
					ret.add(r * verDxImage + c * horDxImage, r * (1* verDyImage) + c * (1 * horDyImage));
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

		// Scale and put bounding rectangle at 0,0
		ret.scale(scale);
		Rectangle rect = ret.getBounds();
		ret.translate(-1 * rect.x, -1 * rect.y);
		System.out.println(ret.getBounds());
		return ret;
	}

	public static Vector<Integer> getIndices(int count, boolean reversed)
	{
		Vector<Integer> dim = new Vector<Integer>();
		if(!reversed)
		{
			for(int i = 0; i < count; i++)
			{
				dim.add(i);
			}
		}
		else
		{
			for(int i = count-1; i > -1; i--)
			{
				dim.add(i);
			}
		}
		return dim;
	}

	public static File stitch(JEXEntry entry, JEXData imageData, List<DimensionMap> imageDimMaps, PointList imageDisplacements, double scale, boolean normalize, double multiplier, int bits, double background, Integer gridWidth, Integer gridHeight, Integer rowsPerPage)
	{
		/// prepare a blank image on which to copy the others
		if(imageData == null || ImageReader.readObjectToImagePath(imageData, imageDimMaps.get(0)) == null)
		{
			Logs.log("Couldn't find the first image in the image data provided. Can't calculate prototypical image size. Aborting.", ImageStackStitcher.class);
			return null;
		}
		ImagePlus original = new ImagePlus(ImageReader.readObjectToImagePath(imageData, imageDimMaps.get(0)));
		double imSizeX = gridWidth;
		double imSizeY = gridHeight;
		if(gridWidth == null)
		{
			imSizeX = original.getWidth() * scale;
		}
		if(gridHeight == null)
		{
			imSizeY = original.getHeight() * scale;
		}
		Rectangle rect = imageDisplacements.getBounds();
		int totalWidth = (rect.width + ((int) imSizeX));
		int totalHeight = (rect.height + ((int) imSizeY));
		FloatProcessor stitchIP = null;
		stitchIP = new FloatProcessor(totalWidth, totalHeight);
		stitchIP.set(background);
		ImagePlus stitch = new ImagePlus("Stitch", stitchIP);

		PointList xy = imageDisplacements;
		int count = 0;
		Iterator<DimensionMap> itr = imageDimMaps.iterator();
		Iterator<IdPoint> itrXY = xy.iterator();
		int percentage;
		FloatProcessor imp;
		ImagePlus im;

		while (itr.hasNext() && itrXY.hasNext())
		{
			DimensionMap map = itr.next();
			// //// Prepare float processor
			String path = ImageReader.readObjectToImagePath(imageData, map);			
			if(path != null)
			{
				Logs.log("Getting file " + path + " in entry " + entry.getTrayX() + "," + entry.getTrayY() + " for dim " + map.toString(), 0, JEX_ImageTools_Stitch_Coord.class);
				im = new ImagePlus(path);
				imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor

				// //// Begin Actual Function
				if(scale != 1.0)
				{
					imp.setInterpolationMethod(ImageProcessor.BILINEAR);
					imp = (FloatProcessor) imp.resize((int) imSizeX, (int) imSizeY);
				}
				Point p = itrXY.next();
				// System.out.println(p);
				stitchIP.copyBits(imp, p.x, p.y, Blitter.COPY);
				// //// End Actual Function
				im.flush();
			}
			count = count + 1;
			percentage = (int) (100 * ((count) / ((double) imageDimMaps.size() + 1)));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}

		// //// Save the results
		stitch.setProcessor("Stitch", stitchIP);
		count = count + 1;
		percentage = (int) (100 * ((count) / ((double) imageDimMaps.size() + 2)));
		JEXStatics.statusBar.setProgressPercentage(percentage);

		// Save the resulting image file
		ImagePlus toSave = FunctionUtility.makeImageToSave(stitchIP, normalize, 1.0, bits);
		String imPath = JEXWriter.saveImage(toSave);
		File result = new File(imPath);

		count = count + 1;
		percentage = (int) (100 * ((count) / ((double) imageDimMaps.size() + 2)));
		JEXStatics.statusBar.setProgressPercentage(percentage);

		return result;
	}
}
