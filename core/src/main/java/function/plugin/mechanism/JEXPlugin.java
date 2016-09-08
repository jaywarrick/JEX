package function.plugin.mechanism;

import org.scijava.plugin.SciJavaPlugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.Type;
import cruncher.Ticket;
import jex.statics.JEXStatics;
import miscellaneous.Cancelable;
import miscellaneous.Canceler;

public abstract class JEXPlugin implements SciJavaPlugin, Canceler, Cancelable {
	
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
		if(data == null || !data.getTypeName().getType().equals(type))
		{
			//			if(data == null)
			//			{
			//				JEXDialog.messageDialog("A required object of type '" + type.toString() + " 'is missing! Aborting.");
			//			}
			//			else
			//			{
			//				JEXDialog.messageDialog(data.getTypeName().getName() + " was expected to be a " + type.toString() + " but is a " + data.getTypeName().getType().toString() + ". Aborting.");
			//			}
			return false;
		}
		return true;
	}
}
