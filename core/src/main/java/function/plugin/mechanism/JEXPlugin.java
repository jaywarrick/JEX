package function.plugin.mechanism;

import org.scijava.plugin.SciJavaPlugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.Type;
import cruncher.Ticket;
import function.JEXCrunchable;
import function.plugin.IJ2.IJ2CrunchablePlugin;
import ij.process.ImageProcessor;
import jex.statics.JEXStatics;
import miscellaneous.Cancelable;
import miscellaneous.Canceler;

public abstract class JEXPlugin implements SciJavaPlugin, Canceler, Cancelable {
	
	public static final String UPDATABLE="Updatable", NONUPDATABLE="Non-updatable";

	public Canceler canceler = null;

	public abstract boolean run(JEXEntry optionalEntry);

	// run after ending
	public void finalizeEntry()
	{
		JEXStatics.statusBar.setProgressPercentage(0);
	}

	public void prepareTicket()
	{   

	}

	public void finalizeTicket(Ticket ticket)
	{   

	}

	public int getMaxThreads()
	{
		return 5;
	}

	public void setCanceler(Canceler canceler)
	{
		this.canceler = canceler;
	}

	public Canceler getCanceler()
	{
		return this.canceler;
	}

	public boolean isCanceled()
	{
		return this.canceler.isCanceled();
	}

	public static boolean isInputValid(JEXData data, Type type)
	{
		return(JEXCrunchable.isInputValid(data, type));
	}
	
	public static String saveAdjustedImage(String imagePath, double oldMin, double oldMax, double newMin, double newMax, double gamma, int bitDepth)
	{
		return IJ2CrunchablePlugin.saveAdjustedImage(imagePath, oldMin, oldMax, newMin, newMax, gamma, bitDepth);
	}
	
	public static String saveAdjustedImage(ImageProcessor imp, double oldMin, double oldMax, double newMin, double newMax, double gamma, int bitDepth)
	{
		return IJ2CrunchablePlugin.saveAdjustedImage(imp, oldMin, oldMax, newMin, newMax, gamma, bitDepth);
	}
}
