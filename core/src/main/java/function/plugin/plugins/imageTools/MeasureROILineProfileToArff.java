package function.plugin.plugins.imageTools;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

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
import ij.ImagePlus;
import ij.gui.ProfilePlot;
import ij.gui.Roi;
import image.roi.ROIPlus;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.JEXCSVWriter;
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
		name="Measure ROI Line Profile to Arff",
		menuPath="Image Tools",
		visible=true,
		description="Measure the pixel intensities along a line (or rectangle)."
		)
public class MeasureROILineProfileToArff extends JEXPlugin {
	
	public MeasureROILineProfileToArff()
	{}
	
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be measured.", optional=false)
	JEXData imageData;
	
	@InputMarker(uiOrder=1, name="Line ROI", type=MarkerConstants.TYPE_ROI, description="ROI to be measured.", optional=false)
	JEXData roiData;
	
	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Line Width", description="Total width of the line for analyzing the profile.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="10.0")
	double width;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Measurements", type=MarkerConstants.TYPE_FILE, flavor="", description="Table of the resultant measurements", enabled=true)
	JEXData output;
	
	
	@Override
	public int getMaxThreads()
	{
		return 10;
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

		if(roiData == null || !roiData.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}
		
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		// Run the function
		TreeMap<DimensionMap,ROIPlus> rois = RoiReader.readObjectToRoiMap(roiData);
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(imageData);

		ROIPlus roi;
		ImagePlus im;
		List<DimensionMap> maps = imageData.getDimTable().getDimensionMaps();
		int total = maps.size(), count = 0;
		JEXCSVWriter writer = new JEXCSVWriter();
		for (DimensionMap map : maps)
		{
			roi = rois.get(map);
			if(roi != null && roi.getPointList() != null && roi.getPointList().size() != 0)
			{
				if(roi.type != ROIPlus.ROI_LINE)
				{
					Logs.log("ROI was not a 'line' ROI. Aborting", this);
					return false;
				}
				
				im = new ImagePlus(images.get(map));
				Roi line = roi.getRoi();
				line.setStrokeWidth(this.width);
				im.setRoi(line);
				ProfilePlot pp = new ProfilePlot(im);
				double[] profile = pp.getProfile();
				
				List<String> info = getRowOfInfo(map, profile);
				
				writer.write(info);
				
				JEXStatics.statusBar.setProgressPercentage((int) (100 * (double) count / total));
			}
		}
		writer.close();
		
		output = FileWriter.makeFileObject("temp", null, writer.getPath());
		
		// Return status
		return true;
	}
	
	public List<String> getRowOfInfo(DimensionMap map, double[] profile)
	{
		List<String> ret = new Vector<>();
		for(Entry<String,String> e : map.entrySet())
		{
			ret.add(e.getValue());
		}
		
		for(double d : profile)
		{
			ret.add(""+d);
		}
		return(ret);
	}
}
