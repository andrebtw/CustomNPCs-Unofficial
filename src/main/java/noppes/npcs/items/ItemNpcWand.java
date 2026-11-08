package noppes.npcs.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import noppes.npcs.entity.NpcSpawner;

/**
 * NPC Wand: right-click a block to place an NPC on it. Game masters only. Opening the NPC edit
 * GUI and right-clicking existing NPCs (1.12.2 behaviour) arrive with networking/GUIs.
 */
public class ItemNpcWand extends Item {

    public ItemNpcWand(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }
        if (!(ctx.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            player.displayClientMessage(
                    Component.translatable("availability.permission").withStyle(ChatFormatting.RED), false);
            return InteractionResult.FAIL;
        }
        NpcSpawner.spawnAt(level, ctx.getClickedPos(), player.getYRot());
        return InteractionResult.SUCCESS;
    }
}
