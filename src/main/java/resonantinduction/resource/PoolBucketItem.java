package resonantinduction.resource;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** A bucket of molten metal or dust mixture (the original per-metal buckets). Use it on a block to pour out a full pool. */
public class PoolBucketItem extends Item {
    private final PoolBlock.Kind kind;

    public PoolBucketItem(PoolBlock.Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public PoolBlock.Kind kind() {
        return kind;
    }

    @Override
    public Component getName(ItemStack stack) {
        String material = Materials.material(stack);
        return Component.translatable(getDescriptionId(), material == null ? Component.literal("?") : Materials.displayName(material));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        String material = Materials.material(context.getItemInHand());
        BlockPlaceContext place = new BlockPlaceContext(context);
        BlockPos pos = place.getClickedPos();
        if (material == null || !place.canPlace()) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            PoolBlock.place(level, pos, kind, material, 8);
            level.playSound(null, pos, kind == PoolBlock.Kind.MOLTEN ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1f, 1f);
            if (player == null || !player.getAbilities().instabuild) {
                if (player != null) {
                    player.setItemInHand(context.getHand(), new ItemStack(Items.BUCKET));
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
