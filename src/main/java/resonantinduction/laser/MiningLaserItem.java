package resonantinduction.laser;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.level.BlockEvent;
import resonantinduction.RIConfig;
import resonantinduction.network.RINetwork;
import resonantinduction.registry.RIRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mining Laser: hold use to fire a beam (range 50). Sneak-use switches mode:
 * <ul>
 * <li>Remove: a block held in the beam for 15 ticks is cut out and drops (wood, plants and wool catch fire instead; TNT is lit).</li>
 * <li>Smelt: heats the block: sand turns to glass, cobblestone to stone, ice to water, obsidian to lava, grass and flammable blocks burn.</li>
 * <li>Damage: only hurts what it hits.</li>
 * </ul>
 * Any mode sets living things on fire. Runs on FE; block changes go through the break event so claims are respected.
 */
public class MiningLaserItem extends Item {
    public static final int MODE_REMOVE = 0;
    public static final int MODE_SMELT = 1;
    public static final int MODE_DAMAGE = 2;
    private static final String[] MODE_KEYS = {"laser.resonantinduction.mode.remove", "laser.resonantinduction.mode.smelt", "laser.resonantinduction.mode.damage"};
    private static final int BEAM_COLOR = 0xFFFF2020;

    /** Block each player is focusing on, and for how many ticks. */
    private static final Map<UUID, Focus> FOCUS = new HashMap<>();

    private record Focus(BlockPos pos, int ticks) {}

    public MiningLaserItem(Properties properties) {
        super(properties);
    }

    public static int capacity() {
        return RIConfig.get(RIConfig.LASER_CAPACITY);
    }

    public static int mode(ItemStack stack) {
        return stack.getOrDefault(RIRegistries.LASER_MODE.get(), MODE_REMOVE);
    }

    private static int cost(int mode) {
        return switch (mode) {
            case MODE_REMOVE -> RIConfig.get(RIConfig.LASER_COST_REMOVE);
            case MODE_SMELT -> RIConfig.get(RIConfig.LASER_COST_REMOVE) / 2;
            default -> RIConfig.get(RIConfig.LASER_COST_REMOVE) / 3;
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            int mode = (mode(stack) + 1) % MODE_KEYS.length;
            stack.set(RIRegistries.LASER_MODE.get(), mode);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("laser.resonantinduction.mode_set", Component.translatable(MODE_KEYS[mode])), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (player.getAbilities().instabuild || (energy != null && energy.getEnergyStored() >= cost(mode(stack)))) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        int held = getUseDuration(stack, entity) - remaining;
        if (held <= 5 || !(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel server)) {
            return;
        }
        int mode = mode(stack);
        if (!player.getAbilities().instabuild) {
            IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            int cost = cost(mode);
            if (energy == null || energy.extractEnergy(cost, true) < cost) {
                player.stopUsingItem();
                clearFocus(server, player);
                return;
            }
            energy.extractEnergy(cost, false);
        }

        int range = RIConfig.get(RIConfig.LASER_RANGE);
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 beamEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, eye, beamEnd,
                new AABB(eye, beamEnd).inflate(1), e -> !e.isSpectator() && e.isPickable());

        if (entityHit != null) {
            Entity target = entityHit.getEntity();
            beamEnd = entityHit.getLocation();
            target.hurt(level.damageSources().playerAttack(player), (float) RIConfig.get(RIConfig.LASER_DAMAGE));
            target.igniteForSeconds(5);
            clearFocus(server, player);
        } else if (blockHit.getType() == HitResult.Type.BLOCK) {
            focusBlock(server, player, blockHit.getBlockPos(), blockHit.getDirection(), mode);
        } else {
            clearFocus(server, player);
        }

        // Two beams from just below and right of the eye, as the original drew them.
        float yaw = player.getYHeadRot() * Mth.DEG_TO_RAD;
        double ox = Mth.cos(yaw) * -0.4 - Mth.sin(yaw) * -0.1;
        double oz = Mth.sin(yaw) * -0.4 + Mth.cos(yaw) * -0.1;
        Vec3 muzzle = eye.add(player.getLookAngle()).add(ox, -0.25, oz);
        RINetwork.sendBeam(server, muzzle, beamEnd, BEAM_COLOR);
        RINetwork.sendBeam(server, muzzle.add(0, -0.2, 0), beamEnd, BEAM_COLOR);
    }

