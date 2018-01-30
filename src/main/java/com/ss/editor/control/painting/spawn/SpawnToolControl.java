package com.ss.editor.control.painting.spawn;

import static com.ss.editor.part3d.editor.impl.scene.AbstractSceneEditor3DPart.KEY_IGNORE_RAY_CAST;
import static com.ss.editor.util.EditorUtil.getAssetManager;
import static com.ss.rlib.util.ObjectUtils.notNull;
import static com.ss.rlib.util.array.ArrayCollectors.toArray;
import com.jme3.asset.ModelKey;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.AssetLinkNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.annotation.JmeThread;
import com.ss.editor.control.painting.PaintingInput;
import com.ss.editor.control.painting.impl.AbstractPaintingControl;
import com.ss.editor.model.undo.editor.ModelChangeConsumer;
import com.ss.editor.ui.component.painting.spawn.SpawnPaintingComponent;
import com.ss.editor.ui.control.tree.action.impl.operation.AddChildOperation;
import com.ss.editor.util.GeomUtils;
import com.ss.editor.util.LocalObjects;
import com.ss.editor.util.NodeUtils;
import com.ss.rlib.util.array.Array;
import com.ss.rlib.util.array.ArrayFactory;
import jme3tools.optimize.GeometryBatchFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The implementation of spawn tool.
 *
 * @author JavaSaBr
 */
public class SpawnToolControl extends AbstractPaintingControl<SpawnPaintingComponent> {

    public enum SpawnMethod {
        AS_IS("As is"),
        BATCHED("Batched"),
        LINKED("Linked");

        @NotNull
        private final String label;

