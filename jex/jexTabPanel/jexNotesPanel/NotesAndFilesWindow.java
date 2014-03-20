package jex.jexTabPanel.jexNotesPanel;

import javax.swing.JDialog;

public class NotesAndFilesWindow extends JDialog {
	
	private static final long serialVersionUID = 1L;
	// private static int DBLEVEL = 0;
	// private static int EXPLEVEL = 1;
	// private static int TRAYLEVEL = 2;
	// private int viewedLevel = DBLEVEL;
	//
	// public int topLeftX = 800 ;
	// public int topLeftY = 50 ;
	// public int windowWidth = 300 ;
	// public int windowHeight = 600 ;
	//
	// private JPanel menuPanel ;
	// private JPanel centerPanel ;
	//
	// private NotesPanel dbNotes ;
	// private NotesPanel expNotes ;
	// private NotesPanel arrayNotes ;
	//
	// private SignalMenuButton dbLevel = new SignalMenuButton();
	// private SignalMenuButton expLevel = new SignalMenuButton();
	// private SignalMenuButton trayLevel = new SignalMenuButton();
	//
	// public NotesAndFilesWindow(){
	// SSCenter.defaultCenter().connect(JEXStatics.jexManager,
	// JEXManager.DATASETS, this, "arrayChanged", (Class[])null);
	//
	// // Initialize
	// initialize();
	// arrayChanged();
	// }
	//
	// /**
	// * Initalize
	// */
	// private void initialize(){
	// this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
	// this.setVisible(false);
	// this.setResizable(true);
	// this.setBounds(topLeftX, topLeftY, windowWidth, windowHeight);
	// this.setMinimumSize(new Dimension(75,300));
	// this.setTitle("Je'Xperiment - Notes and attached Files");
	//
	// menuPanel = new JPanel();
	// menuPanel.setBackground(DisplayStatics.background);
	// menuPanel.setLayout(new BoxLayout(menuPanel,BoxLayout.LINE_AXIS));
	// menuPanel.setMaximumSize(new Dimension(1000,40));
	// menuPanel.setPreferredSize(new Dimension(100,40));
	//
	// Image icondb =
	// JEXStatics.iconRepository.getImageWithName(IconRepository.DATABASE, 20,
	// 20);
	// Image iconOverdb =
	// JEXStatics.iconRepository.getImageWithName(IconRepository.DATABASE, 20,
	// 20);
	// Image iconPresseddb =
	// JEXStatics.iconRepository.getImageWithName(IconRepository.DATABASE, 20,
	// 20);
	// Image greyBox = JEXStatics.iconRepository.boxImage(30,
	// 30,DisplayStatics.dividerColor);
	// dbLevel.setBackgroundColor(DisplayStatics.background);
	// dbLevel.setForegroundColor(DisplayStatics.lightBackground);
	// dbLevel.setClickedColor(DisplayStatics.menuBackground,
	// DisplayStatics.lightBackground);
	// dbLevel.setSize(new Dimension(60,40));
	// dbLevel.setImage(icondb);
	// dbLevel.setMouseOverImage(iconOverdb);
	// dbLevel.setMousePressedImage(iconPresseddb);
	// dbLevel.setDisabledImage(greyBox);
	// dbLevel.setText("DB notes");
	// SSCenter.defaultCenter().connect(dbLevel,
	// SignalMenuButton.SIG_ButtonClicked_NULL, this, "viewDBlevel",
	// (Class[])null);
	// menuPanel.add(Box.createHorizontalStrut(2));
	// menuPanel.add(dbLevel);
	//
	// Image iconexp =
	// JEXStatics.iconRepository.getImageWithName(IconRepository.EXPERIMENT_ICON,
	// 20, 20);
	// Image iconOverexp =
	// JEXStatics.iconRepository.getImageWithName(IconRepository.EXPERIMENT_ICON,
	// 20, 20);
	// Image iconPressedexp =
	// JEXStatics.iconRepository.getImageWithName(IconRepository.EXPERIMENT_ICON,
	// 20, 20);
	// expLevel.setBackgroundColor(DisplayStatics.background);
	// expLevel.setForegroundColor(DisplayStatics.lightBackground);
	// expLevel.setClickedColor(DisplayStatics.menuBackground,
	// DisplayStatics.lightBackground);
	// expLevel.setSize(new Dimension(70,40));
	// expLevel.setImage(iconexp);
	// expLevel.setMouseOverImage(iconOverexp);
	// expLevel.setMousePressedImage(iconPressedexp);
	// expLevel.setDisabledImage(greyBox);
	// expLevel.setText("Exp. notes");
	// SSCenter.defaultCenter().connect(expLevel,
	// SignalMenuButton.SIG_ButtonClicked_NULL, this, "viewEXPlevel",
	// (Class[])null);
	// menuPanel.add(Box.createHorizontalStrut(5));
	// menuPanel.add(expLevel);
	//
	// Image icontray =
	// JEXStatics.iconRepository.getImageWithName(IconRepository.TRAY_ICON, 20,
	// 20);
	// Image iconOvertray =
	// JEXStatics.iconRepository.getImageWithName(IconRepository.TRAY_ICON, 20,
	// 20);
	// Image iconPressedtray =
	// JEXStatics.iconRepository.getImageWithName(IconRepository.TRAY_ICON, 20,
	// 20);
	// trayLevel.setBackgroundColor(DisplayStatics.background);
	// trayLevel.setForegroundColor(DisplayStatics.lightBackground);
	// trayLevel.setClickedColor(DisplayStatics.menuBackground,
	// DisplayStatics.lightBackground);
	// trayLevel.setSize(new Dimension(70,40));
	// trayLevel.setImage(icontray);
	// trayLevel.setMouseOverImage(iconOvertray);
	// trayLevel.setMousePressedImage(iconPressedtray);
	// trayLevel.setDisabledImage(greyBox);
	// trayLevel.setText("Array notes");
	// SSCenter.defaultCenter().connect(trayLevel,
	// SignalMenuButton.SIG_ButtonClicked_NULL, this, "viewTraylevel",
	// (Class[])null);
	// menuPanel.add(Box.createHorizontalStrut(2));
	// menuPanel.add(trayLevel);
	//
	// if (dbNotes == null) dbNotes = new NotesPanel("Database Level");
	// if (expNotes == null) expNotes = new NotesPanel("Experiment Level");
	// if (arrayNotes == null) arrayNotes = new NotesPanel("Array Level");
	//
	// centerPanel = new JPanel();
	// centerPanel.setBackground(DisplayStatics.background);
	// centerPanel.setLayout(new BorderLayout());
	// centerPanel.add(menuPanel,BorderLayout.PAGE_START);
	// centerPanel.add(dbNotes,BorderLayout.CENTER);
	//
	// this.add(centerPanel);
	// }
	//
	//
	// // ----------------------------------------------------
	// // --------- EVENT HANDLING FUNCTIONS -----------------
	// // ----------------------------------------------------
	//
	// public void arrayChanged(){
	// if (JEXStatics.jexManager.getCurrentDatabase() == null){
	// // Make an empty panel
	// JLabel noLabel = new JLabel("No database currently opened");
	// noLabel.setFont(FontUtility.boldFont);
	//
	// JPanel centerPanelTMP = new JPanel();
	// centerPanelTMP.setLayout(new
	// BoxLayout(centerPanelTMP,BoxLayout.PAGE_AXIS));
	// centerPanelTMP.add(Box.createVerticalGlue());
	// centerPanelTMP.add(noLabel);
	// centerPanelTMP.add(Box.createVerticalGlue());
	// centerPanel.add(centerPanelTMP,BorderLayout.CENTER);
	// centerPanel.repaint();
	//
	// dbNotes = null;
	// expNotes = null;
	// arrayNotes = null;
	// }
	// else{
	// if (dbNotes == null) dbNotes = new NotesPanel("Database Level");
	// if (expNotes == null) expNotes = new NotesPanel("Experiment Level");
	// if (arrayNotes == null) arrayNotes = new NotesPanel("Array Level");
	//
	// Iterator<JEXEntry> itr =
	// JEXStatics.jexManager.getCurrentDatabase().getEntries().iterator();
	// if (itr.hasNext()){
	// JEXEntry entry = itr.next();
	// String expFolder =
	// JEXWriter.getAttachedFolderPath(entry,JEXEntry.EXPERIMENT, true, false);
	// String arrayFolder = JEXWriter.getAttachedFolderPath(entry,JEXEntry.TRAY,
	// true, false);
	// expNotes.setFolderViewed(expFolder,JEXWriter.getAttachedNotesPath(entry,JEXEntry.EXPERIMENT,
	// true, false));
	// arrayNotes.setFolderViewed(arrayFolder,JEXWriter.getAttachedNotesPath(entry,JEXEntry.TRAY,
	// true, false));
	// }
	// else
	// {
	// expNotes.setFolderViewed(null,null);
	// arrayNotes.setFolderViewed(null,null);
	// }
	//
	// String dbFolder = JEXWriter.getAttachedFolderPath(null, "", true, false);
	// dbNotes.setFolderViewed(dbFolder,JEXWriter.getAttachedNotesPath(null, "",
	// true, false));
	//
	// rebuild();
	// }
	//
	// }
	//
	// public void rebuild(){
	// centerPanel.removeAll();
	// centerPanel.add(menuPanel,BorderLayout.PAGE_START);
	//
	// if (viewedLevel == DBLEVEL){
	// centerPanel.add(dbNotes,BorderLayout.CENTER);
	// }
	// else if (viewedLevel == EXPLEVEL){
	// centerPanel.add(expNotes,BorderLayout.CENTER);
	// }
	// else if (viewedLevel == TRAYLEVEL){
	// centerPanel.add(arrayNotes,BorderLayout.CENTER);
	// }
	//
	// this.invalidate();
	// this.validate();
	// this.repaint();
	// }
	//
	// public void viewDBlevel(){
	// viewedLevel = DBLEVEL;
	// rebuild();
	// }
	//
	// public void viewEXPlevel(){
	// viewedLevel = EXPLEVEL;
	// rebuild();
	// }
	//
	// public void viewTraylevel(){
	// viewedLevel = TRAYLEVEL;
	// rebuild();
	// }
	//
	// class NotesPanel extends JPanel {
	// private static final long serialVersionUID = 1L;
	//
	// private JPanel titlepane;
	// private JPanel notespane;
	// private JPanel bottompane;
	// private JLabel notesLabel;
	// private String folderViewed;
	// private String notesFile;
	// private String title;
	// private TextComponent editor;
	// private FileRepository fileRepository;
	//
	// public NotesPanel(String title){
	// this.title = title;
	// editor = new TextComponent();
	// this.initialize();
	// }
	//
	// public void setFolderViewed(String folderViewed, String notesFile){
	// this.folderViewed = folderViewed;
	// this.notesFile = notesFile;
	//
	// if (folderViewed == null || notesFile == null){
	// JLabel noLabel = new JLabel("No notes available at this level");
	// notespane.setLayout(new BoxLayout(notespane,BoxLayout.PAGE_AXIS));
	// notespane.removeAll();
	// notespane.add(Box.createVerticalGlue());
	// notespane.add(noLabel);
	// notespane.add(Box.createVerticalGlue());
	// }
	// else {
	// File folder = new File(this.folderViewed);
	// File notes = new File(this.notesFile);
	//
	// notesLabel.setText(notes.getAbsolutePath());
	// notespane.setLayout(new BorderLayout());
	// notespane.removeAll();
	//
	// notespane.add(editor);
	// if(notes.exists()) editor.open(notes);
	// fileRepository.setFolder(folder);
	// }
	// notespane.invalidate();
	// notespane.validate();
	// this.repaint();
	// }
	//
	// private void initialize(){
	// titlepane = new JPanel();
	// titlepane.setBackground(DisplayStatics.background);
	// titlepane.setLayout(new BoxLayout(titlepane,BoxLayout.LINE_AXIS));
	// titlepane.setMaximumSize(new Dimension(1000,20));
	// titlepane.setPreferredSize(new Dimension(100,20));
	//
	// JLabel titleLabel = new JLabel(title);
	// titleLabel.setFont(FontUtility.boldFont);
	// titlepane.add(titleLabel);
	// titlepane.add(Box.createHorizontalGlue());
	//
	// notespane = new JPanel();
	// notespane.setBackground(DisplayStatics.lightBackground);
	// notespane.setLayout(new BoxLayout(notespane,BoxLayout.PAGE_AXIS));
	//
	// notesLabel = new JLabel("");
	// notespane.add(Box.createVerticalGlue());
	// notespane.add(notesLabel);
	// notespane.add(Box.createVerticalGlue());
	//
	// bottompane = new JPanel();
	// bottompane.setBackground(DisplayStatics.background);
	// bottompane.setLayout(new BorderLayout());
	// bottompane.setMaximumSize(new Dimension(1000,60));
	// bottompane.setPreferredSize(new Dimension(100,60));
	//
	// fileRepository = new FileRepository();
	// bottompane.add(fileRepository);
	//
	// this.setBackground(DisplayStatics.background);
	// this.setLayout(new BorderLayout());
	// this.add(titlepane,BorderLayout.PAGE_START);
	// this.add(notespane,BorderLayout.CENTER);
	// this.add(bottompane,BorderLayout.PAGE_END);
	// }
	// }
	//
	// class FileRepository extends JPanel{
	// private static final long serialVersionUID = 1L;
	//
	// private JScrollPane scroll;
	// private JPanel filePane;
	// public File folder;
	//
	// public FileRepository(){
	// initialize();
	//
	// new FileDropArea(this, filePane);
	// }
	//
	// public void setFolder(File file){
	// folder = file;
	// rebuild();
	// }
	//
	// public void rebuild(){
	// filePane.removeAll();
	// File[] files = folder.listFiles();
	// for (File f: files){
	// OpenIcon fl = new OpenIcon(f);
	// filePane.add(fl);
	// filePane.add(Box.createHorizontalStrut(5));
	// }
	// filePane.add(Box.createVerticalGlue());
	// this.invalidate();
	// this.validate();
	// this.repaint();
	// }
	//
	// private void initialize(){
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
	// java.util.List<File> files = (java.util.List<File>)
	// tr.getTransferData(DataFlavor.javaFileListFlavor);
	// for (File f: files){
	// Logs.log("Copying file "+f,1,this);
	// String newDest = parent.folder.getPath() + File.separator + f.getName();
	// FileUtility.copy(f, new File(newDest));
	// }
	// parent.rebuild();
	//
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
	//
	// }
}
