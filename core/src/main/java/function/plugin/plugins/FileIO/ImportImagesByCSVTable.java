package function.plugin.plugins.FileIO;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.LabelReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ValueWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXDialog;
import logs.Logs;
import miscellaneous.FileUtility;
import miscellaneous.JEXCSVReader;
import miscellaneous.Pair;
import tables.DimensionMap;

@Plugin(
		type = JEXPlugin.class,
		name="Import Images by CSV Table",
		menuPath="File IO",
		visible=true,
		description="Import images of (nearly) any format and (nearly) any dimensionality"
				+ "(e.g. one or many ND2 files or tif stacks). Use lables to define which files to import into which entries."
		)
public class ImportImagesByCSVTable extends JEXPlugin {

	public ImportImagesByCSVTable() {}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Label 1", type=MarkerConstants.TYPE_LABEL, description="First label by which to filter the file list.", optional=false)
	JEXData labelData1;

	@InputMarker(uiOrder=1, name="Label 2", type=MarkerConstants.TYPE_LABEL, description="Second label by which to filter the file list.", optional=true)
	JEXData labelData2;

	@InputMarker(uiOrder=1, name="Label 3", type=MarkerConstants.TYPE_LABEL, description="Third label by which to filter the file list.", optional=true)
	JEXData labelData3;

	@InputMarker(uiOrder=1, name="Label 4", type=MarkerConstants.TYPE_LABEL, description="Fourth label by which to filter the file list.", optional=true)
	JEXData labelData4;

	@InputMarker(uiOrder=1, name="Label 5", type=MarkerConstants.TYPE_LABEL, description="Fifth label by which to filter the file list.", optional=true)
	JEXData labelData5;

	@InputMarker(uiOrder=1, name="Label 6", type=MarkerConstants.TYPE_LABEL, description="Sixth label by which to filter the file list.", optional=true)
	JEXData labelData6;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Input File", description="CSV Table of the files to import. Headers define image dimensions.", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	String inFile;

