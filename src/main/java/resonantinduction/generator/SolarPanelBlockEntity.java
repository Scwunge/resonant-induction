package resonantinduction.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import resonantinduction.RIConfig;
import resonantinduction.registry.RIRegistries;

/** Makes {@code solarOutput} FE a tick under open sky in daytime, unless it is raining (original: 50). */
public class SolarPanelBlockEntity extends GeneratorBlockEntity {
    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.SOLAR_PANEL_BE.get(), pos, state);
    }

    @Override
    protected int capacity() {
        return RIConfig.get(RIConfig.SOLAR_OUTPUT) * 20;
    }

    @Override
    protected int generate(ServerLevel level) {
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling() || !level.canSeeSky(worldPosition.above())) {
            return 0;
        }
        if (!level.isDay() || level.isRaining() || level.isThundering()) {
            return 0;
        }
        return RIConfig.get(RIConfig.SOLAR_OUTPUT);
    }
}
