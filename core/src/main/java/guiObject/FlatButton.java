package guiObject;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class FlatButton extends JPanel implements MouseListener {
	
	private static final long serialVersionUID = 1L;
	// INTERFACE VARIABLES
	private String name;
	private List<ActionListener> listeners;
	
	// GUI VARIABLES
	private Color bckGround = Color.BLACK;
	private Color foreGround = Color.WHITE;
	private JLabel nameLabel;
	
	public FlatButton(String name)
	{
		this.name = name;
		this.initialize();
	}
	
	public void initialize()
	{
		this.setBackground(bckGround);
		this.setLayout(new FlowLayout());
		this.setMaximumSize(new Dimension(200, 18));
		
		nameLabel = new JLabel(name);
		nameLabel.setBackground(bckGround);
		nameLabel.setForeground(foreGround);
		nameLabel.setMaximumSize(new Dimension(200, 18));
		
		this.add(nameLabel);
		this.addMouseListener(this);
		
		listeners = new ArrayList<ActionListener>(0);
	}
	
	public void addActionListener(ActionListener id)
	{
		listeners.add(id);
	}
	
	@Override
	public void paint(Graphics g)
	{
		if(g == null)
			return;
		Graphics2D g2 = (Graphics2D) g;
		super.paint(g);
		g2.setColor(this.foreGround);
		// g2.drawRect(0, 3, this.getWidth()-1, this.getHeight()-1);
		g2.drawRoundRect(0, 2, this.getWidth() - 2, this.getHeight() - 7, 3, 3);
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
