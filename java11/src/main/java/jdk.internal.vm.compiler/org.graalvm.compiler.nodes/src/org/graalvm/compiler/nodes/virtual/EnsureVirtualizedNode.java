/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */


package org.graalvm.compiler.nodes.virtual;

import static org.graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static org.graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import org.graalvm.compiler.core.common.type.Stamp;
import org.graalvm.compiler.core.common.type.StampFactory;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.graph.VerificationError;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.AbstractEndNode;
import org.graalvm.compiler.nodes.FixedNode;
import org.graalvm.compiler.nodes.FixedWithNextNode;
import org.graalvm.compiler.nodes.Invoke;
import org.graalvm.compiler.nodes.NodeView;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.java.StoreFieldNode;
import org.graalvm.compiler.nodes.spi.Lowerable;
import org.graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.compiler.nodes.spi.Virtualizable;
import org.graalvm.compiler.nodes.spi.VirtualizerTool;
import org.graalvm.compiler.nodes.type.StampTool;
import org.graalvm.compiler.nodes.util.GraphUtil;

@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
public final class EnsureVirtualizedNode extends FixedWithNextNode implements Virtualizable, Lowerable {

    public static final NodeClass<EnsureVirtualizedNode> TYPE = NodeClass.create(EnsureVirtualizedNode.class);

    @Input ValueNode object;
    private final boolean localOnly;

    public EnsureVirtualizedNode(ValueNode object, boolean localOnly) {
        super(TYPE, StampFactory.forVoid());
        this.object = object;
        this.localOnly = localOnly;
    }

    @Override
    public void virtualize(VirtualizerTool tool) {
        ValueNode alias = tool.getAlias(object);
        if (alias instanceof VirtualObjectNode) {
            VirtualObjectNode virtual = (VirtualObjectNode) alias;
            if (virtual instanceof VirtualBoxingNode) {
                Throwable exception = new VerificationError("ensureVirtual is not valid for boxing objects: %s", virtual.type().getName());
                throw GraphUtil.approxSourceException(this, exception);
            }
            if (!localOnly) {
                tool.setEnsureVirtualized(virtual, true);
            }
            tool.delete();
        }
    }

    @Override
    public void lower(LoweringTool tool) {
        ensureVirtualFailure(this, object.stamp(NodeView.DEFAULT));
    }

    public static void ensureVirtualFailure(Node location, Stamp stamp) {
        String additionalReason = "";
        if (location instanceof FixedWithNextNode && !(location instanceof EnsureVirtualizedNode)) {
            FixedWithNextNode fixedWithNextNode = (FixedWithNextNode) location;
            FixedNode next = fixedWithNextNode.next();
            if (next instanceof StoreFieldNode) {
                additionalReason = " (must not store virtual object into a field)";
            } else if (next instanceof Invoke) {
                additionalReason = " (must not pass virtual object into an invoke that cannot be inlined)";
            } else {
                additionalReason = " (must not let virtual object escape at node " + next + ")";
            }
        }
        Throwable exception = new VerificationError("Object of type %s should not be materialized%s:", StampTool.typeOrNull(stamp).getName(), additionalReason);

        Node pos;
        if (location instanceof FixedWithNextNode) {
            pos = ((FixedWithNextNode) location).next();
        } else if (location instanceof AbstractEndNode) {
            pos = ((AbstractEndNode) location).merge();
        } else {
            pos = location;
        }
        throw GraphUtil.approxSourceException(pos, exception);
    }
}
