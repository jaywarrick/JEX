package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.EDM;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jex.utilities.FunctionUtility;
import logs.Logs;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.ExperimentalDataCrunch;

/**
 * This is a JEXperiment function template
 * To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions
 * 2. Place the file in the Functions/SingleDataPointFunctions folder
 * 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types
 * The specific API for these can be found in the main JEXperiment folder.
 * These API provide methods to retrieve data from these objects,
 * create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 *
 */
/**
 * @author edmyoung
 * 
 */
public class JEX_EY_p65_IntensityCalc_NuclearSelect extends ExperimentalDataCrunch {
	
	// ----------------------------------------------------
	// --------- INFORMATION ABOUT THE FUNCTION -----------
	// ----------------------------------------------------
	
	/**
	 * Returns the name of the function
	 * 
	 * @return Name string
	 */
	public String getName()
	{
		String result = "Nuclear Intensity Analysis - p65";
		return result;
	}
	
	/**
	 * This method returns a string explaining what this method does This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	public String getInfo()
	{
		String result = "Select out nuclear regions of fluorescent cell stain and plot intensity histogram";
		return result;
	}
	
	/**
	 * This method defines in which group of function this function will be shown in... Typical toolboxes are "Image analysis" or "Image filtering" or "Cell tracking"
	 */
	public String getToolbox()
	{
		String toolbox = "Custom Cell Analysis";
		return toolbox;
	}
	
	/**
	 * This method defines if the function appears in the list in JEX It should be set to true expect if you have good reason for it
	 * 
	 * @return true if function shows in JEX
	 */
	public boolean showInList()
	{
		return true;
	}
	
	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
	public boolean allowMultithreading()
	{
		return false;
	}
	
	// ----------------------------------------------------
	// --------- INPUT OUTPUT DEFINITIONS -----------------
	// ----------------------------------------------------
	
	/**
	 * Return the number of inputs required by this function In case of a function that takes optional inputs set this number to the maximum number of inputs possible
	 * 
	 * @return number of inputs
	 */
	public int getNumberOfInputs()
	{
		// Inputs are Hoechst nuclear stain (the mask) and p65 stain (the
		// primary)
		return 2;
	}
	
