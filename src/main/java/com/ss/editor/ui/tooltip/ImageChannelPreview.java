package com.ss.editor.ui.tooltip;

import static com.ss.rlib.util.ObjectUtils.notNull;
import com.ss.editor.annotation.FXThread;
import com.ss.editor.manager.JavaFXImageManager;
import com.ss.editor.ui.css.CSSClasses;
import com.ss.rlib.ui.util.FXUtils;
import javafx.scene.image.*;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * THe implementation of tooltip for showing image channels.
 *
 * @author JavaSaBr
 */
public class ImageChannelPreview extends CustomTooltip<GridPane> {

    @NotNull
    private static final JavaFXImageManager IMAGE_MANAGER = JavaFXImageManager.getInstance();

    /**
     * The red image.
     */
    @Nullable
    private WritableImage redImage;

    /**
     * The green image.
     */
    @Nullable
    private WritableImage greenImage;

    /**
     * The blue image.
     */
    @Nullable
    private WritableImage blueImage;

    /**
     * The alpha image.
     */
    @Nullable
    private WritableImage alphaImage;

    /**
     * The red image view.
     */
    @Nullable
    private ImageView redView;

    /**
     * The green image view.
     */
    @Nullable
    private ImageView greenView;

    /**
     * The blue image view.
     */
    @Nullable
    private ImageView blueView;

    /**
     * The alpha image view.
     */
    @Nullable
    private ImageView alphaView;

    /**
     * The file.
     */
    @Nullable
    private Path file;

    /**
     * The resource path.
     */
    @Nullable
    private String resourcePath;

    /**
     * The flag to build preview from the file.
     */
    private boolean needToBuildFile;

    /**
     * The flag to build preview from the resource path.
     */
    private boolean needToBuildResource;

    /**
     * @return the alpha image.
     */
    @FXThread
    private @NotNull WritableImage getAlphaImage() {
        return notNull(alphaImage);
    }

    /**
     * @return the red image.
     */
    @FXThread
    private @NotNull WritableImage getRedImage() {
        return notNull(redImage);
    }

    /**
     * @return the blue image.
     */
    @FXThread
    private @NotNull WritableImage getBlueImage() {
        return notNull(blueImage);
    }

    /**
     * @return the green image.
     */
    @FXThread
    private @NotNull WritableImage getGreenImage() {
        return notNull(greenImage);
    }

    @Override
    protected @NotNull GridPane createRoot() {
        final GridPane gridPane = new GridPane();
        FXUtils.addClassesTo(gridPane, CSSClasses.DEF_GRID_PANE, CSSClasses.IMAGE_CHANNEL_PREVIEW);
        return gridPane;
    }

    /**
     * @return the alpha image view.
     */
    @FXThread
    private @NotNull ImageView getAlphaView() {
        return notNull(alphaView);
    }

    /**
     * @return the blue image view.
     */
    @FXThread
    private @NotNull ImageView getBlueView() {
        return notNull(blueView);
    }

    /**
     * @return the green image view.
     */
    @FXThread
    private @NotNull ImageView getGreenView() {
        return notNull(greenView);
    }

    /**
     * @return the red image view.
     */
    @FXThread
    private @NotNull ImageView getRedView() {
        return notNull(redView);
    }

    /**
     * Show the file.
     *
     * @param file the file
     */
    @FXThread
    public void showImage(@Nullable final Path file) {
        setFile(file);
        setNeedToBuildFile(true);
        setNeedToBuildResource(false);
    }

    /**
     * Show the resource.
     *
     * @param resourcePath the resource path.
     */
    @FXThread
    public void showImage(@Nullable final String resourcePath) {
        setResourcePath(resourcePath);
        setNeedToBuildResource(true);
        setNeedToBuildFile(false);
    }

    /**
     * Clean.
     */
    @FXThread
    public void clean() {
        setNeedToBuildFile(true);
        setNeedToBuildResource(false);
        setFile(null);
    }

    /**
     * @param resourcePath the resource path.
     */
    private void setResourcePath(@Nullable final String resourcePath) {
        this.resourcePath = resourcePath;
    }

    /**
     * @return the resource path.
     */
    private @Nullable String getResourcePath() {
        return resourcePath;
    }

