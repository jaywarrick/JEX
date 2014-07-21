package cruncher;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class BatchPanel implements ActionListener {
	
	public JPanel panel;
	public Batch batch;
	public JButton cancel;
	public BatchPanelList parent;
	public Vector<TicketPanel> ticketPanels = new Vector<TicketPanel>();
	
	public BatchPanel(Batch batch, BatchPanelList parent)
	{
		this.parent = parent;
		this.batch = batch;
		initialize();
	}
	
	public void initialize()
	{
		this.panel = new JPanel();
		this.panel.setLayout(new MigLayout("flowy, ins 3", "[fill, grow]3", "[]2"));
		this.panel.setBackground(DisplayStatics.lightBackground);
		this.panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		for (Ticket ticket : batch)
		{
			TicketPanel tpanel = new TicketPanel(ticket);
			this.ticketPanels.add(tpanel);
			this.panel.add(tpanel.panel(), "growx");
		}
		this.cancel = new JButton("Cancel (after current entry)");
		this.panel.add(this.cancel, "growx");
		this.cancel.addActionListener(this);
		SSCenter.defaultCenter().connect(this.batch, Batch.SIG_BatchFinished_NULL, this, "finish", (Class[]) null);
		this.panel.revalidate();
		this.panel.repaint();
	}
	
	public void finish()
	{
		this.parent.remove(this);
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == this.cancel)
		{
			if(this.batch.isCanceled())
			{
				this.batch.uncancel();
				this.cancel.setText("Cancel (after current entry)");
			}
			else
			{
				this.batch.cancel();
				this.cancel.setText("Canceled...");
			}
		}
	}
	
}
