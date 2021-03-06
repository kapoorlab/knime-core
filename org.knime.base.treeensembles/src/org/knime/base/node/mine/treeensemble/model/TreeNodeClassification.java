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
 *
 * History
 *   Jan 5, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.LinkedHashMap;

import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeLeaf;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplitPMML;
import org.knime.base.node.mine.treeensemble.data.ClassificationPriors;
import org.knime.base.node.mine.treeensemble.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble.data.TreeMetaData;
import org.knime.base.node.mine.treeensemble.data.TreeTargetNominalColumnMetaData;
import org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.util.MutableInteger;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class TreeNodeClassification extends AbstractTreeNode {

    private static final TreeNodeClassification[] EMPTY_CHILD_ARRAY = new TreeNodeClassification[0];

    private final int m_majorityIndex;

    private final double[] m_targetDistribution;

    /**
     * @param signature
     * @param targetPriors
     * @param configuration
     */
    public TreeNodeClassification(final TreeNodeSignature signature, final ClassificationPriors targetPriors,
        final TreeEnsembleLearnerConfiguration configuration) {
        this(signature, targetPriors, EMPTY_CHILD_ARRAY, configuration);
    }

    /**
     * @param signature
     * @param targetPriors
     * @param childNodes
     * @param configuration
     */
    public TreeNodeClassification(final TreeNodeSignature signature, final ClassificationPriors targetPriors,
        final TreeNodeClassification[] childNodes, final TreeEnsembleLearnerConfiguration configuration) {
        super(signature, targetPriors.getTargetMetaData(), childNodes);
        if (configuration.isSaveTargetDistributionInNodes()) {
            m_targetDistribution = targetPriors.getDistribution();
        } else {
            m_targetDistribution = null;
        }
        m_majorityIndex = targetPriors.getMajorityIndex();
    }

    private TreeNodeClassification(final TreeModelDataInputStream in, final TreeMetaData metaData) throws IOException {
        super(in, metaData);
        TreeTargetNominalColumnMetaData targetMetaData = (TreeTargetNominalColumnMetaData)metaData.getTargetMetaData();
        int targetLength = targetMetaData.getValues().length;
        if (in.isContainsClassDistribution()) {
            double[] targetDistribution = new double[targetLength];
            int majorityIndex = -1;
            double max = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < targetLength; i++) {
                final double d = in.readDouble();
                if (d > max) { // strictly larger, see also PriorDistribution
                    majorityIndex = i;
                    max = d;
                }
                targetDistribution[i] = d;
            }
            m_targetDistribution = targetDistribution;
            m_majorityIndex = majorityIndex;
        } else {
            m_targetDistribution = null;
            m_majorityIndex = in.readInt();
        }
    }

    /** {@inheritDoc} */
    @Override
    public TreeTargetNominalColumnMetaData getTargetMetaData() {
        return (TreeTargetNominalColumnMetaData)super.getTargetMetaData();
    }

    /** @return the majorityClassName */
    public String getMajorityClassName() {
        return getTargetMetaData().getValues()[m_majorityIndex].getNominalValue();
    }

    /** {@inheritDoc} */
    @Override
    public TreeNodeClassification getChild(final int index) {
        return (TreeNodeClassification)super.getChild(index);
    }

    /** @return the targetDistribution */
    public double[] getTargetDistribution() {
        if (m_targetDistribution == null) {
            double[] result = new double[getTargetMetaData().getValues().length];
            result[m_majorityIndex] = 1.0;
            return result;
        } else {
            return m_targetDistribution;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return toStringRecursion("");
    }

    /**
     * recursively prints node and all its descendants
     *
     * @param indent
     * @return String representation of node with all its descendants
     */
    public String toStringRecursion(final String indent) {
        StringBuilder b = new StringBuilder();
        final TreeNodeCondition condition = getCondition();
        if (condition != null) {
            b.append(indent).append(condition).append(" --> ");
        } else {
            b.append(indent);
        }
        // e.g. "Iris-Setosa (50/150)"
        double[] targetDistribution = getTargetDistribution();
        double majorityWeight = targetDistribution[m_majorityIndex];
        double weightSum = 0.0;
        for (double v : targetDistribution) {
            weightSum += v;
        }
        b.append("\"").append(getMajorityClassName()).append("\" (");
        NumberFormat format = NumberFormat.getInstance();
        b.append(format.format(majorityWeight)).append("/");
        b.append(format.format(weightSum)).append(")");
        String childIndent = indent.concat("   ");
        for (int i = 0; i < getNrChildren(); i++) {
            b.append("\n");
            b.append(getChild(i).toStringRecursion(childIndent));
        }
        return b.toString();
    }

    /** {@inheritDoc} */
    @Override
    public void saveInSubclass(final DataOutputStream out) throws IOException {
        if (m_targetDistribution != null) {
            // length is equally to target value list length (no need to store)
            for (int i = 0; i < m_targetDistribution.length; i++) {
                out.writeDouble(m_targetDistribution[i]);
            }
        } else {
            out.writeInt(m_majorityIndex);
        }
    }

    /**
     * Loads a node from input stream <b>in</b>
     *
     * @param in
     * @param metaData
     * @return a TreeNodeClassification
     * @throws IOException
     */
    public static TreeNodeClassification load(final TreeModelDataInputStream in, final TreeMetaData metaData)
        throws IOException {
        return new TreeNodeClassification(in, metaData);
    }

    /** {@inheritDoc} */
    @Override
    TreeNodeClassification loadChild(final TreeModelDataInputStream in, final TreeMetaData metaData) throws IOException {
        return TreeNodeClassification.load(in, metaData);
    }

    /**
     * Creates DecisionTreeNode model that is used in Decision Tree of KNIME
     *
     * @param idGenerator
     * @param metaData
     * @return a DecisionTreeNode
     */
    public DecisionTreeNode createDecisionTreeNode(final MutableInteger idGenerator, final TreeMetaData metaData) {
        DataCell majorityCell = new StringCell(getMajorityClassName());
        double[] targetDistribution = getTargetDistribution();
        int initSize = (int)(targetDistribution.length / 0.75 + 1.0);
        LinkedHashMap<DataCell, Double> scoreDistributionMap = new LinkedHashMap<DataCell, Double>(initSize);
        NominalValueRepresentation[] targets = getTargetMetaData().getValues();
        for (int i = 0; i < targetDistribution.length; i++) {
            String cl = targets[i].getNominalValue();
            double d = targetDistribution[i];
            scoreDistributionMap.put(new StringCell(cl), d);
        }
        final int nrChildren = getNrChildren();
        if (nrChildren == 0) {
            return new DecisionTreeNodeLeaf(idGenerator.inc(), majorityCell, scoreDistributionMap);
        } else {
            int id = idGenerator.inc();
            DecisionTreeNode[] childNodes = new DecisionTreeNode[nrChildren];
            int splitAttributeIndex = getSplitAttributeIndex();
            assert splitAttributeIndex >= 0 : "non-leaf node has no split";
            String splitAttribute = metaData.getAttributeMetaData(splitAttributeIndex).getAttributeName();
            PMMLPredicate[] childPredicates = new PMMLPredicate[nrChildren];
            for (int i = 0; i < nrChildren; i++) {
                final TreeNodeClassification treeNode = getChild(i);
                TreeNodeCondition cond = treeNode.getCondition();
                childPredicates[i] = cond.toPMMLPredicate();
                childNodes[i] = treeNode.createDecisionTreeNode(idGenerator, metaData);
            }
            return new DecisionTreeNodeSplitPMML(id, majorityCell, scoreDistributionMap, splitAttribute,
                childPredicates, childNodes);
        }
    }

}
