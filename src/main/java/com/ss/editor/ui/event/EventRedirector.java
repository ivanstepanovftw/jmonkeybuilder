package com.ss.editor.ui.event;

import static com.ss.rlib.common.util.ObjectUtils.notNull;
import com.ss.editor.config.Config;
import com.ss.editor.ui.component.editor.FileEditor;
import com.ss.editor.ui.component.editor.area.EditorAreaComponent;
import com.ss.editor.ui.util.UiUtils;
import com.ss.rlib.common.logging.Logger;
import com.ss.rlib.common.logging.LoggerManager;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.*;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

/**
 * The class for redirecting stage events to JME.
 *
 * @author JavaSaBr
 */
public class EventRedirector {

    @NotNull
    private static final Logger LOGGER = LoggerManager.getLogger(EventRedirector.class);

    /**
     * The editor area.
     */
    @NotNull
    private final EditorAreaComponent editorAreaComponent;

    /**
     * The view for showing 3D.
     */
    @NotNull
    private final Node destination;

    /**
     * The stage.
     */
    @NotNull
    private final Stage stage;

    /**
     * The flag of pressing mouse.
     */
    @NotNull
    private final boolean[] mousePressed;

    /**
     * The current mouse scene X.
     */
    private double sceneX;

    /**
     * The current mouse scene Y.
     */
    private double sceneY;

    public EventRedirector(@NotNull final EditorAreaComponent editorAreaComponent, @NotNull final Node destination,
                           @NotNull final Stage stage) {
        this.editorAreaComponent = editorAreaComponent;
        this.destination = destination;
        this.stage = stage;
        this.mousePressed = new boolean[MouseButton.values().length];
        init();
    }

    /**
     * Init listeners.
     */
    private void init() {
        stage.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {

            final EventTarget target = event.getTarget();
            if (target == destination) return;

            final FileEditor currentEditor = editorAreaComponent.getCurrentEditor();
            if (currentEditor == null) return;

            if (!isMousePressed(event.getButton())) {
                return;
            }

            setMousePressed(event.getButton(), false);

            Event.fireEvent(destination, event.copyFor(event.getSource(), destination));
        });

        stage.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {

            final EventTarget target = event.getTarget();
            if (target == destination) return;

            final FileEditor currentEditor = editorAreaComponent.getCurrentEditor();
            if (currentEditor == null || !currentEditor.isInside(event.getSceneX(), event.getSceneY(), event.getClass())) {
                return;
            }

            setMousePressed(event.getButton(), true);

            Event.fireEvent(destination, event.copyFor(event.getSource(), destination));
        });

        stage.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {

            final EventTarget target = event.getTarget();
            if (target == destination) return;

            updateCoords(event);

            final FileEditor currentEditor = editorAreaComponent.getCurrentEditor();
            if (currentEditor == null || !currentEditor.isInside(event.getSceneX(), event.getSceneY(), event.getClass())) {
                return;
            }

            Event.fireEvent(destination, event.copyFor(event.getSource(), destination));
        });

        stage.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {

            if (Config.DEV_DEBUG_JFX_MOUSE_INPUT) {
                LOGGER.debug("Mouse dragged " + event);
            }

            final EventTarget target = event.getTarget();
            if (target == destination) return;

            updateCoords(event);

            final FileEditor currentEditor = editorAreaComponent.getCurrentEditor();
            if (currentEditor == null) return;

            if (!isMousePressed(event.getButton()) && !currentEditor.isInside(event.getSceneX(), event.getSceneY(), event.getClass())) {
                return;
            }

            Event.fireEvent(destination, event.copyFor(event.getSource(), destination));
        });

        stage.addEventHandler(ScrollEvent.ANY, this::redirect);
        stage.addEventFilter(KeyEvent.KEY_PRESSED, this::redirect);
        stage.addEventFilter(KeyEvent.KEY_RELEASED, this::redirect);
    }

    private void redirect(@NotNull final GestureEvent event) {

        final EventTarget target = event.getTarget();
        if (target == destination) return;

        final FileEditor currentEditor = editorAreaComponent.getCurrentEditor();
        if (currentEditor == null || !currentEditor.isInside(event.getSceneX(), event.getSceneY(), event.getClass())) {
            return;
        }

        Event.fireEvent(destination, event.copyFor(event.getSource(), destination));
    }

    private void redirect(@NotNull final InputEvent event) {

        final EventTarget target = event.getTarget();
        if (target == destination) {
            return;
        } else if (target instanceof TextInputControl) {
            if (event instanceof KeyEvent && UiUtils.isNotHotKey((KeyEvent) event)) {
                if (Config.DEV_DEBUG_JFX_KEY_INPUT) {
                    LOGGER.debug(this, target, ev -> "Key event was skipped because it was from " + ev);
                }
                return;
            }
        }

        final EventType<? extends InputEvent> eventType = event.getEventType();
        final FileEditor currentEditor = editorAreaComponent.getCurrentEditor();

        if (Config.DEV_DEBUG_JFX_KEY_INPUT) {
            LOGGER.debug(this, event, notNull(currentEditor), (red, ev, editor) -> "Key event " + ev.getEventType() +
                    " is inside " + editor.isInside(red.getSceneX(), red.getSceneY(), ev.getClass()));
        }

        if (currentEditor == null || eventType != KeyEvent.KEY_RELEASED && !currentEditor.isInside(getSceneX(), getSceneY(), event.getClass())) {
            return;
        }

        if (Config.DEV_DEBUG_JFX_KEY_INPUT) {
            LOGGER.debug(this, event, ev -> "Redirect event " + ev);
        }

        Event.fireEvent(destination, event.copyFor(event.getSource(), destination));
    }

    /**
     * Update mouse coords.
     */
    private void updateCoords(@NotNull final MouseEvent event) {
        this.sceneX = event.getSceneX();
        this.sceneY = event.getSceneY();
    }

    /**
     * @return the current mouse scene X.
     */
    private double getSceneX() {
        return sceneX;
    }

    /**
     * @return the current mouse scene Y.
     */
    private double getSceneY() {
        return sceneY;
    }

    /**
     * @param button       the mouse button.
     * @param mousePressed true if mouse is pressed.
     */
    private void setMousePressed(final MouseButton button, final boolean mousePressed) {
        this.mousePressed[button.ordinal()] = mousePressed;
    }

    /**
     * @param button the mouse button.
     * @return true if mouse is pressed.
     */
    private boolean isMousePressed(final MouseButton button) {
        return mousePressed[button.ordinal()];
    }
}
