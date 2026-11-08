package noppes.npcs.client.renderer;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import noppes.npcs.client.NpcTextures;

public class NpcRenderState extends HumanoidRenderState {
    public Identifier texture = NpcTextures.DEFAULT;
}
