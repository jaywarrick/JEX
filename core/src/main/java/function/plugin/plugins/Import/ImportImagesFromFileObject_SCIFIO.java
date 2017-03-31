package function.plugin.plugins.Import;

import java.io.File;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataWriter.ValueWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXStatics;
import miscellaneous.FileUtility;
import miscellaneous.Pair;
import tables.DimensionMap;

@Plugin(
		type = JEXPlugin.class,
		name="Import Images From File Object (SCIFIO)",
		menuPath="Import",
		visible=true,
		description="Import images of (nearly) any format and (nearly) any dimensionality"
				+ "(e.g. one or many ND2 files or tif stacks)."
		)
public class ImportImagesFromFileObject_SCIFIO extends JEXPlugin {

	public ImportImagesFromFileObject_SCIFIO() {}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Files", type=MarkerConstants.TYPE_FILE, description="Files to read in as images.", optional=false)
	JEXData fileData;

	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=-1, name="Name-Value Pair Separator", description="Charactor that separates dimension name-value pairs in the image name (e.g., '_' in X.002_Y.003.tif). Use blank (i.e., no character) to avoid parsing anything whatsoever.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String dimSeparator;
	
	@ParameterMarker(uiOrder=0, name="Name-Value Separator", description="Charactor that separates the name and value of the name-value pair in the image name (e.g., '.' in X.002_Y.003.tif). Use blank (i.e., no character) to split on first numeric character.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String valueSeparator;

	@ParameterMarker(uiOrder=1, name="Montage Rows", description="If this image is a montage and is to be split, how many rows are in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imRows;

	@ParameterMarker(uiOrder=2, name="Montage Cols", description="If this image is a montage and is to be split, how many cols are in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imCols;

	@ParameterMarker(uiOrder=3, name="Binning", description="Amount to bin the pixels to reduce image size. Value of 1 skips binning. Partial values converted to scale operation (e.g., bin=3.5 is converted to scale=1/3.5)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	double binning;

	@ParameterMarker(uiOrder=4, name="Binning Method", description="Method for binning the image.", ui=MarkerConstants.UI_DROPDOWN, choices={"NONE", "NEAREST NEIGHBOR", "BILINEAR", "BICUBIC"}, defaultChoice = 2)
	String binMethod;

	@ParameterMarker(uiOrder=5, name="Gather channel names (and other metadata)?", description="Transfer the name of each channel (e.g. DAPI, FITC, etc) if available in the metadata of the image. Otherwise, channels are named by index in the order they were provided by the image. Text from the entire metadata is saved as a file.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean transferNames;
	
	@ParameterMarker(uiOrder=6, name="Dimension to separate (optional)", description="Optionally name a dimension of the image set to separate into different image objects.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
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

		// FIGURE OUT IF ONE OR MULTIPLE FILES ARE BEING IMPORTED
		List<File> pendingImageFiles = new Vector<File>(); // contains either one or multiple image files; depends on whether one file is selected or a whole directory

		TreeMap<DimensionMap,String> files = FileReader.readObjectToFilePathTable(fileData);

		String fileExtension = null;
		for(String filePath : files.values())
		{
			// initialize pendingImageFiles and add the single file
			JEXStatics.statusBar.setStatusText("Adding file "+ filePath +" to list.");
			pendingImageFiles.add(new File(filePath));
			if(fileExtension == null)
			{
				fileExtension = FileUtility.getFileNameExtension(filePath);
			}
		}

		// DO something
		Pair<Vector<JEXData>, JEXData> imagesAndMetaData = io.importFiles(this.output.getDataObjectName(), pendingImageFiles, this.dimSeparator, this.valueSeparator, fileExtension, this.imRows, this.imCols, binning, binMethod, "ImRow", "ImCol", this.transferNames, this, this.dimensionToSplit);
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
}
