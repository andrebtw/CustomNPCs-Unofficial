package noppes.npcs.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import noppes.npcs.registry.NpcEntities;

/** Places a new NPC in the world. Used by the NPC Wand. */
public final class NpcSpawner {

    private NpcSpawner() {}

    /** Spawns a fresh NPC standing on {@code ground} (centred on the block above it), facing {@code yaw}. */
    public static EntityCustomNpc spawnAt(ServerLevel level, BlockPos ground, float yaw) {
        EntityCustomNpc npc = NpcEntities.CUSTOM_NPC.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (npc == null) {
            throw new IllegalStateException("customnpcs:customnpc is disabled in this world");
        }
        BlockPos pos = ground.above();
        npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setHealth(npc.getMaxHealth());
        level.addFreshEntity(npc);
        return npc;
    }
}
