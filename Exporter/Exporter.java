package Exporter;

import ij.ImagePlus;
import ij.process.ByteProcessor;

import java.awt.Rectangle;
import java.io.File;
import java.util.TreeMap;
import java.util.TreeSet;

import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FileUtility;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.Definition.Type;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;

public class Exporter {
	
	public static int EXPORT2D = 2;
	public static int EXPORTLIST = 1;
	public static String EXPORT_AS_IMAGE = "EXPORT_AS_IMAGE";
	public static String EXPORT_AS_CSV = "EXPORT_AS_CSV";
	
	// Data to export
	JEXData[][] exportArray;
	JEXEntry[][] entryArray;
	TreeSet<JEXEntry> entryList;
	TypeName dataTN;
	
	// Location to export
	public boolean saveAsObject = false;
	public String objectName = "";
	public String path = "";
	public String info = "";
	public String exportType = EXPORT_AS_CSV;
	public int exportMode = EXPORT2D;
	
	// Range for a heatmap
	public Double min = Double.NaN;
	public Double mean = Double.NaN;
	public Double max = Double.NaN;
	public boolean percent = false;
	
	public Exporter()
	{}
	
	public Exporter(String exportType, int mode, boolean saveObject, String objectName, String info)
	{
		this.saveAsObject = saveObject;
		this.objectName = objectName;
		this.info = info;
		this.exportType = exportType;
		this.exportMode = mode;
	}
	
	/**
	 * Perform the export
	 */
	public void export()
	{
		if(exportType.equals(EXPORT_AS_CSV))
		{
			exportAsCSV();
		}
		else if(exportType.equals(EXPORT_AS_IMAGE))
		{
			exportAsImage();
		}
	}
	
	/**
	 * Set the range for coloring when exporting a heat map
	 * 
	 * @param min
	 * @param mean
	 * @param max
	 */
	public void setRange(Double min, Double mean, Double max)
	{
		this.min = min;
		this.max = max;
		this.mean = mean;
	}
	
	/**
	 * Set the exporter to export values as percent variation around mean
	 * 
	 * @param percent
	 */
	public void setMakePercentVariation(boolean percent)
	{
		this.percent = percent;
	}
	
	/**
	 * Set the list of entries to export and the object TypeName to be exported
	 * 
	 * @param entryList
	 * @param dataTN
	 */
	public void setListDataToExport(TreeSet<JEXEntry> entryList, TypeName dataTN)
	{
		this.entryList = entryList;
		this.dataTN = dataTN;
	}
	
	/**
	 * Set the exporting mode, 2D or 1D
	 * 
	 * @param exportMode
	 */
	public void setExportMode(int exportMode)
	{
		this.exportMode = exportMode;
	}
	
	/**
	 * Make an array for exporting in 2D
	 * 
	 * @param entryList
	 * @param dataTN
	 * @return
	 */
	public JEXData[][] makeArrayFromList(TreeSet<JEXEntry> entryList, TypeName dataTN)
	{
		TreeMap<Integer,TreeMap<Integer,JEXData>> dataarray = new TreeMap<Integer,TreeMap<Integer,JEXData>>();
		TreeMap<Integer,TreeMap<Integer,JEXEntry>> entryarray = new TreeMap<Integer,TreeMap<Integer,JEXEntry>>();
		int arrayWidth = 0;
		int arrayHeight = 0;
		
		// Loop through the entries
		for (JEXEntry entry : entryList)
		{
			JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(dataTN, entry);
			int i = entry.getTrayX();
			int j = entry.getTrayY();
			
			// If the data at this entry is non null
			if(data != null)
			{
				// Place the data in the hash map
				TreeMap<Integer,JEXData> row = dataarray.get(i);
				if(row == null)
					row = new TreeMap<Integer,JEXData>();
				row.put(j, data);
				dataarray.put(i, row);
				
				// Place the entry in the hash map
				TreeMap<Integer,JEXEntry> entryrow = entryarray.get(i);
				if(entryrow == null)
					entryrow = new TreeMap<Integer,JEXEntry>();
				entryrow.put(j, entry);
				entryarray.put(i, entryrow);
				
				arrayWidth = Math.max(i, arrayWidth);
				arrayHeight = Math.max(j, arrayHeight);
			}
		}
		
		JEXData[][] datas = new JEXData[arrayWidth + 1][arrayHeight + 1];
		entryArray = new JEXEntry[arrayWidth + 1][arrayHeight + 1];
		for (Integer key1 : dataarray.keySet())
		{
			
			// Get the data at row KEY1
			TreeMap<Integer,JEXData> row = dataarray.get(key1);
			TreeMap<Integer,JEXEntry> entryrow = entryarray.get(key1);
			
			for (Integer key2 : row.keySet())
			{
				
				// Get data at column KEY2
				JEXData data = row.get(key2);
				JEXEntry entry = entryrow.get(key2);
				
				// Fill the array
				datas[key1][key2] = data;
				entryArray[key1][key2] = entry;
			}
		}
		
		return datas;
	}
	
