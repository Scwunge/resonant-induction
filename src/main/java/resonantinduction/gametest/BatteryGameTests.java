package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.battery.BatteryBlock;
import resonantinduction.battery.BatteryBlockEntity;
import resonantinduction.registry.RIRegistries;
import resonantinduction.tesla.TeslaBlockEntity;

import java.util.List;

@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class BatteryGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    static BatteryBlockEntity battery(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, RIRegistries.BATTERY.get());
        return helper.getBlockEntity(pos);
    }

    @GameTest(template = TEMPLATE)
    public static void touchingBatteriesShareEnergy(GameTestHelper helper) {
        BlockPos a = new BlockPos(2, 1, 3);
        battery(helper, a);
        battery(helper, a.east());
        battery(helper, a.east(2));
        IEnergyStorage in = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(a), Direction.WEST);
        helper.assertTrue(in != null && in.receiveEnergy(900_000, false) == 900_000, "battery did not take 900k FE");
        helper.succeedWhen(() -> {
            for (int i = 0; i < 3; i++) {
                int e = ((BatteryBlockEntity) helper.getBlockEntity(a.east(i))).energy();
                helper.assertTrue(e == 300_000, "cell " + i + " holds " + e + ", expected an even 300000");
            }
            helper.assertBlockProperty(a.east(1), BatteryBlock.CONNECTED.get(Direction.WEST), true);
            helper.assertBlockProperty(a, BatteryBlock.LEVEL, 2);
        });
    }

    @GameTest(template = TEMPLATE)
    public static void outputFacePushesEnergy(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 3);
        BatteryBlockEntity battery = battery(helper, pos);
        battery.setEnergy(50_000);
        helper.setBlock(pos.east(), RIRegistries.TESLA.get());
        TeslaBlockEntity sink = helper.getBlockEntity(pos.east());
        helper.assertTrue(battery.cycleIo(Direction.EAST) == BatteryBlockEntity.IO_OUTPUT, "east face should now be an output");
        helper.succeedWhen(() -> helper.assertTrue(sink.getCharge() == 10_000, "output face pushed " + sink.getCharge() + " FE"));
    }

    @GameTest(template = TEMPLATE)
    public static void ioModesGateTheFaces(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 3);
        BatteryBlockEntity battery = battery(helper, pos);
        battery.cycleIo(Direction.NORTH);
        battery.cycleIo(Direction.NORTH);
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), Direction.NORTH) == null,
                "a face set to off still exposes energy");
        battery.cycleIo(Direction.SOUTH);
        IEnergyStorage out = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), Direction.SOUTH);
        helper.assertTrue(out != null && !out.canReceive() && out.canExtract(), "an output face should only extract");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void brokenBatteryKeepsItsCharge(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 3);
        helper.setBlock(pos, RIRegistries.BATTERY.get().defaultBlockState().setValue(BatteryBlock.TIER, 1));
        BatteryBlockEntity battery = helper.getBlockEntity(pos);
        battery.setEnergy(12_345);
        List<ItemStack> drops = Block.getDrops(battery.getBlockState(), helper.getLevel(), helper.absolutePos(pos), battery);
        helper.assertTrue(drops.size() == 1, "expected one drop");
        ItemStack drop = drops.get(0);
        helper.assertTrue(drop.getOrDefault(RIRegistries.BATTERY_TIER.get(), -1) == 1, "drop lost its tier");
        helper.assertTrue(drop.getOrDefault(RIRegistries.ENERGY.get(), 0) == 12_345, "drop lost its charge");
        IEnergyStorage item = drop.getCapability(Capabilities.EnergyStorage.ITEM);
        helper.assertTrue(item != null && item.getEnergyStored() == 12_345, "battery item exposes no energy");
        helper.succeed();
    }
}
