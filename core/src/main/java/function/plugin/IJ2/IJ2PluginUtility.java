package function.plugin.IJ2;

import Database.DBObjects.JEXData;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.RoiWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.Type;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import image.roi.ROIPlus;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import logs.Logs;
import net.imagej.ChannelCollection;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.display.DataView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.OverlayView;
import net.imagej.options.OptionsChannels;
import net.imagej.overlay.Overlay;

import org.scijava.MenuPath;
import org.scijava.command.CommandInfo;
import org.scijava.module.ModuleItem;
import org.scijava.util.ColorRGB;

import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;

public class IJ2PluginUtility {
	
	public static final String NUMTOTAL = "Total", NUMPARAMETERS = "Parameters", NUMINPUTS = "Inputs";
	public static final String DATASET = Dataset.class.getSimpleName(), IMAGEDISPLAY = ImageDisplay.class.getSimpleName(), STRING = String.class.getSimpleName(), FILE = String.class.getSimpleName(), OVERLAY = Overlay.class.getSimpleName();
	public static final String ROISUFFIX = "_ROI";
	
	public static ImageJ ij = new ImageJ();
	
	public static TreeMap<String,IJ2CrunchablePlugin> ijCommands = getIJ2Commands();
	
	static
	{
		final OptionsChannels opts = ij.options().getOptions(OptionsChannels.class);
		ColorRGB black = new ColorRGB(0, 0, 0);
		ColorRGB white = new ColorRGB(255, 255, 255);
		opts.setFgValues(new ChannelCollection(white));
		opts.setBgValues(new ChannelCollection(black));
	}
	
	public static TreeMap<String,IJ2CrunchablePlugin> getIJ2Commands()
	{
		List<CommandInfo> commands = ij.command().getCommands();
		TreeMap<String,IJ2CrunchablePlugin> commandTree = new TreeMap<String,IJ2CrunchablePlugin>();
		for (final CommandInfo command : commands)
		{
			if(isValidForJEX(command))
			{
				if(command.getTitle().equals("Image Calculator..."))
				{
					Logs.log("Found it.", IJ2PluginUtility.class);
				}
				IJ2CrunchablePlugin p = new IJ2CrunchablePlugin(command);
				if(p != null)
				{
					Logs.log("Successfully incorporated ImageJ Plugin: " + command.getTitle(), IJ2CrunchablePlugin.class);
					commandTree.put(command.getTitle(), p);
				}
			}
		}
		return commandTree;
	}
	
	public static ParameterSet getDefaultJEXParameters(CommandInfo command)
	{
		ParameterSet ret = new ParameterSet();
		for (ModuleItem<?> item : command.inputs())
		{
			if(isItemAParameter(item))
			{
				Parameter p = getDefaultJEXParameter(item);
				if(p != null)
				{
					ret.addParameter(p);
				}
			}
		}
		return ret;
	}
	
	public static Parameter getDefaultJEXParameter(ModuleItem<?> item)
	{
		List<?> choices = item.getChoices();
		boolean hasChoices = (choices != null) && (!choices.isEmpty());
		if(!hasChoices && item.getType().getSimpleName().equals("String"))
		{
			return new Parameter(item.getName(), item.getLabel(), "");
		}
		if(!hasChoices && item.getType().getSimpleName().equals("File"))
		{
			return new Parameter(item.getName(), item.getLabel(), Parameter.FILECHOOSER, "./");
		}
		if(!hasChoices && (item.getType().getSimpleName().equals("double") || item.getType().getSimpleName().equals("Double")))
		{
			return new Parameter(item.getName(), item.getLabel(), "0.0");
		}
		if(!hasChoices && (item.getType().getSimpleName().equals("int") || item.getType().getSimpleName().equals("long") || item.getType().getSimpleName().equals("Integer") || item.getType().getSimpleName().equals("Long")))
		{
			return new Parameter(item.getName(), item.getLabel(), "0");
		}
		else if(hasChoices && (item.getType().getSimpleName().equals("String") || item.getType().getSimpleName().equals("double") || item.getType().getSimpleName().equals("int") || item.getType().getSimpleName().equals("long") || item.getType().getSimpleName().equals("Double") || item.getType().getSimpleName().equals("Integer") || item.getType().getSimpleName().equals("Long")))
		{
			String[] strChoices = new String[choices.size()];
			for (int i = 0; i < strChoices.length; i++)
			{
				strChoices[i] = choices.get(i).toString();
			}
			
			return new Parameter(item.getName(), item.getLabel(), Parameter.DROPDOWN, strChoices, 0);
		}
		else if(item.getType().getSimpleName().equals("boolean") || item.getType().getSimpleName().equals("Boolean"))
		{
			return new Parameter(item.getName(), item.getLabel(), Parameter.CHECKBOX, true);
		}
		else
		{
			return null;
		}
	}
	
