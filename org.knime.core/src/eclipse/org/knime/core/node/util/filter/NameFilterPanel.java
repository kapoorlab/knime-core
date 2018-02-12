/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.util.filter;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableRowSorter;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.DataColumnSpecTableCellRenderer;
import org.knime.core.node.util.filter.NameFilterConfiguration.EnforceOption;

/**
 * Name filter panel with additional enforce include/exclude radio buttons.
 *
 * @author Thomas Gabriel, KNIME AG, Zurich, Switzerland
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 *
 * @since 2.6
 *
 * @param <T> the instance T this object is parametrized on
 */
@SuppressWarnings("serial")
public abstract class NameFilterPanel<T> extends JPanel {

    /** Name for the filter by name type. */
    private static final String NAME = "Manual Selection";

    /** Line border for include names. */
    private static final Border INCLUDE_BORDER = BorderFactory.createLineBorder(new Color(0, 221, 0), 2);

    /** Line border for exclude names. */
    private static final Border EXCLUDE_BORDER = BorderFactory.createLineBorder(new Color(240, 0, 0), 2);

    /** Include list. */
    private final JTable m_inclTable;

    /** Include model. */
    @SuppressWarnings("rawtypes")
    private final MyTableModel m_inclMdl;

    /** Include sorter. */
    @SuppressWarnings("rawtypes")
    private final TableRowSorter<MyTableModel> m_inclSorter;

    /** Include cards. */
    private final JPanel m_inclCards;

    /** Include table placeholder. */
    private final TablePlaceholder m_inclTablePlaceholder;

    /** Exclude list. */
    private final JTable m_exclTable;

    /** Exclude model. */
    @SuppressWarnings("rawtypes")
    private final MyTableModel m_exclMdl;

    /** Exclude sorter. */
    @SuppressWarnings("rawtypes")
    private final TableRowSorter<MyTableModel> m_exclSorter;

    /** Include cards. */
    private final JPanel m_exclCards;

    /** Include table placeholder. */
    private final TablePlaceholder m_exclTablePlaceholder;

    /** Radio button for the exclusion option. */
    private final JRadioButton m_enforceExclusion;

    /** Radio button for the inclusion option. */
    private final JRadioButton m_enforceInclusion;

    /** Remove all button. */
    private final JButton m_remAllButton;

    /** Remove button. */
    private final JButton m_remButton;

    /** Add all button. */
    private final JButton m_addAllButton;

    /** Add button. */
    private final JButton m_addButton;

    /** Search Field in include list. */
    private final JTextField m_inclSearchField;

    /** Search Field in exclude list. */
    private final JTextField m_exclSearchField;

    /** List of T elements to keep initial ordering of names. */
    private final LinkedHashSet<T> m_order = new LinkedHashSet<T>();

    /** Border of the include panel, keep it so we can change the title. */
    private final TitledBorder m_includeBorder;

    /** Border of the include panel, keep it so we can change the title. */
    private final TitledBorder m_excludeBorder;

    private final HashSet<T> m_hideNames = new HashSet<T>();

    private List<ChangeListener> m_listeners;

    /** The filter used to filter out/in valid elements. */
    private InputFilter<T> m_filter;

    private String m_currentType = NameFilterConfiguration.TYPE;

    private ButtonGroup m_typeGroup;

    private JPanel m_typePanel;

    private JPanel m_filterPanel;

    private JPanel m_nameFilterPanel;

    /** Constants for updating different parts of the UI*/
    private static final String INCLUDE = "INCLUDE";
    private static final String EXCLUDE = "EXCLUDE";
    private static final String EMPTY = "EMPTY";
    private static final String NOTHING_FOUND = "NOTHING_FOUND";
    private static final String PLACEHOLDER = "PLACEHOLDER";
    private static final String LIST = "LIST";

    /** Text to be displayed in the filter as a placeholder */
    private static final String FILTER = "Filter";

    /**
     * additional checkbox for the middle button panel
     * @since 3.4
     */
    private JCheckBox m_additionalCheckbox;

    private PatternFilterPanel<T> m_patternPanel;

    private JRadioButton m_nameButton;

    private JRadioButton m_patternButton;

    private TreeMap<Integer, String> m_typePriorities = new TreeMap<Integer, String>();

    private List<String> m_invalidIncludes = new ArrayList<String>(0);

    private List<String> m_invalidExcludes = new ArrayList<String>(0);

    private String[] m_availableNames = new String[0];

    /**
     * Creates a panel allowing the user to select elements.
     */
    protected NameFilterPanel() {
        this(false, null);
    }

    /**
     * Creates a new filter panel with three component which are the include list, button panel to shift elements
     * between the two lists, and the exclude list. The include list then will contain all values to filter.
     *
     * @param showSelectionListsOnly if set, the component shows only the basic include/exclude selection panel - no
     *            additional search boxes, force-include-options, etc.
     */
    protected NameFilterPanel(final boolean showSelectionListsOnly) {
        this(showSelectionListsOnly, null);
    }

    /**
     * Creates a new filter column panel with three component which are the include list, button panel to shift elements
     * between the two lists, and the exclude list. The include list then will contain all values to filter.
     * Additionally a {@link InputFilter} can be specified, based on which the shown items are shown or not. The filter
     * can be <code>null
     * </code>, in which case it is simply not used at all.
     *
     * @param showSelectionListsOnly if set, the component shows only the basic include/exclude selection panel - no
     *            additional search boxes, force-include-options, etc.
     * @param filter A filter that specifies which items are shown in the panel (and thus are possible to include or
     *            exclude) and which are not shown.
     */
    protected NameFilterPanel(final boolean showSelectionListsOnly, final InputFilter<T> filter) {
        this(showSelectionListsOnly, filter, null);
    }

    /**
     * Placeholder being displayed instead of table when search matches no items
     *
     * @author Johannes Schweig
     */
    private class TablePlaceholder extends JLabel{

        TablePlaceholder(){
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.TOP);
            setFont(getFont().deriveFont(Font.ITALIC));
            setForeground(Color.GRAY);
        }

