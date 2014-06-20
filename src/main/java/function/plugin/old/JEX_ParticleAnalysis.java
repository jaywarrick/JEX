package function.plugin.old;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.RoiEncoder;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import image.roi.ROIPlus;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import function.imageUtility.JEXUtility_ParticleAnalyzer;

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
public class JEX_ParticleAnalysis extends JEXCrunchable {
	
	public JEX_ParticleAnalysis()
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
		String result = "Particle Analysis";
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
		String result = "Analyze particles in a binary image or use the binary image to drive analysis of a grayscale image.";
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
		TypeName[] inputNames = new TypeName[3];
		inputNames[0] = new TypeName(IMAGE, "Binary Image");
		inputNames[1] = new TypeName(ROI, "ROI");
		inputNames[2] = new TypeName(IMAGE, "Grayscale Image (optional)");
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
		this.defaultOutputNames = new TypeName[3];
		this.defaultOutputNames[0] = new TypeName(IMAGE, "Outline Image");
		this.defaultOutputNames[1] = new TypeName(FILE, "ROI File");
		this.defaultOutputNames[2] = new TypeName(FILE, "CSV Data");
		
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
		Parameter p0 = new Parameter("Particles Are White?", "Are the particles white on a black background?", Parameter.DROPDOWN, new String[] { "True", "False" }, 0);
		Parameter p1 = new Parameter("Min Size", "Min particle size [pixels^2]", "0");
		Parameter p2 = new Parameter("Max Size", "Max particle size [pixels^2]", "10000000");
		Parameter p3 = new Parameter("Min Circularity", "Min circularity of the particle", "0");
		Parameter p4 = new Parameter("Max Circularity", "Max circularity of the particle", "1");
		Parameter p5 = new Parameter("Exclude On Edges?", "Exclude particles that touch the edge of the image?", Parameter.DROPDOWN, new String[] { "True", "False" }, 0);
		Parameter p6 = new Parameter("Fill Holes?", "Fill holes inside particles?", Parameter.DROPDOWN, new String[] { "True", "False" }, 0);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
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
		JEXData binaryData = inputs.get("Binary Image");
		if(binaryData == null || !binaryData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		JEXData roiData = inputs.get("ROI");
		if(roiData != null && !roiData.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}
		JEXData grayData = inputs.get("Grayscale Image (optional)");
		if(grayData != null && !grayData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		// Gather parameters
		boolean particlesAreWhite = Boolean.parseBoolean(this.parameters.getValueOfParameter("Particles Are White?"));
		double minSize = Double.parseDouble(this.parameters.getValueOfParameter("Min Size"));
		double maxSize = Double.parseDouble(this.parameters.getValueOfParameter("Max Size"));
		double minCirc = Double.parseDouble(this.parameters.getValueOfParameter("Min Circularity"));
		double maxCirc = Double.parseDouble(this.parameters.getValueOfParameter("Max Circularity"));
		boolean excludeOnEdges = Boolean.parseBoolean(this.parameters.getValueOfParameter("Exclude On Edges?"));
		
		// Prepare output maps
		TreeMap<DimensionMap,String> outlineMap = new TreeMap<DimensionMap,String>(); // defaultOutputNames[0]
		// =
		// "Outline Image";
		TreeMap<DimensionMap,String> outputRoiMap = new TreeMap<DimensionMap,String>(); // defaultOutputNames[1]
		// =
		// "ROI File";
		TreeMap<DimensionMap,String> csvMap = new TreeMap<DimensionMap,String>(); // defaultOutputNames[2]
		// =
		// "CSV Data";
		
		// Get input maps
		TreeMap<DimensionMap,ROIPlus> roiMap = new TreeMap<DimensionMap,ROIPlus>();
		if(roiData != null)
		{
			roiMap = RoiReader.readObjectToRoiMap(roiData);
		}
		else
		{
			roiMap = new TreeMap<DimensionMap,ROIPlus>();
		}
		TreeMap<DimensionMap,String> grayMap = new TreeMap<DimensionMap,String>();
		if(grayData != null)
		{
			grayMap = ImageReader.readObjectToImagePathTable(grayData);
		}
		else
		{
			grayMap = new TreeMap<DimensionMap,String>();
		}
		TreeMap<DimensionMap,String> binaryMap = ImageReader.readObjectToImagePathTable(binaryData);
		
		// Run the function
		int count = 0, percentage = 0;
		Roi roi;
		ImagePlus grayIm;
		ImageProcessor gray;
		ROIPlus roip;
		String grayPath;

		ImagePlus outline ;
		HashMap<ROIPlus, HashMap<String,Double>> results;
		
		for (DimensionMap map : binaryMap.keySet())
		{
			ByteProcessor binary = (ByteProcessor) new ImagePlus(binaryMap.get(map)).getProcessor();
			
			roi = null;
			roip = roiMap.get(map);
			if(roip != null)
			{
				boolean isLine = roip.isLine();
				if(isLine)
				{
					return false;
				}
				roi = roip.getRoi();
				binary.setRoi(roi);
			}
			
			gray = null;
			grayPath = grayMap.get(map);
			if(grayPath != null)
			{
				grayIm = new ImagePlus(grayPath);
				if(grayIm != null)
				{
					gray = grayIm.getProcessor();
				}
			}
			
			// Run the particle analyzer
			JEXUtility_ParticleAnalyzer pa = new JEXUtility_ParticleAnalyzer(gray, binary, particlesAreWhite, minSize, maxSize, minCirc, maxCirc);

			// Collect results
			results = pa.getMeasurements();
			outline = pa.getOutlineImage();
			
			// Save results
			String[] outputPaths = this.saveResults(results);
			csvMap.put(map, outputPaths[0]);
			outputRoiMap.put(map, outputPaths[1]);
			outlineMap.put(map, JEXWriter.saveImage(outline));
			
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) binaryMap.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		if(outlineMap.size() == 0)
		{
			return false;
		}
		
		JEXData output1 = ImageWriter.makeImageStackFromPaths(this.outputNames[0].getName(), outlineMap);
		JEXData output2 = FileWriter.makeFileTable(JEXData.FILE, this.outputNames[1].getName(), outputRoiMap);
		JEXData output3 = FileWriter.makeFileTable(JEXData.FILE, this.outputNames[2].getName(), csvMap);
		
		// Set the outputs
		this.realOutputs.add(output1);
		this.realOutputs.add(output2);
		this.realOutputs.add(output3);
		
		// Return status
		return true;
	}
	
	private String[] saveResults(HashMap<ROIPlus, HashMap<String,Double>> results)
	{
		LSVList csv = new LSVList();
		CSVList line;
		line = new CSVList(new String[] { "ID", "X", "Y", "AREA", "MEAN", "PERIMETER", "CIRCULARITY" });
		csv.add(line.toString());
		int i = 1;
		double area, mean, perimeter, circularity, x, y;
		Vector<ROIPlus> roips = new Vector<ROIPlus>();
		
		for (ROIPlus roip : results.keySet())
		{
			roips.add(roip);
		}
		
		for (ROIPlus roip : roips)
		{
			HashMap<String,Double> stats = results.get(roip);
			
			x           = stats.get("X");
			y           = stats.get("Y");
			area        = stats.get("AREA");
			mean        = stats.get("MEAN");
			perimeter   = stats.get("PERI");
			circularity = stats.get("CIRC");
			
			line = new CSVList(new String[] { "" + i, "" + x, "" + y, "" + area, "" + mean, "" + perimeter, "" + circularity });
			csv.add(line.toString());
			i++;
		}
		String csvPath = JEXWriter.saveText(csv.toString(), "csv");
		String roiPath = this.saveRois(roips);
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