	public static Vector<Object> getIJ2Parameters(ParameterSet parameters, CommandInfo command)
	{
		Vector<Object> imagejParameters = new Vector<Object>();
		for (Parameter p : parameters.getParameters())
		{
			imagejParameters.add(p.getTitle());
			imagejParameters.add(IJ2PluginUtility.getIJ2Parameter(p, command));
		}
		return imagejParameters;
	}
	
	public static Vector<TypeName> getTypeNamesForIJ2IOItem(ModuleItem<?> input)
	{
		Vector<TypeName> ret = new Vector<TypeName>();
		String name = input.getName();
		if(input.getType().getSimpleName().equals(DATASET))
		{
			ret.add(new TypeName(JEXData.IMAGE, name));
			return ret;
		}
		else if(input.getType().getSimpleName().equals(IMAGEDISPLAY))
		{
			ret.add(new TypeName(JEXData.IMAGE, name));
			ret.add(new TypeName(JEXData.ROI, name + "_ROI"));
			return ret;
		}
		else if(input.getType().getSimpleName().equals(FILE))
		{
			ret.add(new TypeName(JEXData.FILE, name));
			return ret;
		}
		else if(input.getType().getSimpleName().equals(OVERLAY))
		{
			ret.add(new TypeName(JEXData.ROI, name));
			return ret;
		}
		return ret;
	}
	
	public static JEXData getJEXDataForOutput(String name, Class<?> outputType, TreeMap<DimensionMap,Object> output)
	{
		JEXData ret = null;
		if(output.size() == 0)
		{
			return null;
		}
		Type type = getJEXDataTypeForClass(outputType);
		if(type.equals(JEXData.IMAGE) || type.equals(JEXData.FILE))
		{ // Then we expect to see ImageDisplays or Datasets out of which we only pull images
			// We assume that if they wanted to output the Overlay of the ImageDisplay, they would have made the overlay an output
			TreeMap<DimensionMap,String> dataMap = new TreeMap<DimensionMap,String>();
			for (Entry<DimensionMap,Object> e : output.entrySet())
			{
				String path = getString(e.getValue());
				if(path != null)
				{
					dataMap.put(e.getKey(), path);
				}
			}
			// Make JEXData
			if(type.equals(JEXData.IMAGE))
			{
				ret = ImageWriter.makeImageStackFromPaths(name, dataMap);
			}
			else
			{ // it is a file object or data (i.e. string or number)
				if(File.class.isAssignableFrom(outputType))
				{ // save each file separately and just keep a table of the file paths
					ret = FileWriter.makeFileObject(name, null, dataMap);
				}
				else
				{ // it is a string or number set that should be saved as a single file instead of a bunch of files
					String temp = JEXTableWriter.writeTable(name, dataMap);
					ret = FileWriter.makeFileObject(name, null, temp);
				}
			}
		}
		else if(type.equals(JEXData.ROI))
		{ // Then we expect to see ImageDisplays or Datasets out of which we only pull images
			// We assume that if they wanted to output the Overlay of the ImageDisplay, they would have made the overlay an output
			
			TreeMap<DimensionMap,ROIPlus> dataMap = new TreeMap<DimensionMap,ROIPlus>();
			for (Entry<DimensionMap,Object> e : output.entrySet())
			{
				ROIPlus roi = getROI(e.getValue());
				if(roi != null)
				{
					dataMap.put(e.getKey(), roi);
				}
				
				// Make JEXData
				ret = RoiWriter.makeRoiObject(name, dataMap);
			}
		}
		return ret;
	}
	
