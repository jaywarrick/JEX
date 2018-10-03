package function.plugin.plugins.calibration;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import ij.ImagePlus;

@Plugin(
		type = JEXPlugin.class,
		name="Make Spectral Unmixing Matrix 2",
		menuPath="Calibration",
		visible=true,
		description="Treat an image object as a stack, calculate the median/mean of subgroups of images, take the mean of the subgroups, and smooth if desired. Result of the first successful entry is copied to all selected entries if desired."
		)
public class MakeSpectralUnmixingMatrixWithMask extends JEXPlugin {

	public static ImagePlus sharedCalibrationImage = null;

	public MakeSpectralUnmixingMatrixWithMask()
	{}

	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
	/////////// Define Inputs ///////////

	// A typical image set for this might be a sample stained with one fluorophore and imaged in many channels (especially those that observe bleed through).
	// Thus the object typically has one dimension of Channel=channel1, channel 2, ... channel n.
	// This image should already be corrected for background.
	@InputMarker(uiOrder=3, name="Images", type=MarkerConstants.TYPE_IMAGE, description="Images (already background corrected) of an isolated fluorophore in each imaging channel.", optional=false)
	JEXData calData;

	// The mask data would be a set of black and white images to isolate analysis of intensities to those regions. Typically matching or exceeding dimensionality of the calData images
	@InputMarker(uiOrder=4, name="Masks (optional)", type=MarkerConstants.TYPE_IMAGE, description="Mask to localize analysis of intensities in the 'Images'.", optional=true)
	JEXData calMaskData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Channel Dim Name", description="Name of the dimension describing the imaging channels", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Channel")
	String channelDimName;

	@ParameterMarker(uiOrder=1, name="Exposure Times", description="Comma separated list of exposure times of each channel in [ms] ordered as they are in the image", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1,1,1")
	String exposureTimes;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=3, name="Normalized Spectrum", type=MarkerConstants.TYPE_FILE, flavor="", description="The per [ms] signal in each channel normalized to max observed in any channel [i.e., 1 unit is now equivalent to signal from this calibration sample in the optimal channel].", enabled=true)
	JEXData spectrum;

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	@Override
	public boolean run(JEXEntry entry)
	{

		//		// Check required inputs
		//		if(calData == null || !calData.getTypeName().getType().matches(JEXData.IMAGE))
		//		{
		//			return false;
		//		}
		//		
		//		Vector<Double> times = new Vector<Double>();
		//		try
		//		{
		//			CSVList timesList = new CSVList(exposureTimes);
		//			for(String t : timesList)
		//			{
		//				times.add(Double.parseDouble(t));
		//			}
		//		}
		//		catch(Exception e)
		//		{
		//			JEXDialog.messageDialog("Couldn't parse the exposure times from the provided txt. Please use form like 100,300,250");
		//			return false;
		//		}
		//
		//		// Check optional inputs
		//		if(calMaskData != null && !calData.getTypeName().getType().matches(JEXData.IMAGE))
		//		{
		//			return false;
		//		}
		//
		//		// Prepare a data structure to store the results
		//		TreeMap<DimensionMap,Double> sigMeans = new TreeMap<DimensionMap,Double>();
		//		TreeMap<DimensionMap,Double> spectrum = new TreeMap<DimensionMap,Double>();
		//
		//		//		// Grab background rois if possible
		//		//		TreeMap<DimensionMap,ROIPlus> bgRois = new TreeMap<DimensionMap,ROIPlus>();
		//		//		if(bgRoiData != null)
		//		//		{
		//		//			bgRois = RoiReader.readObjectToRoiMap(bgRoiData);
		//		//		}
		//
		//		// Now we have the background intensity for each color that we can subtract from each image of each dye in each color
		//		// But first we need to get mean intensities for each color of each calibration image.
		//		TreeMap<DimensionMap,String> calImages = ImageReader.readObjectToImagePathTable(calData);
		//		TreeMap<DimensionMap,String> calMasks = new TreeMap<DimensionMap,String>();
		//
		//		// Get image masks if they are available
		//		if(calMaskData != null)
		//		{
		//			calMasks = ImageReader.readObjectToImagePathTable(calMaskData);
		//		}
		//
		//		// Go through each calData dimension and ...
		//		DimTable calTable = calData.getDimTable();
		//		Dim channelDim = calTable.getDimWithName(channelDimName);
		//		
		//		// Create a map that links a channel name to an exposure time.
		//		if(channelDim.size() != times.size())
		//		{
		//			JEXDialog.messageDialog("The number of channels and number of exposure times did not match. Aborting for this entry.");
		//			return false;
		//		}
		//		TreeMap<String, Double> timeMap = new TreeMap<String, Double>(new StringUtility()); // Use string utility to sort the map the same way that the dims are sorted.
		//		for(int i = 0; i < channelDim.size(); i++)
		//		{
		//			timeMap.put(channelDim.valueAt(i), times.get(i));
		//		}
		//		
		//		// Measure the calibration images
		//		ByteProcessor mask;
		//		for(DimensionMap map : calTable.getDimensionMaps())
		//		{
		//			// Grab the cal image
		//			ImagePlus calImage = new ImagePlus(calImages.get(map));
		//			FloatProcessor fp = calImage.getProcessor().convertToFloatProcessor();
		//
		//			String maskPath = calMasks.get(map);
		//			if(maskPath != null)
		//			{
		//				mask = (ByteProcessor) (new ImagePlus(maskPath)).getProcessor(); 
		//				fp.setMask(mask);
		//			}
		//
		//			double mean = fp.getStatistics().mean;
		//			sigMeans.put(map, mean);
		//		}
		//
		//		// Now we divide the 
		//		for(DimensionMap map : calTable.getDimensionMaps())
		//		{
		//			spectrum.put(map, sigMeans.get(map) - bgMeans.get(map));
		//		}		
		//
		//		// Write the files and save them
		//		String bgPath = JEXTableWriter.writeTable("Background", bgMeans);
		//		String sigPath = JEXTableWriter.writeTable("Signal", sigMeans);
		//		String unmixingPath = JEXTableWriter.writeTable("UnmixingMatrix", spectrum);
		//
		//		bgMatrix = FileWriter.makeFileObject("temp", null, bgPath);
		//		calMatrix = FileWriter.makeFileObject("temp", null, sigPath);
		//		spectrum = FileWriter.makeFileObject("temp", null, unmixingPath);

		/////////////// CONFLICT //////////////

		//		// Check required inputs
		//		if(calData == null || !calData.getTypeName().getType().matches(JEXData.IMAGE))
		//		{
		//			return false;
		//		}
		//		
		//		Vector<Double> times = new Vector<Double>();
		//		try
		//		{
		//			CSVList timesList = new CSVList(exposureTimes);
		//			for(String t : timesList)
		//			{
		//				times.add(Double.parseDouble(t));
		//			}
		//		}
		//		catch(Exception e)
		//		{
		//			JEXDialog.messageDialog("Couldn't parse the exposure times from the provided txt. Please use form like 100,300,250");
		//			return false;
		//		}
		//
		//		// Check optional inputs
		//		if(calMaskData != null && !calData.getTypeName().getType().matches(JEXData.IMAGE))
		//		{
		//			return false;
		//		}
		//
		//		// Prepare a data structure to store the results
		//		TreeMap<DimensionMap,Double> sigMeans = new TreeMap<DimensionMap,Double>();
		//		TreeMap<DimensionMap,Double> spectrum = new TreeMap<DimensionMap,Double>();
		//
		//		//		// Grab background rois if possible
		//		//		TreeMap<DimensionMap,ROIPlus> bgRois = new TreeMap<DimensionMap,ROIPlus>();
		//		//		if(bgRoiData != null)
		//		//		{
		//		//			bgRois = RoiReader.readObjectToRoiMap(bgRoiData);
		//		//		}
		//
		//		// Now we have the background intensity for each color that we can subtract from each image of each dye in each color
		//		// But first we need to get mean intensities for each color of each calibration image.
		//		TreeMap<DimensionMap,String> calImages = ImageReader.readObjectToImagePathTable(calData);
		//		TreeMap<DimensionMap,String> calMasks = new TreeMap<DimensionMap,String>();
		//
		//		// Get image masks if they are available
		//		if(calMaskData != null)
		//		{
		//			calMasks = ImageReader.readObjectToImagePathTable(calMaskData);
		//		}
		//
		//		// Go through each calData dimension and ...
		//		DimTable calTable = calData.getDimTable();
		//		Dim channelDim = calTable.getDimWithName(channelDimName);
		//		
		//		// Create a map that links a channel name to an exposure time.
		//		if(channelDim.size() != times.size())
		//		{
		//			JEXDialog.messageDialog("The number of channels and number of exposure times did not match. Aborting for this entry.");
		//			return false;
		//		}
		//		TreeMap<String, Double> timeMap = new TreeMap<String, Double>(new StringUtility()); // Use string utility to sort the map the same way that the dims are sorted.
		//		for(int i = 0; i < channelDim.size(); i++)
		//		{
		//			timeMap.put(channelDim.valueAt(i), times.get(i));
		//		}
		//		
		//		// Measure the calibration images
		//		ByteProcessor mask;
		//		for(DimensionMap map : calTable.getDimensionMaps())
		//		{
		//			// Grab the cal image
		//			ImagePlus calImage = new ImagePlus(calImages.get(map));
		//			FloatProcessor fp = calImage.getProcessor().convertToFloatProcessor();
		//
		//			String maskPath = calMasks.get(map);
		//			if(maskPath != null)
		//			{
		//				mask = (ByteProcessor) (new ImagePlus(maskPath)).getProcessor(); 
		//				fp.setMask(mask);
		//			}
		//
		//			double mean = fp.getStatistics().mean;
		//			sigMeans.put(map, mean);
		//		}
		//
		//		// Now we divide the 
		//		for(DimensionMap map : calTable.getDimensionMaps())
		//		{
		//			spectrum.put(map, sigMeans.get(map) - bgMeans.get(map));
		//		}		
		//
		//		// Write the files and save them
		//		String bgPath = JEXTableWriter.writeTable("Background", bgMeans);
		//		String sigPath = JEXTableWriter.writeTable("Signal", sigMeans);
		//		String unmixingPath = JEXTableWriter.writeTable("UnmixingMatrix", spectrum);
		//
		//		bgMatrix = FileWriter.makeFileObject("temp", null, bgPath);
		//		calMatrix = FileWriter.makeFileObject("temp", null, sigPath);
		//		spectrum = FileWriter.makeFileObject("temp", null, unmixingPath);


		return true;
	}
}
