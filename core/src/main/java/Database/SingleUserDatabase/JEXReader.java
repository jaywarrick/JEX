package Database.SingleUserDatabase;

import function.plugin.IJ2.IJ2PluginUtility;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import io.scif.img.SCIFIOImgPlus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import Database.DBObjects.JEXData;
import Database.Definition.TypeName;
import jex.statics.JEXDialog;
import miscellaneous.CSVReader;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class JEXReader {
	
	public static ArrayList<ArrayList<String>> readCSVFileToDataMap(File f)
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
	
	public static  SCIFIOImgPlus<UnsignedByteType> getByteImage(String path)
	{
		ImgOpener imgOpener = new ImgOpener(IJ2PluginUtility.ij().getContext());

		// Always get the first image because that is how JEX stores images.
		SCIFIOImgPlus<UnsignedByteType> img;
		try
		{
			img = (SCIFIOImgPlus<UnsignedByteType>) imgOpener.openImgs(path, new UnsignedByteType()).get(0);
			return img;
		} catch(ImgIOException e)
		{
			JEXDialog.messageDialog("Couldn't open file as 8-bit image. Check input images. File = " + path );
			e.printStackTrace();
			return null;
		}		
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends RealType<T>> Img<T> getSingleImage(String path)
	{
		if(path == null)
		{
			return null;
		}
		
		ImgOpener imgOpener = new ImgOpener(IJ2PluginUtility.ij().getContext());

		// Always get the first image because that is how JEX stores images.
		Img<T> img;
		try
		{
			img = (Img<T>) imgOpener.openImgs(path).get(0);
			return img;
		} catch(ImgIOException e)
		{
			JEXDialog.messageDialog("Couldn't open file as image. Check input images. File = " + path );
			e.printStackTrace();
			return null;
		}		
	}
	
	public static JEXData readFileToJEXData(String file, TypeName tn)
	{
		JEXData ret = new JEXData(tn);
		JEXDataIO.loadJXD(ret, file);
		return ret;
	}
	
}