        /**
         * Updates the labels text with the specified searchString
         * @param searchString term that was searched for
         */
        private void updateText(final String mode, final String searchString, final int total){
            // empty list/table
            if (mode.equals(EMPTY)) {
                setText("No columns in this list");
            } else if (mode.equals(NOTHING_FOUND)) {
                String str = searchString;
                // shorten string if too long
                int max = 15;
                if (str.length() > max){
                    str = str.substring(0, max) + "...";
                }
                setText("<html>No columns found matching<br>\""+str+"\" (total: " + total + ")</html>");
            }
        }

    }

    /**
     * Creates a new filter column panel with three component which are the include list, button panel to shift elements
     * between the two lists, and the exclude list. The include list then will contain all values to filter.
     * Additionally a {@link InputFilter} can be specified, based on which the shown items are shown or not. The filter
     * can be <code>null</code>, in which case it is simply not used at all.
     *
     * @param showSelectionListsOnly if set, the component shows only the basic include/exclude selection panel - no
     *            additional search boxes, force-include-options, etc.
     * @param filter A filter that specifies which items are shown in the panel (and thus are possible to include or
     *            exclude) and which are not shown.
     * @param searchLabel text to show next to the search fields
     * @since 3.4
     */
    @SuppressWarnings({"rawtypes"})
    protected NameFilterPanel(final boolean showSelectionListsOnly, final InputFilter<T> filter,
        final String searchLabel) {
        super(new GridLayout(1, 1));
        m_filter = filter;
        m_patternPanel = getPatternFilterPanel(filter);
        m_patternPanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                fireFilteringChangedEvent();
            }
        });
        m_patternButton = createButtonToFilterPanel(PatternFilterConfiguration.TYPE, "Wildcard/Regex Selection");

        // keeps buttons such add 'add', 'add all', 'remove', and 'remove all'
        final JPanel buttonPan = new JPanel();
        buttonPan.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        buttonPan.setLayout(new BoxLayout(buttonPan, BoxLayout.Y_AXIS));
        buttonPan.add(Box.createVerticalStrut(57));

        // path for images
        Package pack = NameFilterPanel.class.getPackage();
        String iconBase = pack.getName().replace(".", "/") + "/";
        URL filterUrl = this.getClass().getClassLoader().getResource(iconBase + "filter.png");
        URL addUrl = this.getClass().getClassLoader().getResource(iconBase + "add.png");
        URL addAllUrl = this.getClass().getClassLoader().getResource(iconBase + "add_all.png");
        URL remUrl = this.getClass().getClassLoader().getResource(iconBase + "rem.png");
        URL remAllUrl = this.getClass().getClassLoader().getResource(iconBase + "rem_all.png");

        // include list
        m_inclMdl = new MyTableModel();
        m_inclTable = new JTable(m_inclMdl);
        m_inclTable.setShowGrid(false);
        m_inclTable.setTableHeader(null);
        m_inclTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_inclTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent me) {
                if (me.getClickCount() == 2) {
                    onRemIt(m_inclTable.getSelectedRows());
                    me.consume();
                }
            }
        });
        m_inclTable.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(final KeyEvent e) {
                // find first column starting with typed character
                String key = String.valueOf(e.getKeyChar());
                int index = -1;
                for (int i = 0; i < m_inclTable.getRowCount(); i++) {
                    String s = ((DataColumnSpec) m_inclTable.getValueAt(i, 0)).getName();
                    if (s.toLowerCase().startsWith(key)){
                        index = i;
                        break;
                    }
                }
                // if a column is found, select it and scroll the view
                if (index != -1) {
                    m_inclTable.setRowSelectionInterval(index, index);
                    m_inclTable.scrollRectToVisible(new Rectangle(m_inclTable.getCellRect(index, 0, true)));
                }
            }

            @Override
            public void keyReleased(final KeyEvent e) { }

            @Override
            public void keyPressed(final KeyEvent e) { }
        });

        m_inclSorter = new TableRowSorter<MyTableModel>(m_inclMdl);
        m_inclTable.setRowSorter(m_inclSorter);
        // removes selection of exclude table when include table gains focus
        m_inclTable.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(final FocusEvent e) { }

            @Override
            public void focusGained(final FocusEvent e) {
                m_exclTable.clearSelection();
            }
        });
        final JScrollPane jspIncl = new JScrollPane(m_inclTable);
        jspIncl.setPreferredSize(new Dimension(250, 100));
        // setup cardlayout for display of placeholder on search returning no results
        m_inclCards = new JPanel(new CardLayout());
        m_inclCards.setBorder(new EmptyBorder(0, 8, 0, 8));
        m_inclTablePlaceholder = new TablePlaceholder();
        m_inclCards.add(jspIncl, LIST);
        m_inclCards.add(m_inclTablePlaceholder, PLACEHOLDER);

        m_inclSearchField = new JTextField(FILTER, 8);
        m_inclSearchField.setForeground(Color.GRAY);
        m_inclSearchField.setFont(getFont().deriveFont(Font.ITALIC, 14f));
        // reset jtable when searchfield is empty
        m_inclSearchField.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(final KeyEvent e) { }

            @Override
            public void keyReleased(final KeyEvent e) {
                updateRowFilter(INCLUDE);
                updateTablePlaceholder(INCLUDE);
            }

            @Override
            public void keyPressed(final KeyEvent e) { }
        });
        m_inclSearchField.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(final FocusEvent e) {
               updateTextFieldPlaceholder(INCLUDE, false);
            }

            @Override
            public void focusGained(final FocusEvent e) {
                updateTextFieldPlaceholder(INCLUDE, true);
            }
        });
        JPanel inclSearchPanel = new JPanel(new BorderLayout(8, 0));
        inclSearchPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        inclSearchPanel.add(m_inclSearchField, BorderLayout.CENTER);
        // filter icon
        inclSearchPanel.add(new JLabel(new ImageIcon(filterUrl)), BorderLayout.WEST);

        JPanel includePanel = new JPanel(new BorderLayout());
        m_includeBorder = BorderFactory.createTitledBorder(INCLUDE_BORDER, " Include ");
        includePanel.setBorder(m_includeBorder);
        includePanel.add(inclSearchPanel, BorderLayout.NORTH);
        includePanel.add(m_inclCards, BorderLayout.CENTER);

        // exclude list
        m_exclMdl = new MyTableModel();
        m_exclTable = new JTable(m_exclMdl);
        m_exclTable.setShowGrid(false);
        m_exclTable.setTableHeader(null);
        m_exclTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_exclTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent me) {
                if (me.getClickCount() == 2) {
                    onAddIt(m_exclTable.getSelectedRows());
                    me.consume();
                }
            }
        });
        m_exclTable.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(final KeyEvent e) {
                // find first column starting with typed character
                String key = String.valueOf(e.getKeyChar());
                int index = -1;
                for (int i = 0; i < m_exclTable.getRowCount(); i++) {
                    String s = ((DataColumnSpec) m_exclTable.getValueAt(i, 0)).getName();
                    if (s.toLowerCase().startsWith(key)){
                        index = i;
                        break;
                    }
                }
                // if a column is found, select it and scroll the view
                if (index != -1) {
                    m_exclTable.setRowSelectionInterval(index, index);
                    m_exclTable.scrollRectToVisible(new Rectangle(m_exclTable.getCellRect(index, 0, true)));
                }
            }

            @Override
            public void keyReleased(final KeyEvent e) { }

            @Override
            public void keyPressed(final KeyEvent e) { }
        });
        // set renderer for items in the in- and exclude list
        m_inclTable.setDefaultRenderer(Object.class, new DataColumnSpecTableCellRenderer());
        m_exclTable.setDefaultRenderer(Object.class, new DataColumnSpecTableCellRenderer());
        m_exclSorter = new TableRowSorter<MyTableModel>(m_exclMdl);
        m_exclTable.setRowSorter(m_exclSorter);
        // removes selection of include table when exclude table gains focus
        m_exclTable.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(final FocusEvent e) { }

            @Override
            public void focusGained(final FocusEvent e) {
                m_inclTable.clearSelection();
            }
        });
        final JScrollPane jspExcl = new JScrollPane(m_exclTable);
        jspExcl.setPreferredSize(new Dimension(250, 100));
        // setup cardlayout for display of placeholder on search returning no results
        m_exclCards = new JPanel(new CardLayout());
        m_exclCards.setBorder(new EmptyBorder(0, 8, 0, 8));
        m_exclTablePlaceholder = new TablePlaceholder();
        m_exclCards.add(jspExcl, LIST);
        m_exclCards.add(m_exclTablePlaceholder, PLACEHOLDER);

        m_exclSearchField = new JTextField(FILTER, 8);
        m_exclSearchField.setForeground(Color.GRAY);
        m_exclSearchField.setFont(getFont().deriveFont(Font.ITALIC, 14f));
        // reset jtable when searchfield is empty
        m_exclSearchField.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(final KeyEvent e) { }

            @Override
            public void keyReleased(final KeyEvent e) {
                updateRowFilter(EXCLUDE);
                updateTablePlaceholder(EXCLUDE);
            }

            @Override
            public void keyPressed(final KeyEvent e) { }
        });

        m_exclSearchField.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(final FocusEvent e) {
                updateTextFieldPlaceholder(EXCLUDE, false);
            }

            @Override
            public void focusGained(final FocusEvent e) {
                updateTextFieldPlaceholder(EXCLUDE, true);
            }
        });

        JPanel exclSearchPanel = new JPanel(new BorderLayout(8, 0));
        exclSearchPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        exclSearchPanel.add(m_exclSearchField, BorderLayout.CENTER);
        // filter icon
        exclSearchPanel.add(new JLabel(new ImageIcon(filterUrl)), BorderLayout.WEST);
        JPanel excludePanel = new JPanel(new BorderLayout());
        m_excludeBorder = BorderFactory.createTitledBorder(EXCLUDE_BORDER, " Exclude ");
        excludePanel.setBorder(m_excludeBorder);
        excludePanel.add(exclSearchPanel, BorderLayout.NORTH);
        excludePanel.add(m_exclCards, BorderLayout.CENTER);


        // add force incl/excl buttons
        m_enforceInclusion = new JRadioButton("Enforce inclusion");
        m_enforceInclusion.setBorder(new EmptyBorder(8, 8, 8, 8));
        m_enforceExclusion = new JRadioButton("Enforce exclusion");
        m_enforceExclusion.setBorder(new EmptyBorder(8, 8, 8, 8));
        m_enforceInclusion.addActionListener(e -> cleanInvalidValues());
        m_enforceExclusion.addActionListener(e -> cleanInvalidValues());
        if (!showSelectionListsOnly) {
            final ButtonGroup forceGroup = new ButtonGroup();
            m_enforceInclusion.setToolTipText("Force the set of included names to stay the same.");
            forceGroup.add(m_enforceInclusion);
            includePanel.add(m_enforceInclusion, BorderLayout.SOUTH);
            m_enforceExclusion.setToolTipText("Force the set of excluded names to stay the same.");
            forceGroup.add(m_enforceExclusion);
            m_enforceExclusion.doClick();
            excludePanel.add(m_enforceExclusion, BorderLayout.SOUTH);
        }
        m_addButton = new JButton(new ImageIcon(addUrl));
        m_addButton.setMaximumSize(new Dimension(125, 25));
        m_addButton.setToolTipText("Move the selected entries from the left to the right list.");
        buttonPan.add(m_addButton);
        m_addButton.addActionListener(e -> onAddIt(m_exclTable.getSelectedRows()));
        buttonPan.add(Box.createVerticalStrut(25));

        m_addAllButton = new JButton(new ImageIcon(addAllUrl));
        m_addAllButton.setMaximumSize(new Dimension(125, 25));
        m_addAllButton.setToolTipText("Moves all visible entries from the left to the right list.");
        buttonPan.add(m_addAllButton);
        m_addAllButton.addActionListener(e -> {
            // if table is not filtered
            if(m_exclTable.getRowSorter()==null){
                onAddAll();
            }else{ //if table is filtered
                int[] rows = IntStream.range(0, m_exclTable.getRowCount()).toArray();
                onAddIt(rows);
            }
        });
        buttonPan.add(Box.createVerticalStrut(25));

        m_remButton = new JButton(new ImageIcon(remUrl));
        m_remButton.setMaximumSize(new Dimension(125, 25));
        m_remButton.setToolTipText("Move the selected entries from the right to the left list.");
        buttonPan.add(m_remButton);
        m_remButton.addActionListener(e -> onRemIt(m_inclTable.getSelectedRows()));
        buttonPan.add(Box.createVerticalStrut(25));

        m_remAllButton = new JButton(new ImageIcon(remAllUrl));
        m_remAllButton.setMaximumSize(new Dimension(125, 25));
        m_remAllButton.setToolTipText("Moves all visible entries from the right to the left list.");
        buttonPan.add(m_remAllButton);
        m_remAllButton.addActionListener(e -> {
            // if table is not filtered
            if(m_inclTable.getRowSorter()==null){
                onRemAll();
            }else{ //if table is filtered
                int[] rows = IntStream.range(0, m_inclTable.getRowCount()).toArray();
                onRemIt(rows);
            }
        });
        m_additionalCheckbox = createAdditionalButton();
        if (m_additionalCheckbox != null){
            buttonPan.add(Box.createVerticalStrut(25));
            m_additionalCheckbox.setMaximumSize(new Dimension(125, 25));
            buttonPan.add(m_additionalCheckbox);
            m_additionalCheckbox.addActionListener(e -> fireFilteringChangedEvent());
        }
        buttonPan.add(Box.createVerticalStrut(20));
        buttonPan.add(Box.createGlue());
        // adds include, button, exclude component
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
        center.add(excludePanel);
        center.add(buttonPan);
        center.add(includePanel);
        m_nameFilterPanel = center;
        initPanel();
    }

    /**
     * @param filter
     * @return the PatternFilterPanel to be used.
     * @since 3.4
     * @noreference This method is not intended to be referenced by clients outside the KNIME core.
     */
    protected PatternFilterPanel<T> getPatternFilterPanel(final InputFilter<T> filter) {
        return new PatternFilterPanel<T>(this, filter);
    }

    /**
     * @return an additional button to be added to the center panel. To be overwritten by subclasses
     * @since 3.4
     * @nooverride This method is not intended to be re-implemented or extended by clients outside KNIME core.
     */
    protected JCheckBox createAdditionalButton(){
        return null;
    }

    /** The additional button as created by {@link #createAdditionalButton()} or an empty optional if the method
     * was not overridden.
     * @return the additionalCheckbox
     * @since 3.4
     */
    protected final Optional<JCheckBox> getAdditionalButton() {
        return Optional.ofNullable(m_additionalCheckbox);
    }

    /** @return a list cell renderer from items to be rendered in the filer */
    @SuppressWarnings("rawtypes")
    protected abstract ListCellRenderer getListCellRenderer();

    /**
     * Get the a T for the given name.
     *
     * @param name a string to retrieve T for.
     * @return an instance of T
     */
    protected abstract T getTforName(final String name);

    /**
     * Returns the name for the given T.
     *
     * @param t to retrieve the name for
     * @return the name represented by T
     */
    protected abstract String getNameForT(final T t);

    /**
     * Enables or disables all components on this panel. {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        m_inclSearchField.setEnabled(enabled);
        m_exclSearchField.setEnabled(enabled);
        m_inclTable.setEnabled(enabled);
        m_exclTable.setEnabled(enabled);
        m_remAllButton.setEnabled(enabled);
        m_remButton.setEnabled(enabled);
        m_addAllButton.setEnabled(enabled);
        m_addButton.setEnabled(enabled);
        m_enforceInclusion.setEnabled(enabled);
        m_enforceExclusion.setEnabled(enabled);
        if (m_additionalCheckbox != null) {
            m_additionalCheckbox.setEnabled(enabled);
        }
        Enumeration<AbstractButton> buttons = m_typeGroup.getElements();
        while (buttons.hasMoreElements()) {
            buttons.nextElement().setEnabled(enabled);
        }
        m_patternPanel.setEnabled(enabled);
    }

    /**
     * Adds a listener which gets informed whenever the column filtering changes.
     *
     * @param listener the listener
     */
    public void addChangeListener(final ChangeListener listener) {
        if (m_listeners == null) {
            m_listeners = new ArrayList<ChangeListener>();
        }
        m_listeners.add(listener);
    }

    /**
     * Removes the given listener from this filter column panel.
     *
     * @param listener the listener.
     */
    public void removeChangeListener(final ChangeListener listener) {
        if (m_listeners != null) {
            m_listeners.remove(listener);
        }
    }

    /**
     * Removes all column filter change listener.
     */
    public void removeAllColumnFilterChangeListener() {
        if (m_listeners != null) {
            m_listeners.clear();
        }
    }

    /**
     * Updates this filter panel by removing all current selections from the include and exclude list. The include list
     * will contains all column names from the spec afterwards.
     *
     * @param config to be loaded from
     * @param names array of names to be included or excluded (preserve order)
     */
    public void loadConfiguration(final NameFilterConfiguration config, final String[] names) {
        final List<String> ins = Arrays.asList(config.getIncludeList());
        final List<String> exs = Arrays.asList(config.getExcludeList());
        if (supportsInvalidValues()) {
            m_invalidIncludes = new ArrayList<String>(Arrays.asList(config.getRemovedFromIncludeList()));
            m_invalidExcludes = new ArrayList<String>(Arrays.asList(config.getRemovedFromExcludeList()));
        }
        this.update(ins, exs, names);
        switch (config.getEnforceOption()) {
            case EnforceExclusion:
                m_enforceExclusion.doClick();
                break;
            case EnforceInclusion:
                m_enforceInclusion.doClick();
                break;
        }
        m_patternPanel.loadConfiguration(config.getPatternConfig(), names);
        setPatternFilterEnabled(config.isPatternFilterEnabled());
        m_currentType = config.getType();
        boolean typeOk = false;
        Enumeration<AbstractButton> buttons = m_typeGroup.getElements();
        while (buttons.hasMoreElements()) {
            AbstractButton button = buttons.nextElement();
            if (button.getActionCommand().equals(m_currentType)) {
                button.setSelected(true);
                typeOk = true;
                break;
            }
        }
        if (!typeOk) {
            m_currentType = NameFilterConfiguration.TYPE;
            m_nameButton.setSelected(true);
        }
        updateFilterPanel();
        repaint();
    }

    /**
     * Update this panel with the given include, exclude lists and the array of all possible values.
     *
     * @param ins include list
     * @param exs exclude list
     * @param names all available names
     * @noreference This method is not intended to be referenced by clients.
     * @nooverride This method is not intended to be re-implemented or extended by clients.
     */
    @SuppressWarnings("unchecked")
    public void update(final List<String> ins, final List<String> exs, final String[] names) {
        m_availableNames = names;
        // clear internal member
        m_order.clear();
        m_inclMdl.clear();
        m_exclMdl.clear();
        m_hideNames.clear();

        ArrayList<T> tmp_excl = new ArrayList<>(m_order.size());
        ArrayList<T> tmp_incl = new ArrayList<>(m_order.size());
        for (final String name : m_invalidIncludes) {
            final T t = getTforName(name);
            tmp_incl .add(t);
            m_order.add(t);
        }
        for (final String name : m_invalidExcludes) {
            final T t = getTforName(name);
            tmp_excl.add(t);
            m_order.add(t);
        }

        for (final String name : names) {
            final T t = getTforName(name);

            // continue if filter is set and current item t is filtered out
            if (m_filter != null) {
                if (!m_filter.include(t)) {
                    continue;
                }
            }

            // if item is not filtered out, add it to include or exclude list
            if (ins.contains(name)) {
                tmp_incl.add(t);
            } else if (exs.contains(name)) {
                tmp_excl.add(t);
            }
            m_order.add(t);
        }

        m_inclMdl.addAll(tmp_incl);
        m_exclMdl.addAll(tmp_excl);
        // forces update of the UI on startup
        updateTablePlaceholder(INCLUDE);
        updateTablePlaceholder(EXCLUDE);

        repaint();
    }

    /**
     * Save this configuration.
     *
     * @param config settings to be saved into
     */
    public void saveConfiguration(final NameFilterConfiguration config) {
        // save enforce option
        if (m_enforceExclusion.isSelected()) {
            config.setEnforceOption(EnforceOption.EnforceExclusion);
        } else {
            config.setEnforceOption(EnforceOption.EnforceInclusion);
        }

        // save include list
        final Set<T> incls = getIncludeList();
        final String[] ins = new String[incls.size()];
        int index = 0;
        for (T t : incls) {
            ins[index++] = getNameForT(t);
        }
        config.setIncludeList(ins);

        // save exclude option
        final Set<T> excls = getExcludeList();
        final String[] exs = new String[excls.size()];
        index = 0;
        for (T t : excls) {
            exs[index++] = getNameForT(t);
        }
        config.setExcludeList(exs);

        config.setRemovedFromIncludeList(getInvalidIncludes());
        config.setRemovedFromExcludeList(getInvalidExcludes());

        try {
            config.setType(m_currentType);
        } catch (InvalidSettingsException e) {
            NodeLogger.getLogger(getClass()).coding("Could not save settings as the selected filter type '"
                    + m_currentType + "' - this was a valid type when the configuration was loaded");
        }
        m_patternPanel.saveConfiguration(config.getPatternConfig());
    }

    /** @return list of all included T's */
    public Set<T> getIncludeList() {
        final Set<T> list = new LinkedHashSet<T>();
        for (int i = 0; i < m_inclMdl.getRowCount(); i++) {
            @SuppressWarnings("unchecked")
            T t = (T)m_inclMdl.getValueAt(i, 0);
            if (!isInvalidValue(getNameForT(t))) {
                list.add(t);
            }
        }
        return list;
    }

    /** @return list of all excluded T's */
    public Set<T> getExcludeList() {
        final Set<T> list = new LinkedHashSet<T>();
        for (int i = 0; i < m_exclMdl.getSize(); i++) {
            @SuppressWarnings("unchecked")
            T t = (T)m_exclMdl.getElementAt(i);
            if (!isInvalidValue(getNameForT(t))) {
                list.add(t);
            }
        }
        return list;
    }

    /**
     * Get the invalid values in the include list.
     */
    private String[] getInvalidIncludes() {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < m_inclMdl.getRowCount(); i++) {
            @SuppressWarnings("unchecked")
            String name = getNameForT((T)m_inclMdl.getValueAt(i, 0));
            if (isInvalidValue(name)) {
                list.add(name);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * Get the invalid values in the exclude list.
     */
    private String[] getInvalidExcludes() {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < m_exclMdl.getSize(); i++) {
            @SuppressWarnings("unchecked")
            String name = getNameForT((T)m_exclMdl.getElementAt(i));
            if (isInvalidValue(name)) {
                list.add(name);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * Check if a value is invalid.
     *
     * @param value The value to check
     * @return true if the given name is invalid, false otherwise
     */
    private boolean isInvalidValue(final String value) {
        return m_invalidIncludes.contains(value) || m_invalidExcludes.contains(value);
    }

    /**
     * Removes the given columns form either include or exclude list and notifies all listeners. Does not throw an
     * exception if the argument contains <code>null</code> elements or is not contained in any of the lists.
     *
     * @param names a list of names to hide from the filter
     */
    @SuppressWarnings("unchecked")
    public void hideNames(final T... names) {
        boolean changed = false;
        for (T name : names) {
            if (m_inclMdl.contains(name)) {
                m_hideNames.add(name);
                changed |= m_inclMdl.remove(name);
            } else if (m_exclMdl.contains(name)) {
                m_hideNames.add(name);
                changed |= m_exclMdl.remove(name);
            }
        }
        if (changed) {
            fireFilteringChangedEvent();
        }
    }

    /** Re-adds all remove/hidden names to the exclude list. */
    @SuppressWarnings("unchecked")
    public void resetHiding() {
        if (m_hideNames.isEmpty()) {
            return;
        }
        // add all selected elements from the include to the exclude list
        HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(m_hideNames);
        for (int i = 0; i < m_exclMdl.getSize(); i++) {
            hash.add(m_exclMdl.getElementAt(i));
        }
        m_exclMdl.clear();
        for (T name : m_order) {
            if (hash.contains(name)) {
                m_exclMdl.addRow(name);
            }
        }
        m_hideNames.clear();
    }

    /**
     * Sets the title of the include panel.
     *
     * @param title the new title
     */
    public void setIncludeTitle(final String title) {
        m_includeBorder.setTitle(title);
    }

    /**
     * Sets the title of the exclude panel.
     *
     * @param title the new title
     */
    public void setExcludeTitle(final String title) {
        m_excludeBorder.setTitle(title);
    }

    /**
     * Setter for the original "Remove All" button.
     *
     * @param text the new button title
     */
    public void setRemoveAllButtonText(final String text) {
        // TODO
//        m_remAllButton.setText(text);
    }

    /**
     * Setter for the original "Add All" button.
     *
     * @param text the new button title
     */
    public void setAddAllButtonText(final String text) {
        // TODO
//        m_addAllButton.setText(text);
    }

    /**
     * Setter for the original "Remove" button.
     *
     * @param text the new button title
     */
    public void setRemoveButtonText(final String text) {
        // TODO
//        m_remButton.setText(text);
    }

    /**
     * Setter for the original "Add" button.
     *
     * @param text the new button title
     */
    public void setAddButtonText(final String text) {
        // TODO
//        m_addButton.setText(text);
    }

    /**
     * Sets the internal used {@link InputFilter} and calls the {@link #update(List, List, String[])} method to update
     * the panel.
     *
     * @param filter the new {@link InputFilter} to use
     */
    public void setNameFilter(final InputFilter<T> filter) {
        List<String> inclList = new ArrayList<String>(getIncludedNamesAsSet());
        List<String> exclList = new ArrayList<String>(getExcludedNamesAsSet());
        m_filter = filter;
        m_patternPanel.setFilter(filter);
        update(inclList, exclList, m_availableNames);
    }

    /**
     * Returns a set of the names of all included items.
     *
     * @return a set of the names of all included items.
     */
    public Set<String> getIncludedNamesAsSet() {
        Set<T> inclList = getIncludeList();
        Set<String> inclNames = new LinkedHashSet<String>(inclList.size());
        for (T t : inclList) {
            inclNames.add(getNameForT(t));
        }
        return inclNames;
    }

    /**
     * Returns a set of the names of all excluded items.
     *
     * @return a set of the names of all excluded items.
     */
    public Set<String> getExcludedNamesAsSet() {
        Set<T> exclList = getExcludeList();
        Set<String> exclNames = new LinkedHashSet<String>(exclList.size());
        for (T t : exclList) {
            exclNames.add(getNameForT(t));
        }
        return exclNames;
    }

    /**
     * Returns all values include and exclude in its original order they have added to this panel.
     *
     * @return a set of string containing all values from the in- and exclude list
     */
    public Set<String> getAllValues() {
        final Set<String> set = new LinkedHashSet<String>();
        for (T t : m_order) {
            set.add(getNameForT(t));
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * Returns all objects T in its original order.
     *
     * @return a set of T objects retrieved from the in- and exclude list
     */
    public Set<T> getAllValuesT() {
        return Collections.unmodifiableSet(m_order);
    }

    /**
     * Adds the type to the given radio button. Used by subclasses to add a different type of filtering.
     *
     * @param radioButton Radio button to the type that will be added.
     * @param priority the priority of this type (the bigger, the further to the right). The priority of the default
     * type is 0 while the others are usually their FILTER_BY_X flags. If the given priority is already present than a
     * priority bigger then the currently biggest will be used.
     * @see NameFilterConfiguration#setType(String)
     * @since 2.9
     */
    protected void addType(final JRadioButton radioButton, final int priority) {
        int correctPriority = priority;
        if (m_typePriorities.containsKey(priority)) {
            correctPriority = m_typePriorities.lastKey() + 1;
        }
        m_typePriorities.put(correctPriority, radioButton.getActionCommand());
        m_typeGroup.add(radioButton);
        m_typePanel.removeAll();
        Map<String, AbstractButton> buttonMap = new LinkedHashMap<String, AbstractButton>();
        Enumeration<AbstractButton> buttons = m_typeGroup.getElements();
        while (buttons.hasMoreElements()) {
            AbstractButton button = buttons.nextElement();
            buttonMap.put(button.getActionCommand(), button);
        }
        for (String type : m_typePriorities.values()) {
            m_typePanel.add(buttonMap.get(type));
        }
        radioButton.setEnabled(isEnabled());
        updateTypePanel();
    }

    /**
     * Remove the type to the given radio button.
     *
     * @param radioButton Radio button to the type that will be removed.
     * @since 2.9
     */
    protected void removeType(final JRadioButton radioButton) {
        m_typeGroup.remove(radioButton);
        m_typePanel.remove(radioButton);
        String type = radioButton.getActionCommand();
        for (Entry<Integer, String> entry : m_typePriorities.entrySet()) {
            if (type.equals(entry.getValue())) {
                m_typePriorities.remove(entry.getKey());
                break;
            }
        }
        // Reset to default type if current type has been removed
        if (m_currentType.equals(radioButton.getActionCommand())) {
            m_currentType = NameFilterConfiguration.TYPE;
            updateFilterPanel();
        }
        updateTypePanel();
    }

    /**
     * Returns the panel to the given filter type.
     *
     * @param type The type
     * @return The panel to the type
     * @since 2.9
     */
    protected JPanel getFilterPanel(final String type) {
        return new JPanel();
    }

    /**
     * Creates a JRadioButton to the given FilterTypePanel.
     *
     * The created button will be initialized with the correct description, and action listener.
     *
     * @param actionCommand The action command that identifies the type that this button belongs to
     * @param label The label of this button that is shown to the user
     * @return The JRadioButton
     * @since 2.9
     */
    protected JRadioButton createButtonToFilterPanel(final String actionCommand, final String label) {
        JRadioButton button = new JRadioButton(label);
        button.setActionCommand(actionCommand);
        button.addActionListener(e -> {
            if (e.getActionCommand() != null) {
                String oldType = m_currentType;
                m_currentType = e.getActionCommand();
                if (!m_currentType.equals(oldType)) {
                    fireFilteringChangedEvent();
                }
                updateFilterPanel();
            }
        });
        return button;
    }

    /**
     * Informs the registered listeners of changes to the configuration.
     *
     * @since 2.9
     */
    protected void fireFilteringChangedEvent() {
        if (m_listeners != null) {
            for (ChangeListener listener : m_listeners) {
                listener.stateChanged(new ChangeEvent(this));
            }
        }
    }

    /**
     * Checks if class supports the creation of invalid values.
     *
     * If the class does support invalid values it must return an object representing the invalid value in the
     * getTForName() method and must be able to recreate the name from this object in the getNameForT() method.
     *
     * @return true if the class supports invalid values, false otherwise
     * @since 2.10
     */
    protected boolean supportsInvalidValues() {
        return false;
    }

    /**
     * Called by the 'remove >>' button to exclude the selected elements from the include list.
     * @param indices of rows to be removed in table display order
     */
    @SuppressWarnings("unchecked")
    private void onRemIt(final int[] rows) {
        // add all selected elements from the include to the exclude list
        List<T> o = new ArrayList<T>();
        for(int i : rows) {
            o.add((T)m_inclTable.getValueAt(i, 0));
        }
        HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(o);
        for (int i = 0; i < m_exclMdl.getSize(); i++) {
            hash.add(m_exclMdl.getElementAt(i));
        }

        boolean changed = m_inclMdl.removeAll(o);
        m_exclMdl.clear();

        // Here we copy all elements to add into a tmp list and add in bulk at the end.
        // Each add() call on the list model will fire a changed event which leads to
        // bad performance if we want to add many elements. addAll() will fire the event
        // only once.
        ArrayList<T> tmp = new ArrayList<>(m_order.size());
        for (T c : m_order) {
            if (hash.contains(c)) {
                tmp.add(c);
                String name = getNameForT(c);
                if (m_invalidIncludes.remove(name)) {
                    m_invalidExcludes.add(name);
                }
            }
        }
        m_exclMdl.addAll(tmp);

        if (changed) {
            cleanInvalidValues();
            fireFilteringChangedEvent();
            updateTablePlaceholder(INCLUDE);
            updateTablePlaceholder(EXCLUDE);
        }
    }

    /**
     * Called by the 'remove >>' button to exclude all elements from the include list.
     */
    @SuppressWarnings("unchecked")
    private void onRemAll() {
        boolean changed = !m_inclMdl.isEmpty();
        m_inclMdl.clear();
        m_exclMdl.clear();
        m_invalidExcludes.addAll(m_invalidIncludes);
        m_invalidIncludes.clear();

        // Here we copy all elements to add into a tmp list and add in bulk at the end.
        // Each add() call on the list model will fire a changed event which leads to
        // bad performance if we want to add many elements. addAll() will fire the event
        // only once.
        ArrayList<T> tmp = new ArrayList<>(m_order.size());
        for (T c : m_order) {
            if (!m_hideNames.contains(c)) {
                tmp.add(c);
            }
        }
        m_exclMdl.addAll(tmp);

        if (changed) {
            cleanInvalidValues();
            fireFilteringChangedEvent();
            updateTablePlaceholder("INCLUDE");
            updateTablePlaceholder("EXCLUDE");
        }
    }

    /**
     * Called by the '<< add' button to include the selected elements from the exclude list.
     * @param indices of rows to be added in table display order
     */
    @SuppressWarnings("unchecked")
    private void onAddIt(final int[] rows) {
        // add all selected elements from the exclude to the include list
        List<T> o = new ArrayList<T>();
        for(int i : rows) {
            o.add((T)m_exclTable.getValueAt(i, 0));
        }

        HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(o);
        Object e;
        for (int i = 0; i < m_inclMdl.getSize(); i++) {
            e = m_inclMdl.getElementAt(i);
            hash.add(e);
        }

        boolean changed = m_exclMdl.removeAll(o);
        m_inclMdl.clear();

        // Here we copy all elements to add into a tmp list and add in bulk at the end.
        // Each add() call on the list model will fire a changed event which leads to
        // bad performance if we want to add many elements. addAll() will fire the event
        // only once.
        ArrayList<T> tmp = new ArrayList<>(m_order.size());
        for (T c : m_order) {
            if (hash.contains(c)) {
                tmp.add(c);
                String name = getNameForT(c);
                if (m_invalidExcludes.remove(name)) {
                    m_invalidIncludes.add(name);
                }
            }
        }
        m_inclMdl.addAll(tmp);

        if (changed) {
            cleanInvalidValues();
            fireFilteringChangedEvent();
            updateTablePlaceholder(INCLUDE);
            updateTablePlaceholder(EXCLUDE);
        }
    }

    /**
     * Called by the '<< add all' button to include all elements from the exclude list.
     */
    @SuppressWarnings("unchecked")
    private void onAddAll() {
        boolean changed = !m_exclMdl.isEmpty();
        m_inclMdl.clear();
        m_exclMdl.clear();
        m_invalidIncludes.addAll(m_invalidExcludes);
        m_invalidExcludes.clear();

        // Here we copy all elements to add into a tmp list and add in bulk at the end.
        // Each add() call on the list model will fire a changed event which leads to
        // bad performance if we want to add many elements. addAll() will fire the event
        // only once.
        ArrayList<T> tmp = new ArrayList<>(m_order.size());
        for (T c : m_order) {
            if (!m_hideNames.contains(c)) {
                tmp.add(c);
            }
        }
        m_inclMdl.addAll(tmp);

        if (changed) {
            cleanInvalidValues();
            fireFilteringChangedEvent();
            updateTablePlaceholder(INCLUDE);
            updateTablePlaceholder(EXCLUDE);
        }
    }

    @SuppressWarnings("unchecked")
    private void cleanInvalidValues() {
        if (m_enforceExclusion.isSelected()) {
            for (int i = 0; i < m_inclMdl.getSize(); i++) {
                String name = getNameForT((T)m_inclMdl.getElementAt(i));
                if (isInvalidValue(name)) {
                    m_invalidIncludes.remove(name);
                    m_order.remove(m_inclMdl.getElementAt(i));
                    m_inclMdl.remove(i--);
                }
            }
        } else {
            for (int i = 0; i < m_exclMdl.getSize(); i++) {
                String name = getNameForT((T)m_exclMdl.getElementAt(i));
                if (isInvalidValue(name)) {
                    m_invalidExcludes.remove(name);
                    m_order.remove(m_exclMdl.getElementAt(i));
                    m_exclMdl.remove(i--);
                }
            }
        }
        if (m_inclMdl.isEmpty()) {
            m_inclTable.setToolTipText(null);
        }
        if (m_exclMdl.isEmpty()) {
            m_exclTable.setToolTipText(null);
        }
    }

    /**
     * Initializes the panel with the mode selection panel and the panel holding the currently active filter.
     */
    private void initPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.setLayout(new GridBagLayout());
        // Setup the mode panel, containing the options by column name and by column type
        m_typeGroup = new ButtonGroup();
        m_typePanel = new JPanel();
        m_typePanel.setLayout(new BoxLayout(m_typePanel, BoxLayout.X_AXIS));
        m_nameButton = createButtonToFilterPanel(NameFilterConfiguration.TYPE, NAME);
        // Default has priority 0 which is smaller than FILTER_BY_NAMEPATTERN
        addType(m_nameButton, 0);
        // Setup the filter panel which will contain the filter for the selected mode
        m_filterPanel = new JPanel(new BorderLayout());
        // Activate the selected filter
        m_filterPanel.add(m_nameFilterPanel);
        m_nameButton.setSelected(true);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(m_typePanel, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        panel.add(m_filterPanel, gbc);
        this.add(new JScrollPane(panel));
        updateTypePanel();
        updateFilterPanel();
    }

    private void updateTypePanel() {
        m_typePanel.setVisible(m_typeGroup.getButtonCount() > 1);
        m_typePanel.revalidate();
        m_typePanel.repaint();
    }

    /**
     * Changes the content of the filter panel to the currently active filter.
     */
    private void updateFilterPanel() {
        m_filterPanel.removeAll();
        if (NameFilterConfiguration.TYPE.equals(m_currentType)) {
            m_filterPanel.add(m_nameFilterPanel);
        } else if (PatternFilterConfiguration.TYPE.equals(m_currentType)) {
            m_filterPanel.add(m_patternPanel);
        } else {
            m_filterPanel.add(getFilterPanel(m_currentType));
        }
        // Revalidate and repaint are needed to update the view
        m_filterPanel.revalidate();
        m_filterPanel.repaint();
    }

    /**
     * Enables or disables the pattern filter.
     *
     * @param enabled If the pattern filter should be enabled
     * @since 2.9
     */
    private void setPatternFilterEnabled(final boolean enabled) {
        boolean wasEnabled = Collections.list(m_typeGroup.getElements()).contains(m_patternButton);
        if (wasEnabled != enabled) {
            if (enabled) {
                addType(m_patternButton, NameFilterConfiguration.FILTER_BY_NAMEPATTERN);
            } else {
                removeType(m_patternButton);
            }
        }
    }

    /**
     * @param exclude title for the left box
     * @param include title for the right box
     * @since 3.4
     */
    public void setPatternFilterBorderTitles(final String exclude, final String include) {
        m_patternPanel.setBorderTitles(exclude, include);
    }

    /**
     * sets the text of the "Include Missing Value"-checkbox
     * @param newText
     * @since 3.4
     */
    public void setAdditionalCheckboxText(final String newText){
        if (m_additionalCheckbox != null) {
            m_additionalCheckbox.setText(newText);
            m_additionalCheckbox.setToolTipText(newText);
        }
    }

    /**
     * sets the text of the "Include Missing Value"-checkbox
     * @param newText
     * @since 3.4
     */
    public void setAdditionalPatternCheckboxText(final String newText){
        m_patternPanel.setAdditionalCheckboxText(newText);
    }

    /**
     * @return the filter of this panel
     * @since 3.6
     */
    public InputFilter<T> getFilter() {
        return m_filter;
    }

    /**
     * Shows or hides the placeholder text in the textfield
     * @param table
     * @param focus true if focusGained, false if focusLost
     */
    private void updateTextFieldPlaceholder (final String table, final boolean focus){
        String query = table.equals(INCLUDE) ? m_inclSearchField.getText() : m_exclSearchField.getText();
        if (table.equals(INCLUDE)){
            if ((query.isEmpty() || query.equals(FILTER)) && focus) { //  no text or filter text and textfield gains focus -> hide placeholder
                m_inclSearchField.setText("");
                m_inclSearchField.setForeground(Color.BLACK);
                m_inclSearchField.setFont(getFont().deriveFont(Font.PLAIN, 14f));
            } else if (query.isEmpty() && !focus){ // no text and textfield looses focus -> show placeholder
                m_inclSearchField.setText(FILTER);
                m_inclSearchField.setForeground(Color.GRAY);
                m_inclSearchField.setFont(getFont().deriveFont(Font.ITALIC, 14f));
            }
        } else {
            if ((query.isEmpty() || query.equals(FILTER)) && focus) { //  no text or filter text and textfield gains focus -> hide placeholder
                m_exclSearchField.setText("");
                m_exclSearchField.setForeground(Color.BLACK);
                m_exclSearchField.setFont(getFont().deriveFont(Font.PLAIN, 14f));
            } else if (query.isEmpty() && !focus){ // no text and textfield looses focus -> show placeholder
                m_exclSearchField.setText(FILTER);
                m_exclSearchField.setForeground(Color.GRAY);
                m_exclSearchField.setFont(getFont().deriveFont(Font.ITALIC, 14f));
            }

        }
    }

    /**
     * Updates the placeholder of the corresponding table.
     * @param table
     */
    private void updateTablePlaceholder(final String table){
        // include table
        if (table.equals(INCLUDE)) {
            CardLayout cl = (CardLayout) m_inclCards.getLayout();
            // if there are no columns in the list
            if (m_inclMdl.getRowCount() == 0) {
                m_inclTablePlaceholder.updateText(EMPTY, "", 0);
                cl.show(m_inclCards, PLACEHOLDER);
            } else {
                // nothing found
                if (m_inclTable.getRowCount() == 0) {
                    m_inclTablePlaceholder.updateText(NOTHING_FOUND, m_inclSearchField.getText(), m_inclMdl.getRowCount());
                    cl.show(m_inclCards, PLACEHOLDER);
                } else {
                    cl.show(m_inclCards, LIST);
                }
            }
        } else { // exclude table
            CardLayout cl = (CardLayout) m_exclCards.getLayout();
            // if there are no columns in the list
            if (m_exclMdl.getRowCount() == 0) {
                m_exclTablePlaceholder.updateText(EMPTY, "", 0);
                cl.show(m_exclCards, PLACEHOLDER);
            } else {
                // nothing found
                if (m_exclTable.getRowCount() == 0) {
                    m_exclTablePlaceholder.updateText(NOTHING_FOUND, m_exclSearchField.getText(), m_exclMdl.getRowCount());
                    cl.show(m_exclCards, PLACEHOLDER);
                } else {
                    cl.show(m_exclCards, LIST);
                }
            }
        }
    }
    /**
     * Updates the rowfilter of the corresponding table with a new query. If the query is empty, the rowfilter will be set to null.
     * @param table
     */
    @SuppressWarnings("rawtypes")
    private void updateRowFilter(final String table){
        String query = table.equals(INCLUDE) ? m_inclSearchField.getText() : m_exclSearchField.getText();
        // RowFilter is set to null if search field is empty
        RowFilter<MyTableModel, Object> rf = null;
        if(!query.isEmpty()){
            try {
                // by default perform case insensitive search, escape all regex characters [\^$.|?*+()
                rf = RowFilter.regexFilter("(?i)" + Pattern.quote(query));
            } catch (java.util.regex.PatternSyntaxException p) {
                return;
            }
        }
        // include list
        if(table.equals(INCLUDE)){
            m_inclSorter.setRowFilter(rf);
        // exclude list
        }else{
            m_exclSorter.setRowFilter(rf);
        }
    }
}
