package com.ss.editor.model.undo.impl;

import com.ss.editor.annotation.FxThread;
import com.ss.editor.extension.scene.SceneNode;
import com.ss.editor.extension.scene.filter.SceneFilter;
import com.ss.editor.model.undo.editor.SceneChangeConsumer;
import org.jetbrains.annotations.NotNull;

/**
 * The implementation of the {@link AbstractEditorOperation} to remove a {@link SceneFilter} from a {@link SceneNode}.
 *
 * @author JavaSaBr.
 */
public class RemoveSceneFilterOperation extends AbstractEditorOperation<SceneChangeConsumer> {

    /**
     * The new scene filter.
     */
    @NotNull
    private final SceneFilter sceneFilter;

    /**
     * The scene node.
     */
    @NotNull
    private final SceneNode sceneNode;

    public RemoveSceneFilterOperation(@NotNull final SceneFilter sceneFilter, @NotNull final SceneNode sceneNode) {
        this.sceneFilter = sceneFilter;
        this.sceneNode = sceneNode;
    }

    @Override
    @FxThread
    protected void redoInFx(@NotNull final SceneChangeConsumer editor) {
        EXECUTOR_MANAGER.addJmeTask(() -> {
            sceneNode.removeFilter(sceneFilter);
            EXECUTOR_MANAGER.addFxTask(() -> editor.notifyRemovedFilter(sceneFilter));
        });
    }

    @Override
    @FxThread
    protected void undoImpl(@NotNull final SceneChangeConsumer editor) {
        EXECUTOR_MANAGER.addJmeTask(() -> {
            sceneNode.addFilter(sceneFilter);
            EXECUTOR_MANAGER.addFxTask(() -> editor.notifyAddedFilter(sceneFilter));
        });
    }
}
