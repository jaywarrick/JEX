package function.plugin.plugins.singleCell;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;

import org.scijava.plugin.Plugin;

import tables.DimTable;
import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
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
		name="Measure Secretion",
		menuPath="Single Cell",
		visible=true,
		description="Quantify captured secretion on beads in microwells by determining 'secretion per bead'."
		)
public class MeasureSecretion extends JEXPlugin {

	public MeasureSecretion()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be quantified.", optional=false)
	JEXData imageData;
	
	@InputMarker(uiOrder=2, name="Microwell Locations", type=MarkerConstants.TYPE_ROI, description="ROI of microwell locations.", optional=false)
	JEXData roiData;
	
	@InputMarker(uiOrder=3, name="Enhanced BF Image", type=MarkerConstants.TYPE_IMAGE, description="Brightfield image that can help determine which image items are beads and not random fluorescent particles", optional=true)
	JEXData bfData;
	
	@InputMarker(uiOrder=4, name="Cells", type=MarkerConstants.TYPE_ROI, description="Locations of cells in the microwell array images.", optional=true)
	JEXData cellData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="Color Dim Name", description="Name of the 'Color' dimension", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Color")
	String colorDimName;
	
	@ParameterMarker(uiOrder=2, name="Secretion Color Dim", description="Value of the 'Color' dim that represents the secretion signal to be quantified.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	String secretionColor;
	
	@ParameterMarker(uiOrder=3, name="Bead Color Dim", description="Value of the 'Color' dim that represents the bead signal used as reference for quantifying secretion signal.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	String beadColor;
	
	@ParameterMarker(uiOrder=4, name="Microwell Width", description="Width of the microwell in pixels.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	double width;
	
	@ParameterMarker(uiOrder=5, name="Microwell Height", description="Height of the microwell in pixels", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	double height;
	
	@ParameterMarker(uiOrder=6, name="Create Scatter Plots", description="Whether to create plots for each microwell showing the secretion to bead signal ratios of each pixel for each microwell.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean plot;
	
	@ParameterMarker(uiOrder=7, name="Enhanced BF Threshold", description="Threshold to be considered a bead.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	double bfThresh;
	
	@ParameterMarker(uiOrder=8, name="Scale Enhanced BF Microwell ROI", description="Amount to scale the Microwell ROI to match the enhance BF image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.5")
	double bfRoiScale;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Microwell Plots", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Plots of secretion to bead signal ratios for each microwell.", enabled=true)
	JEXData plots;
	
	@OutputMarker(uiOrder=2, name="Ratio Data", type=MarkerConstants.TYPE_FILE, flavor="", description="Table of secretion to bead signal ratios for each microwell.", enabled=true)
	JEXData ratios;
	
	@Override
	public int getMaxThreads()
	{
		return 1; // R doesn't like multiple threads.
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		imageData.getDataMap();
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		
		roiData.getDataMap();
		if(roiData == null || !roiData.getTypeName().getType().matches(JEXData.ROI))
		{
			return false;
		}
		
		boolean haveBF = true;
		if(bfData == null || !bfData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			haveBF = false;
		}
		
		boolean haveCell = true;
		if(cellData == null || !cellData.getTypeName().getType().matches(JEXData.ROI))
		{
			haveCell = false;
		}
		
		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,ROIPlus> roiMap = RoiReader.readObjectToRoiMap(roiData);
		TreeMap<DimensionMap,String> bfMap = null;
		if(haveBF)
		{
			bfMap = ImageReader.readObjectToImagePathTable(bfData);
		}
		TreeMap<DimensionMap,ROIPlus> cellMap = null;
		if(haveCell)
		{
			cellMap = RoiReader.readObjectToRoiMap(cellData);
		}
		TreeMap<DimensionMap,String> outputPlotMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,Double> outputDataMap = new TreeMap<DimensionMap,Double>();
		
		int count = 0, percentage = 0;
		DimensionMap beadMap;
		ROIPlus temp;
		DimTable table = imageData.getDimTable().getSubTable(new DimensionMap(colorDimName + "=" + secretionColor));
		for (DimensionMap secretionMap : table.getMapIterator())
		{
			beadMap = secretionMap.copyAndSet(colorDimName + "=" + beadColor);
			ROIPlus roi = roiMap.get(secretionMap);
			PointList pl = roi.getPointList();
			ImageProcessor sIm = new ImagePlus(imageMap.get(secretionMap)).getProcessor();
			ImageProcessor bIm = new ImagePlus(imageMap.get(beadMap)).getProcessor();
			ImageProcessor bfIm = null;
			if(haveBF)
			{
				bfIm = new ImagePlus(bfMap.get(secretionMap)).getProcessor();
			}
			for(IdPoint p : pl)
			{
				if(this.isCanceled())
				{
					return false;
				}
				DimensionMap dataMap = secretionMap.copyAndSet("Microwell=" + p.id);
				temp = ROIPlus.makeRoi(p.x, p.y, ROIPlus.ORIGIN_CENTER, ROIPlus.ROI_RECT, (int) width, (int) height);
				ROIPlus cellROI = null;
				if(haveCell)
				{
					cellROI = cellMap.get(secretionMap);
				}
				TreeMap<String,Object> result = MicrowellAnalyzer.getSecretionData(sIm, bIm, cellROI, p, temp, plot, bfIm, bfThresh, bfRoiScale);
				if(result != null)
				{
					if(plot)
					{
						outputPlotMap.put(dataMap, result.get("plot").toString());
					}
					for(String key : result.keySet())
					{
						Object val = result.get(key);
						if(val instanceof Number)
						{
							outputDataMap.put(dataMap.copyAndSet("Measurement=" + key), ((Number) result.get(key)).doubleValue());
						}						
					}
				}
				else
				{
					Logs.log("To few detectable beads to quantify for this microwell: " + p.id + " in image " + secretionMap.toString(), this);
				}
			}
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) table.mapCount())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		if(outputDataMap.size() == 0)
		{
			return false;
		}
		
		if(plot)
		{
			this.plots = ImageWriter.makeImageStackFromPaths("temp",outputPlotMap);
		}
		String tablePath = JEXTableWriter.writeTable("SecretionToBeadRatio", outputDataMap, "arff");
		this.ratios = FileWriter.makeFileObject("temp", null, tablePath);
		
		// Return status
		return true;
	}
}
