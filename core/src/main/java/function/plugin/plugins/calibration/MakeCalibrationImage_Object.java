package function.plugin.plugins.calibration;

import ij.ImagePlus;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.FloatBlitter;
import ij.process.FloatProcessor;

import java.io.File;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;

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
		name="Make Calibration Image (Object)",
		menuPath="Calibration",
		visible=true,
		description="Treat an image object as a stack, calculate the median/mean of subgroups of images, take the mean of the subgroups, and smooth if desired. Result of the first successful entry is copied to all selected entries if desired."
		)
public class MakeCalibrationImage_Object extends JEXPlugin {
	
	public static ImagePlus sharedCalibrationImage = null;
	public static String outputName = null;
	
	public MakeCalibrationImage_Object()
	{}
	
	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to use to generate calibration image.", optional=false)
	JEXData imageData;
	
	/////////// Define Parameters ///////////
	
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
	
	@ParameterMarker(
			uiOrder=9,
			name="Copy 1st successful to all",
			description="If true, copies the first successful result to all selected entries (good for making a common calibration image for many entries).",
			ui=MarkerConstants.UI_CHECKBOX,
			defaultBoolean=true
			)
	boolean copyAll;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Calibration Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant calibration image", enabled=true)
	JEXData output;
	
	@Override
	public int getMaxThreads()
	{
		if(copyAll)
		{
			return 1;
		}
		else
		{
			return 10;
		}
	}
	
	@Override
	public boolean run(JEXEntry entry)
	{
		// Check the inputs
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		outputName = output.getDataObjectName();
		
		if(sharedCalibrationImage == null)
		{
			String[] filePaths = ImageReader.readObjectToImagePathStack(imageData);
			
			Vector<Integer> indicesToGrab = getIndicesToGrab(start, total, interval); 
			
			FloatProcessor imp = null;
			if(projectionMethod.equals("Mean"))
			{
				imp = getMeanProjection(filePaths, indicesToGrab);
			}
			else
			{
				imp = getPseudoMedianProjection(filePaths, indicesToGrab, groupSize);
			}
			
			if(!smoothingMethod.equals("none"))
			{
				RankFilters rF = new RankFilters();
				rF.rank(imp, smoothingRadius, convertRankMethodNameToInt(smoothingMethod));
			}
			
			// End Actual Function
			if(copyAll)
			{
				// Then set the shared instance to trigger skipping of rest of entries and copying of result via the "finishTicket" mechanism
				sharedCalibrationImage = new ImagePlus("temp", imp);
			}
			else
			{
				// Save the image to the function output
				String path = JEXWriter.saveImage(imp);
				output = ImageWriter.makeImageObject("temp", path); 
			}
		}
		
		return true;
	}
	
	public static Vector<Integer> getIndicesToGrab(int start, int total, int interval)
	{
		Vector<Integer> ret = new Vector<Integer>();
		
		int i = start-1;
		int count = 0;
		
		while(count < total)
		{
			ret.add(i);
			i = i + interval;
			count = count + 1;
		}
		
		return ret;
	}
	
	public static FloatProcessor getPseudoMedianProjection(String[] fileList, Vector<Integer> indicesToGrab, int groupSize)
	{
		int k = 0;
		FloatProcessor ret = null, imp = null;
		FloatBlitter blit = null;
		for (int i = 0; i < indicesToGrab.size(); i++)
		{
			Vector<File> filesVector = new Vector<File>();
			for (int j = 0; j < groupSize && i < indicesToGrab.size() && indicesToGrab.get(i) < fileList.length; j++)
			{
				filesVector.add(new File(fileList[indicesToGrab.get(i)]));
				i++;
				// don't do anything to j, It just causes the loop to try and loop 'groupsize' number of times.
			}
			// Get the median of the group
			if(filesVector.size() > 0)
			{
				File[] files = filesVector.toArray(new File[filesVector.size()]);
				ImagePlus stack = ImageReader.readFileListToVirtualStack(files);
				stack.setProcessor((FloatProcessor) stack.getProcessor().convertToFloat());
				imp = JEX_StackProjection.evaluate(stack, JEX_StackProjection.METHOD_MEDIAN, groupSize);
				
				// Add it to the total for taking the mean of the groups
				if(k == 0)
				{
					ret = imp;
					blit = new FloatBlitter(ret);
				}
				else
				{
					blit.copyBits(imp, 0, 0, Blitter.ADD);
				}
				JEXStatics.statusBar.setProgressPercentage((int) (100 * (double) i / indicesToGrab.size()));
				k++;
			}
			else
			{
				break;
			}
		}
		// Divide the total by the number of groups to get the final mean of the
		// groups
		if(ret != null && k > 0)
		{
			ret.multiply((double) 1 / k);
		}
		return ret;
	}
	
	public static FloatProcessor getMeanProjection(String[] fileList, Vector<Integer> indicesToGrab)
	{
		int k = 0;
		FloatProcessor imp1 = null, imp2 = null;
		FloatBlitter blit = null;
		for (Integer i : indicesToGrab)
		{
			if(i < fileList.length)
			{
				String f = fileList[i];
				if(i == 0)
				{
					imp1 = (FloatProcessor) (new ImagePlus(f)).getProcessor().convertToFloat();
					blit = new FloatBlitter(imp1);
				}
				else
				{
					imp2 = (FloatProcessor) (new ImagePlus(f)).getProcessor().convertToFloat();
					blit.copyBits(imp2, 0, 0, Blitter.ADD);
				}
				JEXStatics.statusBar.setProgressPercentage((int) (100 * (double) i / indicesToGrab.size()));
				k++;
			}
			else
			{
				break;
			}
		}
		if(imp1 != null && k > 0)
		{
			imp1.multiply((double) 1 / k);
		}
		return imp1;
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
				JEXData temp = ImageWriter.makeImageObject(outputName, finalPath);
				Set<JEXData> data = e.getValue();
				data.clear();
				data.add(temp);
			}
			sharedCalibrationImage.flush();
		}
		outputName = null;
		sharedCalibrationImage = null;
	}
	
	public static int convertRankMethodNameToInt(String method)
	{
		// :,mean,max,min,sum,std. dev.,median"///
		int methodInt = 5;
		if(method.equals(JEX_StackProjection.METHOD_MEAN))
			methodInt = 0;
		else if(method.equals(JEX_StackProjection.METHOD_MAX))
			methodInt = 1;
		else if(method.equals(JEX_StackProjection.METHOD_MIN))
			methodInt = 2;
		else if(method.equals(JEX_StackProjection.METHOD_SUM))
			methodInt = 3;
		else if(method.equals(JEX_StackProjection.METHOD_STDEV))
			methodInt = 4;
		else if(method.equals(JEX_StackProjection.METHOD_MEDIAN))
			methodInt = 5;
		return methodInt;
	}
}
