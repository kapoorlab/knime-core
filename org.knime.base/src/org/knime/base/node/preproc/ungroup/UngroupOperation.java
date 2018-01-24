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
 *   18.02.2015 (tibuch): created
 */
package org.knime.base.node.preproc.ungroup;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;

/**
 * This class performs the ungroup operation.
 * @author Tim-Oliver Buchholz, KNIME AG, Zurich, Switzerland
 * @since 2.12
 */
public class UngroupOperation {

    private boolean m_enableHilite = false;

    private boolean m_skipMissingValues = false;

    private boolean m_removeCollectionCol = true;

    /**
     * The hilite handler instance.
     */
    @Deprecated
    private HiLiteTranslator m_trans = null;

    @Deprecated
    private BufferedDataTable m_table = null;

    @Deprecated
    private int[] m_colIndices;

    private DataTableSpec m_newSpec;

    /**
     * @param enableHilite hilite enable
     * @param skipMissingValues skip missing values
     * @param removeCollectionCol remove collection columns
     * @deprecated use {@link #UngroupOperation(DataTableSpec, boolean, boolean, boolean, String...)} instead
     */
    @Deprecated
    public UngroupOperation(final boolean enableHilite, final boolean skipMissingValues,
        final boolean removeCollectionCol) {
        m_enableHilite = enableHilite;

        m_skipMissingValues = skipMissingValues;

        m_removeCollectionCol = removeCollectionCol;
    }

    /**
     * @param spec data table spec of the original table
     * @param enableHilite hilite enable
     * @param skipMissingValues skip missing values
     * @param removeCollectionCol remove collection columns
     * @param colNames the column names to create the new spec
     * @throws InvalidSettingsException
     * @since 3.6
     */
    public UngroupOperation(final DataTableSpec spec, final boolean enableHilite, final boolean skipMissingValues,
        final boolean removeCollectionCol, final String... colNames) throws InvalidSettingsException {
        m_newSpec = createTableSpec(spec, removeCollectionCol, colNames);

        m_enableHilite = enableHilite;

        m_skipMissingValues = skipMissingValues;

        m_removeCollectionCol = removeCollectionCol;
    }

    /**
     * Only needs to be set if {@link #compute(ExecutionContext)} is called subsequently.
     *
     * @param table to perform the ungroup
     * @deprecated pass it to the compute methods instead
     */
    @Deprecated
    public void setTable(final BufferedDataTable table) {
        m_table = table;
    }

    /**
     *
     * @param indices of the collection columns
     * @deprecated pass it to the compute-methods instead
     */
    @Deprecated
    public void setColIndices(final int[] indices) {
        m_colIndices = indices;
    }

    /**
     * Only needs to be set if {@link #compute(ExecutionContext)} is called subsequently.
     *
     * @param newSpec the new spec created with
     *            {@link UngroupOperation#createTableSpec(DataTableSpec, boolean, String...)}
     * @deprecated use {@link #UngroupOperation(DataTableSpec, boolean, boolean, boolean, String...)} instead
     */
    @Deprecated
    public void setNewSpec(final DataTableSpec newSpec) {
        m_newSpec = newSpec;
    }

    /**
     * @param exec the execution context
     * @return the table with the ungrouped collections
     * @throws Exception the thrown exception
     * @deprecated use {@link #compute(ExecutionContext, BufferedDataTable, int[], HiLiteTranslator)} instead
     */
    @Deprecated
    public BufferedDataTable compute(final ExecutionContext exec) throws Exception {
        return compute(exec, m_table, m_colIndices, m_trans);
    }

    /**
     * @param exec the execution context
     * @param table table to perform the ungroup operation on
     * @param colIndices indices of the collection columns, if <code>null</code> or the length is 0, the input table
     *            will be returned!!
     * @param trans the hilite translate, will be modified directly
     * @return the table with the ungrouped collections
     * @throws Exception the thrown exception
     * @since 3.6
     */
    public BufferedDataTable compute(final ExecutionContext exec, final BufferedDataTable table, final int[] colIndices,
        final HiLiteTranslator trans) throws Exception {
        final BufferedDataContainer dc = exec.createDataContainer(m_newSpec);
        if (table.size() == 0) {
            dc.close();
            return dc.getTable();
        }
        DataTableRowInput in = new DataTableRowInput(table);
        BufferedDataTableRowOutput out = new BufferedDataTableRowOutput(dc);
        compute(in, out, exec, table.size(), colIndices, trans);
        in.close();
        out.close();
        return out.getDataTable();
    }

