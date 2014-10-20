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
		name="Make Calibration Image (Folder, Unfinished)",
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
			name="Stack projection method",
			description="Calculation method for projecting the stack to a single image (pseudo median = The median of subgroups will be averaged)",
			ui=MarkerConstants.UI_DROPDOWN,
			choices={ "Mean", "Median", "Pseudo-Median" },
			defaultChoice=2
			)
	String projectionMethod;
	
	@ParameterMarker(
			uiOrder=2,
			name="Pseudo median subroup size",
			description="The number of images in each subgroup. Used for pseudo median option. Each median operation only produces integer value increments. Mean produces decimal increments",
			ui=MarkerConstants.UI_TEXTFIELD,
			defaultText="4"
			)
	int groupSize;
	
	@ParameterMarker(
			uiOrder=3,
			name="Smoothing method",
			description="Smoothing function to apply at the end, if at all.",
			ui=MarkerConstants.UI_DROPDOWN,
			choices={ "none", JEX_StackProjection.METHOD_MEAN, JEX_StackProjection.METHOD_MEDIAN },
			defaultChoice=1
			)
	String smoothingMethod;
	
	@ParameterMarker(
			uiOrder=4,
			name="Smoothing filter radius",
			description="Radius of the smoothing filter",
			ui=MarkerConstants.UI_TEXTFIELD,
			defaultText="0.0"
			)
	double smoothingRadius;
	
	@ParameterMarker(
			uiOrder=5,
			name="Copy 1st successful to all",
			description="If true, copies the first successful result to all selected entries (good for making a common calibration image for many entries).",
			ui=MarkerConstants.UI_CHECKBOX,
			defaultBoolean=true
			)
	boolean copyAll;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(name="Calibration Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant calibration image", enabled=true)
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
		
		if(sharedCalibrationImage == null)
		{
			String[] filePaths = ImageReader.readObjectToImagePathStack(imageData);
			
			FloatProcessor imp = null;
			if(projectionMethod.equals("Mean"))
			{
				imp = getMeanProjection(filePaths);
			}
			else
			{
				imp = getPseudoMedianProjection(filePaths, groupSize);
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
	
	public FloatProcessor getPseudoMedianProjection(String[] fileList, int groupSize)
	{
		int i = 0, k = 0;
		FloatProcessor ret = null, imp = null;
		FloatBlitter blit = null;
		while (i < fileList.length)
		{
			File[] files = new File[groupSize];
			for (int j = 0; j < groupSize && i < fileList.length; j++)
			{
				files[j] = new File(fileList[i]);
				i++;
			}
			// Get the median of the group
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
			JEXStatics.statusBar.setProgressPercentage((int) (100 * (double) i / fileList.length));
			k++;
		}
		// Divide the total by the number of groups to get the final mean of the
		// groups
		ret.multiply((double) 1 / k);
		return ret;
	}
	
	public FloatProcessor getMeanProjection(String[] fileList)
	{
		int i = 0;
		FloatProcessor imp1 = null, imp2 = null;
		FloatBlitter blit = null;
		for (String f : fileList)
		{
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
			JEXStatics.statusBar.setProgressPercentage((int) (100 * (double) i / fileList.length));
			i++;
		}
		imp1.multiply((double) 1 / fileList.length);
		return imp1;
	}
	
	public void finalizeTicket(Ticket ticket)
	{
		if(sharedCalibrationImage != null && copyAll)
		{
			// Copy the calibration image to all the locations
			TreeMap<JEXEntry,Set<JEXData>> outputs = ticket.getOutputList();
			for (Entry<JEXEntry,Set<JEXData>> e : outputs.entrySet())
			{
				String finalPath = JEXWriter.saveImage(sharedCalibrationImage);
				JEXData temp = ImageWriter.makeImageObject("temp", finalPath);
				Set<JEXData> data = e.getValue();
				data.clear();
				data.add(temp);
			}
			sharedCalibrationImage.flush();
			sharedCalibrationImage = null;
		}
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