	public static String getString(Object d)
	{
		String data = null;
		if(d instanceof Dataset)
		{
			Dataset temp = (Dataset) d;
			data = JEXWriter.saveImage(temp);
		}
		else if(d instanceof ImageDisplay)
		{
			ImageDisplay display = (ImageDisplay) d;
			if(display.size() > 0)
			{
				DataView view = display.get(0);
				Dataset temp = (Dataset) view.getData();
				data = JEXWriter.saveImage(temp);
			}
		}
		else if(d instanceof File)
		{
			data = ((File) d).getAbsolutePath();
		}
		else
		{
			data = d.toString();
		}
		return data;
	}
	
	public static ROIPlus getROI(Object d)
	{
		ROIPlus roi = null;
		if(d instanceof Overlay)
		{   
			
		}
		return roi;
	}
	
	public static Type getJEXDataTypeForClass(Class<?> c)
	{
		if(ImageDisplay.class.isAssignableFrom(c) || Dataset.class.isAssignableFrom(c))
		{
			return JEXData.IMAGE;
		}
		else if(Overlay.class.isAssignableFrom(c))
		{
			return JEXData.ROI;
		}
		else if(Number.class.isAssignableFrom(c) || String.class.isAssignableFrom(c) || File.class.isAssignableFrom(c))
		{
			return JEXData.FILE;
		}
		else
		{ // This might change in the future so it is separated form the previous if statement.
			return JEXData.FILE;
		}
	}
	
