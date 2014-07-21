package guiObject;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import jex.statics.DisplayStatics;
import logs.Logs;
import miscellaneous.FontUtility;

import org.jdesktop.swingx.graphics.GraphicsUtilities;
import org.jdesktop.swingx.image.GaussianBlurFilter;

public class JRoundedAndScrollPanel extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	// Variables
	private String title = "";
	private boolean addRemoveButtons = true;
	private Color backgroundColor = DisplayStatics.background;
	private Color menuBarColor = DisplayStatics.menuBackground;
	private Color foregroundColor = DisplayStatics.lightBackground;
	private List<JPanel> contentList;
	
	// Hidden variables
	private JPanel thisContentPane = new JPanel();
	private JLabel titleLabel = new JLabel("");
	private JPanel titlePane = new JPanel();
	private JPanel mainPane = new JPanel();
	private JPanel subPane = new JPanel();
	private JPanel centerPane = new JPanel();
	private JScrollPane centralScrollPane = new JScrollPane(centerPane);
	private JButton minusButton = new JButton("-");
	private JButton plusButton = new JButton("+");
	// private int arcRadius = 15;
	private int sideSpace = 5;
	private boolean left = false;
	private boolean right = false;
	private boolean top = false;
	private boolean bottom = false;
	
	public JRoundedAndScrollPanel()
	{
		contentList = new ArrayList<JPanel>(0);
		initialize();
	}
	
	/**
	 * Initialize the panel
	 */
	public void initialize()
	{
		this.setLayout(new BorderLayout());
		this.setBackground(backgroundColor);
		thisContentPane.setLayout(new BorderLayout());
		thisContentPane.setBackground(backgroundColor);
		
		// minusButton.setIcon(DisplayStatics.minusIcon2);
		// minusButton.setText(null);
		minusButton.setText("-");
		// plusButton.setIcon(DisplayStatics.plusIcon2);
		// plusButton.setText(null);
		plusButton.setText("+");
		minusButton.setMaximumSize(new Dimension(15, 15));
		plusButton.setMaximumSize(new Dimension(15, 15));
		minusButton.setPreferredSize(new Dimension(15, 15));
		plusButton.setPreferredSize(new Dimension(15, 15));
		minusButton.addActionListener(this);
		plusButton.addActionListener(this);
		
		titlePane.setBackground(menuBarColor);
		titlePane.setLayout(new BoxLayout(titlePane, BoxLayout.X_AXIS));
		titlePane.setPreferredSize(new Dimension(15, 15));
		titlePane.add(Box.createHorizontalGlue());
		titlePane.add(titleLabel);
		titlePane.add(Box.createHorizontalGlue());
		
		centerPane.setBackground(foregroundColor);
		centerPane.setLayout(new BoxLayout(centerPane, BoxLayout.PAGE_AXIS));
		centralScrollPane.setBorder(BorderFactory.createEmptyBorder());
		centralScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		centralScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		
		mainPane.setBackground(foregroundColor);
		mainPane.setLayout(new BorderLayout());
		mainPane.add(centralScrollPane, BorderLayout.CENTER);
		subPane.setBackground(menuBarColor);
		subPane.setLayout(new BoxLayout(subPane, BoxLayout.LINE_AXIS));
		subPane.setPreferredSize(new Dimension(15, 15));
		if(this.addRemoveButtons)
		{
			subPane.add(plusButton);
			// subPane.add(Box.createHorizontalStrut(5));
			subPane.add(minusButton);
			subPane.add(Box.createHorizontalGlue());
		}
		
		thisContentPane.add(titlePane, BorderLayout.PAGE_START);
		thisContentPane.add(mainPane, BorderLayout.CENTER);
		thisContentPane.add(subPane, BorderLayout.PAGE_END);
		
		this.add(thisContentPane, BorderLayout.CENTER);
		if(!right)
			this.add(Box.createRigidArea(new Dimension(sideSpace, sideSpace)), BorderLayout.LINE_END);
		if(!left)
			this.add(Box.createRigidArea(new Dimension(sideSpace, sideSpace)), BorderLayout.LINE_START);
		if(!top)
			this.add(Box.createRigidArea(new Dimension(sideSpace, sideSpace)), BorderLayout.PAGE_START);
		if(!bottom)
			this.add(Box.createRigidArea(new Dimension(sideSpace, sideSpace)), BorderLayout.PAGE_END);
	}
	
	/**
	 * Set the title of the panel
	 * 
	 * @param title
	 */
	public void setPanelTitle(String title)
	{
		this.title = title;
		this.titleLabel.setText(title);
		this.titleLabel.setFont(FontUtility.boldFont);
	}
	
	/**
	 * Set a panel instead of a label for the title bar
	 * 
	 * @param panel
	 */
	public void setPanelTitle(JPanel panel)
	{
		panel.setBackground(menuBarColor);
		titlePane.removeAll();
		titlePane.add(panel);
	}
	
	/**
	 * Return the title of the panel
	 * 
	 * @return the title of the panel
	 */
	public String getPanelTitle()
	{
		return this.title;
	}
	
	/**
	 * Add a panel to the center of this panel
	 * 
	 * @param pane
	 */
	public void add(JPanel pane)
	{
		contentList.add(pane);
		pane.setBackground(foregroundColor);
		this.refreshDisplay();
	}
	
	/**
	 * Remove the content of this panel
	 */
	public void clear()
	{
		centerPane.removeAll();
		contentList = new ArrayList<JPanel>(0);
	}
	
	/**
	 * Refresh the display of this panel
	 */
	public void refreshDisplay()
	{
		centerPane.removeAll();
		for (JPanel p : contentList)
		{
			centerPane.add(p);
		}
		centerPane.add(Box.createVerticalGlue());
		this.repaint();
	}
	
	/**
	 * Set the spacing rules of the panel
	 * 
	 * @param left
	 * @param right
	 * @param top
	 * @param bottom
	 */
	public void setcloseSpacing(boolean left, boolean right, boolean top, boolean bottom)
	{
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
		
		this.removeAll();
		this.add(thisContentPane, BorderLayout.CENTER);
		if(!right)
			this.add(Box.createRigidArea(new Dimension(sideSpace, sideSpace)), BorderLayout.LINE_END);
		if(!left)
			this.add(Box.createRigidArea(new Dimension(sideSpace, sideSpace)), BorderLayout.LINE_START);
		if(!top)
			this.add(Box.createRigidArea(new Dimension(sideSpace, sideSpace)), BorderLayout.PAGE_START);
		if(!bottom)
			this.add(Box.createRigidArea(new Dimension(sideSpace, sideSpace)), BorderLayout.PAGE_END);
		
		this.repaint();
	}
	
	public void add()
	{}
	
	public void remove()
	{}
	
	/**
	 * Listen to button clicks
	 */
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == minusButton)
		{
			Logs.log("Remove button clicked", 1, this);
			remove();
		}
		if(e.getSource() == plusButton)
		{
			Logs.log("Creation button clicked", 1, this);
			add();
		}
	}
	
	class BluryGlassPane extends JPanel {
		
		private static final long serialVersionUID = 1L;
		private BufferedImage blurBuffer;
		private BufferedImage backBuffer;
		private float alpha = 0.0f;
		
		BluryGlassPane()
		{
			setLayout(new GridBagLayout());
			
			// Should also disable key events...
			addMouseListener(new MouseAdapter() {});
		}
		
		private void createBlur()
		{
			JRootPane root = SwingUtilities.getRootPane(this);
			blurBuffer = GraphicsUtilities.createCompatibleImage(getWidth(), getHeight());
			Graphics2D g2 = blurBuffer.createGraphics();
			root.paint(g2);
			g2.dispose();
			
			backBuffer = blurBuffer;
			
			blurBuffer = GraphicsUtilities.createThumbnailFast(blurBuffer, getWidth() / 2);
			blurBuffer = new GaussianBlurFilter(5).filter(blurBuffer, null);
		}
		
		@Override
		protected void paintComponent(Graphics g)
		{
			if(isVisible() && blurBuffer != null)
			{
				Graphics2D g2 = (Graphics2D) g.create();
				
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2.drawImage(backBuffer, 0, 0, null);
				
				g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
				g2.drawImage(blurBuffer, 0, 0, getWidth(), getHeight(), null);
				g2.dispose();
			}
		}
		
		public float getAlpha()
		{
			return alpha;
		}
		
		public void setAlpha(float alpha)
		{
			this.alpha = alpha;
			repaint();
		}
		
		public void fadeIn()
		{
			createBlur();
			
			setVisible(true);
			this.setAlpha(1.0f);
		}
		
		public void fadeOut()
		{
			createBlur();
			
			setVisible(false);
			this.setAlpha(0.0f);
		}
		
	}
}
