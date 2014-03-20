package jex.jexTabPanel.jexNotesPanel;

import javax.swing.JPanel;

public class JEXNotesRightPanel extends JPanel // implements ActionListener
{
	
	private static final long serialVersionUID = 1L;
	// private JEXNotesPanelController parent;
	//
	// // Model
	// private TreeMap<String,List<File>> fileMap ;
	// private File dbFile;
	// private File expFile;
	// private File arrayFile;
	//
	// // Gui
	// private FileRepository databaseFiles ;
	// private FileRepository experimentFiles ;
	// private FileRepository arrayFiles ;
	// private JPanel databasePanel;
	// private JPanel experimentPanel;
	// private JPanel arrayPanel;
	//
	// JEXNotesRightPanel(JEXNotesPanelController parent)
	// {
	// this.parent = parent;
	// initializeModel();
	// initializeGUI();
	//
	// // Connect to changes in the filtering / value to be displayed
	// SSCenter.defaultCenter().connect(this.parent,
	// JEXNotesPanelController.NAVIGATION_CHANGED, this, "changeDetected",
	// (Class[])null);
	// }
	//
	// /**
	// * Detach the signals
	// */
	// public void deInitialize()
	// {
	// SSCenter.defaultCenter().disconnect(this);
	// }
	//
	// //////
	// ////// MODEL AND GUI
	// //////
	//
	// private void initializeModel()
	// {
	// fileMap = new TreeMap<String,List<File>>();
	// }
	//
	// private void initializeGUI()
	// {
	// // Make the database level file drop
	// databaseFiles = new FileRepository(this);
	// databasePanel = new JPanel();
	// databasePanel.setBackground(DisplayStatics.lightBackground);
	// databasePanel.setBorder(BorderFactory.createLineBorder(Color.black, 2));
	// databasePanel.setLayout(new BorderLayout());
	// databasePanel.add(new
	// JLabel("Files attached to the database"),BorderLayout.PAGE_START);
	// databasePanel.add(databaseFiles,BorderLayout.CENTER);
	//
	// // Make the database level file drop
	// experimentFiles = new FileRepository(this);
	// experimentPanel = new JPanel();
	// experimentPanel.setBackground(DisplayStatics.lightBackground);
	// experimentPanel.setBorder(BorderFactory.createLineBorder(Color.black,
	// 2));
	// experimentPanel.setLayout(new BorderLayout());
	// experimentPanel.add(new
	// JLabel("Files attached to the experiment"),BorderLayout.PAGE_START);
	// experimentPanel.add(experimentFiles,BorderLayout.CENTER);
	//
	// // Make the database level file drop
	// arrayFiles = new FileRepository(this);
	// arrayPanel = new JPanel();
	// arrayPanel.setBackground(DisplayStatics.lightBackground);
	// arrayPanel.setBorder(BorderFactory.createLineBorder(Color.black, 2));
	// arrayPanel.setLayout(new BorderLayout());
	// arrayPanel.add(new
	// JLabel("Files attached to the array"),BorderLayout.PAGE_START);
	// arrayPanel.add(arrayFiles,BorderLayout.CENTER);
	//
	// // Place the gui in this panel
	// this.setLayout(new GridLayout(3,1));
	// this.setBackground(DisplayStatics.lightBackground);
	// this.add(databasePanel);
	// this.add(experimentPanel);
	// this.add(arrayPanel);
	// }
	//
	// private void rebuildModel()
	// {
	// fileMap = new TreeMap<String,List<File>>();
	//
	// // Do the database level stuff
	// String dbPath = JEXWriter.getAttachedFolderPath(null, "", true, false);
	// if (dbPath != null)
	// {
	// dbFile = new File(dbPath);
	// File[] allFiles = dbFile.listFiles();
	// List<File> dbFiles = new ArrayList<File>();
	// if (allFiles != null)
	// {
	// for (File f: allFiles)
	// {
	// if (!StringUtility.begginWith(f.getName(),"_")) dbFiles.add(f);
	// }
	// }
	// fileMap.put("Database",dbFiles);
	// }
	//
	// // Get a random entry
	// JEXEntry entry =
	// JEXStatics.jexManager.getCurrentDatabase().getFilteredEntries().first();
	//
	// // Do the experiment level stuff
	// String experiment = JEXStatics.jexManager.getExperimentViewed();
	// if (entry != null && experiment != null && !experiment.equals(""))
	// {
	// String expPath = JEXWriter.getAttachedFolderPath(entry,
	// JEXEntry.EXPERIMENT, true, false);
	// if (expPath != null)
	// {
	// expFile = new File(expPath);
	// File[] allFiles = expFile.listFiles();
	// List<File> expFiles = new ArrayList<File>();
	// if (allFiles != null)
	// {
	// for (File f: allFiles)
	// {
	// if (!StringUtility.begginWith(f.getName(),"_")) expFiles.add(f);
	// }
	// }
	// fileMap.put("Experiment",expFiles);
	// }
	// }
	//
	// // Do the array level stuff
	// String array = JEXStatics.jexManager.getArrayViewed();
	// if (entry != null && array != null && !array.equals(""))
	// {
	// String trayPath = JEXWriter.getAttachedFolderPath(entry, JEXEntry.TRAY,
	// true, false);
	// if (trayPath != null)
	// {
	// arrayFile = new File(trayPath);
	// File[] allFiles = arrayFile.listFiles();
	// List<File> trayFiles = new ArrayList<File>();
	// if (allFiles != null)
	// {
	// for (File f: allFiles)
	// {
	// if (!StringUtility.begginWith(f.getName(),"_")) trayFiles.add(f);
	// }
	// }
	// fileMap.put("Array",trayFiles);
	// }
	// }
	// }
	//
	// private void rebuildGUI()
	// {
	// dbFile = (fileMap.get("Database") == null) ? null : dbFile;
	// databaseFiles.setFiles(fileMap.get("Database"),dbFile);
	//
	// expFile = (fileMap.get("Experiment") == null) ? null : expFile;
	// experimentFiles.setFiles(fileMap.get("Experiment"),expFile);
	//
	// arrayFile = (fileMap.get("Array") == null) ? null : arrayFile;
	// arrayFiles.setFiles(fileMap.get("Array"),arrayFile);
	// }
	//
	// //////
	// ////// METHODS
	// //////
	//
	//
	// //////
	// ////// EVENTS
	// //////
	//
	// public void changeDetected()
	// {
	// rebuildModel();
	// rebuildGUI();
	// }
	//
	// public void fileDroped()
	// {
	// rebuildModel();
	// rebuildGUI();
	// }
	//
	//
	// public void actionPerformed(ActionEvent e){}
	//
	//
	// }
	//
	//
	//
	// class FileRepository extends JPanel{
	// private static final long serialVersionUID = 1L;
	//
	// private JEXNotesRightPanel parent;
	// private JScrollPane scroll ;
	// private JPanel filePane ;
	// public File[] files ;
	// public File folder ;
	//
	// public FileRepository(JEXNotesRightPanel parent){
	// initializeGUI();
	//
	// this.parent = parent;
	// new FileDropArea(this, filePane);
	// }
	//
	// //////
	// ////// MODEL AND GUI
	// //////
	//
	// public void rebuildGUI(){
	// filePane.removeAll();
	//
	// if (files == null || folder == null)
	// {
	// JLabel nullLabel = new JLabel("Hierarchy level not opened...");
	// filePane.add(Box.createHorizontalStrut(5));
	// filePane.add(nullLabel);
	// filePane.add(Box.createHorizontalGlue());
	// }
	// else if (files.length == 0)
	// {
	// JLabel nullLabel = new JLabel("No files here yet...");
	// filePane.add(Box.createHorizontalStrut(5));
	// filePane.add(nullLabel);
	// filePane.add(Box.createHorizontalGlue());
	// }
	// else
	// {
	// for (File f: files){
	// OpenIcon fl = new OpenIcon(f);
	// filePane.add(fl);
	// filePane.add(Box.createHorizontalStrut(5));
	// }
	// filePane.add(Box.createVerticalGlue());
	// }
	//
	// this.invalidate();
	// this.validate();
	// this.repaint();
	// }
	//
	// private void initializeGUI(){
	// filePane = new JPanel();
	// filePane.setBackground(DisplayStatics.background);
	// filePane.setLayout(new BoxLayout(filePane,BoxLayout.LINE_AXIS));
	//
	// scroll = new JScrollPane(filePane);
	// scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	// scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
	//
	// this.setBackground(DisplayStatics.background);
	// this.setLayout(new BorderLayout());
	// this.add(scroll,BorderLayout.CENTER);
	// }
	//
	// //////
	// ////// METHODS
	// //////
	//
	// public void setFiles(File[] files, File folder){
	// this.files = files;
	// this.folder = folder;
	// rebuildGUI();
	// }
	//
	// public void setFiles(List<File> files, File folder){
	// if (files == null)
	// {
	// this.files = new File[0];
	// }
	// else
	// {
	// this.files = files.toArray(new File[0]);
	// }
	// this.folder = folder;
	// rebuildGUI();
	// }
	//
	// //////
	// ////// EVENTS
	// //////
	//
	// public void dropFiles(List<File> files)
	// {
	// if (folder == null)
	// {
	// return;
	// }
	//
	// for (File f: files)
	// {
	// String newDest = folder.getPath() + File.separator + f.getName();
	// try
	// {
	// FileUtility.copy(f, new File(newDest));
	// }
	// catch (IOException e)
	// {
	// e.printStackTrace();
	// }
	// }
	//
	// parent.fileDroped();
	// }
	// }
	//
	// class FileDropArea extends DropTargetAdapter {
	//
	// @SuppressWarnings("unused")
	// private DropTarget dropTarget;
	// private JPanel area ;
	// private FileRepository parent;
	//
	// public FileDropArea(FileRepository parent, JPanel area) {
	// this.area = area;
	// this.parent = parent;
	// dropTarget = new DropTarget(this.area, DnDConstants.ACTION_COPY, this,
	// true, null);
	// }
	//
	// @SuppressWarnings("unchecked")
	// public void drop(DropTargetDropEvent event) {
	//
	// try {
	// if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
	// Transferable tr = event.getTransferable();
	// int action = event.getDropAction();
	// event.acceptDrop(action);
	//
	// Logs.log("Droping files ",1,this);
	// List<File> files = (List<File>)
	// tr.getTransferData(DataFlavor.javaFileListFlavor);
	// parent.dropFiles(files);
	// event.dropComplete(true);
	// Logs.log("Drop completed...",1,this);
	//
	// return;
	// }
	// event.rejectDrop();
	// } catch (Exception e) {
	// e.printStackTrace();
	// event.rejectDrop();
	// }
	// }
	// }
	//
	// class OpenIcon extends JLabel implements MouseListener{
	// private static final long serialVersionUID = 1L;
	// private File f;
	//
	// public OpenIcon(File f){
	// this.f = f;
	// initialize();
	// }
	//
	// private void initialize(){
	// this.setText(f.getName());
	// Icon icon = JEXStatics.fileChooser.getIcon(f);
	// this.setIcon(icon);
	// this.setVerticalTextPosition(JLabel.BOTTOM);
	// this.setHorizontalTextPosition(JLabel.CENTER);
	// this.addMouseListener(this);
	// }
	//
	// public void mouseClicked(MouseEvent e) {}
	// public void mouseEntered(MouseEvent e) {}
	// public void mouseExited(MouseEvent e) {}
	// public void mousePressed(MouseEvent e) {}
	// public void mouseReleased(MouseEvent e) {
	// if (e.getClickCount() == 2){
	// Logs.log("Opening "+f+" in default application",1,this);
	// try {
	// FileUtility.openFileDefaultApplication(f.getAbsolutePath());
	// } catch (Exception e1) {
	// Logs.log("Cannot open file",1,this);
	// }
	// }
	// }
	
}