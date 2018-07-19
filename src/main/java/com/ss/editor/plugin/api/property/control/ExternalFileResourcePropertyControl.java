package com.ss.editor.plugin.api.property.control;

import com.ss.editor.Messages;
import com.ss.editor.annotation.FxThread;
import com.ss.editor.plugin.api.property.PropertyDefinition;
import com.ss.editor.ui.dialog.file.chooser.ExternalFileEditorDialog;
import com.ss.rlib.common.util.VarTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The control to edit files from file system.
 *
 * @author JavaSaBr
 */
public class ExternalFileResourcePropertyControl extends ResourcePropertyEditorControl<Path> {

    /**
     * The target extension.
     */
    @Nullable
    private final String extension;

    public ExternalFileResourcePropertyControl(
            @NotNull VarTable vars,
            @NotNull PropertyDefinition definition,
            @NotNull Runnable validationCallback
    ) {
        super(vars, definition, validationCallback);
        this.extension = definition.getExtension();
    }

    @Override
    @FxThread
    protected void chooseNewResource() {
        super.chooseNewResource();

        var dialog = new ExternalFileEditorDialog(this::openExternalFile);
        dialog.setTitleText(Messages.ASSET_EDITOR_DIALOG_TITLE);
        dialog.setInitDirectory(Paths.get(System.getProperty("user.home")));
        dialog.show();
    }

    /**
     * Handle selected file.
     *
     * @param path the selected file.
     */
    @FxThread
    private void openExternalFile(@NotNull Path path) {
        setPropertyValue(path);
        changed();
        reload();
    }

    @Override
    @FxThread
    protected void reloadImpl() {

        resourceLabel.setText(getPropertyValueOpt()
                .map(Path::toString)
                .orElse(NOT_SELECTED));

        super.reloadImpl();
    }
}
