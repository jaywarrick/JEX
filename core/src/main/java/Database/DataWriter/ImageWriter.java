package Database.DataWriter;

import java.util.Map;
import java.util.TreeMap;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.plugins.imageTools.SeparateImageTiles;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import image.roi.ROIPlus;
import jex.statics.JEXStatics;
import tables.DimensionMap;

public class ImageWriter {
	
	public static JEXData makeImageObject(String objectName, ImagePlus image)
	{
		// Save the image
		String path = JEXWriter.saveImage(image);
		
		// Make a JEXData
		JEXData result = makeImageObject(objectName, path);
		return result;
	}
	
	public static JEXData makeImageStack(String objectName, ImagePlus image, String dimensionName)
	{
		JEXData data = new JEXData(JEXData.IMAGE, objectName);
		
		ImageStack stack = image.getStack();
		int length = image.getStackSize();
		
		for (int i = 0; i < length; i++)
		{
			ImageProcessor slice = stack.getProcessor(i + 1);
			String path = JEXWriter.saveImage(new ImagePlus("", slice));
			JEXDataSingle ds = FileWriter.saveFileDataSingle(path);
			if(ds == null)
			{
				continue;
			}
			DimensionMap map = new DimensionMap();
			map.put(dimensionName, "" + i);
			data.addData(map, ds);
		}
		
		if(data.getDataMap().size() == 0)
		{
			return null;
		}
		
		return data;
	}
	
	/**
	 * Make an image data object with a single image inside
	 * 
	 * @param objectName
	 * @param filePath
	 * @return data
	 */
	public static JEXData makeImageObject(String objectName, String filePath)
	{
		JEXData data = new JEXData(JEXData.IMAGE, objectName);
		JEXDataSingle ds = FileWriter.saveFileDataSingle(filePath);
		if(ds == null)
		{
			return null;
		}
		data.addData(new DimensionMap(), ds);
		return data;
	}
	
	/**
	 * Make an image stack containing an image list, each image is at an index defined by an index from the list INDEXES, a file path from FILEPATHS and a dimension name
	 * 
	 * @param objectName
	 * @param filePaths
	 * @param indexes
	 * @param dimensionName
	 * @return data
	 */
	public static JEXData makeImageStack(String objectName, String[] filePaths, String[] indexes, String dimensionName)
	{
		JEXData data = new JEXData(JEXData.IMAGE, objectName);
		
		for (int index = 0; index < indexes.length; index++)
		{
			String indexStr = indexes[index];
			String filePath = filePaths[index];
			JEXDataSingle ds = FileWriter.saveFileDataSingle(filePath);
			if(ds == null)
			{
				continue;
			}
			DimensionMap map = new DimensionMap();
			map.put(dimensionName, indexStr);
			data.addData(map, ds);
		}
		
		if(data.getDataMap().size() == 0)
		{
			return null;
		}
		return data;
	}
	
	/**
	 * Return an image stack object from filepaths and a dimensionname (ie T, Time, Z, etc...)
	 * 
	 * @param objectName
	 * @param filePaths
	 * @param dimensionName
	 * @return data
	 */
	public static JEXData makeImageStack(String objectName, String[] filePaths, String dimensionName)
	{
		JEXData data = new JEXData(JEXData.IMAGE, objectName);
		
		for (int index = 0; index < filePaths.length; index++)
		{
			String indexStr = "" + index;
			String filePath = filePaths[index];
			JEXDataSingle ds = FileWriter.saveFileDataSingle(filePath);
			if(ds == null)
			{
				continue;
			}
			DimensionMap map = new DimensionMap();
			map.put(dimensionName, indexStr);
			data.addData(map, ds);
		}
		
		if(data.getDataMap().size() == 0)
		{
			return null;
		}
		return data;
	}
	
	/**
	 * Return an image stack object from filepaths and a dimensionname (ie T, Time, Z, etc...)
	 * 
	 * @param objectName
	 * @param imageMap
	 * @return
	 */
	public static JEXData makeImageStackFromPaths(String objectName, Map<DimensionMap,String> imageMap)
	{
		JEXData data = new JEXData(JEXData.IMAGE, objectName);
		
		for (DimensionMap map : imageMap.keySet())
		{
			String path = imageMap.get(map);
			JEXDataSingle ds = FileWriter.saveFileDataSingle(path);
			if(ds == null)
			{
				continue;
			}
			DimensionMap newmap = map.copy();
			data.addData(newmap, ds);
		}
		
		if(data.getDataMap().size() == 0)
		{
			return null;
		}
		return data;
	}
	
	public static JEXData makeImageTilesFromPaths(String objectName, Map<DimensionMap,String> imageMap, double overlap, int rows, int cols)
	{
		JEXData data = new JEXData(JEXData.IMAGE, objectName);
		
		imageMap = separateTiles(imageMap, overlap, rows, cols);
		
		for (DimensionMap map : imageMap.keySet())
		{
			String path = imageMap.get(map);
			JEXDataSingle ds = FileWriter.saveFileDataSingle(path);
			if(ds == null)
			{
				continue;
			}
			DimensionMap newmap = map.copy();
			data.addData(newmap, ds);
		}
		
		if(data.getDataMap().size() == 0)
		{
			return null;
		}
		return data;
	}
	
	public static Map<DimensionMap,String> separateTiles(Map<DimensionMap, String> images, double overlap, int rows, int cols)
	{
		SeparateImageTiles splitter = new SeparateImageTiles();
		overlap = overlap/100.0; // Turn percent into fraction.
		
		// Run the function
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		
		int count = 0;
		TreeMap<DimensionMap,ROIPlus> cropRois = null;
		for (DimensionMap map : images.keySet())
		{
			String path = images.get(map);
			// File f = new File(path);
			
			// get the image
			ImagePlus im = new ImagePlus(path);
			
			cropRois = splitter.getCropRois(im.getWidth(), im.getHeight(), rows, cols);
			TreeMap<DimensionMap,String> toSave = splitter.getCropImages(cropRois, map, im.getProcessor());
			outputMap.putAll(toSave);
			
			// Status bar
			int percentage = (int) (100 * ((double) count / (double) images.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		return outputMap;
	}
	
	/**
	 * Return an image stack object from filepaths and a dimensionname (ie T, Time, Z, etc...)
	 * 
	 * @param objectName
	 * @param imageMap
	 * @return
	 */
	public static JEXData makeImageStackFromImagePluses(String objectName, Map<DimensionMap,ImagePlus> imageMap)
	{
		JEXData data = new JEXData(JEXData.IMAGE, objectName);
		
		for (DimensionMap map : imageMap.keySet())
		{
			ImagePlus image = imageMap.get(map);
			String path = JEXWriter.saveImage(image);
			JEXDataSingle ds = FileWriter.saveFileDataSingle(path);
			if(ds == null)
			{
				continue;
			}
			DimensionMap newmap = map.copy();
			data.addData(newmap, ds);
		}
		
		if(data.getDataMap().size() == 0)
		{
			return null;
		}
		return data;
	}
	
}
