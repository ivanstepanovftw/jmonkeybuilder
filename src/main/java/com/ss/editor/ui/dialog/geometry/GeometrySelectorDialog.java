package com.ss.editor.ui.dialog.geometry;

import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.ss.editor.Messages;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.ui.dialog.node.selector.NodeSelectorDialog;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * The implementation of node selector dialog to select a geometry.
 *
 * @author JavaSaBr
 */
public class GeometrySelectorDialog extends NodeSelectorDialog<Geometry> {

    public GeometrySelectorDialog(@NotNull final Spatial model, @NotNull final Consumer<Geometry> handler) {
        super(model, Geometry.class, handler);
    }

    @Override
    @FromAnyThread
    protected @NotNull String getTitleText() {
        return Messages.GEOMETRY_SELECTOR_DIALOG_TITLE;
    }
}
