package resonantinduction.archaic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** What a crate item holds: one kind of item (kept as a single) and how many, which can be far more than a stack. */
public record CrateContents(ItemStack item, int count) {
    public static final Codec<CrateContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.SINGLE_ITEM_CODEC.fieldOf("item").forGetter(CrateContents::item),
            Codec.INT.fieldOf("count").forGetter(CrateContents::count)).apply(i, CrateContents::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CrateContents> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, CrateContents::item, ByteBufCodecs.VAR_INT, CrateContents::count, CrateContents::new);

    @Override
    public boolean equals(Object o) {
        return o instanceof CrateContents c && c.count == count && ItemStack.isSameItemSameComponents(c.item, item);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(item) * 31 + count;
    }
}
