package resonantinduction.item;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/** A block entity the Quantum Entangler can pair with another of the same kind. */
public interface Linkable {
    /** The block entity that actually stores the link (e.g. the bottom coil of a Tesla tower). */
    Linkable linkOwner();

    /** Where the Entangler should remember this device. */
    default GlobalPos linkKey() {
        BlockEntity be = (BlockEntity) linkOwner();
        return GlobalPos.of(be.getLevel().dimension(), be.getBlockPos());
    }

    /** Links both ways with the device at {@code target}. False if it is missing, of another kind, or out of reach. */
    boolean linkTo(GlobalPos target);

    void unlink();
}
