package guiObject;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import net.miginfocom.swing.MigLayout;

public class FlatIconButton extends JPanel implements MouseListener {
	
	private static final long serialVersionUID = 1L;
	
	// INTERFACE VARIABLES
	private String name;
	private Image imageicon;
	private List<ActionListener> listeners;
	
	// GUI VARIABLES
	private Color bckGround = DisplayStatics.lightBackground;
	private Color foreGround = Color.BLACK;
	private JLabel nameLabel;
	private boolean displayLeftBorder = true;
	private boolean displayRightBorder = true;
	private boolean displayTopBorder = true;
	private boolean displayBottomBorder = true;
	
	public FlatIconButton(String name)
	{
		this.name = name;
		this.initialize();
	}
	
	public FlatIconButton(Image imageicon)
	{
		this.imageicon = imageicon;
		this.initialize();
	}
	
	/**
	 * Set the imageicon of this button
	 * 
	 * @param imageicon
	 */
	public void setImage(Image imageicon)
	{
		this.imageicon = imageicon;
		this.repaint();
	}
	
	/**
	 * Set the imageicon of this button
	 * 
	 * @param imageicon
	 */
	public void setImageIcon(ImageIcon imageicon)
	{
		this.imageicon = imageicon.getImage();
		this.repaint();
	}
	
	/**
	 * Initialize the button
	 */
	public void initialize()
	{
		this.setBackground(bckGround);
		// this.setLayout(new BorderLayout());
		this.setLayout(new MigLayout("left, ins 2 6 2 6", "[center]", "[center]"));
		
		if(name != null)
		{
			nameLabel = new JLabel(name);
			nameLabel.setBackground(bckGround);
			nameLabel.setForeground(foreGround);
			this.add(nameLabel);
		}
		
		this.addMouseListener(this);
		listeners = new ArrayList<ActionListener>(0);
	}
	
	/**
	 * Add action listener for a click on this button
	 * 
	 * @param id
	 */
	public void addActionListener(ActionListener id)
	{
		listeners.add(id);
	}
	
	/**
	 * Set the display rules for the border around the button
	 * 
	 * @param left
	 * @param right
	 * @param top
	 * @param bottom
	 */
	public void setBorderActive(boolean left, boolean right, boolean top, boolean bottom)
	{
		displayLeftBorder = left;
		displayRightBorder = right;
		displayTopBorder = top;
		displayBottomBorder = bottom;
		this.repaint();
	}
	
	/**
	 * Override the painting method
	 */
	@Override
	public void paint(Graphics g)
	{
		if(g == null)
			return;
		Graphics2D g2 = (Graphics2D) g;
		super.paint(g);
		
		// draw border
		g2.setColor(this.foreGround);
		if(displayLeftBorder)
			g2.drawLine(0, 0, 0, this.getHeight() - 1);
		if(displayRightBorder)
			g2.drawLine(this.getWidth() - 1, 0, this.getWidth() - 1, this.getHeight() - 1);
		if(displayTopBorder)
			g2.drawLine(0, 0, this.getWidth() - 1, 0);
		if(displayBottomBorder)
			g2.drawLine(0, this.getHeight() - 1, this.getWidth() - 1, this.getHeight() - 1);
		
		// draw image
		g2.drawImage(imageicon, 2, 2, this.getWidth() - 2, this.getHeight() - 2, bckGround, null);
	}
	
	public void mouseClicked(MouseEvent e)
	{}
	
	public void mouseEntered(MouseEvent e)
	{}
	
	public void mouseExited(MouseEvent e)
	{}
	
	public void mousePressed(MouseEvent e)
	{}
	
	public void mouseReleased(MouseEvent e)
	{
		ActionEvent event = new ActionEvent(this, 0, null);
		for (ActionListener id : listeners)
		{
			id.actionPerformed(event);
		}
	}
}
