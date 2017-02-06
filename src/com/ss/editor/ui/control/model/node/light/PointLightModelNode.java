package com.ss.editor.ui.control.model.node.light;

import com.jme3.light.PointLight;
import com.ss.editor.Messages;
import com.ss.editor.ui.Icons;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javafx.scene.image.Image;
import rlib.util.StringUtils;

/**
 * The implementation of {@link LightModelNode} to present point lights.
 *
 * @author JavaSaBr
 */
public class PointLightModelNode extends LightModelNode<PointLight> {

    public PointLightModelNode(@NotNull final PointLight element, final long objectId) {
        super(element, objectId);
    }

    @Nullable
    @Override
    public Image getIcon() {
        return Icons.POINT_16;
    }

    @NotNull
    @Override
    public String getName() {
        final PointLight element = getElement();
        final String name = element.getName();
        return StringUtils.isEmpty(name) ? Messages.MODEL_FILE_EDITOR_NODE_POINT_LIGHT : name;
    }
}