package function.plugin.plugins.Import;

import java.io.File;

import miscellaneous.FileUtility;

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
	
}
