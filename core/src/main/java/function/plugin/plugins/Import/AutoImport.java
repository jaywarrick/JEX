package function.plugin.plugins.Import;

import java.io.File;

import org.scijava.plugin.Plugin;

import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;

@Plugin(
		type = JEXPlugin.class,
		name="Import Images as Image Object",
		menuPath="Import",
		visible=true,
		description="Import images of any format and any number "
				+ "(e.g. one ND2 file or multiple tif files)"
		)
public class AutoImport extends JEXPlugin {

	public AutoImport() {}

	/////////// Define Inputs ///////////

	/*
	 * None necessary; Input Directory is classified as a parameter.
	 */

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Input Directory", description="Location of the multicolor TIFF images", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	String inDir;
	
	@ParameterMarker(uiOrder=1, name="File Extension", description="The type of file that is being imported. Default is .tif. Not necessary if importing a single file.", ui=MarkerConstants.UI_TEXTFIELD, defaultText=".tif")
	int fileExtension;

	/////////// Define Outputs ///////////

	@OutputMarker(name="Imported Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The imported image object", enabled=true)
	JEXData output;
	
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry) {
		// TODO Tom (see line below)
		/* FIGURE OUT IF ONE OR MULTIPLE FILES ARE BEING IMPORTED */
		File filePath = new File(inDir);
		boolean isDirectory = filePath.isDirectory(); // checks whether user entered a single file or an entire directory
		
		
		
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * TODO for Mengcheng to fill in
	 */
	public DimensionMap getMapFromPath(String filePath) {
		return null; // so that it compiles
	}
	
}
