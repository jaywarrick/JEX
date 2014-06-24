package jex.jexTabPanel.jexFunctionPanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import logs.Logs;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import statics.DisplayStatics;
import cruncher.JEXFunction;
import function.CrunchFactory;
import function.JEXCrunchable;

//----------------------------------------------------
// --------- FUNCTION LIST PANEL ----------------------
// ----------------------------------------------------
/**
 * FunctionChooserPanel class
 */
class FunctionChooserPanel {
	
	// Main GUI
	JEXFunctionPanel parent;
	JPanel panel = new JPanel();
	
	// GUI function list
	JScrollPane treeScrollPane; // = new
	// JScrollPane();
	FunctionTree functionTree;
	
	// JButton closeButton = new JButton("CLOSE");
	
	FunctionChooserPanel(JEXFunctionPanel parent)
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
		
		// Main panel
		this.panel.setBackground(Color.WHITE);
		this.panel.setLayout(new MigLayout("flowy, ins 2", "[fill,grow]", "[]2[fill,grow]"));
		
		// Build the selector header
		JLabel title1 = new JLabel("FUNCTION LIBRARY");
		JPanel headerPane1 = new JPanel(new MigLayout("flowy,center,ins 1", "[center]", "[center]"));
		headerPane1.setBackground(DisplayStatics.menuBackground);
		title1.setFont(FontUtility.boldFont);
		headerPane1.add(title1);
		this.panel.add(headerPane1, "growx");
		
		// Function tree panel
		this.functionTree = new FunctionTree();
		this.functionTree.fillTree();
		FunctionTreePanel treePanel = new FunctionTreePanel(this.functionTree);
		// JPanel treePanel = new JPanel();
		// treePanel.setBackground(DisplayStatics.lightBackground);
		// treePanel.setLayout(new BorderLayout());
		// treePanel.add(functionTree);
		this.treeScrollPane = new JScrollPane(treePanel);
		this.treeScrollPane.getViewport().setBackground(Color.WHITE);
		this.treeScrollPane.setBackground(Color.WHITE);
		this.treeScrollPane.setBorder(BorderFactory.createEmptyBorder());
		this.treeScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.treeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		// treeScrollPane.setViewportView(treePanel);
		
		// add mouse listener to function tree
		MouseAdapter ml = new MouseAdapter() {
			
			@Override
			public void mousePressed(MouseEvent e)
			{
				int selRow = FunctionChooserPanel.this.functionTree.getRowForLocation(e.getX(), e.getY());
				if(selRow != -1)
				{
					if(e.getClickCount() == 2)
					{
						if(FunctionChooserPanel.this.isFunctionSelected())
						{
							FunctionChooserPanel.this.parent.addFunction(FunctionChooserPanel.this.getSelectedFunction());
						}
					}
				}
			}
		};
		this.functionTree.addMouseListener(ml);
		
		this.panel.add(this.treeScrollPane, "growy");
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
			
			// make the function
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
			
			TreeMap<String,JEXCrunchable> availableFunctions = CrunchFactory.getFunctionsFromToolbox(this.toolbox);
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
	// File root = new File(CrunchFactory.matlabPath); // Need to remember
	// // to include
	// // windows folks and
	// // work with classes
	// // instead of
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
