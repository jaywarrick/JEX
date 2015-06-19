package function.plugin.plugins.Export;

import java.io.IOException;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.ParameterMarker;

public class CellProfiler extends JEXPlugin {

	public CellProfiler() {}

	/////////// Define Inputs ///////////

//	@InputMarker(uiOrder=0, name="Images", type=MarkerConstants.TYPE_IMAGE, description="Images to send to CellProfiler.", optional=false)
//	JEXData imageData;

	/////////// Define Parameters ///////////

//	@ParameterMarker(uiOrder=0, name="Pipeline", description="CellProfiler pipeline to be used", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
//	String pipelinePath;
//
//	@ParameterMarker(uiOrder=1, name="CellProfiler Executable", description="The CellProfiler.exe file", ui=MarkerConstants.UI_FILECHOOSER, defaultText="C:\Program Files\CellProfiler\CellProfiler.exe")
//	String CellProfilerPath;
//
//	@ParameterMarker(uiOrder=2, name="Image Folder", description="Location to export images to", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
//	String imagePath;
//
//	@ParameterMarker(uiOrder=3, name="Output Folder", description="Where the CellProfiler output goes", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
//	double outputPath;

	/////////// Define Outputs ///////////

	// No Outputs

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry) {
		
		try {
			Runtime.getRuntime().exec("cmd /c start C:\\ExampleHuman\\ExampleHuman.bat");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}

}
