package function.plugin.plugins.imageProcessing;

import java.io.File;
import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import jex.statics.JEXStatics;
import logs.Logs;
import tables.DimensionMap;

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
		name="Colorize Hue",
		menuPath="Image Processing",
		visible=true,
		description="Enhance a color image to highlight changes in hue using the HSL colorspace, with options to normalize Saturation and Lightness to nominal values. Save the HSL version of the image and the enhanced version."
		)
public class ColorizeHue extends JEXPlugin {

	public ColorizeHue()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="Hue Min", description="Current 'min' intensity to be mapped to new min value.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double oldMin;
	
	@ParameterMarker(uiOrder=2, name="Hue Max", description="Current 'max' intensity to be mapped to new max value.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double oldMax;
	
	@ParameterMarker(uiOrder=3, name="New Hue Min", description="New intensity value for current 'min' to be mapped to new min value.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double newMin;
	
	@ParameterMarker(uiOrder=4, name="New Hue Max", description="New intensity value for current 'max' to be mapped to new min value.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double newMax;
	
	@ParameterMarker(uiOrder=5, name="Saturation (optional)", description="Optionally set the saturation of all colors to a single value (0.0-1.0). Leave blank to ignore/skip.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String saturation;
	
//	@ParameterMarker(uiOrder=6, name="Lightness (optional)", description="Optionally set the lightness of all colors to a single value (0.0-1.0). Leave blank to ignore/skip.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
//	String lightness;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="HSL Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The image hue", enabled=true)
	JEXData hslOutput;
	
	@OutputMarker(uiOrder=1, name="Colorized Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant hue adjusted image", enabled=true)
	JEXData hueAdjustedOutput;
	
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Validate the input data
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		
		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> hslImageMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> rgbImageMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		String tempPaths[];
		String[] channels = new String[] {"H", "S", "L"};
		for (DimensionMap map : imageMap.keySet())
		{
			if(this.isCanceled())
			{
				return false;
			}
			// Call helper method
			tempPaths = saveHueAdjustedImages(imageMap.get(map), oldMin, oldMax, newMin, newMax);
			int i = 0;
			
			for(String tempPath : tempPaths)
			{
				if(i < 3)
				{
					if(tempPath != null)
					{
						hslImageMap.put(map.copyAndSet("HSLChannel=" + channels[i]), tempPath);
					}
				}
				else
				{
					if(tempPath != null)
					{
						rgbImageMap.put(map, tempPath);
					}
				}
				
				i = i + 1;
			}
			
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		if(hslImageMap.size() == 0)
		{
			return false;
		}
		if(rgbImageMap.size() == 0)
		{
			return false;
		}
		
		this.hslOutput = ImageWriter.makeImageStackFromPaths("temp",hslImageMap);
		this.hueAdjustedOutput = ImageWriter.makeImageStackFromPaths("temp", rgbImageMap);
		
		// Return status
		return true;
	}
	
	public String[] saveHueAdjustedImages(String imagePath, double oldMin, double oldMax, double newMin, double newMax)
	{
		String[] ret = new String[4];
		// Get image data
		File f = new File(imagePath);
		if(!f.exists())
		{
			return null;
		}
		ImagePlus im = new ImagePlus(imagePath);
		ImageProcessor imp = im.getProcessor();
		
		if(!(imp instanceof ColorProcessor))
		{
			Logs.log("Image is not a color image. A color image is needed for this function. Aborting.", ColorizeHue.class);
			return null;
		}
		
		byte[] r = ((ColorProcessor) imp).getChannel(1);
		byte[] g = ((ColorProcessor) imp).getChannel(2);
		byte[] b = ((ColorProcessor) imp).getChannel(3);
		
		ColorProcessor retcp = new ColorProcessor(imp.getWidth(), imp.getHeight(), (int[])((ColorProcessor) imp).getPixels());
		
		FloatProcessor h = new FloatProcessor(imp.getWidth(), imp.getHeight(), new float[((int[]) imp.getPixels()).length]);
		FloatProcessor s = new FloatProcessor(imp.getWidth(), imp.getHeight(), new float[((int[]) imp.getPixels()).length]);
		FloatProcessor l = new FloatProcessor(imp.getWidth(), imp.getHeight(), new float[((int[]) imp.getPixels()).length]);
		
		FloatProcessor rp = new FloatProcessor(imp.getWidth(), imp.getHeight(), new float[((int[]) imp.getPixels()).length]);
		FloatProcessor gp = new FloatProcessor(imp.getWidth(), imp.getHeight(), new float[((int[]) imp.getPixels()).length]);
		FloatProcessor bp = new FloatProcessor(imp.getWidth(), imp.getHeight(), new float[((int[]) imp.getPixels()).length]);
		
		double ratio = (newMax - newMin) / (oldMax - oldMin);
		double offset = newMin - ratio*oldMin;
		float[] hsl = new float[3];
		float[] rgb = new float[3];
		for(int i = 0; i < r.length; i++)
		{
			float rf = (int) (r[i] & 0xff);
			float gf = (int) (g[i] & 0xff);
			float bf = (int) (b[i] & 0xff);
			
			rgb2hsl(rf, gf, bf, hsl);
			
			((float[]) h.getPixels())[i] = hsl[0];
			((float[]) s.getPixels())[i] = hsl[1];
			((float[]) l.getPixels())[i] = hsl[2];
		}
		
		ret[0] = JEXWriter.saveImage(h);
		ret[1] = JEXWriter.saveImage(s);
		ret[2] = JEXWriter.saveImage(l);
		
		// Adjust the image
		
		// Create a color image from adjusted hsl
		for(int i = 0; i < r.length; i++)
		{
			float newH = (float) (((float[]) h.getPixels())[i] * ratio + offset);
			if(newH < (float) newMin)
			{
				newH = (float) newMin;
			}
			if(newH > (float) newMax)
			{
				newH = (float) newMax;
			}
			float tempS = ((float[]) s.getPixels())[i];
			float tempL = ((float[]) l.getPixels())[i];
			if(!saturation.equals(""))
			{
				try {
					tempS = Float.parseFloat(saturation);
				} catch (NumberFormatException e) {
					Logs.log("Couldn't parse the saturation parameter to a float value.", this);
				}
			}
//			if(!lightness.equals(""))
//			{
//				try {
//					tempL = Float.parseFloat(lightness);
//				} catch (NumberFormatException e) {
//					Logs.log("Couldn't parse the lightness parameter to a float value.", this);
//				}
//			}
			hsl2rgb(newH, tempS, tempL, rgb);
			
			((float[]) rp.getPixels())[i] = (rgb[0]*255);
			((float[]) gp.getPixels())[i] = (rgb[1]*255);
			((float[]) bp.getPixels())[i] = (rgb[2]*255);
		}

//		FileUtility.showImg(rp, true);
//		FileUtility.showImg(gp, true);
//		FileUtility.showImg(bp, true);
		retcp.setChannel(1, (ByteProcessor) rp.convertToByte(false));
		retcp.setChannel(2, (ByteProcessor) gp.convertToByte(false));
		retcp.setChannel(3, (ByteProcessor) bp.convertToByte(false));
		
		ret[3] = JEXWriter.saveImage(retcp);
		
		return ret;
	}
	
	public static void rgb2hsl(float r, float g, float b, float hsl[]) {


		float var_R = ( r / 255f );                     //RGB values = From 0 to 255
		float var_G = ( g / 255f );
		float var_B = ( b / 255f );

		float var_Min;    //Min. value of RGB
		float var_Max;    //Max. value of RGB
		float del_Max;    //Delta RGB value

		if (var_R > var_G) { var_Min = var_G; var_Max = var_R;
		}
		else { var_Min = var_R; var_Max = var_G;
		}
		if (var_B > var_Max) var_Max = var_B;
		if (var_B < var_Min) var_Min = var_B;
		del_Max = var_Max - var_Min; 

		float H = 0, S, L;
		L = ( var_Max + var_Min ) / 2f;
		
		if(L > 1)
		{
			System.out.println("uy");
		}

		if ( del_Max == 0 ) { H = 0; S = 0; } // gray
		else {                                   //Chromatic data..{
			
			if ( L < 0.5 ) 
				S = del_Max / ( var_Max + var_Min );
			else           
				S = del_Max / ( 2 - var_Max - var_Min );

			float del_R = ( ( ( var_Max - var_R ) / 6f ) + ( del_Max / 2f ) ) / del_Max;
			float del_G = ( ( ( var_Max - var_G ) / 6f ) + ( del_Max / 2f ) ) / del_Max;
			float del_B = ( ( ( var_Max - var_B ) / 6f ) + ( del_Max / 2f ) ) / del_Max;

			if ( var_R == var_Max ) 
				H = del_B - del_G;
			else if ( var_G == var_Max ) 
				H = ( 1 / 3f ) + del_R - del_B;
			else if ( var_B == var_Max ) 
				H = ( 2 / 3f ) + del_G - del_R;

			if ( H < 0 ) H += 1;
			if ( H > 1 ) H -= 1;
		}
		//		float phi = (float)(Math.toRadians(360*H));
		hsl[0] = H; //(int)((S*Math.cos(phi)+1)/2);
		hsl[1] = S; //(int)((S*Math.sin(phi)+1)/2);
		hsl[2] = L; //(int)(L);
	}
	
	public static void hsl2rgb(float h, float s, float l, float[] ret) {
		//	x <- data.table(h=h, s=s, l=l, a=a)
		//	# x[, h:=h / 360]
		//	#ret <- data.table(r=rep(0,nrow(x)), g=as.double(0.0), b=as.double(0.0))
		//	x[, ':='(r=0, g=0, b=0)]
		if(s == 0.0)
		{
			// x[s==0, ':='(r=l, g=l, b=l)]
			ret[0] = l;
			ret[1] = l;
			ret[2] = l;
		}
		else
		{
			//x[s!=0, q:=ifelse(l < 0.5, l * (1.0 + s), l + s - (l*s))]
			//x[s!=0, p:= 2.0 * l - q]
			//x[s!=0, r:= hue_to_rgb(p, q, h + 1/3)]
			//x[s!=0, g:= hue_to_rgb(p, q, h)]
			//x[s!=0, b:= hue_to_rgb(p, q, h - 1/3)]
			
			float q=0f, p=0f, r=0f, g=0f, b=0f;
			if(l < 0.5f)
			{
				q = l * (1f + s);
			}
			else
			{
				q = l + s - (l * s);
			}
			p = 2f*l-q;
			r = hue2rgb(p, q, h + 1f/3f);
			g = hue2rgb(p, q, h);
			b = hue2rgb(p, q, h - 1f/3f);
			
			// print(x)
			ret[0] = r;
			ret[1] = g;
			ret[2] = b;
		}
	}
	
	public static float hue2rgb(float p, float q, float t)
	{
		//		y <- data.table(p=p, q=q, t=t, ret=p)
		//		y[, t:= (t %% 1.0)]
		//		y[t < (1/6), ret:=(p + (q - p) * 6.0 * t)]
		//		y[t >= 1/6 & t < 1/2, ret:= q]
		//		y[t >= 1/2 & t < 2/3, ret:= (p + ((q - p) * ((2/3) - t) * 6.0))]
		//		return(y$ret)
		t = t % 1f;
		float ret = p;
		if(t < (1.0/6.0))
		{
			ret = (p + (q - p) * 6f * t);
		}
		else if(t >= (1f/6f) && t < 0.5f)
		{
			ret = q;
		}
		else if(t >= 0.5f && t < (2f/3f))
		{
			ret = (p + ((q - p) * ((2f/3f) - t) * 6f));
		}
		return ret;
	}
}
