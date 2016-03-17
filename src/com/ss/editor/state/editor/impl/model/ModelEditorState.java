package com.ss.editor.state.editor.impl.model;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.effect.ParticleEmitter;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.input.InputManager;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.debug.WireBox;
import com.jme3.scene.debug.WireSphere;
import com.jme3.scene.shape.Quad;
import com.ss.editor.control.transform.MoveToolControl;
import com.ss.editor.control.transform.RotationToolControl;
import com.ss.editor.control.transform.ScaleToolControl;
import com.ss.editor.control.transform.SceneEditorControl;
import com.ss.editor.control.transform.TransformControl;
import com.ss.editor.model.EditorCamera;
import com.ss.editor.state.editor.impl.AbstractEditorState;
import com.ss.editor.ui.component.editor.impl.model.ModelFileEditor;

import rlib.geom.util.AngleUtils;
import rlib.util.array.Array;
import rlib.util.array.ArrayFactory;
import rlib.util.array.ArrayIterator;
import rlib.util.dictionary.DictionaryFactory;
import rlib.util.dictionary.ObjectDictionary;

/**
 * Реализация 3D части редактора модели.
 *
 * @author Ronn
 */
public class ModelEditorState extends AbstractEditorState<ModelFileEditor> implements SceneEditorControl {

    private static final float H_ROTATION = AngleUtils.degreeToRadians(45);
    private static final float V_ROTATION = AngleUtils.degreeToRadians(15);

    private final JobProgressAdapter<LightProbe> probeHandler = new JobProgressAdapter<LightProbe>() {

        @Override
        public void done(final LightProbe result) {

            if (!isInitialized()) {
                return;
            }

            notifyProbeComplete();
        }
    };

    /**
     * Модели выделения выбранных частей.
     */
    protected final ObjectDictionary<Spatial, Spatial> selectionShape;

    /**
     * Набор кастомных фонов.
     */
    private final Array<Spatial> customSky;

    /**
     * Выбранные модели.
     */
    protected final Array<Spatial> selected;

    /**
     * Узел для размещения модели.
     */
    private final Node modelNode;

    /**
     * Узел для размещения вспомогательных графических элементов.
     */
    private final Node toolNode;

    /**
     * Узел для размещения инструментов для трансформации
     */
    private final Node transformToolNode;

    /**
     * Узел для размещения кастомного фона.
     */
    private final Node customSkyNode;

    /**
     * Набор узлов для манипуляций с моделями.
     */
    private Node moveTool, rotateTool, scaleTool;

    /**
     * Плоскость для вычисления трансформаций.
     */
    private Node collisionPlane;

    /**
     * Разница между предыдущей точкой трансформации и новой.
     */
    private Vector3f deltaVector;

    /**
     * Центр трансформации.
     */
    private Transform transformCenter;

    /**
     * Объект на трансформацию.
     */
    private Spatial toTransform;

    /**
     * Материал для выделения.
     */
    private Material selectionMaterial;

    /**
     * Сетка сцены.
     */
    private Geometry grid;

    /**
     * Узел на который смотрит камера.
     */
    private Node cameraNode;

    /**
     * Текущая отображаемая модель.
     */
    private Spatial currentModel;

    /**
     * Текущее быстрое окружение.
     */
    private Spatial currentFastSky;

    /**
     * Текущий тип трансформации.
     */
    private TransformType transformType;

    /**
     * Текущее направление трансформации.
     */
    private PickedAxis pickedAxis;

    /**
     * Активирован ли свет камеры.
     */
    private boolean lightEnabled;

    /**
     * Отображать ли выбранный узел.
     */
    private boolean showSelection;

    /**
     * Отображать ли сетку.
     */
    private boolean showGrid;

    /**
     * Есть ли активная трансформация.
     */
    private boolean activeTransform;

    /**
     * Кол-во кадров.
     */
    private int frame;

