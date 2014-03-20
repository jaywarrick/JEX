package cruncher;

import java.util.Vector;

import logs.Logs;
import miscellaneous.Canceler;
import signals.SSCenter;

public class Batch extends Vector<Ticket> implements Canceler {
	
	private static final long serialVersionUID = 1L;
	
	private boolean canceled;
	
	public static String SIG_BatchFinished_NULL = "SIG_BatchFinished_NULL";
	
	public int ticketsFinished = 0;
	
	public boolean add(Ticket ticket)
	{
		super.add(ticket);
		ticket.setParentBatch(this);
		return true;
	}
	
	public synchronized void finishedTicket(Ticket ticket)
	{
		Logs.log("Printing batch summary for function: " + ticket.cr.getName(), this);
		ticket.printTicketFlags();
		if(this.lastElement() == ticket)
		{
			// send signal of Batch Finished
			SSCenter.defaultCenter().emit(this, SIG_BatchFinished_NULL, (Object[]) null);
		}
	}
	
	public boolean isCanceled()
	{
		return this.canceled;
	}
	
	public void cancel()
	{
		this.canceled = true;
	}
	
	public void uncancel()
	{
		this.canceled = false;
	}
	
}
