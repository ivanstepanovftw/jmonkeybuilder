package com.ss.editor.ui.control.tree.action.impl.control;

import static com.ss.rlib.common.util.ObjectUtils.notNull;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.ss.editor.annotation.FxThread;
import com.ss.editor.model.undo.editor.ModelChangeConsumer;
import com.ss.editor.model.undo.impl.AddControlOperation;
import com.ss.editor.ui.control.tree.NodeTree;
import com.ss.editor.ui.control.tree.action.AbstractNodeAction;
import com.ss.editor.ui.control.tree.node.TreeNode;
import org.jetbrains.annotations.NotNull;

/**
 * The action to create the {@link Control}.
 *
 * @author JavaSaBr
 */
public abstract class AbstractCreateControlAction extends AbstractNodeAction<ModelChangeConsumer> {

    public AbstractCreateControlAction(@NotNull final NodeTree<?> nodeTree, @NotNull final TreeNode<?> node) {
        super(nodeTree, node);
    }

    @Override
    @FxThread
    protected void process() {
        super.process();

        if (isRequiredDialog()) {
            return;
        }

        final TreeNode<?> treeNode = getNode();
        final Spatial parent = (Spatial) treeNode.getElement();

        final NodeTree<ModelChangeConsumer> nodeTree = getNodeTree();
        final Control control = createControl(parent);

        final ModelChangeConsumer consumer = notNull(nodeTree.getChangeConsumer());
        consumer.execute(new AddControlOperation(control, parent));
    }

    /**
     * Return true if need a dialog to create a control.
     *
     * @return true if need a dialog to create a control.
     */
    @FxThread
    protected boolean isRequiredDialog() {
        return false;
    }

    /**
     * Create a control.
     *
     * @param parent the parent.
     * @return the control.
     */
    @FxThread
    protected @NotNull Control createControl(@NotNull final Spatial parent) {
        throw new UnsupportedOperationException();
    }
}