    public ModelEditorState(final ModelFileEditor fileEditor) {
        super(fileEditor);
        this.modelNode = new Node("ModelNode");
        this.modelNode.setUserData(ModelEditorState.class.getName(), true);
        this.toolNode = new Node("ToolNode");
        this.transformToolNode = new Node("TransformToolNode");
        this.customSkyNode = new Node("Custom Sky");
        this.customSky = ArrayFactory.newArray(Spatial.class);
        this.selected = ArrayFactory.newArray(Spatial.class);
        this.selectionShape = DictionaryFactory.newObjectDictionary();

        final Node stateNode = getStateNode();
        stateNode.attachChild(getCameraNode());
        stateNode.attachChild(getCustomSkyNode());

        createCollisionPlane();
        createToolElements();
        createManipulators();

        final EditorCamera editorCamera = getEditorCamera();
        editorCamera.setDefaultHorizontalRotation(H_ROTATION);
        editorCamera.setDefaultVerticalRotation(V_ROTATION);

        setLightEnabled(true);
        setShowSelection(true);
        setShowGrid(true);

        setTransformType(TransformType.MOVE_TOOL);
    }

    /**
     * @param activeTransform eсть ли активная трансформация.
     */
    private void setActiveTransform(final boolean activeTransform) {
        this.activeTransform = activeTransform;
    }

    /**
     * @return есть ли активная трансформация.
     */
    private boolean isActiveTransform() {
        return activeTransform;
    }

    /**
     * @return узел для размещения инструментов для трансформации.
     */
    private Node getTransformToolNode() {
        return transformToolNode;
    }

    /**
     * @return узел для размещения кастомного фона.
     */
    private Node getCustomSkyNode() {
        return customSkyNode;
    }

    /**
     * @return Набор кастомных фонов.
     */
    private Array<Spatial> getCustomSky() {
        return customSky;
    }

    @Override
    protected void onActionImpl(final String name, final boolean isPressed, final float tpf) {
        super.onActionImpl(name, isPressed, tpf);

        if (MOUSE_RIGHT_CLICK.equals(name)) {
            processClick(isPressed);
        } else if (MOUSE_LEFT_CLICK.equals(name)) {

            if (isPressed) {
                startTransform();
            } else {
                endTransform();
            }
        }
    }

    /**
     * Обработка клика мышкой по области редактора.
     */
    private void processClick(final boolean isPressed) {

        if (!isPressed) {
            return;
        }

        final Camera camera = EDITOR.getCamera();

        final InputManager inputManager = EDITOR.getInputManager();
        final Vector2f cursor = inputManager.getCursorPosition();
        final Vector3f click3d = camera.getWorldCoordinates(cursor, 0f);
        final Vector3f dir = camera.getWorldCoordinates(cursor, 1f).subtractLocal(click3d).normalizeLocal();

        final Ray ray = new Ray();
        ray.setOrigin(click3d);
        ray.setDirection(dir);

        final CollisionResults results = new CollisionResults();

        final Node modelNode = getModelNode();
        modelNode.collideWith(ray, results);

        final ModelFileEditor editor = getFileEditor();

        if (results.size() < 1) {
            EXECUTOR_MANAGER.addFXTask(() -> editor.notifySelected(null));
            return;
        }

        final CollisionResult collision = results.getClosestCollision();

        if (collision == null) {
            EXECUTOR_MANAGER.addFXTask(() -> editor.notifySelected(null));
            return;
        }

        EXECUTOR_MANAGER.addFXTask(() -> editor.notifySelected(collision.getGeometry()));
    }

    /**
     * Создание вспомогательных элементов.
     */
    private void createToolElements() {

        selectionMaterial = createColorMaterial(new ColorRGBA(1F, 170 / 255F, 64 / 255F, 1F));

        //grid
        grid = new Geometry("grid", new Grid(20, 20, 1.0f));
        grid.setMaterial(createColorMaterial(ColorRGBA.Gray));
        grid.setLocalTranslation(-10, 0, -10);

        final Node toolNode = getToolNode();
        toolNode.attachChild(grid);
    }

