package function.plugin.plugins.multipleMyeloma;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXEntry;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FileUtility;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 */

@Plugin(
		type = JEXPlugin.class,
		name="Reorganize Image Flow DAFs",
		menuPath="Multiple Myeloma",
		visible=true,
		description="Organize IDEAS image flow data analysis files (DAFs) into individual folders to aid exporting of tifs."
		)
public class ReorganizeImageFlowDAFs extends JEXPlugin {

	public ReorganizeImageFlowDAFs()
	{}

	/////////// Define Inputs ///////////

	//	@InputMarker(uiOrder=1, name="Data Table (compiled)", type=MarkerConstants.TYPE_FILE, description="Table of (R)ed and (G)reen fluorescence data in arff format.", optional=false)
	//	JEXData folderData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Folder of DAFs", description="Select the folder that contains the DAFs you want to organize into individual folders.", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	String folderString;

	@ParameterMarker(uiOrder=1, name="Do nested folder search?", description="Search subfolders as well?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean nestedSearch;

	/////////// Define Outputs ///////////

	//	@OutputMarker(uiOrder=1, name="Single-Cell Table", type=MarkerConstants.TYPE_FILE, flavor="", description="The live dead information for each cell in the compiled data.", enabled=true)
	//	JEXData singleCellOutput;

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		File folder = new File(folderString);

		if(!folder.exists())
		{
			JEXDialog.messageDialog("The specified folder was not found: " + folderString, this);
			return false;
		}

		Collection<File> files = null;
		if(nestedSearch)
		{
			files = FileUtils.listFiles(folder, new RegexFileFilter("^(.*?)"), DirectoryFileFilter.DIRECTORY);
		}
		else
		{
			files = Arrays.asList(folder.listFiles());
		}

		int count = 0, total = files.size(), percentage = 0;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}

		for(File f : files)
		{
			if(this.isCanceled())
			{
				return false;
			}
			if(FileUtility.getFileNameExtension(f.getAbsolutePath()).equals("daf"))
			{
				File parent = f.getParentFile();
				if(parent.isDirectory())
				{
					File newLoc_DAF = new File(FileUtility.getFileParent(f.getAbsolutePath()) + File.separator + FileUtility.getFileNameWithoutExtension(f.getAbsolutePath()) + File.separator + FileUtility.getFileNameWithExtension(f.getAbsolutePath()));
					File newLoc_RIF = new File(FileUtility.getFileParent(f.getAbsolutePath()) + File.separator + FileUtility.getFileNameWithoutExtension(f.getAbsolutePath()) + File.separator + FileUtility.getFileNameWithoutExtension(f.getAbsolutePath()) + ".rif");
					File newLoc_CIF = new File(FileUtility.getFileParent(f.getAbsolutePath()) + File.separator + FileUtility.getFileNameWithoutExtension(f.getAbsolutePath()) + File.separator + FileUtility.getFileNameWithoutExtension(f.getAbsolutePath()) + ".cif");
					File oldLoc_RIF = new File(FileUtility.getFileParent(f.getAbsolutePath()) + File.separator + FileUtility.getFileNameWithoutExtension(f.getAbsolutePath()) + ".rif");
					File oldLoc_CIF = new File(FileUtility.getFileParent(f.getAbsolutePath()) + File.separator + FileUtility.getFileNameWithoutExtension(f.getAbsolutePath()) + ".cif");

					try
					{
						// Makes new directories if necessary
						Logs.log("Moving " + f.getAbsolutePath() + " into its own directory in the same place with the same name.", this);
						FileUtility.moveFileOrFolder(f, newLoc_DAF, true);
						if(oldLoc_RIF.exists())
						{
							FileUtility.moveFileOrFolder(oldLoc_RIF, newLoc_RIF, true);
						}
						if(oldLoc_CIF.exists())
						{
							FileUtility.moveFileOrFolder(oldLoc_CIF, newLoc_CIF, true);
						}
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					count = count + 1;
				}
			}
		}

		// Return status
		return true;
	}

}
