package function.plugin.plugins.quantification;

import java.awt.Shape;
import java.text.DecimalFormat;
import java.util.Collection;
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
import function.singleCellAnalysis.SingleCellUtility;
import ij.ImagePlus;
import ij.measure.Measurements;
import ij.process.ImageStatistics;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;
import image.roi.ROIPlus.PatternRoiIterator;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;

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
		name="Measure Maxima (v2)",
		menuPath="Quantification",
		visible=true,
		description="Measure intensity in area around points. Optionally limit which points are measured by supplying and additional region ROI (or patterned region Roi). Point patterns in region patterns is not implemented yet."
		)
public class MeasureMaxima_v2 extends JEXPlugin {

	public MeasureMaxima_v2()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;

	@InputMarker(uiOrder=2, name="Maxima ROI (Required)", type=MarkerConstants.TYPE_ROI, description="Maxima to be quantified.", optional=false)
	JEXData maximaRoiData;

	@InputMarker(uiOrder=3, name="Region ROI (optional)", type=MarkerConstants.TYPE_ROI, description="Region(s) in which maxima are to be quantified.", optional=true)
	JEXData regionRoiData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Measurement", description="The type(s) of measurement(s) to be performed.", ui=MarkerConstants.UI_DROPDOWN, choices={ "Mean", "Median", "Max", "Min" }, defaultChoice=0)
	String measurementType;

	@ParameterMarker(uiOrder=2, name="Type", description="The type of roi around each maxima to be created to quantify intensity information.", ui=MarkerConstants.UI_DROPDOWN, choices={ "Rectangle", "Ellipse", "Line", "Point" }, defaultChoice=0)
	String type;