    private void focusBlock(ServerLevel level, ServerPlayer player, BlockPos pos, Direction face, int mode) {
        Focus last = FOCUS.get(player.getUUID());
        BlockState state = level.getBlockState(pos);
        if (state.getDestroySpeed(level, pos) < 0) {
            clearFocus(level, player);
            return;
        }
        int ticks = last != null && last.pos.equals(pos) ? last.ticks + 1 : 1;
        if (last != null && !last.pos.equals(pos)) {
            level.destroyBlockProgress(player.getId(), last.pos, -1);
        }
        int breakTicks = RIConfig.get(RIConfig.LASER_BREAK_TICKS);
        if (mode == MODE_REMOVE && ticks >= breakTicks) {
            level.destroyBlockProgress(player.getId(), pos, -1);
            FOCUS.remove(player.getUUID());
            mine(level, player, pos, state);
            return;
        }
        if (mode == MODE_SMELT) {
            heat(level, player, pos, face, state);
        }
        if (mode != MODE_DAMAGE) {
            level.destroyBlockProgress(player.getId(), pos, Math.min(9, ticks * 10 / breakTicks));
        }
        FOCUS.put(player.getUUID(), new Focus(pos, ticks));
    }

    private static void clearFocus(ServerLevel level, Player player) {
        Focus last = FOCUS.remove(player.getUUID());
        if (last != null) {
            level.destroyBlockProgress(player.getId(), last.pos, -1);
        }
    }

    /** Lets claim and protection mods veto the change. */
    private static boolean allowed(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        BlockEvent.BreakEvent event = CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state);
        return !event.isCanceled();
    }

    private static void mine(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        if (!allowed(level, player, pos, state)) {
            return;
        }
        if (state.getBlock() instanceof TntBlock) {
            level.removeBlock(pos, false);
            PrimedTnt tnt = new PrimedTnt(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player);
            tnt.setFuse(level.random.nextInt(20) + 10);
            level.addFreshEntity(tnt);
            return;
        }
        // Wood, plants, wool and webs burn rather than being cut out.
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.WOOL)
                || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.CROPS) || state.is(Blocks.COBWEB)
                || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.VINE) || state.is(Blocks.PUMPKIN)) {
            BlockPos below = pos.below();
            if (level.getBlockState(below).getBlock() instanceof FarmBlock) {
                level.setBlockAndUpdate(below, Blocks.DIRT.defaultBlockState());
            }
            level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
            return;
        }
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, new ItemStack(Items.NETHERITE_PICKAXE));
        if (drops.isEmpty() && (state.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_BLOCKS) || state.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_PANES))) {
            drops = List.of(new ItemStack(state.getBlock()));
        }
        level.removeBlock(pos, false);
        level.levelEvent(2001, pos, Block.getId(state));
        for (ItemStack drop : drops) {
            Block.popResource(level, pos, drop);
        }
    }

    private static void heat(ServerLevel level, ServerPlayer player, BlockPos pos, Direction face, BlockState state) {
        BlockState result = null;
        if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) {
            result = Blocks.GLASS.defaultBlockState();
        } else if (state.is(Blocks.COBBLESTONE)) {
            result = Blocks.STONE.defaultBlockState();
        } else if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.SNOW_BLOCK)) {
            result = Blocks.WATER.defaultBlockState();
        } else if (state.is(Blocks.OBSIDIAN)) {
            result = Blocks.LAVA.defaultBlockState();
        } else if (state.is(Blocks.GRASS_BLOCK) && level.getBlockState(pos.above()).isAir()) {
            if (allowed(level, player, pos, state)) {
                level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                level.setBlockAndUpdate(pos.above(), BaseFireBlock.getState(level, pos.above()));
            }
            return;
        } else if (state.getFlammability(level, pos, face) / 300f >= level.random.nextFloat()) {
            result = BaseFireBlock.getState(level, pos);
        }
        if (result != null && allowed(level, player, pos, state)) {
            level.setBlockAndUpdate(pos, result);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level instanceof ServerLevel server && entity instanceof Player player) {
            clearFocus(server, player);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!selected && level instanceof ServerLevel server && entity instanceof Player player && FOCUS.containsKey(player.getUUID())
                && !player.isUsingItem()) {
            clearFocus(server, player);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return energy == null || energy.getMaxEnergyStored() == 0 ? 0 : Math.round(13f * energy.getEnergyStored() / energy.getMaxEnergyStored());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF3030;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !oldStack.is(newStack.getItem());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energy != null) {
            tooltip.add(Component.translatable("tooltip.resonantinduction.energy", energy.getEnergyStored(), energy.getMaxEnergyStored()).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("laser.resonantinduction.mode_set", Component.translatable(MODE_KEYS[mode(stack)])).withStyle(ChatFormatting.GRAY));
    }
}
