package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import resonantinduction.ResonantInduction;
import resonantinduction.archaic.CrateBlockEntity;
import resonantinduction.archaic.CrateContents;
import resonantinduction.archaic.EngineeringTableBlockEntity;
import resonantinduction.archaic.ImprintItem;
import resonantinduction.archaic.ImprinterBlockEntity;
import resonantinduction.archaic.TurntableBlock;
import resonantinduction.registry.RIRegistries;

@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class WorkshopGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    static CrateBlockEntity crate(GameTestHelper helper, BlockPos pos, int tier) {
        helper.setBlock(pos, RIRegistries.CRATES.get(tier).get());
        return helper.getBlockEntity(pos);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void crateHoldsThirtyTwoStacksOfOneItem(GameTestHelper helper) {
        CrateBlockEntity crate = crate(helper, new BlockPos(3, 1, 3), 0);
        ItemStack left = crate.add(new ItemStack(Items.COBBLESTONE, 3000), false);
        helper.assertTrue(crate.count() == 2048 && left.getCount() == 952, "holds " + crate.count() + ", left " + left.getCount());
        helper.assertTrue(crate.add(new ItemStack(Items.DIRT), false).getCount() == 1, "took a second kind of item");
        helper.assertTrue(crate.take(64, false).getCount() == 64 && crate.count() == 1984, "took out wrong");
        CrateBlockEntity steel = crate(helper, new BlockPos(5, 1, 3), 2);
        helper.assertTrue(steel.capacity() == 256 * 64, "steel crate holds " + steel.capacity());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void crateFilterAndOreFilter(GameTestHelper helper) {
        CrateBlockEntity crate = crate(helper, new BlockPos(3, 1, 3), 0);
        crate.setFilter(new ItemStack(Items.CHEST));
        helper.assertTrue(crate.add(new ItemStack(Items.DIRT), false).getCount() == 1, "a locked crate took dirt");
        helper.assertTrue(crate.add(new ItemStack(Items.CHEST, 2), false).isEmpty(), "a locked crate refused its own item");
        helper.assertTrue(crate.add(new ItemStack(Items.TRAPPED_CHEST), false).getCount() == 1, "took a trapped chest without the ore filter");
        crate.toggleOreFilter();
        // Chests and trapped chests share c:chests/wooden; with the ore filter on, a trapped chest goes in as a chest.
        helper.assertTrue(crate.add(new ItemStack(Items.TRAPPED_CHEST), false).isEmpty(), "ore filter refused a trapped chest");
        helper.assertTrue(crate.count() == 3 && crate.item().is(Items.CHEST), "holds " + crate.count() + " " + crate.item());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void hoppersFillAndEmptyCrates(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        CrateBlockEntity crate = crate(helper, pos, 0);
        IItemHandler h = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(h.insertItem(1, new ItemStack(Items.IRON_INGOT, 64), false).isEmpty(), "hopper slot refused ingots");
        helper.assertTrue(h.insertItem(1, new ItemStack(Items.IRON_INGOT, 64), false).isEmpty(), "second stack refused");
        helper.assertTrue(h.extractItem(0, 64, false).getCount() == 64 && crate.count() == 64, "extract failed, count " + crate.count());
        // A real hopper under it pulls the rest out.
        helper.setBlock(pos.below(), Blocks.HOPPER);
        helper.succeedWhen(() -> helper.assertTrue(crate.count() < 64, "hopper pulled nothing"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void brokenCrateKeepsItsContents(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        crate(helper, pos, 1).add(new ItemStack(Items.REDSTONE, 1000), false);
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
        helper.succeedWhen(() -> {
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(pos)).inflate(2));
            helper.assertTrue(drops.stream().anyMatch(e -> {
                CrateContents c = e.getItem().get(RIRegistries.CRATE_CONTENTS.get());
                return e.getItem().is(RIRegistries.CRATE_ITEMS.get(1).get()) && c != null && c.count() == 1000 && c.item().is(Items.REDSTONE);
            }), "no iron crate item holding 1000 redstone");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void engineeringTableCraftsFromItsGrid(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.ENGINEERING_TABLE.get());
        EngineeringTableBlockEntity table = helper.getBlockEntity(pos);
        table.setSlot(1, new ItemStack(Items.OAK_PLANKS, 2));
        table.setSlot(4, new ItemStack(Items.OAK_PLANKS, 2));
        helper.assertTrue(table.output().is(Items.STICK) && table.output().getCount() == 4, "shows " + table.output());
        ItemStack made = table.craft(null);
        helper.assertTrue(made.is(Items.STICK) && made.getCount() == 4, "made " + made);
        helper.assertTrue(table.grid().get(1).getCount() == 1 && table.grid().get(4).getCount() == 1, "grid not used");
        helper.succeed();
    }

    /** The grid is a pattern: with planks in a chest beside the table, crafting takes those and leaves the grid alone. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void engineeringTableTakesFromNeighbouringChests(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.ENGINEERING_TABLE.get());
        helper.setBlock(pos.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(pos.east());
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
        EngineeringTableBlockEntity table = helper.getBlockEntity(pos);
        table.setSlot(1, new ItemStack(Items.OAK_PLANKS));
        table.setSlot(4, new ItemStack(Items.OAK_PLANKS));
        helper.assertTrue(table.craft(null).is(Items.STICK), "no sticks");
        helper.assertTrue(chest.getItem(0).isEmpty(), "chest still holds " + chest.getItem(0));
        helper.assertTrue(table.grid().get(1).getCount() == 1 && table.grid().get(4).getCount() == 1, "grid was used");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void engineeringTableCraftsWhatsOnAnImprint(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.ENGINEERING_TABLE.get());
        helper.setBlock(pos.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(pos.east());
        chest.setItem(0, new ItemStack(Items.COBBLESTONE, 8));
        ItemStack imprint = new ItemStack(RIRegistries.IMPRINT.get());
        ImprintItem.setFilters(imprint, java.util.List.of(new ItemStack(Items.FURNACE)));
        EngineeringTableBlockEntity table = helper.getBlockEntity(pos);
        table.setSlot(EngineeringTableBlockEntity.CENTER, imprint);
        ItemStack made = table.craft(null);
        helper.assertTrue(made.is(Items.FURNACE), "made " + made);
        helper.assertTrue(chest.getItem(0).isEmpty(), "cobblestone left: " + chest.getItem(0));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void hammerCrushesOreOnTheTable(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.ENGINEERING_TABLE.get());
        EngineeringTableBlockEntity table = helper.getBlockEntity(pos);
        table.setSlot(0, new ItemStack(Items.IRON_ORE, 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(RIRegistries.HAMMER.get()));
        for (int i = 0; i < 60 && table.grid().get(0).getCount() == 2; i++) {
            helper.useBlock(pos, player);
        }
        helper.assertTrue(table.grid().get(0).getCount() == 1, "60 blows crushed nothing");
        helper.assertTrue(player.getInventory().contains(s -> s.is(RIRegistries.RUBBLE.get())), "no rubble");
        helper.assertTrue(player.getMainHandItem().getDamageValue() > 0, "hammer not worn");
        helper.succeed();
    }

    /** A piston coming down on the imprinter stamps the items on its top into the imprint; stamping again takes them off. */
    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void pistonStampsTheImprinter(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.IMPRINTER.get());
        ImprinterBlockEntity imprinter = helper.getBlockEntity(pos);
        imprinter.inventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        imprinter.inventory().setStackInSlot(4, new ItemStack(Items.DIRT, 5));
        imprinter.inventory().setStackInSlot(ImprinterBlockEntity.IMPRINT_SLOT, new ItemStack(RIRegistries.IMPRINT.get()));
        helper.setBlock(pos.above(2), Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.DOWN));
        helper.setBlock(pos.above(3), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            ItemStack imprint = imprinter.inventory().getStackInSlot(ImprinterBlockEntity.IMPRINT_SLOT);
            helper.assertTrue(ImprintItem.isFiltering(imprint, new ItemStack(Items.COBBLESTONE)) && ImprintItem.isFiltering(imprint, new ItemStack(Items.DIRT))
                    && ImprintItem.filters(imprint).size() == 2, "imprint lists " + ImprintItem.filters(imprint));
            // A second stamp toggles them off again.
            imprinter.stamp();
            helper.assertTrue(ImprintItem.filters(imprinter.inventory().getStackInSlot(ImprinterBlockEntity.IMPRINT_SLOT)).isEmpty(), "second stamp did not clear");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void poweredTurntableTurnsTheBlockOnIt(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, RIRegistries.TURNTABLE.get().defaultBlockState().setValue(TurntableBlock.FACING, Direction.UP));
        helper.setBlock(pos.above(), Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH));
        helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(pos.above()).getValue(FurnaceBlock.FACING) != Direction.NORTH, "furnace not turned"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void sidewaysTurntableTurnsALog(GameTestHelper helper) {
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        helper.assertTrue(TurntableBlock.turn(log, helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), Direction.EAST)
                .getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS) == Direction.Axis.Z, "upright log not laid along z");
        helper.succeed();
    }
}
