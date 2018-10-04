package jex.jexTabPanel.jexFunctionPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import jex.statics.DisplayStatics;
import jex.statics.JEXDialog;
import logs.Logs;
import net.miginfocom.swing.MigLayout;
import cruncher.JEXFunction;
import function.CrunchFactory;
import function.JEXCrunchable;

public class FunctionListPanel implements MouseWheelListener {
	
	// GUI variables
	private JPanel panel;
	private JEXFunctionPanel parent;
	private JPanel blocksPanel;
	// private FunctionParameterPanel paramPanel;
	private FunctionLoadSaveAndRunPanel editPanel;
	protected JScrollPane scroll;
	private FunctionBlockPanel selectedFunction;
	
	public FunctionListPanel(JEXFunctionPanel parent)
	{
		this.parent = parent;
		this.initialize();
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	private void initialize()
	{
		this.panel = new JPanel();
		this.panel.setBackground(DisplayStatics.lightBackground);
		this.panel.setLayout(new MigLayout("flowx,ins 0 0 0 0", "[0:0,fill,grow]-1[0:0,fill,100]", "[0:0,fill,grow]"));
		
		// Create the add button
		this.editPanel = new FunctionLoadSaveAndRunPanel(this);
		
		// Create the panel to hold the FunctionBlockPanels
		this.blocksPanel = new JPanel();
		this.blocksPanel.setBackground(DisplayStatics.lightBackground);
		this.blocksPanel.setLayout(new MigLayout("left,flowx,ins 5 5 5 5", "[center,0:0,fill,200]5[center,0:0,fill,200]", "[center,0:0,fill,grow]"));
		this.scroll = new JScrollPane(this.blocksPanel);
		this.scroll.setBackground(DisplayStatics.lightBackground);
		this.scroll.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		this.scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		this.scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		
		// Create the parameters panel
		// this.paramPanel = new FunctionParameterPanel();
		
		// Build the panel
		// this.panel.add(editPanel.panel(),"growx,south");
		this.panel.add(this.scroll, "grow, height 100:150:");
		this.panel.add(this.editPanel.panel(), "growx");
		// this.panel.add(paramPanel.panel(),"grow, height 100:150:,gap 0 0 3 3");
		
		this.rebuildList();
	}
	
	public void rebuildList()
	{
		this.blocksPanel.removeAll();
		List<FunctionBlockPanel> functionPanelList = JEXFunctionPanel.functionList;
		
		// New code
		for (FunctionBlockPanel panel : functionPanelList)
		{
			this.blocksPanel.add(panel.panel(), "grow");
		}
		
		this.blocksPanel.revalidate();
		this.blocksPanel.repaint();
	}
	
	public void addFunction()
	{
		Logs.log("Opening functionlist selection panel", 1, this);
		
		@SuppressWarnings("unused")
		FunctionChooserDialog flistpane = new FunctionChooserDialog();
	}
	
	public void saveAllFunctions(boolean autoSave)
	{
		this.parent.saveAllFunctions(autoSave);
	}
	
	public void runAllFunctions(boolean autoSave, boolean autoUpdate)
	{
		this.parent.runAllFunctions(autoSave, autoUpdate);
	}
	
	public void loadFunctionList()
	{
		// Creating file chooser to open user preferences
		String path = JEXDialog.fileChooseDialog(false);
		
		if(path != null)
		{
			// Save the function list
			this.parent.loadWorkflowFile(new File(path));
		}
		else
		{
			Logs.log("Cannot load the file", 0, this);
		}
	}
	
	public void saveFunctionList()
	{
		// Creating file chooser to open user preferences
		String path = JEXDialog.fileSaveDialog();
		
		if(path != null)
		{
			// Save the function list
			this.parent.saveFunctionList(new File(path));
		}
		else
		{
			Logs.log("Cannot save the file", 0, this);
		}
	}
	
	public void selectFunction(FunctionBlockPanel fb)
	{
		this.selectedFunction = fb;
		if(this.selectedFunction == null)
		{
			this.parent.paramPanel.selectFunction(null);
		}
		else
		{
			this.parent.paramPanel.selectFunction(this.selectedFunction.getFunction());
		}
	}
	
	public FunctionBlockPanel getSelectedFunction()
	{
		return this.selectedFunction;
	}
	
	public boolean isAutoSaveSelected()
	{
		return this.editPanel.isAutoSavingOn();
	}
	
	// ----------------------------------------------------
	// --------- FUNCTION LIST PANEL ----------------------
	// ----------------------------------------------------
	/**
	 * FunctionChooserPanel class
	 */
	class FunctionChooserDialog extends JDialog implements ActionListener {
		
		private static final long serialVersionUID = 1L;
		
		// Main GUI
		JPanel panel = new JPanel();
		
		// GUI function list
		JScrollPane treeScrollPane = new JScrollPane();
		FunctionTree functionTree;
		JButton closeButton = new JButton("CLOSE");
		
		FunctionChooserDialog()
		{
			this.initialize();
		}
		
		private void initialize()
		{
			this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			this.setSize(300, 500);
			this.setLocation(850, 100);
			
			// Always add the actions to the action map of the menuPane because
			// that is the one that never changes
			// If it does ever change, call setQuickKeys again to reconnect
			// quick keys
			KeyStroke stroke;
			InputMap inputs = this.panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
			ActionMap actions = this.panel.getActionMap();
			
			// Close action
			stroke = KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.META_DOWN_MASK, false);
			inputs.put(stroke, "close");
			stroke = KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK, false);
			inputs.put(stroke, "close");
			actions.put("close", new ActionClose(this));
			
			// Main panel
			this.panel.setBackground(DisplayStatics.lightBackground);
			this.panel.setLayout(new BorderLayout());
			
			// Function tree panel
			this.functionTree = new FunctionTree();
			this.functionTree.fillTree();
			JPanel treePanel = new JPanel();
			treePanel.setBackground(DisplayStatics.lightBackground);
			treePanel.setLayout(new BorderLayout());
			treePanel.add(this.functionTree);
			this.treeScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			this.treeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			this.treeScrollPane.setViewportView(treePanel);
			this.closeButton.setMinimumSize(new Dimension(100, 50));
			this.closeButton.setMaximumSize(new Dimension(1000, 50));
			this.closeButton.setPreferredSize(new Dimension(1000, 50));
			this.closeButton.addActionListener(this);
			
			// add mouse listener to function tree
			MouseAdapter ml = new MouseAdapter() {
				
				@Override
				public void mousePressed(MouseEvent e)
				{
					int selRow = FunctionChooserDialog.this.functionTree.getRowForLocation(e.getX(), e.getY());
					if(selRow != -1)
					{
						if(e.getClickCount() == 2)
						{
							if(FunctionChooserDialog.this.isFunctionSelected())
							{
								FunctionListPanel.this.parent.addFunction(FunctionChooserDialog.this.getSelectedFunction());
							}
						}
					}
				}
			};
			this.functionTree.addMouseListener(ml);
			
			this.panel.add(this.treeScrollPane, BorderLayout.CENTER);
			this.panel.add(this.closeButton, BorderLayout.SOUTH);
			
			this.getContentPane().add(this.panel);
			this.setModal(true);
			this.setVisible(true);
		}
		