    private void createManipulators() {

        final AssetManager assetManager = EDITOR.getAssetManager();

        final Material redMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        redMaterial.setColor("Color", ColorRGBA.Red);

        final Material blueMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        blueMaterial.setColor("Color", ColorRGBA.Blue);

        final Material greenMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        greenMaterial.setColor("Color", ColorRGBA.Green);

        moveTool = (Node) assetManager.loadModel("graphics/models/manipulators/manipulators_move.j3o");
        moveTool.getChild("move_x").setMaterial(redMaterial);
        moveTool.getChild("collision_move_x").setMaterial(redMaterial);
        moveTool.getChild("collision_move_x").setCullHint(Spatial.CullHint.Always);
        moveTool.getChild("move_y").setMaterial(blueMaterial);
        moveTool.getChild("collision_move_y").setMaterial(blueMaterial);
        moveTool.getChild("collision_move_y").setCullHint(Spatial.CullHint.Always);
        moveTool.getChild("move_z").setMaterial(greenMaterial);
        moveTool.getChild("collision_move_z").setMaterial(greenMaterial);
        moveTool.getChild("collision_move_z").setCullHint(Spatial.CullHint.Always);
        moveTool.scale(0.1f);
        moveTool.addControl(new MoveToolControl(this));

        rotateTool = (Node) assetManager.loadModel("graphics/models/manipulators/manipulators_rotate.j3o");
        rotateTool.getChild("rot_x").setMaterial(redMaterial);
        rotateTool.getChild("collision_rot_x").setMaterial(redMaterial);
        rotateTool.getChild("collision_rot_x").setCullHint(Spatial.CullHint.Always);
        rotateTool.getChild("rot_y").setMaterial(blueMaterial);
        rotateTool.getChild("collision_rot_y").setMaterial(blueMaterial);
        rotateTool.getChild("collision_rot_y").setCullHint(Spatial.CullHint.Always);
        rotateTool.getChild("rot_z").setMaterial(greenMaterial);
        rotateTool.getChild("collision_rot_z").setMaterial(greenMaterial);
        rotateTool.getChild("collision_rot_z").setCullHint(Spatial.CullHint.Always);
        rotateTool.scale(0.1f);
        rotateTool.addControl(new RotationToolControl(this));

        scaleTool = (Node) assetManager.loadModel("graphics/models/manipulators/manipulators_scale.j3o");
        scaleTool.getChild("scale_x").setMaterial(redMaterial);
        scaleTool.getChild("collision_scale_x").setMaterial(redMaterial);
        scaleTool.getChild("collision_scale_x").setCullHint(Spatial.CullHint.Always);
        scaleTool.getChild("scale_y").setMaterial(blueMaterial);
        scaleTool.getChild("collision_scale_y").setMaterial(blueMaterial);
        scaleTool.getChild("collision_scale_y").setCullHint(Spatial.CullHint.Always);
        scaleTool.getChild("scale_z").setMaterial(greenMaterial);
        scaleTool.getChild("collision_scale_z").setMaterial(greenMaterial);
        scaleTool.getChild("collision_scale_z").setCullHint(Spatial.CullHint.Always);
        scaleTool.scale(0.1f);
        scaleTool.addControl(new ScaleToolControl(this));
    }

    /**
     * Создание плоскости для детектирования перемещения.
     */
    private void createCollisionPlane() {

        final AssetManager assetManager = EDITOR.getAssetManager();

        final Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        final RenderState renderState = material.getAdditionalRenderState();
        renderState.setFaceCullMode(RenderState.FaceCullMode.Off);
        renderState.setWireframe(true);

        final float size = 20000;

        final Geometry geometry = new Geometry("plane", new Quad(size, size));
        geometry.setMaterial(material);
        geometry.setLocalTranslation(-size / 2, -size / 2, 0);

        collisionPlane = new Node();
        collisionPlane.attachChild(geometry);
    }

    /**
     * @param transformType текущий тип трансформации.
     */
    public void setTransformType(final TransformType transformType) {
        this.transformType = transformType;
    }

    @Override
    public Transform getTransformCenter() {
        return transformCenter;
    }

    /**
     * @param pickedAxis текущее направление трансформации.
     */
    public void setPickedAxis(final PickedAxis pickedAxis) {
        this.pickedAxis = pickedAxis;
    }

    @Override
    public PickedAxis getPickedAxis() {
        return pickedAxis;
    }

    @Override
    public Node getCollisionPlane() {

        if (collisionPlane == null) {
            throw new RuntimeException("collisionPlane is null");
        }

        return collisionPlane;
    }

    @Override
    public void setDeltaVector(Vector3f deltaVector) {
        this.deltaVector = deltaVector;
    }

    @Override
    public Vector3f getDeltaVector() {
        return deltaVector;
    }

    @Override
    public Spatial getToTransform() {
        return toTransform;
    }

    @Override
    public void notifyTransformed(final Spatial spatial) {
        final ModelFileEditor fileEditor = getFileEditor();
        fileEditor.notifyTransformed(spatial);
    }

