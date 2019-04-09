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
import logs.Logs;
import miscellaneous.Cancelable;
import miscellaneous.Canceler;
import tables.DimensionMap;

public class ImageWriter implements Cancelable {
	
	Canceler c = null;
	
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
		return makeImageStack(objectName, image, dimensionName, null);
	}
	
	public static JEXData makeImageStack(String objectName, ImagePlus image, String dimensionName, Canceler canceler)
	{
		JEXData data = new JEXData(JEXData.IMAGE, objectName);
		
		ImageStack stack = image.getStack();
		int length = image.getStackSize();
		
		for (int i = 0; i < length; i++)
		{
			if(canceler != null && canceler.isCanceled())
			{
				continue;
			}
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
		return makeImageStack(objectName, filePaths, indexes, dimensionName, false, null);
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
	public static JEXData makeImageStack(String objectName, String[] filePaths, String[] indexes, String dimensionName, boolean virtual, Canceler canceler)
	{
		JEXData data = new JEXData(JEXData.IMAGE, objectName);
		
		for (int index = 0; index < indexes.length; index++)
		{
			if(canceler != null && canceler.isCanceled())
			{
				continue;
			}
			String indexStr = indexes[index];
			String filePath = filePaths[index];
			JEXDataSingle ds = FileWriter.saveFileDataSingle(filePath, virtual);
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
		return makeImageStack(objectName, filePaths, dimensionName, null);
	}
	
	/**
	 * Return an image stack object from filepaths and a dimensionname (ie T, Time, Z, etc...)
	 * 
	 * @param objectName
	 * @param filePaths
	 * @param dimensionName
	 * @return data
	 */
	public static JEXData makeImageStack(String objectName, String[] filePaths, String dimensionName, Canceler canceler)
	{
		JEXData data = new JEXData(JEXData.IMAGE, objectName);
		
		for (int index = 0; index < filePaths.length; index++)
		{
			if(canceler != null && canceler.isCanceled())
			{
				continue;
			}
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
		return makeImageStackFromPaths(objectName, imageMap, null);
	}
	
	/**
	 * Return an image stack object from filepaths and a dimensionname (ie T, Time, Z, etc...)
	 * 
	 * @param objectName
	 * @param imageMap
	 * @return
	 */
	public static JEXData makeImageStackFromPaths(String objectName, Map<DimensionMap,String> imageMap, Canceler canceler)
	{
		return makeImageStackFromPaths(objectName, imageMap, false, canceler);
	}
	
	/**
	 * Return an image stack object from filepaths and a dimensionname (ie T, Time, Z, etc...)
	 * 
	 * @param objectName
	 * @param imageMap
	 * @return
	 */
	public static JEXData makeImageStackFromPaths(String objectName, Map<DimensionMap,String> imageMap, boolean virtual, Canceler canceler)
	{
		JEXData data = new JEXData(JEXData.IMAGE, objectName);
		
		for (DimensionMap map : imageMap.keySet())
		{
			if(canceler != null && canceler.isCanceled())
			{
				continue;
			}
			String path = imageMap.get(map);
			JEXDataSingle ds = FileWriter.saveFileDataSingle(path, virtual);
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
		return makeImageTilesFromPaths(objectName, imageMap, overlap, rows, cols, null);
	}
	
	public static JEXData makeImageTilesFromPaths(String objectName, Map<DimensionMap,String> imageMap, double overlap, int rows, int cols, Canceler canceler)
	{
		JEXData data = new JEXData(JEXData.IMAGE, objectName);
		
		imageMap = separateTilesToPaths(imageMap, overlap, rows, cols, canceler);
		
		for (DimensionMap map : imageMap.keySet())
		{
			//			if(canceler != null && canceler.isCanceled())
			//			{
			//				continue;
			//			}
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
	
	public static TreeMap<DimensionMap,String> separateTilesToPaths(Map<DimensionMap, String> images, double overlap, int rows, int cols)
	{
		return separateTilesToPaths(images, overlap, rows, cols, null);
	}
	
	public static TreeMap<DimensionMap,ImageProcessor> separateTilesToProcessors(Map<DimensionMap, ImageProcessor> images, double overlap, int rows, int cols)
	{
		return separateTilesToProcessors(images, overlap, rows, cols, null);
	}
	
	public static TreeMap<DimensionMap,String> separateTilesToPaths(Map<DimensionMap, String> images, double overlap, int rows, int cols, Canceler canceler)
	{
		SeparateImageTiles splitter = new SeparateImageTiles();
		overlap = overlap/100.0; // Turn percent into fraction.
		
		// Run the function
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		
		int count = 0;
		TreeMap<DimensionMap,ROIPlus> cropRois = null;
		for (DimensionMap map : images.keySet())
		{
			if(canceler != null && canceler.isCanceled())
			{
				continue;
			}
			String path = images.get(map);
			// File f = new File(path);
			
			// get the image
			
			Logs.log("Getting image at " + path, ImageWriter.class);
			ImagePlus im = new ImagePlus(path);
			
			cropRois = splitter.getCropRois(im.getWidth(), im.getHeight(), rows, cols, overlap);
			TreeMap<DimensionMap,String> toSave = splitter.getCropImages(cropRois, map, im.getProcessor());
			outputMap.putAll(toSave);
			
			// Status bar
			int percentage = (int) (100 * ((double) count / (double) images.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		return outputMap;
	}
	
	public static TreeMap<DimensionMap,ImageProcessor> separateTilesToProcessors(Map<DimensionMap, ImageProcessor> images, double overlap, int rows, int cols, Canceler canceler)
	{
		SeparateImageTiles splitter = new SeparateImageTiles();
		overlap = overlap/100.0; // Turn percent into fraction.
		
		// Run the function
		TreeMap<DimensionMap,ImageProcessor> outputMap = new TreeMap<>();
		
		int count = 0;
		TreeMap<DimensionMap,ROIPlus> cropRois = null;
		for (DimensionMap map : images.keySet())
		{
			if(canceler != null && canceler.isCanceled())
			{
				continue;
			}
			ImageProcessor imp = images.get(map);
			
			cropRois = splitter.getCropRois(imp.getWidth(), imp.getHeight(), rows, cols, overlap);
			TreeMap<DimensionMap,ImageProcessor> toSave = splitter.getCropImageProcessors(cropRois, map, imp);
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
		return makeImageStackFromImagePluses(objectName, imageMap, null);
	}
	
	/**
	 * Return an image stack object from filepaths and a dimensionname (ie T, Time, Z, etc...)
	 * 
	 * @param objectName
	 * @param imageMap
	 * @return
	 */
	public static JEXData makeImageStackFromImagePluses(String objectName, Map<DimensionMap,ImagePlus> imageMap, Canceler canceler)
	{
		JEXData data = new JEXData(JEXData.IMAGE, objectName);
		
		for (DimensionMap map : imageMap.keySet())
		{
			if(canceler != null && canceler.isCanceled())
			{
				continue;
			}
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

	@Override
	public void setCanceler(Canceler canceler) {
		this.c = canceler;
	}

	@Override
	public Canceler getCanceler() {
		return c;
	}
	
}
