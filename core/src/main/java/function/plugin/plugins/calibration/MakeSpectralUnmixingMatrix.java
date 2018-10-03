package function.plugin.plugins.calibration;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageStatistics;
import image.roi.ROIPlus;

import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import tables.DimTable;
import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;

@Plugin(
		type = JEXPlugin.class,
		name="Make Spectral Unmixing Matrix",
		menuPath="Calibration",
		visible=true,
		description="Treat an image object as a stack, calculate the median/mean of subgroups of images, take the mean of the subgroups, and smooth if desired. Result of the first successful entry is copied to all selected entries if desired."
		)
public class MakeSpectralUnmixingMatrix extends JEXPlugin {
	
	public static ImagePlus sharedCalibrationImage = null;
	
	public MakeSpectralUnmixingMatrix()
	{}
	
	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Background Image (optional)", type=MarkerConstants.TYPE_IMAGE, description="Image with representative background intensities in the imaging channels of interest. Must match those in 'Isolated Fluor Images'.", optional=true)
	JEXData bgData;
	
	@InputMarker(uiOrder=2, name="Background Roi (optional)", type=MarkerConstants.TYPE_ROI, description="ROI to localize analysis of background within the 'Background Image' or the 'Isolated Fluor Images' if no background image is provided.", optional=true)
	JEXData bgRoiData;
	
	@InputMarker(uiOrder=3, name="Isolated Fluor Images", type=MarkerConstants.TYPE_IMAGE, description="Images of each isolated fluorophore in each imaging channel.", optional=false)
	JEXData calData;
	
	@InputMarker(uiOrder=4, name="Isolated Fluor Image Masks (optional)", type=MarkerConstants.TYPE_IMAGE, description="Mask to localize analysis of fluorescent intensities in the 'Isolated Fluor Images'.", optional=true)
	JEXData calMaskData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="Channel Dim Name", description="Name of the dimension describing the imaging channels", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Channel")
	String channelDimName;
	
	@ParameterMarker(uiOrder=2, name="Fluor Dim Name", description="Name of the dimension describing the fluorophores", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Fluor")
	String fluorDimName;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Background Matrix (BG)", type=MarkerConstants.TYPE_FILE, flavor="", description="The mean background intensities ", enabled=true)
	JEXData bgMatrix;
	
	@OutputMarker(uiOrder=2, name="Signal Matrix (SIG)", type=MarkerConstants.TYPE_FILE, flavor="", description="The full signal cross-talk/mixing matrix", enabled=true)
	JEXData calMatrix;
	
	@OutputMarker(uiOrder=3, name="Mixing Matrix (A=SIG-BG in p=Ax)", type=MarkerConstants.TYPE_FILE, flavor="", description="The background corrected cross-talk/mixing matrix", enabled=true)
	JEXData unmixingMatrix;
	
	@Override
	public int getMaxThreads()
	{
		return 1;
	}
	
	@Override
	public boolean run(JEXEntry entry)
	{
		
		// Check required inputs
		if(calData == null || !calData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		
		// Check optional inputs
		if(calMaskData != null && !calData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		if(bgData != null && !bgData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		if(bgRoiData != null && !bgRoiData.getTypeName().getType().matches(JEXData.ROI))
		{
			return false;
		}
		
		// Prepare a data structure to store the results
		TreeMap<DimensionMap,Double> bgMeans = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> sigMeans = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> unmixingMeans = new TreeMap<DimensionMap,Double>();
		
		// First calculate the amount of intensity in each channel of the background image
		TreeMap<DimensionMap,String> bgImages;
		
		if(bgData == null)
		{
			bgImages = ImageReader.readObjectToImagePathTable(calData);
		}
		else
		{
			bgImages = ImageReader.readObjectToImagePathTable(bgData);
		}
		
		DimTable table = calData.getDimTable();
		TreeMap<DimensionMap,ROIPlus> bgRois = new TreeMap<DimensionMap,ROIPlus>();
		if(bgRoiData != null)
		{
			bgRois = RoiReader.readObjectToRoiMap(bgRoiData);
		}
		
		for(DimensionMap map : table.getDimensionMaps())
		{
			ROIPlus roi = bgRois.get(map);
			ImagePlus bgImage = new ImagePlus(bgImages.get(map));
			FloatProcessor fp = bgImage.getProcessor().convertToFloatProcessor();
			
			if(bgRois.size() > 0)
			{
				// Only use images that have a roi defined
				if(roi != null)
				{
					fp.setRoi(roi.getRoi());
					ImageStatistics stats = fp.getStatistics();
					
					// We want a result that only has a color dimension
					DimensionMap toAdd = new DimensionMap();
					toAdd.put(channelDimName, map.get(channelDimName));
					bgMeans.put(toAdd, stats.mean);
				}
			}
			else
			{
				// Use all images but store only 1 value per color
				ImageStatistics stats = fp.getStatistics();
				
				// We want a result that only has a color dimension
				DimensionMap toAdd = new DimensionMap();
				toAdd.put(channelDimName, map.get(channelDimName));
				bgMeans.put(toAdd, stats.mean);
			}
			bgImage.flush();
		}
		
		// Now we have the gackground intensity for each color that we can subtract from each image of each dye in each color
		// But first we need to get mean intensities for each color of each calibration image.
		TreeMap<DimensionMap,String> calImages = ImageReader.readObjectToImagePathTable(calData);
		TreeMap<DimensionMap,String> calMasks = new TreeMap<DimensionMap,String>();
		if(calMaskData != null)
		{
			calMasks = ImageReader.readObjectToImagePathTable(calMaskData);
		}
		
		ByteProcessor mask;
		for(DimensionMap map : table.getDimensionMaps())
		{
			ImagePlus calImage = new ImagePlus(calImages.get(map));
			FloatProcessor fp = calImage.getProcessor().convertToFloatProcessor();
			
			if(calMasks.size() > 0)
			{
				mask = (ByteProcessor) (new ImagePlus(calMasks.get(map))).getProcessor(); 
				fp.setMask(mask);
			}
			
			double mean = fp.getStatistics().mean;
			sigMeans.put(map, mean);
		}
		
		// Now we subtract background intensities from the the full signal intensities
		for(DimensionMap map : table.getDimensionMaps())
		{
			unmixingMeans.put(map, sigMeans.get(map) - bgMeans.get(map));
		}		
		
		// Write the files and save them
		String bgPath = JEXTableWriter.writeTable("Background", bgMeans);
		String sigPath = JEXTableWriter.writeTable("Signal", sigMeans);
		String unmixingPath = JEXTableWriter.writeTable("UnmixingMatrix", unmixingMeans);
		
		bgMatrix = FileWriter.makeFileObject("temp", null, bgPath);
		calMatrix = FileWriter.makeFileObject("temp", null, sigPath);
		unmixingMatrix = FileWriter.makeFileObject("temp", null, unmixingPath);
		
		return true;
	}
}
