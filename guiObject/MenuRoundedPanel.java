package guiObject;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jex.statics.DisplayStatics;
import miscellaneous.FontUtility;

public class MenuRoundedPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	// Variables
	// private String title = "";
	protected Color backColor = DisplayStatics.lightBackground;
	protected Color borderColor = DisplayStatics.dividerColor;
	
	// Hidden variables
	private JPanel contentPane = new JPanel();
	private JLabel titleLabel = new JLabel("");
	private JPanel titlePane = new JPanel();
	private JPanel mainPane = new JPanel();
	private JPanel subPane = new JPanel();
	// private Component centerPane ;
	protected int arcRadius = 15;
	
	public MenuRoundedPanel()
	{
		initialize();
	}
	
	/**
	 * Initialize the function
	 */
	protected void initialize()
	{
		this.setLayout(new BorderLayout());
		this.setBackground(backColor);
		
		contentPane.setLayout(new BorderLayout());
		contentPane.setBackground(backColor);
		
		titlePane.setBackground(backColor);
		titlePane.setLayout(new BoxLayout(titlePane, BoxLayout.X_AXIS));
		titlePane.add(Box.createHorizontalGlue());
		titlePane.add(titleLabel);
		titlePane.add(Box.createHorizontalGlue());
		
		mainPane.setLayout(new BorderLayout());
		mainPane.setBackground(backColor);
		mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));
		
		subPane.setLayout(new BoxLayout(subPane, BoxLayout.LINE_AXIS));
		subPane.setBackground(backColor);
		
		subPane.add(Box.createRigidArea(new Dimension(20, 20)));
		subPane.add(Box.createHorizontalGlue());
		
		contentPane.add(titlePane, BorderLayout.PAGE_START);
		contentPane.add(mainPane, BorderLayout.CENTER);
		contentPane.add(subPane, BorderLayout.PAGE_END);
		
		this.add(Box.createRigidArea(new Dimension(3, 3)), BorderLayout.PAGE_START);
		this.add(Box.createRigidArea(new Dimension(3, 3)), BorderLayout.PAGE_END);
		this.add(Box.createRigidArea(new Dimension(10, 3)), BorderLayout.LINE_END);
		this.add(Box.createRigidArea(new Dimension(10, 3)), BorderLayout.LINE_START);
		this.add(contentPane, BorderLayout.CENTER);
	}
	
	/**
	 * Set the title
	 * 
	 * @param title
	 */
	public void setPanelTitle(String title)
	{
		// this.title = title;
		this.titleLabel.setText(title);
		this.titleLabel.setFont(FontUtility.boldFont);
		this.repaint();
	}
	
	/**
	 * Set the content
	 * 
	 * @param pane
	 */
	public void setCenterPane(JPanel pane)
	{
		// centerPane = pane;
		mainPane.removeAll();
		mainPane.add(pane, BorderLayout.CENTER);
		this.revalidate();
	}
	
	/**
	 * Set the content
	 * 
	 * @param pane
	 */
	public void setCenterPane(JScrollPane pane)
	{
		// centerPane = pane;
		mainPane.removeAll();
		mainPane.add(pane, BorderLayout.CENTER);
		this.revalidate();
	}
	
	/**
	 * Paint this componement with cool colors
	 */
	@Override
	protected void paintComponent(Graphics g)
	{
		int x = 1;
		int y = 1;
		int w = getWidth() - 3;
		int h = getHeight() - 3;
		
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2.setColor(backColor);
		g2.fillRect(x, y, w, h);
		
		g2.setColor(backColor);
		g2.fillRoundRect(x, y, w, h, arcRadius, arcRadius);
		
		g2.setStroke(new BasicStroke(1f));
		g2.setColor(borderColor);
		g2.drawRoundRect(x, y, w, h, arcRadius, arcRadius);
		
		g2.dispose();
	}
	
}
