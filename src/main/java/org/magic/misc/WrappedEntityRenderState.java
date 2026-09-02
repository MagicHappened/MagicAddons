package org.magic.misc;



public interface WrappedEntityRenderState {
    int magicaddons$entityTintColor();
    boolean magicaddons$isWrappedEntity();

    void magicaddons$setWrappedEntity(boolean value);
    void magicaddons$setWrappedEntityTintColor(int value);

    /**
     * An outline for the head alone. Kept separate from the render state's outlineColor, which
     * outlines the entire entity.
     */
    int magicaddons$headOutlineColor();

    void magicaddons$setHeadOutlineColor(int value);
}
