package function.imageUtility;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.File;
import java.io.IOException;

/** Implements the Image/Stacks/Make Montage command. */
public class jMontageMaker implements PlugIn {
	
	private static int columns, rows, first, last, inc, borderWidth;
	private static double scale;
	private static boolean label;
	private static boolean useForegroundColor;
	private static int saveID;
	
	public void run(String arg)
	{
		
		ImagePlus imp = WindowManager.getCurrentImage();
		if(imp == null || imp.getStackSize() == 1)
		{
			IJ.error("Stack required");
			return;
		}
		int channels = imp.getNChannels();
		if(imp.isComposite() && channels > 1)
		{
			int channel = imp.getChannel();
			CompositeImage ci = (CompositeImage) imp;
			int mode = ci.getMode();
			if(mode == CompositeImage.COMPOSITE)
				ci.setMode(CompositeImage.COLOR);
			ImageStack stack = new ImageStack(imp.getWidth(), imp.getHeight());
			for (int c = 1; c <= channels; c++)
			{
				imp.setPosition(c, imp.getSlice(), imp.getFrame());
				Image img = imp.getImage();
				stack.addSlice(null, new ColorProcessor(img));
			}
			if(ci.getMode() != mode)
				ci.setMode(mode);
			imp.setPosition(channel, imp.getSlice(), imp.getFrame());
			imp = new ImagePlus(imp.getTitle(), stack);
		}
		makeMontage(imp);
		imp.updateImage();
		saveID = imp.getID();
		
	}
	