    /**
     * @param file the file.
     */
    private void setFile(@Nullable final Path file) {
        this.file = file;
    }

    /**
     * @return the file.
     */
    private @Nullable Path getFile() {
        return file;
    }

    /**
     * @param needToBuildFile true of need to build from the file.
     */
    private void setNeedToBuildFile(final boolean needToBuildFile) {
        this.needToBuildFile = needToBuildFile;
    }

    /**
     * @return true of need to build from the file.
     */
    private boolean isNeedToBuildFile() {
        return needToBuildFile;
    }

    /**
     * @param needToBuildResource true of need to build from the resource path.
     */
    private void setNeedToBuildResource(final boolean needToBuildResource) {
        this.needToBuildResource = needToBuildResource;
    }

    /**
     * @return true of need to build from the resource path.
     */
    private boolean isNeedToBuildResource() {
        return needToBuildResource;
    }

    @Override
    protected void show() {
        super.show();

        if (redImage == null) {
            redImage = new WritableImage(120, 120);
            greenImage = new WritableImage(120, 120);
            blueImage = new WritableImage(120, 120);
            alphaImage = new WritableImage(120, 120);

            redView = new ImageView();
            greenView = new ImageView();
            blueView = new ImageView();
            alphaView = new ImageView();

            final GridPane root = getRoot();
            root.add(redView, 0, 0);
            root.add(greenView, 1, 0);
            root.add(blueView, 0, 1);
            root.add(alphaView, 1, 1);
        }

        if (isNeedToBuildFile()) {
            final Path file = getFile();
            buildPreview(file == null ? null : IMAGE_MANAGER.getImagePreview(file, 120, 120));
            setNeedToBuildFile(false);
        } else if (isNeedToBuildResource()) {
            final String resourcePath = getResourcePath();
            buildPreview(IMAGE_MANAGER.getImagePreview(resourcePath, 120, 120));
            setNeedToBuildResource(false);
        }
    }

    private void buildPreview(@Nullable final Image image) {

        if (image == null || image.getWidth() != 120) {

            final ImageView redView = getRedView();
            redView.setImage(null);

            final ImageView greenView = getGreenView();
            greenView.setImage(null);

            final ImageView blueView = getBlueView();
            blueView.setImage(null);

            final ImageView alphaView = getAlphaView();
            alphaView.setImage(null);
            return;
        }

        final PixelReader pixelReader = image.getPixelReader();

        final WritableImage alphaImage = getAlphaImage();
        final PixelWriter alphaWriter = alphaImage.getPixelWriter();

        final WritableImage redImage = getRedImage();
        final PixelWriter redWriter = redImage.getPixelWriter();

        final WritableImage greenImage = getGreenImage();
        final PixelWriter greenWriter = greenImage.getPixelWriter();

        final WritableImage blueImage = getBlueImage();
        final PixelWriter blueWriter = blueImage.getPixelWriter();

        for (int y = 0, height = (int) image.getHeight(); y < height; y++) {
            for (int x = 0, width = (int) image.getWidth(); x < width; x++) {

                final int argb = pixelReader.getArgb(x, y);

                final int alpha = argb >>> 24;
                final int red = (argb >> 16) & 0xff;
                final int green = (argb >> 8) & 0xff;
                final int blue = (argb) & 0xff;

                redWriter.setArgb(x, y, ((255 << 24) | (red << 16) | (red << 8) | red));
                greenWriter.setArgb(x, y, ((255 << 24) | (green << 16) | (green << 8) | green));
                blueWriter.setArgb(x, y, ((255 << 24) | (blue << 16) | (blue << 8) | blue));
                alphaWriter.setArgb(x, y, ((255 << 24) | (alpha << 16) | (alpha << 8) | alpha));
            }
        }

        final ImageView redView = getRedView();
        redView.setImage(null);
        redView.setImage(redImage);

        final ImageView greenView = getGreenView();
        greenView.setImage(null);
        greenView.setImage(greenImage);

        final ImageView blueView = getBlueView();
        blueView.setImage(null);
        blueView.setImage(blueImage);

        final ImageView alphaView = getAlphaView();
        alphaView.setImage(null);
        alphaView.setImage(alphaImage);
    }
}
