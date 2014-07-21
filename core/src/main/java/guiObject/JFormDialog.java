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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import miscellaneous.FontUtility;

public class JFormDialog extends JDialog implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	// Results
	public boolean isValidated = false;
	
	// Model variables
	List<JFormEntry> formEntries;
	String dialogTitle = "";
	String dialogMessage;
	
	// GUI objects
	private JButton okButton = new JButton("OK");
	private JButton cancelButton = new JButton("Cancel");
	private JPanel buttonPanel = new JPanel();
	private JPanel centerPane = new JPanel();
	private List<JFormEntryPane> formEntryPanels;
	
	/**
	 * Create a new empty FormPanel
	 */
	public JFormDialog()
	{
		formEntries = new ArrayList<JFormEntry>(0);
		initialize();
	}
	
	/**
	 * Create a new empty FromPanel with a title and a message
	 * 
	 * @param dialogTitle
	 * @param dialogMessage
	 */
	public JFormDialog(String dialogTitle, String dialogMessage)
	{
		formEntries = new ArrayList<JFormEntry>(0);
		this.dialogTitle = dialogTitle;
		this.dialogMessage = dialogMessage;
		initialize();
	}
	
	/**
	 * Create a new empty FromPanel with a title and a message
	 * 
	 * @param dialogTitle
	 * @param dialogMessage
	 * @param owner
	 */
	public JFormDialog(String dialogTitle, String dialogMessage, JDialog owner)
	{
		super(owner);
		formEntries = new ArrayList<JFormEntry>(0);
		this.dialogTitle = dialogTitle;
		this.dialogMessage = dialogMessage;
		initialize();
	}
	
	/**
	 * Initialize the form
	 */
	public void initialize()
	{
		// layout of this form
		centerPane.setBackground(Color.white);
		centerPane.setLayout(new BoxLayout(centerPane, BoxLayout.PAGE_AXIS));
		this.setContentPane(centerPane);
		this.setTitle(dialogTitle);
		
		// Buttons
		okButton.setMaximumSize(new Dimension(60, 20));
		okButton.setPreferredSize(new Dimension(60, 20));
		okButton.addActionListener(this);
		cancelButton.setMaximumSize(new Dimension(60, 20));
		cancelButton.setPreferredSize(new Dimension(60, 20));
		cancelButton.addActionListener(this);
		buttonPanel.setBackground(Color.white);
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setMaximumSize(new Dimension(140, 20));
		buttonPanel.setPreferredSize(new Dimension(140, 20));
		buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(okButton);
		buttonPanel.add(Box.createHorizontalStrut(10));
		buttonPanel.add(cancelButton);
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
	 * Display the form
	 */
	public void dispay()
	{
		this.makeForm();
		this.setModal(true);
		this.setVisible(true);
	}
	
	/**
	 * Make the form
	 */
	public void makeForm()
	{
		if(dialogMessage != null)
		{
			JLabel messageLabel = new JLabel(dialogMessage);
			messageLabel.setFont(FontUtility.boldFont);
			messageLabel.setPreferredSize(new Dimension(250, 20));
			messageLabel.setAlignmentX(LEFT_ALIGNMENT);
			centerPane.add(messageLabel);
			// centerPane.add(new JLabel(""));
			centerPane.add(new JSeparator(SwingConstants.HORIZONTAL));
		}
		
		formEntryPanels = new ArrayList<JFormEntryPane>(0);
		for (JFormEntry fe : formEntries)
		{
			JFormEntryPane fep = new JFormEntryPane(fe);
			formEntryPanels.add(fep);
			fep.setAlignmentX(LEFT_ALIGNMENT);
			centerPane.add(fep);
		}
		
		buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
		centerPane.add(new JSeparator(SwingConstants.HORIZONTAL));
		centerPane.add(buttonPanel);
		centerPane.add(Box.createVerticalGlue());
		
		int height = (formEntries.size() + 4) * 25 + 40;
		this.setMaximumSize(new Dimension(250, height));
		this.setPreferredSize(new Dimension(250, height));
		this.setBounds(300, 200, 400, height);
	}
	
	/**
	 * Return the value of entry line titled NAME
	 * 
	 * @param name
	 * @return String value for key NAME
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
	 * Clicked ok or cancel
	 */
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == okButton)
		{
			isValidated = true;
			for (JFormEntryPane fep : formEntryPanels)
			{
				fep.entry.result = fep.resultField.toString();
			}
			this.dispose();
		}
		if(e.getSource() == cancelButton)
		{
			isValidated = false;
			this.dispose();
		}
	}
	
}