	public static ImagePlus makeMontage(File[][] imageArray)
	{
		rows = imageArray.length;
		columns = imageArray[0].length;
		File[] result = new File[imageArray.length * imageArray[0].length];
		
		for (int i = 0, leni = imageArray.length; i < leni; i++)
		{
			for (int j = 0, lenj = imageArray[0].length; j < lenj; j++)
			{
				int pos = lenj * i + j;
				result[pos] = imageArray[i][j];
			}
		}
		
		ImagePlus imp = null;
		try
		{
			imp = new ImagePlus(result[0].getCanonicalPath());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		// System.out.println("   MontageMaker ---> showing image");
		// imp.show();
		// ImageProcessor ip = imp.getProcessor();
		// int stackWidth = ip.getWidth();
		// int stackHeight = ip.getHeight();
		
		int nSlices = result.length;
		scale = 1.0;
		if(imp.getWidth() * columns > 800)
			scale = 0.5;
		if(imp.getWidth() * columns > 1600)
			scale = 0.25;
		inc = 1;
		first = 1;
		last = nSlices;
		
		GenericDialog gd = new GenericDialog("Make Montage", IJ.getInstance());
		gd.addNumericField("Columns:", columns, 0);
		gd.addNumericField("Rows:", rows, 0);
		gd.addNumericField("Scale Factor:", scale, 2);
		gd.addNumericField("First Slice:", first, 0);
		gd.addNumericField("Last Slice:", last, 0);
		gd.addNumericField("Increment:", inc, 0);
		gd.addNumericField("Border Width:", borderWidth, 0);
		gd.addCheckbox("Label Slices", label);
		gd.addCheckbox("Use Foreground Color", useForegroundColor);
		gd.showDialog();
		if(gd.wasCanceled())
			return null;
		columns = (int) gd.getNextNumber();
		rows = (int) gd.getNextNumber();
		scale = gd.getNextNumber();
		first = (int) gd.getNextNumber();
		last = (int) gd.getNextNumber();
		inc = (int) gd.getNextNumber();
		borderWidth = (int) gd.getNextNumber();
		if(borderWidth < 0)
			borderWidth = 0;
		if(first < 1)
			first = 1;
		if(last > nSlices)
			last = nSlices;
		if(first > last)
		{
			first = 1;
			last = nSlices;
		}
		if(inc < 1)
			inc = 1;
		if(gd.invalidNumber())
		{
			IJ.error("Invalid number");
			return null;
		}
		label = gd.getNextBoolean();
		useForegroundColor = gd.getNextBoolean();
		ImagePlus im = makeMontage(result, columns, rows, scale, first, last, inc, borderWidth, label, useForegroundColor, true);
		return im;
	}
	
	public void makeMontage(ImagePlus imp)
	{
		int nSlices = imp.getStackSize();
		if(columns == 0 || imp.getID() != saveID)
		{
			columns = (int) Math.sqrt(nSlices);
			rows = columns;
			int n = nSlices - columns * rows;
			if(n > 0)
				columns += (int) Math.ceil((double) n / rows);
			scale = 1.0;
			if(imp.getWidth() * columns > 800)
				scale = 0.5;
			if(imp.getWidth() * columns > 1600)
				scale = 0.25;
			inc = 1;
			first = 1;
			last = nSlices;
		}
		
		GenericDialog gd = new GenericDialog("Make Montage", IJ.getInstance());
		gd.addNumericField("Columns:", columns, 0);
		gd.addNumericField("Rows:", rows, 0);
		gd.addNumericField("Scale Factor:", scale, 2);
		gd.addNumericField("First Slice:", first, 0);
		gd.addNumericField("Last Slice:", last, 0);
		gd.addNumericField("Increment:", inc, 0);
		gd.addNumericField("Border Width:", borderWidth, 0);
		gd.addCheckbox("Label Slices", label);
		gd.addCheckbox("Use Foreground Color", useForegroundColor);
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		columns = (int) gd.getNextNumber();
		rows = (int) gd.getNextNumber();
		scale = gd.getNextNumber();
		first = (int) gd.getNextNumber();
		last = (int) gd.getNextNumber();
		inc = (int) gd.getNextNumber();
		borderWidth = (int) gd.getNextNumber();
		if(borderWidth < 0)
			borderWidth = 0;
		if(first < 1)
			first = 1;
		if(last > nSlices)
			last = nSlices;
		if(first > last)
		{
			first = 1;
			last = nSlices;
		}
		if(inc < 1)
			inc = 1;
		if(gd.invalidNumber())
		{
			IJ.error("Invalid number");
			return;
		}
		label = gd.getNextBoolean();
		useForegroundColor = gd.getNextBoolean();
		makeMontage(imp, columns, rows, scale, first, last, inc, borderWidth, label, useForegroundColor, null, null, 12, true, false);
	}
	
	public static ImagePlus makeMontage(File[] imageList, int columns, int rows, double scale, int first, int last, int inc, int borderWidth, boolean labels, boolean useForegroundColor, boolean quiet)
	{
		ImagePlus imp = null;
		try
		{
			imp = new ImagePlus(imageList[0].getCanonicalPath());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		ImageProcessor ip = imp.getProcessor();
		
		System.out.println("making a process of type " + ip.getClass());
		
		int stackWidth = ip.getWidth();
		int stackHeight = ip.getHeight();
		int nSlices = imageList.length;
		if(scale > 1)
		{
			if(columns * stackWidth > rows * stackHeight)
			{
				scale = scale / (columns * stackWidth);
			}
			else
			{
				scale = scale / (rows * stackHeight);
			}
		}
		if(nSlices == 0)
			return null;
		
		int width = (int) (stackWidth * scale);
		int height = (int) (stackHeight * scale);
		int montageWidth = width * columns;
		int montageHeight = height * rows;
		
		// ImageProcessor montage =
		// ip.createProcessor(montageWidth+borderWidth/2,
		// montageHeight+borderWidth/2);
		ImageProcessor montage = new ColorProcessor(montageWidth + borderWidth / 2, montageHeight + borderWidth / 2);
		Color fgColor = Color.white;
		Color bgColor = Color.black;
		if(useForegroundColor)
		{
			fgColor = Toolbar.getForegroundColor();
			bgColor = Toolbar.getBackgroundColor();
		}
		else
		{
			boolean whiteBackground = false;
			if((ip instanceof ByteProcessor) || (ip instanceof ColorProcessor))
			{
				ImageStatistics is = imp.getStatistics();
				whiteBackground = is.mode >= 200;
				if(imp.isInvertedLut())
					whiteBackground = !whiteBackground;
			}
			if(whiteBackground)
			{
				fgColor = Color.black;
				bgColor = Color.white;
			}
		}
		montage.setColor(bgColor);
		montage.fill();
		montage.setColor(fgColor);
		// ImageStack stack = imp.getStack();
		int x = 0;
		int y = 0;
		ImageProcessor aSlice;
		int slice = first;
		int index = 0;
		while (slice <= last)
		{
			ImagePlus nextImp = null;
			try
			{
				nextImp = new ImagePlus(imageList[index].getCanonicalPath());
				System.out.println("   jMontageMaker ---> Montaging figure number " + index + " at path " + imageList[index].getCanonicalPath());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			aSlice = nextImp.getProcessor();
			index++;
			
			// aSlice = stack.getProcessor(slice);
			if(scale != 1.0)
				aSlice = aSlice.resize(width, height);
			montage.insert(aSlice, x, y);
			// String label = stack.getShortSliceLabel(slice);
			String label = "Image " + index;
			if(borderWidth > 0)
				drawBorder(montage, x, y, width, height, borderWidth);
			if(labels)
				drawLabel(montage, slice, label, x, y, width, height);
			x += width;
			if(x >= montageWidth)
			{
				x = 0;
				y += height;
				if(y >= montageHeight)
					break;
			}
			IJ.showProgress((double) (slice - first) / (last - first));
			slice += inc;
		}
		if(borderWidth > 0)
		{
			int w2 = borderWidth / 2;
			drawBorder(montage, w2, w2, montageWidth - w2, montageHeight - w2, borderWidth);
		}
		IJ.showProgress(1.0);
		ImagePlus imp2 = new ImagePlus("Montage", montage);
		imp2.setCalibration(imp.getCalibration());
		Calibration cal = imp2.getCalibration();
		if(cal.scaled())
		{
			cal.pixelWidth /= scale;
			cal.pixelHeight /= scale;
		}
		imp2.setProperty("Info", "xMontage=" + columns + "\nyMontage=" + rows + "\n");
		
		if(!quiet)
		{
			imp2.show();
		}
		return imp2;
	}
	
	public static ImagePlus makeMontage(ImagePlus imp, int columns, int rows, double scale, int first, int last, int inc, int borderWidth, boolean labels, boolean useForegroundColor, Color foregroundColor, Color backgroundColor, int fontSize, boolean quiet, boolean forceColorProcessor)
	{
		int stackWidth = imp.getWidth();
		int stackHeight = imp.getHeight();
		// int nSlices = imp.getStackSize();
		//		if(scale > 1)
		//		{
		//			if(columns * stackWidth > rows * stackHeight)
		//			{
		//				scale = scale / (columns * stackWidth);
		//			}
		//			else
		//			{
		//				scale = scale / (rows * stackHeight);
		//			}
		//		}
		int width = (int) (stackWidth * scale);
		int height = (int) (stackHeight * scale);
		int colWidth = width + borderWidth;
		int rowHeight = height + borderWidth;
		int montageWidth = colWidth * columns;
		int montageHeight = rowHeight * rows;
		ImageProcessor ip = imp.getProcessor();
		ImageProcessor montage = null;
		if(forceColorProcessor)
		{
			montage = new ColorProcessor(montageWidth, montageHeight);
		}
		else
		{
			montage = ip.createProcessor(montageWidth, montageHeight);
		}
		
		if(fontSize < 1)
		{
			fontSize = 1;
		}
		montage.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
		Color fgColor = Color.white;
		Color bgColor = Color.black;
		if(useForegroundColor)
		{
			fgColor = Toolbar.getForegroundColor();
			bgColor = Toolbar.getBackgroundColor();
		}
		else if(foregroundColor == null || backgroundColor == null)
		{
			boolean whiteBackground = false;
			if((ip instanceof ByteProcessor) || (ip instanceof ColorProcessor))
			{
				ImageStatistics is = imp.getStatistics();
				whiteBackground = is.mode >= 200;
				if(imp.isInvertedLut())
					whiteBackground = !whiteBackground;
			}
			if(whiteBackground)
			{
				fgColor = Color.black;
				bgColor = Color.white;
			}
		}
		else
		{
			fgColor = foregroundColor;
			bgColor = backgroundColor;
		}
		montage.setColor(bgColor);
		montage.fill();
		montage.setColor(fgColor);
		ImageStack stack = imp.getStack();
		int x = 0;
		int y = 0;
		ImageProcessor aSlice;
		int slice = first;
		while (slice <= last)
		{
			aSlice = stack.getProcessor(slice);
			if(scale != 1.0)
				aSlice = aSlice.resize(width, height);
			montage.insert(aSlice, x + borderWidth/2, y + borderWidth/2);
			String label = stack.getShortSliceLabel(slice);
//			if(borderWidth > 0)
//			{
//				montage.setColor(Color.black);
//				drawBorder(montage, x, y, colWidth, rowHeight, borderWidth);
//			}
			if(labels)
			{
				montage.setColor(Color.white);
				drawLabel(montage, slice, label, x, y, colWidth, rowHeight);
			}
			x += colWidth;
			if(x >= montageWidth)
			{
				x = 0;
				y += rowHeight;
				if(y >= montageHeight)
					break;
			}
			IJ.showProgress((double) (slice - first) / (last - first));
			slice += inc;
		}
		//		if(borderWidth > 0)
		//		{
		//			int w2 = borderWidth / 2;
		//			drawBorder(montage, w2, w2, montageWidth - w2, montageHeight - w2, borderWidth);
		//		}
		if(labels)
		{
			montage.setColor(Color.white);
			String label = stack.getShortSliceLabel(slice);
			drawLabel(montage, last+1, label, 0, montageHeight + borderWidth/2, width, height);
		}
		IJ.showProgress(1.0);
		ImagePlus imp2 = new ImagePlus("Montage", montage);
		imp2.setCalibration(imp.getCalibration());
		Calibration cal = imp2.getCalibration();
		if(cal.scaled())
		{
			cal.pixelWidth /= scale;
			cal.pixelHeight /= scale;
		}
		imp2.setProperty("Info", "xMontage=" + columns + "\nyMontage=" + rows + "\n");
		
		if(!quiet)
		{
			imp2.show();
		}
		return imp2;
	}
	
	static void drawBorder(ImageProcessor montage, int x, int y, int width, int height, int borderWidth)
	{
		montage.setLineWidth(borderWidth);
		montage.moveTo(x, y);
		montage.lineTo(x + width, y);
		montage.lineTo(x + width, y + height);
		montage.moveTo(x, y + height);
		montage.lineTo(x, y);
	}
	
	static void drawLabel(ImageProcessor montage, int slice, String label, int x, int y, int width, int height)
	{
		if(label != null && !label.equals("") && montage.getStringWidth(label) >= width)
		{
			do
			{
				label = label.substring(0, label.length() - 1);
			}
			while (label.length() > 1 && montage.getStringWidth(label) >= width);
		}
		if(label == null || label.equals(""))
			label = "" + slice;
		int swidth = montage.getStringWidth(label);
		x += width / 2 - swidth / 2;
		y += height;
		montage.moveTo(x, y);
		montage.drawString(label);
	}
}