    /**
     * @return текущий тип трансформации.
     */
    private TransformType getTransformType() {
        return transformType;
    }

    /**
     * @return сетка.
     */
    private Geometry getGrid() {
        return grid;
    }

    /**
     * Создание материала для отображения граф. элемнтов.
     */
    private Material createColorMaterial(final ColorRGBA color) {
        final Material material = new Material(EDITOR.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.getAdditionalRenderState().setWireframe(true);
        material.setColor("Color", color);
        return material;
    }

    /**
     * @return узел для размещения вспомогательных графических элементов.
     */
    private Node getToolNode() {
        return toolNode;
    }

    @Override
    protected Node getNodeForCamera() {

        if (cameraNode == null) {
            cameraNode = new Node("CameraNode");
        }

        return cameraNode;
    }

    /**
     * @return узел на который смотрит камера.
     */
    private Node getCameraNode() {
        return cameraNode;
    }

    /**
     * Активая узла с моделями.
     */
    private void notifyProbeComplete() {

        final Node stateNode = getStateNode();
        stateNode.attachChild(getModelNode());
        stateNode.attachChild(getToolNode());

        final Node customSkyNode = getCustomSkyNode();
        customSkyNode.detachAllChildren();
    }

    /**
     * @param currentFastSky текущее быстрое окружение.
     */
    private void setCurrentFastSky(final Spatial currentFastSky) {
        this.currentFastSky = currentFastSky;
    }

    /**
     * @return текущее быстрое окружение.
     */
    private Spatial getCurrentFastSky() {
        return currentFastSky;
    }

    /**
     * @return узел для размещения модели.
     */
    private Node getModelNode() {
        return modelNode;
    }

    /**
     * Отобразить на сцене указанную модель.
     */
    public void openModel(final Spatial model) {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> openModelImpl(model));
    }

    /**
     * Процесс отображения указанной модели.
     */
    private void openModelImpl(final Spatial model) {

        final Node modelNode = getModelNode();
        modelNode.attachChild(model);

        setCurrentModel(model);
    }

    /**
     * @param currentModel текущая отображаемая модель.
     */
    private void setCurrentModel(final Spatial currentModel) {
        this.currentModel = currentModel;
    }

    /**
     * @return текущая отображаемая модель.
     */
    private Spatial getCurrentModel() {
        return currentModel;
    }

    /**
     * @return активирован ли свет камеры.
     */
    private boolean isLightEnabled() {
        return lightEnabled;
    }

    /**
     * @param lightEnabled активирован ли свет камеры.
     */
    private void setLightEnabled(boolean lightEnabled) {
        this.lightEnabled = lightEnabled;
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application application) {
        super.initialize(stateManager, application);

        frame = 0;
    }

    @Override
    public void cleanup() {
        super.cleanup();

        final Node stateNode = getStateNode();
        stateNode.detachChild(getModelNode());
        stateNode.detachChild(getToolNode());
    }

    /**
     * @return узел для перемещения модели.
     */
    private Node getMoveTool() {
        return moveTool;
    }

    /**
     * @return узел для вращения модели.
     */
    private Node getRotateTool() {
        return rotateTool;
    }

