package jex;

import Database.SingleUserDatabase.JEXDBInfo;
import guiObject.DialogGlassCenterPanel;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;

public class OpenAnywaysPane extends DialogGlassCenterPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private JLabel message = new JLabel();
	private JButton yesbutton = new JButton("Yes");
	private JButton nobutton = new JButton("No");
	private JPanel buttonPane = new JPanel();
	
	private JEXDBInfo dbItem;
	
	public OpenAnywaysPane(JEXDBInfo dbItem)
	{
		this.message.setText("Previous database has not been saved, continue?");
		this.dbItem = dbItem;
		
		// initialize
		initialize();
	}
	
	/**
	 * Initialize
	 */
	private void initialize()
	{
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		this.setBackground(DisplayStatics.lightBackground);
		
		// Fill the layout
		this.add(Box.createVerticalStrut(20));
		this.add(message);
		this.add(Box.createVerticalStrut(15));
		
		yesbutton.setMaximumSize(new Dimension(40, 20));
		yesbutton.setPreferredSize(new Dimension(40, 20));
		yesbutton.addActionListener(this);
		nobutton.setMaximumSize(new Dimension(40, 20));
		nobutton.setPreferredSize(new Dimension(40, 20));
		nobutton.addActionListener(this);
		
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBackground(DisplayStatics.lightBackground);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(yesbutton);
		buttonPane.add(Box.createHorizontalStrut(10));
		buttonPane.add(nobutton);
		buttonPane.add(Box.createHorizontalGlue());
		
		this.add(buttonPane);
		this.add(Box.createVerticalGlue());
	}
	
	/**
	 * Called when clicked yes on the dialog panel
	 */
	public void yes()
	{
		Logs.log("Warning overturned", 1, this);
		JEXStatics.jexManager.setDatabaseInfo(dbItem);
	}
	
	/**
	 * Called when clicked cancel on the dialog panel
	 */
	public void cancel()
	{
		Logs.log("Warning accepted", 1, this);
	}
	
	// ----------------------------------------------------
	// --------- EVENT HANDLING FUNCTIONS -----------------
	// ----------------------------------------------------
	
	public void actionPerformed(ActionEvent e)
	{}
}
