package com.ss.editor.part3d.editor.control.impl;

import static com.ss.editor.part3d.editor.control.impl.InputStateEditor3dPartControl.*;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.ss.editor.annotation.BackgroundThread;
import com.ss.editor.annotation.JmeThread;
import com.ss.editor.part3d.editor.Editor3dPart;
import com.ss.editor.part3d.editor.ExtendableEditor3dPart;
import com.ss.editor.ui.component.editor.FileEditor;
import com.ss.editor.util.JmeUtils;
import com.ss.rlib.common.util.ObjectUtils;
import com.ss.rlib.common.util.dictionary.ObjectDictionary;
import javafx.scene.input.KeyCode;
import org.jetbrains.annotations.NotNull;

/**
 * The control to redirect some input events from {@link Editor3dPart} to {@link FileEditor}.
 *
 * @author JavaSaBr
 */
public class KeyEventRedirectEditor3dPartControl extends BaseInputEditor3dPartControl<ExtendableEditor3dPart>
        implements ActionListener {

    private static final KeyCode[] KEY_CODES = new KeyCode[Byte.MAX_VALUE * 3];

    static {
        KEY_CODES[KeyInput.KEY_C] = KeyCode.C;
        KEY_CODES[KeyInput.KEY_S] = KeyCode.S;
        KEY_CODES[KeyInput.KEY_R] = KeyCode.R;
        KEY_CODES[KeyInput.KEY_DELETE] = KeyCode.DELETE;
        KEY_CODES[KeyInput.KEY_P] = KeyCode.P;
        KEY_CODES[KeyInput.KEY_L] = KeyCode.L;
    }

    @NotNull
    private final ObjectDictionary<String, KeyTrigger> triggers;

    @NotNull
    private final String[] mappings;

    protected KeyEventRedirectEditor3dPartControl(
            @NotNull ExtendableEditor3dPart editor3dPart,
            @NotNull ObjectDictionary<String, KeyTrigger> triggers,
            @NotNull String[] mappings
    ) {
        super(editor3dPart);
        this.triggers = triggers;
        this.mappings = mappings;

        triggers.forEach(actionHandlers, (handlers, mapping, trigger) ->
                handlers.put(mapping, (isPressed, tpf) ->
                        makeHandler(editor3dPart, KEY_CODES[trigger.getKeyCode()], isPressed)));
    }

    @BackgroundThread
    private void makeHandler(@NotNull ExtendableEditor3dPart editor3dPart, @NotNull KeyCode keyCode, boolean isPressed) {

        var fileEditor = editor3dPart.getFileEditor();
        var isControlDown =  editor3dPart.getBooleanProperty(PROP_IS_CONTROL_DOWN);
        var isShiftDown = editor3dPart.getBooleanProperty(PROP_IS_SHIFT_DOWN);
        var isButtonMiddleDown =editor3dPart.getBooleanProperty(PROP_IS_BUTTON_MIDDLE_DOWN);

        fileEditor.handleKeyAction(ObjectUtils.notNull(keyCode), isPressed,
                isControlDown, isShiftDown, isButtonMiddleDown);
    }

    @Override
    @JmeThread
    public void register(@NotNull InputManager inputManager) {
        triggers.forEach(inputManager, JmeUtils::addMapping);
        inputManager.addListener(this, mappings);
    }
}