package guiObject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import logs.Logs;
import miscellaneous.FontUtility;

public class JRoundedCollapsablePanel extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	// Variables
	private String title = "";
	private Color backgroundColor = DisplayStatics.background;
	private Color foregroundColor = Color.white;
	private List<JComponent> contentList;
	
	// Hidden variables
	private JPanel thisContentPane = new JPanel();
	private JLabel titleLabel = new JLabel("");
	private JPanel titlePane = new JPanel();
	private JPanel mainPane = new JPanel();
	private JButton reduce = new JButton("^");
	private JButton expand = new JButton("v");
	// private int arcRadius = 15;
	private boolean left = false;
	private boolean right = false;
	private boolean top = false;
	private boolean bottom = false;
	
	public JRoundedCollapsablePanel()
	{
		contentList = new ArrayList<JComponent>(0);
		initialize();
	}
	
	public void initialize()
	{
		this.setLayout(new BorderLayout());
		this.setBackground(backgroundColor);
		thisContentPane.setLayout(new BorderLayout());
		thisContentPane.setBackground(backgroundColor);
		
		reduce.setMaximumSize(new Dimension(20, 20));
		reduce.setPreferredSize(new Dimension(20, 20));
		reduce.addActionListener(this);
		expand.setMaximumSize(new Dimension(20, 20));
		expand.setPreferredSize(new Dimension(20, 20));
		expand.addActionListener(this);
		
		titlePane.setBackground(foregroundColor);
		titlePane.setLayout(new BoxLayout(titlePane, BoxLayout.X_AXIS));
		titlePane.setPreferredSize(new Dimension(20, 20));
		titlePane.add(titleLabel);
		titlePane.add(Box.createHorizontalGlue());
		titlePane.add(reduce);
		
		mainPane.setBackground(foregroundColor);
		mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));
		
		thisContentPane.add(titlePane, BorderLayout.PAGE_START);
		thisContentPane.add(mainPane, BorderLayout.CENTER);
		
		this.add(thisContentPane, BorderLayout.CENTER);
		if(!right)
			this.add(Box.createRigidArea(new Dimension(3, 3)), BorderLayout.LINE_END);
		if(!left)
			this.add(Box.createRigidArea(new Dimension(3, 3)), BorderLayout.LINE_START);
		if(!top)
			this.add(Box.createRigidArea(new Dimension(3, 3)), BorderLayout.PAGE_START);
		if(!bottom)
			this.add(Box.createRigidArea(new Dimension(3, 3)), BorderLayout.PAGE_END);
	}
	
	public void reduce()
	{
		thisContentPane.removeAll();
		thisContentPane.add(titlePane, BorderLayout.PAGE_START);
		titlePane.removeAll();
		titlePane.add(titleLabel);
		titlePane.add(Box.createHorizontalGlue());
		titlePane.add(expand);
		
		mainPane.setMaximumSize(new Dimension(200, 20));
		this.setMaximumSize(new Dimension(200, 20));
		
		if(this.getParent() != null)
		{
			if(this.getParent() instanceof JPanel)
			{
				JPanel parent = (JPanel) this.getParent();
				parent.updateUI();
			}
		}
		
		this.invalidate();
		this.validate();
		this.repaint();
	}
	
	public void expand()
	{
		titlePane.removeAll();
		titlePane.add(titleLabel);
		titlePane.add(Box.createHorizontalGlue());
		titlePane.add(reduce);
		thisContentPane.removeAll();
		thisContentPane.add(titlePane, BorderLayout.PAGE_START);
		thisContentPane.add(mainPane, BorderLayout.CENTER);
		
		refresh();
	}
	
	public void setPanelTitle(String title)
	{
		this.title = title;
		this.titleLabel.setText(title);
		this.titleLabel.setFont(FontUtility.boldFont);
	}
	
	public String getPanelTitle()
	{
		return this.title;
	}
	
	// public void add(JPanel pane){
	// contentList.add(pane);
	// pane.setBackground(foregroundColor);
	// this.refresh();
	// }
	
	public void addComponent(JComponent pane)
	{
		contentList.add(pane);
		pane.setBackground(foregroundColor);
		this.refresh();
	}
	
	public void clear()
	{
		mainPane.removeAll();
		contentList = new ArrayList<JComponent>(0);
	}
	
	public void refresh()
	{
		mainPane.removeAll();
		for (JComponent p : contentList)
		{
			p.setAlignmentX(Component.LEFT_ALIGNMENT);
			mainPane.add(p);
		}
		mainPane.setMaximumSize(new Dimension(200, contentList.size() * 20 + 40));
		this.setMaximumSize(new Dimension(200, contentList.size() * 20 + 60));
		
		if(this.getParent() != null)
		{
			if(this.getParent() instanceof JPanel)
			{
				JPanel parent = (JPanel) this.getParent();
				parent.updateUI();
			}
		}
		
		this.repaint();
	}
	
	public void setcloseSpacing(boolean left, boolean right, boolean top, boolean bottom)
	{
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
		
		this.removeAll();
		this.add(thisContentPane, BorderLayout.CENTER);
		if(!right)
			this.add(Box.createRigidArea(new Dimension(3, 3)), BorderLayout.LINE_END);
		if(!left)
			this.add(Box.createRigidArea(new Dimension(3, 3)), BorderLayout.LINE_START);
		if(!top)
			this.add(Box.createRigidArea(new Dimension(3, 3)), BorderLayout.PAGE_START);
		if(!bottom)
			this.add(Box.createRigidArea(new Dimension(3, 3)), BorderLayout.PAGE_END);
		
		this.repaint();
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == reduce)
		{
			Logs.log("Reducing panel", 1, this);
			reduce();
		}
		if(e.getSource() == expand)
		{
			Logs.log("Expanding panel", 1, this);
			expand();
		}
	}
}
