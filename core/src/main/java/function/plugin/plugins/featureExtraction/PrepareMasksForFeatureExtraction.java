// Define package name as "plugins" as show here
package function.plugin.plugins.featureExtraction;

import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXReader;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.IJ2.IJ2PluginUtility;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
// Import needed classes here 
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.Pair;
import net.imagej.ops.Op;
import net.imagej.ops.logic.RealLogic;
import net.imagej.ops.map.MapIterableIntervalToSamplingRAI;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;

// Specify plugin characteristics here
@Plugin(
		type = JEXPlugin.class,
		name="Prepare Masks for Feature Extraction",
		menuPath="Feature Extraction",
		visible=true,
		description="Function for manipulating a set of 8-bit black and white images to be used for feature extraction (e.g., overlaying segmentation lines and subtracting one channel from another like done for defining the cytoplasm)."
		)
public class PrepareMasksForFeatureExtraction<T extends RealType<T>> extends JEXPlugin {

	FeatureUtils utils = new FeatureUtils();

	// Define a constructor that takes no arguments.
	public PrepareMasksForFeatureExtraction()
	{}

	// ///////// Define Inputs here ///////////

	@InputMarker(uiOrder = 2, name = "Maxima", type = MarkerConstants.TYPE_ROI, description = "Maxima ROI", optional = false)
	JEXData roiData;

	@InputMarker(uiOrder = 1, name = "Segmentation Lines", type = MarkerConstants.TYPE_IMAGE, description = "Mask images (SHOULD have channel dimension)", optional = false)
	JEXData segData;

	@InputMarker(uiOrder = 1, name = "Thresholded Images", type = MarkerConstants.TYPE_IMAGE, description = "Mask that encompasses the entire cell or groups of cells (i.e., prior to segmentation).", optional = false)
	JEXData maskData;	


	// ///////// Define Parameters here ///////////

	@ParameterMarker(uiOrder = 1, name = "Channel Dim Name", description = "Name of the 'Channel' dimension of the image.", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "Channel")
	String channelDimName;

