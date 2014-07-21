package jex.infoPanels;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;

public class InfoPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	// GUI
	protected JPanel centerPane;
	protected JLabel titleLabel;
	
	// Variables
	protected String title = "";
	
	// Color
	public static Color centerPaneBGColor = new Color(75, 75, 75);
	
	public InfoPanel()
	{
		initialize();
	}
	
	private void initialize()
	{
		this.setLayout(new MigLayout("flowy,ins 7", "[fill,grow]", ""));
		this.setBackground(centerPaneBGColor);
		
		// Make a dummy central panel
		centerPane = new JPanel();
		centerPane.setLayout(new BoxLayout(centerPane, BoxLayout.PAGE_AXIS));
		centerPane.setBackground(centerPaneBGColor);
		
		// Make a dummy title label
		titleLabel = new JLabel();
		titleLabel.setText(title);
		titleLabel.setForeground(Color.white);
		titleLabel.setFont(FontUtility.boldFont);
		
		// Fill the panel
		this.add(titleLabel, "height 15!,growx");
		this.add(centerPane, "growx");
	}
	
	/**
	 * Update the title of the info panel
	 * 
	 * @param title
	 */
	public void setTitle(String title)
	{
		this.title = title;
		titleLabel.setText(title);
		titleLabel.repaint();
	}
	
	/**
	 * Returns the title of the infopanel
	 * 
	 * @return
	 */
	public String getTitle()
	{
		return this.title;
	}
	
	/**
	 * Set the inner panel
	 * 
	 * @param pane
	 */
	public void setCenterPanel(JPanel pane)
	{
		// set the central panel to the panel PANE
		centerPane = pane;
		
		// Reset the gui
		this.removeAll();
		this.add(titleLabel, "height 15!,growx");
		this.add(centerPane, "growx");
		
		// refresh
		this.revalidate();
		this.repaint();
	}
	
	// ----------------------------------------------------
	// --------- DRAWING AND GRAPHICS ---------------------
	// ----------------------------------------------------
	
	/**
	 * Paint this componement with cool colors
	 */
	@Override
	protected void paintComponent(Graphics g)
	{
		
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2.setColor(DisplayStatics.background);
		g2.fillRect(0, 0, this.getWidth(), this.getHeight());
		
		int x = 0;
		int y = 0;
		int w = getWidth() - 0;
		int h = getHeight();
		int arc = 10;
		
		g2.setColor(centerPaneBGColor);
		g2.fillRoundRect(x, y, w, h, arc, arc);
		
		g2.dispose();
	}
	
	@Override
	public void paint(Graphics g)
	{
		super.paint(g);
	}
	
}
