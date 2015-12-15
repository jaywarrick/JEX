package function.plugin.plugins.Import;

import java.io.File;
import java.util.List;
import java.util.Vector;

import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import miscellaneous.FileUtility;
import miscellaneous.SimpleFileFilter;

import org.scijava.plugin.Plugin;

import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.LabelReader;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;

@Plugin(
		type = JEXPlugin.class,
		name="Import Images by Label (SCIFIO)",
		menuPath="Import",
		visible=true,
		description="Import images of (nearly) any format and (nearly) any dimensionality"
				+ "(e.g. one or many ND2 files or tif stacks). Use lables to define which files to import into which entries."
		)
public class ImportByImagesLabel extends JEXPlugin {
	
	public ImportByImagesLabel() {}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Label 1", type=MarkerConstants.TYPE_LABEL, description="First label in file name.", optional=false)
	JEXData labelData1;
	
	@InputMarker(uiOrder=1, name="Label 2", type=MarkerConstants.TYPE_LABEL, description="Second label in file name.", optional=true)
	JEXData labelData2;
	
	@InputMarker(uiOrder=1, name="Label 3", type=MarkerConstants.TYPE_LABEL, description="Third label in file name.", optional=true)
	JEXData labelData3;
	
	@InputMarker(uiOrder=1, name="Label 4", type=MarkerConstants.TYPE_LABEL, description="Fourth label in file name.", optional=true)
	JEXData labelData4;
	
	@InputMarker(uiOrder=1, name="Label 5", type=MarkerConstants.TYPE_LABEL, description="Fifth label in file name.", optional=true)
	JEXData labelData5;
	
	@InputMarker(uiOrder=1, name="Label 6", type=MarkerConstants.TYPE_LABEL, description="Sixth label in file name.", optional=true)
	JEXData labelData6;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=0, name="Input Directory/File", description="Location of the multicolor TIFF images", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	String inDir;
	
	@ParameterMarker(uiOrder=1, name="File Extension", description="The type of file that is being imported. Default is tif. Not necessary if importing a single file.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="tif")
	String fileExtension;
	
	@ParameterMarker(uiOrder=2, name="Separator Character", description="Charactor that separates dimension names in the image name (e.g., '_' in X002_Y003.tif). Use blank (i.e., no character) to avoid parsing.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="_")
	String separator;
	
	@ParameterMarker(uiOrder=3, name="Montage Rows", description="If this image is a montage and is to be split, how many rows are in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imRows;
	
	@ParameterMarker(uiOrder=4, name="Montage Cols", description="If this image is a montage and is to be split, how many cols are in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imCols;
	
	@ParameterMarker(uiOrder=5, name="Gather channel names?", description="Transfer the name of each channel (e.g. DAPI, FITC, etc) if available in the metadata of the image. Otherwise, channels are named by index in the order they were provided by the image.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean transferNames;
	
	@ParameterMarker(uiOrder=6, name="Filename Matching Method", description="How to match the name. 1) If filename 'contains each' label name/value pair, 2) If filename 'contains all' name/value pair in uninterrupted sequence spearated by the provided separator, or 3) If filename 'exactly matches' the sequence of label name/value paires separated by provided separator (no extra chars).", ui=MarkerConstants.UI_DROPDOWN, choices={"Contains Each","Contains All","Exactly Matches"}, defaultChoice=0)
	String method;	
	
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
		
		// GATHER DATA FROM PARAMETERS
		// create file object for input directory
		File filePath = new File(inDir);
		
		// SETUP THE PENDING IMAGE FILES FOR THIS ENTRY
		// Gather a list of the provided labels.
		List<JEXData> labels = new Vector<JEXData>();
		if(!JEXPlugin.isInputValid(labelData1, JEXData.LABEL))
		{
			return false;
		}
		labels.add(labelData1);
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
		
		// First gather all the files chosen and then we'll filter them for this entry
		List<File> pendingImageFiles; // contains either one or multiple image files; depends on whether one file is selected or a whole directory		
		
		if(!filePath.exists())
		{
			JEXDialog.messageDialog("Warning! The file or folder specified could not be found.");
			return false;
		}
		if (filePath.isDirectory()) { // multiple files
			// gather and sort the input files
			JEXStatics.statusBar.setStatusText("Sorting files to convert. May take minutes...");
			pendingImageFiles = FileUtility.getSortedFileList(filePath.listFiles(new SimpleFileFilter(new String[] { fileExtension })));
		}
		else { // one file
			// initialize pendingImageFiles and add the single file
			JEXStatics.statusBar.setStatusText("Adding file "+inDir+" to list.");
			pendingImageFiles = new Vector<File>();
			pendingImageFiles.add(filePath);
		}
		
		// Filter the file list according to the label filters
		pendingImageFiles = this.filterFiles(pendingImageFiles, labels);
		
		output = ImportImages_SCIFIO.importFiles(pendingImageFiles, this.separator, this.fileExtension, this.imRows, this.imCols, "ImRow", "ImCol", this.transferNames, this);
		
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
	
	public String getCatString(List<JEXData> labelList)
	{
		List<String> stringList = getLabels(labelList);
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < stringList.size()-1; i++)
		{
			sb.append(stringList.get(i));
			sb.append(this.separator);
		}
		sb.append(stringList.get(stringList.size()-1));
		return(sb.toString());
	}
	
	public boolean containsEachTest(String fileString, List<JEXData> labelList)
	{
		String[] pieces = fileString.split("\\" + separator);
		List<String> piecesList = new Vector<String>();
		for(String peice : pieces)
		{
			piecesList.add(peice);
		}
		List<String> labels = getLabels(labelList);
		for(String label : labels)
		{
			if(!piecesList.contains(label))
			{
				return false;
			}
		}
		return true;
	}
	
	public boolean containsAllTest(String fileString, List<JEXData> labelList)
	{
		String[] pieces = fileString.split("\\" + separator);
		List<String> labels = getLabels(labelList);
		
		for(int i = 0; i <= pieces.length - labels.size(); i++)
		{
			boolean match = true;
			for(int j = 0; j < labels.size(); j++)
			{
				if(!labels.get(j).equals(pieces[i]))
				{
					match = false;
				}
			}
			if(match)
			{
				return true;
			}
		}
		
		return false;
	}
	
	public boolean exactlyMatchesTest(String fileString, List<JEXData> labelList)
	{
		String toTest = getCatString(labelList);
		return(fileString.equals(toTest));
	}
	
	public boolean matches(String fileName, List<JEXData> labelList)
	{
		if(!FileUtility.getFileNameExtension(fileName).equals(this.fileExtension))
		{
			return false;
		}
		String fileString = FileUtility.getFileNameWithoutExtension(fileName);
		if(this.method.equals("Contains Each"))
		{
			return this.containsEachTest(fileString, labelList);
		}
		else if(this.method.equals("Contains All"))
		{
			return this.containsAllTest(fileString, labelList);
		}
		else
		{
			return this.exactlyMatchesTest(fileString, labelList);
		}
	}
	
	public List<File> filterFiles(List<File> files, List<JEXData> labelList)
	{
		List<File> ret = new Vector<File>();
		for(File f : files)
		{
			if(this.matches(f.getName(), labelList))
			{
				ret.add(f);
			}
		}
		return ret;
	}
}