    /**
     * Performs the ungroup operation on the given row input and pushes the result to the row output.
     *
     * @param in the row input, will NOT be closed when finished
     * @param out the row input, will NOT be closed when finished
     * @param exec the execution context to check cancellation and (optional) progress logging
     * @param rowCount row count to track the progress or <code>-1</code> without progress tracking
     * @throws Exception the thrown exception
     * @since 3.2
     * @deprecated use {@link #compute(ExecutionContext, BufferedDataTable, int[], HiLiteTranslator)} instead
     */
    @Deprecated
    public void compute(final RowInput in, final RowOutput out, final ExecutionContext exec, final long rowCount)
        throws Exception {
        compute(in, out, exec, rowCount, m_colIndices, m_trans);
    }

    /**
     * Performs the ungroup operation on the given row input and pushes the result to the row output.
     *
     * @param in the row input, will NOT be closed when finished
     * @param out the row input, will NOT be closed when finished
     * @param exec the execution context to check cancellation and (optional) progress logging
     * @param rowCount row count to track the progress or <code>-1</code> without progress tracking
     * @param colIndices indices of the collection columns, if <code>null</code> or the length is 0 the original rows
     *            will be output!!
     * @param trans the hilite translate, will be modified directly
     * @throws Exception the thrown exception
     * @since 3.6
     */
    public void compute(final RowInput in, final RowOutput out, final ExecutionContext exec, final long rowCount,
        final int[] colIndices, final HiLiteTranslator trans) throws Exception {
        final Map<RowKey, Set<RowKey>> hiliteMapping = new HashMap<RowKey, Set<RowKey>>();
        @SuppressWarnings("unchecked")
        Iterator<DataCell>[] iterators = new Iterator[colIndices.length];
        final DataCell[] missingCells = new DataCell[colIndices.length];
        Arrays.fill(missingCells, DataType.getMissingCell());
        long rowCounter = 0;
        DataRow row = null;
        while ((row = in.poll()) != null) {
            rowCounter++;
            exec.checkCanceled();
            if (rowCount > 0) {
                exec.setProgress(rowCounter / (double)rowCount,
                    "Processing row " + rowCounter + " of " + rowCount);
            }
            boolean allMissing = true;
            for (int i = 0, length = colIndices.length; i < length; i++) {
                final DataCell cell = row.getCell(colIndices[i]);
                final CollectionDataValue listCell;
                final Iterator<DataCell> iterator;
                if (cell instanceof CollectionDataValue) {
                    listCell = (CollectionDataValue)cell;
                    iterator = listCell.iterator();
                    allMissing = false;
                } else {
                    iterator = null;
                }
                iterators[i] = iterator;
            }
            if (allMissing) {
                //all collection column cells are missing cells append a row
                //with missing cells as well if the skip missing value option is disabled
                if (!m_skipMissingValues) {
                    final DefaultRow newRow =
                        createClone(row.getKey(), row, colIndices, m_removeCollectionCol, missingCells);
                    if (m_enableHilite) {
                        //create the hilite entry
                        final Set<RowKey> keys = new HashSet<RowKey>(1);
                        keys.add(row.getKey());
                        hiliteMapping.put(row.getKey(), keys);
                    }
                    out.push(newRow);
                }
                continue;
            }
            long counter = 1;
            final Set<RowKey> keys;
            if (m_enableHilite) {
                keys = new HashSet<RowKey>();
            } else {
                keys = null;
            }
            boolean continueLoop = false;
            boolean allEmpty = true;
            do {
                //reset the loop flag
                allMissing = true;
                continueLoop = false;
                final DataCell[] newCells = new DataCell[iterators.length];
                for (int i = 0, length = iterators.length; i < length; i++) {
                    Iterator<DataCell> iterator = iterators[i];
                    DataCell newCell;
                    if (iterator != null && iterator.hasNext()) {
                        allEmpty = false;
                        continueLoop = true;
                        newCell = iterator.next();
                    } else {
                        if (iterator == null) {
                            allEmpty = false;
                        }
                        newCell = DataType.getMissingCell();
                    }
                    if (!newCell.isMissing()) {
                        allMissing = false;
                    }
                    newCells[i] = newCell;
                }
                if (!allEmpty && !continueLoop) {
                    break;
                }
                if (!allEmpty && allMissing && m_skipMissingValues) {
                    continue;
                }
                final RowKey oldKey = row.getKey();
                final RowKey newKey = new RowKey(oldKey.getString() + "_" + counter++);
                final DefaultRow newRow = createClone(newKey, row, colIndices, m_removeCollectionCol, newCells);
                out.push(newRow);
                if (keys != null) {
                    keys.add(newKey);
                }
            } while (continueLoop);
            if (keys != null && !keys.isEmpty()) {
                hiliteMapping.put(row.getKey(), keys);
            }
        }
        if (m_enableHilite) {
            trans.setMapper(new DefaultHiLiteMapper(hiliteMapping));
        }
    }


