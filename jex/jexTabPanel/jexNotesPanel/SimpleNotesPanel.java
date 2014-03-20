package jex.jexTabPanel.jexNotesPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import miscellaneous.FontUtility;

public class SimpleNotesPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	private JPanel titlepane;
	private JPanel notespane;
	private JLabel notesLabel;
	
	private TextComponent editor;
	
	private File notesFile;
	private String title;
	
	public SimpleNotesPanel(String title)
	{
		this.title = title;
		editor = new TextComponent();
		this.initialize();
	}
	
	public void setNoteViewed(File notesFile)
	{
		this.notesFile = notesFile;
		
		notespane.setLayout(new BorderLayout());
		notespane.removeAll();
		
		if(notesFile == null)
		{
			notesLabel.setText("No notes");
			editor.open(null);
		}
		else
		{
			notesLabel.setText(notesFile.getName());
			if(notesFile.exists())
				editor.open(this.notesFile);
			else
				editor.open(null);
		}
		
		notespane.add(editor, BorderLayout.CENTER);
		notespane.invalidate();
		notespane.validate();
		this.repaint();
	}
	
	private void initialize()
	{
		titlepane = new JPanel();
		titlepane.setBackground(DisplayStatics.lightBackground);
		titlepane.setLayout(new BoxLayout(titlepane, BoxLayout.LINE_AXIS));
		titlepane.setMaximumSize(new Dimension(1000, 30));
		titlepane.setPreferredSize(new Dimension(100, 30));
		
		notesLabel = new JLabel(title);
		notesLabel.setFont(FontUtility.boldFont);
		notesLabel.setAlignmentY(CENTER_ALIGNMENT);
		titlepane.add(notesLabel);
		titlepane.add(Box.createHorizontalGlue());
		
		notespane = new JPanel();
		notespane.setBackground(DisplayStatics.lightBackground);
		notespane.setLayout(new BoxLayout(notespane, BoxLayout.PAGE_AXIS));
		
		this.setBackground(DisplayStatics.background);
		this.setLayout(new BorderLayout());
		this.add(titlepane, BorderLayout.PAGE_START);
		this.add(notespane, BorderLayout.CENTER);
	}
}
