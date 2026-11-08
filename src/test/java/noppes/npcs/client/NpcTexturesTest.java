package noppes.npcs.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class NpcTexturesTest {
    @Test
    void validTextureIsKept() {
        assertEquals(Identifier.parse("customnpcs:textures/entity/humanmale/arcticsteve.png"),
                NpcTextures.resolve("customnpcs:textures/entity/humanmale/arcticsteve.png"));
    }

    @Test
    void invalidTextureFallsBackToSteve() {
        assertEquals(NpcTextures.DEFAULT, NpcTextures.resolve("Not A Path!"));
        assertEquals(NpcTextures.DEFAULT, NpcTextures.resolve(""));
        assertEquals(NpcTextures.DEFAULT, NpcTextures.resolve("   "));
        assertEquals(NpcTextures.DEFAULT, NpcTextures.resolve(null));
    }
}
