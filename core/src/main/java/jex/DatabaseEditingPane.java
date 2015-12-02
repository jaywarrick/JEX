package jex;

import Database.SingleUserDatabase.JEXDBInfo;
import Database.SingleUserDatabase.JEXWriter;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jex.statics.DisplayStatics;
import logs.Logs;

public class DatabaseEditingPane extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	// private JTextField repField = new JTextField() ;
	private JTextField nameField = new JTextField();
	private JTextField infoField = new JTextField();
	private JTextField passField = new JTextField();
	// private Repository rep;
	private JEXDBInfo db;
	private JEXDatabaseChooser parent;
	private JButton doItButton;
	
	public DatabaseEditingPane(JEXDatabaseChooser parent, JEXDBInfo db)
	{
		this.parent = parent;
		this.db = db;
		
		// initialize
		initialize();
	}
	
	/**
	 * Initialize
	 */
	private void initialize()
	{
		// Make the button
		doItButton = new JButton("YES");
		doItButton.addActionListener(this);
		
		// Make the form layout
		ColumnSpec column1 = new ColumnSpec(ColumnSpec.FILL, Sizes.dluX(70), FormSpec.NO_GROW);
		ColumnSpec column2 = new ColumnSpec(ColumnSpec.FILL, Sizes.dluX(100), FormSpec.DEFAULT_GROW);
		ColumnSpec column3 = new ColumnSpec(ColumnSpec.FILL, Sizes.dluX(50), FormSpec.NO_GROW);
		ColumnSpec[] cspecs = new ColumnSpec[] { column1, column2, column3 };
		
		RowSpec row1 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row2 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row3 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row4 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row5 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row6 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec[] rspecs = new RowSpec[] { row1, row2, row3, row4, row5, row6 };
		
		FormLayout layout = new FormLayout(cspecs, rspecs);
		
		CellConstraints cc = new CellConstraints();
		this.setLayout(layout);
		this.setBackground(DisplayStatics.lightBackground);
		
		JLabel nameLabel = new JLabel("Name");
		this.add(nameLabel, cc.xy(1, 2));
		this.add(nameField, cc.xywh(2, 2, 2, 1));
		nameField.setText(db.getDBName());
		
		JLabel infoLabel = new JLabel("Info");
		this.add(infoLabel, cc.xy(1, 3));
		this.add(infoField, cc.xywh(2, 3, 2, 1));
		infoField.setText(db.get(JEXDBInfo.DB_INFO));
		
		JLabel passLabel = new JLabel("Password");
		this.add(passLabel, cc.xy(1, 4));
		this.add(passField, cc.xywh(2, 4, 2, 1));
		
		this.add(doItButton, cc.xy(2, 6));
	}
	
	// ----------------------------------------------------
	// --------- EVENT HANDLING FUNCTIONS -----------------
	// ----------------------------------------------------
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == doItButton)
		{
			Logs.log("Database edition validated", 1, this);
			// String rep = repField.getText();
			String name = nameField.getText();
			String info = infoField.getText();
			String password = passField.getText();
			
			JEXWriter.editDBInfo(this.db, name, info, password);
			
			// Reset the datrabase chooser
			parent.setAlternatePanel(null);
			Logs.log("Database information changed: [Name = " + name + "] [Info = " + info + "] [Pass = " + password + "]", 1, this);
		}
	}
}
