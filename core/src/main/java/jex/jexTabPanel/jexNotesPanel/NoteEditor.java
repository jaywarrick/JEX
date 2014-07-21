package jex.jexTabPanel.jexNotesPanel;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import logs.Logs;

public class NoteEditor extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	// ... Components
	private JTextArea editArea;
	
	// variables
	private File openFile;
	
	public NoteEditor()
	{
		// ... Create scrollable text area.
		editArea = new JTextArea(15, 80);
		editArea.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		editArea.setFont(new Font("monospaced", Font.PLAIN, 11));
		JScrollPane scrollingText = new JScrollPane(editArea);
		
		// -- Create a content pane, set layout, add component.
		this.setLayout(new BorderLayout());
		this.add(scrollingText, BorderLayout.CENTER);
	}
	
	public void open(File file)
	{
		try
		{
			FileReader reader = new FileReader(file);
			editArea.read(reader, "");
			openFile = file;
		}
		catch (FileNotFoundException e)
		{
			Logs.log("ERROR IN OPENING NOTES FILE", 1, this);
		}
		catch (IOException e)
		{
			Logs.log("ERROR IN OPENING NOTES FILE", 1, this);
		}
	}
	
	public void save()
	{
		try
		{
			FileWriter writer = new FileWriter(openFile);
			editArea.write(writer); // Use TextComponent write
		}
		catch (IOException ioex)
		{
			Logs.log("ERROR IN SAVING NOTES FILE", 1, this);
		}
	}
	
}
