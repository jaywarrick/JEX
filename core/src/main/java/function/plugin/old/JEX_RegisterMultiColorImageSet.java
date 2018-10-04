package function.plugin.old;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.RoiWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import function.imageUtility.TurboReg_;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import image.roi.ROIPlus;
import jex.statics.JEXStatics;
import jex.utilities.ROIUtility;
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
public class JEX_RegisterMultiColorImageSet extends JEXCrunchable {
	
	// ----------------------------------------------------
	// --------- INFORMATION ABOUT THE FUNCTION -----------
	// ----------------------------------------------------
	
	/**
	 * Returns the name of the function
	 * 
	 * @return Name string
	 */
	@Override
	public String getName()
	{
		String result = "Register a Multi-color Image Set";
		return result;
	}
	
	/**
	 * This method returns a string explaining what this method does This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	@Override
	public String getInfo()
	{
		String result = "Register a multi-color image set based on one color channel so ecah color stays perfectly registered with the others.";
		return result;
	}
	
	/**
	 * This method defines in which group of function this function will be shown in... Toolboxes (choose one, caps matter): Visualization, Image processing, Custom Cell Analysis, Cell tracking, Image tools Stack processing, Data Importing, Custom
	 * image analysis, Matlab/Octave
	 * 
	 */
	@Override
	public String getToolbox()
	{
		String toolbox = "Image processing";
		return toolbox;
	}
	
	/**
	 * This method defines if the function appears in the list in JEX It should be set to true expect if you have good reason for it
	 * 
	 * @return true if function shows in JEX
	 */
	@Override
	public boolean showInList()
	{
		return true;
	}
	
	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
	@Override
	public boolean allowMultithreading()
	{
		return true;
	}
	
	// ----------------------------------------------------
	// --------- INPUT OUTPUT DEFINITIONS -----------------
	// ----------------------------------------------------
	
	/**
	 * Return the array of input names
	 * 
	 * @return array of input names
	 */
	@Override
	public TypeName[] getInputNames()
	{
		TypeName[] inputNames = new TypeName[2];
		inputNames[0] = new TypeName(IMAGE, "Multicolor Image Set");
		inputNames[1] = new TypeName(ROI, "Alignment Region ROI (rect, optional)");
		return inputNames;
	}
	
	/**
	 * Return the number of outputs returned by this function
	 * 
	 * @return number of outputs
	 */
	@Override
	public TypeName[] getOutputs()
	{
		this.defaultOutputNames = new TypeName[2];
		this.defaultOutputNames[0] = new TypeName(IMAGE, "Registered Images");
		this.defaultOutputNames[1] = new TypeName(ROI, "Crop ROI");
		
		if(this.outputNames == null)
		{
			return this.defaultOutputNames;
		}
		return this.outputNames;
	}
	
	/**
	 * Returns a list of parameters necessary for this function to run... Every parameter is defined as a line in a form that provides the ability to set how it will be displayed to the user and what options are available to choose from The simplest
	 * FormLine can be written as: FormLine p = new FormLine(parameterName); This will provide a text field for the user to input the value of the parameter named parameterName More complex displaying options can be set by consulting the FormLine API
	 * 
	 * @return list of FormLine to create a parameter panel
	 */
	@Override
	public ParameterSet requiredParameters()
	{
		Parameter pa1 = new Parameter("Color Dim Name", "Name of the color dimension", "Color");
		Parameter pa2 = new Parameter("Reference Color", "Name or number of the color to use for determining registration", "0");
		Parameter pa3 = new Parameter("Remove Black Borders?", "Should the black region surrounding the image be cropped?", Parameter.CHECKBOX, true);
		Parameter pa4 = new Parameter("Time Dim Name", "Name of the time dimension.", "Time");
		Parameter pa5 = new Parameter("Align To First Timepoint?", "Each image timepoint will be aligned to the first if set to true. Otherwise, time t aligns to t-1.", Parameter.CHECKBOX, true);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(pa1);
		parameterArray.addParameter(pa2);
		parameterArray.addParameter(pa3);
		parameterArray.addParameter(pa4);
		parameterArray.addParameter(pa5);
		return parameterArray;
	}
	
