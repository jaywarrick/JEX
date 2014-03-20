package cruncher;

import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import statics.DisplayStatics;

public class BatchPanelList {
	
	public JPanel outerPanel;
	public JPanel innerPanel;
	public JScrollPane scroll;
	public Vector<BatchPanel> batchList = new Vector<BatchPanel>();
	
	public BatchPanelList()
	{
		this.initialize();
	}
	
	public void initialize()
	{
		// Build the selector header
		JLabel title1 = new JLabel("BATCH QUEUE");
		JPanel headerPane1 = new JPanel(new MigLayout("flowy,center,ins 1", "[center]", "[center]"));
		headerPane1.setBackground(DisplayStatics.menuBackground);
		title1.setFont(FontUtility.boldFont);
		headerPane1.add(title1);
		this.innerPanel = new JPanel();
		this.innerPanel.setLayout(new MigLayout("flowy, ins 3", "[fill,grow]", "[]2[]"));
		this.innerPanel.setBackground(DisplayStatics.lightBackground);
		this.scroll = new JScrollPane(this.innerPanel);
		this.scroll.setBackground(DisplayStatics.lightBackground);
		this.scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		this.scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		this.scroll.setBorder(BorderFactory.createEmptyBorder());
		this.outerPanel = new JPanel();
		this.outerPanel.setLayout(new MigLayout("flowy, ins 2", "[fill,grow]", "[]1[fill,grow]"));
		this.outerPanel.setBackground(DisplayStatics.lightBackground);
		this.outerPanel.add(headerPane1, "growx");
		this.outerPanel.add(this.scroll, "grow");
	}
	
	public void add(Batch batch)
	{
		BatchPanel bpanel = new BatchPanel(batch, this);
		this.innerPanel.add(bpanel.panel(), "growx");
		this.innerPanel.revalidate();
		this.innerPanel.repaint();
	}
	
	public void remove(BatchPanel batchPanel)
	{
		this.innerPanel.remove(batchPanel.panel());
		this.innerPanel.revalidate();
		this.innerPanel.repaint();
	}
	
	public JPanel panel()
	{
		return this.outerPanel;
	}
}
