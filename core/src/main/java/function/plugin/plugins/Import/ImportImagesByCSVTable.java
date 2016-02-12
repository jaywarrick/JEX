package function.plugin.plugins.Import;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DBObjects.JEXLabel;
import Database.DataReader.ImageReader;
import Database.DataReader.LabelReader;
import Database.DataWriter.ImageWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXDialog;
import logs.Logs;
import miscellaneous.FileUtility;
import miscellaneous.JEXCSVReader;
import tables.DimensionMap;

@Plugin(
		type = JEXPlugin.class,
		name="Import Images by CSV Table",
		menuPath="Import",
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
	
	@ParameterMarker(uiOrder=5, name="Gather channel names?", description="Transfer the name of each channel (e.g. DAPI, FITC, etc) if available in the metadata of the image. Otherwise, channels are named by index in the order they were provided by the image.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean transferNames;
	
	@ParameterMarker(uiOrder=6, name="Filename Matching Method", description="How to match the name. 1) If filename 'contains each' label name/value pair, 2) If filename 'contains all' name/value pair in uninterrupted sequence spearated by the provided separator, or 3) If filename 'exactly matches' the sequence of label name/value paires separated by provided separator (no extra chars).", ui=MarkerConstants.UI_DROPDOWN, choices={"Contains Each","Contains All","Exactly Matches"}, defaultChoice=0)
	String method;	
	
	@ParameterMarker(uiOrder=7, name="Overwrite?", description="Overwrite existing object of the same name if it exists in that particular entry?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean overwrite;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Imported Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The imported image object", enabled=true)
	JEXData output;
	
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
		if(labelData1 != null && labelData1.getTypeName().getType().equals(JEXData.LABEL))
		{
			labels.add(labelData1);
		}
		if(labelData2 != null && labelData2.getTypeName().getType().equals(JEXData.LABEL))
		{
			labels.add(labelData2);
		}
		if(labelData3 != null && labelData3.getTypeName().getType().equals(JEXData.LABEL))
		{
			labels.add(labelData3);
		}
		if(labelData4 != null && labelData4.getTypeName().getType().equals(JEXData.LABEL))
		{
			labels.add(labelData4);
		}
		if(labelData5 != null && labelData5.getTypeName().getType().equals(JEXData.LABEL))
		{
			labels.add(labelData5);
		}
		if(labelData6 != null && labelData6.getTypeName().getType().equals(JEXData.LABEL))
		{
			labels.add(labelData6);
		}
		
		TreeMap<DimensionMap,String> filteredFiles = this.filterFiles(paths, labels);
		
		if(filteredFiles.size() == 0)
		{
			Logs.log("Skipping this entry: x" + optionalEntry.getTrayX() + " y"  +optionalEntry.getTrayY() + ". No files found to import.", this);
			for(JEXData label : labels)
			{
				Logs.log(label.getDataObjectName() + " = " + ((JEXLabel) label).getLabelValue(), this);
			}
			return false;
		}
		
		TreeMap<DimensionMap,String> toOutput = new TreeMap<>();
		for(Entry<DimensionMap,String> e : filteredFiles.entrySet())
		{
			Vector<File> tempList = new Vector<>();
			tempList.add(new File(e.getValue()));
			JEXData temp = ImportImages_SCIFIO.importFiles(tempList, "", FileUtility.getFileNameExtension(e.getValue()), this.imRows, this.imCols, "ImRow", "ImCol", this.transferNames, this);
			for(Entry<DimensionMap,String> e2 : ImageReader.readObjectToImagePathTable(temp).entrySet())
			{
				DimensionMap toPut = e2.getKey().copy();
				toPut.putAll(e.getKey());
				toOutput.put(toPut, e2.getValue());
			}
		}
		
		output = ImageWriter.makeImageStackFromPaths("temp", toOutput);
		
		return true;
	}
	
	/**
	 * Create DimensionMap of a given image 
	 * The image name should be in certain format, ex. Image_x001_y002_z004.tif
	 * 
	 * @param filePath image Path and Name
	 * @param separator separator of the image Name
	 * @return
	 */
	public DimensionMap getMapFromPath(String filePath, String separator) {
		String name = FileUtility.getFileNameWithoutExtension(filePath);
		String[] names = name.split(separator);
		
		DimensionMap dimMap = new DimensionMap();
		String dimValue, dimName, temp;
		int splitIndex = 0;
		
		for (int i = 0; i < names.length; i++){
			temp = names[i];
			
			// find the first Digit in the string in order to separate dimName and dimValue
			for (int j = 0; j < temp.length(); j++){
				if (Character.isDigit(temp.charAt(j))){
					splitIndex = j;
					break;
				}
				else
					splitIndex = 0;
			}
			
			// if the string is not a dimName followed by a dimValue then skip it.
			if (splitIndex != 0) {
				dimName = temp.substring(0, splitIndex);
				dimValue = temp.substring(splitIndex);
				
				dimMap.put(dimName, dimValue);
			}
		}
		
		return dimMap;
		
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
