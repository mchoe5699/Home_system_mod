package com.qwerty.homenet.block;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class SmartLightBlock extends ControlBlock {
    public SmartLightBlock(Properties props) {
        super(props, false);
    }

    @Override
    public DeviceType defaultType() {
        return DeviceType.LIGHT;
    }
}
