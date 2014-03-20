package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.gui.EllipseRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.RoiEncoder;
import ij.measure.Measurements;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import image.roi.ROIPlus;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jex.statics.JEXStatics;
import miscellaneous.CSVList;
import miscellaneous.LSVList;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.RoiWriter;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.ExperimentalDataCrunch;
import function.imageUtility.AutoThresholder;
import function.imageUtility.JEXUtility_ParticleAnalyzer;
import function.imageUtility.WatershedUtility;

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
public class JEX_ET_Quantification extends ExperimentalDataCrunch {
	
	public JEX_ET_Quantification()
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
		String result = "ET identification";
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
		String result = "Quantification of the number of cells undergoing ET formation through time.";
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
		String toolbox = "ET Toolbox";
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
		TypeName[] inputNames = new TypeName[1];
		inputNames[0] = new TypeName(IMAGE, "DNA Stain Image");
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
		this.defaultOutputNames = new TypeName[8];

		this.defaultOutputNames[0] = new TypeName(IMAGE, "Outlined Cells");
		this.defaultOutputNames[1] = new TypeName(ROI, "All Cells");
		this.defaultOutputNames[2] = new TypeName(ROI, "Cells ET");
		this.defaultOutputNames[3] = new TypeName(ROI, "Cells alive");
		this.defaultOutputNames[4] = new TypeName(VALUE, "Number of cells");
		this.defaultOutputNames[5] = new TypeName(VALUE, "Number of ET cells");
		this.defaultOutputNames[6] = new TypeName(VALUE, "Number of live cells");
		this.defaultOutputNames[7] = new TypeName(FILE, "Measurement tables");
		
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
		Parameter p0 = new Parameter("Thesholding method", "Method of automatically choosing the threshold.", Parameter.DROPDOWN, new String[] { "Huang", "Intermodes", "IsoData", "Li", "MaxEntropy", "Mean", "MinError(I)", "Minimum", "Moments", "Otsu", "Percentile", "RenyiEntropy", "Shanbhag", "Triangle", "Yen" });
		Parameter p1 = new Parameter("Particles Are White?", "Are particles dark or light?", Parameter.DROPDOWN, new String[] { "true" , "false" });
		Parameter p2 = new Parameter("Min Size", "Min particle size [pixels^2]", "10");
		Parameter p3 = new Parameter("Max Size", "Max particle size [pixels^2]", "10000000");
		Parameter p4 = new Parameter("Min ET size", "Minimum size at which a detected cell will be counted as an ET", "100");
		//Parameter p5 = new Parameter("Max ET intensity", "Maximum average intensity at which a detected cell will be counted as an ET", "180");
		Parameter p6 = new Parameter("Max Stdev", "Maximum standard deviation of the intensities for counting a cell as an ET", "20");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		//parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
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
		// Collect the inputs
		JEXData stainData = inputs.get("DNA Stain Image");
		if(stainData == null || !stainData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		// Convert the inputs into imageJ format
		TreeMap<DimensionMap, String> imageMap = ImageReader.readObjectToImagePathTable(stainData);
		
		// Gather parameters
		String threshMethod       = this.parameters.getValueOfParameter("Thesholding method");
		boolean particlesAreWhite = Boolean.parseBoolean(this.parameters.getValueOfParameter("Particles Are White?"));
		double minSize            = Double.parseDouble(this.parameters.getValueOfParameter("Min Size"));
		double maxSize            = Double.parseDouble(this.parameters.getValueOfParameter("Max Size"));
		double minETSize          = Double.parseDouble(this.parameters.getValueOfParameter("Min ET size"));
		//double maxETIntensity     = Double.parseDouble(this.parameters.getValueOfParameter("Max ET intensity"));
		double maxETStDev         = Double.parseDouble(this.parameters.getValueOfParameter("Max Stdev"));
		
		// ----------------
		// Run the function
		// ----------------
		
		// Set up the outputs of the function
		TreeMap<DimensionMap, String> outlineImageMap         = new TreeMap<DimensionMap, String>();
		TreeMap<DimensionMap, List<ROIPlus>> outlineRoiMap    = new TreeMap<DimensionMap, List<ROIPlus>>();
		TreeMap<DimensionMap, List<ROIPlus>> outlineLiveRoiMap= new TreeMap<DimensionMap, List<ROIPlus>>();
		TreeMap<DimensionMap, List<ROIPlus>> outlineETRoiMap  = new TreeMap<DimensionMap, List<ROIPlus>>();
		TreeMap<DimensionMap, Double> nbOfCellsMap            = new TreeMap<DimensionMap, Double>();
		TreeMap<DimensionMap, Double> nbOfLiveCellsMap        = new TreeMap<DimensionMap, Double>();
		TreeMap<DimensionMap, Double> nbOfETCellsMap          = new TreeMap<DimensionMap, Double>();
		TreeMap<DimensionMap, String> measurementTableMap     = new TreeMap<DimensionMap, String>();
		
		// Loop through the images
		Set<DimensionMap> keys = imageMap.keySet();
		int count = 0, percentage = 0;
		
		// Set the threshold 
		
		for (DimensionMap key: keys)
		{
			// Get the image path at that key
			String imagePath = imageMap.get(key);
			
			// Open the image at that path
			ImagePlus      grayIm  = new ImagePlus(imagePath);
			ImageProcessor grayImp = grayIm.getProcessor();
			
			// Convert to byte
			ByteProcessor thresholded = (ByteProcessor) grayImp.duplicate().convertToByte(false);
			
			// Find the threshold level
			int[] hist         = thresholded.getHistogram();
			AutoThresholder at = new AutoThresholder();
			int threshold      = at.getThreshold(threshMethod, hist);
			
			// Make the binary thresholded image
			thresholded.threshold(threshold);

			// Watershed the mask
			WatershedUtility ws = new WatershedUtility();
			thresholded = ws.watershed(thresholded);
			
			// Run the particle analyzer
			JEXUtility_ParticleAnalyzer pa = new JEXUtility_ParticleAnalyzer(grayImp, thresholded, particlesAreWhite, minSize, maxSize, 0.0, 1.0);
			
			// Collect results
			//HashMap<ROIPlus, HashMap<String,Double>> results = pa.getMeasurements();
			ImagePlus outline                                = pa.getOutlineImage();
			ArrayList<Roi> foundRois						 = pa.getFoundRois();
			LinkedHashMap<ROIPlus, HashMap<String,Double>> results = this.analyzeImage(foundRois, grayIm);
			
			// Save the results
			String[] outputPaths = this.saveResults(results);
			
			// Set the rois for this frame
			List<ROIPlus> allRois  = new ArrayList<ROIPlus>() ;
			List<ROIPlus> etRois   = new ArrayList<ROIPlus>() ;
			List<ROIPlus> liveRois = new ArrayList<ROIPlus>() ;
			
			// Add the ROIS to the output list
			Set<ROIPlus> rois = results.keySet();	
			int i = 0;
			for (ROIPlus roi:rois)
			{
				// Get the meaaures
				HashMap<String,Double> stats = results.get(roi);
				
				// Get the characteristics of the current ROI
				double area  = stats.get("AREA");
				//double mean  = stats.get("MEAN");
				//double min   = stats.get("MIN");
				//double max   = stats.get("MAX");
				double stdev = stats.get("STDEV");
				
				// Get the corresponding roi from the particle analyzer
				Roi polyroi  = foundRois.get(i);
				ROIPlus proi = new ROIPlus(polyroi);
				i ++;
				
				// If the ROI meets the requirements for an ET add it to that list else add to the live cells
				//if (area > minETSize && max < maxETIntensity)
				if (area > minETSize && stdev < maxETStDev)
				{
					// then it's an ET
					etRois.add(proi);
				}
				else
				{
					// else it's a live cell
					liveRois.add(proi);
				}
				
				// add this roi to the list of rois
				allRois.add(proi);
			}
			
			// Make the outline image
			outline = makeOutlineImage(etRois, liveRois, grayIm);
			
			// Full the roi outputs
			outlineRoiMap.put(key, allRois);
			outlineETRoiMap.put(key, etRois);
			outlineLiveRoiMap.put(key, liveRois);
			
			// Fill the value outputs
			nbOfCellsMap.put(key, (double) allRois.size());
			nbOfETCellsMap.put(key, (double) etRois.size());
			nbOfLiveCellsMap.put(key, (double) liveRois.size());
			
			// Add the image to the output list
			outlineImageMap.put(key, JEXWriter.saveImage(outline));
			
			// Add the measurement table to the list
			measurementTableMap.put(key, outputPaths[0]);
			
			// Display the advancement
			count      = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		// Convert the outputted data back into JEX format
		JEXData output1 = ImageWriter.makeImageStackFromPaths(this.outputNames[0].getName(), outlineImageMap);
		JEXData output2 = RoiWriter.makeRoiObjectFromListMap(this.outputNames[1].getName(), outlineRoiMap);
		JEXData output3 = RoiWriter.makeRoiObjectFromListMap(this.outputNames[2].getName(), outlineETRoiMap);
		JEXData output4 = RoiWriter.makeRoiObjectFromListMap(this.outputNames[3].getName(), outlineLiveRoiMap);
		JEXData output5 = ValueWriter.makeValueTableFromDouble(this.outputNames[4].getName(), nbOfCellsMap);
		JEXData output6 = ValueWriter.makeValueTableFromDouble(this.outputNames[5].getName(), nbOfETCellsMap);
		JEXData output7 = ValueWriter.makeValueTableFromDouble(this.outputNames[6].getName(), nbOfLiveCellsMap);
		JEXData output8 = FileWriter.makeFileTable(this.outputNames[7].getName(), measurementTableMap);
		
		// Set the outputs
		this.realOutputs.add(output1);
		this.realOutputs.add(output2);
		this.realOutputs.add(output3);
		this.realOutputs.add(output4);
		this.realOutputs.add(output5);
		this.realOutputs.add(output6);
		this.realOutputs.add(output7);
		this.realOutputs.add(output8);
		
		// Return status
		return true;
	}
	
	private LinkedHashMap<ROIPlus, HashMap<String,Double>> analyzeImage(ArrayList<Roi> rois, ImagePlus im)
	{
		// Find the center of masses of the rois
		List<Point> points = new ArrayList<Point>();
		List<Roi>   cRois  = new ArrayList<Roi>();
		for (Roi roi: rois)
		{
			// Make the ROIPlus
			PolygonRoi proi = (PolygonRoi) roi;
			
			// Do the measures
			int myMeasurements  = Measurements.CENTER_OF_MASS ;
			im.setRoi(proi);
			ImageStatistics stats = im.getStatistics(myMeasurements);
			
			// Prepare the measure variables
			double x    = stats.xCenterOfMass;
			double y    = stats.yCenterOfMass;
			
			// put the points and rois into the list
			points.add(new Point((int) x, (int) y));
			EllipseRoi croi = new EllipseRoi(x-12, y-12, x+12, y+12, 1);
			cRois.add(croi);
		}
		
		// Create the output
		LinkedHashMap<ROIPlus, HashMap<String,Double>> result = new LinkedHashMap<ROIPlus, HashMap<String,Double>>();
		
		// Loop through the rois
		for (Roi roi:cRois)
		{
			// Make the ROIPlus
//			PolygonRoi proi = (PolygonRoi) roi;
			EllipseRoi eroi = (EllipseRoi) roi;
			ROIPlus roip    = new ROIPlus(eroi);
			
			// Make the Hash for this roi
			HashMap<String,Double> measure = new HashMap<String,Double>();
			
			// Prepare the measure variables
			double area = 0.0;
			double mean = 0.0;
			double min  = 0.0;
			double max  = 0.0;
			double x    = 0.0;
			double y    = 0.0;
			double circ = 0.0;
			double peri = 0.0;
			double stdv = 0.0;
			
			// Do the measures
			int myMeasurements  = Measurements.CENTROID + Measurements.AREA + Measurements.CENTER_OF_MASS + Measurements.CIRCULARITY + Measurements.MEAN + Measurements.MIN_MAX;
			im.setRoi(eroi);
			ImageStatistics stats = im.getStatistics(myMeasurements);
			
			// Extract the measures
			area = stats.area;
			mean = stats.mean;
			x    = stats.xCenterOfMass;
			y    = stats.yCenterOfMass;
			min  = stats.min;
			max  = stats.max;
			peri = eroi.getLength();
			circ = peri == 0.0 ? 0.0 : 4.0 * Math.PI * (stats.pixelCount / (peri * peri));
			stdv = stats.stdDev;
			
			// Add the measures to the MEASURE hash
			measure.put("AREA", area);
			measure.put("MEAN", mean);
			measure.put("MIN", min);
			measure.put("MAX", max);
			measure.put("CIRC", circ);
			measure.put("PERI", peri);
			measure.put("X", x);
			measure.put("Y", y);
			measure.put("STDEV", stdv);
			
			// Add the MEASURE hash to the main hash
			result.put(roip, measure);
		}
		
		// Return the result
		return result;
	}
	
	private ImagePlus makeOutlineImage(List<ROIPlus> et, List<ROIPlus> live, ImagePlus image)
	{
		// Make the background imag
		ColorProcessor cimp = (ColorProcessor) image.getProcessor().duplicate().convertToRGB();
		BufferedImage  bimp = cimp.getBufferedImage();
				
		// Get graphics
		Graphics g = bimp.createGraphics();
		
		// Plot the live rois
		g.setColor(Color.YELLOW);
		for (ROIPlus roip: live)
		{
			// Get the roi
			PolygonRoi proi = (PolygonRoi) roip.getRoi();
			
			// Display the roi
			java.awt.Polygon poly = proi.getPolygon();
			
			// Get the points
			int[] xpos = poly.xpoints;
			int[] ypos = poly.ypoints;
			
			// Loop through the points and draw them
			int x1 = xpos[0];
			int x2 ;
			int y1 = ypos[0];
			int y2 ;
			for (int i=1; i<xpos.length-1; i++)
			{
				// Get point
				x2 = xpos[i];
				y2 = ypos[i];
				
				// Draw point and line
				g.fillRect(x2-1, y2-1, 2, 2);
				g.drawLine(x1, y1, x2, y2);
				
				// Update points
				x1 = x2;
				y1 = y2;
			}
			
			//g.drawPolygon(poly);
			//proi.drawOverlay(g);
		}
		
		// Plot the et rois
		g.setColor(Color.RED);
		for (ROIPlus roip: et)
		{
			// Get the roi
			PolygonRoi proi = (PolygonRoi) roip.getRoi();

			// Display the roi
			java.awt.Polygon poly = proi.getPolygon();

			// Get the points
			int[] xpos = poly.xpoints;
			int[] ypos = poly.ypoints;

			// Loop through the points and draw them
			int x1 = xpos[0];
			int x2 ;
			int y1 = ypos[0];
			int y2 ;
			for (int i=1; i<xpos.length-1; i++)
			{
				// Get point
				x2 = xpos[i];
				y2 = ypos[i];
				
				// Draw point and line
				g.fillRect(x2-1, y2-1, 2, 2);
				g.drawLine(x1, y1, x2, y2);
				
				// Update points
				x1 = x2;
				y1 = y2;
			}
			
			//g.drawPolygon(poly);
			//proi.drawOverlay(g);
		}
		
		// Release the graphics handle
		g.dispose();
		
		// Make output imageplus
		ImagePlus retIm = new ImagePlus("Outlines", bimp);
		return retIm;
	}
	
	private String[] saveResults(LinkedHashMap<ROIPlus, HashMap<String,Double>> results)
	{
		LSVList csv = new LSVList();
		
		// Setup useful variables
		boolean firstLine = false ;
		String[] measures = new String[0];
		
		for (ROIPlus roip : results.keySet())
		{
			HashMap<String,Double> m = results.get(roip);
			
			// Get the measurements available in this hash
			if (!firstLine)
			{
				// get all the keys and put them in the first line
				Set<String> keys = m.keySet(); 
				measures         = keys.toArray(new String[0]);
				CSVList line     = new CSVList(measures);
				csv.add(line.toString());
				
				// don't go back here again
				firstLine = true;
			}
			
			// Fill the next lines of the table
			String[] res = new String[measures.length];
			for (int i=0; i<measures.length; i++)
			{
				double d = m.get(measures[i]);
				res[i]   = ""+d;
			}
			CSVList line = new CSVList(res);
			csv.add(line.toString());
		}
		
		// Save the results table
		String csvPath = JEXWriter.saveText(csv.toString(), "csv");
		
		// Save the rois
		Vector<ROIPlus> roips = new Vector<ROIPlus>();
		for (ROIPlus roip : results.keySet())
		{
			roips.add(roip);
		}
		String roiPath = this.saveRois(roips);
		
		// Return the results
		return new String[] { csvPath, roiPath };
	}
	
	private String saveRois(List<ROIPlus> roips)
	{
		if(roips == null || roips.size() == 0)
		{
			return null;
		}
		String path = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("zip");
		try
		{
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
			RoiEncoder re = new RoiEncoder(out);
			int i = 1;
			String label;
			Roi roi;
			for (ROIPlus roip : roips)
			{
				roi = roip.getRoi();
				label = "" + i + ".roi";
				zos.putNextEntry(new ZipEntry(label));
				re.write(roi);
				out.flush();
				i++;
			}
			out.close();
			return path;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
}