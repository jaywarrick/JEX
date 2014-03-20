package preferences;

import javax.swing.table.TableCellRenderer;

/**
 * Factory for Property renderers.<br>
 */
public interface PropertyRendererFactory {
	
	TableCellRenderer createTableCellRenderer(Property property);
	
	@SuppressWarnings("rawtypes")
	TableCellRenderer createTableCellRenderer(Class type);
	
}
