package function.ops.featuresets.wrappers;

import java.util.Set;

import logs.Logs;
import miscellaneous.JEXCSVReader;
import miscellaneous.JEXCSVWriter;
import miscellaneous.Pair;
import tables.DimTableBuilder;
import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;

public class WriterWrapper {
	
	public JEXCSVWriter writer = null;
	public Set<String> header = null;
	
	public static synchronized void write(WriterWrapper writer, DimensionMap map, Double value)
	{
		if (writer.writer == null) {
			writer.writer = new JEXCSVWriter();
			writer.writer.writeHeader(map);
		}
		DimensionMap temp = map.copy();
		if (writer.header == null) {
			writer.header = map.copy().keySet();
		}
		for (String s : writer.header) {
			if (!map.containsKey(s)) {
				temp.put(s, "NA");
			}
		}
		writer.writer.write(temp, value.toString());
	}
	
	public static synchronized Pair<String,String> close(WriterWrapper writer, boolean saveArff)
	{
		Logs.log("Closing the function (closing file writers and converting output file to arff as well).", WriterWrapper.class);
		if(writer == null || writer.writer == null)
		{
			return new Pair<String,String>(null, null);
		}
		writer.writer.close();
		String csvPath = writer.writer.getPath();
		
		String arffPath = null;
		if(saveArff)
		{
			JEXCSVReader reader = new JEXCSVReader(csvPath, true);

			// In order to write an Arff table we need to build a DimTable
			// We can't keep all the data in memory as it might be too large so just
			// build DimTable for now.
			DimTableBuilder builder = new DimTableBuilder();
			while (!reader.isEOF()) {
				Pair<DimensionMap, String> toAdd = reader.readRowToDimensionMapString();
				if (toAdd != null) {
					builder.add(toAdd.p1);
				} else {
					Logs.log("Went past end of file!?!", WriterWrapper.class);
				}
			}
			reader.close();

			// Now that we have the DimTable we can transfer each row of the csv to
			// the arff file after writing the header.
			reader = new JEXCSVReader(csvPath, true);
			JEXTableWriter arffWriter = new JEXTableWriter("FeatureTable", "arff");
			arffWriter.writeNumericTableHeader(builder.getDimTable());
			while (!reader.isEOF()) {
				Pair<DimensionMap, String> result = reader.readRowToDimensionMapString();
				if (result != null) {
					arffWriter.writeData(result.p1, Double.parseDouble(result.p2));
				} else {
					Logs.log("Went past end of file!?!", WriterWrapper.class);
				}
			}

			// Close and save the data.
			reader.close();
			arffPath = arffWriter.getPath();
			arffWriter.close();
			//			outputARFF = FileWriter.makeFileObject("temp", null, arffPath);
		}
		
		// Always do this at the end (i.e., before trying to make an Arff version as doing so moves the location of the file prior to conversion cause a FileNotFoundException). Here, everything is fine.
		//			JEXData outputCSV = FileWriter.makeFileObject("temp", null, csvPath);
		
		return new Pair<String,String>(csvPath, arffPath);
	}
}