	// ----------------------------------------------------
	// --------- ERROR CHECKING METHODS -------------------
	// ----------------------------------------------------
	
	/**
	 * Returns the status of the input validity checking It is HIGHLY recommended to implement input checking however this can be over-rided by returning false If over-ridden ANY batch function using this function will not be able perform error
	 * checking...
	 * 
	 * @return true if input checking is on
	 */
	@Override
	public boolean isInputValidityCheckingEnabled()
	{
		return true;
	}
	
	// ----------------------------------------------------
	// --------- THE ACTUAL MEAT OF THIS FUNCTION ---------
	// ----------------------------------------------------
	
	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		JEXData data = inputs.get("Multicolor Image Set");
		if(data == null || !data.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		
		JEXData roiData = inputs.get("Alignment Region ROI (rect, optional)");
		TreeMap<DimensionMap,ROIPlus> roiMap = new TreeMap<DimensionMap,ROIPlus>();
		if(roiData != null && roiData.getDataObjectType().equals(JEXData.ROI))
		{
			roiMap = RoiReader.readObjectToRoiMap(roiData);
		}
		
		// //// Get params
		String colorDimName = this.parameters.getValueOfParameter("Color Dim Name");
		String timeDimName = this.parameters.getValueOfParameter("Time Dim Name");
		Dim colorDim = data.getDimTable().getDimWithName(colorDimName);
		int colorDimSize = 1;
		if(colorDim != null)
		{
			colorDimSize = colorDim.size();
		}
		String referenceColor = this.parameters.getValueOfParameter("Reference Color");
		boolean cropResults = Boolean.parseBoolean(this.parameters.getValueOfParameter("Remove Black Borders?"));
		boolean firstTimer = Boolean.parseBoolean(this.parameters.getValueOfParameter("Align To First Timepoint?"));
		
		// Create a dim table that points only to the target images (i.e.
		// reference color and time point 1)
		DimTable imageLocationDimTable = data.getDimTable().getSubTable(new DimensionMap(colorDimName + "=" + referenceColor));
		Dim timeDim = imageLocationDimTable.getDimWithName(timeDimName);
		int timeDimSize = 1;
		if(timeDim != null)
		{
			timeDimSize = timeDim.size();
		}
		imageLocationDimTable = imageLocationDimTable.getSubTable(new DimensionMap(timeDimName + "=" + timeDim.dimValues.get(0)));
		
		// Run the function
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(data);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		
		// //// Create a TurboReg reference and important variables
		TurboReg_ reg = new TurboReg_();
		ImagePlus target = null;
		ImagePlus targetCropImage = null;
		ImagePlus source = null;
		ImagePlus sourceCropImage = null;
		Rectangle rTarget = null;
		Rectangle rSource = null;
		ROIPlus roi; // Temp variable
		int[] sCrop, tCrop;
		TreeMap<DimensionMap,double[][]> sourcePts = new TreeMap<DimensionMap,double[][]>();
		TreeMap<DimensionMap,double[][]> targetPts = new TreeMap<DimensionMap,double[][]>();
		
		// Do the alignment for the reference color first to fill sourcePts and
		// targetPts for use with the other colors.
		int count = 0;
		int percentage = 0;
		DimensionMap ptsMap = null;
		Rectangle cropRoiDims = null;
		int total = imageLocationDimTable.mapCount() * (timeDimSize) * (colorDimSize + 1);
		JEXStatics.statusBar.setProgressPercentage(0);
		for (DimensionMap map : imageLocationDimTable.getMapIterator())
		{
			if(this.isCanceled())
			{
				return false;
			}
			
			// Align the appropriate sources
			String lastTimeVal = null;
			double dx = 0, dy = 0;
			boolean firstTimeThrough = true;
			for (String val : timeDim.dimValues) // Find the transformation for each time in the alignment color
			{
				if(this.isCanceled())
				{
					return false;
				}
				
				// Get the region of the target image to align
				if(firstTimeThrough)
				{
					// Initialize the target image information
					target = new ImagePlus(images.get(map));
					roi = roiMap.get(map);
					if(roi == null)
					{
						rTarget = new Rectangle(0, 0, target.getWidth(), target.getHeight());
					}
					else
					{
						rTarget = roi.getPointList().getBounds();
					}
				}
				else if(!firstTimer)
				{
					// Update the target image information
					DimensionMap tempMap = map.copy();
					tempMap.put(timeDimName, lastTimeVal);
					target = new ImagePlus(images.get(tempMap));
					roi = roiMap.get(map);
					if(roi == null)
					{
						rTarget = new Rectangle(0, 0, target.getWidth(), target.getHeight());
					}
					else
					{
						rTarget = roi.getPointList().getBounds();
					}
				}
				target.setRoi(rTarget);
				targetCropImage = new ImagePlus("TargetCropImage", target.getProcessor().crop());
				tCrop = new int[]{0, 0, targetCropImage.getWidth(), targetCropImage.getHeight()};
				
				// Now get the region of the source image to align
				DimensionMap newMap = map.copy();
				newMap.put(timeDimName, val);
				source = new ImagePlus(images.get(newMap));
				roi = roiMap.get(newMap);
				if(roi == null)
				{
					rSource = new Rectangle(rTarget);
				}
				else
				{
					rSource = roi.getPointList().getBounds();
				}
				Logs.log("Aligning " + newMap.toString(), 0, this);
				Point cropCenterDisplacement = this.getDisplacement(rTarget,  rSource);
				rSource = getSourceRoiLikeTargetRoi(rTarget, cropCenterDisplacement);
				source.setRoi(rSource);
				sourceCropImage = new ImagePlus("SourceCropImage", source.getProcessor().crop());
				sCrop = new int[]{0, 0, sourceCropImage.getWidth(), sourceCropImage.getHeight()};
				
				//				targetCropImage.show();
				//				sourceCropImage.show();
				
				// Align the selected region of the source image with the target image
				reg.alignImages(sourceCropImage, sCrop, targetCropImage, tCrop, TurboReg_.TRANSLATION, false);
				
				// Don't save the image yet. We need to crop it after finding all the necessary translations
				ptsMap = newMap.copy();
				ptsMap.remove(colorDimName);
				if(firstTimeThrough)
				{
					// Put a zero translations in for the first image relative to itself (i.e., source = target)
					sourcePts.put(ptsMap, reg.getTargetPoints());
					targetPts.put(ptsMap, reg.getTargetPoints());
				}
				else if(!firstTimer)
				{
					dx = dx + reg.getSourcePoints()[0][0] + cropCenterDisplacement.x;
					dy = dy + reg.getSourcePoints()[0][1] + cropCenterDisplacement.y;
					double[][] newSourcePoints = new double[][] { { dx, dy }, { 0.0, 0.0 }, { 0.0, 0.0 }, { 0.0, 0.0 } };
					sourcePts.put(ptsMap, newSourcePoints);
					targetPts.put(ptsMap, reg.getTargetPoints());
				}
				else
				{
					dx = reg.getSourcePoints()[0][0] + cropCenterDisplacement.x;
					dy = reg.getSourcePoints()[0][1] + cropCenterDisplacement.y;
					double[][] newSourcePoints = new double[][] { { dx, dy }, { 0.0, 0.0 }, { 0.0, 0.0 }, { 0.0, 0.0 } };
					sourcePts.put(ptsMap, newSourcePoints);
					targetPts.put(ptsMap, reg.getTargetPoints());
				}
				
				if(source != null)
				{
					source.flush();
					source = null;
				}
				
				lastTimeVal = val;
				
				// Status bar
				count = count + 1;
				percentage = (int) (100 * ((double) count / (double) total));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				firstTimeThrough = false;
			}
			firstTimeThrough = true;
		}
		
		cropRoiDims = this.getCropROI(sourcePts, targetPts, target.getWidth(), target.getHeight());
		ImagePlus im;
		ImageProcessor imp;
		DimensionMap newMap = null;
		for (DimensionMap map : imageLocationDimTable.getMapIterator())
		{
			if(this.isCanceled())
			{
				return false;
			}
			newMap = map.copy();
			for (String tVal : timeDim.dimValues)
			{
				if(this.isCanceled())
				{
					return false;
				}
				newMap.put(timeDimName, tVal);
				
				for (int i = 0; i < colorDimSize; i++) // Loop through each color and perform the same transformation
				//for (String cVal : colorDim.dimValues) // Loop through each
				// color and perform
				// the same
				// transformation
				{
					if(this.isCanceled())
					{
						return false;
					}
					if(colorDim != null)
					{
						newMap.put(colorDimName, colorDim.dimValues.get(i));
					}
					source = new ImagePlus(images.get(newMap));
					Logs.log("Applying alignment to " + newMap.toString(), 0, this);
					reg.sourcePoints = sourcePts.get(newMap);
					reg.targetPoints = targetPts.get(newMap);
					im = reg.transformImage(source, target.getWidth(), target.getHeight(), TurboReg_.TRANSLATION, false);
					if(cropResults)
					{
						imp = im.getProcessor();
						imp.setRoi(cropRoiDims);
						imp = imp.crop();
					}
					String path = JEXWriter.saveImage(im);
					outputMap.put(newMap.copy(), path);
					
					imp = null;
					im.killRoi();
					im.flush();
					im = null;
					
					// Status bar
					count = count + 1;
					percentage = (int) (100 * ((double) count / (double) total));
					JEXStatics.statusBar.setProgressPercentage(percentage);
				}
			}
		}
		
		// Set the outputs
		JEXData output = ImageWriter.makeImageStackFromPaths(this.outputNames[0].getName(), outputMap);
		this.realOutputs.add(output);
		if(cropRoiDims != null)
		{
			JEXData outputCropRoi = RoiWriter.makeRoiObject(this.outputNames[1].getName(), new ROIPlus(cropRoiDims));
			this.realOutputs.add(outputCropRoi);
		}
		
		// Return status
		return true;
	}
	
