package Database.DataReader;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import ij.ImagePlus;
import ij.VirtualStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import tables.DimensionMap;

public class ImageReader {
	
	public static String readImagePath(JEXDataSingle ds, boolean virtual)
	{
		return FileReader.readToPath(ds, virtual);
	}
	
	private static String readImageFileName(JEXDataSingle ds)
	{
		String fileName = ds.get(JEXDataSingle.RELATIVEPATH);
		return fileName;
	}
	
	/**
	 * Get the imagepath stored in the data object
	 * 
	 * @param data
	 * @return
	 */
	public static String readObjectToImagePath(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.IMAGE))
			return null;
		JEXDataSingle ds = data.getFirstSingle();
		String result = readImagePath(ds, data.getDataObjectType().getFlavor().equals(JEXData.FLAVOR_VIRTUAL));
		// String folder = ds.get(JEXDataSingle.FOLDERNAME);
		// String fileName = ds.get(JEXDataSingle.FILENAME);
		// String result = folder + File.separator + fileName;
		return result;
	}
	
	/**
	 * Get the imagepath stored in the data object
	 * 
	 * @param data
	 * @return
	 */
	public static String readObjectToImagePath(JEXData data, DimensionMap map)
	{
		if(!data.getDataObjectType().matches(JEXData.IMAGE))
			return null;
		JEXDataSingle ds = data.getData(map);
		if(ds == null)
			return null;
		String result = readImagePath(ds, data.getDataObjectType().getFlavor().equals(JEXData.FLAVOR_VIRTUAL));
		// String folder = ds.get(JEXDataSingle.FOLDERNAME);
		// String fileName = ds.get(JEXDataSingle.FILENAME);
		// String result = folder + File.separator + fileName;
		return result;
	}
	
	/**
	 * Get the imagepath stored in the data object
	 * 
	 * @param data
	 * @return
	 */
	public static String readObjectToImageName(JEXData data, DimensionMap map)
	{
		if(!data.getDataObjectType().matches(JEXData.IMAGE))
			return null;
		JEXDataSingle ds = data.getData(map);
		String result = readImageFileName(ds);
		// //String folder = ds.get(JEXDataSingle.FOLDERNAME);
		// String fileName = ds.get(JEXDataSingle.FILENAME);
		// String result = fileName;
		return result;
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
	public static String[] readObjectToImagePathStack(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.IMAGE))
			return null;
		List<String> resultList = new ArrayList<String>(0);
		for (JEXDataSingle ds : data.getDataMap().values())
		{
			String path = readImagePath(ds, data.getDataObjectType().getFlavor().equals(JEXData.FLAVOR_VIRTUAL));
			// String folder = ds.get(JEXDataSingle.FOLDERNAME);
			// String fileName = ds.get(JEXDataSingle.FILENAME);
			// String path = folder + File.separator + fileName;
			resultList.add(path);
		}
		String[] result = resultList.toArray(new String[0]);
		return result;
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
	public static List<String> readObjectToImagePathList(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.IMAGE))
			return null;
		List<String> resultList = new ArrayList<String>(0);
		for (JEXDataSingle ds : data.getDataMap().values())
		{
			String path = readImagePath(ds, data.getDataObjectType().getFlavor().equals(JEXData.FLAVOR_VIRTUAL));
			// String folder = ds.get(JEXDataSingle.FOLDERNAME);
			// String fileName = ds.get(JEXDataSingle.FILENAME);
			// String path = folder + File.separator + fileName;
			resultList.add(path);
		}
		return resultList;
	}
	
	/**
	 * Read all the images in the value object into a hashable table of image paths
	 * 
	 * @param data
	 * @return
	 */
	public static TreeMap<DimensionMap,String> readObjectToImagePathTable(JEXData data)
	{
		// if Type is an image, then return null
		if(!data.getDataObjectType().matches(JEXData.IMAGE)) return null;
		
		
		TreeMap<DimensionMap,String> result = new TreeMap<DimensionMap,String>();
		for (DimensionMap map : data.getDataMap().keySet())
		{
			JEXDataSingle ds = data.getData(map);
			String path = readImagePath(ds, data.getDataObjectType().getFlavor().equals(JEXData.FLAVOR_VIRTUAL));
			// String folder = ds.get(JEXDataSingle.FOLDERNAME);
			// String fileName = ds.get(JEXDataSingle.FILENAME);
			// String path = folder + File.separator + fileName;
			result.put(map, path);
		}
		return result;
	}
	
	/**
	 * Get the imagepath stored in the data object
	 * 
	 * @param data
	 * @return
	 */
	public static ImagePlus readObjectToImagePlus(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.IMAGE))
			return null;
		JEXDataSingle ds = data.getFirstSingle();
		String path = readImagePath(ds, data.getDataObjectType().getFlavor().equals(JEXData.FLAVOR_VIRTUAL));
		ImagePlus im = new ImagePlus(path);
		return im;
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
	public static ImagePlus[] readObjectToImagePlusStack(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.IMAGE))
			return null;
		List<String> resultList = new ArrayList<String>(0);
		for (JEXDataSingle ds : data.getDataMap().values())
		{
			String path = readImagePath(ds, data.getDataObjectType().getFlavor().equals(JEXData.FLAVOR_VIRTUAL));
			// String folder = ds.get(JEXDataSingle.FOLDERNAME);
			// String fileName = ds.get(JEXDataSingle.FILENAME);
			// String path = folder + File.separator + fileName;
			resultList.add(path);
		}
		String[] result = resultList.toArray(new String[0]);
		ImagePlus[] imresult = new ImagePlus[result.length];
		for (int i = 0; i < result.length; i++)
		{
			String path = result[i];
			imresult[i] = new ImagePlus(path);
		}
		return imresult;
	}
	
	/**
	 * Read all the images in the value object into a hashable table of image paths
	 * 
	 * @param data
	 * @return
	 */
	public static TreeMap<DimensionMap,ImagePlus> readObjectToImagePlusTable(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.IMAGE))
			return null;
		TreeMap<DimensionMap,ImagePlus> result = new TreeMap<DimensionMap,ImagePlus>();
		for (DimensionMap map : data.getDataMap().keySet())
		{
			JEXDataSingle ds = data.getData(map);
			String path = readImagePath(ds, data.getDataObjectType().getFlavor().equals(JEXData.FLAVOR_VIRTUAL));
			// String folder = ds.get(JEXDataSingle.FOLDERNAME);
			// String fileName = ds.get(JEXDataSingle.FILENAME);
			// String path = folder + File.separator + fileName;
			ImagePlus im = new ImagePlus(path);
			result.put(map, im);
		}
		return result;
	}
	
	public static ImagePlus readFileListToVirtualStack(File[] fileList)
	{
		if(fileList == null || fileList.length == 0)
		{
			return null;
		}
		
		String firstPath = fileList[0].getPath();
		ImagePlus im = new ImagePlus(firstPath);
		
		File temp = new File(firstPath);
		VirtualStack stack = new VirtualStack(im.getWidth(), im.getHeight(), im.getProcessor().getColorModel(), temp.getParent());
		for (File f : fileList)
		{
			if(f != null)
			{
				stack.addSlice(f.getName());
			}
		}
		ImagePlus ret = new ImagePlus(temp.getPath() + "... ", stack);
		return ret;
	}
	
	public static ImagePlus readObjectVirtualStack(JEXData imageData)
	{
		TreeMap<DimensionMap,String> paths = ImageReader.readObjectToImagePathTable(imageData);
		if(paths == null || paths.size() == 0)
		{
			return null;
		}
		
		String firstPath = paths.firstEntry().getValue();
		ImagePlus im = new ImagePlus(firstPath);
		
		File temp = new File(firstPath);
		VirtualStack stack = new VirtualStack(im.getWidth(), im.getHeight(), im.getProcessor().getColorModel(), temp.getParent());
		for (DimensionMap map : paths.keySet())
		{
			temp = new File(paths.get(map));
			stack.addSlice(temp.getName());
		}
		ImagePlus ret = new ImagePlus(temp.getPath() + "... ", stack);
		return ret;
	}
	
	public static ImagePlus readSomeOfObjectToVirtualStack(JEXData imageData, List<DimensionMap> maps)
	{
		if(maps.size() == 0)
			return null;
		TreeMap<DimensionMap,String> paths = ImageReader.readObjectToImagePathTable(imageData);
		File temp = new File(paths.get(maps.get(0)));
		ImagePlus im = new ImagePlus(paths.get(maps.get(0)));
		VirtualStack stack = new VirtualStack(im.getWidth(), im.getHeight(), im.getProcessor().getColorModel(), temp.getParent());
		for (DimensionMap map : maps)
		{
			temp = new File(paths.get(map));
			stack.addSlice(temp.getName());
		}
		ImagePlus ret = new ImagePlus(temp.getPath() + "... ", stack);
		return ret;
	}
}
