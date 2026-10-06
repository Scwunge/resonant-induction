package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.archaic.MillstoneBlock;
import resonantinduction.mechanical.gear.GearBlock;
import resonantinduction.mechanical.process.GrindingWheelBlockEntity;
import resonantinduction.mechanical.process.MachineBlock;
import resonantinduction.registry.RIRegistries;
import resonantinduction.resource.Materials;
import resonantinduction.resource.PoolBlock;

import java.util.List;

@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class ProcessingGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    static List<ItemEntity> items(GameTestHelper helper, BlockPos around, double radius) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(around)).inflate(radius));
    }

    static boolean has(List<ItemEntity> items, net.minecraft.world.item.Item item, String material) {
        return items.stream().anyMatch(e -> e.getItem().is(item) && (material == null || material.equals(Materials.material(e.getItem()))));
    }

    /** Five strikes, one per turn; at most half a turn a tick, as in the original, so about ten seconds. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void pistonCrushesOreIntoRubble(GameTestHelper helper) {
        BlockPos piston = new BlockPos(3, 1, 3);
        helper.setBlock(piston, RIRegistries.MECHANICAL_PISTON.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(piston.west(), RIRegistries.GEARS.get(GearBlock.CREATIVE).get().defaultBlockState().setValue(GearBlock.ATTACH, Direction.EAST));
        helper.setBlock(piston.east(), Blocks.IRON_ORE);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, piston.east());
            helper.assertTrue(has(items(helper, piston.east(), 1.5), RIRegistries.RUBBLE.get(), "iron"), "no iron rubble from the crushed ore");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void pistonPushesOtherBlocks(GameTestHelper helper) {
        BlockPos piston = new BlockPos(3, 1, 3);
        helper.setBlock(piston, RIRegistries.MECHANICAL_PISTON.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(piston.west(), RIRegistries.GEARS.get(GearBlock.CREATIVE).get().defaultBlockState().setValue(GearBlock.ATTACH, Direction.EAST));
        helper.setBlock(piston.east(), Blocks.OAK_PLANKS);
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.OAK_PLANKS, piston.east(2)));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void grindingWheelGrindsRubbleIntoDust(GameTestHelper helper) {
        BlockPos wheel = new BlockPos(3, 1, 3);
        helper.setBlock(wheel, RIRegistries.GRINDING_WHEEL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        GrindingWheelBlockEntity be = helper.getBlockEntity(wheel);
        // The wheel pushes things off itself; a wall around its top holds the item against it.
        for (Direction d : Direction.Plane.HORIZONTAL) {
            helper.setBlock(wheel.above().relative(d), Blocks.GLASS);
        }
        // Drive it hard (a big turbine would), so it works every tick.
        helper.onEachTick(() -> {
            be.node().torque = 5000;
            be.node().angularVelocity = 0.1;
        });
        ItemEntity rubble = new ItemEntity(helper.getLevel(), helper.absolutePos(wheel).getX() + 0.5, helper.absolutePos(wheel).getY() + 1, helper.absolutePos(wheel).getZ() + 0.5,
                Materials.of(RIRegistries.RUBBLE.get(), "iron", 1));
        rubble.setDeltaMovement(0, 0, 0);
        helper.getLevel().addFreshEntity(rubble);
        helper.succeedWhen(() -> helper.assertTrue(items(helper, wheel, 3).stream().filter(e -> e.getItem().is(RIRegistries.DUST.get())).mapToInt(e -> e.getItem().getCount()).sum() == 2,
                "rubble did not grind into two dust"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void mixerTurnsDustAndWaterIntoMixture(GameTestHelper helper) {
        BlockPos mixer = new BlockPos(4, 1, 3);
        helper.setBlock(mixer, RIRegistries.MIXER.get());
        helper.setBlock(mixer.above(), RIRegistries.GEARS.get(2).get().defaultBlockState().setValue(GearBlock.ATTACH, Direction.DOWN));
        // A steady, gentle drive (like a small turbine).
        helper.onEachTick(() -> {
            var gear = (resonantinduction.mechanical.MechanicalBlockEntity) helper.getBlockEntity(mixer.above());
            gear.node().torque = 50;
            gear.node().angularVelocity = 3;
        });
        for (int x = -1; x <= 1; x += 2) {
            for (int z = -1; z <= 1; z += 2) {
                helper.setBlock(mixer.offset(x, 0, z), Blocks.STONE);
            }
        }
        // The mixer stands in a little pool: water on its four sides, a wall around that, a floor below.
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                helper.setBlock(mixer.offset(x, -1, z), Blocks.STONE);
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    helper.setBlock(mixer.offset(x, 0, z), Blocks.STONE);
                    helper.setBlock(mixer.offset(x, 1, z), Blocks.GLASS);
                }
            }
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            helper.setBlock(mixer.relative(d), Blocks.WATER);
        }
        BlockPos water = mixer.east();
        ItemEntity dust = new ItemEntity(helper.getLevel(), helper.absolutePos(water).getX() + 0.5, helper.absolutePos(water).getY() + 0.3, helper.absolutePos(water).getZ() + 0.5,
                Materials.of(RIRegistries.DUST.get(), "gold", 1));
        helper.getLevel().addFreshEntity(dust);
        helper.succeedWhen(() -> {
            boolean found = false;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos p = helper.absolutePos(mixer.relative(d));
                if (helper.getLevel().getBlockState(p).is(RIRegistries.MIXTURE_POOL.get()) && PoolBlock.material(helper.getLevel(), p).equals("gold")) {
                    found = true;
                }
            }
            helper.assertTrue(found, "no gold mixture around the mixer");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void filterWashesMixtureIntoRefinedDust(GameTestHelper helper) {
        BlockPos filter = new BlockPos(3, 2, 3);
        helper.setBlock(filter, RIRegistries.FILTER.get());
        // Walled in, so the mixture can't spread out sideways.
        SmeltingGameTests.wallIn(helper, filter.above(), null);
        PoolBlock.place(helper.getLevel(), helper.absolutePos(filter.above()), PoolBlock.Kind.MIXTURE, "copper", 8);
        helper.succeedWhen(() -> {
            helper.assertTrue(has(items(helper, filter.above(), 1.5), RIRegistries.REFINED_DUST.get(), "copper"), "no refined copper dust");
            helper.assertBlockProperty(filter.above(), PoolBlock.LEVEL, 6);
        });
    }

    @GameTest(template = TEMPLATE)
    public static void dustSmeltsIntoIngots(GameTestHelper helper) {
        var level = helper.getLevel();
        for (var item : List.of(RIRegistries.DUST.get(), RIRegistries.REFINED_DUST.get())) {
            SingleRecipeInput input = new SingleRecipeInput(Materials.of(item, "iron", 1));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
            helper.assertTrue(recipe.isPresent(), "no furnace recipe for " + item);
            ItemStack out = recipe.get().value().assemble(input, level.registryAccess());
            helper.assertTrue(out.is(Items.IRON_INGOT), "iron dust smelts into " + out);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @SuppressWarnings("removal")
    public static void millstoneGrindsWithACrank(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.MILLSTONE.get());
        MillstoneBlock.Tile mill = helper.getBlockEntity(pos);
        mill.inventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 2));
        var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(RIRegistries.HAND_CRANK.get()));
        for (int i = 0; i < 21; i++) {
            helper.useBlock(pos, player);
        }
        helper.assertTrue(mill.inventory().getStackInSlot(0).getCount() == 1, "millstone did not use up one cobblestone after 21 turns");
        helper.assertTrue(player.getInventory().countItem(Items.SAND) == 1, "player did not get sand");
        helper.succeed();
    }
}
