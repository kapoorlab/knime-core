/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   04.09.2017 (Adrian): created
 */
package org.knime.base.node.mine.treeensemble2.model.pmml;

import java.util.List;

import org.apache.xmlbeans.SchemaType;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.dmg.pmml.TreeModelDocument;
import org.dmg.pmml.TreeModelDocument.TreeModel;
import org.knime.base.node.mine.treeensemble2.data.TreeMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetColumnMetaData;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeModel;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeNode;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLTranslator;

/**
 *
 * @author Adrian Nembach, KNIME
 * @param <N> the type of node the trees handled by this translator consist of
 * @param <T> the type of meta data information of the target column
 *
 */
public abstract class AbstractTreeModelPMMLTranslator<N extends AbstractTreeNode, T extends TreeTargetColumnMetaData>
implements PMMLTranslator {

    private AbstractTreeModel<N> m_treeModel;
    private TreeMetaData m_treeMetaData;
    private String m_warning;

    /**
     * @param treeModel
     *
     */
    public AbstractTreeModelPMMLTranslator(final AbstractTreeModel<N> treeModel) {
        m_treeModel = treeModel;
    }

    public AbstractTreeModelPMMLTranslator() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeFrom(final PMMLDocument pmmlDoc) {
        PMML pmml = pmmlDoc.getPMML();
        List<TreeModel> trees = pmml.getTreeModelList();
        if (trees.size() > 1) {
            throw new IllegalArgumentException("This translator handles only single trees.");
        } else if (trees.isEmpty()) {
            throw new IllegalArgumentException("The provided PMMLDocument contains no tree models.");
        }

        MetaDataMapper<T> metaDataMapper = createMetaDataMapper(pmmlDoc);
        TreeModelImporter<N, T> importer = createImporter(metaDataMapper);
        m_treeModel = importer.importFromPMML(trees.get(0));
        m_treeMetaData = metaDataMapper.getTreeMetaData();
    }

    /**
     * Checks if the provided spec is a valid spec for a model translatable with this translator.
     * @param pmmlSpec the {@link PMMLPortObjectSpec} of a tree that should be imported
     */
    public static void checkPMMLSpec(final PMMLPortObjectSpec pmmlSpec) {
        // it won't be possible to construct a meta data mapper from an incompatible spec
        AbstractMetaDataMapper.createMetaDataMapper(pmmlSpec.getDataTableSpec());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SchemaType exportTo(final PMMLDocument pmmlDoc, final PMMLPortObjectSpec spec) {
        PMML pmml = pmmlDoc.getPMML();
        TreeModelDocument.TreeModel treeModel = pmml.addNewTreeModel();
        AbstractTreeModelExporter<N> exporter = createExporter();
        SchemaType st = exporter.writeModelToPMML(treeModel, spec);
        if (exporter.hasWarning()) {
            m_warning = exporter.getWarning();
        }
        return st;
    }

    public AbstractTreeModel<N> getTree() {
        return m_treeModel;
    }

    public TreeMetaData getTreeMetaData() {
        return m_treeMetaData;
    }

    protected abstract AbstractTreeModelExporter<N> createExporter();

    protected abstract MetaDataMapper<T> createMetaDataMapper(PMMLDocument pmmlDoc);

    protected abstract TreeModelImporter<N, T> createImporter(final MetaDataMapper<T> metaDataMapper);

    /**
     * @return true if a warning is present
     */
    public boolean hasWarning() {
        return m_warning != null;
    }

    /**
     * @return the warning message or null if there is not warning present
     */
    public String getWarning() {
        return m_warning;
    }

}