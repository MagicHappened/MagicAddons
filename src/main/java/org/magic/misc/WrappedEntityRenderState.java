package org.magic.misc;



public interface WrappedEntityRenderState {
    int magicaddons$entityTintColor();
    boolean magicaddons$isWrappedEntity();

    void magicaddons$setWrappedEntity(boolean value);
    void magicaddons$setWrappedEntityTintColor(int value);

    /**
     * An outline for the head alone, kept off the render state's own outlineColor because that
     * one outlines the whole body: a hidden entity with an outline colour is drawn as an outline
     * of all of itself, which is how highlighted mobs are outlined.
     */
    int magicaddons$headOutlineColor();

    void magicaddons$setHeadOutlineColor(int value);
}
