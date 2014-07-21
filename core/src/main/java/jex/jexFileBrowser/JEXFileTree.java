package jex.jexFileBrowser;

import java.io.File;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import logs.Logs;

public class JEXFileTree extends JTree implements TreeSelectionListener {
	
	// Static variables
	private static final long serialVersionUID = 1L;
	
	// Class variables
	private String rootPath = null;
	private DefaultMutableTreeNode top;
	
	public JEXFileTree(String rootPath)
	{
		// Pass the variables
		this.rootPath = rootPath;
		
		// Create the model
		top = new DefaultMutableTreeNode("Root");
		DefaultTreeModel treeModel = new DefaultTreeModel(top);
		this.setModel(treeModel);
		this.setRootVisible(false);
		this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		this.addTreeSelectionListener(this);
		
		// Fill the tree
		initialize();
	}
	
	private void initialize()
	{
		// clear the current tree
		top.removeAllChildren();
		DefaultTreeModel treeModel = new DefaultTreeModel(top);
		
		// get all IDs
		Logs.log("Filling tree ", 1, this);
		
		// Get the sub folders
		File rootFile = new File(rootPath);
		File[] expFolders = rootFile.listFiles();
		
		for (File f : expFolders)
		{
			FolderNode dbNode = new FolderNode(f);
			top.add(dbNode);
		}
		
		this.setModel(treeModel);
		this.expandAll();
	}
	
	/**
	 * Expand each node of the tree until level determined by internal variable defaultdepth
	 */
	private void expandAll()
	{
		for (int i = 0; i < this.getRowCount(); i++)
		{
			this.expandRow(i);
		}
	}
	
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
		
		if(o instanceof FolderNode)
		{
			String project = ((FolderNode) o).f.getPath();
			Logs.log("Project selected is " + project, 1, this);
		}
	}
}

class FolderNode extends DefaultMutableTreeNode {
	
	private static final long serialVersionUID = 1L;
	File f;
	
	public FolderNode(File f)
	{
		super(f.getPath());
		this.f = f;
	}
}

class EntryNode extends DefaultMutableTreeNode {
	
	private static final long serialVersionUID = 1L;
	String path;
	
	public EntryNode(String path)
	{
		super(path);
		this.path = path;
	}
}

class TypeNameNode extends DefaultMutableTreeNode {
	
	private static final long serialVersionUID = 1L;
	String path;
	
	public TypeNameNode(String path)
	{
		super(path);
		this.path = path;
	}
}