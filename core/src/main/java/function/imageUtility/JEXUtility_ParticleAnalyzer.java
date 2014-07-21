package function.imageUtility;

import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import image.roi.ROIPlus;

import java.util.ArrayList;
import java.util.HashMap;

import logs.Logs;

public class JEXUtility_ParticleAnalyzer {
	
	// Save a link to the original Particle analyzer
	ParticleAnalyzer pa ;
	
	// Output variables
	HashMap<ROIPlus, HashMap<String,Double>> measures ;
	ImagePlus outline ;
	
	public static final int DEFAULT_MEASUREMENTS = ImageStatistics.CENTER_OF_MASS | ImageStatistics.AREA | ImageStatistics.MEAN;
	
	public JEXUtility_ParticleAnalyzer(ImageProcessor grayScale, ByteProcessor blackAndWhite, boolean particlesAreWhite, double minSize, double maxSize, double minCirc, double maxCirc)
	{
		int options         = 0;
		ByteProcessor bw2   = (ByteProcessor) blackAndWhite.duplicate();
		
		// Set the options
		options |= EXCLUDE_EDGE_PARTICLES;
		options |= SHOW_OVERLAY_OUTLINES ;
		
		// Respond to the type of image mask
		if(particlesAreWhite)
		{
			bw2.invert();
		}
				
		// Make the results table
		ResultsTable rt = new ResultsTable();
		
		// Prepare the measurements
		int myMeasurements  = Measurements.CENTROID + Measurements.AREA + Measurements.CENTER_OF_MASS + Measurements.CIRCULARITY + Measurements.MEAN + Measurements.STD_DEV;
		
		// Create a particle analyzer
		pa = new ParticleAnalyzer(options, myMeasurements, rt, minSize, maxSize, minCirc, maxCirc);
	
		// Make an imageplus out of the b&w image to perform the traditional image analysis
		ImagePlus im = new ImagePlus("binary",bw2);
		
		// Perform the particle analysis
		pa.analyze(im);
		
		// Retrieve the data
		ArrayList<Roi> foundRois = pa.getFoundRois();
		outline                  = pa.getOutputImage();
		Logs.log("Total number of rois is " + foundRois.size(), 1, this);
		
		// Make measures specific to the found rois and the image
		measures = analyzeImage(foundRois, new ImagePlus("",grayScale));
	}
	
	/**
	 * Return the first roi in the list of rois ROIS that contains the point at location X,Y
	 * @param x
	 * @param y
	 * @param rois
	 * @return
	 */
	@SuppressWarnings("unused")
	private Roi findRoi(int x, int y, ArrayList<Roi> rois)
	{
		for (Roi roi: rois)
		{
			if (roi.contains(x, y))
			{
				return roi;
			}
		}
		return null;
	}
	
	private HashMap<ROIPlus, HashMap<String,Double>> analyzeImage(ArrayList<Roi> rois, ImagePlus im)
	{
		// Create the output
		HashMap<ROIPlus, HashMap<String,Double>> result = new HashMap<ROIPlus, HashMap<String,Double>>();
		
		// Loop through the rois
		for (Roi roi:rois)
		{
			// Make the ROIPlus
			PolygonRoi proi = (PolygonRoi) roi;
			ROIPlus roip    = new ROIPlus(proi);
			
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
			im.setRoi(proi);
			ImageStatistics stats = im.getStatistics(myMeasurements);
			
			// Extract the measures
			area = stats.area;
			mean = stats.mean;
			x    = stats.xCenterOfMass;
			y    = stats.yCenterOfMass;
			min  = stats.min;
			max  = stats.max;
			peri = proi.getLength();
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
	
	public ArrayList<Roi> getFoundRois()
	{
		return pa.getFoundRois();
	}
	
	public HashMap<ROIPlus, HashMap<String,Double>> getMeasurements()
	{
		return measures;
	}
	
	public ImagePlus getOutlineImage()
	{
		return outline;
		//return pa.getOutputImage();
	}
		
	
	
	
	
	
	
	
	
	
	/** Display results in the ImageJ console. */
	public static final int SHOW_RESULTS = 1;
	
	/** Obsolete */
	public static final int SHOW_SUMMARY = 2;
	
	/** Display image containing outlines of measured particles. */
	public static final int SHOW_OUTLINES = 4;
	
	/** Do not measure particles touching edge of image. */
	public static final int EXCLUDE_EDGE_PARTICLES = 8;
	
	/**
	 * Display image containing grayscales masks that identify measured particles.
	 */
	public static final int SHOW_ROI_MASKS = 16;
	
	/** Display a progress bar. */
	public static final int SHOW_PROGRESS = 32;
	
	/** Clear ImageJ console before starting. */
	public static final int CLEAR_WORKSHEET = 64;
	
	/**
	 * Record starting coordinates so outline can be recreated later using doWand(x,y).
	 */
	public static final int RECORD_STARTS = 128;
	
	/** Display a summary. */
	public static final int DISPLAY_SUMMARY = 256;
	
	/** Do not display particle outline image. */
	public static final int SHOW_NONE = 512;
	
	/** Flood fill to ignore interior holes. */
	public static final int INCLUDE_HOLES = 1024;
	
	/** Add particles to ROI Manager. */
	public static final int ADD_TO_MANAGER = 2048;
	
	/** Display image containing binary masks of measured particles. */
	public static final int SHOW_MASKS = 4096;
	
	/** Use 4-connected particle tracing. */
	public static final int FOUR_CONNECTED = 8192;
	
	/** Replace original image with masks. */
	public static final int IN_SITU_SHOW = 16384;
	
	/** Display particle outlines as an overlay. */
	public static final int SHOW_OVERLAY_OUTLINES = 32768;
	
	/** Display filled particle as an overlay. */
	public static final int SHOW_OVERLAY_MASKS = 65536;
}
