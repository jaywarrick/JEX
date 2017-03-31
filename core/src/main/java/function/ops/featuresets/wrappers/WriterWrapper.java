package function.ops.featuresets.wrappers;

import java.util.Set;

import Database.DBObjects.JEXData;
import Database.DataWriter.FileWriter;
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
	
	public void write(DimensionMap map, Double value) {
		if (this.writer == null) {
			this.writer = new JEXCSVWriter();
			this.writer.writeHeader(map);
		}
		DimensionMap temp = map.copy();
		if (header == null) {
			header = map.copy().keySet();
		}
		for (String s : header) {
			if (!map.containsKey(s)) {
				temp.put(s, "NA");
			}
		}
		writer.write(temp, value.toString());
	}
	
	public Pair<JEXData,JEXData> close() {
		Logs.log("Closing the function (closing file writers and converting output file to arff as well).", this);
		if(this.writer == null)
		{
			return new Pair<JEXData,JEXData>(null, null);
		}
		writer.close();
		String csvPath = writer.getPath();
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
				Logs.log("Went past end of file!?!", this);
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
				Logs.log("Went past end of file!?!", this);
			}
		}

		// Close and save the data.
		reader.close();
		String arffPath = arffWriter.getPath();
		arffWriter.close();
		JEXData outputCSV = FileWriter.makeFileObject("temp", null, csvPath);
		JEXData outputARFF = FileWriter.makeFileObject("temp", null, arffPath);
		return new Pair<JEXData,JEXData>(outputCSV, outputARFF);
		
	}

}
