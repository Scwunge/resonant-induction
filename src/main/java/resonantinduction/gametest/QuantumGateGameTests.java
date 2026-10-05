package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import resonantinduction.ResonantInduction;
import resonantinduction.quantum.QuantumGateBlockEntity;
import resonantinduction.registry.RIRegistries;

/** Each test uses its own frequency (glyph pattern) so tests running side by side don't share gates. */
@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class QuantumGateGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    static QuantumGateBlockEntity gate(GameTestHelper helper, BlockPos pos, int[] glyphs) {
        helper.setBlock(pos, RIRegistries.QUANTUM_GATE.get());
        QuantumGateBlockEntity gate = helper.getBlockEntity(pos);
        for (int slot = 0; slot < glyphs.length; slot++) {
            gate.setGlyph(slot, glyphs[slot]);
        }
        return gate;
    }

    @GameTest(template = TEMPLATE)
    public static void frequencyNeedsAllEightGlyphs(GameTestHelper helper) {
        QuantumGateBlockEntity gate = gate(helper, new BlockPos(2, 1, 2), new int[]{1, 2, 3, 0, 1, 2, 3});
        helper.assertTrue(gate.frequency() == -1, "a gate with 7 glyphs has a frequency");
        gate.setGlyph(7, 1);
        int expected = 1 + 2 * 4 + 3 * 16 + 0 + 1 * 256 + 2 * 1024 + 3 * 4096 + 1 * 16384;
        helper.assertTrue(gate.frequency() == expected, "frequency " + gate.frequency() + " != " + expected);
        helper.assertFalse(gate.setGlyph(7, 2), "a filled slot accepted a second glyph");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void teleportsItemsBetweenGates(GameTestHelper helper) {
        int[] glyphs = {3, 3, 3, 3, 3, 3, 3, 2};
        gate(helper, new BlockPos(2, 1, 3), glyphs);
        BlockPos far = new BlockPos(15, 1, 3);
        gate(helper, far, glyphs);
        helper.spawnItem(Items.NETHER_STAR, 2.5f, 2.2f, 3.5f);
        helper.succeedWhen(() -> {
            AABB around = new AABB(helper.absolutePos(far)).inflate(1.5, 3, 1.5);
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(Items.NETHER_STAR)).isEmpty(),
                    "item did not arrive at the other gate");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void incompleteGateDoesNothing(GameTestHelper helper) {
        int[] glyphs = {2, 2, 2, 2, 2, 2, 2};
        gate(helper, new BlockPos(2, 1, 3), glyphs);
        gate(helper, new BlockPos(15, 1, 3), glyphs);
        helper.spawnItem(Items.NETHER_STAR, 2.5f, 2.2f, 3.5f);
        helper.runAfterDelay(40, () -> {
            helper.assertEntityPresent(EntityType.ITEM, new BlockPos(2, 1, 3).above());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void gatesShareInventoryAndTank(GameTestHelper helper) {
        int[] glyphs = {0, 1, 0, 1, 0, 1, 0, 1};
        BlockPos a = new BlockPos(2, 1, 3);
        BlockPos b = new BlockPos(12, 1, 3);
        gate(helper, a, glyphs);
        gate(helper, b, glyphs);
        IItemHandler itemsA = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(a), Direction.UP);
        IItemHandler itemsB = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(b), Direction.DOWN);
        helper.assertTrue(itemsA != null && itemsB != null, "complete gates expose no inventory");
        helper.assertTrue(itemsA.insertItem(0, new ItemStack(Items.EMERALD, 10), false).isEmpty(), "gate A did not take emeralds");
        helper.assertTrue(itemsB.extractItem(0, 64, false).getCount() == 10, "gate B does not see gate A's emeralds");

        IFluidHandler tankA = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(a), Direction.NORTH);
        IFluidHandler tankB = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(b), Direction.SOUTH);
        helper.assertTrue(tankA.fill(new FluidStack(Fluids.WATER, 1500), IFluidHandler.FluidAction.EXECUTE) == 1000, "tank should hold one bucket");
        helper.assertTrue(tankB.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount() == 1000, "gate B does not see gate A's water");
        helper.succeed();
    }
}
