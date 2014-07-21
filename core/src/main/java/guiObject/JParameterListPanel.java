package guiObject;

import Database.Definition.Parameter;
import Database.Definition.ParameterSet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import jex.statics.DisplayStatics;
import logs.Logs;
import net.miginfocom.swing.MigLayout;

public class JParameterListPanel {
	
	// Results
	public boolean isValidated = false;
	
	// Model variables
	ParameterSet parameters;
	
	// GUI objects
	private JPanel panel;
	private List<JParameterPanel> parameterPanels;
	private Color backgroundColor = DisplayStatics.lightBackground;
	private JPanel centerPanel;
	private JScrollPane scroll;
	private List<ActionListener> listeners;
	
	/**
	 * Create a new empty JParameterPanel
	 */
	public JParameterListPanel()
	{
		this.parameters = new ParameterSet();
		this.listeners = new ArrayList<ActionListener>(0);
		this.initialize();
	}
	
	public JParameterListPanel(ParameterSet parameters)
	{
		this.parameters = parameters;
		this.listeners = new ArrayList<ActionListener>(0);
		this.initialize();
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	/**
	 * Initialize the form
	 */
	public void initialize()
	{
		// layout of this form
		this.panel = new JPanel();
		this.panel.setLayout(new BorderLayout());
		
		this.centerPanel = new JPanel();
		this.centerPanel.setBackground(this.backgroundColor);
		this.centerPanel.setLayout(new MigLayout("flowy, ins 3", "[fill,grow]", "[]2"));
		this.scroll = new JScrollPane(this.centerPanel);
		this.scroll.setBorder(BorderFactory.createEmptyBorder());
		this.scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		this.scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		this.panel.add(this.scroll);
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
	public void addParameter(String title, String note, int type, String[] options)
	{
		Parameter p = new Parameter(title, note, type, options, 0);
		this.addParameter(p);
	}
	
	/**
	 * Add an entry for parameter P
	 * 
	 * @param p
	 */
	public void addParameter(Parameter p)
	{
		this.parameters.addParameter(p);
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
		this.centerPanel.removeAll();
		
		Collection<Parameter> params = this.parameters.getParameters();
		this.parameterPanels = new ArrayList<JParameterPanel>(0);
		for (Parameter fe : params)
		{
			JParameterPanel ppanel = new JParameterPanel(fe);
			ppanel.panel().setBackground(this.backgroundColor);
			this.parameterPanels.add(ppanel);
			this.centerPanel.add(ppanel.panel(), "growx");
		}
		this.panel.revalidate();
	}
	
	/**
	 * Return the value of entry line titled NAME
	 * 
	 * @param name
	 * @return the value for key NAME
	 */
	public String getValue(String name)
	{
		for (JParameterPanel fep : this.parameterPanels)
		{
			if(fep.p.title.equals(name))
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
		this.listeners.add(listener);
	}
	
	/**
	 * Save the parameters
	 */
	public void saveParameters()
	{
		Logs.log("Saving the parameter set...", 1, this);
		for (JParameterPanel fep : this.parameterPanels)
		{
			String name = fep.p.title;
			Parameter p = this.parameters.getParameter(name);
			String result = fep.getValue();
			p.setValue(result);
		}
	}
	
}
