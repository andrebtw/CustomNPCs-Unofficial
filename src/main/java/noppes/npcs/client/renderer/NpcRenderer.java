package noppes.npcs.client.renderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import noppes.npcs.entity.EntityCustomNpc;

/**
 * Plain humanoid NPC renderer. Uses the 64x64 zombie mesh; the full player model (sleeves,
 * jacket), custom parts and the title line are stage 4. Scale comes from the vanilla SCALE
 * attribute, so no manual scaling here.
 */
public class NpcRenderer extends HumanoidMobRenderer<EntityCustomNpc, NpcRenderState, HumanoidModel<NpcRenderState>> {

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public NpcRenderState createRenderState() {
        return new NpcRenderState();
    }

    @Override
    public void extractRenderState(EntityCustomNpc npc, NpcRenderState state, float partialTick) {
        super.extractRenderState(npc, state, partialTick);
        state.texture = npc.getSkinTexture();
    }

    @Override
    public Identifier getTextureLocation(NpcRenderState state) {
        return state.texture;
    }

    /** 1.12.2: showName 1 ("never") also hides the name when the crosshair is on the NPC. */
    @Override
    protected boolean shouldShowName(EntityCustomNpc npc, double distanceToCameraSq) {
        return npc.getSyncedShowName() != 1 && super.shouldShowName(npc, distanceToCameraSq);
    }
}
