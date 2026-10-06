package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.atomic.particle.ParticleEntity;
import resonantinduction.registry.RIRegistries;
import resonantinduction.schematic.CreativeBuilderBlock;
import resonantinduction.schematic.Schematics;

@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class SchematicGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    static void build(GameTestHelper helper, BlockPos at, String name, Direction dir, int size) {
        helper.setBlock(at, RIRegistries.CREATIVE_BUILDER.get());
        Schematics.Schematic schematic = Schematics.ALL.stream().filter(s -> s.name().equals(name)).findFirst().orElseThrow();
        CreativeBuilderBlock.build(helper.getLevel(), helper.absolutePos(at), schematic, dir, size);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void acceleratorSchematicMakesATunnel(GameTestHelper helper) {
        BlockPos at = new BlockPos(6, 3, 3);
        build(helper, at, "accelerator", Direction.NORTH, 3);
        for (BlockPos cell : new BlockPos[] {at.offset(-2, 0, 0), at.offset(1, 0, 0), at.offset(0, 0, -2), at.offset(0, 0, 1)}) {
            helper.assertTrue(ParticleEntity.canTravel(helper.getLevel(), helper.absolutePos(cell)), "no tunnel at " + cell);
        }
        helper.assertBlockPresent(RIRegistries.ELECTROMAGNET.get(), at.offset(-3, 0, 0));
        helper.assertBlockPresent(RIRegistries.ELECTROMAGNET.get(), at.offset(-1, 0, 0));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void fissionSchematicBuildsAReactor(GameTestHelper helper) {
        BlockPos at = new BlockPos(6, 4, 3);
        build(helper, at, "fission_reactor", Direction.NORTH, 1);
        helper.assertBlockPresent(RIRegistries.REACTOR_CELL.get(), at);
        helper.assertBlockPresent(RIRegistries.ELECTRIC_TURBINE.get(), at.offset(1, 1, 1));
        helper.assertBlockPresent(Blocks.WATER, at.offset(2, 0, 2));
        helper.assertBlockPresent(RIRegistries.CONTROL_ROD.get(), at.offset(1, -1, 0));
        helper.assertBlockPresent(RIRegistries.THERMOMETER.get(), at.below());
        helper.assertBlockPresent(RIRegistries.SIREN.get(), at.below(3));
        helper.assertBlockProperty(at.offset(1, -2, 0), BlockStateProperties.FACING, Direction.UP);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void fusionSchematicBuildsAChamber(GameTestHelper helper) {
        BlockPos at = new BlockPos(6, 3, 3);
        build(helper, at, "fusion_reactor", Direction.NORTH, 1);
        helper.assertBlockPresent(RIRegistries.REACTOR_CELL.get(), at);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            helper.assertBlockPresent(RIRegistries.ELECTROMAGNET.get(), at.relative(d));
        }
        helper.assertBlockPresent(RIRegistries.ELECTROMAGNET_GLASS.get(), at.offset(2, 0, 0));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void turbineSchematicFacesThePlayer(GameTestHelper helper) {
        BlockPos at = new BlockPos(6, 3, 3);
        build(helper, at, "wind_turbine", Direction.NORTH, 1);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                helper.assertBlockProperty(at.offset(x, y, 0), BlockStateProperties.FACING, Direction.NORTH);
            }
        }
        helper.assertBlockPresent(Blocks.AIR, at.offset(0, 0, 1));
        helper.succeed();
    }
}
