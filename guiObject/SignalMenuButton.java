package guiObject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import signals.SSCenter;

public class SignalMenuButton extends JPanel implements MouseListener {
	
	private static final long serialVersionUID = 1L;
	
	// statics
	public static final String SIG_ButtonClicked_NULL = "clicked";
	
	// color variables
	private Color backColor = Color.DARK_GRAY;
	private Color foreColor = Color.white;
	private Color clickedBackColor = Color.DARK_GRAY;
	private Color clickedForeColor = Color.white;
	
	private Font userFont = null;
	
	// display info
	private String text = "";
	private Icon icon;
	private Icon mouseOverIcon;
	private Icon mousePressedIcon;
	private Icon disabledIcon;
	private boolean isEnabled = true;
	
	// display gui objects
	private JLabel label;
	
	// listeners
	private Set<ActionListener> listeners;
	
	// ---------------------------------------------
	// Creating and defining
	// ---------------------------------------------
	
	public SignalMenuButton()
	{
		// Initialize variables
		listeners = new HashSet<ActionListener>();
		
		// Initialize graphics
		initialize();
		refresh();
	}
	
	public SignalMenuButton(String text, Image icon, Color background, Color foreground, String tooltiptext)
	{
		this();
		this.setBackgroundColor(background);
		this.setForegroundColor(foreground);
		this.setClickedColor(background, foreground);
		this.setImage(icon);
		this.setMouseOverImage(icon);
		this.setMousePressedImage(icon);
		this.setDisabledImage(icon);
		this.setText(text);
		this.setToolTipText(tooltiptext);
	}
	
	public SignalMenuButton(String text, Image icon, Image mouseOverIcon, Image mousePressedIcon, Image disabledImage, Color background, Color foreground, Color clickedBackground, Color clickedForeground, String tooltiptext)
	{
		this();
		this.setBackgroundColor(background);
		this.setForegroundColor(foreground);
		this.setClickedColor(clickedBackground, clickedForeground);
		this.setImage(icon);
		this.setMouseOverImage(mouseOverIcon);
		this.setMousePressedImage(mousePressedIcon);
		this.setDisabledImage(disabledImage);
		this.setText(text);
		this.setToolTipText(tooltiptext);
	}
	
	public SignalMenuButton(String text, Image icon, Image mouseOverIcon, Image mousePressedIcon, Image disabledImage, Color background, Color foreground, Color clickedBackground, Color clickedForeground, String tooltiptext, Dimension dimension)
	{
		this();
		this.setBackgroundColor(background);
		this.setForegroundColor(foreground);
		this.setClickedColor(clickedBackground, clickedForeground);
		this.setImage(icon);
		this.setMouseOverImage(mouseOverIcon);
		this.setMousePressedImage(mousePressedIcon);
		this.setDisabledImage(disabledImage);
		this.setText(text);
		this.setToolTipText(tooltiptext);
		this.setSize(dimension);
	}
	
	/**
	 * Initialize the graphics
	 */
	private void initialize()
	{
		this.addMouseListener(this);
		this.setBackground(backColor);
		this.setForeground(foreColor);
		this.setLayout(new BorderLayout());
		this.setSize(new Dimension(50, 50));
		this.setBorder(BorderFactory.createEmptyBorder());
	}
	
	/**
	 * Rebuild graphics
	 */
	private void refresh()
	{
		label = new JLabel();
		label.setText(text);
		if(userFont != null)
			label.setFont(userFont);
		label.setIcon(icon);
		label.setVerticalTextPosition(SwingConstants.BOTTOM);
		label.setHorizontalTextPosition(SwingConstants.CENTER);
		label.setForeground(Color.black);
		label.setBackground(backColor);
		
		this.removeAll();
		this.add(label, BorderLayout.CENTER);
		
		this.invalidate();
		this.validate();
		this.repaint();
	}
	
	// ---------------------------------------------
	// Getters and setters
	// ---------------------------------------------
	
	/**
	 * Set the size of the button
	 */
	@Override
	public void setSize(Dimension d)
	{
		// this.setMinimumSize(d);
		this.setMaximumSize(d);
		// this.setPreferredSize(d);
		refresh();
	}
	
	/**
	 * Set the icon displayed in the button
	 * 
	 * @param icon
	 */
	public void setIcon(Icon icon)
	{
		this.icon = icon;
		refresh();
	}
	
	/**
	 * Return the icon of this button
	 * 
	 * @return icon
	 */
	public Icon icon()
	{
		return this.icon;
	}
	
	/**
	 * Set the icon displayed when the mouse is over the button
	 * 
	 * @param mouseOverIcon
	 */
	public void setMouseOverIcon(Icon mouseOverIcon)
	{
		this.mouseOverIcon = mouseOverIcon;
		refresh();
	}
	
