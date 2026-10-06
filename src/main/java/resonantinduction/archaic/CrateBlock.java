package resonantinduction.archaic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import resonantinduction.battery.BatteryBlock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Crate block, with the original's controls. Right-click puts the held stack in (double-click: every matching stack you carry),
 * spilling over into crates it touches when full. Left-click takes a stack out (sneaking: one; double-click: everything).
 * Sneak + right-click with an item locks the crate to it (empty hand unlocks). Wrench: switch to the next item with the same
 * material tag; wrench left-click: ore filter on or off; sneak + wrench: pick the crate up with what's in it. In creative,
 * right-clicking the top of an empty crate with an item fills it.
 */
public class CrateBlock extends BaseEntityBlock {
    private static final int DOUBLE_CLICK = 10;

    private final int tier;
    private final MapCodec<CrateBlock> codec;

    public CrateBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
        this.codec = simpleCodec(p -> new CrateBlock(tier, p));
    }

    public int tier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrateBlockEntity(pos, state);
    }

    private static boolean doubleClick(Level level, CrateBlockEntity crate) {
        boolean quick = level.getGameTime() - crate.prevClickTime < DOUBLE_CLICK;
        crate.prevClickTime = level.getGameTime();
        return quick;
    }

    /** This crate and every crate joined to it. */
    static List<CrateBlockEntity> connected(Level level, CrateBlockEntity start) {
        List<CrateBlockEntity> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(start.getBlockPos());
        seen.add(start.getBlockPos());
        while (!open.isEmpty() && out.size() < 1024) {
            BlockPos pos = open.poll();
            if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) {
                continue;
            }
            out.add(crate);
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (seen.add(next) && level.isLoaded(next) && level.getBlockEntity(next) instanceof CrateBlockEntity) {
                    open.add(next);
                }
            }
        }
        return out;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (stack.is(BatteryBlock.WRENCHES)) {
            if (player.isSecondaryUseActive()) {
                pickUp(level, pos, crate, player);
            } else {
                crate.cycleVariant();
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            crate.setFilter(stack.isDamageableItem() && !stack.isDamaged() ? ItemStack.EMPTY : stack);
            player.displayClientMessage(Component.translatable("message.resonantinduction.crate.filter", stack.getHoverName()), true);
            return ItemInteractionResult.SUCCESS;
        }
        if (player.isCreative() && hit.getDirection() == Direction.UP && crate.sample().isEmpty()) {
            crate.add(stack.copyWithCount(crate.capacity()), false);
            return ItemInteractionResult.SUCCESS;
        }
        boolean all = doubleClick(level, crate);
        if (stack.getItem() instanceof CrateItem) {
            insertCrateItem(crate, stack);
        } else {
            insert(level, crate, player, all);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (player.isSecondaryUseActive()) {
                crate.setFilter(ItemStack.EMPTY);
                player.displayClientMessage(Component.translatable("message.resonantinduction.crate.no_filter"), true);
            } else if (doubleClick(level, crate)) {
                insert(level, crate, player, true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The held stack (or, for {@code all}, every stack of the crate's item the player has), into this crate or ones joined to it. */
    private static void insert(Level level, CrateBlockEntity crate, Player player, boolean all) {
        List<CrateBlockEntity> crates = connected(level, crate);
        if (all) {
            ItemStack request = !crate.sample().isEmpty() ? crate.sample() : player.getMainHandItem();
            if (request.isEmpty() || request.getItem() instanceof CrateItem) {
                return;
            }
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, request)) {
                    inv.setItem(i, intoAny(crates, s));
                }
            }
            player.containerMenu.broadcastChanges();
        } else {
            player.setItemInHand(InteractionHand.MAIN_HAND, intoAny(crates, player.getMainHandItem()));
        }
    }

    private static ItemStack intoAny(List<CrateBlockEntity> crates, ItemStack stack) {
        for (CrateBlockEntity c : crates) {
            if (stack.isEmpty()) {
                break;
            }
            stack = c.add(stack, false);
        }
        return stack;
    }

    /** A filled crate item emptied into this one. */
    private static void insertCrateItem(CrateBlockEntity crate, ItemStack crateItem) {
        CrateContents contents = crateItem.get(resonantinduction.registry.RIRegistries.CRATE_CONTENTS.get());
        if (contents == null || contents.count() <= 0) {
            return;
        }
        ItemStack left = crate.add(contents.item().copyWithCount(contents.count()), false);
        if (left.isEmpty()) {
            crateItem.remove(resonantinduction.registry.RIRegistries.CRATE_CONTENTS.get());
        } else {
            crateItem.set(resonantinduction.registry.RIRegistries.CRATE_CONTENTS.get(), new CrateContents(contents.item(), left.getCount()));
        }
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) {
            return;
        }
        if (player.getMainHandItem().is(BatteryBlock.WRENCHES)) {
            crate.toggleOreFilter();
            player.displayClientMessage(Component.translatable(crate.oreFilter()
                    ? "message.resonantinduction.crate.ore_filter_on" : "message.resonantinduction.crate.ore_filter_off"), true);
            return;
        }
        boolean all = doubleClick(level, crate);
        if (crate.count() == 0) {
            return;
        }
        int amount = all && !player.isShiftKeyDown() ? crate.capacity() : player.isShiftKeyDown() ? 1 : crate.item().getMaxStackSize();
        while (amount > 0 && crate.count() > 0) {
            ItemStack out = crate.take(Math.min(amount, crate.item().getMaxStackSize()), false);
            amount -= out.getCount();
            ItemEntity drop = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), out);
            drop.setNoPickUpDelay();
            level.addFreshEntity(drop);
        }
    }

    private void pickUp(Level level, BlockPos pos, CrateBlockEntity crate, Player player) {
        if (crate.count() <= 0) {
            return;
        }
        ItemStack drop = new ItemStack(this);
        drop.applyComponents(crate.collectComponents());
        crate.setContents(ItemStack.EMPTY, 0);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        Block.popResource(level, pos, drop);
    }
}