	/**
	 * Export the data as a value array
	 */
	public void exportAsCSV()
	{
		if(dataTN == null || entryList == null)
		{
			JEXStatics.statusBar.setStatusText("Export error");
			return;
		}
		
		String result = "";
		result = result + "\n";
		
		if(this.exportMode == EXPORT2D)
		{
			// Make the array
			exportArray = makeArrayFromList(entryList, dataTN);
			
			for (int i = 0, leni = exportArray[0].length; i < leni; i++)
			{
				for (int j = 0, lenj = exportArray.length; j < lenj; j++)
				{
					if(exportArray[j][i] == null)
					{
						result = result + ",";
						continue;
					}
					JEXData data = exportArray[j][i];
					JEXDataSingle xd = data.getFirstSingle();
					if(xd == null)
						result = result + ",";
					else
						result = result + "," + xd.toString();
				}
				result = result + "\n";
			}
		}
		else
		{
			for (JEXEntry entry : entryList)
			{
				JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(dataTN, entry);
				if(data == null)
				{
					result = result + ",";
					continue;
				}
				JEXDataSingle xd = data.getFirstSingle();
				if(xd == null)
					result = result + ",";
				else
					result = result + "," + xd.toString();
			}
		}
		
		if(saveAsObject)
		{
			Logs.log("Printing csv array to file and saving in databse as jexfile", 1, this);
			if(objectName.equals(""))
				return;
			
			TreeMap<JEXEntry,JEXData> dataArray = new TreeMap<JEXEntry,JEXData>();
			for (JEXEntry entry : entryList)
			{
				String objectPath = JEXWriter.saveText(result, "csv");
				
				JEXData data2add = FileWriter.makeFileObject(JEXData.FILE, objectName, objectPath);
				data2add.setDataObjectInfo(info);
				dataArray.put(entry, data2add);
			}
			JEXStatics.jexDBManager.saveDataInEntries(dataArray);
		}
		else
		{
			Logs.log("Printing csv array to file", 1, this);
			String objectPath = JEXWriter.saveText(result, "csv");
			try
			{
				FileUtility.openFileDefaultApplication(objectPath);
			}
			catch (Exception e1)
			{
				Logs.log("Cannot open file", 1, this);
			}
		}
		JEXStatics.statusBar.setStatusText("Data exported");
	}
	
