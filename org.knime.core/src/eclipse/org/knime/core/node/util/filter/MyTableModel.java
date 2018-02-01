/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jan 30, 2018 (jschweig): created
 */
package org.knime.core.node.util.filter;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Johannes Schweig
 */
@SuppressWarnings("serial")
class MyTableModel<T> extends AbstractTableModel{
    private ArrayList<T> m_data = new ArrayList<T>();

    private String[] m_header = {"type", "name"};

    MyTableModel (){

    }


    int getSize () {
        return m_data.size();
    }

    boolean isEmpty () {
        return m_data.isEmpty();
    }

    Object getElementAt (final int index) {
        return m_data.get(index);
    }

    boolean contains(final Object elem) {
        return m_data.contains(elem);
    }

    boolean remove (final Object elem) {
        m_data.remove(elem);
        fireTableDataChanged();
        return true;
    }

    void clear() {
        m_data.clear();
        fireTableDataChanged();
    }

    boolean removeAll (final Collection<T> c){
        m_data.removeAll(c);
        fireTableDataChanged();
        return true;
    }

    void addRow (final T s){
        m_data.add(s);
        fireTableDataChanged();
    }

    void addAll (final Collection<T> c){
        if (c.isEmpty()) {
            return;
        }
        m_data.addAll(c);
        fireTableDataChanged();
    }
   /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return m_data.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getColumnCount() {
        return m_header.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
//        System.out.println(name + "-row " + rowIndex + "-" + "data " + m_data.size());
//        System.out.println(rowIndex >= m_data.size() ? null : m_data.get(rowIndex));
        if(m_data.size()<=rowIndex){
            return "invalid";
        }else{
            return columnIndex == 0 ? columnIndex : m_data.get(rowIndex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName (final int column) {
        return m_header[column];
    }
}
