package function.plugin.plugins.imageThresholding;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXEntry;
import fiji.threshold.Auto_Local_Threshold;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.ParameterMarker;


@Plugin(
		type = JEXPlugin.class,
		name="Auto Local Threshold",
		menuPath="Image Thresholding > Auto Local Threshold",
		visible=true,
		description="Function that allows you to stitch an image ARRAY into a single image using two image alignment objects."
		)
public class AutoLocalThresholding extends JEXPlugin{

	/////////// Define Inputs ///////////

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Method", description="select algorithm to be applied", ui=MarkerConstants.UI_DROPDOWN, choices={"Bernsen", "Contrast", "Mean", "Median", "MidGrey", "Niblack", "Otsu", "Phansalkar", "Sauvola"}, defaultChoice=0)
	String startPt;

	@Override
	public boolean run(JEXEntry optionalEntry) {
		// TODO Auto-generated method stub
		return false;
	}

	//////////Define Outputs ///////////
}
