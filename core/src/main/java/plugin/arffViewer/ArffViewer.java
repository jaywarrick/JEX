package plugin.arffViewer;

import java.awt.BorderLayout;

import jex.statics.DisplayStatics;
import plugins.plugin.PlugIn;
import plugins.plugin.PlugInController;
import weka.core.converters.WekaConverterUtility;
import weka.gui.arffviewer.ArffViewerMainPanel;

public class ArffViewer implements PlugInController {
	
	public PlugIn dialog;
	public ArffViewerMainPanel main;
	
	static
	{
		WekaConverterUtility.addNewFileExtensionToViewer("jxd");
		WekaConverterUtility.addNewFileExtensionToViewer("jxw");
		WekaConverterUtility.addNewFileExtensionToViewer("txt");
	}
	
	public ArffViewer()
	{
		this.main = new ArffViewerMainPanel(this.dialog);
		this.main.setExitOnClose(false);
		this.initializeDialog();
	}
	
	private void initializeDialog()
	{
		this.dialog = new PlugIn(this);
		this.dialog.setBounds(100, 100, 800, 600);
		this.dialog.setDefaultCloseOperation(PlugIn.DISPOSE_ON_CLOSE);
		this.dialog.getContentPane().setBackground(DisplayStatics.background);
		this.dialog.getContentPane().setLayout(new BorderLayout());
		this.dialog.getContentPane().add(this.main, BorderLayout.CENTER);
	}
	
	public void setFile(String filePath)
	{
		this.main.closeAllFiles();
		this.main.loadFile(filePath);
	}
	
	public void show()
	{
		this.dialog.setVisible(true);
	}
	
	@Override
	public void finalizePlugIn()
	{
		// Do nothing
	}
	
	@Override
	public PlugIn plugIn()
	{
		return this.dialog;
	}
	
}