    /**
     * @return узел для маштабирования модели.
     */
    private Node getScaleTool() {
        return scaleTool;
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (frame == 2) {

            final Node customSkyNode = getCustomSkyNode();

            final Array<Spatial> customSky = getCustomSky();
            customSky.forEach(spatial -> customSkyNode.attachChild(spatial.clone(false)));

            EDITOR.updateProbe(probeHandler);
        }

        final Node transformToolNode = getTransformToolNode();
        final Transform selectionCenter = getTransformCenter();

        // Transform Selected Objects!
        if (isActiveTransform() && selectionCenter != null) {
            if (transformType == TransformType.MOVE_TOOL) {
                final TransformControl control = getMoveTool().getControl(TransformControl.class);
                transformToolNode.detachAllChildren();
                control.processTransform();
            } else if (transformType == TransformType.ROTATE_TOOL) {
                final TransformControl control = getRotateTool().getControl(TransformControl.class);
                transformToolNode.detachAllChildren();
                control.processTransform();
            } else if (transformType == TransformType.SCALE_TOOL) {
                final TransformControl control = getScaleTool().getControl(TransformControl.class);
                transformToolNode.detachAllChildren();
                control.processTransform();
            }
        }

        final EditorCamera editorCamera = getEditorCamera();

        if (editorCamera != null) {
            editorCamera.update(tpf);
        }

        final ObjectDictionary<Spatial, Spatial> selectionShape = getSelectionShape();
        final Array<Spatial> selected = getSelected();
        selected.forEach(spatial -> {

            final Spatial shape = selectionShape.get(spatial);
            shape.setLocalTranslation(spatial.getWorldTranslation());
            shape.setLocalRotation(spatial.getWorldRotation());
            shape.setLocalScale(spatial.getWorldScale());

            updateTransformNode(spatial.getLocalTransform());
        });

        final Node toolNode = getToolNode();
        transformToolNode.detachAllChildren();

        final TransformType transformType = getTransformType();

        if (transformType == TransformType.MOVE_TOOL) {
            transformToolNode.attachChild(getMoveTool());
        } else if (transformType == TransformType.ROTATE_TOOL) {
            transformToolNode.attachChild(getRotateTool());
        } else if (transformType == TransformType.SCALE_TOOL) {
            transformToolNode.attachChild(getScaleTool());
        }

        if (selected.isEmpty()) {
            toolNode.detachChild(transformToolNode);
        } else {
            toolNode.attachChild(transformToolNode);
        }

        frame++;
    }

    /**
     * Процесс обновления положения элементов трансформации.
     */
    protected void updateTransformNode(final Transform transform) {

        if (transform == null) {
            return;
        }

        final Camera camera = EDITOR.getCamera();

        final Vector3f location = transform.getTranslation();
        final Vector3f resultPosition = location.subtract(camera.getLocation()).normalize().multLocal(camera.getFrustumNear() + 0.4f);

        final Node transformToolNode = getTransformToolNode();
        transformToolNode.setLocalTranslation(camera.getLocation().add(resultPosition));
        transformToolNode.setLocalRotation(transform.getRotation());
    }

    @Override
    protected boolean needEditorCamera() {
        return true;
    }

    @Override
    protected boolean needUpdateCameraLight() {
        return true;
    }

    @Override
    protected boolean needLightForCamera() {
        return true;
    }

