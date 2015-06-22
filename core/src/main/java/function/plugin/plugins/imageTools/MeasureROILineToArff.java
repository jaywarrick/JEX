package function.plugin.plugins.imageTools;

import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Point;
import java.util.List;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;

import org.scijava.plugin.Plugin;

import tables.DimTable;
import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;

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
		name="Measure ROI Line to Arff",
		menuPath="Image Tools",
		visible=true,
		description="Measure the width, height, length, and angle of the line"
		)
public class MeasureROILineToArff extends JEXPlugin {
	
	public MeasureROILineToArff()
	{}
	
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Line ROI", type=MarkerConstants.TYPE_ROI, description="ROI to be measured.", optional=false)
	JEXData roiData;
	
	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Angle Range", description="Choose the way in which the angle is measured by indicating the limits of the measurement.", ui=MarkerConstants.UI_DROPDOWN, choices={ "-pi/2 to pi/2", "-pi to pi" }, defaultChoice=1)
	String angleRange;
	
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
			
		boolean useAtan2 = angleRange.equals("-pi to pi");
		
		// Run the function
		TreeMap<DimensionMap,ROIPlus> rois = RoiReader.readObjectToRoiMap(roiData);
		
		TreeMap<DimensionMap,Double> measurements = new TreeMap<DimensionMap,Double>();
		ROIPlus roi;
		double width, height, length, angle;
		DimTable roiDimTable = roiData.getDimTable();
		List<DimensionMap> maps = roiDimTable.getDimensionMaps();
		int total = maps.size(), count = 0;
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
				PointList pl = roi.getPointList();
				Point p1 = pl.get(0);
				Point p2 = pl.get(1);
				width = Math.abs(p2.x - p1.x);
				height = Math.abs(p2.y - p1.y);
				length = Math.sqrt(width * width + height * height);
				if(useAtan2)
				{
					angle = Math.atan2(p2.y - p1.y, p2.x - p1.x);
				}
				else
				{
					angle = Math.atan(height / width);
				}
				DimensionMap temp = map.copyAndSet("Measurement=width");
				measurements.put(temp, width);
				temp = map.copyAndSet("Measurement=height");
				measurements.put(temp, height);
				temp = map.copyAndSet("Measurement=length");
				measurements.put(temp, length);
				temp = map.copyAndSet("Measurement=angle");
				measurements.put(temp, angle);
				
				JEXStatics.statusBar.setProgressPercentage((int) (100 * (double) count / total));
			}
		}
		
		String outputFile = JEXTableWriter.writeTable("LineMeasurements", measurements);
		
		output = FileWriter.makeFileObject("temp", null, outputFile);
		
		// Return status
		return true;
	}
}
