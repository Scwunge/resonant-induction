package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.registry.RIRegistries;
import resonantinduction.tesla.TeslaBlock;
import resonantinduction.tesla.TeslaBlockEntity;
import resonantinduction.tesla.TeslaPart;

/**
 * Tesla behaviour. Every test has its own batch: batches run one after another, so towers in neighbouring test areas
 * (within beam range of each other) cannot interfere.
 */
@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class TeslaGameTests {
    static final String TEMPLATE = "empty20x8x7";
    static final BlockPos A = new BlockPos(1, 1, 3);

    static BlockPos tower(GameTestHelper helper, BlockPos base, int height) {
        for (int i = 0; i < height; i++) {
            helper.setBlock(base.above(i), RIRegistries.TESLA.get());
        }
        return base;
    }

    static TeslaBlockEntity coil(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos);
    }

    static void charge(GameTestHelper helper, BlockPos pos, int amount) {
        IEnergyStorage storage = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), Direction.NORTH);
        helper.assertTrue(storage != null, "bottom coil exposes no energy storage");
        helper.assertTrue(storage.receiveEnergy(amount, false) == amount, "bottom coil did not take " + amount + " FE");
    }

    /** Waits, then checks the tower at {@code pos} received nothing. */
    static void assertNothingAfter(GameTestHelper helper, BlockPos pos, int ticks) {
        helper.runAfterDelay(ticks, () -> {
            helper.assertTrue(coil(helper, pos).getReceived() == 0, "tower at " + pos + " received " + coil(helper, pos).getReceived());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "tesla_parts")
    public static void towerParts(GameTestHelper helper) {
        tower(helper, A, 3);
        helper.assertBlockProperty(A, TeslaBlock.PART, TeslaPart.BOTTOM);
        helper.assertBlockProperty(A.above(), TeslaBlock.PART, TeslaPart.MIDDLE);
        helper.assertBlockProperty(A.above(2), TeslaBlock.PART, TeslaPart.TOP);
        TeslaBlockEntity bottom = coil(helper, A);
        helper.assertTrue(bottom.height() == 3 && bottom.range() == 8, "3-coil tower should have range 8, got " + bottom.range());
        helper.assertTrue(coil(helper, A.above(2)).primary() == bottom, "top coil does not resolve to the bottom coil");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(A.above(2)), Direction.NORTH) == null,
                "upper coils must not expose energy");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(A), Direction.UP) == null,
                "bottom coil must not take energy from the top");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "tesla_in_range")
    public static void sendsInRange(GameTestHelper helper) {
        tower(helper, A, 3);
        BlockPos b = tower(helper, A.east(7), 2);
        charge(helper, A, 10000);
        helper.succeedWhen(() -> helper.assertTrue(coil(helper, b).getReceived() > 0, "tower 7 blocks away received nothing"));
    }

    @GameTest(template = TEMPLATE, batch = "tesla_out_of_range")
    public static void ignoresOutOfRange(GameTestHelper helper) {
        tower(helper, A, 3);
        BlockPos b = tower(helper, A.east(9), 2);
        charge(helper, A, 10000);
        assertNothingAfter(helper, b, 40);
    }

    @GameTest(template = TEMPLATE, batch = "tesla_single_coil")
    public static void singleCoilsNeitherSendNorReceive(GameTestHelper helper) {
        tower(helper, A, 3);
        BlockPos lone = tower(helper, A.east(4), 1);
        BlockPos loneSender = tower(helper, A.east(10), 1);
        BlockPos receiver = tower(helper, A.east(12), 2);
        charge(helper, A, 10000);
        charge(helper, loneSender, 10000);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(coil(helper, lone).getReceived() == 0, "a lone coil received a beam");
            helper.assertTrue(coil(helper, receiver).getReceived() == 0, "a lone coil sent a beam");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "tesla_colour_mismatch")
    public static void differentColoursDoNotTalk(GameTestHelper helper) {
        tower(helper, A, 3);
        BlockPos b = tower(helper, A.east(5), 2);
        coil(helper, A).setColor(DyeColor.RED);
        coil(helper, b).setColor(DyeColor.BLUE);
        charge(helper, A, 10000);
        assertNothingAfter(helper, b, 40);
    }

    @GameTest(template = TEMPLATE, batch = "tesla_colour_default")
    public static void defaultColourTalksToAll(GameTestHelper helper) {
        tower(helper, A, 3);
        BlockPos b = tower(helper, A.east(5), 2);
        coil(helper, A).setColor(DyeColor.RED);
        charge(helper, A, 10000);
        helper.succeedWhen(() -> helper.assertTrue(coil(helper, b).getReceived() > 0, "default-coloured tower received nothing from a red one"));
    }

    @GameTest(template = TEMPLATE, batch = "tesla_receive_off")
    public static void receiveToggle(GameTestHelper helper) {
        tower(helper, A, 3);
        BlockPos b = tower(helper, A.east(5), 2);
        coil(helper, b).toggleReceive();
        charge(helper, A, 10000);
        assertNothingAfter(helper, b, 40);
    }

    @GameTest(template = TEMPLATE, batch = "tesla_redstone")
    public static void redstoneStopsSending(GameTestHelper helper) {
        tower(helper, A, 3);
        helper.setBlock(A.west(), Blocks.REDSTONE_BLOCK);
        BlockPos b = tower(helper, A.east(5), 2);
        charge(helper, A, 10000);
        assertNothingAfter(helper, b, 40);
    }

    @GameTest(template = TEMPLATE, batch = "tesla_conservation")
    public static void energyIsConserved(GameTestHelper helper) {
        tower(helper, A, 3);
        BlockPos b = tower(helper, A.east(4), 2);
        BlockPos c = tower(helper, A.east(6), 3);
        charge(helper, A, 10000);
        helper.runAfterDelay(40, () -> {
            TeslaBlockEntity ta = coil(helper, A);
            TeslaBlockEntity tb = coil(helper, b);
            TeslaBlockEntity tc = coil(helper, c);
            // c also beams back what it was charged with (nothing), so the total must stay exactly 10000.
            long total = (long) ta.getCharge() + ta.getReceived() + tb.getCharge() + tb.getReceived() + tc.getCharge() + tc.getReceived();
            helper.assertTrue(total == 10000, "energy changed: " + total);
            helper.assertTrue(tb.getReceived() > 0 && tc.getReceived() > 0, "both towers should get a share");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "tesla_link")
    public static void linkedTowersSendOnlyToEachOther(GameTestHelper helper) {
        tower(helper, A, 1);
        BlockPos far = tower(helper, A.east(17), 1);
        BlockPos near = tower(helper, A.east(3), 2);
        tower(helper, A.above(), 2); // A becomes a 3-coil tower, near is in range
        TeslaBlockEntity a = coil(helper, A);
        helper.assertTrue(a.linkTo(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(far))), "link failed");
        helper.assertTrue(coil(helper, far).getLink() != null, "link is not two-way");
        charge(helper, A, 10000);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(coil(helper, far).getReceived() > 0, "linked tower received nothing");
            helper.assertTrue(coil(helper, near).getReceived() == 0, "a linked tower still beamed to a nearby one");
            coil(helper, far).unlink();
            helper.assertTrue(coil(helper, A).getLink() == null, "unlink did not clear the partner");
            helper.succeed();
        });
    }
}
