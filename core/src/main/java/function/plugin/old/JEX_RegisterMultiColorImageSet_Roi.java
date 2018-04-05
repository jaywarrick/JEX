package function.plugin.old;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.TreeMap;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.RoiWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import function.imageUtility.TurboReg_;
import ij.ImagePlus;
import image.roi.ROIPlus;
import jex.statics.JEXDialog;
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
public class JEX_RegisterMultiColorImageSet_Roi extends JEXCrunchable {

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
		String result = "Register a Multi-color Image Set (Roi)";
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
		inputNames[1] = new TypeName(ROI, "Alignment Region ROI (optional, rect or point)");
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
		this.defaultOutputNames = new TypeName[1];
		this.defaultOutputNames[0] = new TypeName(ROI, "Crop ROI");

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
		Parameter pa4 = new Parameter("Time Dim Name", "Name of the time dimension.", "Time");
		Parameter pa3 = new Parameter("Other Dim Name to Split (optional)", "Name of another dimension to split results on.", "");
		Parameter pa5 = new Parameter("Align To First Timepoint?", "Each image timepoint will be aligned to the first if set to true. Otherwise, time t aligns to t-1.", Parameter.CHECKBOX, true);
		Parameter pa6 = new Parameter("Just Convert Point Roi?", "If a point roi is provided for the roi input, use the positions in the point roi to determine the output crop roi?", Parameter.CHECKBOX, false);
		Parameter p4 = getNumThreadsParameter(10, 6);

		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p4);
		parameterArray.addParameter(pa1);
		parameterArray.addParameter(pa2);
		parameterArray.addParameter(pa4);
		parameterArray.addParameter(pa3);
		parameterArray.addParameter(pa5);
		parameterArray.addParameter(pa6);
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
		if(data == null || !data.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		// Run the function
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(data);

		JEXData roiData = inputs.get("Alignment Region ROI (optional, rect or point)");
		TreeMap<DimensionMap,ROIPlus> roiMap = new TreeMap<DimensionMap,ROIPlus>();
		boolean havePointRoi = false;
		if(roiData != null && roiData.getDataObjectType().equals(JEXData.ROI))
		{
			roiMap = RoiReader.readObjectToRoiMap(roiData);
			if(roiMap.firstEntry().getValue().type == ROIPlus.ROI_POINT)
			{
				havePointRoi = true;
			}
		}

		// //// Get params
		String colorDimName = this.parameters.getValueOfParameter("Color Dim Name");
		String timeDimName = this.parameters.getValueOfParameter("Time Dim Name");
		String splitDimName = this.parameters.getValueOfParameter("Other Dim Name to Split (optional)");
		boolean convertPoints = Boolean.parseBoolean(this.parameters.getValueOfParameter("Just Convert Point Roi?"));

		String referenceColor = this.parameters.getValueOfParameter("Reference Color");
		boolean firstTimer = Boolean.parseBoolean(this.parameters.getValueOfParameter("Align To First Timepoint?"));

		// Get a DimTable for calculating ROI crops.
		if(!splitDimName.equals(""))
		{
			if(data.getDimTable().getDimWithName(splitDimName) == null)
			{
				JEXDialog.messageDialog("Couldn't find the 'Split Dim' specified. Please check it is a dimension of the image being registered. (case sensitive)", this);
				return false;
			}
		}

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
		TreeMap<DimensionMap,double[][]> sourcePts = new TreeMap<>();
		TreeMap<DimensionMap,double[][]> targetPts = new TreeMap<>();

		// Do the alignment for the reference color first to fill sourcePts and
		// targetPts for use with the other colors.
		int count = 0;
		int percentage = 0;
		DimensionMap ptsMap = null;
		int total = imageLocationDimTable.mapCount() * (timeDimSize);
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
			for (String timeVal : timeDim.dimValues) // Find the transformation for each time in the alignment color
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

					// Convert the roiMap to regions if necessary fir the first time through

					if(havePointRoi)
					{
						roiMap = convertPointsToRegions(roiMap, images, timeDimName, (double) target.getWidth(), (double) target.getHeight());
						if(convertPoints)
						{
							// Set the outputs
							JEXData outputCropRoi = RoiWriter.makeRoiObject(this.outputNames[0].getName(), roiMap);
							this.realOutputs.add(outputCropRoi);
							return true;
						}
					}

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
				newMap.put(timeDimName, timeVal);
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

				// Align the selected region of the source image with the target image
				reg.alignImages(sourceCropImage, sCrop, targetCropImage, tCrop, TurboReg_.TRANSLATION, false);

				// Don't save the image yet. We need to crop it after finding all the necessary translations
				ptsMap = newMap.copy();
				ptsMap.remove(colorDimName); // Now ptsMap has time and location dims but no color dim
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

				lastTimeVal = timeVal;

				// Status bar
				count = count + 1;
				percentage = (int) (100 * ((double) count / (double) total));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				firstTimeThrough = false;
			}
			firstTimeThrough = true;
		}

		TreeMap<DimensionMap,ROIPlus> crops = this.getCropROIs(sourcePts, targetPts, target.getWidth(), target.getHeight(), data.getDimTable(), colorDimName, timeDimName);

		// Set the outputs
		JEXData outputCropRoi = RoiWriter.makeRoiObject(this.outputNames[0].getName(), crops);
		this.realOutputs.add(outputCropRoi);

		// Return status
		return true;
	}

	public TreeMap<DimensionMap,ROIPlus> convertPointsToRegions(TreeMap<DimensionMap,ROIPlus> points, TreeMap<DimensionMap,String> imageMap, String timeDimName, Double width, Double height)
	{
		DimTable dt = new DimTable(imageMap);

		TreeMap<DimensionMap,ROIPlus> ret = new TreeMap<>();
		DimTable dt2 = dt.getSubTable(timeDimName);
		for(DimensionMap filter : dt2.getMapIterator())
		{
			double minx = Double.MAX_VALUE;
			double maxx = Double.MIN_VALUE;
			double miny = Double.MAX_VALUE;
			double maxy = Double.MIN_VALUE;
			double x = 0, y = 0;
			for(DimensionMap map2 : dt.getSubTable(filter).getMapIterator())
			{
				ROIPlus r = points.get(map2);
				if(r != null)
				{
					x = x + r.getPointList().get(0).x;
					y = y + r.getPointList().get(0).y;
					minx = Math.min(minx, x);
					miny = Math.min(miny, y);
					maxx = Math.max(maxx, x);
					maxy = Math.max(maxy, y);
				}
			}
			x = 0;
			y = 0;
			for(DimensionMap map2 : dt.getSubTable(filter).getMapIterator())
			{
				ROIPlus r = points.get(map2);
				if(r != null)
				{
					x = x + r.getPointList().get(0).x;
					y = y + r.getPointList().get(0).y;
					Rectangle roi = new Rectangle((int) Math.round(0-minx), (int) Math.round(0-miny), (int) Math.round(width-maxx+minx), (int) Math.round(height-maxy+miny));
					ROIPlus region = new ROIPlus(roi);
					region.pointList.translate(x, y);
					ret.put(map2.copy(), region);
				}
			}
		}
		
		return ret;
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

	public TreeMap<DimensionMap,ROIPlus> getCropROIs(TreeMap<DimensionMap,double[][]> sourcePts, TreeMap<DimensionMap,double[][]> targetPts, int imageWidth, int imageHeight, DimTable dataDimTable, String colorDimName, String timeDimName)
	{
		// The table forCropping comes in with only the color dimension removed.
		TreeMap<DimensionMap,ROIPlus> ret = new TreeMap<>();

		DimTable tableWOColor = dataDimTable.getSubTable(colorDimName);
		DimTable tableWOColorOrTime = tableWOColor.getSubTable(timeDimName);
		for(DimensionMap map : tableWOColorOrTime.getMapIterator())
		{
			// Just iterating over time for 
			DimTable tableWithOnlyTimeVarying = tableWOColor.getSubTable(map);
			ret.putAll(this.getCropROIs_Sub(sourcePts, targetPts, imageWidth, imageHeight, tableWithOnlyTimeVarying));
		}
		return ret;
	}

	public TreeMap<DimensionMap,ROIPlus> getCropROIs_Sub(TreeMap<DimensionMap,double[][]> sourcePts, TreeMap<DimensionMap,double[][]> targetPts, int imageWidth, int imageHeight, DimTable tableWithMapsToGet)
	{
		int maxdx = 0, mindx = 0, maxdy = 0, mindy = 0;
		double xs, ys, xt, yt;
		int dx, dy;
		for (DimensionMap map : tableWithMapsToGet.getMapIterator())
		{
			xs = sourcePts.get(map)[0][0];
			ys = sourcePts.get(map)[0][1];
			xt = targetPts.get(map)[0][0];
			yt = targetPts.get(map)[0][1];
			dx = (int) Math.round(xs - xt);
			dy = (int) Math.round(ys - yt);
			maxdx = Math.max(maxdx, dx);
			mindx = Math.min(mindx, dx);
			maxdy = Math.max(maxdy, dy);
			mindy = Math.min(mindy, dy);
		}

		Rectangle r = new Rectangle(-1 * mindx, -1 * mindy, (imageWidth - (maxdx - mindx)), (imageHeight - (maxdy - mindy)));

		TreeMap<DimensionMap,ROIPlus> ret = new TreeMap<>();
		for(DimensionMap map : tableWithMapsToGet.getMapIterator())
		{
			Rectangle temp = new Rectangle(r);
			temp.x = temp.x + (int) Math.round(sourcePts.get(map)[0][0]);
			temp.y = temp.y + (int) Math.round(sourcePts.get(map)[0][1]);
			ret.put(map, new ROIPlus(temp));
		}
		return ret;
	}

}
