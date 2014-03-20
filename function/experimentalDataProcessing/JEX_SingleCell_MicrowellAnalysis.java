package function.experimentalDataProcessing;

import ij.process.FloatProcessor;
import ij.process.ImageStatistics;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import jex.statics.JEXStatics;
import miscellaneous.CSVList;
import miscellaneous.StatisticsUtility;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.converters.JEXTableReader;
import weka.core.converters.JEXTableWriter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.RoiWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.ExperimentalDataCrunch;

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
public class JEX_SingleCell_MicrowellAnalysis extends ExperimentalDataCrunch {

	public JEX_SingleCell_MicrowellAnalysis()
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
		String result = "Microwell Analysis";
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
		String result = "Categorize wells according to cell number and perform signal correction.";
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
		String toolbox = "Single Cell Analysis";
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
		return false;
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
		TypeName[] inputNames = new TypeName[5];
		inputNames[0] = new TypeName(ROI, "Microwells");
		inputNames[1] = new TypeName(FILE, "Microwell Count Stats");
		inputNames[2] = new TypeName(FILE, "Microwell Intensities");
		inputNames[3] = new TypeName(IMAGE, "IF-DF");
		inputNames[4] = new TypeName(ROI, "Crop ROI (optional)");
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
		this.defaultOutputNames = new TypeName[8];
		this.defaultOutputNames[0] = new TypeName(FILE, "Microwell Categories");
		this.defaultOutputNames[1] = new TypeName(ROI, "Zero-Cell Microwells");
		this.defaultOutputNames[2] = new TypeName(ROI, "Single-Cell Microwells");
		this.defaultOutputNames[3] = new TypeName(FILE, "All Intensities");
		this.defaultOutputNames[4] = new TypeName(FILE, "Zero-Cell Intensities");
		this.defaultOutputNames[5] = new TypeName(FILE, "Single-Cell Intensities");
		this.defaultOutputNames[6] = new TypeName(FILE, "Multi-Cell Intensities");
		this.defaultOutputNames[7] = new TypeName(FILE, "UnCat-Cell Intensities");

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
		Parameter p1 = new Parameter("Microwell ROI Width", "Width (and height) of the microwell in pixels", "33");
		Parameter p2 = new Parameter("Microwell ROI Border Width 1", "Width (and height) of the microwell in pixels", "3");
		Parameter p3 = new Parameter("Microwell ROI Border Width 2", "Width (and height) of the microwell in pixels", "5");
		Parameter p6a = new Parameter("Min Thresh Count Method", "Measurement method for establishing the min count.", Parameter.DROPDOWN, new String[] { "Count Avg", "Count Max" }, 0);
		Parameter p6b = new Parameter("Max Thresh Count Method", "Measurement method for establishing the max count.", Parameter.DROPDOWN, new String[] { "Count Avg", "Count Max" }, 1);
		Parameter p6c = new Parameter("0-Cell Threshold (Min,Max)", "Comma separated min and max values to determine a 'Zero-Cell' well.\nAvg thresholds use decimal values while max values use integer values.", "0,0");
		Parameter p6d = new Parameter("1-Cell Threshold (Min,Max)", "Comma separated min and max values to determine a 'Single-Cell' well.\nAvg thresholds use decimal values while max values use integer values.", "0.8,1");
		Parameter p7 = new Parameter("'Time' Dim Name", "Name of the 'Time' Dimension in the image", "Time");
		Parameter p8 = new Parameter("Name for 'Microwell' Dim", "Name of the Dimension to create for the microwell index", "ROI");

		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		// parameterArray.addParameter(p0);
		// parameterArray.addParameter(p1a);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		// parameterArray.addParameter(p4);
		// parameterArray.addParameter(p5);
		// parameterArray.addParameter(p6a);
		parameterArray.addParameter(p6a);
		parameterArray.addParameter(p6b);
		parameterArray.addParameter(p6c);
		parameterArray.addParameter(p6d);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p8);
		// parameterArray.addParameter(p9);
		// parameterArray.addParameter(p10);
		// parameterArray.addParameter(p11);
		// parameterArray.addParameter(p12);
		// parameterArray.addParameter(p13);
		// parameterArray.addParameter(p14);
		// parameterArray.addParameter(p15);
		// parameterArray.addParameter(p16);
		// parameterArray.addParameter(p17);
		// parameterArray.addParameter(p18);
		// parameterArray.addParameter(p19);
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

	public String timeDimName, colorDimName, microwellDimName;
	public String minMethod = null, maxMethod = null;
	public CSVList thresh_0Range = null, thresh_1Range = null;

	/**
	 * Perform the algorithm here
	 * 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		JEXData microwellData = inputs.get("Microwells");
		if(microwellData == null || !microwellData.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}
		JEXData countData = inputs.get("Microwell Count Stats");
		if(countData == null || !countData.getTypeName().getType().equals(JEXData.FILE))
		{
			return false;
		}
		JEXData intData = inputs.get("Microwell Intensities");
		if(intData == null || !intData.getTypeName().getType().equals(JEXData.FILE))
		{
			return false;
		}
		JEXData ifData = inputs.get("IF-DF");
		if(ifData == null || !ifData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		JEXData cropData = inputs.get("Crop ROI (optional)");
		if(cropData == null || !cropData.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}

		// Get Parameters
		int w0 = Integer.parseInt(this.parameters.getValueOfParameter("Microwell ROI Width"));
		int rw1 = Integer.parseInt(this.parameters.getValueOfParameter("Microwell ROI Border Width 1"));
		int rw2 = Integer.parseInt(this.parameters.getValueOfParameter("Microwell ROI Border Width 2"));
		this.minMethod = this.parameters.getValueOfParameter("Min Thresh Count Method");
		this.maxMethod = this.parameters.getValueOfParameter("Max Thresh Count Method");
		this.thresh_0Range = new CSVList(this.parameters.getValueOfParameter("0-Cell Threshold (Min,Max)"));
		this.thresh_1Range = new CSVList(this.parameters.getValueOfParameter("1-Cell Threshold (Min,Max)"));
		this.microwellDimName = this.parameters.getValueOfParameter("Name for 'Microwell' Dim");
		this.timeDimName = this.parameters.getValueOfParameter("'Time' Dim Name"); // Typically "Time" or "T"
		this.colorDimName = this.parameters.getValueOfParameter("'Color' Dim Name");

		// Gather the data
		if(this.isCanceled())
		{
			return false;
		}
		DimTable fileMaps = intData.getDimTable();
		TreeMap<DimensionMap,String> intFiles = FileReader.readObjectToFilePathTable(intData);
		Table<Double> countNums = JEXTableReader.getNumericTable(FileReader.readFileObject(countData));
		TreeMap<DimensionMap,ROIPlus> microwells = RoiReader.readObjectToRoiMap(microwellData);
		FloatProcessor IF = (FloatProcessor) (ImageReader.readObjectToImagePlus(ifData).getProcessor()).convertToFloat();
		ROIPlus cropRoi = RoiReader.readObjectToRoi(cropData);
		IF.setRoi(cropRoi.getRoi());
		IF = (FloatProcessor) IF.crop();

		// Set status bar variables
		int count = 0;
		int total = intData.getDimTable().mapCount() + 3;
		int percentage = 0;

		// Determine the category of each well (i.e. cell number)
		JEXStatics.statusBar.setProgressPercentage(0);
		Dim timeDim = JEXTableReader.getDimTable(intFiles.firstEntry().getValue()).getDimWithName(timeDimName);
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (double) (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}
		Object[] categoryResults = this.getMicrowellCategories(countNums, timeDim, microwells);
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (double) (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}
		Table<Double> categories = JEXTableReader.getNumericTable((String) categoryResults[0]);
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (double) (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);

		// Correct the intensity data and save off files based on category

		TreeMap<DimensionMap,String> intFilesAll = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> intFiles0 = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> intFiles1 = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> intFilesMulti = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> intFilesUnCat = new TreeMap<DimensionMap,String>();
		for(DimensionMap map : fileMaps.getMapIterator())
		{

			if(this.isCanceled())
			{
				return false;
			}
			Table<Double> intNums = JEXTableReader.getNumericTable(intFiles.get(map));

			if(this.isCanceled())
			{
				return false;
			}
			String[] correctedIntensities = this.getCorrectedIntensityData(intNums, categories, IF, w0, rw1, rw2);
			if(this.isCanceled())
			{
				return false;
			}
			intFilesAll.put(map, correctedIntensities[0]);
			if(this.isCanceled())
			{
				return false;
			}
			intFiles0.put(map, correctedIntensities[1]);
			if(this.isCanceled())
			{
				return false;
			}
			intFiles1.put(map, correctedIntensities[2]);
			if(this.isCanceled())
			{
				return false;
			}
			intFilesMulti.put(map, correctedIntensities[3]);
			if(this.isCanceled())
			{
				return false;
			}
			intFilesUnCat.put(map, correctedIntensities[4]);

			// Update the status
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / (double) (total)));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}

		JEXData output0 = FileWriter.makeFileObject(this.outputNames[0].getName(), null, (String) categoryResults[0]);
		JEXData output1 = RoiWriter.makeRoiObject(this.outputNames[1].getName(), (TreeMap<DimensionMap,ROIPlus>) categoryResults[1]);
		JEXData output2 = RoiWriter.makeRoiObject(this.outputNames[2].getName(), (TreeMap<DimensionMap,ROIPlus>) categoryResults[2]);
		JEXData output3 = FileWriter.makeFileObject(outputNames[3].getName(), null, intFilesAll);
		JEXData output4 = FileWriter.makeFileObject(outputNames[4].getName(), null, intFiles0);
		JEXData output5 = FileWriter.makeFileObject(outputNames[5].getName(), null, intFiles1);
		JEXData output6 = FileWriter.makeFileObject(outputNames[6].getName(), null, intFilesMulti);
		JEXData output7 = FileWriter.makeFileObject(outputNames[7].getName(), null, intFilesUnCat);

		this.realOutputs.add(output0);
		this.realOutputs.add(output1);
		this.realOutputs.add(output2);
		this.realOutputs.add(output3);
		this.realOutputs.add(output4);
		this.realOutputs.add(output5);
		this.realOutputs.add(output6);
		this.realOutputs.add(output7);

		// Return status
		return true;
	}

	public Object[] getMicrowellCategories(Table<Double> countNums, Dim timeDim, TreeMap<DimensionMap,ROIPlus> microwells)
	{
		// Convert total point counts to average point counts and store any id's that are either single cell or zero cell wells.
		TreeMap<DimensionMap,TreeSet<String>> single = new TreeMap<DimensionMap,TreeSet<String>>();
		TreeMap<DimensionMap,TreeSet<String>> zero = new TreeMap<DimensionMap,TreeSet<String>>();
		double min0 = Double.parseDouble(this.thresh_0Range.get(0));
		double max0 = Double.parseDouble(this.thresh_0Range.get(1));
		double min1 = Double.parseDouble(this.thresh_1Range.get(0));
		double max1 = Double.parseDouble(this.thresh_1Range.get(1));
		TreeMap<DimensionMap,Double> newCountStats = new TreeMap<DimensionMap,Double>();
		for (Entry<DimensionMap,Double> e : countNums.data.entrySet())
		{
			newCountStats.put(e.getKey(), e.getValue());
			if(e.getKey().get("Measurement").equals("Count Avg"))
			{
				// Convert totals to averages by dividing by time
				e.setValue(e.getValue() / timeDim.dimValues.size());

				DimensionMap minMap0 = e.getKey().copy();
				DimensionMap maxMap0 = minMap0.copy();
				minMap0.put("Measurement", this.minMethod);
				maxMap0.put("Measurement", this.maxMethod);
				DimensionMap minMap1 = e.getKey().copy();
				DimensionMap maxMap1 = minMap1.copy();
				minMap1.put("Measurement", this.minMethod);
				maxMap1.put("Measurement", this.maxMethod);
				DimensionMap catMap = e.getKey().copy();
				catMap.put("Measurement", "Category");
				DimensionMap maxMap = e.getKey().copy();
				maxMap.put("Measurement", "Count Max");
				if(countNums.data.get(minMap0) >= min0 && countNums.data.get(maxMap0) <= max0)
				{ // Then it is a zero-cell well
					newCountStats.put(catMap, 0.0);

					// save info for creating rois
					TreeSet<String> temp = zero.get(e.getKey());
					if(temp == null)
					{
						temp = new TreeSet<String>();
					}
					DimensionMap toAdd = e.getKey().copy();
					toAdd.remove(this.microwellDimName);
					toAdd.remove("Measurement");
					temp.add(e.getKey().get(this.microwellDimName));
					zero.put(toAdd, temp);
				}
				else if(countNums.data.get(minMap1) >= min1 && countNums.data.get(maxMap1) <= max1)
				{ // Then it is a one-cell well
					newCountStats.put(catMap, 1.0);

					// save info for creating rois
					TreeSet<String> temp = single.get(e.getKey());
					if(temp == null)
					{
						temp = new TreeSet<String>();
					}
					DimensionMap toAdd = e.getKey().copy();
					toAdd.remove(this.microwellDimName);
					toAdd.remove("Measurement");
					temp.add(e.getKey().get(this.microwellDimName));
					single.put(toAdd, temp);
				}
				else if(countNums.data.get(maxMap1) > max1)
				{ // Then it is a multi-cell well and use the max count as the category
					newCountStats.put(catMap, countNums.data.get(maxMap));
				}
				else
				{ // Then it cannot be categorized
					newCountStats.put(catMap, -1.0);
				}
			}
		}

		TreeMap<DimensionMap,ROIPlus> filteredSingles = new TreeMap<DimensionMap,ROIPlus>();
		TreeMap<DimensionMap,ROIPlus> filteredZeros = new TreeMap<DimensionMap,ROIPlus>();
		for (Entry<DimensionMap,ROIPlus> e : microwells.entrySet())
		{
			PointList pl = e.getValue().getPointList();
			PointList singleP = new PointList();
			PointList zeroP = new PointList();
			TreeSet<String> singles = single.get(e.getKey());
			TreeSet<String> zeros = zero.get(e.getKey());
			for (IdPoint p : pl)
			{
				if(singles != null && singles.contains("" + p.id))
				{
					singleP.add(p);
				}
				else if(zeros != null && zeros.contains("" + p.id))
				{
					zeroP.add(p);
				}
			}
			ROIPlus singleRoi = new ROIPlus(singleP, ROIPlus.ROI_POINT);
			filteredSingles.put(e.getKey(), singleRoi);
			ROIPlus zeroRoi = new ROIPlus(zeroP, ROIPlus.ROI_POINT);
			filteredZeros.put(e.getKey(), zeroRoi);
		}

		return new Object[] { JEXTableWriter.writeTable("CategorizedCountStats", newCountStats), filteredZeros, filteredSingles };
	}

	public String[] getCorrectedIntensityData(Table<Double> intNums, Table<Double> categories, FloatProcessor IF, int w0, int rw1, int rw2)
	{
		// ////////////////////////
		// Correct Intensities   //
		// ////////////////////////
		// save all data in single table for old and new methods

		double A1 = Math.pow(((w0) + 2 * rw1), 2);
		double A2 = Math.pow(((w0) + 2 * rw1 + 2 * rw2), 2);
		TreeMap<DimensionMap,Double> xs = new TreeMap<DimensionMap,Double>();
		for (DimensionMap maxMap : intNums)
		{
			String m = maxMap.get("Measurement");
			if(m.endsWith("Max")) // CellMax
			{
				// "New" Method (CellMax - Mode)
				DimensionMap modeMap = maxMap.copy();
				modeMap.put("Measurement", m.substring(0, 1) + "0_Mode");
				DimensionMap xMapNew = maxMap.copy();
				xMapNew.put("Measurement", m.substring(0, 1) + "_New");
				double x = intNums.getData(maxMap) - intNums.getData(modeMap);
				xs.put(xMapNew, x);

				// "Old" Method (big and little box)
				DimensionMap map1 = maxMap.copy();
				map1.put("Measurement", m.substring(0, 1) + "1");
				DimensionMap map2 = maxMap.copy();
				map2.put("Measurement", m.substring(0, 1) + "2");
				DimensionMap xMapOld = maxMap.copy();
				xMapOld.put("Measurement", m.substring(0, 1) + "_Old");
				double bg = (intNums.getData(map2) * A2 - intNums.getData(map1) * A1) / (A2 - A1);
				xs.put(xMapOld, intNums.getData(map1) - bg);
			}
		}

		TreeMap<DimensionMap,Double> all = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> zeros = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> ones = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> mores = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> uncat = new TreeMap<DimensionMap,Double>();
		for (DimensionMap xMap : xs.keySet())
		{
			DimensionMap numMap = xMap.copy();
			numMap.put("Measurement", "Category");
			double count = categories.getData(numMap);
			if(count == 0)
			{
				zeros.put(xMap.copy(), xs.get(xMap));
			}
			else if(count == 1)
			{
				ones.put(xMap.copy(), xs.get(xMap));
			}
			else if(count > 1)
			{
				mores.put(xMap.copy(), xs.get(xMap));
			}
			else if(count == -1)
			{
				uncat.put(xMap.copy(), xs.get(xMap));
			}
			all.put(xMap.copy(), xs.get(xMap));
		}

		DimTable allDimTable = new DimTable(all);
		Dim measurementDim = allDimTable.getDimWithName("Measurement");
		TreeMap<String,Double> zeroMedians = new TreeMap<String,Double>();
		for(String measurement : measurementDim.values())
		{
			zeroMedians.put(measurement, StatisticsUtility.median(Table.getFilteredData(zeros, "Measurement="+measurement).values()));
		}
		double IFmean = ImageStatistics.getStatistics(IF, ImageStatistics.MEAN, null).mean;
		IF.multiply(1 / IFmean);

		String pathAll = correctWithIF(all, categories, IF, zeroMedians, "CorrectedIntensitiesAll");
		String path0 = correctWithIF(zeros, categories, IF, zeroMedians, "CorrectedIntensities0");
		String path1 = correctWithIF(ones, categories, IF, zeroMedians, "CorrectedIntensities1");
		String pathMulti = correctWithIF(mores, categories, IF, zeroMedians, "CorrectedIntensitiesMulti");
		String pathUnCat = correctWithIF(uncat, categories, IF, zeroMedians, "CorrectedIntensitiesUnCat");

		return new String[]{pathAll, path0, path1, pathMulti, pathUnCat};

	}

	public String correctWithIF(TreeMap<DimensionMap,Double> data, Table<Double> categories, FloatProcessor IF, TreeMap<String,Double> zeroMedians, String tableName)
	{
		for (Entry<DimensionMap,Double> e : data.entrySet())
		{
			DimensionMap xLocMap = e.getKey().copy();
			xLocMap.put("Measurement", "X");
			DimensionMap yLocMap = e.getKey().copy();
			yLocMap.put("Measurement", "Y");
			int xLoc = categories.getData(xLocMap).intValue();
			int yLoc = categories.getData(yLocMap).intValue();
			e.setValue((e.getValue() - zeroMedians.get(e.getKey().get("Measurement"))) / IF.getf(xLoc, yLoc));
		}

		return JEXTableWriter.writeTable(tableName, data);
	}

	public String getFilteredData(TreeMap<DimensionMap,Double> data, double categoryNum, String tableName)
	{
		TreeMap<DimensionMap,Double> ret = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> cats = Table.getFilteredData(data, "Measurement=Category");
		for(Entry<DimensionMap,Double> e : cats.entrySet())
		{
			if(categoryNum < 2 && e.getValue() == categoryNum) // (i.e. uncat'd, zero, or single)
			{
				DimensionMap filter = e.getKey().copy();
				filter.remove("Measurement");
				ret.putAll(Table.getFilteredData(data, filter));
			}
			else // we are looking for a multicell well
			{
				DimensionMap filter = e.getKey().copy();
				filter.remove("Measurement");
				ret.putAll(Table.getFilteredData(data, filter));
			}
		}
		String path = JEXTableWriter.writeTable(tableName, ret);
		return path;
	}
}
