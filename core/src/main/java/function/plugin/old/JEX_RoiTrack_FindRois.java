package function.plugin.old;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.RoiWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import function.imageUtility.AutoThresholder;
import function.imageUtility.JEXUtility_ParticleAnalyzer;
import function.imageUtility.WatershedUtility;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import image.roi.ROIPlus;
import jex.statics.JEXStatics;
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
public class JEX_RoiTrack_FindRois extends JEXCrunchable {

	public JEX_RoiTrack_FindRois()
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
		String result = "Find Rois";
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
		String result = "Find ROIs in the images composed of gray scale cells on a black background.";
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
		String toolbox = "Roi tracking";
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
		inputNames[0] = new TypeName(IMAGE, "Grayscale Image");
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
		this.defaultOutputNames = new TypeName[2];

		this.defaultOutputNames[0] = new TypeName(IMAGE, "Outlined Cells");
		this.defaultOutputNames[1] = new TypeName(ROI, "Cells");

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
		Parameter p4 = new Parameter("Min Circularity", "Min circularity of the particle", "0");
		Parameter p5 = new Parameter("Max Circularity", "Max circularity of the particle", "1");
		Parameter p6 = new Parameter("Watershed?", "Proceed to a watershed after the thresholding", Parameter.DROPDOWN, new String[] { "true" , "false" });

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
		JEXData data = inputs.get("Grayscale Image");
		if(data == null || !data.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}

		// Convert the inputs into imageJ format
		TreeMap<DimensionMap, String> imageMap = ImageReader.readObjectToImagePathTable(data);

		// Gather parameters
		String threshMethod       = this.parameters.getValueOfParameter("Thesholding method");
		boolean particlesAreWhite = Boolean.parseBoolean(this.parameters.getValueOfParameter("Particles Are White?"));
		double minSize            = Double.parseDouble(this.parameters.getValueOfParameter("Min Size"));
		double maxSize            = Double.parseDouble(this.parameters.getValueOfParameter("Max Size"));
		//		double minCirc            = Double.parseDouble(this.parameters.getValueOfParameter("Min Circularity"));
		//		double maxCirc            = Double.parseDouble(this.parameters.getValueOfParameter("Max Circularity"));
		boolean watershed         = Boolean.parseBoolean(this.parameters.getValueOfParameter("Watershed?"));

		// ----------------
		// Run the function
		// ----------------

		// Set up the outputs of the function
		TreeMap<DimensionMap, String> outlineImageMap  = new TreeMap<DimensionMap, String>();
		TreeMap<DimensionMap, List<ROIPlus>> roiMap    = new TreeMap<DimensionMap, List<ROIPlus>>();

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
			if (watershed)
			{
				WatershedUtility ws = new WatershedUtility();
				thresholded = ws.watershed(thresholded);
			}

			// Run the particle analyzer
			JEXUtility_ParticleAnalyzer pa = new JEXUtility_ParticleAnalyzer(grayImp, thresholded, particlesAreWhite, minSize, maxSize, 0.0, 1.0);

			// Collect results
			ArrayList<Roi> foundRois = pa.getFoundRois();

			// Set the rois for this frame
			List<ROIPlus> rois  = new ArrayList<ROIPlus>() ;
			for (Roi roi:foundRois)
			{
				// add this roi to the list of rois
				rois.add(new ROIPlus(roi));
			}

			// Make the outline image
			ImagePlus outline = makeOutlineImage(rois, grayIm);

			// Full the roi outputs
			roiMap.put(key, rois);

			// Add the image to the output list
			outlineImageMap.put(key, JEXWriter.saveImage(outline));

			// Display the advancement
			count      = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}

		// Convert the outputted data back into JEX format
		JEXData output1 = ImageWriter.makeImageStackFromPaths(this.outputNames[0].getName(), outlineImageMap);
		JEXData output2 = RoiWriter.makeRoiObjectFromListMap(this.outputNames[1].getName(), roiMap);

		// Set the outputs
		this.realOutputs.add(output1);
		this.realOutputs.add(output2);

		// Return status
		return true;
	}

	private ImagePlus makeOutlineImage(List<ROIPlus> rois, ImagePlus image)
	{
		// Make the background imag
		ColorProcessor cimp = (ColorProcessor) image.getProcessor().duplicate().convertToRGB();
		BufferedImage  bimp = cimp.getBufferedImage();

		// Get graphics
		Graphics g = bimp.createGraphics();

		// Plot the live rois
		g.setColor(Color.YELLOW);
		for (ROIPlus roip: rois)
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
		}

		// Release the graphics handle
		g.dispose();

		// Make output imageplus
		ImagePlus retIm = new ImagePlus("Outlines", bimp);
		return retIm;
	}

	//	private String saveRois(List<ROIPlus> roips)
	//	{
	//		if(roips == null || roips.size() == 0)
	//		{
	//			return null;
	//		}
	//		String path = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("zip");
	//		try
	//		{
	//			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path));
	//			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
	//			RoiEncoder re = new RoiEncoder(out);
	//			int i = 1;
	//			String label;
	//			Roi roi;
	//			for (ROIPlus roip : roips)
	//			{
	//				roi = roip.getRoi();
	//				label = "" + i + ".roi";
	//				zos.putNextEntry(new ZipEntry(label));
	//				re.write(roi);
	//				out.flush();
	//				i++;
	//			}
	//			out.close();
	//			return path;
	//		}
	//		catch (IOException e)
	//		{
	//			e.printStackTrace();
	//			return null;
	//		}
	//	}
}