	@ParameterMarker(uiOrder=3, name="ROI Width", description="Width of the roi to create (ignored for Point ROI).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="10")
	int roiWidth;

	@ParameterMarker(uiOrder=4, name="ROI Height", description="Height of the roi to create (ignored for Point ROI).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="10")
	int roiHeight;

	@ParameterMarker(uiOrder=5, name="ROI Origin", description="Where should the maxima be placed in the created roi?", ui=MarkerConstants.UI_DROPDOWN, choices={ ROIPlus.ORIGIN_CENTER, ROIPlus.ORIGIN_UPPERLEFT, ROIPlus.ORIGIN_UPPERRIGHT, ROIPlus.ORIGIN_LOWERRIGHT, ROIPlus.ORIGIN_LOWERLEFT }, defaultChoice=0)
	String roiOrigin;

	@ParameterMarker(uiOrder=6, name="Channel Dim Name (optional)", description="Name of the channel/color dimension of the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Channel")
	String colorDimName;

	@ParameterMarker(uiOrder=7, name="Offset", description="Value to offset all values by. Useful if the image does not have a zero background. (final value = measured value + offset)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	double nominal;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Data Table", type=MarkerConstants.TYPE_FILE, flavor="", description="The resultant data table", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	private Dim measurementDim = null;
	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry)
	{
		// Check the inputs
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		if(maximaRoiData == null || !maximaRoiData.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}		

		// Gather parameters
		int roiType = ROIPlus.ROI_RECT;
		if(this.type.equals("Ellipse"))
		{
			roiType = ROIPlus.ROI_ELLIPSE;
		}
		else if(this.type.equals("Line"))
		{
			roiType = ROIPlus.ROI_LINE;
		}
		else if(this.type.equals("Point"))
		{
			roiType = ROIPlus.ROI_POINT;
		}
		
		// Catch bad scenarios
		if(!this.colorDimName.equals("") && this.imageData.getDimTable().getDimWithName(this.colorDimName) == null)
		{
			JEXDialog.messageDialog("The channel dimension name specified (" + this.colorDimName + ") does not exist within the image. Aborting.", this);
			return false;
		}

		// Create useful Dim's and DimTable's
		DimTable imageTable = imageData.getDimTable();
		DimTable dataTable = imageTable.copy();
		dataTable.removeDimWithName(this.colorDimName);
		// dataTable.add(trackDim.copy());
		Dim colorDim = imageTable.getDimWithName(colorDimName);
		Vector<String> measurementNames = new Vector<>();
		if(colorDim == null)
		{
			measurementNames.add(this.normalizeName(this.measurementType));
		}
		else
		{
			measurementNames.addAll(this.normalizeNames(colorDim.dimValues));
		}
		measurementNames.add(SingleCellUtility.x);
		measurementNames.add(SingleCellUtility.y);
		this.measurementDim = new Dim("Measurement", measurementNames);
		dataTable.add(this.measurementDim.copy());

		// Get the input data
		TreeMap<DimensionMap,ROIPlus> rois = RoiReader.readObjectToRoiMap(maximaRoiData);
		TreeMap<DimensionMap,String> paths = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,ROIPlus> regions = new TreeMap<>();
		if(regionRoiData != null && regionRoiData.getTypeName().getType().equals(JEXData.ROI))
		{
			regions = RoiReader.readObjectToRoiMap(regionRoiData);
		}

		// Get the maximum number of points in a roi and add it to the Data Table
		int max = -1;
		int min = Integer.MAX_VALUE;
		int totalPoints = 0;
		for (Entry<DimensionMap,ROIPlus> e : rois.entrySet())
		{
			for (IdPoint point : e.getValue().getPointList())
			{
				min = Math.min(point.id, min);
				max = Math.max(point.id, max);
			}
			totalPoints = totalPoints + e.getValue().getPointList().size();
		}
		Dim pDim = new Dim("Point", min, max);
		dataTable.add(pDim);

		// Get the maximum number of regions in the region roi(s) and add it to the Data Table
		if(regions.size() > 0)
		{
			max = -1;
			min = Integer.MAX_VALUE;
			for (Entry<DimensionMap,ROIPlus> e : regions.entrySet())
			{
				for (IdPoint point : e.getValue().getPattern())
				{
					min = Math.min(point.id, min);
					max = Math.max(point.id, max);
				}
			}
			Dim rDim = new Dim("Region", min, max);
			dataTable.add(rDim);
		}

		// Make a template roi
		ROIPlus templateRoi = ROIPlus.makeRoi(0, 0, roiOrigin, roiType, roiWidth, roiHeight);
		if(templateRoi == null || templateRoi.getPointList().size() == 0)
		{
			Logs.log("Couldn't get the points to make the template roi!", 9, this);
			JEXStatics.statusBar.setStatusText("Couldn't get the points to make the roi!");
			return false;
		}

		// Write the beginning of the csv file
		JEXTableWriter writer = new JEXTableWriter(output.getTypeName().getName(), "arff");
		writer.writeNumericTableHeader(dataTable);
		String fullPath = writer.getPath();

		// Initialize loop variables
		ROIPlus maximaRoi = null;
		ROIPlus regionRoi = null;
		PatternRoiIterator regionIterator = null;
		ImagePlus im;
		DecimalFormat formatD = new DecimalFormat("##0.000");
		int count = 0;
		int total = imageTable.mapCount();
		int percentage = 0;

		try
		{
			JEXStatics.statusBar.setStatusText("Measuring " + totalPoints + " points in " + total + " images.");
			Logs.log("Measuring " + totalPoints + " points in " + total + " images.", 0, this);

			for (DimensionMap imMap : imageTable.getMapIterator())
			{
				if(this.isCanceled())
				{
					return false;
				}
				im = new ImagePlus(paths.get(imMap));
				

				// In case the maxima roi has a color dim, cycle through the color dimension to grab the 
				// first available maxima roi. Do the same for the region roi. This maxima roi and region
				// roi will be applied to all colors.
				maximaRoi = null;
				regionRoi = null;
				regionIterator = null;
				maximaRoi = rois.get(imMap);
				regionRoi = regions.get(imMap);
				if((maximaRoi == null || maximaRoi.getPointList().size() == 0) && !this.colorDimName.equals(""))
				{
					for (String color : imageTable.getDimWithName(this.colorDimName).dimValues)
					{
						DimensionMap newMap = imMap.copy();
						newMap.put(this.colorDimName, color);
						maximaRoi = rois.get(newMap);
						if(maximaRoi != null)
						{
							Logs.log("Making a best guess as to which color dim value to use to choose a maxima roi. Using " + this.colorDimName + " = " + color, this);
							break;
						}
					}
				}
				if((regionRoi == null || regionRoi.getPointList().size() == 0) && !this.colorDimName.equals(""))
				{
					for (String color : imageTable.getDimWithName(this.colorDimName).dimValues)
					{
						DimensionMap newMap = imMap.copy();
						newMap.put(this.colorDimName, color);
						regionRoi = regions.get(newMap);
						if(regionRoi != null)
						{
							Logs.log("Making a best guess as to which color dim value to use to choose a region roi. Using " + this.colorDimName + " = " + color, this);
							break;
						}
					}
				}
				if(regionRoi != null && regionRoi.getPointList().size() > 0)
				{
					regionIterator = regionRoi.patternRoiIterator();
				}

				// For each region
				if(regionIterator != null)
				{
					while(regionIterator.hasNext())
					{
						ROIPlus region = regionIterator.next();
						if(region.getPointList().size() > 0)
						{
							int regionId = regionIterator.currentPatternPoint().id;
							
							// subselect the points of maximaRoi and quantify
							Shape shape = region.getShape();
							PointList subset = new PointList();
							for(IdPoint p : maximaRoi.getPointList())
							{
								if(shape.contains(p.x, p.y))
								{
									subset.add(p.copy());
								}
							}
							ROIPlus maximaSubset = new ROIPlus(subset, ROIPlus.ROI_POINT);
							
							this.quantifyPoints(im, maximaSubset, templateRoi, formatD, imMap, writer, regionId);
						}
					}
				}
				else if(regionRoiData == null)
				{
					// Just do all the points
					this.quantifyPoints(im, maximaRoi, templateRoi, formatD, imMap, writer, -1);
				}

				im.flush();
				im = null;
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / (double) total));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
			writer.close();

			this.output = FileWriter.makeFileObject("dummy", null, fullPath);

			// Return status
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			if(writer != null)
			{
				writer.close();
			}
			return false;
		}
	}
	
	private void quantifyPoints(ImagePlus im, ROIPlus maximaRoi, ROIPlus templateRoi, DecimalFormat formatD, DimensionMap imMap, JEXTableWriter writer, int regionId)
	{
		// Create a copy of the templateRoi and move it to the correct location for measurement.
		IdPoint pointToMeasure = new IdPoint(-1, -1, 0);

		for (IdPoint p : maximaRoi.getPointList())
		{
			// Create the roi at the desired location
			// This is centered on 0,0. Just need to translate to this new location
			ROIPlus roip = templateRoi.copy(); 
			roip.getPointList().translate(p.x, p.y);

			pointToMeasure = p.copy();

			im.setRoi(roip.getRoi());
			ImageStatistics stats = im.getStatistics(Measurements.MEAN + Measurements.MEDIAN + Measurements.MIN_MAX);
			if(stats != null)
			{
				String dataString = formatD.format(stats.mean);
				if(dataString != null && !dataString.equals(""))
				{
					// DimensionMap for storing data (Color will be
					// detected and removed in writeData function, we
					// need color temporarily to convert it to a
					// measurement e.g. color 0 = measurement Blue
					DimensionMap mapToSave = imMap.copy();
					mapToSave.put("Point", "" + pointToMeasure.id);
					if(regionId >=0)
					{
						mapToSave.put("Region", "" + regionId);
					}

					// Write the data to the ongoing file
					this.writeData(writer, mapToSave, stats, pointToMeasure, nominal, measurementType);
				}
			}
		}
	}
	
	private String normalizeName(String name)
	{
		name = name.replaceAll("\\s+","");
		name = name.replace('.','_');
		return(name);
	}
	
	private Collection<String> normalizeNames(Collection<String> names)
	{
		Vector<String> newNames = new Vector<>();
		for(String name : names)
		{
			newNames.add(this.normalizeName(name));
		}
		return(newNames);
	}

	private void writeData(JEXTableWriter writer, DimensionMap mapToSave, ImageStatistics stats, IdPoint p, double nominal, String measurementType)
	{
		// DimensionMap mapToSave = map.copy();
		String color = mapToSave.remove(this.colorDimName);
		
		// Normalize the string for good behavior in tables and data analysis software.
		if(color != null)
		{
			color = this.normalizeName(color);
		}
		
		double measurement = stats.mean;
		if(measurementType.equals("Median"))
		{
			measurement = stats.median;
		}
		else if(measurementType.equals("Max"))
		{
			measurement = stats.max;
		}
		else if(measurementType.equals("Min"))
		{
			measurement = stats.min;
		}

		// Write the data to the ongoing file
		if(color == null)
		{
			mapToSave.put("Measurement", SingleCellUtility.x);
			writer.writeData(mapToSave, new Double(p.x));
			mapToSave.put("Measurement", SingleCellUtility.y);
			writer.writeData(mapToSave, new Double(p.y));
			mapToSave.put("Measurement", this.measurementType);
			writer.writeData(mapToSave, new Double(measurement - nominal));
		}
		else
		{
			if(color.equals(this.measurementDim.dimValues.get(0)))
			{
				mapToSave.put("Measurement", SingleCellUtility.x);
				writer.writeData(mapToSave, new Double(p.x));
				mapToSave.put("Measurement", SingleCellUtility.y);
				writer.writeData(mapToSave, new Double(p.y));
				mapToSave.put("Measurement", color);
				writer.writeData(mapToSave, new Double(measurement - nominal));
			}
			else
			{
				mapToSave.put("Measurement", color);
				writer.writeData(mapToSave, new Double(measurement - nominal));
			}
		}
	}
}
