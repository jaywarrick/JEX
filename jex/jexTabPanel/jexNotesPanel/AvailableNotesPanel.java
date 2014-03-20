package jex.jexTabPanel.jexNotesPanel;

import javax.swing.JPanel;

public class AvailableNotesPanel extends JPanel // implements MouseListener,
// ActionListener
{
	
	private static final long serialVersionUID = 1L;
	//
	// // Graphics panel
	// private JPanel fileList ;
	// private SimpleNotesPanel notePanel ;
	//
	// // Model
	// private TreeMap<String,List<File>> fileMap ;
	// private File viewedNote ;
	//
	// public AvailableNotesPanel()
	// {
	// initializeModel();
	// initializeGUI();
	// }
	//
	// //////
	// ////// MODEL AND GUI
	// //////
	//
	// private void initializeModel()
	// {
	// fileMap = new TreeMap<String,List<File>>();
	// viewedNote = null;
	// }
	//
	// private void initializeGUI()
	// {
	// // Make the notes list panel
	// fileList = new JPanel();
	// fileList.setBackground(DisplayStatics.lightBackground);
	// fileList.setLayout(new
	// MigLayout("flowy, ins 3, gap 3","[fill,grow]5[]","[]"));
	// fileList.setPreferredSize(new Dimension(150,500));
	// fileList.setMaximumSize(new Dimension(150,1000));
	// fileList.setMinimumSize(new Dimension(150,400));
	//
	// // Make the notes panel
	// notePanel = new SimpleNotesPanel("Notes");
	//
	// // Put together the GUI
	// this.setLayout(new BorderLayout());
	// this.setBackground(DisplayStatics.lightBackground);
	// this.add(fileList,BorderLayout.LINE_START);
	// this.add(notePanel,BorderLayout.CENTER);
	// }
	//
	// public void changeDetected()
	// {
	// rebuildModel();
	// rebuildGUI();
	// }
	//
	// private void rebuildModel()
	// {
	// fileMap = new TreeMap<String,List<File>>();
	// viewedNote = null;
	//
	// // Do the database level stuff
	// String dbPath = JEXWriter.getAttachedFolderPath(null, "", true, false);
	// if (dbPath != null)
	// {
	// File dbFile = new File(dbPath);
	// File[] allFiles = dbFile.listFiles();
	// List<File> dbNotes = new ArrayList<File>();
	// if (allFiles != null)
	// {
	// for (File f: allFiles)
	// {
	// if (StringUtility.begginWith(f.getName(),"_")) dbNotes.add(f);
	// }
	// }
	// fileMap.put("Database",dbNotes);
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
	// File expFile = new File(expPath);
	// File[] allFiles = expFile.listFiles();
	// List<File> expNotes = new ArrayList<File>();
	// if (allFiles != null)
	// {
	// for (File f: allFiles)
	// {
	// if (StringUtility.begginWith(f.getName(),"_")) expNotes.add(f);
	// }
	// }
	// fileMap.put("Experiment",expNotes);
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
	// File trayFile = new File(trayPath);
	// File[] allFiles = trayFile.listFiles();
	// List<File> trayNotes = new ArrayList<File>();
	// if (allFiles != null)
	// {
	// for (File f: allFiles)
	// {
	// if (StringUtility.begginWith(f.getName(),"_")) trayNotes.add(f);
	// }
	// }
	// fileMap.put("Array",trayNotes);
	// }
	// }
	// }
	//
	// private void rebuildGUI()
	// {
	// // Rebuild the notes list
	// fileList.removeAll();
	//
	// // Add the components
	// fileList.add(Box.createVerticalStrut(5));
	// for (String type: fileMap.keySet())
	// {
	// // Add the label
	// JLabel typeLabel = new JLabel(type+" notes");
	// typeLabel.setFont(FontUtility.boldFont);
	// typeLabel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
	//
	// // Add the new note button
	// FlatRoundedStaticButton newNote = new FlatRoundedStaticButton( "New" );
	// newNote.enableUnselection(false);
	// newNote.panel().setFont(FontUtility.defaultFonts);
	// //newNote.panel().setMaximumSize(new Dimension(30,20));
	// //newNote.panel().setAlignmentX(JPanel.LEFT_ALIGNMENT);
	// newNote.panel().setToolTipText("New note for the "+type);
	// newNote.addActionListener(this);
	//
	// // Make the title panel
	// JPanel typePane = new JPanel();
	// typePane.setBackground(DisplayStatics.lightBackground);
	// typePane.setLayout(new
	// MigLayout("flowx, ins 0, left","[fill,grow]5[]","[]"));
	// //typePane.setMaximumSize(new Dimension(600,30));
	// //typePane.setPreferredSize(new Dimension(150,30));
	// //typePane.setAlignmentX(JPanel.LEFT_ALIGNMENT);
	// typePane.add(typeLabel);
	// //typePane.add(Box.createHorizontalStrut(5));
	// typePane.add(newNote.panel());
	// fileList.add(typePane);
	//
	// // Add the files
	// fileList.add(Box.createVerticalStrut(5));
	// List<File> files = fileMap.get(type);
	// for (File f: files)
	// {
	// String noteName = f.getName().substring(1);
	// RichJLabel fileLabel = new RichJLabel(noteName);
	// fileLabel.setPath(f.getPath());
	// fileLabel.addMouseListener(this);
	// //fileLabel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
	//
	// fileList.add(fileLabel,"span,grow");
	// }
	// //fileList.add(Box.createVerticalStrut(10));
	// }
	//
	// fileList.invalidate();
	// fileList.validate();
	// fileList.repaint();
	//
	// // Rebuild the notes panel
	// notePanel.setNoteViewed(viewedNote);
	// notePanel.repaint();
	// }
	//
	// //////
	// ////// METHODS
	// //////
	//
	// private void makeNewNote(String type)
	// {
	// Logs.log("New "+type+" note requested", 1, this);
	//
	// // Ask for the note title
	// String title = (String)JOptionPane.showInputDialog(
	// this,
	// "Name the new note:\n",
	// "Create note",
	// JOptionPane.PLAIN_MESSAGE,
	// null,
	// null,
	// "New note");
	//
	// // If a string was returned, check it
	// if ((title == null) || (title.length() == 0)) {
	// Logs.log("New note title is unvalid", 1, this);
	// JEXStatics.statusBar.setStatusText("Error creating note: Name invalid");
	// return;
	// }
	//
	// // Get a random entry
	// if(JEXStatics.jexManager.getCurrentDatabase().getFilteredEntries().size()
	// == 0) return;
	// JEXEntry entry =
	// JEXStatics.jexManager.getCurrentDatabase().getFilteredEntries().first();
	//
	// // Create the note file
	// String note = "Created "+DateUtility.getDate()+"\n"+"\n";
	// String folder = null;
	// if (type.equals("Database"))
	// {
	// folder = JEXWriter.getAttachedFolderPath(entry, "", true, false);
	// }
	// else if (type.equals("Experiment"))
	// {
	// folder = JEXWriter.getAttachedFolderPath(entry, JEXEntry.EXPERIMENT,
	// true, false);
	// }
	// else if (type.equals("Array"))
	// {
	// folder = JEXWriter.getAttachedFolderPath(entry, JEXEntry.TRAY, true,
	// false);
	// }
	//
	// // If the folder is wrong exit
	// File ffolder = new File(folder);
	// if (folder == null || !ffolder.exists())
	// {
	// Logs.log("Cannot create note, wrong folder", 1, this);
	// JEXStatics.statusBar.setStatusText("Error creating note");
	// return;
	// }
	//
	// // Make the full path
	// String path = folder + "/_" + title+".rtf";
	// File fPath = new File(path);
	// if (fPath.exists())
	// {
	// Logs.log("Cannot create note, name already in use", 1,
	// this);
	// JEXStatics.statusBar.setStatusText("Error creating note: Name already used");
	// return;
	// }
	//
	// // Create the file and rebuild
	// DefaultStyledDocument doc = new DefaultStyledDocument();
	// try
	// {
	// doc.insertString(0, note, null);
	// RTFEditorKit kit = new RTFEditorKit();
	// FileOutputStream fos = new FileOutputStream(fPath);
	// kit.write(fos, doc, 0, doc.getLength());
	// fos.flush();
	// fos.close();
	// } catch (BadLocationException e)
	// {
	//
	// } catch (FileNotFoundException e)
	// {
	//
	// } catch (IOException e)
	// {
	//
	// }
	//
	//
	// // Rebuild
	// rebuildModel();
	// rebuildGUI();
	// }
	//
	// //////
	// ////// EVENTS
	// //////
	//
	// public void actionPerformed(ActionEvent e)
	// {
	// if (e.getSource() instanceof FlatRoundedStaticButton)
	// {
	// String name =
	// ((FlatRoundedStaticButton)e.getSource()).panel().getToolTipText();
	// String type = name.substring(17);
	// makeNewNote(type);
	// }
	// }
	//
	// public void mouseClicked(MouseEvent arg0) {}
	//
	// public void mouseEntered(MouseEvent arg0) {}
	//
	// public void mouseExited(MouseEvent arg0) {}
	//
	// public void mousePressed(MouseEvent arg0) {}
	//
	// public void mouseReleased(MouseEvent arg0)
	// {
	// RichJLabel label = (RichJLabel)arg0.getSource();
	// String text = label.getText();
	// String path = label.getPath();
	//
	// Logs.log("Opening note "+text+" at path "+path, 1,
	// this);
	// notePanel.setNoteViewed(new File(path));
	// }
	//
	// class RichJLabel extends JLabel
	// {
	// private static final long serialVersionUID = 1L;
	// private String path = null;
	//
	// RichJLabel(String title)
	// {
	// super(title);
	// }
	//
	// public void setPath(String path)
	// {
	// this.path = path;
	// }
	//
	// public String getPath()
	// {
	// return this.path;
	// }
	//
	// }
}