    /**
     * Обновление активированности света от камеры.
     */
    public void updateLightEnabled(final boolean enabled) {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> updateLightEnabledImpl(enabled));
    }

    /**
     * Процесс обновление света от камеры.
     */
    private void updateLightEnabledImpl(boolean enabled) {

        if (enabled == isLightEnabled()) {
            return;
        }

        final DirectionalLight light = getLightForCamera();
        final Node stateNode = getStateNode();

        if (enabled) {
            stateNode.addLight(light);
        } else {
            stateNode.removeLight(light);
        }

        setLightEnabled(enabled);
    }

    /**
     * Обработка смены быстрого окружения редактора.
     */
    public void changeFastSky(final Spatial fastSky) {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> changeFastSkyImpl(fastSky));
    }

    /**
     * Процесс смены окружения редактора.
     */
    private void changeFastSkyImpl(final Spatial fastSky) {

        final Node stateNode = getStateNode();
        final Spatial currentFastSky = getCurrentFastSky();

        if (currentFastSky != null) {
            stateNode.detachChild(currentFastSky);
        }

        if (fastSky != null) {
            stateNode.attachChild(fastSky);
        }

        stateNode.detachChild(getModelNode());
        stateNode.detachChild(getToolNode());

        setCurrentFastSky(fastSky);

        frame = 0;
    }

    /**
     * @return выбранные модели.
     */
    private Array<Spatial> getSelected() {
        return selected;
    }

    /**
     * @return модели выделения выбранных частей.
     */
    private ObjectDictionary<Spatial, Spatial> getSelectionShape() {
        return selectionShape;
    }

    /**
     * @return материал для выделения.
     */
    private Material getSelectionMaterial() {
        return selectionMaterial;
    }

    /**
     * Обновление выбранной части модели.
     */
    public void updateSelection(final Array<Spatial> spatials) {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> updateSelectionImpl(spatials));
    }

    /**
     * Процесс обновления выбранной части модели.
     */
    private void updateSelectionImpl(final Array<Spatial> spatials) {

        final Array<Spatial> selected = getSelected();

        for (final ArrayIterator<Spatial> iterator = selected.iterator(); iterator.hasNext(); ) {

            final Spatial spatial = iterator.next();

            if (spatials.contains(spatial)) {
                continue;
            }

            removeFromSelection(spatial);
            iterator.fastRemove();
        }

        for (final Spatial spatial : spatials) {
            if (!selected.contains(spatial)) {
                addToSelection(spatial);
            }
        }

        updateToTransform();
        updateTransformCenter();
    }

    private void updateToTransform() {
        setToTransform(getSelected().first());
    }

    private void updateTransformCenter() {
        final Spatial toTransform = getToTransform();
        setTransformCenter(toTransform == null ? null : toTransform.getLocalTransform().clone());
    }

    /**
     * Добавление части модели к выделенным.
     */
    private void addToSelection(final Spatial spatial) {

        Spatial shape;

        if (spatial instanceof ParticleEmitter) {
            shape = buildBoxSelection(spatial);
        } else if (spatial instanceof Geometry) {
            shape = buildGeometrySelection((Geometry) spatial);
        } else {
            shape = buildBoxSelection(spatial);
        }

        if (shape == null) {
            return;
        }

        if (isShowSelection()) {
            final Node toolNode = getToolNode();
            toolNode.attachChild(shape);
        }

        final Array<Spatial> selected = getSelected();
        selected.add(spatial);

        final ObjectDictionary<Spatial, Spatial> selectionShape = getSelectionShape();
        selectionShape.put(spatial, shape);
    }

    /**
     * Удаление части модели из списка выделенных.
     */
    private void removeFromSelection(final Spatial spatial) {
        setTransformCenter(null);
        setToTransform(null);

        final ObjectDictionary<Spatial, Spatial> selectionShape = getSelectionShape();
        final Spatial shape = selectionShape.remove(spatial);

        if (shape != null) {
            shape.removeFromParent();
        }
    }


    /**
     * Построение выделения для модели.
     *
     * @param spatial выделяемая модель.
     * @return выделение этой модели.
     */
    private Spatial buildBoxSelection(final Spatial spatial) {

        final BoundingVolume bound = spatial.getWorldBound();

        if (bound instanceof BoundingBox) {

            final BoundingBox boundingBox = (BoundingBox) bound;

            final Geometry geometry = WireBox.makeGeometry(boundingBox);
            geometry.setName("SelectionShape");
            geometry.setMaterial(getSelectionMaterial());
            geometry.setLocalTranslation(boundingBox.getCenter().subtract(spatial.getWorldTranslation()));

            return geometry;

        } else if (bound instanceof BoundingSphere) {

            final BoundingSphere boundingSphere = (BoundingSphere) bound;

            final WireSphere wire = new WireSphere();
            wire.fromBoundingSphere(boundingSphere);

            final Geometry geometry = new Geometry("SelectionShape", wire);
            geometry.setMaterial(getSelectionMaterial());
            geometry.setLocalTranslation(boundingSphere.getCenter().subtract(spatial.getWorldTranslation()));

            return geometry;
        }

        return null;
    }

    /**
     * Построение выделения указанной геометрии.
     *
     * @param spatial геометря которую надо выделить.
     * @return выделение этой геометрии.
     */
    private Spatial buildGeometrySelection(final Geometry spatial) {

        final Mesh mesh = spatial.getMesh();

        if (mesh == null) {
            return null;
        }

        final Geometry geometry = new Geometry("SelectionShape", mesh);
        geometry.setMaterial(getSelectionMaterial());
        geometry.setLocalTransform(spatial.getWorldTransform());

        return geometry;
    }

    /**
     * @param showGrid отображать ли сетку.
     */
    private void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    /**
     * @param showSelection отображать ли выбранный узел.
     */
    private void setShowSelection(boolean showSelection) {
        this.showSelection = showSelection;
    }

    /**
     * @return отображать ли сетку.
     */
    private boolean isShowGrid() {
        return showGrid;
    }

    /**
     * @return отображать ли выбранный узел.
     */
    private boolean isShowSelection() {
        return showSelection;
    }

    /**
     * Обновленине видимости выделения.
     */
    public void updateShowSelection(final boolean showSelection) {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> updateShowSelectionImpl(showSelection));
    }

    /**
     * Процесс обновления видимости выделения.
     */
    private void updateShowSelectionImpl(boolean showSelection) {

        if (isShowSelection() == showSelection) {
            return;
        }

        final ObjectDictionary<Spatial, Spatial> selectionShape = getSelectionShape();
        final Node toolNode = getToolNode();

        if (showSelection && !selectionShape.isEmpty()) {
            selectionShape.forEach(toolNode::attachChild);
        } else if (!showSelection && !selectionShape.isEmpty()) {
            selectionShape.forEach(toolNode::detachChild);
        }

        setShowSelection(showSelection);
    }

    /**
     * Обновление отображения сетки.
     */
    public void updateShowGrid(final boolean showGrid) {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> updateShowGridImpl(showGrid));
    }

    /**
     * Процесс обновления отображения сетки.
     */
    private void updateShowGridImpl(final boolean showGrid) {

        if (isShowGrid() == showGrid) {
            return;
        }

        final Node toolNode = getToolNode();
        final Geometry grid = getGrid();

        if (showGrid) {
            toolNode.attachChild(grid);
        } else {
            toolNode.detachChild(grid);
        }

        setShowGrid(showGrid);
    }

    /**
     * Добавление кастомного фона.
     */
    public void addCustomSky(final Spatial sky) {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> addCustomSkyImpl(sky));
    }

    /**
     * Процесс добавление кастомного фона.
     */
    private void addCustomSkyImpl(Spatial sky) {
        final Array<Spatial> customSky = getCustomSky();
        customSky.add(sky);
    }

    /**
     * Удаление кастомного фона.
     */
    public void removeCustomSky(final Spatial sky) {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> removeCustomSkyImpl(sky));
    }

    /**
     * Процесс удаления кастомного фона.
     */
    private void removeCustomSkyImpl(Spatial sky) {
        final Array<Spatial> customSky = getCustomSky();
        customSky.slowRemove(sky);
    }

    /**
     * Обновить цвето-пробу.
     */
    public void updateLightProbe() {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> {

            final Node stateNode = getStateNode();
            stateNode.detachChild(getModelNode());
            stateNode.detachChild(getToolNode());

            frame = 0;
        });
    }

    public void endTransform() {
        setPickedAxis(PickedAxis.NONE);
        setActiveTransform(false);
        setDeltaVector(null);
        updateTransformCenter();
    }

    /**
     * Обработка попытки начать трансформацию.
     */
    public boolean startTransform() {

        boolean result = false;

        final Camera camera = EDITOR.getCamera();
        final InputManager inputManager = EDITOR.getInputManager();
        final Vector2f cursorPosition = inputManager.getCursorPosition();

        final CollisionResults collisionResults = new CollisionResults();

        final Vector3f position = camera.getWorldCoordinates(cursorPosition, 0f);
        final Vector3f direction = camera.getWorldCoordinates(cursorPosition, 1f);
        direction.subtractLocal(position).normalizeLocal();

        final Ray ray = new Ray();
        ray.setOrigin(position);
        ray.setDirection(direction);

        final Node transformToolNode = getTransformToolNode();
        transformToolNode.collideWith(ray, collisionResults);

        if (collisionResults.size() > 0) {

            final CollisionResult collisionResult = collisionResults.getClosestCollision();
            final TransformType transformType = getTransformType();

            if (transformType == TransformType.MOVE_TOOL) {
                final Node moveTool = getMoveTool();
                final TransformControl control = moveTool.getControl(TransformControl.class);
                control.setCollisionPlane(collisionResult);
            } else if (transformType == TransformType.ROTATE_TOOL) {
                final Node rotateTool = getRotateTool();
                final TransformControl control = rotateTool.getControl(TransformControl.class);
                control.setCollisionPlane(collisionResult);
            } else if (transformType == TransformType.SCALE_TOOL) {
                final Node scaleTool = getScaleTool();
                final TransformControl control = scaleTool.getControl(TransformControl.class);
                control.setCollisionPlane(collisionResult);
            }

            setActiveTransform(true);
            result = true;
        }

        return result;
    }

    /**
     * @param toTransform объект на трансформацию.
     */
    private void setToTransform(final Spatial toTransform) {
        this.toTransform = toTransform;
    }

    /**
     * @param transformCenter центр трансформации.
     */
    private void setTransformCenter(final Transform transformCenter) {
        this.transformCenter = transformCenter;
    }
}