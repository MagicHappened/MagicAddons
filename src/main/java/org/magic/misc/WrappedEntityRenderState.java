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

    /** How much Smol People shrinks this entity's body and its head; 1 and 1 when it does not. */
    float magicaddons$smolBody();

    float magicaddons$smolHead();

    void magicaddons$setSmolFactors(float body, float head);
}
