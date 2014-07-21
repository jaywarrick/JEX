package Exporter;

import Database.DBObjects.JEXEntry;
import Database.Definition.TypeName;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;

import guiObject.DialogGlassCenterPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeSet;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;

public class ExporterOptionPanel extends DialogGlassCenterPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private JComboBox savingBox = new JComboBox(new String[] { "Yes", "No" });
	private JComboBox exportMode = new JComboBox(new String[] { "2D", "1D" });
	private JTextField nameField = new JTextField();
	private JTextField infoField = new JTextField();
	
	private JTextField minValueField = new JTextField();
	private JTextField meanValueField = new JTextField();
	private JTextField maxValueField = new JTextField();
	private JCheckBox makePercent = new JCheckBox("Percent variation");
	
	// variables
	private String mode;
	
	public ExporterOptionPanel(String mode)
	{
		this.mode = mode;
		
		// initialize
		initialize();
	}
	
	/**
	 * Initialize
	 */
	private void initialize()
	{
		// Make the form layout
		ColumnSpec column1 = new ColumnSpec(ColumnSpec.FILL, Sizes.dluX(70), FormSpec.NO_GROW);
		ColumnSpec column2 = new ColumnSpec(ColumnSpec.FILL, Sizes.dluX(100), FormSpec.DEFAULT_GROW);
		ColumnSpec[] cspecs = new ColumnSpec[] { column1, column2 };
		
		RowSpec row1 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row2 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row3 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row4 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row5 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row6 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row7 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row8 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row9 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row10 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec[] rspecs = new RowSpec[] { row1, row2, row3, row4, row5, row6, row7, row8, row9, row10 };
		
		FormLayout layout = new FormLayout(cspecs, rspecs);
		
		CellConstraints cc = new CellConstraints();
		this.setLayout(layout);
		this.setBackground(DisplayStatics.lightBackground);
		
		// Fill the layout
		JLabel savingLabel = new JLabel("Save Object");
		this.add(savingLabel, cc.xy(1, 1));
		this.add(savingBox, cc.xy(2, 1));
		
		JLabel modeLabel = new JLabel("Export mode");
		this.add(modeLabel, cc.xy(1, 2));
		this.add(exportMode, cc.xy(2, 2));
		
		JLabel nameLabel = new JLabel("Name");
		this.add(nameLabel, cc.xy(1, 3));
		this.add(nameField, cc.xy(2, 3));
		nameField.setText("New object");
		
		JLabel infoLabel = new JLabel("Info");
		this.add(infoLabel, cc.xy(1, 4));
		this.add(infoField, cc.xy(2, 4));
		infoField.setText("No info yet");
		
		JLabel minValueFieldLabel = new JLabel("Min");
		this.add(minValueFieldLabel, cc.xy(1, 6));
		this.add(minValueField, cc.xy(2, 6));
		minValueField.setText("MIN");
		minValueField.setToolTipText("Set to MIN to use the minimum value of the dataset");
		
		JLabel meanValueFieldLabel = new JLabel("Mean");
		this.add(meanValueFieldLabel, cc.xy(1, 7));
		this.add(meanValueField, cc.xy(2, 7));
		meanValueField.setText("MEAN");
		meanValueField.setToolTipText("Set to MEAN to use the mean value of the dataset");
		
		JLabel maxValueFieldLabel = new JLabel("Max");
		this.add(maxValueFieldLabel, cc.xy(1, 8));
		this.add(maxValueField, cc.xy(2, 8));
		maxValueField.setText("MAX");
		maxValueField.setToolTipText("Set to MAX to use the maximum value of the dataset");
		
		this.add(makePercent, cc.xy(1, 9));
		makePercent.setToolTipText("If checked, values will be displayed as percent variation around the mean");
	}
	
	/**
	 * Called when clicked yes on the dialog panel
	 */
	@Override
	public void yes()
	{
		Logs.log("Exporting data ", 1, this);
		
		// Retrieve the info
		String name = nameField.getText();
		String info = infoField.getText();
		String saving = savingBox.getSelectedItem().toString();
		String exportmode = exportMode.getSelectedItem().toString();
		Boolean savingb = (saving.equals("Yes")) ? true : false;
		Integer exportModeInt = Exporter.EXPORT2D;
		if(exportmode.equals("1D"))
			exportModeInt = Exporter.EXPORTLIST;
		
		String minStr = minValueField.getText();
		String meanStr = meanValueField.getText();
		String maxStr = maxValueField.getText();
		Double min = (minStr.equals("MIN")) ? Double.NaN : new Double(minStr);
		Double mean = (meanStr.equals("MEAN")) ? Double.NaN : new Double(meanStr);
		Double max = (maxStr.equals("MAX")) ? Double.NaN : new Double(maxStr);
		
		boolean percent = makePercent.isSelected();
		
		TreeSet<JEXEntry> entries = JEXStatics.jexManager.getSelectedEntries();
		TypeName tn = JEXStatics.jexManager.getSelectedObject();
		if(entries.size() > 0 && tn != null)
		{
			Exporter export = new Exporter(mode, exportModeInt, savingb, name, info);
			export.setRange(min, mean, max);
			export.setMakePercentVariation(percent);
			export.setListDataToExport(entries, tn);
			export.export();
		}
	}
	
	/**
	 * Called when clicked cancel on the dialog panel
	 */
	@Override
	public void cancel()
	{
		Logs.log("Data export cancelled", 1, this);
	}
	
	// ----------------------------------------------------
	// --------- EVENT HANDLING FUNCTIONS -----------------
	// ----------------------------------------------------
	
	public void actionPerformed(ActionEvent e)
	{}
}