	public Point getDisplacement(Rectangle roi1, Rectangle roi2)
	{
		Point p1 = ROIUtility.getRectangleCenter(new ROIPlus(roi1));
		Point p2 = ROIUtility.getRectangleCenter(new ROIPlus(roi2));
		return new Point(p2.x-p1.x, p2.y-p1.y);
	}
	
	public Rectangle getSourceRoiLikeTargetRoi(Rectangle rTarget, Point cropCenterDisplacement)
	{
		Rectangle ret = new Rectangle(rTarget);
		ret.x = ret.x + cropCenterDisplacement.x;
		ret.y = ret.y + cropCenterDisplacement.y;
		return ret;
	}
	
	public Rectangle getCropROI(TreeMap<DimensionMap,double[][]> sourcePts, TreeMap<DimensionMap,double[][]> targetPts, int imageWidth, int imageHeight)
	{
		int maxdx = 0, mindx = 0, maxdy = 0, mindy = 0;
		double xs, ys, xt, yt;
		int dx, dy;
		for (Entry<DimensionMap,double[][]> e : sourcePts.entrySet())
		{
			xs = e.getValue()[0][0];
			ys = e.getValue()[0][1];
			xt = targetPts.get(e.getKey())[0][0];
			yt = targetPts.get(e.getKey())[0][1];
			dx = (int) Math.round(xs - xt);
			dy = (int) Math.round(ys - yt);
			maxdx = Math.max(maxdx, dx);
			mindx = Math.min(mindx, dx);
			maxdy = Math.max(maxdy, dy);
			mindy = Math.min(mindy, dy);
		}
		
		Rectangle r = new Rectangle(-1 * mindx, -1 * mindy, (imageWidth - (maxdx - mindx)), (imageHeight - (maxdy - mindy)));
		return r;
	}
	
}
