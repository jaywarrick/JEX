package jex.jexTabPanel.jexFunctionPanel;

import Database.DBObjects.JEXEntry;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.tnvi;
import cruncher.JEXFunction;
import function.JEXCrunchable;
import guiObject.FlatRoundedButton;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import transferables.TransferableTypeName;

public class FunctionBlockPanel implements ActionListener, MouseListener {
	
	// GUI
	protected Color foregroundColor = DisplayStatics.lightBackground;
	JPanel panel = new JPanel();
	JScrollPane scroll = new JScrollPane();
	JPanel centerPane = new JPanel();
	JPanel inputList = new JPanel();
	JPanel outputList = new JPanel();
	JPanel titlePane = new JPanel();
	FlatRoundedButton upOneButton = new FlatRoundedButton("<-");
	FlatRoundedButton downOneButton = new FlatRoundedButton("->");
	FlatRoundedButton deleteButton = new FlatRoundedButton("X");
	JLabel functionName = new JLabel();
	JPanel runPane = new JPanel();
	JButton runButton = new JButton();
	// JButton testButton = new JButton() ;
	
	// variables
	protected JEXFunction function;
	protected ParameterSet parameters;
	private JEXFunctionPanel parent;
	
	// Function variables
	TreeMap<String,FunctionInputDrop> inputPanes;
	TreeMap<Integer,FunctionOutputDrag> outputPanes;
	JEXCrunchable crunch;
	
