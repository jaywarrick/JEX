package function.plugin.plugins.imageTools;

import java.awt.Rectangle;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.RoiWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import image.roi.ROIPlus;
import jex.statics.JEXStatics;
import logs.Logs;
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
		name="Separate Image Tiles",
		menuPath="Image Tools",
		visible=true,
		description="Stitched images can be separated into tiles and trimmed to remove overlap for treating independently."
		)
public class SeparateImageTiles extends JEXPlugin {
	
	public SeparateImageTiles()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="Overlap", description="The overlap of image tiles expressed as a percentage of the original size of an individual tile.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="10.0")
	double overlap;
	
	@ParameterMarker(uiOrder=2, name="Rows", description="How many rows of tiles are there?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2")
	int rows;
	
	@ParameterMarker(uiOrder=3, name="Cols", description="How many cols of tiles are there?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2")
	int cols;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Image Tiles", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData imageOutput;
	
	@OutputMarker(uiOrder=2, name="Crop Rois", type=MarkerConstants.TYPE_ROI, flavor="", description="The rois used for cropping the tiles from the original image.", enabled=true)
	JEXData roiOutput;
	
	@Override
	public int getMaxThreads()
	{
		return 10;
	}
	
	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		
		this.overlap = this.overlap/100.0; // Turn percent into fraction.
		
		// Run the function
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		
		int count = 0;
		int total = images.size();
		TreeMap<DimensionMap,ROIPlus> cropRois = null;
		for (DimensionMap map : images.keySet())
		{
			
			if(this.isCanceled())
			{
				return false;
			}
			
			String path = images.get(map);
			// File f = new File(path);
			
			// get the image
			ImagePlus im = new ImagePlus(path);
			
			cropRois = getCropRois(im.getWidth(), im.getHeight(), this.rows, this.cols, this.overlap);
			TreeMap<DimensionMap,String> toSave = this.getCropImages(cropRois, map, im.getProcessor());
			outputMap.putAll(toSave);
			
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			count++;
			
			// Status bar
			int percentage = (int) (100 * ((double) count / (double) images.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		// Set the outputs
		imageOutput = ImageWriter.makeImageStackFromPaths("temp", outputMap);
		imageOutput.setDataObjectInfo("Stack binned using binning function");
		roiOutput = RoiWriter.makeRoiObject("temp", cropRois);
		
		// Return status
		return true;
	}
	
	public TreeMap<DimensionMap,ROIPlus> getCropRois(int w, int h, int rs, int cs, double overlap)
	{
		Dim rows = new Dim("ImRow", 0, rs-1);
		Dim cols = new Dim("ImCol", 0, cs-1);
		DimTable dt = new DimTable();
		dt.add(rows);
		dt.add(cols);
		double w0, h0;
		w0 = w/(cs - (cs - 1)*overlap);
		h0 = h/(rs - (rs - 1)*overlap);
		double overlapX = w0*overlap;
		double overlapY = h0*overlap;
		w0 = w0 - 2*overlapX;
		h0 = h0 - 2*overlapY;
		
		TreeMap<DimensionMap,ROIPlus> ret = new TreeMap<>();
		for(DimensionMap map : dt.getMapIterator())
		{
			int r = Integer.parseInt(map.get("ImRow"));
			int c = Integer.parseInt(map.get("ImCol"));
			double ulX = overlapX + c*(overlapX + w0);
			double ulY = overlapY + r*(overlapY + h0);
			ret.put(map, new ROIPlus(new Rectangle((int) Math.round(ulX), (int) Math.round(ulY), (int) Math.round(w0), (int) Math.round(h0))));
		}
		return ret;
	}
	
	public TreeMap<DimensionMap,String> getCropImages(TreeMap<DimensionMap,ROIPlus> cropRois, DimensionMap imageMap, ImageProcessor ip)
	{
		TreeMap<DimensionMap,String> ret = new TreeMap<>();
		if(imageMap == null)
		{
			imageMap = new DimensionMap();
		}
		for(Entry<DimensionMap,ROIPlus> e : cropRois.entrySet())
		{
			DimensionMap toSave = imageMap.copy();
			toSave.putAll(e.getKey());
			ip.setRoi(e.getValue().getRoi());
			ImageProcessor cropped = ip.crop();
			String path = JEXWriter.saveImage(cropped);
			ret.put(toSave, path);
		}
		return ret;
	}
	
	public TreeMap<DimensionMap,ImageProcessor> getCropImageProcessors(TreeMap<DimensionMap,ROIPlus> cropRois, DimensionMap imageMap, ImageProcessor ip)
	{
		TreeMap<DimensionMap,ImageProcessor> ret = new TreeMap<>();
		if(imageMap == null)
		{
			imageMap = new DimensionMap();
		}
		for(Entry<DimensionMap,ROIPlus> e : cropRois.entrySet())
		{
			DimensionMap toSave = imageMap.copy();
			toSave.putAll(e.getKey());
			ip.setRoi(e.getValue().getRoi());
			ImageProcessor cropped = ip.crop();
			ret.put(toSave, cropped);
		}
		return ret;
	}
}
