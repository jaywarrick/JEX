package function.plugin.plugins.imageTools;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import image.roi.IdPoint;
import image.roi.PointList;

import java.awt.Rectangle;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.StatisticsUtility;

import org.scijava.plugin.Plugin;

import tables.Dim;
import tables.DimensionMap;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ValueWriter;

import function.imageUtility.TurboReg_;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;


/**
 * The same as JEX_ImageTools_FindImageAlignment, but in new Plugin version
 * 
 * @author erwinberthier, updated to new Plugin version by Mengcheng
 *
 */
@Plugin(
		type = JEXPlugin.class,
		name="Auto-Find Image Alignments",
		menuPath="Image Tools > Stitching", // TODO
		visible=true,
		description="Function that allows you to stitch an image ARRAY into " +
				"a single image using two GUESSES for image alignment objects. " +
				"Turbo Reg is used to refine the guess."
		)
public class AutoFindImageAlignments extends JEXPlugin {

	public static final int LtoR = 0, TtoB = 1;
	
	public AutoFindImageAlignments()
	{}

	
	/////////// Define Inputs ///////////
		
	@InputMarker(uiOrder=1, name="Images", type=MarkerConstants.TYPE_IMAGE, description="Images to be aligned.", optional=false)
	JEXData imageData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="ImRow Dim Name", description="Name of the dimension that indicates the rows", ui=MarkerConstants.UI_TEXTFIELD, defaultText="ImRow")
	String imRow;
	
	@ParameterMarker(uiOrder=2, name="ImCol Dim Name", description="Name of the dimension that indicates the cols", ui=MarkerConstants.UI_TEXTFIELD, defaultText="ImCol")
	String imCol;
	
	@ParameterMarker(uiOrder=3, name="Color Dim Name", description="Name of the dimension that indicates the color (leave black if not applicable)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Color")
	String colorDim;
	
	@ParameterMarker(uiOrder=4, name="Color Dim Value", description="Value of the color dimension that should be used to guide stitching", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	String colorVal;
	
	@ParameterMarker(uiOrder=5, name="Horizontal Overlap %", description="Approximate percentage of image width overlapping between columns.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="15")
	double hOverPercent;
	
	@ParameterMarker(uiOrder=6, name="Vertical Overlap %", description="Approximate percentage of image height overlapping between rows.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="15")
	double vOverPercent;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Hor. Alignment", type=MarkerConstants.TYPE_VALUE, flavor="", description="The horizontal alignment data", enabled=true)
	JEXData horData;
	
	@OutputMarker(uiOrder=2, name="Ver. Alignment", type=MarkerConstants.TYPE_VALUE, flavor="", description="The vertical alignment data", enabled=true)
	JEXData verData;
	
	@Override
	public boolean run(JEXEntry optionalEntry) {
		
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		
		// Run the function
		// Get the Partial DimTable and iterate through it and stitch.
		DimensionMap firstMap = imageData.getDimTable().getDimensionMaps().get(0);
		firstMap.remove(imRow);
		firstMap.remove(imCol);
		if(!colorDim.equals(""))
		{
			firstMap.put(colorDim, colorVal);
		}
		Dim rows = imageData.getDimTable().getDimWithName(imRow);
		Dim cols = imageData.getDimTable().getDimWithName(imCol);
		PointList horMoves = new PointList();
		PointList verMoves = new PointList();
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(imageData);
		TurboReg_ reg = new TurboReg_();
		int width = 0;
		int height = 0;
		
		Integer vOver = null;
		Integer hOver = null;
		
		int count = 0, total = imageData.getDimTable().getSubTable(firstMap.copy()).mapCount();
		for (DimensionMap targetMap : imageData.getDimTable().getMapIterator(firstMap.copy()))
		{
			if(this.isCanceled())
			{
				return false;
			}
			// Looping through ImRow and ImCol for the color of interest
			// Use TurboReg to assess
			if(!targetMap.get(imRow).equals(rows.max()))
			{
				ImagePlus targetIm = new ImagePlus(images.get(targetMap));
				DimensionMap sourceMap = targetMap.copy();
				int nextRow = Integer.parseInt(targetMap.get(imRow)) + 1;
				sourceMap.put(imRow, "" + nextRow);
				ImagePlus sourceIm = new ImagePlus(images.get(sourceMap));
				width = targetIm.getWidth();
				height = targetIm.getHeight();
				
				// calculate vOver
				if(vOver == null)
				{
					vOver = (int) (vOverPercent * height / 100.0);
				}
				
				Rectangle targetRect = new Rectangle(0, height - vOver, width - 1, vOver - 1); // Bottom of target image
				Rectangle sourceRect = new Rectangle(0, 0, width - 1, vOver - 1); // Top of source image
				ImageProcessor targetImp = targetIm.getProcessor();
				targetImp.setRoi(targetRect);
				targetIm.setProcessor(targetImp.crop());
				ImageProcessor sourceImp = sourceIm.getProcessor();
				sourceImp.setRoi(sourceRect);
				sourceIm.setProcessor(sourceImp.crop());
				// int[] targetCrop = new int[] { 0, 0, width - 1, height - 1 }; // Bottom of target image
				// int[] sourceCrop = new int[] { 0, 0, width - 1, height - 1 }; // Top of source image
				int[] targetCrop = new int[] { 0, 0, targetRect.width, targetRect.height };
				int[] sourceCrop = new int[] { 0, 0, sourceRect.width, sourceRect.height };
				reg.alignImages(sourceIm, sourceCrop, targetIm, targetCrop, TurboReg_.TRANSLATION, false);
				double dx = reg.getSourcePoints()[0][0];
				double dy = reg.getSourcePoints()[0][1];
				verMoves.add((int) dx, (int) dy);
				Logs.log(targetMap.toString() + " Vertical Align Result: " + (int) dx + "," + (int) dy, 0, this);
			}
			
			if(this.isCanceled())
			{
				return false;
			}
			if(!targetMap.get(imCol).equals(cols.max()))
			{
				ImagePlus targetIm = new ImagePlus(images.get(targetMap));
				DimensionMap sourceMap = targetMap.copy();
				int nextCol = Integer.parseInt(targetMap.get(imCol)) + 1;
				sourceMap.put(imCol, "" + nextCol);
				ImagePlus sourceIm = new ImagePlus(images.get(sourceMap));
				width = targetIm.getWidth();
				height = targetIm.getHeight();
				
				// calculate hOver
				if(hOver == null)
				{
					hOver = (int)(hOverPercent * width / 100.0);
				}
				
				Rectangle targetRect = new Rectangle(width - hOver, 0, hOver - 1, height - 1); // RHS of target image
				Rectangle sourceRect = new Rectangle(0, 0, hOver - 1, height - 1); // LHS of source image
				ImageProcessor targetImp = targetIm.getProcessor();
				targetImp.setRoi(targetRect);
				targetIm.setProcessor(targetImp.crop());
				ImageProcessor sourceImp = sourceIm.getProcessor();
				sourceImp.setRoi(sourceRect);
				sourceIm.setProcessor(sourceImp.crop());
				// int[] targetCrop = new int[] { 0, 0, width - 1, height - 1 }; // Bottom of target image
				// int[] sourceCrop = new int[] { 0, 0, width - 1, height - 1 }; // Top of source image
				int[] targetCrop = new int[] { 0, 0, targetRect.width, targetRect.height };
				int[] sourceCrop = new int[] { 0, 0, sourceRect.width, sourceRect.height };
				
				reg.alignImages(sourceIm, sourceCrop, targetIm, targetCrop, TurboReg_.TRANSLATION, false);
				double dx = reg.getSourcePoints()[0][0];
				double dy = reg.getSourcePoints()[0][1];
				horMoves.add((int) dx, (int) dy);
				Logs.log(targetMap.toString() + " Horizontal Align Result: " + (int) dx + "," + (int) dy, 0, this);
			}
			// // Update the display
			count = count + 1;
			int percentage = (int) (100 * ((double) (count) / ((double) total)));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		// Find the median moves
		Vector<Double> dxs = new Vector<Double>();
		Vector<Double> dys = new Vector<Double>();
		for (IdPoint p : horMoves)
		{
			dxs.add((double) p.x);
			dys.add((double) p.y);
		}
		int horDx = width - hOver - StatisticsUtility.median(dxs).intValue();
		int horDy = StatisticsUtility.median(dys).intValue();
		
		dxs = new Vector<Double>();
		dys = new Vector<Double>();
		for (IdPoint p : verMoves)
		{
			dxs.add((double) p.x);
			dys.add((double) p.y);
		}
		int verDx = StatisticsUtility.median(dxs).intValue();
		int verDy = height - vOver - StatisticsUtility.median(dys).intValue();
		
		String horMove = horDx + "," + horDy;
		String verMove = verDx + "," + verDy;
		
		this.horData = ValueWriter.makeValueObject(this.horData.name, horMove);
		this.verData = ValueWriter.makeValueObject(this.verData.name, verMove);
		
		// Return status
		return true;
	}


}