	public FunctionBlockPanel(JEXFunctionPanel parent)
	{
		this.parent = parent;
		
		this.makeTitlePane();
		this.initializeSingleFunctionPanel();
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	private void makeTitlePane()
	{
		// Prepare the name label
		String funName = (this.function == null) ? "NONE" : this.function.getFunctionName();
		funName = funName.substring(0, Math.min(funName.length(), 14));
		this.functionName.setText(funName);
		this.functionName.setMinimumSize(new Dimension(10, 25));
		this.functionName.setFont(FontUtility.boldFont);
		this.functionName.setMaximumSize(new Dimension(1000, 25));
		this.functionName.addMouseListener(this);
		
		// Prepare the left button
		// upOneButton.setText("<");
		this.upOneButton.addActionListener(this);
		// upOneButton.setPreferredSize(new Dimension(25,25));
		// upOneButton.setMaximumSize(new Dimension(25,25));
		
		// Prepare the right button
		// downOneButton.setText(">");
		this.downOneButton.addActionListener(this);
		// downOneButton.setPreferredSize(new Dimension(25,25));
		// downOneButton.setMaximumSize(new Dimension(25,25));
		
		// Prepare the right button
		// deleteButton.setText("x");
		this.deleteButton.addActionListener(this);
		// deleteButton.setPreferredSize(new Dimension(25,25));
		// deleteButton.setMaximumSize(new Dimension(25,25));
		
		// Prepare the title pane container
		this.titlePane.removeAll();
		this.titlePane.setBackground(this.foregroundColor);
		this.titlePane.setLayout(new MigLayout("center,flowx, ins 0", "[]2[center,fill,grow]0[]0[]", "[]"));
		
		// Add the objects inside
		this.titlePane.add(this.upOneButton.panel());
		this.titlePane.add(this.functionName, "growx");
		this.titlePane.add(this.deleteButton.panel());
		this.titlePane.add(this.downOneButton.panel());
		
		// Make the run buttons
		// testButton.setText("TEST");
		// testButton.addActionListener(this);
		this.runButton.setText("RUN");
		this.runButton.addActionListener(this);
		
		// Make the run panel
		this.runPane.setBackground(this.foregroundColor);
		this.runPane.setLayout(new MigLayout("flowx,ins 0", "[fill,grow]", "[]"));
		// runPane.add(testButton,"growx, width 25:60:");
		// runPane.add(runButton,"growx, width 25:60:");
		this.runPane.add(this.runButton, "growx");
	}
	
	private void initializeSingleFunctionPanel()
	{
		
		this.inputList.setBackground(this.foregroundColor);
		this.inputList.setLayout(new MigLayout("flowy, ins 0", "[fill,grow]", "[]2"));
		// inputList.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		this.outputList.setBackground(this.foregroundColor);
		this.outputList.setLayout(new MigLayout("flowy, ins 0", "[fill,grow]", "[]2"));
		// outputList.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		
		this.centerPane.setBackground(this.foregroundColor);
		this.centerPane.setLayout(new MigLayout("flowy, ins 0, gapy 3", "[left,fill,grow]", "[]"));
		this.centerPane.add(this.inputList, "growx,width 50:100:");
		this.centerPane.add(this.outputList, "growx,width 50:100:");
		
		this.scroll = new JScrollPane(this.centerPane);
		this.scroll.setBorder(BorderFactory.createEmptyBorder());
		this.scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		this.scroll.addMouseWheelListener(this.parent.centerTopHalf);
		this.scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		this.panel = new JPanel();
		this.panel.setLayout(new MigLayout("flowy,ins 3", "[fill,grow]", "[]0[fill,grow,]0[]"));
		this.panel.setBackground(this.foregroundColor);
		this.panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		this.panel.add(this.titlePane, "growx,width 50:100:");
		this.panel.add(this.scroll);
		this.panel.add(this.runPane, "growx,width 50:100:");
	}
	
	public void setFunction(JEXFunction function)
	{
		this.function = function;
		
		JEXCrunchable crunch = null;
		if(function != null)
		{
			crunch = function.getCrunch();
		}
		this.inputPanes = new TreeMap<String,FunctionInputDrop>();
		this.outputPanes = new TreeMap<Integer,FunctionOutputDrag>();
		this.inputList.removeAll();
		this.outputList.removeAll();
		
		// Creating the input drop panels
		int nbInput = 0;
		TypeName[] inNames = null;
		if(crunch != null)
		{
			nbInput = crunch.getInputNames().length;
			inNames = crunch.getInputNames();
		}
		JLabel inputLabel = new JLabel("Inputs:");
		// inputLabel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		this.inputList.add(inputLabel, "growx");
		// inputList.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		for (int i = 0; i < nbInput; i++)
		{
			FunctionInputDrop ind = new FunctionInputDrop(this, i, inNames[i]);
			// ind.setAlignmentX(JPanel.LEFT_ALIGNMENT);
			this.inputList.add(ind, "growx");
			this.inputPanes.put(inNames[i].getName(), ind);
		}
		Logs.log("Created " + nbInput + " input drop panels", 1, this);
		
		// Create the output drag panels
		int nbOutput = 0;
		if(crunch != null)
		{
			nbOutput = crunch.getOutputs().length;
		}
		JLabel outputLabel = new JLabel("Outputs:");
		// outputLabel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		this.outputList.add(outputLabel, "growx");
		// outputList.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		for (int i = 0; i < nbOutput; i++)
		{
			FunctionOutputDrag oud = new FunctionOutputDrag(i, function.getSavingSelections().get(i), function.getExpectedOutputTN(i));
			// oud.setAlignmentX(JPanel.LEFT_ALIGNMENT);
			this.outputList.add(oud, "growx");
			this.outputPanes.put(new Integer(i), oud);
		}
		Logs.log("Created " + nbOutput + " output drag panels", 1, this);
		
		// Prepare the name label
		this.functionName.setText(function.getFunctionName());
		this.functionName.setFont(FontUtility.boldFont);
		
		this.centerPane.revalidate();
		this.centerPane.repaint();
	}
	
	/**
	 * An input has been droped... check the new set of inputs for validity
	 */
	public void inputsChanged()
	{
		Logs.log("Inputs changed... testing function", 1, this);
		
		// Get output names before test function
		Set<Integer> intKeys = this.outputPanes.keySet();
		for (Integer index : intKeys)
		{
			FunctionOutputDrag oud = this.outputPanes.get(index);
			String outputName = oud.getOutputName();
			this.function.setExpectedOutputName(index, outputName);
		}
	}
	
	/**
	 * Set input of name NAME with typename INPUTTN
	 * 
	 * @param name
	 * @param inputTN
	 */
	public void setInput(String inputName, TypeName inputTN)
	{
		this.function.setInput(inputName, inputTN);
		this.inputsChanged();
	}
	
	/**
	 * Set the list of given output names for this function
	 */
	public void setListOfOutputNames()
	{
		Set<Integer> outputIndeces = this.outputPanes.keySet();
		for (Integer index : outputIndeces)
		{
			FunctionOutputDrag outPane = this.outputPanes.get(index);
			this.function.setExpectedOutputName(index, outPane.getOutputName());
		}
		return;
	}
	
	/**
	 * Set the list of given output names for this function
	 */
	public void setSavingSelections()
	{
		this.function.setSavingSelections(this.getSavingSelections());
	}
	
	/**
	 * Test the function and set the status to green or red
	 */
	public void testFunction()
	{
		// Set the cruncher
		this.crunch = this.function.getCrunch();
		this.crunch.setInputs(this.function.getInputs());
		
		// Loop through the inputs to see if they exist in the database or if
		// another function
		// has an output with that typeName
		for (String inputName : this.function.getInputs().keySet())
		{
			// Get the inputTN
			TypeName inputTN = this.function.getInputs().get(inputName);
			
			// Check if the object is in the database
			if(this.isDataInDatabase(inputTN))
			{
				FunctionInputDrop in = this.inputPanes.get(inputName);
				in.setInputTN(inputTN);
			}
			
			// Else check if the object is the output of a previous object
			if(inputTN != null && this.isDataOutputOfFunction(inputTN))
			{
				FunctionInputDrop in = this.inputPanes.get(inputName);
				in.setInputTN(inputTN);
			}
		}
		
		// Set the outputs
		boolean canRun = (this.crunch.checkInputs() == JEXCrunchable.INPUTSOK);
		for (FunctionOutputDrag outPane : this.outputPanes.values())
		{
			outPane.setCanRun(canRun);
		}
	}
	
	/**
	 * Return true if the object exists in the database
	 * 
	 * @param tn
	 * @return
	 */
	private boolean isDataInDatabase(TypeName tn)
	{
		if(tn == null)
		{
			return false;
		}
		
		tnvi TNVI = JEXStatics.jexManager.getTNVI();
		
		TreeMap<String,TreeMap<String,Set<JEXEntry>>> nvi = TNVI.get(tn.getType());
		if(nvi == null)
		{
			return false;
		}
		
		TreeMap<String,Set<JEXEntry>> vi = nvi.get(tn.getName());
		if(vi == null)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Return true if the object is the output of another function of the function list
	 * 
	 * @param tn
	 * @return
	 */
	private boolean isDataOutputOfFunction(TypeName tn)
	{
		return this.parent.isDataOutputOfFunction(tn);
	}
	
	/**
	 * Return the function in this functionblockpanel
	 * 
	 * @return
	 */
	public JEXFunction getFunction()
	{
		this.setListOfOutputNames();
		this.setSavingSelections();
		return this.function;
	}
	
	public TreeMap<Integer,Boolean> getSavingSelections()
	{
		TreeMap<Integer,Boolean> selections = new TreeMap<Integer,Boolean>();
		for(Integer pane : this.outputPanes.keySet())
		{
			selections.put(pane, this.outputPanes.get(pane).getSavingSelection());
		}
		return selections;
	}
	
	// ----------------------------------------------------
	// --------- INPUT DROP BOX ---------------------------
	// ----------------------------------------------------
	class FunctionInputDrop extends JPanel {
		
		private static final long serialVersionUID = 1L;
		
		TypeName inputTN;
		int index;
		TypeName inname;
		FunctionBlockPanel parent;
		
		JPanel dropArea = new JPanel();
		JLabel inputTNLabel = new JLabel();
		JPanel box;
		
		FunctionInputDrop(FunctionBlockPanel parent, int index, TypeName inname)
		{
			this.parent = parent;
			this.index = index;
			this.inname = inname;
			new InputButtonListener(this);
			this.initialize();
		}
		
		private void initialize()
		{
			this.setLayout(new MigLayout("flowx, ins 0", "[center]2[center]2[fill,grow]", "[center]"));
			this.setBackground(FunctionBlockPanel.this.foregroundColor);
			// this.setMinimumSize(new Dimension(400,20));
			// this.setMaximumSize(new Dimension(200,20));
			
			this.inputTNLabel.setFont(FontUtility.italicFonts);
			this.inputTNLabel.setText(this.inname.toString());
			this.box = new JPanel() {
				
				private static final long serialVersionUID = 1L;
				
				@Override
				protected void paintComponent(Graphics g)
				{
					int x = 0;
					int y = 0;
					int w = this.getWidth() - 1;
					int h = this.getHeight() - 1;
					
					Graphics2D g2 = (Graphics2D) g.create();
					// g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					// RenderingHints.VALUE_ANTIALIAS_ON);
					
					Color c;
					if(FunctionInputDrop.this.inputTN != null)
					{
						c = Color.green;
					}
					else
					{
						c = Color.red;
					}
					g2.setColor(c);
					g2.fillRect(x, y, w, h);
					
					g2.setStroke(new BasicStroke(1f));
					g2.setColor(Color.black);
					g2.drawRect(x, y, w, h);
					
					g2.dispose();
				}
			};
			// box.setPreferredSize(new Dimension(20,20));
			// box.setMaximumSize(new Dimension(20,20));
			// box.setMinimumSize(new Dimension(20,20));
			this.box.setBorder(BorderFactory.createLineBorder(Color.black));
			this.box.setBackground(Color.RED);
			this.box.setToolTipText(this.inname.getName());
			this.add(this.box, "width 20:20:20, height 20:20:20");
			this.add(Box.createHorizontalStrut(5));
			this.add(this.inputTNLabel, "growx");
		}
		
		private void rebuild()
		{
			if(this.inputTN != null)
			{
				this.box.setBackground(Color.GREEN);
				this.inputTNLabel.setText(this.inputTN.getType() + ": " + this.inputTN.getName());
			}
			else
			{
				this.box.setBackground(Color.RED);
				this.inputTNLabel.setText("Set input: " + this.inname);
			}
			this.repaint();
		}
		
		public void setInputTN(TypeName inputTN)
		{
			Logs.log("Set typename of the input drop...", 1, this);
			this.inputTN = inputTN;
			this.parent.setInput(this.inname.getName(), inputTN);
			
			this.rebuild();
		}
		
		public void setIsCrunchable(boolean b)
		{
			this.rebuild();
		}
	}
	
	// ----------------------------------------------------
	// --------- INPUT LISTENER ---------------------------
	// ----------------------------------------------------
	class InputButtonListener extends DropTargetAdapter {
		
		private DropTarget dropTarget;
		private FunctionInputDrop button;
		
		public InputButtonListener(FunctionInputDrop button)
		{
			this.button = button;
			
			this.dropTarget = new DropTarget(button, DnDConstants.ACTION_COPY, this, true, null);
			Logs.log("Drop target constructed ..." + this.dropTarget, 1, this);
		}
		
		@Override
		public void drop(DropTargetDropEvent event)
		{
			
			try
			{
				if(event.isDataFlavorSupported(TransferableTypeName.jexDataFlavor))
				{
					Transferable tr = event.getTransferable();
					
					if(tr.isDataFlavorSupported(TransferableTypeName.jexDataFlavor))
					{
						TypeName name = (TypeName) tr.getTransferData(TransferableTypeName.jexDataFlavor);
						Logs.log("Passing a typeName...", 1, this);
						this.button.setInputTN(name);
					}
					
					event.acceptDrop(DnDConstants.ACTION_COPY);
					event.dropComplete(true);
					Logs.log("Drop completed...", 1, this);
					
					return;
				}
				event.rejectDrop();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				event.rejectDrop();
			}
		}
	}
	
	// ----------------------------------------------------
	// --------- OUTPUT BOX -------------------------------
	// ----------------------------------------------------
	class FunctionOutputDrag extends JPanel implements DragGestureListener, DocumentListener {
		
		private static final long serialVersionUID = 1L;
		
		TypeName tn;
		int index;
		boolean canRun = false;
		
		JPanel dragArea = new JPanel();
		JTextField outputTNLabel = new JTextField();
		JCheckBox saveOutput = new JCheckBox();
		JPanel box;
		
		FunctionOutputDrag(int index, boolean savingSelection, TypeName tn)
		{
			this.tn = tn;
			this.index = index;
			this.saveOutput.setSelected(savingSelection);
			this.initialize();
		}
		
		private void initialize()
		{
			this.setLayout(new MigLayout("flowx, ins 0", "[center]2[center]2[center]2[fill,grow]", "[center]"));
			this.setBackground(FunctionBlockPanel.this.foregroundColor);
			// this.setMinimumSize(new Dimension(400,20));
			// this.setMaximumSize(new Dimension(200,20));
			
			this.outputTNLabel.setFont(FontUtility.italicFonts);
			this.outputTNLabel.getDocument().addDocumentListener(this);
			this.box = new JPanel() {
				
				private static final long serialVersionUID = 1L;
				
				@Override
				protected void paintComponent(Graphics g)
				{
					int x = 0;
					int y = 0;
					int w = this.getWidth() - 1;
					int h = this.getHeight() - 1;
					
					Graphics2D g2 = (Graphics2D) g.create();
					// g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					// RenderingHints.VALUE_ANTIALIAS_ON);
					
					Color c;
					if(FunctionOutputDrag.this.canRun)
					{
						c = Color.green;
					}
					else
					{
						c = Color.red;
					}
					g2.setColor(c);
					g2.fillRect(x, y, w, h);
					
					g2.setStroke(new BasicStroke(1f));
					g2.setColor(Color.black);
					g2.drawRect(x, y, w, h);
					
					g2.dispose();
				}
			};
			// box.setPreferredSize(new Dimension(20,20));
			// box.setMaximumSize(new Dimension(20,20));
			// box.setMinimumSize(new Dimension(20,20));
			this.box.setBorder(BorderFactory.createLineBorder(Color.black));
			this.box.setBackground(Color.RED);
			
			this.saveOutput.setToolTipText("Check this box to set the output to be saved in the database");
			
			// Create the drag source
			this.createDragSource();
			
			if(this.tn == null)
			{
				this.outputTNLabel.setText("Set name");
			}
			else
			{
				this.outputTNLabel.setText(this.tn.getName());
			}
			
			this.add(this.box, "width 20:20:20, height 20:20:20");
			this.add(Box.createHorizontalStrut(5));
			this.add(this.saveOutput);
			this.add(this.outputTNLabel, "growx");
			this.repaint();
		}
		
		public boolean getSavingSelection()
		{
			return this.saveOutput.isSelected();
		}
		
		public void setCanRun(boolean canRun)
		{
			this.canRun = canRun;
			this.repaint();
		}
		
		public String getOutputName()
		{
			return this.outputTNLabel.getText();
		}
		
		/**
		 * Set up a drag source on this object
		 */
		private void createDragSource()
		{
			// create the drag source for the button
			DragSource ds = new DragSource();
			ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, this);
		}
		
		@Override
		public void dragGestureRecognized(DragGestureEvent event)
		{
			Cursor cursor = null;
			if(event.getDragAction() == DnDConstants.ACTION_COPY)
			{
				cursor = DragSource.DefaultCopyDrop;
			}
			event.startDrag(cursor, new TransferableTypeName(this.tn.duplicate()));
		}
		
		@Override
		public void changedUpdate(DocumentEvent arg0)
		{
			this.tn.setName(this.outputTNLabel.getText());
		}
		
		@Override
		public void insertUpdate(DocumentEvent arg0)
		{
			this.tn.setName(this.outputTNLabel.getText());
		}
		
		@Override
		public void removeUpdate(DocumentEvent arg0)
		{
			this.tn.setName(this.outputTNLabel.getText());
		}
	}
	
	// ----------------------------------------------------
	// --------- EVENT HANDLING FUNCTIONS -----------------
	// ----------------------------------------------------
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == this.upOneButton)
		{
			this.parent.upOne(this);
		}
		else if(e.getSource() == this.downOneButton)
		{
			this.parent.downOne(this);
		}
		else if(e.getSource() == this.deleteButton)
		{
			if(this == this.parent.getSelectedFunction())
			{
				this.parent.selectFunction(null);
			}
			this.parent.delete(this);
			
		}
		// else if (e.getSource() == this.testButton)
		// {
		// this.setListOfOutputNames();
		// parent.runOneFunction(function, true);
		// }
		else if(e.getSource() == this.runButton)
		{
			this.setListOfOutputNames();
			this.setSavingSelections();
			this.parent.runOneFunction(this.function, this.parent.isAutoSaveSelected());
		}
	}
	
	@Override
	public void mouseClicked(MouseEvent arg0)
	{}
	
	@Override
	public void mouseEntered(MouseEvent arg0)
	{}
	
	@Override
	public void mouseExited(MouseEvent arg0)
	{}
	
	@Override
	public void mousePressed(MouseEvent arg0)
	{}
	
	@Override
	public void mouseReleased(MouseEvent arg0)
	{
		this.parent.selectFunction(this);
	}
}
