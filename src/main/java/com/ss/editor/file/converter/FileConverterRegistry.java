package com.ss.editor.file.converter;

import static com.ss.rlib.common.util.FileUtils.containsExtensions;
import static com.ss.rlib.common.util.array.ArrayCollectors.toArray;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.file.converter.impl.*;
import com.ss.rlib.common.logging.Logger;
import com.ss.rlib.common.logging.LoggerManager;
import com.ss.rlib.common.util.array.Array;
import com.ss.rlib.common.util.array.ConcurrentArray;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collection;

/**
 * The registry of file converters.
 *
 * @author JavaSaBr
 */
public class FileConverterRegistry {

    private static final Logger LOGGER = LoggerManager.getLogger(FileConverterRegistry.class);

    private static final FileConverterRegistry INSTANCE = new FileConverterRegistry();

    @FromAnyThread
    public static @NotNull FileConverterRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * The list of converters.
     */
    @NotNull
    private final ConcurrentArray<FileConverterDescription> descriptions;

    private FileConverterRegistry() {
        this.descriptions = ConcurrentArray.ofType(FileConverterDescription.class);
        register(BlendToJ3oFileConverter.DESCRIPTION);
        register(FbxToJ3oFileConverter.DESCRIPTION);
        register(ObjToJ3oFileConverter.DESCRIPTION);
        register(SceneToJ3oFileConverter.DESCRIPTION);
        register(MeshXmlToJ3oFileConverter.DESCRIPTION);
        register(XbufToJ3oFileConverter.DESCRIPTION);
        register(GltfToJ3oFileConverter.DESCRIPTION);
        LOGGER.info("initialized.");
    }

    /**
     * Add the new file converter descriptor.
     *
     * @param description the new descriptor.
     */
    @FromAnyThread
    public void register(@NotNull FileConverterDescription description) {
        descriptions.runInWriteLock(description, Collection::add);
    }

    /**
     * Get the list of converters.
     *
     * @return the list of converters.
     */
    @FromAnyThread
    private @NotNull ConcurrentArray<FileConverterDescription> getDescriptions() {
        return descriptions;
    }

    /**
     * Get the list of available converters for the file.
     *
     * @param path the path.
     * @return the list of available converters.
     */
    @FromAnyThread
    public @NotNull Array<FileConverterDescription> getDescriptions(@NotNull Path path) {
        var descriptions = getDescriptions();
        var stamp = descriptions.readLock();
        try {

            return descriptions.stream()
                .filter(desc -> containsExtensions(desc.getExtensions(), path))
                .collect(toArray(FileConverterDescription.class));

        } finally {
            descriptions.readUnlock(stamp);
        }
    }

    /**
     * Create a file converter using the converter description.
     *
     * @param description the converter description.
     * @param file        the file.
     * @return the new converter.
     */
    @FromAnyThread
    public @NotNull FileConverter newCreator(@NotNull FileConverterDescription description, @NotNull Path file) {
        return description.getConstructor()
            .get();
    }
}
