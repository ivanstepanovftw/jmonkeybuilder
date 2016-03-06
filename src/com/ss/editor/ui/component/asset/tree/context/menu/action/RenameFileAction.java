package com.ss.editor.ui.component.asset.tree.context.menu.action;

import com.ss.editor.Editor;
import com.ss.editor.Messages;
import com.ss.editor.ui.component.asset.tree.resource.ResourceElement;
import com.ss.editor.ui.dialog.RenameDialog;
import com.ss.editor.ui.event.FXEventManager;
import com.ss.editor.ui.event.impl.RenamedFileEvent;
import com.ss.editor.ui.scene.EditorFXScene;
import com.ss.editor.util.EditorUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.scene.control.MenuItem;
import rlib.util.FileUtils;
import rlib.util.StringUtils;

/**
 * Реализация действия по переименованию файла.
 *
 * @author Ronn
 */
public class RenameFileAction extends MenuItem {

    private static final FXEventManager FX_EVENT_MANAGER = FXEventManager.getInstance();

    /**
     * Элемент действия.
     */
    private final ResourceElement element;

    public RenameFileAction(final ResourceElement element) {
        this.element = element;
        setText(Messages.ASSET_COMPONENT_RESOURCE_TREE_CONTEXT_MENU_RENAME_FILE);
        setOnAction(event -> processRename());
    }

    /**
     * Процесс переименования файла.
     */
    private void processRename() {

        final Editor editor = Editor.getInstance();
        final EditorFXScene scene = editor.getScene();

        final Path file = element.getFile();

        final RenameDialog renameDialog = new RenameDialog();
        renameDialog.setValidator(this::checkName);
        renameDialog.setHandler(this::processRename);
        renameDialog.setInitName(FileUtils.getNameWithoutExtension(file));
        renameDialog.show(scene.getWindow());
    }

    /**
     * Проверка введенного имени.
     */
    private Boolean checkName(final String newFileName) {

        if (!FileUtils.isValidName(newFileName)) {
            return false;
        }

        final Path file = element.getFile();
        final String extension = FileUtils.getExtension(file);

        final Path parent = file.getParent();
        final Path targetFile = parent.resolve(StringUtils.isEmpty(extension) ? newFileName : newFileName + "." + extension);

        return !Files.exists(targetFile);
    }

    /**
     * Процесс переименования.
     */
    private void processRename(final String newFileName) {

        final Path file = element.getFile();

        final String extension = FileUtils.getExtension(file);
        final String resultName = StringUtils.isEmpty(extension) ? newFileName : newFileName + "." + extension;

        final Path newFile = file.resolveSibling(resultName);

        try {
            Files.move(file, newFile);
        } catch (IOException e) {
            EditorUtil.handleException(null, this, e);
            return;
        }

        final RenamedFileEvent event = new RenamedFileEvent();
        event.setPrevFile(file);
        event.setNewFile(newFile);

        FX_EVENT_MANAGER.notify(event);
    }
}
