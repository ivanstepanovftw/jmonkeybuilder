package com.ss.editor.model.undo.impl.emitter;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.influencers.ParticleInfluencer;
import com.ss.editor.model.undo.editor.ModelChangeConsumer;
import com.ss.editor.model.undo.impl.AbstractEditorOperation;
import org.jetbrains.annotations.NotNull;

/**
 * The implementation of the {@link AbstractEditorOperation} to change a {@link ParticleInfluencer} to a {@link
 * ParticleEmitter}.
 *
 * @author JavaSaBr.
 */
public class ChangeParticleInfluencerOperation extends AbstractEditorOperation<ModelChangeConsumer> {

    /**
     * The particle emitter.
     */
    @NotNull
    private final ParticleEmitter emitter;

    /**
     * The prev influencer.
     */
    @NotNull
    private ParticleInfluencer prevInfluencer;

    /**
     * Instantiates a new Add particle influencer operation.
     *
     * @param influencer the influencer
     * @param emitter    the particle emitter
     */
    public ChangeParticleInfluencerOperation(@NotNull final ParticleInfluencer influencer,
                                             @NotNull final ParticleEmitter emitter) {
        this.prevInfluencer = influencer;
        this.emitter = emitter;
    }

    @Override
    protected void redoImpl(@NotNull final ModelChangeConsumer editor) {
        EXECUTOR_MANAGER.addJmeTask(() -> switchInfluencer(editor));
    }

    private void switchInfluencer(final @NotNull ModelChangeConsumer editor) {

        final ParticleInfluencer influencer = emitter.getParticleInfluencer();
        final ParticleInfluencer newInfluencer = prevInfluencer;
        prevInfluencer = influencer;
        emitter.setParticleInfluencer(newInfluencer);

        EXECUTOR_MANAGER.addFxTask(() -> editor.notifyFxReplaced(emitter, prevInfluencer, newInfluencer, true, true));
    }

    @Override
    protected void undoImpl(@NotNull final ModelChangeConsumer editor) {
        EXECUTOR_MANAGER.addJmeTask(() -> switchInfluencer(editor));
    }
}