    private DefaultRow createClone(final RowKey newKey, final DataRow row, final int[] colIdxs,
        final boolean removeCollectionCol, final DataCell[] newCells) {
        assert colIdxs.length == newCells.length;
        final Map<Integer, DataCell> map = new HashMap<Integer, DataCell>(newCells.length);
        for (int i = 0, length = newCells.length; i < length; i++) {
            map.put(Integer.valueOf(colIdxs[i]), newCells[i]);
        }
        final int cellCount;
        if (removeCollectionCol) {
            cellCount = row.getNumCells();
        } else {
            cellCount = row.getNumCells() + colIdxs.length;
        }
        final DataCell[] cells = new DataCell[cellCount];
        int cellIdx = 0;
        int newCellidx = 0;
        for (int i = 0, length = row.getNumCells(); i < length; i++) {
            if (map.containsKey(Integer.valueOf(i))) {
                if (!removeCollectionCol) {
                    cells[cellIdx++] = row.getCell(i);
                }
                cells[cellIdx++] = newCells[newCellidx++];
            } else {
                cells[cellIdx++] = row.getCell(i);
            }
        }
        return new DefaultRow(newKey, cells);
    }

    /**
     * @return the new resulting data table spec
     * @since 3.6
     */
    public DataTableSpec getDataTableSpec() {
        return m_newSpec;
    }

    /**
     * @param spec original spec
     * @param removeCollectionCol <code>true</code> if the collection column should be removed
     * @param colNames the collection column names
     * @return the new spec
     * @throws InvalidSettingsException if an exception occurs
     * @deprecated will be removed in future versions, use {@link #getDataTableSpec()} instead
     */
    @Deprecated
    public static DataTableSpec createTableSpec(final DataTableSpec spec, final boolean removeCollectionCol,
        final String... colNames) throws InvalidSettingsException {
        return createTableSpecInternal(spec, removeCollectionCol, colNames);
    }

    /**
     * @param spec original spec
     * @param removeCollectionCol <code>true</code> if the collection column should be removed
     * @param colNames the collection column names
     * @return the new spec
     * @throws InvalidSettingsException if an exception occurs
     */
    private static DataTableSpec createTableSpecInternal(final DataTableSpec spec, final boolean removeCollectionCol,
        final String... colNames) throws InvalidSettingsException {
        if (colNames == null || colNames.length <= 0) {
            //the user has not selected any column
            return spec;
        }
        final Collection<DataColumnSpec> specs = new LinkedList<DataColumnSpec>();
        final Map<String, DataType> collectionColsMap = new LinkedHashMap<String, DataType>(colNames.length);
        for (final String colName : colNames) {
            final int index = spec.findColumnIndex(colName);
            if (index < 0) {
                throw new InvalidSettingsException("Invalid column name '" + colName + "'");
            }
            final DataColumnSpec colSpec = spec.getColumnSpec(index);
            final DataType type = colSpec.getType();
            final DataType basicType = type.getCollectionElementType();
            if (basicType == null) {
                throw new InvalidSettingsException("Column '" + colName + "' is not of collection type");
            }
            collectionColsMap.put(colName, basicType);
        }
        final DataColumnSpecCreator specCreator = new DataColumnSpecCreator("dummy", StringCell.TYPE);
        for (final DataColumnSpec origColSpec : spec) {
            final String origColName = origColSpec.getName();
            final DataType resultType = collectionColsMap.get(origColName);
            if (resultType != null) {
                if (!removeCollectionCol) {
                    specs.add(origColSpec);
                    specCreator.setName(DataTableSpec.getUniqueColumnName(spec, origColName));
                } else {
                    specCreator.setName(origColName);
                }
                specCreator.setType(resultType);
                specs.add(specCreator.createSpec());
            } else {
                specs.add(origColSpec);
            }
        }
        //        final DataColumnSpecCreator specCreator =
        //                new DataColumnSpecCreator("dummy", StringCell.TYPE);
        //        for (Entry<String, DataType> entry : collectionColsMap.entrySet()) {
        //            if (removeCollectionCol) {
        //                //keep the original column name if the collection columns are removed
        //                specCreator.setName(entry.getKey());
        //            } else {
        //                specCreator.setName(DataTableSpec.getUniqueColumnName(
        //                                               spec, entry.getKey()));
        //            }
        //            specCreator.setType(entry.getValue());
        //            specs.add(specCreator.createSpec());
        //        }
        final DataTableSpec resultSpec = new DataTableSpec(specs.toArray(new DataColumnSpec[0]));
        return resultSpec;
    }

    /**
     * @return the hilite translator
     * @deprecated pass it to the compute-methods directly
     */
    @Deprecated
    public HiLiteTranslator getTrans() {
        return m_trans;
    }

    /**
     * @param trans the hilite translator to set
     * @deprecated pass it to the compute-methods directly
     */
    @Deprecated
    public void setTrans(final HiLiteTranslator trans) {
        m_trans = trans;
    }
}
