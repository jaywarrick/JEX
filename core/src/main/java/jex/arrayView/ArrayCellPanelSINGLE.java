package jex.arrayView;

import Database.DBObjects.JEXEntry;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import net.miginfocom.swing.MigLayout;

public class ArrayCellPanelSINGLE extends ArrayCellPanel implements MouseListener {
	
	private static final long serialVersionUID = 1L;
	
	private JLabel title = new JLabel("");
	private JPanel titlePane = new JPanel();
	
	private JLabel validLabel = new JLabel();
	private JLabel invalidLabel = new JLabel();
	
	private JLabel eyeLabel = new JLabel();
	private JLabel noeyeLabel = new JLabel();
	
	ArrayCellPanelSINGLE(ArrayCellController controller)
	{
		super(controller);
		
		this.addMouseListener(this);
		
		title.setMaximumSize(new Dimension(80, 20));
		title.setAlignmentX(LEFT_ALIGNMENT);
		title.setText(controller.cell().getName());
		titlePane.setLayout(new BoxLayout(titlePane, BoxLayout.LINE_AXIS));
		titlePane.add(title);
		
		validLabel.setText(null);
		validLabel.setMaximumSize(new Dimension(25, 25));
		validLabel.setAlignmentX(CENTER_ALIGNMENT);
		validLabel.setHorizontalAlignment(SwingConstants.CENTER);
		validLabel.setIcon(JEXStatics.iconRepository.validImage);
		validLabel.addMouseListener(this);
		
		invalidLabel.setText(null);
		invalidLabel.setMaximumSize(new Dimension(25, 25));
		invalidLabel.setAlignmentX(CENTER_ALIGNMENT);
		invalidLabel.setHorizontalAlignment(SwingConstants.CENTER);
		invalidLabel.setIcon(JEXStatics.iconRepository.invalidImage);
		invalidLabel.addMouseListener(this);
		
		eyeLabel.setText(null);
		eyeLabel.setMaximumSize(new Dimension(25, 20));
		eyeLabel.setAlignmentX(CENTER_ALIGNMENT);
		eyeLabel.setHorizontalAlignment(SwingConstants.CENTER);
		eyeLabel.setIcon(JEXStatics.iconRepository.eyeImage);
		eyeLabel.addMouseListener(this);
		
		noeyeLabel.setText(null);
		noeyeLabel.setMaximumSize(new Dimension(25, 20));
		noeyeLabel.setAlignmentX(CENTER_ALIGNMENT);
		noeyeLabel.setHorizontalAlignment(SwingConstants.CENTER);
		noeyeLabel.setIcon(JEXStatics.iconRepository.noeyeImage);
		noeyeLabel.addMouseListener(this);
	}
	
	public void rebuild()
	{
		// Set the background
		this.setBackground(DisplayStatics.lightBackground);
		
		// rebuild the gui
		this.removeAll();
		this.setLayout(new MigLayout("ins 0", "2[20]2[fill,grow]2[20]2", "2[20]2[fill,grow]2"));
		
		// Viewed status
		if(controller.viewed)
			this.add(eyeLabel, "width 20,height 20");
		else
			this.add(noeyeLabel, "width 20,height 20");
		
		// The title and the label color
		if(controller.labelColor == null && temporaryGround == null)
			titlePane.setBackground(background);
		else if(controller.labelColor != null || temporaryGround == null)
			titlePane.setBackground(controller.labelColor);
		else
			titlePane.setBackground(temporaryGround);
		this.add(titlePane, "growx");
		
		// Valid status
		if(controller.valid)
			this.add(validLabel, "width 20,height 20,wrap");
		else
			this.add(invalidLabel, "width 20,height 20,wrap");
		
		// The data view
		boolean flag = JEXStatics.jexManager.viewDataInArray();
		if(!flag || controller.dataView == null)
		{
			JPanel nullPane = new JPanel();
			nullPane.setBackground(DisplayStatics.lightBackground);
			this.add(nullPane, "span 3, width 100%, height 100%");
		}
		else
		{
			JPanel panel = controller.dataView.panel();
			this.add(panel, "span 3, width 100%, height 100%");
		}
		
		// The selection status
		this.setBorder(BorderFactory.createLineBorder(controller.borderColor, 2));
		
		this.invalidate();
		this.validate();
		this.repaint();
	}
	
	public void mouseClicked(MouseEvent e)
	{}
	
	public void mouseEntered(MouseEvent e)
	{
		temporaryGround = DisplayStatics.selectedLightBBackground;
		titlePane.setBackground(temporaryGround);
		this.repaint();
	}
	
	public void mouseExited(MouseEvent e)
	{
		temporaryGround = null;
		titlePane.setBackground(controller.labelColor);
		this.repaint();
	}
	
	public void mousePressed(MouseEvent e)
	{}
	
	public void mouseReleased(MouseEvent e)
	{
		if(e.getSource() == validLabel)
		{
			Set<JEXEntry> entries = controller.entries();
			JEXEntry entry = null;
			if(entries != null && entries.size() > 0)
				entry = entries.iterator().next();
			JEXStatics.jexManager.setEntryValid(entry, false);
			return;
		}
		if(e.getSource() == invalidLabel)
		{
			Set<JEXEntry> entries = controller.entries();
			JEXEntry entry = null;
			if(entries != null && entries.size() > 0)
				entry = entries.iterator().next();
			JEXStatics.jexManager.setEntryValid(entry, true);
			return;
		}
		if(e.getSource() == eyeLabel)
		{
			JEXStatics.jexManager.setViewedEntry(null);
			return;
		}
		if(e.getSource() == noeyeLabel)
		{
			Set<JEXEntry> entries = controller.entries();
			JEXEntry entry = null;
			if(entries != null && entries.size() > 0)
				entry = entries.iterator().next();
			JEXStatics.jexManager.setViewedEntry(entry);
			return;
		}
		
		boolean selected = JEXStatics.jexManager.isAllSelected(controller.entries());
		if(selected)
		{
			Logs.log("Removing entry from selection", 1, this);
			JEXStatics.jexManager.removeEntriesFromSelection(controller.entries());
		}
		else
		{
			Logs.log("Adding entry to selection", 1, this);
			JEXStatics.jexManager.addEntriesToSelection(controller.entries());
		}
	}
	
}
