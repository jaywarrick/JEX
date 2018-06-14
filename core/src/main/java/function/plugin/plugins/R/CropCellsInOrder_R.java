package function.plugin.plugins.R;

import java.awt.Point;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.RoiWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import function.plugin.plugins.dataEditing.SortDecimalDimensionValues;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import logs.Logs;
import rtools.R;
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
		name="Crop Cells In Order (R)",
		menuPath="Single Cell",
		visible=true,
		description="Function that allows you to crop cells from images in user specified order based upon cellular data contained within a CSV table."
		)
public class CropCellsInOrder_R extends JEXPlugin {

	public CropCellsInOrder_R()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be quantified.", optional=false)
	JEXData imageData;

	@InputMarker(uiOrder=2, name="Maxima", type=MarkerConstants.TYPE_ROI, description="ROI of microwell locations.", optional=false)
	JEXData roiData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="CSV Table", description="CSV Table with cell data containing column names and values that match entry 'ds' (dataset), 'x', and 'y' values; image and roi dimension names and values; and maxima 'Id' values for finding data for each cell).", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	String tableFilePath;

	@ParameterMarker(uiOrder=2, name="R Filter Statement", description="Statement to be substituted into 'x <- x[<Filter Statement>]'", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String filterStatement;

	@ParameterMarker(uiOrder=3, name="Data Column To Order By", description="Name of the column in the table by which to order the cropped cells.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Id")
	String dataCol;

	@ParameterMarker(uiOrder=4, name="Crop Width", description="Width of the cropped region surrounding point.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="50")
	int width;

	@ParameterMarker(uiOrder=5, name="Crop Height", description="Height of the cropped region surrounding point.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="50")
	int height;


	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Ordered Cells", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Plots of secretion to bead signal ratios for each microwell.", enabled=true)
	JEXData outputCells;

	@OutputMarker(uiOrder=2, name="Ordered Maxima", type=MarkerConstants.TYPE_FILE, flavor="", description="Table of secretion to bead signal ratios for each microwell.", enabled=true)
	JEXData outputMaxima;

	@Override
	public int getMaxThreads()
	{
		return 1; // R doesn't like multiple threads.
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{

		// Collect the inputs
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			JEXDialog.messageDialog("An image must be specified for this function. Aborting.", this);
			return false;
		}

		// Collect the inputs
		if(roiData == null || !roiData.getTypeName().getType().matches(JEXData.ROI))
		{
			JEXDialog.messageDialog("A maxima ROI must be specified for this function. Aborting.", this);
			return false;
		}

		// Run the function
		TreeMap<DimensionMap,String> imageList = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,ROIPlus> pointROIMap = RoiReader.readObjectToRoiMap(roiData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,ROIPlus> outputRoiMap = new TreeMap<DimensionMap,ROIPlus>();

		ROIPlus pointROI;
		ImagePlus im;
		FloatProcessor imp;
		int count = 0;
		String actualPath = "";
		int total = imageList.size();
		DimTable dt = imageData.getDimTable();

		ROIPlus prototype;
		PointList pl = new PointList();
		pl.add(new Point(-width / 2, -height / 2));
		pl.add(new Point(-width / 2 + width, -height / 2 + height));
		prototype = new ROIPlus(pl.copy(), ROIPlus.ROI_RECT);
		FloatProcessor cell;
		
		this.loadTableIfNeeded();
		if(!this.tableHasNecessaryColumns(dt))
		{
			JEXDialog.messageDialog("Not all of the necessary column names were found in the data table to identify cell data. Aborting.", this);
			return false;
		}
		if(!this.filterTableToEntry(optionalEntry))
		{
			JEXDialog.messageDialog("No data in the table for this dataset and entry so skipping by returning false.", this);
			return false;
		}
		for (DimensionMap map : dt.getMapIterator())
		{
			if(this.isCanceled())
			{
				return false;
			}
			
			if(!this.filterStatement.equals("") && !this.filterTableIfNeeded())
			{
				// Then there is no cell or data to get so skip.
				continue;
			}
			
			im = new ImagePlus(imageList.get(map));
			imp = (FloatProcessor) im.getProcessor().convertToFloat();
			int bitDepth = im.getBitDepth();
			pointROI = pointROIMap.get(map);
			if(pointROI != null)
			{
				boolean isLine = pointROI.isLine();
				if(isLine || pointROI.type != ROIPlus.ROI_POINT)
				{
					return false;
				}
				pl = pointROI.getPointList();
				for (IdPoint p : pl)
				{
					if(this.isCanceled())
					{
						return false;
					}
					
					Double d = this.getCellDataValue(map, p.id);
					if(d == null)
					{
						continue;
					}
					
					ROIPlus copy = prototype.copy();
					copy.pointList.translate(p.x, p.y);
					imp.setRoi(copy.getRoi());
					cell = (FloatProcessor) imp.crop().convertToFloat();
					actualPath = this.saveImage(cell, bitDepth);
					DimensionMap newMap = map.copy();
					newMap.put(this.dataCol, "" + d);
					outputImageMap.put(newMap, actualPath);
					PointList toSave = new PointList();
					toSave.add(p.copy());
					toSave.translate(-p.x, -p.y);
					toSave.translate(width / 2, height / 2);
					outputRoiMap.put(newMap, new ROIPlus(toSave, ROIPlus.ROI_POINT));
					Logs.log("Outputing cell: " + p.id, this);
				}
			}
			count = count + 1;
			JEXStatics.statusBar.setProgressPercentage(count * 100 / total);
		}

		// Save the output cells in the correct order
		this.outputCells = ImageWriter.makeImageStackFromPaths("temp", outputImageMap);
		DimTable dtToSave = SortDecimalDimensionValues.getDecimalOrderedDimTable(this.outputCells.getDimTable(), new String[] {this.dataCol});
		this.outputCells.setDimTable(dtToSave);
		
		// Save the maxima.
		this.outputMaxima = RoiWriter.makeRoiObject("temp", outputRoiMap);

		// Return status
		return true;
	}
	
	private boolean loadTableIfNeeded()
	{
		R.load("data.table");
		R.load("bit64");
		if(R.getExpressionResultAsBoolean("exists('table.path')"))
		{
			if(R.getExpressionResultAsBoolean("table.path == " + R.quotedPath(this.tableFilePath)))
			{
				return(R.getExpressionResultAsBoolean("exists('x') && !is.null(x) && is.data.table(x) && nrow(x) > 0"));
			}
		}
		R.eval("x <- fread(file=" + R.quotedPath(this.tableFilePath) + ", header=T)");
		return(R.getExpressionResultAsBoolean("exists('x') && !is.null(x) && is.data.table(x) && nrow(x) > 0"));
	}
	
	private boolean tableHasNecessaryColumns(DimTable dt)
	{
		for(String d : dt.getDimensionNames())
		{
			if(d.equals("Channel"))
			{
				// Skip
				continue;
			}
			if(!R.getExpressionResultAsBoolean(R.sQuote(d) + " %in% names(x)"))
			{
				return false;
			}
		}
		if(!R.getExpressionResultAsBoolean("'ds' %in% names(x)"))
		{
			return false;
		}
		if(!R.getExpressionResultAsBoolean("'x' %in% names(x)"))
		{
			return false;
		}
		if(!R.getExpressionResultAsBoolean("'y' %in% names(x)"))
		{
			return false;
		}
		if(!R.getExpressionResultAsBoolean(R.sQuote(this.dataCol) + " %in% names(x)"))
		{
			return false;
		}
		return true;
	}
	
	private boolean filterTableToEntry(JEXEntry e)
	{
		if(R.getExpressionResultAsBoolean("!all(c('ds','x','y') %in% names(x1))"))
		{
			return false;
		}
		R.eval("x1 <- x[ds==" + R.sQuote(e.getEntryExperiment()) + " & x==" + R.sQuote("" + e.getTrayX()) + " & y==" + R.sQuote("" + e.getTrayY()) + "]");
		return(R.getExpressionResultAsBoolean("exists('x1') && !is.null(x1) && is.data.table(x1) && nrow(x1) > 0"));
	}
	
	private boolean filterTableIfNeeded()
	{
		if(this.filterStatement != null && !this.filterStatement.equals(""))
		{
			R.eval("x1 <- x1[" + this.filterStatement + "]");
		}
		return(R.getExpressionResultAsBoolean("exists('x1') && !is.null(x1) && is.data.table(x1) && nrow(x1) > 0"));
	}
	
	private Double getCellDataValue(DimensionMap map, int id)
	{
		R.eval("val <- x1[" + getFilterStringFromMap(map) + " & Id==" + id + "][[" + R.sQuote(this.dataCol) + "]]");
		if(R.getExpressionResultAsBoolean("exists('val') && !is.null(val) && is.numeric(val)"))
		{
			return R.getExpressionResultAsDouble("val"); // This checks for length=0 values and length > 1 values print a warning message.
		}
		return null;
	}
	
	private String getFilterStringFromMap(DimensionMap map)
	{
		StringBuilder sb = new StringBuilder();
		int count = 1;
		for(Entry<String,String> e : map.entrySet())
		{
			if(!e.getKey().equals("Channel"))
			{
				sb.append(e.getKey() + "==" + e.getValue());
				if(count != map.size())
				{
					sb.append(" & ");
				}
			}
			count = count + 1;
		}
		return sb.toString();
	}

	private String saveImage(FloatProcessor imp, int bitDepth)
	{
		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
		String imPath = JEXWriter.saveImage(toSave);
		return imPath;
	}
}
