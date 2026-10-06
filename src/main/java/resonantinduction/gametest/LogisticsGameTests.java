package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.archaic.ImprintItem;
import resonantinduction.logistic.BreakerBlock;
import resonantinduction.logistic.ConveyorBeltBlock;
import resonantinduction.logistic.DetectorBlock;
import resonantinduction.logistic.DetectorBlockEntity;
import resonantinduction.logistic.ManipulatorBlock;
import resonantinduction.logistic.ManipulatorBlockEntity;
import resonantinduction.logistic.PlacerBlock;
import resonantinduction.logistic.PlacerBlockEntity;
import resonantinduction.logistic.SorterBlockEntity;
import resonantinduction.registry.RIRegistries;

import java.util.List;

@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class LogisticsGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    static ItemEntity spawn(GameTestHelper helper, BlockPos pos, ItemStack stack, double y) {
        BlockPos abs = helper.absolutePos(pos);
        ItemEntity item = new ItemEntity(helper.getLevel(), abs.getX() + 0.5, abs.getY() + y, abs.getZ() + 0.5, stack);
        item.setDeltaMovement(0, 0, 0);
        helper.getLevel().addFreshEntity(item);
        return item;
    }

    static ItemStack imprint(net.minecraft.world.item.Item... items) {
        ItemStack imprint = new ItemStack(RIRegistries.IMPRINT.get());
        ImprintItem.setFilters(imprint, java.util.Arrays.stream(items).map(ItemStack::new).toList());
        return imprint;
    }

    static int count(ChestBlockEntity chest, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (chest.getItem(i).is(item)) {
                n += chest.getItem(i).getCount();
            }
        }
        return n;
    }

    /** Belts set things moving at 0.05 blocks a tick; friction takes some of that off, as it did in the original. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void beltCarriesItemsAlong(GameTestHelper helper) {
        for (int x = 1; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 1, 3), RIRegistries.CONVEYOR_BELT.get().defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.EAST));
        }
        ItemEntity item = spawn(helper, new BlockPos(1, 1, 3), new ItemStack(Items.IRON_INGOT), 0.4);
        double start = item.getX();
        helper.runAfterDelay(60, () -> {
            double moved = item.getX() - start;
            helper.assertTrue(moved > 1.2 && moved < 3.2, "item moved " + moved + " blocks in 3 s");
            helper.assertTrue(Math.abs(item.getZ() - helper.absolutePos(new BlockPos(1, 1, 3)).getZ() - 0.5) < 0.2, "item wandered off the middle");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void poweredBeltStops(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.CONVEYOR_BELT.get().defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.EAST));
        helper.setBlock(pos.north(), Blocks.REDSTONE_BLOCK);
        ItemEntity item = spawn(helper, pos, new ItemStack(Items.IRON_INGOT), 0.4);
        double start = item.getX();
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(Math.abs(item.getX() - start) < 0.2, "powered belt moved the item " + (item.getX() - start));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void manipulatorStoresItemsInTheChestAbove(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.MANIPULATOR.get().defaultBlockState().setValue(ManipulatorBlock.FACING, Direction.NORTH));
        helper.setBlock(pos.above(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(pos.above());
        spawn(helper, pos, new ItemStack(Items.COBBLESTONE, 5), 0.2);
        helper.succeedWhen(() -> helper.assertTrue(count(chest, Items.COBBLESTONE) == 5, "chest holds " + count(chest, Items.COBBLESTONE)));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void manipulatorWithImprintIgnoresOtherItems(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.MANIPULATOR.get().defaultBlockState().setValue(ManipulatorBlock.FACING, Direction.NORTH));
        helper.setBlock(pos.above(), Blocks.CHEST);
        ManipulatorBlockEntity m = helper.getBlockEntity(pos);
        m.setImprint(imprint(Items.COBBLESTONE));
        ChestBlockEntity chest = helper.getBlockEntity(pos.above());
        spawn(helper, pos, new ItemStack(Items.DIRT), 0.2);
        spawn(helper, pos, new ItemStack(Items.COBBLESTONE), 0.2);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(count(chest, Items.COBBLESTONE) == 1 && count(chest, Items.DIRT) == 0, "chest holds wrong items");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void outputManipulatorDropsItemsInFront(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.MANIPULATOR.get().defaultBlockState().setValue(ManipulatorBlock.FACING, Direction.EAST));
        helper.setBlock(pos.west(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(pos.west());
        chest.setItem(0, new ItemStack(Items.REDSTONE, 3));
        ManipulatorBlockEntity m = helper.getBlockEntity(pos);
        m.setMode(true, true);
        helper.succeedWhen(() -> {
            // In front of it (they drop to the floor of the test area).
            List<ItemEntity> out = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos.east())).expandTowards(0, -1.5, 0).inflate(0.3));
            helper.assertTrue(out.stream().mapToInt(e -> e.getItem().getCount()).sum() == 3, "items in front: " + out.size());
            helper.assertTrue(count(chest, Items.REDSTONE) == 0, "chest not emptied");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void detectorSignalsItemsInFront(GameTestHelper helper) {
        BlockPos plain = new BlockPos(1, 1, 3);
        BlockPos picky = new BlockPos(5, 1, 3);
        for (BlockPos p : new BlockPos[] {plain, picky}) {
            helper.setBlock(p, RIRegistries.DETECTOR.get().defaultBlockState().setValue(DetectorBlock.FACING, Direction.UP));
            helper.setBlock(p.above(2), Blocks.GLASS);
            spawn(helper, p.above(), new ItemStack(Items.DIRT), 0.1);
        }
        ((DetectorBlockEntity) helper.getBlockEntity(picky)).setImprint(imprint(Items.COBBLESTONE));
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(plain, DetectorBlock.POWERED, true);
            // What a block east of it reads: the detector's signal in the direction from that block toward it.
            helper.assertTrue(helper.getLevel().getSignal(helper.absolutePos(plain), Direction.WEST) == 15, "no signal beside the detector");
            helper.assertTrue(helper.getLevel().getSignal(helper.absolutePos(plain), Direction.DOWN) == 0, "signal out of the front");
            helper.assertBlockProperty(picky, DetectorBlock.POWERED, false);
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void sorterSendsItemsOutOfTheirFace(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, RIRegistries.SORTER.get());
        helper.setBlock(pos.east(), Blocks.CHEST);
        helper.setBlock(pos.west(), Blocks.CHEST);
        SorterBlockEntity sorter = helper.getBlockEntity(pos);
        sorter.imprints().setStackInSlot(Direction.EAST.get3DDataValue(), imprint(Items.COBBLESTONE));
        helper.assertBlockProperty(pos, net.minecraft.world.level.block.PipeBlock.EAST, true);
        var h = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
        for (int i = 0; i < 8; i++) {
            h.insertItem(0, new ItemStack(Items.COBBLESTONE), false);
        }
        ChestBlockEntity east = helper.getBlockEntity(pos.east());
        ChestBlockEntity west = helper.getBlockEntity(pos.west());
        helper.assertTrue(count(east, Items.COBBLESTONE) == 8 && count(west, Items.COBBLESTONE) == 0, "east " + count(east, Items.COBBLESTONE) + ", west " + count(west, Items.COBBLESTONE));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void breakerBreaksIntoTheChestBehind(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos.west(), Blocks.CHEST);
        helper.setBlock(pos.east(), Blocks.STONE);
        helper.setBlock(pos, RIRegistries.BREAKER.get().defaultBlockState().setValue(BreakerBlock.FACING, Direction.EAST));
        helper.setBlock(pos.above(), Blocks.REDSTONE_BLOCK);
        ChestBlockEntity chest = helper.getBlockEntity(pos.west());
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, pos.east());
            helper.assertTrue(count(chest, Items.COBBLESTONE) == 1, "chest holds " + count(chest, Items.COBBLESTONE) + " cobblestone");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void placerPlacesItsBlock(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.PLACER.get().defaultBlockState().setValue(PlacerBlock.FACING, Direction.EAST));
        PlacerBlockEntity placer = helper.getBlockEntity(pos);
        placer.inventory().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 2));
        helper.setBlock(pos.above(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.OAK_PLANKS, pos.east());
            helper.assertTrue(placer.inventory().getStackInSlot(0).getCount() == 1, "placer still holds " + placer.inventory().getStackInSlot(0));
        });
    }
}
