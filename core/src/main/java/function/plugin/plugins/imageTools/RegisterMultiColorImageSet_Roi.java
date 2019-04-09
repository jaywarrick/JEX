package function.plugin.plugins.imageTools;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.RoiWriter;
import function.imageUtility.TurboReg_;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
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
@Plugin(
		type = JEXPlugin.class,
		name="Register a Multi-color Image Set (Roi)",
		menuPath="Image Tools",
		visible=true,
		description="Register a multi-color image set based on one color channel so ecah color stays perfectly registered with the others."
		)
public class RegisterMultiColorImageSet_Roi extends JEXPlugin {

	// ----------------------------------------------------
	// --------- INFORMATION ABOUT THE FUNCTION -----------
	// ----------------------------------------------------

	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	// ----------------------------------------------------
	// --------- INPUT OUTPUT DEFINITIONS -----------------
	// ----------------------------------------------------
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Multicolor Image Set", updatable=false, type=MarkerConstants.TYPE_IMAGE, description="Horizontal alignment (L to R adjacent images) object obtained from the image aligner plugin on the plugins tab of JEX.", optional=false)
	JEXData data;
	
	@InputMarker(uiOrder=2, name="Alignment Region ROI (or Median Deltas ROI, point roi)", updatable=false, type=MarkerConstants.TYPE_IMAGE, description="Either the region of the image to align between frames or a point roi of median deltas to calculate crop rois.", optional=false)
	JEXData roiData;
	
	/////////// Define Parameters ///////////
	

	@ParameterMarker(uiOrder=1, name="Color Dim Name", description="Name of the color dimension", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Color")
	String colorDimName;
	
	@ParameterMarker(uiOrder=2, name="Reference Color", description="Name or number of the color to use for determining registration", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	String referenceColor;
	
	@ParameterMarker(uiOrder=3, name="Time Dim Name", description="Name of the time dimension.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Time")
	String timeDimName;
	
	@ParameterMarker(uiOrder=4, name="Other Dim Name to Split (optional)", description="Name of another dimension to split results on.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String splitDimName;
	
	@ParameterMarker(uiOrder=5, name="Align To First Timepoint?", description="Each image timepoint will be aligned to the first if set to true. Otherwise, time t aligns to t-1.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean firstTimer;

	////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Crop ROI", type=MarkerConstants.TYPE_ROI, flavor="", description="The resultant roi that can be used to crop images for stabalizing the image set.", enabled=true)
	JEXData output;

	public RegisterMultiColorImageSet_Roi()
	{}

	// ----------------------------------------------------
	// --------- THE ACTUAL MEAT OF THIS FUNCTION ---------
	// ----------------------------------------------------

	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Collect the inputs
		if(data == null || !data.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		// Run the function
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(data);

		TreeMap<DimensionMap,ROIPlus> roiMap = new TreeMap<DimensionMap,ROIPlus>();
		boolean havePointRoi = false;
		if(roiData != null && roiData.getDataObjectType().matches(JEXData.ROI))
		{
			roiMap = RoiReader.readObjectToRoiMap(roiData);
			if(roiMap.firstEntry().getValue().type == ROIPlus.ROI_POINT)
			{
				havePointRoi = true;
			}
		}

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

					// Convert the roiMap to regions if necessary for the first time through

					if(havePointRoi)
					{
						roiMap = convertPointsToRegions(roiMap, images, timeDimName, colorDimName, (double) target.getWidth(), (double) target.getHeight());
						// Set the outputs
						JEXData outputCropRoi = RoiWriter.makeRoiObject("temp", roiMap);
						this.output = outputCropRoi;
						return true;
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
		JEXData outputCropRoi = RoiWriter.makeRoiObject("temp", crops);
		this.output = outputCropRoi;

		// Return status
		return true;
	}

	public TreeMap<DimensionMap,ROIPlus> convertPointsToRegions(TreeMap<DimensionMap,ROIPlus> points, TreeMap<DimensionMap,String> imageMap, String timeDimName, String colorDimName, Double width, Double height)
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
					DimensionMap temp = map2.copy();
					if(colorDimName != null)
					{
						temp.remove(colorDimName);
					}
					ret.put(temp, region);
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
