package function.plugin.plugins.imageProcessing;

import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.FloatBlitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import jex.statics.JEXStatics;
import miscellaneous.Pair;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.converters.JEXTableReader;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 * 
 */

@Plugin(
		type = JEXPlugin.class,
		name="Unmix Image Channel",
		menuPath="Image Processing",
		visible=true,
		description="Subtract bleed-through from background channels into another to extract the true signal. Final = Signal - alpha1*bg1 - alpha2*bg2 ... - beta. Typically do BG subtraction first, then run. Can subtract bleed-through from multiple channels."
		)
public class UnmixImageChannel extends JEXPlugin {

	public UnmixImageChannel()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Multichannel Images", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;

	@InputMarker(uiOrder=2, name="Unmixing Parameters", type=MarkerConstants.TYPE_FILE, description="File containing the extraction parameters.", optional=false)
	JEXData paramData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Channel Dim Name", description="Which dimension represents the channel dimension in the dataset?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Channel")
	String channelDimName;
	
	@ParameterMarker(uiOrder=2, name="Output Offset", description="How much should be added back to the image when done.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	int offset;
	
	@ParameterMarker(uiOrder=3, name="Output Bit-Depth", description="What bitdepth should the result be saved as.", ui=MarkerConstants.UI_DROPDOWN, choices={"8", "16", "32"}, defaultChoice=1)
	int bitDepth;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Unmixed Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Final = Signal - Alpha1*bg1 - Alpha2*bg2 ... - Beta", enabled=true)
	JEXData outputData;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}

		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		Table<Double> inputParams = JEXTableReader.getNumericTable(FileReader.readFileObject(paramData));
		
		// Channel Dim
		Dim channelDim = imageData.getDimTable().getDimWithName(channelDimName);
		Dim tableChannelDim = inputParams.dimTable.getDimWithName(channelDimName);
		Pair<String,Vector<String>> channels = this.getChannels(channelDim, tableChannelDim);
		DimTable loopTable = imageData.getDimTable();
		if(imageData.getDimTable().size() == inputParams.dimTable.size())
		{
			DimTable intersection = DimTable.intersect(imageData.getDimTable(), inputParams.dimTable);
			if(intersection.size() == loopTable.size())
			{
				loopTable = intersection;
				loopTable.removeDimWithName(channelDimName);
				loopTable.add(new Dim(channelDimName, new String[] {channels.p1}));
			}
		}
		else
		{
			loopTable = loopTable.getSubTable(new DimensionMap(channelDimName + "=" + channels.p1));
		}
		
		int count = 0, percentage = 0;
		
		// Run the function
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		for (DimensionMap map : loopTable.getMapIterator())
		{
			if(this.isCanceled())
			{
				return false;
			}
			ImagePlus imA = new ImagePlus(imageMap.get(map));
			FloatProcessor ipA = (FloatProcessor) imA.getProcessor().convertToFloat();
			FloatBlitter blit = new FloatBlitter(ipA);
			
			for(String bg : channels.p2)
			{
				DimensionMap bgMap = map.copyAndSet(channelDimName + "=" + bg);
				String pathB = imageMap.get(bgMap);
				ImagePlus imB = new ImagePlus(pathB);
				FloatProcessor ipB = (FloatProcessor) imB.getProcessor().convertToFloat();
				double alpha = inputParams.getData(bgMap);
				ipB.multiply(alpha);
				blit.copyBits(ipB, 0, 0, Blitter.SUBTRACT);
			}
			ipA.add(inputParams.getData(map.copyAndSet(channelDimName + "=" + "Beta")) + this.offset);
			
			ImageProcessor toSave = ipA;
			if(bitDepth == 8)
			{
				toSave = ipA.convertToByte(false);
			}
			else if(bitDepth == 16)
			{
				toSave = ipA.convertToShort(false);
			}

			String path = JEXWriter.saveImage(toSave);

			if(path != null)
			{
				outputMap.put(map, path);
			}
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) loopTable.mapCount())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}

		if(outputMap.size() == 0)
		{
			return false;
		}

		this.outputData = ImageWriter.makeImageStackFromPaths("temp", outputMap);

		// Return status
		return true;
	}

	public Pair<String, Vector<String>> getChannels(Dim channelDim, Dim tableChannelDim)
	{
		String signal = null;
		Vector<String> bgs = new Vector<>();
		for(String channel : channelDim.dimValues)
		{
			if(tableChannelDim.dimValueSet.contains(channel))
			{
				bgs.add(channel);
			}
			else if(channel != "Beta")
			{
				signal = channel;
			}
		}
		return new Pair<String, Vector<String>>(signal, bgs);
	}
}