	/**
	 * Export the data as an image array or heat map
	 */
	public void exportAsImage()
	{
		if(dataTN == null || entryList == null)
			return;
		
		Type type = null;
		for (JEXEntry entry : entryList)
		{
			JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(dataTN, entry);
			if(data == null)
				continue;
			Type datatype = data.getDataObjectType();
			if(!type.equals("") && !type.equals(datatype))
			{
				Logs.log("All the types don't match... exiting", 1, this);
				JEXStatics.statusBar.setStatusText("Export error");
				return;
			}
			if(datatype.equals(JEXData.IMAGE))
				type = JEXData.IMAGE;
			else if(datatype.equals(JEXData.VALUE))
				type = JEXData.VALUE;
		}
		if(type == null)
		{
			Logs.log("Cannot export this type of data", 1, this);
			JEXStatics.statusBar.setStatusText("Export impossible for this data");
		}
		
		ImagePlus im2Save = null;
		if(type.equals(JEXData.VALUE))
		{
			if(this.exportMode == EXPORT2D)
			{
				// Make the array
				exportArray = makeArrayFromList(entryList, dataTN);
				
				try
				{
					HeatMapFrame mapIT = new HeatMapFrame();
					mapIT.setRange(min, mean, max);
					mapIT.setpercentVariation(percent);
					mapIT.makeHeatMap(exportArray);
					mapIT.setBounds(new Rectangle(50, 50, 400, 400));
					mapIT.setVisible(true);
					
					int height = 40 * exportArray[0].length;
					int width = 40 * exportArray.length;
					im2Save = mapIT.getHeatMapImage(width, height);
				}
				catch (Exception e1)
				{
					Logs.log("Error occrued in creation of heat map", 1, this);
					JEXStatics.statusBar.setStatusText("Export error");
				}
			}
			else
			{   
				
			}
		}
		else if(type.equals(JEXData.IMAGE))
		{
			if(this.exportMode == EXPORT2D)
			{
				// Make the array
				exportArray = makeArrayFromList(entryList, dataTN);
				
				int rows = exportArray.length;
				int cols = exportArray[0].length;
				File[][] imageArray = new File[cols][rows];
				
				int r, c;
				for (r = 0; r < rows; r++)
				{
					for (c = 0; c < cols; c++)
					{
						if(exportArray[r][c] == null)
						{
							ByteProcessor bimp = new ByteProcessor(800, 600);
							ImagePlus emptyIm = new ImagePlus("", bimp);
							String path = JEXWriter.saveImage(emptyIm);
							imageArray[c][r] = new File(path);
							// imageArray[c][r] = null;
						}
						else
						{
							
							// Get the data object
							JEXData data = exportArray[r][c];
							
							if(data == null)
								imageArray[c][r] = null;
							else
							{
								String fPath = ImageReader.readObjectToImagePath(data);
								File f = new File(fPath);
								imageArray[c][r] = f;
							}
						}
					}
				}
				
				try
				{
					im2Save = function.imageUtility.jMontageMaker.makeMontage(imageArray);
					im2Save.show();
				}
				catch (Exception e)
				{
					Logs.log("Error occrued in creation of the montage", 1, this);
					JEXStatics.statusBar.setStatusText("Export error");
				}
			}
		}
		
		// Save the image
		if(im2Save == null)
		{
			Logs.log("Null image created... error", 1, this);
			JEXStatics.statusBar.setStatusText("Export error");
			return;
		}
		
		if(saveAsObject)
		{
			TreeMap<JEXEntry,JEXData> dataArray = new TreeMap<JEXEntry,JEXData>();
			for (JEXEntry entry : entryList)
			{
				String objectPath = JEXWriter.saveImage(im2Save);
				JEXData data2add = ImageWriter.makeImageObject(objectName, objectPath);
				data2add.setDataObjectInfo(info);
				dataArray.put(entry, data2add);
			}
			JEXStatics.jexDBManager.saveDataInEntries(dataArray);
			JEXStatics.statusBar.setStatusText("Data exported");
		}
		else
		{
			String objectPath = JEXWriter.saveImage(im2Save);
			Logs.log("Printing image montage to file " + objectPath, 1, this);
			JEXStatics.statusBar.setStatusText("Printing image montage to file " + objectPath);
		}
	}
}