        SpawnMethod(@NotNull final String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * The list of spawned models.
     */
    @NotNull
    private final Array<Spatial> spawnedModels;

    /**
     * The list of examples model.
     */
    @NotNull
    private final Array<Spatial> examples;

    /**
     * The spawn method.
     */
    @NotNull
    private SpawnMethod method;

    /**
     * The painting time.
     */
    private float time;

    public SpawnToolControl(@NotNull final SpawnPaintingComponent component) {
        super(component);
        this.spawnedModels = ArrayFactory.newArray(Spatial.class);
        this.examples = ArrayFactory.newArray(Spatial.class);
        this.method = SpawnMethod.BATCHED;
    }

    /**
     * Get the spawn method.
     *
     * @return the spawn method.
     */
    @JmeThread
    public @NotNull SpawnMethod getMethod() {
        return method;
    }

    /**
     * Set the spawn method.
     *
     * @param method the spawn method.
     */
    @JmeThread
    public void setMethod(@NotNull final SpawnMethod method) {
        this.method = method;
    }

    @Override
    @FromAnyThread
    protected @NotNull ColorRGBA getBrushColor() {
        return ColorRGBA.Orange;
    }

    /**
     * Get the list of spawned models.
     *
     * @return the list of spawned models.
     */
    @JmeThread
    private @NotNull Array<Spatial> getSpawnedModels() {
        return spawnedModels;
    }

    /**
     * Update available examples.
     *
     * @param examples the list of available examples.
     */
    @JmeThread
    public void updateExamples(@NotNull final Array<Spatial> examples) {
        this.examples.clear();
        this.examples.addAll(examples);
    }

    /**
     * Get the list of examples model.
     *
     * @return the list of examples model.
     */
    @JmeThread
    private @NotNull Array<Spatial> getExamples() {
        return examples;
    }

    /**
     * Get the change consumer.
     *
     * @return the change consumer.
     */
    @FromAnyThread
    protected @NotNull ModelChangeConsumer getChangeConsumer() {
        return component.getChangeConsumer();
    }

    @Override
    @JmeThread
    public @Nullable Node getPaintedModel() {
        return component.getPaintedObject();
    }

    @Override
    public void startPainting(@NotNull final PaintingInput input, @NotNull final Quaternion brushRotation,
                              @NotNull final Vector3f contactPoint) {

        final Array<Spatial> spawnedModel = getExamples();
        if (spawnedModel.isEmpty()) {
            return;
        }

        super.startPainting(input, brushRotation, contactPoint);

        getSpawnedModels().clear();
        time = 0;
    }

    @Override
    @JmeThread
    public void updatePainting(@NotNull final Quaternion brushRotation, @NotNull final Vector3f contactPoint,
                               final float tpf) {

        time += (tpf * 10F);

        if (time > getBrushPower()) {
            time = 0;
            return;
        }

        final PaintingInput currentInput = notNull(getCurrentInput());

        switch (currentInput) {
            case MOUSE_PRIMARY: {
                spawn(brushRotation, contactPoint);
                break;
            }
        }
    }

    /**
     * Spawn models.
     *
     * @param brushRotation the brush rotation.
     * @param contactPoint  the contact point.
     */
    @JmeThread
    protected void spawn(@NotNull final Quaternion brushRotation, @NotNull final Vector3f contactPoint) {

        final float brushSize = getBrushSize();

        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final LocalObjects local = getLocalObjects();
        final Vector3f spawnPosition = local.nextVector();

        final Node paintedModel = getPaintedModel();
        final Vector3f direction = GeomUtils.getDirection(brushRotation, local.nextVector())
                .negateLocal()
                .multLocal(10);

        final Vector3f sourcePoint = contactPoint.subtract(direction, local.nextVector());
        final Ray ray = local.nextRay();
        ray.setOrigin(sourcePoint);

        final Vector3f resultPosition = local.nextVector();
        final CollisionResults collisions = local.nextCollisionResults();
        final CollisionResults spawnedCollisions = local.nextCollisionResults();

        final int maxCount = (int) Math.max(getBrushPower() / 2F, 1F);

        for(int count = 0; count < maxCount; count++) {
            for (int attempts = 0; attempts < 10; attempts++, attempts++) {
                collisions.clear();
                spawnedCollisions.clear();

                final float x = nextOffset(brushSize, random);
                final float y = nextOffset(brushSize, random);
                final float z = nextOffset(brushSize, random);

                spawnPosition.set(x, y, z)
                        .addLocal(contactPoint)
                        .subtractLocal(sourcePoint)
                        .normalizeLocal();

                ray.setDirection(spawnPosition);

                paintedModel.collideWith(ray, collisions);

                final CollisionResult closest = collisions.getClosestCollision();
                if (closest == null || contactPoint.distance(closest.getContactPoint()) > brushSize / 2) {
                    continue;
                }

                resultPosition.set(closest.getContactPoint())
                        .subtractLocal(paintedModel.getWorldTranslation());

                final Spatial clone = examples.get(random.nextInt(0, examples.size())).clone();
                clone.setUserData(KEY_IGNORE_RAY_CAST, Boolean.TRUE);
                clone.setLocalTranslation(resultPosition);
                clone.updateModelBound();

                if (paintedModel.collideWith(clone.getWorldBound(), spawnedCollisions) > 1) {
                    continue;
                }

                getSpawnedModels().add(clone);
                paintedModel.attachChild(clone);
                break;
            }
        }
    }

    /**
     * Calculate next random offset.
     *
     * @param brushSize the brush size.
     * @param random    the random.
     * @return the new coordinate.
     */
    @JmeThread
    protected float nextOffset(final float brushSize, @NotNull final ThreadLocalRandom random) {
        float result = random.nextInt(0, (int) (brushSize * 100)) / 100F;
        result /= 2F;
        return random.nextBoolean() ? result * -1 : result;
    }

    @Override
    @JmeThread
    public void finishPainting(@NotNull final Quaternion brushRotation, @NotNull final Vector3f contactPoint) {
        super.finishPainting(brushRotation, contactPoint);

        final Array<Spatial> spawnedModels = getSpawnedModels();
        spawnedModels.stream().peek(Spatial::removeFromParent)
                .forEach(sp -> sp.setUserData(KEY_IGNORE_RAY_CAST, null));

        final Node paintedModel = notNull(getPaintedModel());
        final ModelChangeConsumer changeConsumer = getChangeConsumer();

        final SpawnMethod method = getMethod();
        switch (method) {
            case AS_IS: {
                final Node spawnedNode = new Node("Spawned");
                spawnedModels.forEach(spawnedNode::attachChild);
                changeConsumer.execute(new AddChildOperation(spawnedNode, paintedModel));
                break;
            }
            case LINKED: {

                final Node spawnedNode = new Node("Spawned");
                spawnedModels.stream().map(this::linkSpatial)
                        .forEach(spawnedNode::attachChild);

                changeConsumer.execute(new AddChildOperation(spawnedNode, paintedModel));
                break;
            }
            case BATCHED: {

                final Node spawnedNode = new Node("Spawned");
                final Array<Geometry> geometries = spawnedModels.stream()
                        .flatMap(NodeUtils::children)
                        .filter(Geometry.class::isInstance)
                        .map(Geometry.class::cast)
                        .collect(toArray(Geometry.class));

                GeometryBatchFactory.makeBatches(geometries)
                        .forEach(spawnedNode::attachChild);

                changeConsumer.execute(new AddChildOperation(spawnedNode, paintedModel));
                break;
            }
        }
    }

    /**
     * Create an asset link node for the spatial.
     *
     * @param spatial the spatial.
     * @return the asset link node.
     */
    @JmeThread
    protected @NotNull AssetLinkNode linkSpatial(@NotNull final Spatial spatial) {
        final AssetLinkNode linkNode = new AssetLinkNode();
        linkNode.setName(spatial.getName());
        linkNode.attachLinkedChild(getAssetManager(), (ModelKey) spatial.getKey());
        linkNode.setLocalTranslation(spatial.getLocalTranslation());
        return linkNode;
    }
}