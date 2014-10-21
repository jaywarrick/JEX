package function.plugin.plugins.calibration;

import ij.ImagePlus;
import ij.plugin.filter.RankFilters;
import ij.process.FloatProcessor;

import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import cruncher.Ticket;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import function.plugin.old.JEX_StackProjection;

@Plugin(
		type = JEXPlugin.class,
		name="Make Calibration Image (Folder)",
		menuPath="Calibration",
		visible=true,
		description="Treat an image object as a stack, calculate the median/mean of subgroups of images, take the mean of the subgroups, and smooth if desired. Result of the first successful entry is copied to all selected entries if desired."
		)
public class MakeCalibrationImage_Folder extends JEXPlugin {
	
	public static ImagePlus sharedCalibrationImage = null;
	
	public MakeCalibrationImage_Folder()
	{}
	
	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
	/////////// Define Inputs ///////////
	
	@InputMarker(name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to use to generate calibration image.", optional=false)
	JEXData imageData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(
			uiOrder=1,
			name="Folder",
			description="Folder from which to grab a list of image files and treat as a stack",
			ui=MarkerConstants.UI_FILECHOOSER,
			choices={ "Mean", "Median", "Pseudo-Median" },
			defaultChoice=2
			)
	String folder;
	
	@ParameterMarker(
			uiOrder=2,
			name="Stack projection method",
			description="Calculation method for projecting the stack to a single image (pseudo median = The median of subgroups will be averaged)",
			ui=MarkerConstants.UI_DROPDOWN,
			choices={ "Mean", "Median", "Pseudo-Median" },
			defaultChoice=2
			)
	String projectionMethod;
	
	@ParameterMarker(
			uiOrder=3,
			name="Pseudo median subroup size",
			description="The number of images in each subgroup. Used for pseudo median option. Each median operation only produces integer value increments. Mean produces decimal increments",
			ui=MarkerConstants.UI_TEXTFIELD,
			defaultText="4"
			)
	int groupSize;
	
	@ParameterMarker(
			uiOrder=4,
			name="Start frame",
			description="Index of first frame to include from stack (first index is 1, inclusive)",
			ui=MarkerConstants.UI_TEXTFIELD,
			defaultText="1"
			)
	int start;
	
	@ParameterMarker(
			uiOrder=5,
			name="Interval",
			description="Interval between successive images used from the stack (typically set to 1 to grab all frames starting at 'start' and ending at 'start' + 'total' + 1).",
			ui=MarkerConstants.UI_TEXTFIELD,
			defaultText="1"
			)
	int interval;
	
	@ParameterMarker(
			uiOrder=6,
			name="Total",
			description="Number of frames to grab total",
			ui=MarkerConstants.UI_TEXTFIELD,
			defaultText="10"
			)
	int total;
	
	@ParameterMarker(
			uiOrder=7,
			name="Smoothing method",
			description="Smoothing function to apply at the end, if at all.",
			ui=MarkerConstants.UI_DROPDOWN,
			choices={ "none", JEX_StackProjection.METHOD_MEAN, JEX_StackProjection.METHOD_MEDIAN },
			defaultChoice=1
			)
	String smoothingMethod;
	
	@ParameterMarker(
			uiOrder=8,
			name="Smoothing filter radius",
			description="Radius of the smoothing filter",
			ui=MarkerConstants.UI_TEXTFIELD,
			defaultText="0.0"
			)
	double smoothingRadius;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(name="Calibration Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant calibration image", enabled=true)
	JEXData output;
	
	@Override
	public int getMaxThreads()
	{
		return 1;
	}
	
	@Override
	public boolean run(JEXEntry entry)
	{
		// Check the inputs
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		if(sharedCalibrationImage == null)
		{
			String[] filePaths = ImageReader.readObjectToImagePathStack(imageData);
			
			Vector<Integer> indicesToGrab = getIndicesToGrab(); 
			
			FloatProcessor imp = null;
			if(projectionMethod.equals("Mean"))
			{
				imp = getMeanProjection(filePaths, indicesToGrab);
			}
			else
			{
				imp = getPseudoMedianProjection(filePaths, indicesToGrab);
			}
			
			if(!smoothingMethod.equals("none"))
			{
				RankFilters rF = new RankFilters();
				rF.rank(imp, smoothingRadius, MakeCalibrationImage_Object.convertRankMethodNameToInt(smoothingMethod));
			}
			
			// End Actual Function
			// Then set the shared instance to trigger skipping of rest of entries and copying of result via the "finishTicket" mechanism
			sharedCalibrationImage = new ImagePlus("temp", imp); // setting this to something other than null causes other entries to be skipped
		}
		
		return true;
	}
	
	public Vector<Integer> getIndicesToGrab()
	{
		return MakeCalibrationImage_Object.getIndicesToGrab(start, total, interval);
	}
	
	public FloatProcessor getPseudoMedianProjection(String[] fileList, Vector<Integer> indicesToGrab)
	{
		return MakeCalibrationImage_Object.getMeanProjection(fileList, indicesToGrab);
	}
	
	public FloatProcessor getMeanProjection(String[] fileList, Vector<Integer> indicesToGrab)
	{
		return MakeCalibrationImage_Object.getMeanProjection(fileList, indicesToGrab);
	}
	
	@Override
	public void finalizeTicket(Ticket ticket)
	{
		if(sharedCalibrationImage != null)
		{
			// Copy the calibration image to all the locations
			TreeMap<JEXEntry,Set<JEXData>> outputs = ticket.getOutputList();
			for (Entry<JEXEntry,Set<JEXData>> e : outputs.entrySet())
			{
				String finalPath = JEXWriter.saveImage(sharedCalibrationImage);
				JEXData temp = ImageWriter.makeImageObject("CalibrationImage_Common", finalPath);
				Set<JEXData> data = e.getValue();
				data.clear();
				data.add(temp);
			}
			sharedCalibrationImage.flush();
		}
		
		sharedCalibrationImage = null;
	}
}
