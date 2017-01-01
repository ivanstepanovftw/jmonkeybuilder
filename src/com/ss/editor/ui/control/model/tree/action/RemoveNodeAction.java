package com.ss.editor.ui.control.model.tree.action;

import com.jme3.scene.Spatial;
import com.ss.editor.Messages;
import com.ss.editor.model.undo.editor.ModelChangeConsumer;
import com.ss.editor.ui.Icons;
import com.ss.editor.ui.control.model.tree.ModelNodeTree;
import com.ss.editor.ui.control.model.tree.action.operation.RemoveChildOperation;
import com.ss.editor.ui.control.model.tree.node.ModelNode;
import com.ss.editor.util.GeomUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javafx.scene.image.Image;

/**
 * The action to remove a node from model.
 *
 * @author JavaSaBr
 */
public class RemoveNodeAction extends AbstractNodeAction {

    public RemoveNodeAction(@NotNull final ModelNodeTree nodeTree, @NotNull final ModelNode<?> node) {
        super(nodeTree, node);
    }

    @NotNull
    @Override
    protected String getName() {
        return Messages.MODEL_NODE_TREE_ACTION_REMOVE;
    }

    @Nullable
    @Override
    protected Image getIcon() {
        return Icons.REMOVE_18;
    }

    @Override
    protected void process() {

        final ModelNode<?> node = getNode();
        final Object element = node.getElement();
        if (!(element instanceof Spatial)) return;

        final Spatial spatial = (Spatial) element;

        final ModelNodeTree nodeTree = getNodeTree();
        final ModelChangeConsumer modelChangeConsumer = nodeTree.getModelChangeConsumer();

        final int index = GeomUtils.getIndex(modelChangeConsumer.getCurrentModel(), spatial.getParent());

        modelChangeConsumer.execute(new RemoveChildOperation(spatial, index));
    }
}
