package guiObject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jex.statics.DisplayStatics;
import jex.statics.JEXDialog;
import logs.Logs;
import net.miginfocom.swing.MigLayout;
import Database.Definition.Parameter;

public class JParameterPanel implements DocumentListener, ChangeListener, ActionListener {
	
	// Model variable
	Parameter p;
	
	// GUI variables
	private JPanel panel;
	private JLabel titleField;
	private JComponent resultField;
	private JButton fileButton;
	private JButton scriptButton;
	private JScrollPane scriptScroll;
	private JButton applyButton;
	private JButton cancelButton;
	private JDialog editor;
	
	public JParameterPanel(Parameter p)
	{
		this.p = p;
		this.initialize(140);
	}
	
	public JParameterPanel(Parameter p, int rightWidth)
	{
		this.p = p;
		this.initialize(rightWidth);
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	/**
	 * initialize the entry
	 */
	private void initialize(int minRightWidth)
	{
		// This
		this.panel = new JPanel();
		this.panel.setBackground(DisplayStatics.lightBackground);
		this.panel.setLayout(new MigLayout("flowx, ins 0, hidemode 2", "[0:0,fill,grow][0:0,fill," + minRightWidth + "]", "[24,center]"));
		
		// GUI objects
		this.fileButton = new JButton("...");
		this.fileButton.addActionListener(this);
		this.scriptButton = new JButton("Edit Script");
		this.scriptButton.addActionListener(this);
		this.applyButton = new JButton("Apply");
		this.applyButton.addActionListener(this);
		this.cancelButton = new JButton("Cancel");
		this.cancelButton.addActionListener(this);
		this.titleField = new JLabel(this.p.title);
		this.titleField.setToolTipText(this.p.note);
		if(this.p.type == (Parameter.DROPDOWN) && this.p.options != null)
		{
			this.resultField = new JComboBox<String>(this.p.options);
			
			for (int i = 0, len = this.p.options.length; i < len; i++)
			{
				if(this.p.result.equals(this.p.options[i]))
				{
					((JComboBox<?>) this.resultField).setSelectedIndex(i);
				}
			}
			((JComboBox<?>) this.resultField).addActionListener(this);
		}
		else if(this.p.type == (Parameter.CHECKBOX))
		{
			this.resultField = new JCheckBox();
			if(this.p.result.equals("true"))
			{
				((JCheckBox) this.resultField).setSelected(true);
			}
			((JCheckBox) this.resultField).addChangeListener(this);
		}
		else if(this.p.type == (Parameter.FILECHOOSER))
		{
			this.resultField = new JTextField("");
			((JTextField) this.resultField).setText(this.p.result);
			((JTextField) this.resultField).getDocument().addDocumentListener(this);
		}
		else if(this.p.type == (Parameter.PASSWORD))
		{
			this.resultField = new JPasswordField("");
			((JPasswordField) this.resultField).setText(this.p.result);
			((JPasswordField) this.resultField).getDocument().addDocumentListener(this);
		}
		else if(this.p.type == (Parameter.SCRIPT))
		{
			// Add the text editor area
			this.resultField = new JTextArea("", 15, 80);
			((JTextArea) this.resultField).setLineWrap(true);
			((JTextArea) this.resultField).setWrapStyleWord(true);
			((JTextArea) this.resultField).setText(this.p.result);
			this.scriptScroll = new JScrollPane((JTextArea) this.resultField);
			this.scriptScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			this.scriptScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			this.scriptScroll.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		}
		else
		{
			this.resultField = new JTextField("");
			((JTextField) this.resultField).setText(this.p.result);
			((JTextField) this.resultField).getDocument().addDocumentListener(this);
		}
		if(this.p.result != null && this.p.type != Parameter.SCRIPT)
		{
			this.resultField.setToolTipText(this.p.getValue());
		}
		
		this.panel.add(this.titleField, "wmin 10");
		if(this.p.type == Parameter.FILECHOOSER)
		{
			JPanel temp = new JPanel();
			temp.setLayout(new MigLayout("flowx, ins 0", "[fill,grow][fill]", "[fill,grow]"));
			temp.add(this.resultField, "growx, wmin 10");
			temp.add(this.fileButton, "grow,width 40:40:40, height 10:20:");
			this.panel.add(temp, "growx");
		}
		else if(this.p.type == Parameter.SCRIPT)
		{
			JPanel temp = new JPanel();
			temp.setLayout(new MigLayout("flowx, ins 0", "[fill,grow][fill]", "[fill,grow]"));
			temp.add(this.scriptButton, "growx, wmin 10, height 10:20:");
			this.panel.add(temp, "growx");
		}
		else
		{
			this.panel.add(this.resultField, "growx, wmin 10, height 10:20:");
		}
		
	}
	
	/**
	 * Return the set value in the result Field
	 * 
	 * @return String value of the component
	 */
	public String getValue()
	{
		String result;
		if(this.p.type == (Parameter.DROPDOWN) && this.p.options != null)
		{
			result = ((JComboBox<?>) this.resultField).getSelectedItem().toString();
		}
		else if(this.p.type == (Parameter.CHECKBOX))
		{
			result = "" + ((JCheckBox) this.resultField).isSelected();
		}
		else if(this.p.type == (Parameter.SCRIPT))
		{
			result = "" + ((JTextArea) this.resultField).getText();
		}
		else
			// works for both plain text field and file chooser text field
		{
			result = ((JTextField) this.resultField).getText();
		}
		
		return result;
	}
	
	/**
	 * Set the displayed value of the parameter
	 * 
	 * @param paramValue
	 */
	public void setValue(String paramValue)
	{
		this.p.setValue(paramValue);
		((JTextField) this.resultField).setText(paramValue);
	}
	
	/**
	 * Return the parameter displayed in this panel
	 * 
	 * @return
	 */
	public Parameter getParameter()
	{
		return this.p;
	}
	
	public void validate()
	{
		this.p.setValue(this.getValue());
	}
	
	@Override
	public void changedUpdate(DocumentEvent arg0)
	{
		String result = this.getValue();
		String old = this.p.getValue();
		this.p.setValue(result);
		if(this.p.type == Parameter.PASSWORD)
		{
			return;
		}
		this.resultField.setToolTipText(this.p.getValue());
		Logs.log("Changed parameter from " + old + " to " + this.p.getValue(), 0, this);
	}
	
	@Override
	public void insertUpdate(DocumentEvent arg0)
	{
		String result = this.getValue();
		String old = this.p.getValue();
		this.p.setValue(result);
		if(this.p.type == Parameter.PASSWORD)
		{
			return;
		}
		this.resultField.setToolTipText(this.p.getValue());
		Logs.log("Changed parameter from " + old + " to " + this.p.getValue(), 0, this);
	}
	
	@Override
	public void removeUpdate(DocumentEvent arg0)
	{
		String result = this.getValue();
		String old = this.p.getValue();
		this.p.setValue(result);
		if(this.p.type == Parameter.PASSWORD)
		{
			return;
		}
		this.resultField.setToolTipText(this.p.getValue());
		Logs.log("Changed parameter from " + old + " to " + this.p.getValue(), 0, this);
	}
	
	@Override
	public void actionPerformed(ActionEvent action)
	{
		if(action.getSource() == this.fileButton)
		{
			// Choose a file/directory
			String path = JEXDialog.fileChooseDialog(false);
				
			if(path != null)
			{
				String old = this.p.getValue();
				((JTextField) this.resultField).setText(path);
				
				this.p.setValue(this.getValue());
				if(this.p.type == Parameter.PASSWORD)
				{
					return;
				}
				this.resultField.setToolTipText(this.p.getValue());
				Logs.log("Changed parameter from " + old + " to " + this.p.getValue(), 0, this);
			}
			else
			{
				Logs.log("File chooser dialog canceled", 0, this);
			}			
		}
		else if(action.getSource() == this.scriptButton)
		{
			this.editor = new JDialog();
			this.editor.setModal(true);
			this.editor.setLayout(new BorderLayout());
			this.editor.setBackground(DisplayStatics.background);
			this.editor.add(this.scriptScroll, BorderLayout.CENTER);
			
			// Add the buttons
			JPanel temp = new JPanel();
			temp.setLayout(new MigLayout("flowx, ins 0", "[fill,grow][fill,grow]", "[fill,grow]"));
			temp.add(this.cancelButton, "growx 50, wmin 10, height 10:20:");
			temp.add(this.applyButton, "growx 50, wmin 10, height 10:20:");
			this.panel.add(temp, "growx");
			this.editor.add(temp, BorderLayout.SOUTH);
			this.editor.setUndecorated(false);
			this.editor.setBounds(400, 300, 450, 300);
			this.editor.setVisible(true);
			this.editor.repaint();
		}
		else if(action.getSource() == this.applyButton)
		{
			this.p.setValue(((JTextArea) this.resultField).getText());
			Logs.log("Changed parameter to...\n\n" + this.p.getValue(), 0, this);
			this.editor.dispose();
		}
		else if(action.getSource() == this.cancelButton)
		{
			Logs.log("Changes canceled. Restored script to...\n\n" + this.p.getValue(), 0, this);
			this.editor.dispose();
		}
		else
		{
			String result = this.getValue();
			String old = this.p.getValue();
			this.p.setValue(result);
			if(this.p.type == Parameter.PASSWORD)
			{
				return;
			}
			this.resultField.setToolTipText(this.p.getValue());
			Logs.log("Changed parameter from " + old + " to " + this.p.getValue(), 0, this);
		}
	}
	
	@Override
	public void stateChanged(ChangeEvent arg0)
	{
		String result = this.getValue();
		String old = this.p.getValue();
		this.p.setValue(result);
		if(this.p.type == Parameter.PASSWORD)
		{
			return;
		}
		this.resultField.setToolTipText(this.p.getValue());
		Logs.log("Changed parameter from " + old + " to " + this.p.getValue(), 0, this);
	}
	
}