	@ParameterMarker(uiOrder=1, name="Column Name with File Path", description="What is the name of the column in the table file that defines the file path?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="PATH")
	String pathCol;

	@ParameterMarker(uiOrder=3, name="Montage Rows", description="If this image is a montage and is to be split, how many rows are in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imRows;

	@ParameterMarker(uiOrder=4, name="Montage Cols", description="If this image is a montage and is to be split, how many cols are in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imCols;

	@ParameterMarker(uiOrder=5, name="Binning", description="Amount to bin the pixels to reduce image size. Value of 1 skips binning. Partial values converted to scale operation (e.g., bin=3.5 is converted to scale=1/3.5)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	double binning;

	@ParameterMarker(uiOrder=6, name="Binning Method", description="Method for binning the image.", ui=MarkerConstants.UI_DROPDOWN, choices={"NONE", "NEAREST NEIGHBOR", "BILINEAR", "BICUBIC"}, defaultChoice = 2)
	String binMethod;

	@ParameterMarker(uiOrder=7, name="Gather channel names?", description="Transfer the name of each channel (e.g. DAPI, FITC, etc) if available in the metadata of the image. Otherwise, channels are named by index in the order they were provided by the image.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean transferNames;

	@ParameterMarker(uiOrder=8, name="Filename Matching Method", description="How to match the name. 1) If filename 'contains each' label name/value pair, 2) If filename 'contains all' name/value pair in uninterrupted sequence spearated by the provided separator, or 3) If filename 'exactly matches' the sequence of label name/value paires separated by provided separator (no extra chars).", ui=MarkerConstants.UI_DROPDOWN, choices={"Contains Each","Contains All","Exactly Matches"}, defaultChoice=0)
	String method;	

	@ParameterMarker(uiOrder=9, name="Overwrite?", description="Overwrite existing object of the same name if it exists in that particular entry?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean overwrite;
	
	@ParameterMarker(uiOrder=10, name="Dimension to separate (optional)", description="Optionally name a dimension of the image set to separate into different image objects.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String dimensionToSplit;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Imported Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The imported image object", enabled=true)
	JEXData output;

	@OutputMarker(uiOrder=2, name="List of Imported Files", type=MarkerConstants.TYPE_VALUE, flavor="", description="The list of imported files.", enabled=true)
	JEXData inputFileList;

	@OutputMarker(uiOrder=3, name="Metadata", type=MarkerConstants.TYPE_FILE, flavor="", description="The imported image object metadata", enabled=true)
	JEXData meta;
	
	@OutputMarker(uiOrder=4, name="Split Imported Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The imported image objects", enabled=true)
	Vector<JEXData> output2 = new Vector<JEXData>();


	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	@Override
	public boolean run(JEXEntry optionalEntry) {

		if(!overwrite)
		{
			if(optionalEntry.getData(this.output.getTypeName()) != null)
			{
				// Skip
				Logs.log("Skipping this entry: x" + optionalEntry.getTrayX() + " y"  +optionalEntry.getTrayY() + ". Found existing data of same type and name: " + output.getTypeName(), this);
				return true;
			}
		}

		// GATHER DATA FROM PARAMETERS
		// create file object for input directory
		File filePath = new File(inFile);
		if(!filePath.exists() || !filePath.isFile())
		{
			JEXDialog.messageDialog("The file could not be found or was not a file (e.g., might have been a directory). Aborting as this function requires a file. Entry X" + optionalEntry.getTrayX() + " Y" + optionalEntry.getTrayY());
			return false;
		}
		TreeMap<DimensionMap, String> paths = JEXCSVReader.getCSVTable(inFile, true);

		// SETUP THE PENDING IMAGE FILES FOR THIS ENTRY
		// Gather a list of the provided labels.
		List<JEXData> labels = new Vector<JEXData>();
		if(labelData1 != null && labelData1.getTypeName().getType().matches(JEXData.LABEL))
		{
			labels.add(labelData1);
		}
		if(labelData2 != null && labelData2.getTypeName().getType().matches(JEXData.LABEL))
		{
			labels.add(labelData2);
		}
		if(labelData3 != null && labelData3.getTypeName().getType().matches(JEXData.LABEL))
		{
			labels.add(labelData3);
		}
		if(labelData4 != null && labelData4.getTypeName().getType().matches(JEXData.LABEL))
		{
			labels.add(labelData4);
		}
		if(labelData5 != null && labelData5.getTypeName().getType().matches(JEXData.LABEL))
		{
			labels.add(labelData5);
		}
		if(labelData6 != null && labelData6.getTypeName().getType().matches(JEXData.LABEL))
		{
			labels.add(labelData6);
		}

		TreeMap<DimensionMap,String> filteredFiles = this.filterFiles(paths, labels);

		if(filteredFiles.size() == 0)
		{
			Logs.log("Skipping this entry: x" + optionalEntry.getTrayX() + " y"  +optionalEntry.getTrayY() + ". No files found to import.", this);
			for(JEXData label : labels)
			{
				Logs.log(label.getDataObjectName() + " = " + LabelReader.readLabelValue(label), this);
			}
			return false;
		}

		TreeMap<DimensionMap,String> valueTable = new TreeMap<>();
		
		Vector<File> tempList = new Vector<>();
		for(Entry<DimensionMap,String> e : filteredFiles.entrySet())
		{
			tempList.add(new File(e.getValue()));
		}
		
		// DO something
		ImportImages_SCIFIO io = new ImportImages_SCIFIO();
		Pair<Vector<JEXData>, JEXData> imagesAndMetaData = io.importFiles(this.output.getDataObjectName(), tempList, "", "", FileUtility.getFileNameExtension(filteredFiles.firstEntry().getValue()), this.imRows, this.imCols, this.binning, this.binMethod, "ImRow", "ImCol", this.transferNames, this, this.dimensionToSplit);
		if(imagesAndMetaData.p1.get(0) == null || (imagesAndMetaData.p1.size() < 2 && imagesAndMetaData.p1.get(0).getDataObjectName().equals(this.output.getDataObjectName())))
		{
			// We have a single output to return.
			this.output = imagesAndMetaData.p1.get(0);
		}
		else
		{
			// We have multiple outputs to return due to splitting the 
			this.output2 = imagesAndMetaData.p1;
		}
		
		this.inputFileList = ValueWriter.makeValueTable("temp", valueTable);
		this.meta = FileWriter.makeFileObject("temp", null, imagesAndMetaData.p2);

		return true;
	}

	public List<String> getLabels(List<JEXData> labelList)
	{
		Vector<String> ret = new Vector<String>();
		for(JEXData label : labelList)
		{
			String name = LabelReader.readLabelName(label);
			String value = LabelReader.readLabelValue(label);
			ret.add(name + value);
		}
		return ret;
	}

	public TreeMap<DimensionMap,String> filterFiles(TreeMap<DimensionMap,String> paths, JEXData label)
	{
		TreeMap<DimensionMap,String> ret = new TreeMap<>();
		for(Entry<DimensionMap,String> path : paths.entrySet())
		{
			String name = LabelReader.readLabelName(label);
			String val =LabelReader.readLabelValue(label);
			if(path.getKey().get(name) != null && path.getKey().get(name).equals(val))
			{
				ret.put(path.getKey().copy(), path.getValue());
			}
		}
		return ret;
	}

	public TreeMap<DimensionMap,String> filterFiles(TreeMap<DimensionMap,String> paths, List<JEXData> labelList)
	{
		TreeMap<DimensionMap,String> temp = new TreeMap<>();
		for(Entry<DimensionMap,String> e : paths.entrySet())
		{
			temp.put(e.getKey().copy(), e.getValue());
		}
		for(JEXData label : labelList)
		{
			temp = this.filterFiles(temp, label);
		}
		return temp;
	}
}
