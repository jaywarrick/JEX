package function;

import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import guiObject.JParameterPanel;
import guiObject.JRoundedCollapsablePanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;

public class GraphicalFunctionWrap extends JDialog implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	// Working variables
	HashMap<String,Parameter> parameters;
	HashMap<Integer,String[]> parametersForSteps;
	HashMap<Integer,String> labelForStep;
	GraphicalCrunchingEnabling function;
	boolean isValid = false;
	
	// button looping panel
	boolean displayLoopPanel = false;
	boolean autoReduce = true;
	JPanel buttonPane = new JPanel();
	JButton prevButton = new JButton("Previous");
	JButton nextButton = new JButton("Next");
	JButton thisButton = new JButton("Recalculate");
	
	// GUI variables
	HashMap<Integer,JLabel> jlabelForStep;
	HashMap<Integer,JButton> buttonForStep;
	HashMap<Integer,JParameterPanel[]> paramPanelForStep;
	JButton validateButton;
	JPanel contentPane;
	JPanel controlPane;
	JPanel centerPane;
	Color background = DisplayStatics.background;
	
	// Center panel
	// ImagePanel containedPane ;
	JComponent containedPane;
	
	public GraphicalFunctionWrap(GraphicalCrunchingEnabling function, HashMap<String,Parameter> parameters)
	{
		super(JEXStatics.main, true);
		
		this.function = function;
		this.parameters = parameters;
		parametersForSteps = new HashMap<Integer,String[]>();
		labelForStep = new HashMap<Integer,String>();
		
		jlabelForStep = new HashMap<Integer,JLabel>();
		buttonForStep = new HashMap<Integer,JButton>();
		paramPanelForStep = new HashMap<Integer,JParameterPanel[]>();
	}
	
	public GraphicalFunctionWrap(GraphicalCrunchingEnabling function, ParameterSet params)
	{
		super(JEXStatics.main, true);
		
		this.parameters = new HashMap<String,Parameter>();
		for (Parameter p : params.getParameters())
		{
			this.parameters.put(p.title, p);
		}
		this.function = function;
		parametersForSteps = new HashMap<Integer,String[]>();
		labelForStep = new HashMap<Integer,String>();
		
		jlabelForStep = new HashMap<Integer,JLabel>();
		buttonForStep = new HashMap<Integer,JButton>();
		paramPanelForStep = new HashMap<Integer,JParameterPanel[]>();
	}
	
	/**
	 * Add a step to the function give the step a naming description and a list of parameters it requires
	 * 
	 * @param index
	 * @param name
	 * @param parameters
	 */
	public void addStep(int index, String name, String[] parameters)
	{
		labelForStep.put(new Integer(index), name);
		parametersForSteps.put(new Integer(index), parameters);
	}
	
	/**
	 * Set up the necessary objects
	 */
	public void initialize()
	{
		Logs.log("Initializing the graphics", 1, this);
		validateButton = new JButton("Done !");
		validateButton.setMaximumSize(new Dimension(100, 20));
		validateButton.setPreferredSize(new Dimension(100, 20));
		validateButton.addActionListener(this);
		
		controlPane = new JPanel();
		controlPane.setBackground(background);
		controlPane.setLayout(new BoxLayout(controlPane, BoxLayout.PAGE_AXIS));
		controlPane.setMaximumSize(new Dimension(200, 800));
		controlPane.setPreferredSize(new Dimension(200, 400));
		
		Set<Integer> keys = labelForStep.keySet();
		for (Integer key : keys)
		{
			JButton stepButton = new JButton("Next");
			stepButton.setMaximumSize(new Dimension(100, 20));
			stepButton.setPreferredSize(new Dimension(100, 20));
			stepButton.addActionListener(this);
			buttonForStep.put(key, stepButton);
			
			JLabel label = new JLabel(labelForStep.get(key));
			label.setMaximumSize(new Dimension(100, 20));
			label.setPreferredSize(new Dimension(100, 20));
			jlabelForStep.put(key, label);
			
			String[] paramNames = parametersForSteps.get(key);
			JParameterPanel[] panels = new JParameterPanel[paramNames.length];
			for (int i = 0, len = paramNames.length; i < len; i++)
			{
				Parameter p = parameters.get(paramNames[i]);
				if(p == null)
					continue;
				panels[i] = new JParameterPanel(p);
			}
			paramPanelForStep.put(key, panels);
		}
		
		buttonPane.setBackground(DisplayStatics.lightBackground);
		buttonPane.setLayout(new FlowLayout());
		buttonPane.setMaximumSize(new Dimension(1000, 30));
		buttonPane.setPreferredSize(new Dimension(400, 30));
		prevButton.setMaximumSize(new Dimension(80, 20));
		prevButton.setPreferredSize(new Dimension(80, 20));
		prevButton.addActionListener(this);
		nextButton.setMaximumSize(new Dimension(80, 20));
		nextButton.setPreferredSize(new Dimension(80, 20));
		nextButton.addActionListener(this);
		thisButton.setMaximumSize(new Dimension(80, 20));
		thisButton.setPreferredSize(new Dimension(80, 20));
		thisButton.addActionListener(this);
		buttonPane.add(prevButton);
		buttonPane.add(thisButton);
		buttonPane.add(nextButton);
		
		centerPane = new JPanel();
		centerPane.setBackground(background);
		centerPane.setLayout(new BorderLayout());
		centerPane.add(containedPane, BorderLayout.CENTER);
	}
	
	// /**
	// * Place panel in central panel of function maker
	// * @param pane
	// */
	// public void setInCentralPanel(ImagePanel pane){
	// containedPane = pane;
	// }
	
	/**
	 * Place panel in central panel of function maker
	 * 
	 * @param pane
	 */
	public void setInCentralPanel(JComponent pane)
	{
		containedPane = pane;
	}
	
	/**
	 * Set the display of the button panel
	 * 
	 * @param b
	 */
	public void setDisplayLoopPanel(boolean b)
	{
		this.displayLoopPanel = b;
	}
	
	public void setAutoReduce(boolean b)
	{
		this.autoReduce = b;
	}
	
	/**
	 * Refresh and rebuild the function maker
	 */
	public void refresh()
	{
		containedPane.updateUI();
		containedPane.repaint();
		this.invalidate();
		this.validate();
	}
	
	/**
	 * Display options until step index
	 * 
	 * @param index
	 */
	public void displayUntilStep()
	{
		controlPane.removeAll();
		// int displayUntil = (currentStep >= labelForStep.size()) ?
		// labelForStep.size()-1 : currentStep;
		
		int step = function.getStep();
		Logs.log("Displaying until step " + step, 1, this);
		for (int i = 0; i <= step; i++)
		{
			JLabel label = jlabelForStep.get(new Integer(i));
			JButton button = buttonForStep.get(new Integer(i));
			JParameterPanel[] panels = paramPanelForStep.get(new Integer(i));
			
			StepPanel paramPanel = new StepPanel(label.getText(), panels, button);
			if(i < step && autoReduce)
				paramPanel.reduce();
			controlPane.add(paramPanel);
			
			controlPane.add(Box.createVerticalStrut(5));
		}
		
		controlPane.add(Box.createVerticalStrut(5));
		int nbsteps = labelForStep.size();
		if(step >= nbsteps - 1)
		{
			controlPane.add(validateButton);
		}
		
		controlPane.add(Box.createVerticalGlue());
		
		controlPane.invalidate();
		controlPane.validate();
		controlPane.repaint();
	}
	
	/**
	 * Start the display
	 * 
	 * @return true if finished by a validation
	 */
	public boolean start()
	{
		this.setTitle("Automated JEX algorithm GUI");
		this.setBounds(100, 100, 950, 650);
		this.setBackground(background);
		
		initialize();
		function.startIT();
		
		JPanel contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(centerPane, BorderLayout.CENTER);
		contentPane.add(controlPane, BorderLayout.LINE_END);
		if(this.displayLoopPanel)
			contentPane.add(buttonPane, BorderLayout.PAGE_END);
		
		Logs.log("Starting the modal interface", 1, this);
		this.setModal(true);
		this.setVisible(true);
		
		return isValid;
	}
	
	public void validateParameters()
	{
		Set<Integer> indeces = paramPanelForStep.keySet();
		for (Integer index : indeces)
		{
			JParameterPanel[] panels = paramPanelForStep.get(index);
			if(panels == null)
				continue;
			for (JParameterPanel panel : panels)
			{
				if(panel == null)
					continue;
				panel.validate();
			}
		}
	}
	
	/**
	 * Set the parameter named paramName to the value paramValue
	 * 
	 * @param paramName
	 * @param paramValue
	 */
	public void setParameter(String paramName, String paramValue)
	{
		Set<Integer> indeces = paramPanelForStep.keySet();
		for (Integer index : indeces)
		{
			JParameterPanel[] panels = paramPanelForStep.get(index);
			if(panels == null)
				continue;
			for (JParameterPanel panel : panels)
			{
				if(panel == null)
					continue;
				
				Parameter p = panel.getParameter();
				if(p.getTitle().equals(paramName))
				{
					panel.setValue(paramValue);
				}
			}
		}
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == validateButton)
		{
			Logs.log("Validating the function... end of visual processing", 1, this);
			function.finishIT();
			isValid = true;
			this.dispose();
			// containedPane.paint(containedPane.getGraphics());
			// this.refresh();
		}
		else if(e.getSource() == nextButton)
		{
			function.loopNext();
		}
		else if(e.getSource() == prevButton)
		{
			function.loopPrevious();
		}
		else if(e.getSource() == thisButton)
		{
			function.recalculate();
		}
		else
		{
			Set<Integer> keys = buttonForStep.keySet();
			for (Integer key : keys)
			{
				if(e.getSource() == buttonForStep.get(key))
				{
					Logs.log("Finished step " + key + " going to next step", 1, this);
					validateParameters();
					function.runStep(key);
					function.runNext();
					displayUntilStep();
				}
			}
		}
		
	}
	
	class StepPanel extends JRoundedCollapsablePanel implements ActionListener {
		
		private static final long serialVersionUID = 1L;
		
		JParameterPanel[] parameters;
		JButton doneButton;
		String label;
		
		StepPanel(String label, JParameterPanel[] parameters, JButton button)
		{
			super();
			this.parameters = parameters;
			this.label = label;
			this.setPanelTitle(label);
			
			doneButton = button;
			doneButton.setMaximumSize(new Dimension(100, 20));
			doneButton.setPreferredSize(new Dimension(100, 20));
			
			rebuild();
		}
		
		private void rebuild()
		{
			this.clear();
			
			if(parameters != null)
			{
				for (JParameterPanel panel : parameters)
				{
					if(panel == null)
						continue;
					panel.panel().setBackground(background);
					this.addComponent(panel.panel());
				}
			}
			// this.add(Box.createVerticalStrut(10));
			this.addComponent(doneButton);
			
			this.updateUI();
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			super.actionPerformed(e);
		}
	}
	
}
