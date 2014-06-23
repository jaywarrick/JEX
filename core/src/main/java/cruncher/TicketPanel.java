package cruncher;

import javax.swing.JLabel;
import javax.swing.JPanel;

import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;
import jex.statics.DisplayStatics;

public class TicketPanel {
	
	public JPanel panel;
	public Ticket ticket;
	private JLabel funcName = new JLabel(" ");
	private JLabel funcStart = new JLabel(" ");
	private JLabel funcEnd = new JLabel(" ");
	
	public TicketPanel(Ticket ticket)
	{
		this.ticket = ticket;
		initialize();
	}
	
	private void initialize()
	{
		// this.funcEnd.setDoubleBuffered(true);
		this.panel = new JPanel();
		this.panel.setLayout(new MigLayout("flowy, ins 0", "[grow, 100]3", "[]"));
		this.panel.setBackground(DisplayStatics.lightBackground);
		this.funcName.setText(this.ticket.cr.getName());
		this.funcName.setFont(FontUtility.boldFont);
		this.panel.add(funcName, "left");
		this.panel.add(funcStart, "right");
		this.panel.add(funcEnd, "right");
		this.panel.revalidate();
		this.panel.repaint();
		SSCenter.defaultCenter().connect(this.ticket, Ticket.SIG_TicketStarted_NULL, this, "ticketStarted", (Class[]) null);
		SSCenter.defaultCenter().connect(this.ticket, Ticket.SIG_TicketUpdated_NULL, this, "ticketUpdated", (Class[]) null);
		SSCenter.defaultCenter().connect(this.ticket, Ticket.SIG_TicketFinished_NULL, this, "ticketFinished", (Class[]) null);
	}
	
	public synchronized void ticketStarted()
	{
		funcStart.setText("Start Time: " + ticket.startTime);
		funcStart.repaint();
	}
	
	public synchronized void ticketFinished()
	{
		funcEnd.setText("End Time: " + ticket.endTime);
		funcEnd.repaint();
	}
	
	public synchronized void ticketUpdated()
	{
		funcEnd.setText("Completed: " + ticket.functionsFinished + " of " + ticket.size() + ", Threads: " + (ticket.functionsStarted - ticket.functionsFinished));
		funcEnd.repaint();
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
}
