package Database.SingleUserDatabase;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import Database.DBObjects.JEXData;
import Database.Definition.TypeName;
import ij.ImagePlus;
import logs.Logs;
import miscellaneous.CSVReader;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public class JEXReader {

	public synchronized static ArrayList<ArrayList<String>> readCSVFileToDataMap(File f)
	{
		ArrayList<ArrayList<String>> ret = new ArrayList<ArrayList<String>>();
		try
		{
			CSVReader reader = new CSVReader(new FileReader(f));
			while (!reader.isEOF())
			{
				ArrayList<String> fields = new ArrayList<String>();
				reader.readFields(fields);
				ret.add(fields);
			}
			reader.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return ret;
	}

	/**
	 * Read in and image and convert it to float and wrap in Img<FloatType>
	 * @param path
	 * @param offset
	 * @return
	 */
	public synchronized static Img<FloatType> getSingleFloatImage(String path, Double offset)
	{
		Logs.log("Opening image - " + path, JEXReader.class);
		ImagePlus im = new ImagePlus(path);
		Img<FloatType> ret = ImageJFunctions.convertFloat(im);

		// Adjust the image if necessary.
		if(offset != null && offset != 0.0)
		{
			for ( FloatType type : ret )
			{
				type.setReal(type.getRealDouble() - offset);
			} 
		}

		return ret;
	}

	public synchronized static <T extends RealType<T>> Img<T> getSingleImage(String path, Double offset)
	{
		Logs.log("Opening image - " + path, JEXReader.class);
		ImagePlus im = new ImagePlus(path);
		Img<T> ret = ImageJFunctions.wrapReal(im);

		// Adjust the image if necessary.
		if(offset != null && offset != 0.0)
		{
			for ( T type : ret )
			{
				if(type.getRealDouble() > offset)
				{
					type.setReal(type.getRealDouble() - offset);
				}
				else
				{
					type.setZero();
				}
			} 
		}
		return ret;
	}
	
	public synchronized static <T extends RealType<T>> Img<T> getSingleImage(String path, Double offset, Rectangle cropRegion)
	{
		Logs.log("Opening image - " + path, JEXReader.class);
		ImagePlus im = new ImagePlus(path);
		if(cropRegion != null)
		{
			im.setRoi(cropRegion);
			im = im.crop();
		}
		Img<T> ret = ImageJFunctions.wrapReal(im);

		// Adjust the image if necessary.
		if(offset != null && offset != 0.0)
		{
			for ( T type : ret )
			{
				if(type.getRealDouble() > offset)
				{
					type.setReal(type.getRealDouble() - offset);
				}
				else
				{
					type.setZero();
				}
			} 
		}
		return ret;
	}

	public static JEXData readFileToJEXData(String file, TypeName tn)
	{
		JEXData ret = new JEXData(tn);
		JEXDataIO.loadJXD(ret, file);
		return ret;
	}

}
