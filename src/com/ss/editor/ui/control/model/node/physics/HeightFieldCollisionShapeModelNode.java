package com.ss.editor.ui.control.model.node.physics;

import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.ss.editor.ui.Icons;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The implementation of node to show {@link HeightfieldCollisionShape}.
 *
 * @author JavaSaBr
 */
public class HeightFieldCollisionShapeModelNode extends CollisionShapeModelNode<HeightfieldCollisionShape> {

    public HeightFieldCollisionShapeModelNode(@NotNull final HeightfieldCollisionShape element, final long objectId) {
        super(element, objectId);
    }

    @Nullable
    @Override
    public Image getIcon() {
        return Icons.TERRAIN_16;
    }

    @NotNull
    @Override
    public String getName() {
        return "Height field shape";
    }
}
