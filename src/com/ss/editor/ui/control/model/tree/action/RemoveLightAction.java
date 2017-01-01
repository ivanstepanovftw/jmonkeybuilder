package com.ss.editor.ui.control.model.tree.action;

import com.jme3.light.Light;
import com.jme3.scene.Node;
import com.ss.editor.Messages;
import com.ss.editor.model.undo.editor.ModelChangeConsumer;
import com.ss.editor.ui.Icons;
import com.ss.editor.ui.control.model.tree.ModelNodeTree;
import com.ss.editor.ui.control.model.tree.action.operation.RemoveLightOperation;
import com.ss.editor.ui.control.model.tree.node.ModelNode;
import com.ss.editor.util.GeomUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javafx.scene.image.Image;

/**
 * The action to remove a light.
 *
 * @author JavaSaBr
 */
public class RemoveLightAction extends AbstractNodeAction {

    public RemoveLightAction(final ModelNodeTree nodeTree, final ModelNode<?> node) {
        super(nodeTree, node);
    }

    @Nullable
    @Override
    protected Image getIcon() {
        return Icons.REMOVE_18;
    }

    @NotNull
    @Override
    protected String getName() {
        return Messages.MODEL_NODE_TREE_ACTION_REMOVE;
    }

    @Override
    protected void process() {

        final ModelNode<?> node = getNode();
        final Object element = node.getElement();

        if (!(element instanceof Light)) return;

        final Light light = (Light) element;

        final ModelNodeTree nodeTree = getNodeTree();
        final ModelNode<?> parentNode = nodeTree.findParent(node);
        if (parentNode == null) return;

        final Object parent = parentNode.getElement();
        if (!(parent instanceof Node)) return;

        final ModelChangeConsumer modelChangeConsumer = nodeTree.getModelChangeConsumer();

        final int index = GeomUtils.getIndex(modelChangeConsumer.getCurrentModel(), parent);

        modelChangeConsumer.execute(new RemoveLightOperation(light, index));
    }
}
