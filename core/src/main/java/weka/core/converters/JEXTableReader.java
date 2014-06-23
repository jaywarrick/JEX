package weka.core.converters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import miscellaneous.Canceler;
import miscellaneous.Pair;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.Instance;
import weka.core.Instances;

public class JEXTableReader {
	
	public String filePath = null;
	public ArffLoader loader = null;
	public Instances structure = null;
	public DimTable dimTable = null;
	public boolean multipleReadsFlag = true; // flag to automatically reset
	
	// the reader
	
	public static Table<String> getStringTable(String filePath)
	{
		JEXTableReader reader = new JEXTableReader(filePath);
		reader.readHeader(); // leaves reader open
		reader.multipleReadsFlag = false;
		Table<String> ret = reader.readStringData(); // this function calls
		// close();
		return ret;
	}
	
	public static Table<String> getStringTable(String filePath, DimensionMap filter)
	{
		JEXTableReader reader = new JEXTableReader(filePath);
		reader.readHeader(); // leaves reader open
		reader.multipleReadsFlag = false;
		Table<String> ret = reader.readStringData(filter); // this function
		// calls close();
		return ret;
	}
	
	public static Table<Double> getNumericTable(String filePath)
	{
		JEXTableReader reader = new JEXTableReader(filePath);
		reader.readHeader(); // leaves reader open
		reader.multipleReadsFlag = false;
		Table<Double> ret = reader.readNumericData(); // this function calls
		// close();
		return ret;
	}
	
	public static Table<Double> getNumericTable(String filePath, DimensionMap filter)
	{
		JEXTableReader reader = new JEXTableReader(filePath);
		reader.readHeader(); // leaves reader open
		reader.multipleReadsFlag = false;
		Table<Double> ret = reader.readNumericData(filter); // this function
		// calls close();
		return ret;
	}
	
	public static DimTable getDimTable(String filePath)
	{
		JEXTableReader reader = new JEXTableReader(filePath);
		reader.readHeader(); // leaves reader open
		reader.close(); // closes reader
		return reader.dimTable.copy();
	}
	
	public static <E> TreeMap<DimensionMap,E> filter(TreeMap<DimensionMap,E> data, DimensionMap filter)
	{
		TreeMap<DimensionMap,E> ret = new TreeMap<DimensionMap,E>();
		for (DimensionMap map : data.keySet())
		{
			int matches = map.compareTo(filter);
			if(matches == 0)
			{
				ret.put(map, data.get(map));
			}
		}
		return ret;
	}
	
	public static Table<String> splitTable(String filePath, String splitDimName, String tableName, String fileExtension, Canceler canceler)
	{
		JEXTableReader reader = new JEXTableReader(filePath);
		return split(reader, splitDimName, tableName, fileExtension, canceler);
	}
	