	@ParameterMarker(uiOrder = 2, name = "Channels to Union", description = "Which channels should be 'unioned' to determine the 'master/primary' region encompassing the 'whole cell'. (comma separated name list, avoid spaces, can be a single channel)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "Nuc,Cyt")
	String channelsToUnion;

	@ParameterMarker(uiOrder = 3, name = "Name for Union Result", description = "Which channels should be 'unioned' to determine the 'master/primary' region encompassing the 'whole cell'. (comma separated name list, avoid spaces, can be a single channel)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "WholeCell")
	String unionName;

	@ParameterMarker(uiOrder = 4, name = "Channels to Subtract from Union", description = "Which channels should be 'subtracted' from the unioned mask to define other regions for quantifiction (e.g., 'whole cell' - 'nuclear' = 'cytoplasm') [blank skips this step]", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String channelsToSubtract;

	@ParameterMarker(uiOrder = 5, name = "Names for Subtraction Results", description = "Names for subtraction result (e.g., 'Cyt' for result of 'whole cell' - 'nuclear' = 'cytoplasm', comma separated list of names, avoid spaces) [blank skips subtraction calculations]", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "Cyt")
	String subtractedNames;

	@ParameterMarker(uiOrder = 6, name = "Channels to Remove", description = "After unioning and subtracting, which channels should be removed, potentially due to redundancy (e.g., cytoplasmic stain unioned with nuclear and recalculated via subtraction) [blank avoids removing anything]", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String channelsToRemove;

	@ParameterMarker(uiOrder = 7, name = "Pixel Connectedness", description = "Connectedness of neighboring pixels for identifying objects in the mask/thresholded images.", ui = MarkerConstants.UI_DROPDOWN, choices = {"4 Connected", "8 Connected"}, defaultChoice = 0)
	String connectedness;

	/////////// Define Outputs here ///////////

	@OutputMarker(uiOrder = 1, name = "Prepared Masks", type = MarkerConstants.TYPE_IMAGE, flavor = "", description = "Thresholded images segmented by the segmentation lines and cleaned up to only show regions associated with cells.", enabled = true)
	JEXData outputImage;

	@OutputMarker(uiOrder = 1, name = "Clump Data", type = MarkerConstants.TYPE_FILE, flavor = "", description = "Test table output (i.e., for Weka etc).", enabled = true)
	JEXData outputFile;

	// Define threading capability here (set to 1 if using non-final static variables shared between function instances).
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	// Code the actions of the plugin here using comments for significant sections of code to enhance readability as shown here
	@Override
	public boolean run(JEXEntry optionalEntry)
	{

		// Check input validity
		if(!isInputValid(roiData, JEXData.ROI) || !isInputValid(segData, JEXData.IMAGE) || !isInputValid(maskData, JEXData.IMAGE))
		{
			return false;
		}

		// Check validity of parameters
		if(maskData.getDimTable().getDimWithName(channelDimName) == null)
		{
			JEXDialog.messageDialog("The mask images do not have a 'Channel' dimension named: " + channelDimName + ". Aborting.");
			return false;
		}
		Dim channelDim = maskData.getDimTable().getDimWithName(channelDimName);
		CSVList namesToUnion = new CSVList(channelsToUnion);
		CSVList namesToSubtract = new CSVList();
		if(!channelsToSubtract.equals(""))
		{
			namesToSubtract = new CSVList(channelsToSubtract);
		}
		for(String name : namesToUnion)
		{
			if(!channelDim.containsValue(name))
			{
				JEXDialog.messageDialog("The name: " + name + " does not exist in the image: " + maskData.getTypeName().getName() + ". Aborting.");
				return false;
			}
		}
		for(String name : namesToSubtract)
		{
			if(!channelDim.containsValue(name))
			{
				JEXDialog.messageDialog("The name: " + name + " does not exist in the image: " + maskData.getTypeName().getName() + ". Aborting.");
				return false;
			}
		}
		if(unionName.equals(""))
		{
			JEXDialog.messageDialog("Union name cannot be blank. Aborting.");
			return false;
		}
		CSVList subtractedResultNames = new CSVList(subtractedNames);
		if(namesToSubtract.size() > 0 && subtractedResultNames.size() != namesToSubtract.size())
		{
			JEXDialog.messageDialog("The list of specified channels to subtract does not match the number of names for subtraction results. Aborting.");
			return false;
		}
		CSVList channelsToRemoveList = new CSVList(channelsToRemove);

		TreeMap<DimensionMap, ROIPlus> roiMap = RoiReader.readObjectToRoiMap(roiData);
		TreeMap<DimensionMap, String> segMap = ImageReader.readObjectToImagePathTable(segData);
		TreeMap<DimensionMap, String> maskMap = ImageReader.readObjectToImagePathTable(maskData);

		TreeMap<DimensionMap, String> finalMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap, Integer> clumpMap = new TreeMap<DimensionMap,Integer>();

		// Get the subDimTable to iterate over
		DimTable subTable = maskData.getDimTable().getSubTable(channelDimName);

		// Calculate status variables
		int count = 0, percentage = 0;
		int total = subTable.mapCount();

		// Loop subTable
		for(DimensionMap subMap : subTable.getMapIterator())
		{
			if(this.isCanceled())
			{
				return true;
			}

			// Get, save, and put the union image
			Logs.log("Getting union images for subMap: " + subMap.toString(), this);
			Img<UnsignedByteType> union = getUnion(subMap.copy(), namesToUnion, maskMap);
			
			// Get the regions in the union image that correspond to maxima
			Logs.log("Getting regions corresponding to maxima", this);
			Pair<Img<UnsignedByteType>,TreeMap<Integer,PointList>> temp = utils.keepRegionsWithMaxima(union, connectedness.equals("4 Connected"), roiMap.get(subMap), this.getCanceler());
			union = temp.p1;
			
			// Get clump stats
			clumpMap.putAll(getClumpSize(temp.p2, subMap));

			// Segment and save the union image into finalMap
			Img<UnsignedByteType> segImage = JEXReader.getByteImage(segMap.get(subMap));
			
			Op andOp = IJ2PluginUtility.ij().op().op(RealLogic.And.class, RealType.class, RealType.class);
			IJ2PluginUtility.ij().op().run(MapIterableIntervalToSamplingRAI.class, union, segImage, andOp);
			
			//ImageJFunctions.show(segImage);
			//ImageJFunctions.show(union);
			String path = JEXWriter.saveImage(union);
			finalMap.put(subMap.copyAndSet(channelDimName + "=" + unionName), path);

			// Get subtracted forms of the segmented union image and put them into finalMap
			for(int i = 0; i < namesToSubtract.size(); i++)
			{
				Logs.log("Subtracting from whole cell image: " + namesToSubtract.get(i), this);
				if(this.isCanceled())
				{
					return true;
				}
				Img<UnsignedByteType> subtracted = getSubtractedImage(subMap.copy(), namesToSubtract.get(i), maskMap, union);
				path = JEXWriter.saveImage(subtracted);					
				finalMap.put(subMap.copyAndSet(channelDimName + "=" + subtractedResultNames.get(i)), path);
			}

			for(String name : channelDim.values())
			{
				if(this.isCanceled())
				{
					return true;
				}
				if(!(channelsToRemoveList.contains(name)))
				{
					Logs.log("Intersecting remaining images with Segmented Union Image: " + name, this);
					DimensionMap mapTemp = subMap.copyAndSet(channelDimName + "=" + name);
					Img<UnsignedByteType> tempMaskImg = JEXReader.getByteImage(maskMap.get(mapTemp));
					IJ2PluginUtility.ij().op().run(MapIterableIntervalToSamplingRAI.class, tempMaskImg, union, andOp);
					path = JEXWriter.saveImage(tempMaskImg);
					finalMap.put(mapTemp, path);
				}
			}
			// Count the fact we quantified an image
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) total)));
			Logs.log("Percentage: "+percentage, this);
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		outputImage = ImageWriter.makeImageStackFromPaths("temp", finalMap);
		String path = JEXTableWriter.writeTable("ClumpSize", clumpMap);
		outputFile = FileWriter.makeFileObject("temp", null, path);

		return true;
	}

	private Img<UnsignedByteType> getUnion(DimensionMap subMap, CSVList namesToUnion, TreeMap<DimensionMap,String> maskMap)
	{
		Img<UnsignedByteType> union = null;
		Op orOp = IJ2PluginUtility.ij().op().op(RealLogic.Or.class, RealType.class, RealType.class);
		for(String name : namesToUnion)
		{
			DimensionMap temp = subMap.copyAndSet(channelDimName + "=" + name);
			Img<UnsignedByteType> mask = JEXReader.getByteImage(maskMap.get(temp));
			if(union == null)
			{
				union = mask;
			}
			else
			{
				IJ2PluginUtility.ij().op().run(MapIterableIntervalToSamplingRAI.class, union, mask, orOp);
			}
		}
		return union;
	}

	private Img<UnsignedByteType> getSubtractedImage(DimensionMap subMap, String nameToSubtract, TreeMap<DimensionMap,String> maskMap, Img<UnsignedByteType> union)
	{
		Img<UnsignedByteType> ret = union.copy();
		DimensionMap temp = subMap.copyAndSet(channelDimName + "=" + nameToSubtract);
		Img<UnsignedByteType> toSubtract = JEXReader.getByteImage(maskMap.get(temp));
		Op lessThanOp = IJ2PluginUtility.ij().op().op(RealLogic.LessThan.class, RealType.class, RealType.class);
		IJ2PluginUtility.ij().op().run(MapIterableIntervalToSamplingRAI.class, ret, toSubtract, lessThanOp);
		return ret;
	}
	
	private TreeMap<DimensionMap,Integer> getClumpSize(TreeMap<Integer,PointList> clumps, DimensionMap subMap)
	{
		TreeMap<DimensionMap, Integer> ret = new TreeMap<DimensionMap,Integer>();
		for(PointList pl : clumps.values())
		{
			for(IdPoint p : pl)
			{
				DimensionMap toSave = subMap.copyAndSet("Id=" + p.id);
				ret.put(toSave, pl.size());
			}
		}
		return ret;
	}
}

