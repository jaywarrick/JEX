package function.plugin.plugins.dataImporting;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;


@Plugin(
		type = JEXPlugin.class,
		name="Convert Tiffs to Image Object",
		menuPath="Data Importing",
		visible=true,
		description="Modification of JEX-ConvertNISElementsTiffs that instead"
				+ "converts multicolor Tiff images to an IMAGE DataType object."
				+ " This plugin uses new JEX plugin interface."
		)
public class ConvertTiffsToImageObject extends JEXPlugin {

	public ConvertTiffsToImageObject()
	{}

	/////////// Define Inputs ///////////

	/*
	 * None necessary; Input Directory is classified as a parameter but could
	 * still be considered an input.
	 */

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Input Directory", description="Location of the multicolor Tiff images", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	double inDir;

	@ParameterMarker(uiOrder=1, name="ImRows", description="Number of rows in 'Large Image Array'", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	double oldMax;

	@ParameterMarker(uiOrder=2, name="ImCols", description="Number of columns in 'Large Image Array'", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	double newMin;


	/////////// Define Outputs ///////////

	@OutputMarker(name="Converted Image Object", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The converted image object", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry) {
		// TODO Auto-generated method stub
		return false;
	}

}
