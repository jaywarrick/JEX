package guiObject;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public class JFormPanel extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	// Results
	public boolean isValidated = false;
	
	// Model variables
	List<JFormEntry> formEntries;
	
	// GUI objects
	private List<JFormEntryPane> formEntryPanels;
	private Color backgroundColor = Color.WHITE;
	private JButton createButton;
	private List<ActionListener> listeners;
	
	/**
	 * Create a new empty FormPanel
	 */
	public JFormPanel()
	{
		formEntries = new ArrayList<JFormEntry>(0);
		listeners = new ArrayList<ActionListener>(0);
		initialize();
	}
	
	/**
	 * Initialize the form
	 */
	public void initialize()
	{
		// layout of this form
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		createButton = new JButton("Create");
		createButton.setMaximumSize(new Dimension(100, 20));
		createButton.setPreferredSize(new Dimension(100, 20));
		createButton.addActionListener(this);
		
		this.makeForm();
	}
	
	/**
	 * Add an entry line to the form
	 * 
	 * @param title
	 * @param note
	 * @param type
	 * @param options
	 */
	public void addEntry(String title, String note, String type, String[] options)
	{
		JFormEntry entry = new JFormEntry(title, note, type, options);
		this.formEntries.add(entry);
	}
	
	/**
	 * Add an entry line to the form
	 * 
	 * @param title
	 * @param note
	 * @param type
	 * @param options
	 */
	public void addEntry(String title, String note, String type, String[] options, int selected)
	{
		JFormEntry entry = new JFormEntry(title, note, type, options, selected);
		this.formEntries.add(entry);
	}
	
	/**
	 * Set the background color
	 * 
	 * @param backgroundColor
	 */
	public void setBackgroundColor(Color backgroundColor)
	{
		this.backgroundColor = backgroundColor;
	}
	
	/**
	 * Make the form
	 */
	public void makeForm()
	{
		this.removeAll();
		this.setBackground(backgroundColor);
		
		formEntryPanels = new ArrayList<JFormEntryPane>(0);
		for (JFormEntry fe : formEntries)
		{
			JFormEntryPane fep = new JFormEntryPane(fe);
			fep.setBackground(backgroundColor);
			formEntryPanels.add(fep);
			fep.setAlignmentX(LEFT_ALIGNMENT);
			this.add(fep);
		}
		this.add(Box.createVerticalStrut(10));
		this.add(createButton);
		
		// this.add(new JSeparator(JSeparator.HORIZONTAL));
		// this.add(Box.createVerticalGlue());
	}
	
	/**
	 * Return the value of entry line titled NAME
	 * 
	 * @param name
	 * @return the value for key NAME
	 */
	public String getValue(String name)
	{
		for (JFormEntryPane fep : formEntryPanels)
		{
			if(fep.entry.title.equals(name))
			{
				String result = fep.getValue();
				return result;
			}
		}
		return null;
	}
	
	/**
	 * Add an action listener to this form panel
	 * 
	 * @param listener
	 */
	public void addActionListener(ActionListener listener)
	{
		listeners.add(listener);
	}
	
	/**
	 * Clicked ok or cancel
	 */
	public void actionPerformed(ActionEvent e)
	{
		ActionEvent event = new ActionEvent(this, 0, null);
		for (ActionListener id : listeners)
		{
			id.actionPerformed(event);
		}
	}
	
}
