package org.magic.misc;



public interface WrappedEntityRenderState {
    int magicaddons$entityTintColor();
    boolean magicaddons$isWrappedEntity();

    void magicaddons$setWrappedEntity(boolean value);
    void magicaddons$setWrappedEntityTintColor(int value);

    int magicaddons$headOutlineColor();

    void magicaddons$setHeadOutlineColor(int value);

    float magicaddons$smolBody();

    float magicaddons$smolHead();

    void magicaddons$setSmolFactors(float body, float head);
}
