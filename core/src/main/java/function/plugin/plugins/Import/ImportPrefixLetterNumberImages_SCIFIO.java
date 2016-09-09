package function.plugin.plugins.Import;

import java.io.File;
import java.util.List;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.ValueWriter;
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
		name="Import <Prefix><LetterRow><NumberCol>.<ext> (SCIFIO)",
		menuPath="Import",
		visible=true,
		description="Import a single file that matches the filenaming format provided from (nearly) any format and (nearly) any dimensionality"
				+ "(e.g. an ND2 file or tif stack)"
		)
public class ImportPrefixLetterNumberImages_SCIFIO extends JEXPlugin {

	public ImportPrefixLetterNumberImages_SCIFIO() {}

	/////////// Define Inputs ///////////

	/*
	 * None necessary; Input Directory is classified as a parameter.
	 */

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Input Directory/File", description="Location of the multicolor TIFF images", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	String inDir;

	@ParameterMarker(uiOrder=1, name="File Prefix", description="Prefix in the file name preceeding the RowLetterColumnNumber.fileExtension name format.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String prefix;

	@ParameterMarker(uiOrder=1, name="Column Num Digits", description="Number of digits used to define the column number (e.g., 02 is represented with 2 digits)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2")
	int digits;

	@ParameterMarker(uiOrder=2, name="File Extension", description="The type of file that is being imported. Default is nd2.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="nd2")
	String fileExtension;

	@ParameterMarker(uiOrder=3, name="Name-Value Pair Separator", description="Charactor that separates dimension name-value pairs in the image name (e.g., '_' in X.002_Y.003.tif). Use blank (i.e., no character) to avoid parsing anything whatsoever.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String dimSeparator;
	
	@ParameterMarker(uiOrder=4, name="Name-Value Separator", description="Charactor that separates the name and value of the name-value pair in the image name (e.g., '.' in X.002_Y.003.tif). Use blank (i.e., no character) to split on first numeric character.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String valueSeparator;

	@ParameterMarker(uiOrder=5, name="Montage Rows", description="If this image is a montage and is to be split, how many rows are in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imRows;

	@ParameterMarker(uiOrder=6, name="Montage Cols", description="If this image is a montage and is to be split, how many cols are in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imCols;

	@ParameterMarker(uiOrder=7, name="Binning", description="Amount to bin the pixels to reduce image size. Value of 1 skips binning. Partial values converted to scale operation (e.g., bin=3.5 is converted to scale=1/3.5)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	double binning;

	@ParameterMarker(uiOrder=8, name="Binning Method", description="Method for binning the image.", ui=MarkerConstants.UI_DROPDOWN, choices={"NONE", "NEAREST NEIGHBOR", "BILINEAR", "BICUBIC"}, defaultChoice = 2)
	String binMethod;

	@ParameterMarker(uiOrder=9, name="Gather channel names (and other metadata)?", description="Transfer the name of each channel (e.g. DAPI, FITC, etc) if available in the metadata of the image. Otherwise, channels are named by index in the order they were provided by the image. Text from the entire metadata is saved as a file.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean transferNames;
	
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
	
	private ImportImages_SCIFIO io = new ImportImages_SCIFIO();

	@Override
	public boolean run(JEXEntry optionalEntry) {
		// GATHER DATA FROM PARAMETERS
		// create file object for input directory
		File filePath = new File(inDir);

		// FIGURE OUT IF ONE OR MULTIPLE FILES ARE BEING IMPORTED
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

		// Filter files for this particular entry
		String expectedFileName = prefix + getEntryString(optionalEntry.getTrayY(), optionalEntry.getTrayX());
		List<File> newFileList = new Vector<>();
		for(File file : pendingImageFiles)
		{
			if(FileUtility.getFileNameWithoutExtension(file.getAbsolutePath()).equals(expectedFileName))
			{
				newFileList.add(file);
				break;
			}
		}
		if(newFileList.size() == 0)
		{
			Logs.log("No file named '" + expectedFileName + "." + this.fileExtension + "' found for X:" + optionalEntry.getTrayX() + " Y:" + optionalEntry.getTrayY(), this);
			return false;
		}

		// DO something
		Pair<Vector<JEXData>, JEXData> imagesAndMetaData = io.importFiles(this.output.getDataObjectName(), newFileList, this.dimSeparator, this.valueSeparator, this.fileExtension, this.imRows, this.imCols, binning, binMethod, "ImRow", "ImCol", this.transferNames, this, this.dimensionToSplit);
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

	public String getColString(int trayX)
	{
		String formatted = String.format("%0" + digits + "d", trayX);
		return formatted;
	}

	public String getEntryString(int trayY, int trayX)
	{
		StringBuilder sb = new StringBuilder();
		int rowMultiple = (trayY / 26);
		for(int i = 0; i < rowMultiple; i++)
		{
			sb.append(getRowString(rowMultiple));
		}
		sb.append(getRowString(trayY));
		sb.append(getColString(trayX + 1));
		return sb.toString();
	}

	public String getRowString(int trayY)
	{
		return String.valueOf((char)((trayY % 26) + 65));
	}
}