	public JEXTableReader(String filePath)
	{
		try
		{
			this.filePath = filePath;
			this.loader = new ArffLoader();
			// TODO figure out what to do with this. this.loader.setRetainStringValues(false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void readHeader()
	{
		try
		{
			File thisFile = new File(this.filePath);
			if(!thisFile.exists())
			{
				throw new FileNotFoundException();
			}
			this.loader.setFile(thisFile);
			this.structure = this.loader.getStructure();
			while (this.loader.m_ArffReader.m_Tokenizer.nextToken() != StreamTokenizer.TT_EOL)
			{}
			;
			this.createDimTable();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void close()
	{
		try
		{
			if(this.loader != null)
			{
				this.loader.m_sourceReader.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public DimTable getDimTable()
	{
		return this.dimTable;
	}
	
	private void createDimTable()
	{
		this.dimTable = new DimTable();
		for (int i = 0; i < this.structure.numAttributes() - 1; i++)
		{
			Dim newDim = new Dim(this.structure.attribute(i));
			this.dimTable.add(newDim);
		}
	}
	
	public Table<String> readStringData()
	{
		return this.readStringData(null);
	}
	
	public Table<Double> readNumericData()
	{
		return this.readNumericData(null);
	}
	
	public Table<String> readStringData(DimensionMap filter)
	{
		return this.readData(filter, "");
	}
	
	public Table<Double> readNumericData(DimensionMap filter)
	{
		return this.readData(filter, 1.0);
	}
	
	public Boolean isNumeric()
	{
		return this.structure.attribute(this.structure.numAttributes() - 1).isNumeric();
	}
	
	private <E> Table<E> readData(DimensionMap filter, E dummyInstance)
	{
		JEXStatics.statusBar.setStatusText("Started Reading ARFF");
		DimTable filteredDimTable;
		if(filter == null)
		{
			filteredDimTable = this.dimTable.copy();
		}
		else
		{
			filteredDimTable = this.dimTable.getSubTable(filter);
		}
		
		double count = 0;
		try
		{
			if(this.multipleReadsFlag)
			{
				this.reset();
			}
			// gather the data
			TreeMap<DimensionMap,E> data = new TreeMap<DimensionMap,E>();
			
			Instance instance;
			Pair<DimensionMap,E> pair;
			if(filter == null)
			{
				while ((instance = this.loader.m_ArffReader.readInstance(this.structure)) != null)
				{
					pair = this.getPair(instance);
					if(pair.p2 != null)
					{
						data.put(pair.p1, pair.p2);
					}
					count++;
					if(count % 1000 == 0)
					{
						JEXStatics.statusBar.setStatusText("Reading ARFF Line Number: " + count + "");
					}
				}
			}
			else
			{
				while ((instance = this.loader.m_ArffReader.readInstance(this.structure)) != null)
				{
					pair = this.getPair(instance);
					if(pair.p2 != null && filteredDimTable.hasDimensionMap(pair.p1))
					{
						data.put(pair.p1, pair.p2);
					}
					count++;
					if(count % 1000 == 0)
					{
						JEXStatics.statusBar.setStatusText("Reading ARFF Line Number: " + count + "");
					}
				}
			}
			
			Table<E> ret = new Table<E>(filteredDimTable, data);
			this.close();
			JEXStatics.statusBar.setStatusText("Reading ARFF Done.");
			return ret;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Make a new table for each unique value of the split dim. Return the table of file paths that point to the new Tables.
	 * 
	 * @param <E>
	 * @param reader
	 * @param splitDimName
	 * @param tableName
	 * @param fileExtension
	 * @return
	 */
	private static <E> Table<String> split(JEXTableReader reader, String splitDimName, String tableName, String fileExtension, Canceler canceler)
	{
		if(splitDimName == null || tableName == null || reader == null)
		{
			return null;
		}
		
		reader.readHeader();
		int displayPercentage = -1, currentPercentage = 0;
		try
		{
			// Set up the writers
			JEXStatics.statusBar.setStatusText("Creating ARFF Writers: 0%");
			Dim splitDim = reader.dimTable.getDimWithName(splitDimName);
			TreeMap<String,JEXTableWriter> writers = new TreeMap<String,JEXTableWriter>();
			double count = 0;
			double total = splitDim.size();
			displayPercentage = 0;
			for (String newDim : splitDim.dimValues)
			{
				DimTable subTable = reader.dimTable.getSubTable(new DimensionMap(splitDimName + "=" + newDim));
				JEXTableWriter writer = new JEXTableWriter(tableName, fileExtension);
				writer.writeHeader(subTable, reader.isNumeric());
				writers.put(newDim, writer);
				
				count++;
				currentPercentage = (int) (100.0 * count / total);
				if(currentPercentage != displayPercentage)
				{
					displayPercentage = currentPercentage;
					JEXStatics.statusBar.setStatusText("Preparing ARFF Files: " + displayPercentage + "%");
				}
			}
			
			// Write each datapoint to a file
			JEXStatics.statusBar.setStatusText("Splitting ARFFs: 0%");
			Instance instance;
			Pair<DimensionMap,E> pair;
			count = 0;
			total = reader.dimTable.mapCount();
			displayPercentage = 0;
			while ((instance = reader.loader.m_ArffReader.readInstance(reader.structure)) != null)
			{
				if(canceler.isCanceled())
				{
					// Exit gracefully
					for (String splitDimValue : splitDim.dimValues)
					{
						JEXTableWriter writer = writers.get(splitDimValue);
						writer.close();
					}
					return null;
				}
				pair = reader.getPair(instance);
				DimensionMap map = pair.p1;
				JEXTableWriter theWriter = writers.get(map.get(splitDimName));
				theWriter.writeData(map, pair.p2);
				
				count++;
				currentPercentage = (int) (100.0 * count / total);
				if(currentPercentage != displayPercentage)
				{
					displayPercentage = currentPercentage;
					JEXStatics.statusBar.setStatusText("Splitting ARFF: " + displayPercentage + "%");
				}
			}
			
			// Close the writers and return the filePaths
			TreeMap<DimensionMap,String> filePaths = new TreeMap<DimensionMap,String>();
			JEXStatics.statusBar.setStatusText("Defragging ARFFs: 0%");
			count = 0;
			total = splitDim.size();
			displayPercentage = 0;
			for (String splitDimValue : splitDim.dimValues)
			{
				JEXTableWriter writer = writers.get(splitDimValue);
				writer.close();
				String path = JEXTableWriter.defrag(writer.getPath());
				filePaths.put(new DimensionMap(splitDimName + "=" + splitDimValue), path);
				
				count++;
				currentPercentage = (int) (100.0 * count / total);
				if(currentPercentage != displayPercentage)
				{
					displayPercentage = currentPercentage;
					JEXStatics.statusBar.setStatusText("Defragging ARFFs: " + displayPercentage + "%");
				}
			}
			DimTable outputDimTable = new DimTable();
			outputDimTable.add(splitDim.copy());
			return new Table<String>(outputDimTable, filePaths);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Call this before rereading/requerying the same table.
	 */
	private void reset()
	{
		try
		{
			// Read file through the header
			this.loader.reset();
			this.loader.getStructure();
			while (this.loader.m_ArffReader.m_Tokenizer.nextToken() != StreamTokenizer.TT_EOL)
			{}
			;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	protected <E> Pair<DimensionMap,E> getPair(Instance instance)
	{
		DimensionMap map = new DimensionMap();
		E value = null;
		for (int i = 0; i < this.structure.numAttributes() - 1; i++)
		{
			map.put(this.structure.attribute(i).name(), instance.stringValue(i));
		}
		if(this.isNumeric())
		{
			double val = instance.value(this.structure.numAttributes() - 1);
			if(Double.isNaN(val))
			{
				return new Pair<DimensionMap,E>(map, null);
			}
			else
			{
				value = (E) (new Double(val));
			}
		}
		else
		{
			String val = instance.toString(this.structure.numAttributes() - 1);
			if(val.equals("?"))
			{
				return new Pair<DimensionMap,E>(map, null);
			}
			else
			{
				value = (E) instance.stringValue(this.structure.numAttributes() - 1);
			}
		}
		
		return new Pair<DimensionMap,E>(map, value);
	}
}