	/**
	 * Return the mouse over icon of this button
	 * 
	 * @return icon
	 */
	public Icon mouseOverIcon()
	{
		return this.mouseOverIcon;
	}
	
	/**
	 * Set the icon displayed when the mouse button pressed
	 * 
	 * @param mouseOverIcon
	 */
	public void setMousePressedIcon(Icon mousePressedIcon)
	{
		this.mousePressedIcon = mousePressedIcon;
		refresh();
	}
	
	/**
	 * Return the mouse pressed icon of this button
	 * 
	 * @return
	 */
	public Icon mousePressedIcon()
	{
		return this.mousePressedIcon;
	}
	
	/**
	 * Set the icon displayed in the button from image at path PATH and dimension D
	 * 
	 * @param path
	 * @param d
	 */
	public void setImage(String path, Dimension d)
	{
		try
		{
			// the line that reads the image file
			BufferedImage image = ImageIO.read(new File(path));
			Image scaledimage = image.getScaledInstance((int) d.getWidth(), (int) d.getHeight(), Image.SCALE_DEFAULT);
			this.icon = new ImageIcon(scaledimage);
			refresh();
		}
		catch (IOException e)
		{
			// log the exception
			// re-throw if desired
		}
	}
	
	/**
	 * Set the icon displayed in the button from image at path PATH and dimension D
	 * 
	 * @param path
	 * @param d
	 */
	public void setImage(URL url, Dimension d)
	{
		if(url == null)
		{
			this.icon = null;
			refresh();
			return;
		}
		Image image = Toolkit.getDefaultToolkit().getImage(url);
		Image scaledimage = image.getScaledInstance((int) d.getWidth(), (int) d.getHeight(), Image.SCALE_SMOOTH);
		this.icon = new ImageIcon(scaledimage);
		refresh();
	}
	
	/**
	 * Set the icon displayed when the mouse is over the button from image at path PATH and dimension D
	 * 
	 * @param path
	 * @param d
	 */
	public void setMouseOverImage(URL url, Dimension d)
	{
		if(url == null)
		{
			this.mouseOverIcon = null;
			refresh();
			return;
		}
		Image image = Toolkit.getDefaultToolkit().getImage(url);
		Image scaledimage = image.getScaledInstance((int) d.getWidth(), (int) d.getHeight(), Image.SCALE_SMOOTH);
		this.mouseOverIcon = new ImageIcon(scaledimage);
		refresh();
	}
	
	/**
	 * Set the icon displayed when the mouse button pressed from image at path PATH and dimension D
	 * 
	 * @param path
	 * @param d
	 */
	public void setMousePressedImage(URL url, Dimension d)
	{
		if(url == null)
		{
			this.mousePressedIcon = null;
			refresh();
			return;
		}
		Image image = Toolkit.getDefaultToolkit().getImage(url);
		Image scaledimage = image.getScaledInstance((int) d.getWidth(), (int) d.getHeight(), Image.SCALE_SMOOTH);
		this.mousePressedIcon = new ImageIcon(scaledimage);
		refresh();
	}
	
	/**
	 * Set the icon displayed when the enabled flag is set to false from image at path PATH and dimension D
	 * 
	 * @param path
	 * @param d
	 */
	public void setDisabledImage(URL url, Dimension d)
	{
		if(url == null)
		{
			this.disabledIcon = null;
			refresh();
			return;
		}
		Image image = Toolkit.getDefaultToolkit().getImage(url);
		Image scaledimage = image.getScaledInstance((int) d.getWidth(), (int) d.getHeight(), Image.SCALE_SMOOTH);
		this.disabledIcon = new ImageIcon(scaledimage);
		
		if(disabledIcon != null)
			label.setIcon(disabledIcon);
		this.repaint();
	}
	
	/**
	 * Set the icon displayed in the button from image at path PATH and dimension D
	 * 
	 * @param path
	 * @param d
	 */
	public void setImage(Image image)
	{
		if(image == null)
		{
			this.icon = null;
			refresh();
			return;
		}
		this.icon = new ImageIcon(image);
		refresh();
	}
	
	/**
	 * Set the icon displayed when the mouse is over the button from image IMAGE
	 * 
	 * @param image
	 */
	public void setMouseOverImage(Image image)
	{
		if(image == null)
		{
			this.mouseOverIcon = null;
			refresh();
			return;
		}
		this.mouseOverIcon = new ImageIcon(image);
		refresh();
	}
	
