package function.plugin.plugins.Import;

import java.io.File;
import java.util.List;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DBObjects.JEXLabel;
import Database.DataReader.LabelReader;
import Database.DataWriter.ValueWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FileUtility;
import miscellaneous.Pair;
import miscellaneous.SimpleFileFilter;

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

	@ParameterMarker(uiOrder=2, name="Name-Value Pair Separator", description="Charactor that separates dimension name-value pairs in the image name (e.g., '_' in X.002_Y.003.tif). Use blank (i.e., no character) to avoid parsing anything whatsoever.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String dimSeparator;
	
	@ParameterMarker(uiOrder=3, name="Name-Value Separator", description="Charactor that separates the name and value of the name-value pair in the image name (e.g., '.' in X.002_Y.003.tif). Use blank (i.e., no character) to split on first numeric character.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String valueSeparator;

	@ParameterMarker(uiOrder=4, name="Montage Rows", description="If this image is a montage and is to be split, how many rows are in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imRows;

	@ParameterMarker(uiOrder=5, name="Montage Cols", description="If this image is a montage and is to be split, how many cols are in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imCols;
	
	@ParameterMarker(uiOrder=6, name="Binning", description="Amount to bin the pixels to reduce image size. Value of 1 skips binning. Partial values converted to scale operation (e.g., bin=3.5 is converted to scale=1/3.5)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	double binning;
	
	@ParameterMarker(uiOrder=7, name="Binning Method", description="Method for binning the image.", ui=MarkerConstants.UI_DROPDOWN, choices={"NONE", "NEAREST NEIGHBOR", "BILINEAR", "BICUBIC"}, defaultChoice = 2)
	String binMethod;

	@ParameterMarker(uiOrder=8, name="Gather channel names?", description="Transfer the name of each channel (e.g. DAPI, FITC, etc) if available in the metadata of the image. Otherwise, channels are named by index in the order they were provided by the image.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean transferNames;

	@ParameterMarker(uiOrder=9, name="Filename Matching Method", description="How to match the name. 1) If filename 'contains each' label name/value pair, 2) If filename 'contains all' name/value pair in uninterrupted sequence spearated by the provided separator, or 3) If filename 'exactly matches' the sequence of label name/value paires separated by provided separator (no extra chars).", ui=MarkerConstants.UI_DROPDOWN, choices={"Contains Each","Contains All","Exactly Matches"}, defaultChoice=0)
	String method;	

	@ParameterMarker(uiOrder=10, name="Overwrite?", description="Overwrite existing object of the same name if it exists in that particular entry?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean overwrite;
	
	@ParameterMarker(uiOrder=11, name="Dimension to separate (optional)", description="Optionally name a dimension of the image set to separate into different image objects.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
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

	private ImportImages_SCIFIO io = new ImportImages_SCIFIO();

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
		File filePath = new File(inDir);

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

		if(pendingImageFiles.size() == 0)
		{
			Logs.log("Skipping this entry: x" + optionalEntry.getTrayX() + " y"  +optionalEntry.getTrayY() + ". No files found to import.", this);
			for(JEXData label : labels)
			{
				Logs.log(label.getDataObjectName() + " = " + ((JEXLabel) label).getLabelValue(), this);
			}
			return false;
		}

		// DO something
		Pair<Vector<JEXData>, JEXData> imagesAndMetaData = io.importFiles(this.output.getDataObjectName(), pendingImageFiles, this.dimSeparator, this.valueSeparator, this.fileExtension, this.imRows, this.imCols, binning, binMethod, "ImRow", "ImCol", this.transferNames, this, this.dimensionToSplit);
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
		this.inputFileList = ValueWriter.makeValueObject("temp", io.getFileList(pendingImageFiles).toString());
		this.meta = imagesAndMetaData.p2;

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

	public String getCatString(List<JEXData> labelList)
	{
		List<String> stringList = getLabels(labelList);
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < stringList.size()-1; i++)
		{
			sb.append(stringList.get(i));
			sb.append(this.dimSeparator);
		}
		sb.append(stringList.get(stringList.size()-1));
		return(sb.toString());
	}

	public boolean containsEachTest(String fileString, List<JEXData> labelList)
	{
		String[] pieces = fileString.split("\\" + this.dimSeparator);
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
		String[] pieces = fileString.split("\\" + this.dimSeparator);
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
		if(!FileUtility.getFileNameExtension(fileName).equals(this.fileExtension) || labelList.size() == 0)
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
