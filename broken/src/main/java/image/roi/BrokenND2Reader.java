package image.roi;

import io.scif.FormatException;
import io.scif.Reader;
import io.scif.SCIFIO;
import io.scif.config.SCIFIOConfig;

import java.io.IOException;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;


public class BrokenND2Reader {
	
	public static ImageJ ij = new ImageJ();
	
	public static void main(String[] args) throws FormatException, IOException
	{
		String file1 = "/Users/jaywarrick/Google Drive/example.nd2";
		//		String file2 = "/Users/jaywarrick/Google Drive/example001.nd2";
		//		trySCIFIO(file1);
		trySCIFIO(file1);
	}
	
	public static void trySCIFIO(String path) throws FormatException, IOException
	{
		final SCIFIO scifio = new SCIFIO(ij.getContext());
		final Reader reader = scifio.initializer().initializeReader(path, new SCIFIOConfig().checkerSetOpen(true));
		System.out.println("IMAGE METADATA\n");
		System.out.println(reader.getMetadata().toString());
		System.out.println("\n\n\n\n\n\n\nIMAGE TABLE\n\n");
		System.out.println(reader.getMetadata().getTable().toString());
		System.out.println("\n\n\n\n\n\n\nPLANE TABLE\n\n");
		System.out.println(reader.getMetadata().get(0).getTable().toString());
	}
	
	public static void tryImageJ(String path) throws IOException
	{
		Dataset ds = ij.dataset().open(path, new SCIFIOConfig().checkerSetOpen(true));
		System.out.println("IMAGE METADATA\n");
		System.out.println(ds.getProperties().toString());
		try
		{
			ImgPlus<? extends RealType<?>> im = ds.getImgPlus();
			ij.ui().show(im);
			Object p = ds.getPlane(0);
			System.out.println(p.toString());
		}
		catch(Exception e)
		{
			System.out.println("What???");
			e.printStackTrace();
		}
	}
}