	/**
	 * Return the array of input names
	 * 
	 * @return array of input names
	 */
	public TypeName[] getInputNames()
	{
		TypeName[] inputNames = new TypeName[getNumberOfInputs()];
		
		inputNames[0] = new TypeName(IMAGE, "Hoechst image");
		inputNames[1] = new TypeName(IMAGE, "p65 image");
		
		return inputNames;
	}
	
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[5];
		defaultOutputNames[0] = new TypeName(IMAGE, "Cytoplasm Binary Mask");
		defaultOutputNames[1] = new TypeName(IMAGE, "Nuclei Isolated");
		defaultOutputNames[2] = new TypeName(IMAGE, "Cytoplasm Isolated");
		defaultOutputNames[3] = new TypeName(VALUE, "Histogram");
		defaultOutputNames[4] = new TypeName(VALUE, "Average nuclei intensity");
		if(outputNames == null)
			return defaultOutputNames;
		return outputNames;
	}
	
	/**
	 * Returns a list of parameters necessary for this function to run... Every parameter is defined as a line in a form that provides the ability to set how it will be displayed to the user and what options are available to choose from The simplest
	 * FormLine can be written as: FormLine p = new FormLine(parameterName); This will provide a text field for the user to input the value of the parameter named parameterName More complex displaying options can be set by consulting the FormLine API
	 * 
	 * @return list of FormLine to create a parameter panel
	 */
	public ParameterSet requiredParameters()
	{
		Parameter p0 = new Parameter("Automatic", "Enable visual interface", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p1 = new Parameter("Binning", "Binning factor for quicker analysis", "2.0");
		Parameter p2 = new Parameter("Threshold", "Threshold for identifying neutrophil locations", "40.0");
		Parameter p3 = new Parameter("Filter tracks", "Keep tracks of mean velocity superior to", "1.0");
		Parameter p4 = new Parameter("Cell Radius", "Cell radius in pixels (e.g. 3 to 30)", "15");
		Parameter p5 = new Parameter("Minimum length", "Minimum length for keeping a track in number of points", "10");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
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
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		
		// Collect inputs
		// NOTE: ALL variables ending in '1' are associated with Hoechst images
		// (input: data1)
		// NOTE: ALL variables ending in '2' are associated with p65 images
		// (input: data2)
		
		// data1 contains all the Hoechst images per entry
		// data2 contains all the p65 images per entry
		JEXData data1 = inputs.get("Hoechst image");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		JEXData data2 = inputs.get("p65 image");
		if(!data2.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// Read image stack from data1 and data 2 to Lists image1 and image2
		List<String> image1 = ImageReader.readObjectToImagePathList(data1);
		List<String> image2 = ImageReader.readObjectToImagePathList(data2);
		
		// Keep count of total cell ROIs from all images in stack
		// Use counter for the combined data arrays
		int totalRoiCount = 0;
		int RoiCounter = 0;
		double totalIR = 0.0;
		double avgIR = 0.0;
		int validIRcount = 0;
		
		// Declare arrays for holding combined data from all images in image
		// stack
		ArrayList<Double> cytoplasmIntensityAll = new ArrayList<Double>();
		ArrayList<Double> nucleusIntensityAll = new ArrayList<Double>();
		ArrayList<Double> avgCytoplasmIntensityAll = new ArrayList<Double>();
		ArrayList<Double> avgNucleusIntensityAll = new ArrayList<Double>();
		ArrayList<Integer> cytoplasmPixelCountAll = new ArrayList<Integer>();
		ArrayList<Integer> nucleusPixelCountAll = new ArrayList<Integer>();
		ArrayList<Integer> totalPixelCountAll = new ArrayList<Integer>();
		ArrayList<Double> intensityRatioAll = new ArrayList<Double>();
		
		// for(int i = 1; i < image1.size()-1; i++) { // For HUVECs, avoid the
		// first and last images because of ports
		for (int i = 0; i < image1.size(); i++)
		{
			
			// path1 and path2 hold the paths to the images inside the stack
			String path1 = image1.get(i);
			String path2 = image2.get(i);
			// Read the paths into ImagePlus images
			ImagePlus imPlus1 = new ImagePlus(path1);
			ImagePlus imPlus2 = new ImagePlus(path2);
			
			ShortProcessor shortOriginalImPlus1 = (ShortProcessor) imPlus1.getProcessor().convertToShort(true);
			ShortProcessor shortOriginalImPlus2 = (ShortProcessor) imPlus2.getProcessor().convertToShort(true);
			ShortProcessor shortImPlus1 = new ShortProcessor(imPlus1.getWidth(), imPlus1.getHeight());
			ShortProcessor shortImPlus2 = new ShortProcessor(imPlus2.getWidth(), imPlus2.getHeight());
			shortImPlus1.copyBits(shortOriginalImPlus1, 0, 0, Blitter.COPY);
			shortImPlus2.copyBits(shortOriginalImPlus2, 0, 0, Blitter.COPY);
			
			System.out.println("Hoechst stain max pixel intensity = " + shortImPlus1.getHistogramMax());
			System.out.println("p65 stain max pixel intensity = " + shortImPlus2.maxValue());
			
			// Background subtracter
			BackgroundSubtracter bgs = new BackgroundSubtracter();
			
			// Background subtracter parameters
			double radius1 = 30.0;
			double radius2 = 30.0;
			boolean createBackground = false;
			boolean lightBackground = false;
			boolean useParaboloid = false;
			boolean doPresmooth = false;
			boolean correctCorners = false;
			
			// Perform background subtraction for both Hoechst and p65 images
			bgs.rollingBallBackground(shortImPlus1, radius1, createBackground, lightBackground, useParaboloid, doPresmooth, correctCorners);
			bgs.rollingBallBackground(shortImPlus2, radius2, createBackground, lightBackground, useParaboloid, doPresmooth, correctCorners);
			System.out.println("BG subtract ok");
			
			shortImPlus1.autoThreshold(); // auto Threshold
			shortImPlus2.autoThreshold();
			
			shortImPlus1.invertLut(); // invert LUT, i.e., 255 - v, changing the
			// display w/o changing actual pixel value
			// v
			shortImPlus2.invertLut();
			
			// Convert ShortProcessor to ByteProcessor for watershedding
			ByteProcessor byteImPlus1 = (ByteProcessor) shortImPlus1.convertToByte(true);
			ByteProcessor byteImPlus2 = (ByteProcessor) shortImPlus2.convertToByte(true);
			
			EDM edm1 = new EDM();
			EDM edm2 = new EDM();
			edm1.toWatershed(byteImPlus1); // Watershed
			// byteImPlus2.dilate(); // dilate cytoplasm before watershedding
			edm2.toWatershed(byteImPlus2);
			
			// Erode function, makes nuclei thinner by one perimeter of pixels
			// NOTE: Only apply to the nuclei
			byteImPlus1.erode();
			
			// Convert ByteProcessor back to ImagePlus after watershedding is
			// done
			// CHECK the images
			ImagePlus imPlusWatershed1 = new ImagePlus("Watershed Hoechst", byteImPlus1.duplicate());
			ImagePlus imPlusWatershed2 = new ImagePlus("Watershed p65", byteImPlus2.duplicate());
			
			// FOR HUVECs
			// ByteProcessor byteImPlus1Dilate = new
			// ByteProcessor(imPlus2.getWidth(), imPlus2.getHeight());
			// byteImPlus1Dilate.copyBits(byteImPlus1, 0, 0, Blitter.COPY);
			// byteImPlus1Dilate.invertLut();
			// byteImPlus1Dilate.dilate();
			// byteImPlus1Dilate.dilate();
			// byteImPlus1Dilate.dilate();
			// ImagePlus imPlusWatershed2 = new ImagePlus("Dilated Nuclei",
			// byteImPlus1Dilate.duplicate());
			// END OF HUVECs code
			// imPlusWatershed2.show();
			
			// Use ImageJ Particle Analyzer on data1
			int options = ParticleAnalyzer.ADD_TO_MANAGER | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES | ParticleAnalyzer.INCLUDE_HOLES;
			int measure = ParticleAnalyzer.AREA | ParticleAnalyzer.CIRCULARITY | ParticleAnalyzer.INTEGRATED_DENSITY | ParticleAnalyzer.CENTROID | ParticleAnalyzer.ELLIPSE | ParticleAnalyzer.PERIMETER;
			
			ResultsTable rt1 = new ResultsTable();
			ResultsTable rt2 = new ResultsTable();
			double minSize1 = 10;
			double minSize2 = 50;
			double maxSize1 = 1000.0;
			double maxSize2 = 1000.0;
			double minCirc1 = 0.1;
			double minCirc2 = 0.1;
			double maxCirc1 = 1.0;
			double maxCirc2 = 1.0;
			
			ParticleAnalyzer analyzer1 = new ParticleAnalyzer(options, measure, rt1, minSize1, maxSize1, minCirc1, maxCirc1);
			analyzer1.analyze(imPlusWatershed1);
			
			ParticleAnalyzer analyzer2 = new ParticleAnalyzer(options, measure, rt2, minSize2, maxSize2, minCirc2, maxCirc2);
			analyzer2.analyze(imPlusWatershed2); // Use this one for normal RPMI
			// work
			
			// Make a mask of only the cytoplasm by subtracting nuclei from
			// whole cell threshold mask via Blitter
			ByteProcessor cytoplasm = (ByteProcessor) byteImPlus2.duplicate(); // RPMI
			// ByteProcessor cytoplasm = (ByteProcessor)
			// byteImPlus1Dilate.duplicate(); //HUVEC
			cytoplasm.copyBits(byteImPlus1, 0, 0, Blitter.SUBTRACT);
			// ImagePlus ipCytoplasm = new ImagePlus("cytoplasm mask",
			// cytoplasm.duplicate());
			// ipCytoplasm.show();
			
			// Acquire the ROIs from the particle analysis and apply to the p65
			// image
			List<Roi> foundRois1 = analyzer1.foundRois;
			List<Roi> foundRois2 = analyzer2.foundRois;
			
			System.out.println("Size of Roi list after Hoechst analyzer: " + foundRois1.size());
			System.out.println("Size of Roi list after p65 analyzer: " + foundRois2.size());
			
			// // Visual overlay to see ROIs
			// ColorProcessor visu = (ColorProcessor)
			// shortOriginalImPlus2.duplicate().convertToRGB();
			// ImagePlus im_visu = new ImagePlus("visu ImagePlus P65",
			// visu.duplicate());
			//
			// Overlay (for image display only)
			Overlay overlay = new Overlay();
			overlay.drawLabels(false);
			//
			// for (int n = 0; n < foundRois1.size(); n++){
			// Roi roi1 = foundRois1.get(n);
			// // Outline display image
			// roi1.setStrokeColor(Color.red);
			// overlay.add((Roi)roi1.clone());
			// }
			
			totalRoiCount += foundRois2.size();
			System.out.println("Current image foundRois2: " + foundRois2.size());
			System.out.println("Total foundRois2: " + totalRoiCount);
			
			int u, v;
			double[] cytoplasmIntensity = new double[foundRois2.size()];
			double[] nucleusIntensity = new double[foundRois2.size()];
			double[] avgCytoplasmIntensity = new double[foundRois2.size()];
			double[] avgNucleusIntensity = new double[foundRois2.size()];
			int[] cytoplasmPixelCount = new int[foundRois2.size()];
			int[] nucleusPixelCount = new int[foundRois2.size()];
			int[] totalPixelCount = new int[foundRois2.size()];
			double[] intensityRatio = new double[foundRois2.size()];
			
			for (int m = 0; m < foundRois2.size(); m++)
			{
				Roi roi = foundRois2.get(m);
				
				// Outline display image
				roi.setStrokeColor(Color.yellow);
				overlay.add((Roi) roi.clone());
				
				shortOriginalImPlus2.setRoi(roi);
				cytoplasm.setRoi(roi);
				
				ShortProcessor statcrop = (ShortProcessor) shortOriginalImPlus2.crop();
				ByteProcessor statcrop2 = (ByteProcessor) cytoplasm.crop();
				
				// ImagePlus ipStatcrop = new ImagePlus("shortOriginalImPlus2",
				// statcrop.duplicate());
				// ipStatcrop.show();
				// ImagePlus ipStatcrop2 = new ImagePlus("cytoplasm",
				// statcrop2.duplicate());
				// ipStatcrop2.show();
				
				// Calibration cal = new Calibration(imPlus2);
				// ImageStatistics stats =
				// ImageStatistics.getStatistics(statcrop,
				// Measurements.INTEGRATED_DENSITY|Measurements.AREA|Measurements.MEAN|Measurements.PERIMETER,
				// cal);
				
				ImageProcessor roiMask = roi.getMask();
				// ImagePlus ipRoiMask = new ImagePlus("roi",
				// roiMask.duplicate());
				// ipRoiMask.show();
				
				// ShortProcessor sipRoiMask = (ShortProcessor)
				// ipRoiMask.getProcessor().convertToShort(true);
				//
				// File imFile_is = new File(path1);
				// String currentName_is = imFile_is.getName();
				// String localDir_is =
				// JEXStatics.jexManager.getLocalFolder(entry);
				//
				// // TEST: ROI printout
				// String newFileNametest =
				// FunctionUtility.getNextName(localDir_is, currentName_is,
				// "CYTOMASK-");
				// Logs.log("Saving in " + localDir_is +
				// File.separator + newFileNametest ,1, this);
				// FunctionUtility.imSave(statcrop2, localDir_is +
				// File.separator + newFileNametest);
				//
				// String newFileNametest2 =
				// FunctionUtility.getNextName(localDir_is, currentName_is,
				// "CROP-");
				// Logs.log("Saving in " + localDir_is +
				// File.separator + newFileNametest2 ,1, this);
				// FunctionUtility.imSave(statcrop, localDir_is + File.separator
				// + newFileNametest2);
				//
				// String newFileNametest3 =
				// FunctionUtility.getNextName(localDir_is, currentName_is,
				// "ROI-");
				// Logs.log("Saving in " + localDir_is +
				// File.separator + newFileNametest3 ,1, this);
				// FunctionUtility.imSave(sipRoiMask, localDir_is +
				// File.separator + newFileNametest3);
				
				for (v = 0; v < roiMask.getHeight(); v++)
				{
					for (u = 0; u < roiMask.getWidth(); u++)
					{
						// Only worry about the pixels that are masked by the
						// roiMask, which includes everything inside the cell
						if(roiMask.getPixelValue(u, v) != 0)
						{
							totalPixelCount[m]++;
							if(statcrop2.getPixelValue(u, v) == 0)
							{
								
								// if zero, then it's nucleus
								nucleusIntensity[m] += statcrop.getPixelValue(u, v);
								nucleusPixelCount[m]++;
								// System.out.println("pixelValue[" + u + "," +
								// v + "]: " + statcrop.getPixelValue(u, v));
							}
							else
							{
								cytoplasmIntensity[m] += statcrop.getPixelValue(u, v);
								cytoplasmPixelCount[m]++;
							}
						}
					}
				}
				// Calculate localized intensities and ratio
				avgCytoplasmIntensity[m] = cytoplasmIntensity[m] / cytoplasmPixelCount[m];
				avgNucleusIntensity[m] = nucleusIntensity[m] / nucleusPixelCount[m];
				intensityRatio[m] = avgNucleusIntensity[m] / avgCytoplasmIntensity[m];
				// Store values in combined storage
				cytoplasmIntensityAll.add(cytoplasmIntensity[m]);
				nucleusIntensityAll.add(nucleusIntensity[m]);
				avgCytoplasmIntensityAll.add(avgCytoplasmIntensity[m]);
				avgNucleusIntensityAll.add(avgNucleusIntensity[m]);
				cytoplasmPixelCountAll.add(cytoplasmPixelCount[m]);
				nucleusPixelCountAll.add(nucleusPixelCount[m]);
				totalPixelCountAll.add(totalPixelCount[m]);
				intensityRatioAll.add(intensityRatio[m]);
				
				// System output for debugging
				// System.out.println("intensityRatio[" + m + "] = " +
				// intensityRatio[m] + "; " + intensityRatio[m].isNaN() + "; " +
				// intensityRatio[m].isInfinite());
				// System.out.println("roi[" + m + "]: " + cytoplasmIntensity[m]
				// + ", " + cytoplasmPixelCount[m] + ", " + nucleusIntensity[m]
				// + ", " + nucleusPixelCount[m] + ", "
				// + avgCytoplasmIntensity[m] + ", " + avgNucleusIntensity[m] +
				// ", " + intensityRatio[m]);
				// System.out.println("stats: " + stats.area + ", " +
				// stats.roiHeight + ", " + stats.roiWidth + ", " +
				// totalPixelCount[m]);
				//
				// System.out.println("intensityRatioAll.get(" + m + ") = " +
				// intensityRatio[m] + " (" +
				// intensityRatioAll.get(totalRoiCounter) + ") " +
				// "; " + intensityRatioAll.get(totalRoiCounter).isNaN() + "; "
				// + intensityRatioAll.get(totalRoiCounter).isInfinite() +
				// "; " + intensityRatio[m].isNaN() + "; " +
				// intensityRatio[m].isInfinite());
				
				if(intensityRatioAll.get(RoiCounter).isInfinite() == false && intensityRatioAll.get(RoiCounter).isNaN() == false)
				{
					validIRcount++;
					totalIR += intensityRatioAll.get(RoiCounter);
					System.out.println("totalIR = " + totalIR);
					
				}
				else
					System.out.println("intensityRatioAll(m) is not a real number");
				
				RoiCounter++;
			}
			
			// avgIRvalid = totalIR/validIRcount;
			// avgIRinvalid = totalIR/foundRois2.size();
			// System.out.println("validIRcount = " + validIRcount +
			// "; cell count = " + foundRois2.size());
			// System.out.println("totalIR = " + totalIR);
			// System.out.println("avgIR valid IR count = " + avgIRvalid);
			// System.out.println("avgIR invalid IR count = " + avgIRinvalid);
			
			// im_visu.setOverlay(overlay);
			// im_visu.show();
			//
			// ColorProcessor colorVisu = (ColorProcessor)
			// im_visu.getProcessor().convertToRGB();
			// ipCytoplasm.setOverlay(overlay);
			// ipCytoplasm.show();
			
			// ShortProcessor p65_copy = (ShortProcessor)
			// shortImPlus2.duplicate();
			// ImagePlus im_p65_copy = new ImagePlus("p65_copy for pixel look",
			// p65_copy);
			
			// Separate the "visual image" from the actual data info from the
			// ROIs
			// We want an image of just the ROIs (nuclei) taken from the p65
			// image
			
			// double[] roiIntDen = new double[foundRois1.size()];
			// int u, v;
			// float[] roiTotalIntensity = new float[foundRois1.size()];
			// int[] pixelCount = new int[foundRois1.size()];
			//
			// System.out.println("Max pixel value = " + p65_copy.maxValue());
			//
			// for (int mm = 0; mm < foundRois1.size(); mm++){
			// Roi roi = foundRois1.get(mm);
			//
			// // Outline display image
			// roi.setStrokeColor(Color.yellow);
			// overlay.add((Roi)roi.clone());
			//
			// p65_copy.setRoi(roi);
			// ShortProcessor statcrop = (ShortProcessor) p65_copy.crop();
			//
			// Calibration cal = new Calibration(im_p65_copy);
			// ImageStatistics stats = ImageStatistics.getStatistics(statcrop,
			// Measurements.INTEGRATED_DENSITY|Measurements.AREA|Measurements.MEAN|Measurements.PERIMETER,
			// cal);
			//
			// ImageProcessor roiMask = roi.getMask();
			//
			// for (v = 0; v < roiMask.getHeight(); v++) {
			// for (u = 0; u < roiMask.getWidth(); u++) {
			// if (roiMask.getPixelValue(u, v) != 0) {
			//
			// roiTotalIntensity[mm] += statcrop.getPixelValue(u, v);
			// pixelCount[mm]++;
			//
			// totalNucleiIntensity += statcrop.getPixelValue(u, v);
			// totalNucleiArea++;
			// }
			// }
			// }
			// roiIntDen[mm] = stats.area;
			//
			// }
			
			int imWidth = imPlus1.getWidth();
			int imHeight = imPlus1.getHeight();
			
			ShortProcessor p65Nuclei = new ShortProcessor(imPlus1.getWidth(), imPlus1.getHeight());
			ShortProcessor p65Cytoplasm = new ShortProcessor(imPlus1.getWidth(), imPlus1.getHeight());
			
			for (v = 0; v < imHeight; v++)
			{
				for (u = 0; u < imWidth; u++)
				{
					if(byteImPlus1.getPixelValue(u, v) != 0)
					{
						
						p65Nuclei.putPixel(u, v, shortOriginalImPlus2.getPixel(u, v));
					}
					
					if(cytoplasm.getPixelValue(u, v) != 0)
					{
						p65Cytoplasm.putPixel(u, v, shortOriginalImPlus2.getPixel(u, v));
					}
				}
			}
			
			ImagePlus im_p65_nuclei = new ImagePlus("p65 nuclei only", p65Nuclei);
			// im_p65_nuclei.show();
			
			ImagePlus im_p65_cytoplasm = new ImagePlus("p65 cytoplasm only", p65Cytoplasm);
			// im_p65_cytoplasm.show();
			
			// ImagePlus im_p65_nuclei_overlay = new
			// ImagePlus("p65 nuclei overlay", p65Nuclei);
			// im_p65_nuclei_overlay.setOverlay(overlay);
			// im_p65_nuclei_overlay.show();
			
			// // Save the results
			ByteProcessor bim1 = (ByteProcessor) cytoplasm.duplicate();
			FloatProcessor fim2 = (FloatProcessor) im_p65_nuclei.getProcessor().convertToFloat();
			FloatProcessor fim3 = (FloatProcessor) im_p65_cytoplasm.getProcessor().convertToFloat();
			
			// *********************************
			// Output files to JEX and to folders
			// *********************************
			
			// File imFile = new File(path1);
			// String currentName = imFile.getName();
			
			// String localDir = JEXStatics.jexManager.getLocalFolder(entry);
			// String localDir = JEXWriter.getEntryFolder(entry);
			// String newFileName1 = FunctionUtility.getNextName(localDir,
			// currentName, "MASK-");
			// String finalPath1 = localDir + File.separator + newFileName1;
			// Logs.log("Saving the image to path "+
			// finalPath1, 1, this);
			// FunctionUtility.imSave(bim1, finalPath1);
			
			Logs.log("Saving the Mask", 1, this);
			String finalPath1 = JEXWriter.saveImage(bim1);
			JEXData JEXbim1 = ImageWriter.makeImageObject(outputNames[0].getName(), finalPath1);
			realOutputs.add(JEXbim1);
			
			// String newFileName2 = FunctionUtility.getNextName(localDir,
			// currentName, "NUC-");
			// String finalPath2 = localDir + File.separator + newFileName2;
			// Logs.log("Saving the image to path "+
			// finalPath2, 1, this);
			// FunctionUtility.imSave(fim2,"false", 16, finalPath2);
			
			Logs.log("Saving the Nucular mask", 1, this);
			ImagePlus toSave = FunctionUtility.makeImageToSave(fim2, "false", 16);
			String finalPath2 = JEXWriter.saveImage(toSave);
			JEXData JEXfim2 = ImageWriter.makeImageObject(outputNames[1].getName(), finalPath2);
			realOutputs.add(JEXfim2);
			
			// String newFileName3 = FunctionUtility.getNextName(localDir,
			// currentName, "CYTO-");
			// String finalPath3 = localDir + File.separator + newFileName3;
			// Logs.log("Saving the image to path "+
			// finalPath3, 1, this);
			// FunctionUtility.imSave(fim3,"false", 16, finalPath3);
			
			Logs.log("Saving the Cytoplasmic mask", 1, this);
			ImagePlus toSave2 = FunctionUtility.makeImageToSave(fim3, "false", 16);
			String finalPath3 = JEXWriter.saveImage(toSave2);
			JEXData JEXfim3 = ImageWriter.makeImageObject(outputNames[2].getName(), finalPath3);
			realOutputs.add(JEXfim3);
			
			// JEXData JEXintensityRatio =
			// ValueWriter.makeValueStack(outputNames[3], "" +
			// IntensityRatioAll, dimensionName);
			// realOutputs.add(JEXintensityRatio);
			// makeValueObject(outputNames[4], "" + avgIR);
			
			//
			// /*
			// String localDir =
			// JEXStatics.fileManager.getLocalDirectory(entry);
			//
			// // Output Image of Hoechst masks
			// String newFileName1 = FunctionUtility.getNextName(localDir,
			// jim1.getFileName(), "MASK-");
			// Logs.log("Saving in " + localDir +
			// File.separator + newFileName1 ,1, this);
			// FunctionUtility.imSave(bim1, localDir + File.separator +
			// newFileName1);
			// JEXImage outputJim1 = new JEXImage(localDir + File.separator +
			// newFileName1, jim1.getDimensionmap().duplicate());
			// output1.addData(outputJim1);
			//
			// // Output Image of p65 intensities
			// String newFileName2 = FunctionUtility.getNextName(localDir,
			// jim1.getFileName(), "NUC-");
			// Logs.log("Saving in " + localDir +
			// File.separator + newFileName2 ,1, this);
			// FunctionUtility.imSave(fim2, "false", 16, localDir +
			// File.separator + newFileName2);
			// JEXImage outputJim2 = new JEXImage(localDir + File.separator +
			// newFileName2, jim1.getDimensionmap().duplicate());
			// output2.addData(outputJim2);
			//
			// // Output Image of p65 intensities
			// String newFileName3 = FunctionUtility.getNextName(localDir,
			// jim1.getFileName(), "CYTO-");
			// Logs.log("Saving in " + localDir +
			// File.separator + newFileName3 ,1, this);
			// FunctionUtility.imSave(fim3, "false", 16, localDir +
			// File.separator + newFileName3);
			// JEXImage outputJim3 = new JEXImage(localDir + File.separator +
			// newFileName3, jim1.getDimensionmap().duplicate());
			// output3.addData(outputJim3);
			//
			//
			// // // Output Image of p65 intensities
			// // String newFileName3 = FunctionUtility.getNextName(localDir,
			// jim1.getFileName(), "Color-");
			// // Logs.log("Saving in " + localDir +
			// File.separator + newFileName3 ,1, this);
			// // FunctionUtility.imSave(colorVisu, localDir + File.separator +
			// newFileName3);
			//
			// */
			// Output Excel Results
			int ii = i + 1;
			String localDir = JEXWriter.getEntryFolder(entry, false, false);
			String sFileName = ("" + localDir + File.separator + "R1-Excel-Results-" + ii + ".csv");
			try
			{
				FileWriter writer = new FileWriter(sFileName);
				
				// Add label
				writer.append("Roi No. ,");
				writer.append("Cytoplasm Int ,");
				writer.append("Nucleus Int ,");
				writer.append("Avg Cytoplasm Int ,");
				writer.append("Avg Nucleus Int ,");
				writer.append("Cytoplasm Area ,");
				writer.append("Nucleus Area ,");
				writer.append("Total Area ,");
				writer.append("Intensity Ratio \n");
				
				for (int jj = 0; jj < foundRois2.size(); jj++)
				{
					writer.append("" + jj + ",");
					writer.append("" + cytoplasmIntensity[jj] + ",");
					writer.append("" + nucleusIntensity[jj] + ",");
					writer.append("" + avgCytoplasmIntensity[jj] + ",");
					writer.append("" + avgNucleusIntensity[jj] + ",");
					writer.append("" + cytoplasmPixelCount[jj] + ",");
					writer.append("" + nucleusPixelCount[jj] + ",");
					writer.append("" + totalPixelCount[jj] + ",");
					writer.append("" + intensityRatio[jj] + "\n");
					
				}
				
				writer.flush();
				writer.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			Logs.log("Saving in " + localDir + File.separator + sFileName, 1, this);
			
		}
		
		avgIR = totalIR / validIRcount;
		System.out.println("avgIR = " + avgIR);
		
		JEXData JEXavgIRvalid = ValueWriter.makeValueObject(outputNames[4].getName(), "" + avgIR);
		realOutputs.add(JEXavgIRvalid);
		
		// String localDir = JEXStatics.jexManager.getLocalFolder(entry);
		String localDir = JEXWriter.getEntryFolder(entry, false, false);
		String sFileName2 = ("" + localDir + File.separator + "Excel-Results-ALL.csv");
		try
		{
			FileWriter writer2 = new FileWriter(sFileName2);
			
			// Add label
			writer2.append("Roi No. ,");
			writer2.append("Cytoplasm Int ,");
			writer2.append("Nucleus Int ,");
			writer2.append("Avg Cytoplasm Int ,");
			writer2.append("Avg Nucleus Int ,");
			writer2.append("Cytoplasm Area ,");
			writer2.append("Nucleus Area ,");
			writer2.append("Total Area ,");
			writer2.append("Intensity Ratio \n");
			
			for (int jj = 0; jj < totalRoiCount; jj++)
			{
				writer2.append("" + jj + ",");
				writer2.append("" + cytoplasmIntensityAll.get(jj) + ",");
				writer2.append("" + nucleusIntensityAll.get(jj) + ",");
				writer2.append("" + avgCytoplasmIntensityAll.get(jj) + ",");
				writer2.append("" + avgNucleusIntensityAll.get(jj) + ",");
				writer2.append("" + cytoplasmPixelCountAll.get(jj) + ",");
				writer2.append("" + nucleusPixelCountAll.get(jj) + ",");
				writer2.append("" + totalPixelCountAll.get(jj) + ",");
				writer2.append("" + intensityRatioAll.get(jj) + "\n");
				
			}
			
			writer2.flush();
			writer2.close();
		}
		
		catch (IOException e)
		{
			e.printStackTrace();
		}
		Logs.log("Saving in " + localDir + File.separator + sFileName2, 1, this);
		
		// Return status
		return true;
		
	}
	
}