		public JEXFunction getSelectedFunction()
		{
			TreePath selPath = this.functionTree.getSelectionPath();
			if(selPath != null)
			{
				JEXCrunchable cr = this.functionTree.getFunctionForPath(selPath);
				if(cr == null)
				{
					return null;
				}
				
				// Try to make the function
				JEXFunction result = null;
				try
				{
					result = new JEXFunction(cr.getName());
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				return result;
			}
			return null;
		}
		
		public boolean isFunctionSelected()
		{
			return this.getSelectedFunction() != null;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			if(e.getSource() == this.closeButton)
			{
				Logs.log("Function creation canceled", 1, this);
				this.setVisible(false);
				this.dispose();
			}
		}
		
		// ACTIONS
		@SuppressWarnings("serial")
		public class ActionClose extends AbstractAction {
			
			private FunctionChooserDialog chooser;
			
			public ActionClose(FunctionChooserDialog chooser)
			{
				this.chooser = chooser;
			}
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				this.chooser.setVisible(false);
				this.chooser.dispose();
				Logs.log("Closing Image Browser", 0, this);
			}
		}
	}
	
	// ----------------------------------------------------
	// --------- FUNCTION TREE PANEL ----------------------
	// ----------------------------------------------------
	class FunctionTree extends JTree implements TreeSelectionListener {
		
		private static final long serialVersionUID = 1L;
		public DefaultMutableTreeNode top;
		public JEXCrunchable cr;
		
		public FunctionTree()
		{
			this.top = new DefaultMutableTreeNode("Root");
			DefaultTreeModel treeModel = new DefaultTreeModel(this.top);
			this.setModel(treeModel);
			this.setRootVisible(false);
			this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			this.addTreeSelectionListener(this);
			ToolTipManager.sharedInstance().registerComponent(this);
			this.setCellRenderer(new FunctionNodeRenderer());
		}
		
		public JEXCrunchable getSelectedFunction()
		{
			return this.cr;
		}
		
		public void fillTree()
		{
			// clear the current tree
			this.top.removeAllChildren();
			DefaultTreeModel treeModel = new DefaultTreeModel(this.top);
			
			// get all IDs
			Logs.log("Filling tree ", 1, this);
			
			TreeSet<String> toolboxes = CrunchFactory.getToolboxes();
			for (String tb : toolboxes)
			{
				ToolBoxNode dbNode = new ToolBoxNode(tb);
				this.top.add(dbNode);
			}
			
			// MatlabToolboxNode dbNode = new MatlabToolboxNode();
			// this.top.add(dbNode);
			
			this.setModel(treeModel);
			// this.expandAll();
		}
		
		/**
		 * Expand each node of the tree until level determined by internal variable defaultdepth
		 */
		public void expandAll()
		{
			for (int i = 0; i < this.getRowCount(); i++)
			{
				this.expandRow(i);
			}
			
		}
		
		@Override
		public void valueChanged(TreeSelectionEvent e)
		{
			// get all selected nodes of the browsing tree
			TreePath[] selectedPaths = this.getSelectionPaths();
			// if the user clicked elsewhere than on a node tag
			if(selectedPaths == null)
			{
				return;
			}
			
			// Get the path selected
			Object[] path = selectedPaths[0].getPath();
			Object o = path[path.length - 1];
			
			if(o instanceof FunctionNode)
			{
				this.cr = ((FunctionNode) o).function;
				Logs.log("Selecting function " + this.cr.getName(), 1, this);
			}
		}
		
		public JEXCrunchable getFunctionForPath(TreePath selectedpath)
		{
			// Get the path selected
			Object[] path = selectedpath.getPath();
			Object o = path[path.length - 1];
			
			JEXCrunchable crunch = null;
			if(o instanceof FunctionNode)
			{
				crunch = ((FunctionNode) o).function;
			}
			return crunch;
		}
		
		class ToolBoxNode extends DefaultMutableTreeNode {
			
			private static final long serialVersionUID = 1L;
			String toolbox;
			
			public ToolBoxNode(String toolbox)
			{
				super(toolbox);
				this.toolbox = toolbox;
				this.fill();
			}
			
			public void fill()
			{
				this.removeAllChildren();
				
				TreeMap<String,JEXCrunchable> availableFunctions = CrunchFactory.getJEXCrunchablesInToolbox(this.toolbox);
				for (JEXCrunchable c : availableFunctions.values())
				{
					FunctionNode dbNode = new FunctionNode(c);
					this.add(dbNode);
				}
			}
		}
		
		class FunctionNode extends DefaultMutableTreeNode {
			
			private static final long serialVersionUID = 1L;
			JEXCrunchable function;
			
			public FunctionNode(JEXCrunchable function)
			{
				super(function.getName());
				this.function = function;
			}
		}
		
		// class MatlabToolboxNode extends DefaultMutableTreeNode {
		//
		// private static final long serialVersionUID = 1L;
		//
		// public MatlabToolboxNode()
		// {
		// super("Matlab/Octave");
		// this.fill();
		// }
		//
		// public void fill()
		// {
		// this.removeAllChildren();
		//
		// List<MatlabNode> availableFunctions = this.getAvailableFunctions();
		// for (int i = 0, len = availableFunctions.size(); i < len; i++)
		// {
		// MatlabNode dbNode = availableFunctions.get(i);
		// this.add(dbNode);
		// }
		//
		// CustomMatlabNode lastNode = new CustomMatlabNode();
		// this.add(lastNode);
		// }
		//
		// public List<MatlabNode> getAvailableFunctions()
		// {
		// File root = new File(CrunchFactory.matlabPath);
		// File[] l = root.listFiles();
		//
		// List<MatlabNode> ret = new ArrayList<MatlabNode>(0);
		//
		// if(l == null)
		// {
		// return ret;
		// }
		// for (int i = 0; i < l.length; i++)
		// {
		// String name = l[i].getName();
		// if(name.substring(name.length() - 2, name.length()).equals(".m"))
		// {
		// MatlabNode mFunction = new MatlabNode(l[i]);
		// ret.add(mFunction);
		// }
		// }
		//
		// return ret;
		// }
		// }
		//
		// class MatlabNode extends DefaultMutableTreeNode {
		//
		// private static final long serialVersionUID = 1L;
		// String path;
		//
		// public MatlabNode(File file)
		// {
		// super(file.getName());
		// this.path = file.toString();
		// }
		//
		// public ExperimentalDataCrunch makeFunction()
		// {
		// ExperimentalDataCrunch ret = new MatlabFunctionClass(this.path);
		// return ret;
		// }
		// }
		//
		// class CustomMatlabNode extends DefaultMutableTreeNode {
		//
		// private static final long serialVersionUID = 1L;
		//
		// public CustomMatlabNode()
		// {
		// super("Choose custom .m");
		// }
		//
		// public ExperimentalDataCrunch makeFunction()
		// {
		// String path = this.selectPath();
		// ExperimentalDataCrunch ret = new MatlabFunctionClass(path);
		// return ret;
		// }
		//
		// public String selectPath()
		// {
		// JFileChooser fc = new JFileChooser();
		// fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		// int returnVal = fc.showOpenDialog(JEXStatics.main);
		// if(returnVal == JFileChooser.APPROVE_OPTION)
		// {
		// File saveFile = fc.getSelectedFile();
		// String savepath = saveFile.getAbsolutePath();
		// return savepath;
		// }
		// else
		// {
		// System.out.println("Not possible to choose file as a Matlab function...");
		// }
		// return null;
		// }
		// }
		
		class FunctionNodeRenderer extends DefaultTreeCellRenderer {
			
			private static final long serialVersionUID = 1L;
			Icon functionIcon;
			
			public FunctionNodeRenderer()
			{
				// functionIcon = icon;
			}
			
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
			{
				
				super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
				
				if(value instanceof FunctionNode)
				{
					FunctionNode fn = (FunctionNode) value;
					this.setToolTipText(fn.function.getInfo());
				}
				else
				{
					this.setToolTipText(null); // no tool tip
				}
				
				return this;
			}
			
		}
		
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		// Zoom around mouse pointer
		boolean isVertical = !e.isShiftDown();
		if(!isVertical) // then horizontal
		{
			this.scroll.getHorizontalScrollBar().dispatchEvent(e);
		}
	}
	
}
