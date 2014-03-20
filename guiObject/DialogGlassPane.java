package guiObject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import miscellaneous.FontUtility;

public class DialogGlassPane extends FixedGlassPane implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private DialogGlassCenterPanel centerPane;
	private int thisW = 300;
	private int thisH = 400;
	private String title;
	
	private JButton yesButton = new JButton("Ok");
	private JButton noButton = new JButton("Cancel");
	
	public DialogGlassPane()
	{
		super(JEXStatics.main.getContentPane());
		
		// initialize
		yesButton.addActionListener(this);
		noButton.addActionListener(this);
		
		// build gui
		rebuild();
	}
	
	public DialogGlassPane(String title)
	{
		super(JEXStatics.main.getContentPane());
		
		// initialize
		this.title = title;
		yesButton.addActionListener(this);
		noButton.addActionListener(this);
		
		// build gui
		rebuild();
	}
	
	private void rebuild()
	{
		this.removeAll();
		
		yesButton.setMaximumSize(new Dimension(50, 20));
		noButton.setMaximumSize(new Dimension(50, 20));
		JPanel buttonpane = new JPanel();
		buttonpane.setLayout(new BoxLayout(buttonpane, BoxLayout.LINE_AXIS));
		buttonpane.setBackground(DisplayStatics.lightBackground);
		buttonpane.add(Box.createHorizontalGlue());
		buttonpane.add(noButton);
		buttonpane.add(yesButton);
		buttonpane.add(Box.createHorizontalGlue());
		JPanel bottomPane = new JPanel();
		bottomPane.setLayout(new BorderLayout());
		bottomPane.setBackground(DisplayStatics.lightBackground);
		bottomPane.add(buttonpane, BorderLayout.CENTER);
		bottomPane.add(Box.createRigidArea(new Dimension(10, 10)), BorderLayout.PAGE_END);
		
		// Place the panel in a larger panel
		int w = JEXStatics.main.getWidth();
		int h = JEXStatics.main.getHeight();
		JPanel diagPane = new JPanel();
		diagPane.setLayout(new BorderLayout());
		diagPane.setBackground(DisplayStatics.lightBackground);
		diagPane.setBounds(w / 2 - thisW / 2, 70, thisW, thisH);
		diagPane.setBorder(BorderFactory.createLineBorder(DisplayStatics.textColor, 3));
		diagPane.add(Box.createRigidArea(new Dimension(10, 10)), BorderLayout.LINE_END);
		diagPane.add(Box.createRigidArea(new Dimension(10, 10)), BorderLayout.LINE_START);
		if(title == null)
		{
			diagPane.add(Box.createRigidArea(new Dimension(10, 10)), BorderLayout.PAGE_START);
		}
		else
		{
			JLabel titleLabel = new JLabel(title);
			titleLabel.setFont(FontUtility.boldFont);
			JPanel titlePane = new JPanel();
			titlePane.setLayout(new BoxLayout(titlePane, BoxLayout.LINE_AXIS));
			titlePane.setBackground(DisplayStatics.lightBackground);
			titlePane.add(Box.createHorizontalGlue());
			titlePane.add(titleLabel);
			titlePane.add(Box.createHorizontalGlue());
			diagPane.add(titlePane, BorderLayout.PAGE_START);
		}
		diagPane.add(bottomPane, BorderLayout.PAGE_END);
		if(centerPane == null)
		{
			JPanel defaultCenterPane = new JPanel();
			defaultCenterPane.setLayout(new BorderLayout());
			defaultCenterPane.setBackground(DisplayStatics.lightBackground);
			diagPane.add(defaultCenterPane, BorderLayout.CENTER);
		}
		else
		{
			diagPane.add(centerPane, BorderLayout.CENTER);
		}
		
		// make this panel
		Color c = new Color(0, 0, 0, 0.4f);
		this.setBackground(c);
		this.setBounds(0, 0, w, h);
		this.setLayout(null);
		this.add(diagPane);
		this.revalidate();
		this.repaint();
	}
	
	// ----------------------------------------------------
	// --------- GETTERS and SETTERS ----------------------
	// ----------------------------------------------------
	/**
	 * Set the size of the central dialog panel
	 */
	@Override
	public void setSize(int thisW, int thisH)
	{
		this.thisH = thisH;
		this.thisW = thisW;
		this.rebuild();
	}
	
	/**
	 * Set the central panel of the dialog
	 * 
	 * @param centerPane
	 */
	public void setCentralPanel(DialogGlassCenterPanel centerPane)
	{
		this.centerPane = centerPane;
		this.rebuild();
	}
	
	// ----------------------------------------------------
	// --------- EVENT HANDLING FUNCTIONS -----------------
	// ----------------------------------------------------
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == noButton)
		{
			JEXStatics.main.displayGlassPane(this, false);
			if(centerPane != null)
			{
				centerPane.cancel();
			}
		}
		if(e.getSource() == yesButton)
		{
			JEXStatics.main.displayGlassPane(this, false);
			if(centerPane != null)
			{
				centerPane.yes();
			}
		}
	}
}
