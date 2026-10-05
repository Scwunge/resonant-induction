package resonantinduction.quantum;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import resonantinduction.registry.RIRegistries;

import java.util.List;

/**
 * One of the four glyphs. Each fills one eighth (a corner) of a block; eight in one block make a Quantum Gate.
 * The slot is picked from where you click, like the original corner placement.
 */
public class QuantumGlyphItem extends Item {
    private final int glyph;

    public QuantumGlyphItem(int glyph, Properties properties) {
        super(properties);
        this.glyph = glyph;
    }

    public int glyph() {
        return glyph;
    }

    public static Item byGlyph(int glyph) {
        return RIRegistries.GLYPHS.get(Math.max(0, Math.min(glyph, RIRegistries.GLYPHS.size() - 1))).get();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        Direction face = context.getClickedFace();
        Vec3 hit = context.getClickLocation();
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());

        // First choice: the quarter-block just outside the clicked face (next corner of the same gate, or a new gate).
        Vec3 outside = hit.add(normal.scale(0.25));
        BlockPos target = BlockPos.containing(outside);
        if (tryPlace(context, target, outside)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // Otherwise fill the empty corner just inside a clicked gate.
        Vec3 inside = hit.subtract(normal.scale(0.25));
        BlockPos clicked = BlockPos.containing(inside);
        if (level.getBlockEntity(clicked) instanceof QuantumGateBlockEntity && tryPlace(context, clicked, inside)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.FAIL;
    }

    private boolean tryPlace(UseOnContext context, BlockPos pos, Vec3 point) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), stack)) {
            return false;
        }
        int slot = QuantumGateBlockEntity.slotAt(point.x - pos.getX(), point.y - pos.getY(), point.z - pos.getZ());
        if (level.getBlockEntity(pos) instanceof QuantumGateBlockEntity gate) {
            if (gate.glyph(slot) >= 0) {
                return false;
            }
            if (!level.isClientSide) {
                gate.setGlyph(slot, glyph);
            }
        } else {
            BlockPlaceContext placeContext = new BlockPlaceContext(context);
            if (!level.getBlockState(pos).canBeReplaced(placeContext) || !level.isUnobstructed(RIRegistries.QUANTUM_GATE.get().defaultBlockState(), pos, net.minecraft.world.phys.shapes.CollisionContext.empty())) {
                return false;
            }
            if (!level.isClientSide) {
                level.setBlock(pos, RIRegistries.QUANTUM_GATE.get().defaultBlockState(), 3);
                if (level.getBlockEntity(pos) instanceof QuantumGateBlockEntity gate) {
                    gate.setGlyph(slot, glyph);
                }
            }
        }
        if (!level.isClientSide) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 1f, 0.8f + 0.1f * glyph);
            stack.consume(1, player);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.resonantinduction.glyph"));
    }
}