	/**
	 * Set the icon displayed when the mouse button pressed from image IMAGE
	 * 
	 * @param image
	 */
	public void setMousePressedImage(Image image)
	{
		if(image == null)
		{
			this.mousePressedIcon = null;
			refresh();
			return;
		}
		this.mousePressedIcon = new ImageIcon(image);
		refresh();
	}
	
	/**
	 * Set the icon displayed when the enabled flag is set to false from image IMAGE
	 * 
	 * @param image
	 */
	public void setDisabledImage(Image image)
	{
		if(image == null)
		{
			this.disabledIcon = null;
			refresh();
			return;
		}
		this.disabledIcon = new ImageIcon(image);
		
		// if (disabledIcon != null) label.setIcon(disabledIcon);
		// this.repaint();
	}
	
	/**
	 * Set the text displayed under the button
	 * 
	 * @param text
	 */
	public void setText(String text)
	{
		this.text = text;
		refresh();
	}
	
	/**
	 * Set the font used to display the text of this button
	 * 
	 * @param f
	 */
	public void setLabelFont(Font f)
	{
		this.userFont = f;
	}
	
	/**
	 * Set the background color of the button
	 * 
	 * @param backColor
	 */
	public void setBackgroundColor(Color backColor)
	{
		this.backColor = backColor;
		this.setBackground(backColor);
		label.setBackground(backColor);
		this.repaint();
	}
	
	/**
	 * Set the foreground color of the button
	 * 
	 * @param foreColor
	 */
	public void setForegroundColor(Color foreColor)
	{
		this.foreColor = foreColor;
		this.setForeground(foreColor);
		label.setForeground(foreColor);
		this.repaint();
	}
	
	/**
	 * Set the mouse pressed color of the button
	 * 
	 * @param clickedColor
	 */
	public void setClickedColor(Color clickedBackColor, Color clickedForeColor)
	{
		this.clickedBackColor = clickedBackColor;
		this.clickedForeColor = clickedForeColor;
	}
	
	/**
	 * Add action listener to the button
	 * 
	 * @param listener
	 */
	public void addActionListener(ActionListener listener)
	{
		this.listeners.add(listener);
	}
	
	/**
	 * Remove action listener from the button
	 * 
	 * @param listener
	 */
	public void removeActionListener(ActionListener listener)
	{
		listeners.remove(listener);
	}
	
	/**
	 * Set the enabled flag to ISENABLED
	 */
	@Override
	public void setEnabled(boolean isEnabled)
	{
		this.isEnabled = isEnabled;
		
		if(isEnabled)
		{
			label.setIcon(icon);
		}
		else
		{
			label.setIcon(disabledIcon);
		}
		
		this.repaint();
	}
	
	/**
	 * Returns the isEnabled flag
	 */
	@Override
	public boolean isEnabled()
	{
		return this.isEnabled;
	}
	
	// ---------------------------------------------
	// Signals
	// ---------------------------------------------
	
	/**
	 * Emit the signal
	 */
	private void emitSignal()
	{
		SSCenter.defaultCenter().emit(this, SIG_ButtonClicked_NULL, (Object[]) null);
	}
	
	// ---------------------------------------------
	// Events
	// ---------------------------------------------
	
	/**
	 * Emit an action event to all listeners
	 */
	private void emitActionEvent()
	{
		for (ActionListener a : listeners)
		{
			ActionEvent e = new ActionEvent(this, 0, null);
			a.actionPerformed(e);
		}
	}
	
	public void mouseClicked(MouseEvent e)
	{   
		
	}
	
	public void mouseEntered(MouseEvent e)
	{
		if(!this.isEnabled())
			return;
		
		if(mouseOverIcon != null)
			label.setIcon(mouseOverIcon);
		label.repaint();
	}
	
	public void mouseExited(MouseEvent e)
	{
		if(!this.isEnabled())
			return;
		
		label.setIcon(icon);
		label.repaint();
	}
	
	public void mousePressed(MouseEvent e)
	{
		if(!this.isEnabled())
			return;
		
		if(mousePressedIcon != null)
			label.setIcon(mousePressedIcon);
		
		label.setBackground(clickedBackColor);
		this.setBackground(clickedBackColor);
		label.setForeground(clickedForeColor);
		this.setForeground(clickedForeColor);
		this.repaint();
	}
	
	public void mouseReleased(MouseEvent e)
	{
		if(!this.isEnabled())
			return;
		
		label.setIcon(icon);
		
		label.setBackground(backColor);
		this.setBackground(backColor);
		label.setForeground(Color.black);
		this.setForeground(foreColor);
		this.repaint();
		
		emitActionEvent();
		emitSignal();
	}
	
	// public void repaint(){
	// super.repaint();
	// }
}