	public static Vector<Object> getIJ2Inputs(DimensionMap map, HashMap<String,JEXData> inputs, CommandInfo command)
	{
		Vector<Object> ret = new Vector<Object>();
		for (ModuleItem<?> item : command.inputs())
		{
			if(isItemAnInput(item))
			{
				if(isItemADataset(item))
				{
					JEXData data = inputs.get(item.getName());
					TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(data);
					String path = images.get(map);
					if(path != null)
					{
						if(ij.dataset().canOpen(path))
						{
							Dataset d;
							try
							{
								d = ij.dataset().open(path);
								ret.add(item.getName());
								ret.add(d);
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
						}
					}
				}
				else if(isItemAnImageDisplay(item))
				{
					// Then get both an image and an optional roi
					JEXData data1 = inputs.get(item.getName());
					JEXData data2 = inputs.get(item.getName() + ROISUFFIX);
					TreeMap<DimensionMap,ROIPlus> rois = null;
					ROIPlus roi = null;
					Overlay ij2Roi = null;
					TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(data1);
					if(data2 != null)
					{
						rois = RoiReader.readObjectToRoiMap(data2);
					}
					String path = images.get(map);
					if(rois != null)
					{
						roi = rois.get(map);
					}
					if(roi != null)
					{
						ij2Roi = IJ2ROIUtility.getROIOverlay(roi);
					}
					if(path != null)
					{
						if(ij.dataset().canOpen(path))
						{
							Dataset d;
							try
							{
								d = ij.dataset().open(path);
								ImageDisplay display = (ImageDisplay) ij.display().createDisplay(d);
								ij.display().setActiveDisplay(display);
								if(ij2Roi != null)
								{
									List<Overlay> overlays = new Vector<Overlay>();
									overlays.add(ij2Roi);
									ij.overlay().addOverlays(display, overlays);
									for (DataView view : display)
									{
										if(view instanceof OverlayView)
										{
											view.setSelected(true);
											break;
										}
									}
								}
								ret.add(item.getName());
								ret.add(display);
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
						}
					}
				}
				else if(isItemAString(item))
				{   
					
				}
				else if(isItemANumber(item))
				{   
					
				}
			}
		}
		return ret;
	}
	
	public static Object getIJ2Parameter(Parameter p, CommandInfo command)
	{
		for (ModuleItem<?> item : command.inputs())
		{
			if(p.getTitle().equals(item.getName()))
			{
				if(item.getType().getSimpleName().equals("String"))
				{
					return p.getValue();
				}
				else if(item.getType().getSimpleName().equals("Double") || item.getType().getSimpleName().equals("double"))
				{
					return Double.parseDouble(p.getValue());
				}
				else if(item.getType().getSimpleName().equals("Integer") || item.getType().getSimpleName().equals("int"))
				{
					return Integer.parseInt(p.getValue());
				}
				else if(item.getType().getSimpleName().equals("Long") || item.getType().getSimpleName().equals("long"))
				{
					return Long.parseLong(p.getValue());
				}
				else if(item.getType().getSimpleName().equals("Boolean") || item.getType().getSimpleName().equals("boolean"))
				{
					return Boolean.parseBoolean(p.getValue());
				}
				else
				{
					return null;
				}
			}
		}
		return null;
	}
	
	// public static TreeMap<String,Integer> getNumCommandInputItems(CommandInfo command)
	// {
	// int total = 0;
	// int parameters = 0;
	// int inputs = 0;
	// for (ModuleItem<?> item : command.inputs())
	// {
	// if(isItemAnInput(item))
	// {
	// inputs++;
	// total++;
	// }
	// else if(isItemAParameter(item))
	// {
	// parameters++;
	// total++;
	// }
	// }
	// TreeMap<String,Integer> ret = new TreeMap<String,Integer>();
	// ret.put(NUMTOTAL, total);
	// ret.put(NUMINPUTS, inputs);
	// ret.put(NUMPARAMETERS, parameters);
	// return ret;
	// }
	
	public static boolean isItemAParameter(ModuleItem<?> item)
	{
		if(item.getType().getSimpleName().equals("File") || item.getType().getSimpleName().equals("String") || item.getType().getSimpleName().equals("Double") || item.getType().getSimpleName().equals("double") || item.getType().getSimpleName().equals("Integer") || item.getType().getSimpleName().equals("int") || item.getType().getSimpleName().equals("Long") || item.getType().getSimpleName().equals("long") || item.getType().getSimpleName().equals("Boolean") || item.getType().getSimpleName().equals("boolean"))
		{
			return true;
		}
		return false;
	}
	
	public static boolean isItemAnInput(ModuleItem<?> item)
	{
		if(item.getType().getSimpleName().equals(DATASET) || item.getType().getSimpleName().equals(IMAGEDISPLAY))
		{
			return true;
		}
		return false;
	}
	
	public static boolean isItemADataset(ModuleItem<?> item)
	{
		if(item.getType().getSimpleName().equals(DATASET))
		{
			return true;
		}
		return false;
	}
	
	public static boolean isItemAnImageDisplay(ModuleItem<?> item)
	{
		if(item.getType().getSimpleName().equals(IMAGEDISPLAY))
		{
			return true;
		}
		return false;
	}
	
	public static boolean isItemAString(ModuleItem<?> item)
	{
		if(item.getType().getSimpleName().equals(STRING))
		{
			return true;
		}
		return false;
	}
	
	public static boolean isItemANumber(ModuleItem<?> item)
	{
		if(item.getType().isAssignableFrom(Number.class))
		{
			return true;
		}
		return false;
	}
	
	public static boolean isValidForJEX(CommandInfo command)
	{
		return command.canRunHeadless();
	}
	
	public static String getToolboxString(String prefix, MenuPath menuPath)
	{
		if(menuPath.size() == 0)
		{
			return prefix;
		}
		else
		{
			String toolbox = prefix;
			if(prefix.equals("JEX"))
			{
				for(int i = 0; i < menuPath.size(); i++)
				{
					toolbox = toolbox + "/" + menuPath.get(i).getName();
				}
				return toolbox;
			}
			else
			{ // ImageJ
				for(int i = 0; i < menuPath.size()-1; i++)
				{
					toolbox = toolbox + "/" + menuPath.get(i).getName();
				}
				return toolbox;
			}
			
		}
	}
	
}
