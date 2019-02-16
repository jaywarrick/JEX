package function.plugin.old;

import java.awt.Shape;
import java.io.File;
import java.util.HashMap;
import java.util.TreeMap;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.RoiWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import function.imageUtility.MaximumFinder;
import function.plugin.plugins.imageProcessing.RankFilters2;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.watershed.Watershed;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.JEXCSVWriter;
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
public class JEX_FindMaximaSegmentation extends JEXCrunchable {
	
	public JEX_FindMaximaSegmentation()
	{}
	
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
		String result = "Find Maxima Segmentation";
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
		String result = "Find maxima in a grayscale image or one color of a multi-color image.";
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
		inputNames[0] = new TypeName(IMAGE, "Image");
		inputNames[1] = new TypeName(ROI, "ROI (optional)");
		return inputNames;
	}
	
	/**
	 * Return the array of output names defined for this function
	 * 
	 * @return
	 */
	@Override
	public TypeName[] getOutputs()
	{
		this.defaultOutputNames = new TypeName[4];
		this.defaultOutputNames[0] = new TypeName(ROI, "Maxima");
		this.defaultOutputNames[1] = new TypeName(FILE, "XY List");
		this.defaultOutputNames[2] = new TypeName(FILE, "Counts");
		this.defaultOutputNames[3] = new TypeName(IMAGE, "Segmented Image");
		
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
		// (ImageProcessor ip, double tolerance, double threshold, int
		// outputType, boolean excludeOnEdges, boolean isEDM, Roi roiArg,
		// boolean lightBackground)
		// Parameter p0 = new
		// Parameter("Dummy Parameter","Lets user know that the function has been selected.",FormLine.DROPDOWN,new
		// String[] {"true"},0);
		Parameter p00 = getNumThreadsParameter(10, 1);
		Parameter p0a = new Parameter("Pre-Despeckle Radius", "Radius of median filter applied before max finding", "0");
		Parameter p0b = new Parameter("Pre-Smoothing Radius", "Radius of mean filter applied before max finding", "0");
		Parameter pa1 = new Parameter("Color Dim Name", "Name of the color dimension.", "Color");
		Parameter pa2 = new Parameter("Maxima Color Dim Value", "Value of the color dimension to analyze for determing maxima. (leave blank to ignore and perform on all images)", "");
		Parameter pa3 = new Parameter("Segmentation Color Dim Value", "Value of the color dimension to use for segmentation using the found maxima. (leave blank to apply to the same color used to find maxima)", "");
		Parameter p1 = new Parameter("Tolerance", "Local intensity increase threshold.", "20");
		Parameter p2 = new Parameter("Threshold", "Minimum hieght of a maximum.", "0");
		Parameter p3a = new Parameter("Exclude Maximima on Edges?", "Exclude particles on the edge of the image?", Parameter.CHECKBOX, true);
		Parameter p3b = new Parameter("Exclude Segments on Edges?", "Exclude segements on the edge of the image? (helpful so that half-nuclei aren't counted with the maxima found while excluding maxima on edges)", Parameter.CHECKBOX, false);
		Parameter p4 = new Parameter("Is EDM?", "Is the image being analyzed already a Euclidean Distance Measurement?", Parameter.CHECKBOX, false);
		Parameter p5 = new Parameter("Particles Are White?", "Are the particles displayed as white on a black background?", Parameter.CHECKBOX, true);
		Parameter p6 = new Parameter("Create Segmented Image?", "Should a segmented image, point count, and XY List of points be created?", Parameter.CHECKBOX, false);
		Parameter p7 = new Parameter("Use New Watershed Segmentation?", "Use the old ImageJ watershed or new MorphLibJ implementation?.", Parameter.CHECKBOX, true);
		Parameter p8 = new Parameter("If New: 4 or 8 Connected?", "Use the old ImageJ watershed or new MorphLibJ implementation?.", Parameter.DROPDOWN, new String[] {"4","8"}, 0);
		Parameter p9 = new Parameter("Segmentation Pre-Despeckle Radius", "The radius of the median filter used on the channel to be used for segmentation prior to watershedding.", Parameter.TEXTFIELD, "0");
		Parameter p10 = new Parameter("Segmentation Pre-Smoothing Radius", "The radius of the mean filter used on the channel to be used for segmentation prior to watershedding.", Parameter.TEXTFIELD, "0");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p00);
		parameterArray.addParameter(p0a);
		parameterArray.addParameter(p0b);
		parameterArray.addParameter(pa1);
		parameterArray.addParameter(pa2);
		parameterArray.addParameter(pa3);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3a);
		parameterArray.addParameter(p3b);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p8);
		parameterArray.addParameter(p9);
		parameterArray.addParameter(p10);
		return parameterArray;
	}
	
	// ----------------------------------------------------
	// --------- ERROR CHECKING METHODS -------------------
	// ----------------------------------------------------
	
	/**
	 * Returns the status of the input validity checking It is HIGHLY recommended to implement input checking however this can be over-ridden by returning false If over-ridden ANY batch function using this function will not be able perform error
	 * checking...
	 * 
	 * @return true if input checking is on
	 */
	@Override
	public boolean isInputValidityCheckingEnabled()
	{
		return false;
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
		try
		{
			/* COLLECT DATA INPUTS */
			boolean roiProvided = false;
			JEXData imageData = inputs.get("Image");
			// if/else to figure out whether or not valid image data has been given;
			// ends run if not
			if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
			{
				return false;
			}
			JEXData roiData = inputs.get("ROI (optional)");
			if(roiData != null && roiData.getTypeName().getType().matches(JEXData.ROI))
			{
				roiProvided = true;
			}
			
			
			/* GATHER PARAMETERS */
			double despeckleR = Double.parseDouble(this.parameters.getValueOfParameter("Pre-Despeckle Radius"));
			double smoothR = Double.parseDouble(this.parameters.getValueOfParameter("Pre-Smoothing Radius"));
			String colorDimName = this.parameters.getValueOfParameter("Color Dim Name");
			String nuclearDimValue = this.parameters.getValueOfParameter("Maxima Color Dim Value");
			String segDimValue = this.parameters.getValueOfParameter("Segmentation Color Dim Value");
			double tolerance = Double.parseDouble(this.parameters.getValueOfParameter("Tolerance"));
			double threshold = Double.parseDouble(this.parameters.getValueOfParameter("Threshold"));
			boolean excludePtsOnEdges = Boolean.parseBoolean(this.parameters.getValueOfParameter("Exclude Maximima on Edges?"));
			boolean excludeSegsOnEdges = Boolean.parseBoolean(this.parameters.getValueOfParameter("Exclude Segments on Edges?"));
			boolean isEDM = Boolean.parseBoolean(this.parameters.getValueOfParameter("Is EDM?"));
			boolean lightBackground = !Boolean.parseBoolean(this.parameters.getValueOfParameter("Particles Are White?"));
			boolean maximaOnly = !Boolean.parseBoolean(this.parameters.getValueOfParameter("Create Segmented Image?"));
			boolean newWatershed = Boolean.parseBoolean(this.parameters.getValueOfParameter("Use New Watershed Segmentation?"));
			int connectedness = Integer.parseInt(this.parameters.getValueOfParameter("If New: 4 or 8 Connected?"));
			double waterDespeckleR = Double.parseDouble(this.parameters.getValueOfParameter("Segmentation Pre-Despeckle Radius"));
			double waterSmoothR = Double.parseDouble(this.parameters.getValueOfParameter("Segmentation Pre-Smoothing Radius"));
			
			if(!nuclearDimValue.equals("") && segDimValue.equals(""))
			{
				Logs.log("Found a blank segmentation dimension value even though a maxima dim value was specified. Using the maxima dim value as the segmentation dim value.", this);
				segDimValue = nuclearDimValue;
			}
			
			
			/* RUN THE FUNCTION */
			// validate roiMap (if provided)
			TreeMap<DimensionMap,ROIPlus> roiMap;
			if(roiProvided)	roiMap = RoiReader.readObjectToRoiMap(roiData);
			else roiMap = new TreeMap<DimensionMap,ROIPlus>();
			
			// Read the images in the IMAGE data object into imageMap
			TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
			
			
			DimTable filteredTable = null;// imageData.getDimTable().copy();
			// if a maxima color dimension is given
			if(!nuclearDimValue.equals(""))
			{
				filteredTable = imageData.getDimTable().getSubTable(new DimensionMap(colorDimName + "=" + nuclearDimValue));
			}
			else {
				// copy the DimTable from imageData
				filteredTable = imageData.getDimTable().copy();
			}
			
			
			// Declare outputs
			TreeMap<DimensionMap,ROIPlus> outputRoiMap = new TreeMap<DimensionMap,ROIPlus>();
			TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
			TreeMap<DimensionMap,String> outputFileMap = new TreeMap<DimensionMap,String>();
			TreeMap<DimensionMap,Double> outputCountMap = new TreeMap<DimensionMap,Double>();
			
			
			// determine value of total	
			int total;// filteredTable.mapCount() * 4; // if maximaOnly
			if(!maximaOnly & !segDimValue.equals(nuclearDimValue))
			{
				total = filteredTable.mapCount() * 8;
			}
			else if(!maximaOnly)
			{
				total = filteredTable.mapCount() * 5;
			}
			else { // if maximaOnly
				total = filteredTable.mapCount() * 4;
			}
			
			
			Roi roi;
			ROIPlus roip;
			int count = 0, percentage = 0, counter = 0;
			for (DimensionMap map : filteredTable.getMapIterator())
			{
				MaximumFinder mf = new MaximumFinder();
				if(this.isCanceled())
				{
					return false;
				}
				// // Update the display
				count ++;
				percentage = (int) (100 * ((double) (count) / ((double) total)));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				counter ++;
				
				String pathToGet = imageMap.get(map);
				if(pathToGet == null)
				{
					// Counting will be messed up now for the progress bar but hard to remedy
					continue;
				}
				ImagePlus im = new ImagePlus(pathToGet);
				FloatProcessor ip = (FloatProcessor) im.getProcessor().convertToFloat();
				im.setProcessor(ip);
				
				if(despeckleR > 0)
				{
					// Smooth the image
					RankFilters2 rF = new RankFilters2();
					rF.rank(ip, despeckleR, RankFilters2.MEDIAN);
				}
				if(this.isCanceled())
				{
					return false;
				}
				// // Update the display
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) total)));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				counter = counter + 1;
				
				if(smoothR > 0)
				{
					// Smooth the image
					RankFilters2 rF = new RankFilters2();
					rF.rank(ip, smoothR, RankFilters2.MEAN);
				}
				if(this.isCanceled())
				{
					return false;
				}
				// // Update the display
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) total)));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				counter = counter + 1;
				
				roi = null;
				roip = null;
				roip = roiMap.get(map);
				if(roip != null)
				{
					boolean isLine = roip.isLine();
					if(isLine)
					{
						return false;
					}
					roi = roip.getRoi();
					im.setRoi(roi);
				}
				
				// // Find the Maxima
				ROIPlus points = (ROIPlus) mf.findMaxima(im.getProcessor(), tolerance, threshold, MaximumFinder.ROI, excludePtsOnEdges, isEDM, roi, lightBackground, true);
				// // Retain maxima within the optional roi
				PointList filteredPoints = new PointList();
				if(roiProvided && roip.getPointList().size() != 0)
				{
					Shape shape = roip.getShape();
					for (IdPoint p : points.getPointList())
					{
						if(shape.contains(p))
						{
							filteredPoints.add(p.x, p.y);
						}
					}
				}
				else
				{
					filteredPoints = points.getPointList();
				}
				
				// // Create the new ROIPlus
				ROIPlus newRoip = new ROIPlus(filteredPoints, ROIPlus.ROI_POINT);
				DimensionMap tempMap = map.copy();
				if(!nuclearDimValue.equals(""))
				{
					tempMap.remove(colorDimName);
				}
				outputRoiMap.put(tempMap, newRoip);
				
				// // Count the maxima
				outputCountMap.put(map, (double) filteredPoints.size());
				
				// // Create the file of XY locations
				String listPath = createXYPointListFile(filteredPoints);
				outputFileMap.put(map, listPath);
				
				if(!maximaOnly)
				{
					// // Create the segemented image
					DimensionMap segMap = map.copy();
					segMap.put(colorDimName, segDimValue);
					ImageProcessor toSeg = ip;
					if(!segDimValue.equals(nuclearDimValue) || (smoothR != waterSmoothR) || (despeckleR != waterDespeckleR))
					{
						if(this.isCanceled())
						{
							return false;
						}
						// // Update the display
						count = count + 1;
						percentage = (int) (100 * ((double) (count) / ((double) total)));
						JEXStatics.statusBar.setProgressPercentage(percentage);
						counter = counter + 1;
						
						toSeg = new ImagePlus(imageMap.get(segMap)).getProcessor();
						if(waterDespeckleR > 0)
						{
							// Smooth the image
							RankFilters2 rF = new RankFilters2();
							rF.rank(toSeg, despeckleR, RankFilters2.MEDIAN);
						}
						if(this.isCanceled())
						{
							return false;
						}
						// // Update the display
						count = count + 1;
						percentage = (int) (100 * ((double) (count) / ((double) total)));
						JEXStatics.statusBar.setProgressPercentage(percentage);
						counter = counter + 1;
						
						if(waterSmoothR > 0)
						{
							// Smooth the image
							RankFilters2 rF = new RankFilters2();
							rF.rank(toSeg, smoothR, RankFilters2.MEAN);
						}
						if(this.isCanceled())
						{
							return false;
						}
						// // Update the display
						count = count + 1;
						percentage = (int) (100 * ((double) (count) / ((double) total)));
						JEXStatics.statusBar.setProgressPercentage(percentage);
						counter = counter + 1;
					}
					
					// Create the Segmented Image
					ByteProcessor segmentedImage = null;
					if(excludeSegsOnEdges != excludePtsOnEdges)
					{
						// // Find the Maxima again with the correct exclusion criteria
						if(newWatershed)
						{
							ImageProcessor markerPoints = (ByteProcessor) mf.findMaxima(im.getProcessor(), tolerance, threshold, MaximumFinder.SINGLE_POINTS, excludeSegsOnEdges, isEDM, roi, lightBackground, true);
							toSeg.invert();
							markerPoints = BinaryImages.componentsLabeling(markerPoints, 4, 32);
							ImageProcessor seg = Watershed.computeWatershed(toSeg, markerPoints, connectedness, true);
							seg.multiply(255);
							// FileUtility.openFileDefaultApplication(JEXWriter.saveImage(seg));
							segmentedImage = seg.convertToByteProcessor(false);
						}
						else
						{
							points = (ROIPlus) mf.findMaxima(im.getProcessor(), tolerance, threshold, MaximumFinder.ROI, excludeSegsOnEdges, isEDM, roi, lightBackground, true);
							segmentedImage = mf.segmentImageUsingMaxima(toSeg, excludeSegsOnEdges);
						}
					}
					else
					{
						// Just use the points we already found
						if(newWatershed)
						{
							toSeg.invert();
							ImageProcessor markerPoints = BinaryImages.componentsLabeling(mf.getMarkerPoints(), 4, 32);
							ImageProcessor seg = Watershed.computeWatershed(toSeg, markerPoints, connectedness, true);
							seg.multiply(255);
							// FileUtility.openFileDefaultApplication(JEXWriter.saveImage(seg));
							segmentedImage = seg.convertToByteProcessor(false);
						}
						else
						{
							segmentedImage = mf.segmentImageUsingMaxima(toSeg, excludeSegsOnEdges);
						}						
					}
					if(this.isCanceled())
					{
						return false;
					}
					// // Update the display
					count = count + 1;
					percentage = (int) (100 * ((double) (count) / ((double) total)));
					JEXStatics.statusBar.setProgressPercentage(percentage);
					counter = counter + 1;
					
					String segmentedImagePath = JEXWriter.saveImage(segmentedImage);
					if(segmentedImagePath == null)
					{
						Logs.log("Failed to create/write segmented image", Logs.ERROR, this);
					}
					else
					{
						outputImageMap.put(tempMap, segmentedImagePath);
					}
					
				}
				
				// // Update the display
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) total)));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				counter = counter + 1;
				im.flush();
				im = null;
				ip = null;
			}
			if(outputRoiMap.size() == 0)
			{
				return false;
			}
			
			// roi, file file(value), image
			JEXData output0 = RoiWriter.makeRoiObject(this.outputNames[0].getName(), outputRoiMap);
			JEXData output1 = FileWriter.makeFileObject(this.outputNames[1].getName(), null, outputFileMap);
			String countsFile = JEXTableWriter.writeTable(this.outputNames[2].getName(), outputCountMap, "arff");
			JEXData output2 = FileWriter.makeFileObject(this.outputNames[2].getName(), null, countsFile);
			this.realOutputs.add(output0);
			this.realOutputs.add(output1);
			this.realOutputs.add(output2);
			
			
			if(!maximaOnly)
			{
				JEXData output3 = ImageWriter.makeImageStackFromPaths(this.outputNames[3].getName(), outputImageMap);
				
				this.realOutputs.add(output3);
			}
			
			// Return status
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	// private String saveAdjustedImage(String imagePath, double oldMin, double
	// oldMax, double newMin, double newMax, double gamma, int bitDepth)
	// {
	// // Get image data
	// File f = new File(imagePath);
	// if(!f.exists()) return null;
	// ImagePlus im = new ImagePlus(imagePath);
	// FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat();
	// // should be a float processor
	//
	// // Adjust the image
	// FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
	//
	// // Save the results
	// ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false",
	// bitDepth);
	// String imPath = JEXWriter.saveImage(toSave);
	// im.flush();
	//
	// // return temp filePath
	// return imPath;
	// }
	
	public static String createXYPointListFile(PointList pl)
	{
		JEXCSVWriter w = new JEXCSVWriter(JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("csv"));
		w.write(new String[] { "ID", "X", "Y" });
		for (IdPoint p : pl)
		{
			w.write(new String[] { "" + p.id, "" + p.x, "" + p.y });
		}
		w.close();
		return w.path;
	}
}