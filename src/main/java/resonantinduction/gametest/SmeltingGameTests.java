package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.archaic.CastingMoldBlockEntity;
import resonantinduction.archaic.FireboxBlock;
import resonantinduction.archaic.FireboxBlockEntity;
import resonantinduction.archaic.HotPlateBlockEntity;
import resonantinduction.registry.RIRegistries;
import resonantinduction.resource.DustPileBlock;
import resonantinduction.resource.MaterialBlockEntity;
import resonantinduction.resource.MaterialFluid;
import resonantinduction.resource.PoolBlock;

@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class SmeltingGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    static FireboxBlockEntity firebox(GameTestHelper helper, BlockPos pos, boolean electric, boolean coal) {
        helper.setBlock(pos, (electric ? RIRegistries.ELECTRIC_FIREBOX : RIRegistries.FIREBOX).get());
        FireboxBlockEntity be = helper.getBlockEntity(pos);
        if (coal) {
            be.fuel().insertItem(0, new ItemStack(Items.COAL), false);
        }
        return be;
    }

    static void pile(GameTestHelper helper, BlockPos pos, boolean refined, int layers, String material) {
        helper.setBlock(pos, (refined ? RIRegistries.REFINED_DUST_PILE : RIRegistries.DUST_PILE).get().defaultBlockState().setValue(DustPileBlock.LAYERS, layers));
        ((MaterialBlockEntity) helper.getBlockEntity(pos)).setMaterial(material);
    }

    /** Glass on the four sides of {@code pos}, except {@code open}. */
    static void wallIn(GameTestHelper helper, BlockPos pos, Direction open) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (d != open) {
                helper.setBlock(pos.relative(d), Blocks.GLASS);
            }
        }
    }

    static void assertPool(GameTestHelper helper, BlockPos pos, int level, String material) {
        helper.assertBlockPresent(RIRegistries.MOLTEN_POOL.get(), pos);
        helper.assertBlockProperty(pos, PoolBlock.LEVEL, level);
        helper.assertTrue(PoolBlock.material(helper.getLevel(), helper.absolutePos(pos)).equals(material), "pool is not " + material);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void fireboxBurnsFuelAndLightsAFire(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        FireboxBlockEntity firebox = firebox(helper, pos, false, true);
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(pos, FireboxBlock.LIT, true);
            helper.assertBlockPresent(Blocks.FIRE, pos.above());
            helper.assertTrue(firebox.fuel().getStackInSlot(0).isEmpty(), "coal not used");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void fireboxBurnsABucketOfLava(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        firebox(helper, pos, false, false);
        IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.NORTH);
        helper.assertTrue(tank != null, "no fluid handler");
        helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "took water");
        helper.assertTrue(tank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "did not take lava");
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(pos, FireboxBlock.LIT, true);
            helper.assertTrue(tank.getFluidInTank(0).isEmpty(), "lava not burnt");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void electricFireboxBurnsOnPower(GameTestHelper helper) {
        BlockPos plain = new BlockPos(1, 1, 3);
        BlockPos electric = new BlockPos(5, 1, 3);
        firebox(helper, plain, false, false);
        firebox(helper, electric, true, false);
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(plain), Direction.NORTH) == null,
                "a plain firebox takes power");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(electric), Direction.UP) == null,
                "the electric firebox takes power from the top");
        IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(electric), Direction.NORTH);
        helper.assertTrue(energy != null && energy.receiveEnergy(100000, false) == 100000, "did not take power");
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(electric, FireboxBlock.LIT, true);
            helper.assertBlockProperty(plain, FireboxBlock.LIT, false);
        });
    }

    /** About 190 ticks to heat and melt 125 mB of iron at 100 kW. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void fireboxMeltsRefinedDustIntoMoltenMetal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        firebox(helper, pos, false, true);
        pile(helper, pos.above(), true, 1, "iron");
        helper.succeedWhen(() -> assertPool(helper, pos.above(), 1, "iron"));
    }

    /** Dirty dust gives half the metal refined dust does. */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void dirtyDustMeltsIntoHalfAsMuch(GameTestHelper helper) {
        BlockPos refined = new BlockPos(1, 1, 3);
        BlockPos dirty = new BlockPos(5, 1, 3);
        for (BlockPos pos : new BlockPos[] {refined, dirty}) {
            firebox(helper, pos, false, true);
            wallIn(helper, pos.above(), null);
        }
        pile(helper, refined.above(), true, 2, "copper");
        pile(helper, dirty.above(), false, 2, "copper");
        helper.succeedWhen(() -> {
            assertPool(helper, refined.above(), 2, "copper");
            assertPool(helper, dirty.above(), 1, "copper");
        });
    }

    /** About 52 ticks per 100 mB, so a bucket boils away in under half a minute. */
    @GameTest(template = TEMPLATE, timeoutTicks = 700)
    public static void fireboxBoilsAwayWater(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        firebox(helper, pos, false, true);
        wallIn(helper, pos.above(), null);
        helper.setBlock(pos.above(), Blocks.WATER);
        helper.runAfterDelay(100, () -> helper.assertBlockPresent(Blocks.WATER, pos.above()));
        helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(pos.above()).getFluidState().isEmpty(), "water still there"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 500)
    public static void hotPlateSmeltsWholeStacksOnABurningFirebox(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        firebox(helper, pos, false, true);
        helper.setBlock(pos.above(), RIRegistries.HOT_PLATE.get());
        HotPlateBlockEntity plate = helper.getBlockEntity(pos.above());
        plate.inventory().insertItem(0, new ItemStack(Items.RAW_IRON, 2), false);
        plate.inventory().insertItem(3, new ItemStack(Items.SAND), false);
        helper.assertTrue(!plate.inventory().insertItem(1, new ItemStack(Items.STICK), false).isEmpty(), "took something it can't smelt");
        // Two items take twice as long as one.
        helper.runAfterDelay(300, () -> {
            helper.assertTrue(plate.inventory().getStackInSlot(3).is(Items.GLASS), "sand not smelted after 15 s");
            helper.assertTrue(plate.inventory().getStackInSlot(0).is(Items.RAW_IRON), "two raw iron smelted as fast as one");
        });
        helper.succeedWhen(() -> {
            ItemStack iron = plate.inventory().getStackInSlot(0);
            helper.assertTrue(iron.is(Items.IRON_INGOT) && iron.getCount() == 2, "expected 2 iron ingots, got " + iron);
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void hotPlateNeedsABurningFirebox(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        firebox(helper, pos, false, false);
        helper.setBlock(pos.above(), RIRegistries.HOT_PLATE.get());
        HotPlateBlockEntity plate = helper.getBlockEntity(pos.above());
        plate.inventory().insertItem(0, new ItemStack(Items.SAND), false);
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(plate.inventory().getStackInSlot(0).is(Items.SAND) && plate.smeltTime(0) == 0, "smelting without heat");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void castingMoldCastsAPoolIntoIngots(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.CASTING_MOLD.get());
        CastingMoldBlockEntity mold = helper.getBlockEntity(pos);
        PoolBlock.place(helper.getLevel(), helper.absolutePos(pos.above()), PoolBlock.Kind.MOLTEN, "iron", 8);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, pos.above());
            ItemStack out = mold.output().getStackInSlot(0);
            helper.assertTrue(out.is(Items.IRON_INGOT) && out.getCount() == 10, "expected 10 iron ingots, got " + out);
            helper.assertTrue(mold.tank().isEmpty(), "metal left in the mold");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void castingMoldTakesPipedMoltenMetal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.CASTING_MOLD.get());
        CastingMoldBlockEntity mold = helper.getBlockEntity(pos);
        IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.WEST);
        helper.assertTrue(tank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "took lava");
        helper.assertTrue(tank.fill(MaterialFluid.stack(RIRegistries.MOLTEN_METAL.get(), "gold", 250), IFluidHandler.FluidAction.EXECUTE) == 250, "did not take molten gold");
        helper.assertTrue(tank.fill(MaterialFluid.stack(RIRegistries.MOLTEN_METAL.get(), "iron", 100), IFluidHandler.FluidAction.EXECUTE) == 0, "mixed iron into gold");
        helper.succeedWhen(() -> {
            ItemStack out = mold.output().getStackInSlot(0);
            helper.assertTrue(out.is(Items.GOLD_INGOT) && out.getCount() == 2, "expected 2 gold ingots, got " + out);
            helper.assertTrue(mold.tank().getFluidAmount() == 50, "expected 50 mB left, got " + mold.tank().getFluidAmount());
            IFluidHandler below = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.DOWN);
            helper.assertTrue(below.drain(1000, IFluidHandler.FluidAction.SIMULATE).isEmpty(), "metal can be drained out");
        });
    }

    /** A molten pool spreads like the original's finite fluid; poured off a firebox onto the space above a mold, it's cast. */
    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void moltenMetalFlowsOffTheFireboxIntoAMold(GameTestHelper helper) {
        BlockPos fire = new BlockPos(2, 1, 3);
        BlockPos moldPos = fire.east();
        firebox(helper, fire, false, true);
        helper.setBlock(moldPos, RIRegistries.CASTING_MOLD.get());
        CastingMoldBlockEntity mold = helper.getBlockEntity(moldPos);
        wallIn(helper, fire.above(), Direction.EAST);
        wallIn(helper, moldPos.above(), Direction.WEST);
        pile(helper, fire.above(), true, 4, "iron");
        // 500 mB melts into a pool four deep; it shares out with the space over the mold (two levels, cast), then again (one
        // level, cast), and a one-level puddle stays on the firebox.
        helper.succeedWhen(() -> {
            ItemStack out = mold.output().getStackInSlot(0);
            helper.assertTrue(out.is(Items.IRON_INGOT) && out.getCount() == 3, "expected 3 iron ingots, got " + out);
            assertPool(helper, fire.above(), 1, "iron");
            helper.assertBlockPresent(Blocks.AIR, moldPos.above());
        });
    }
}
