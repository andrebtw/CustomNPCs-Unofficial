package noppes.npcs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NpcNamesTest {
    @Test
    void hasTenGenerators() {
        assertEquals(10, NpcNames.generatorCount());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
    void everyGeneratorProducesNames(int id) {
        for (int gender = 0; gender < 2; gender++)
            assertFalse(NpcNames.generator(id).fetch(gender).isBlank(), "generator " + id);
    }

    // Not deterministic per seed: the 1.12.2 generators draw from the static, unseeded
    // MarkovDictionary.rnd. The Markov algorithm is copied as-is, so only non-blank is asserted.
    @Test
    void randomNameIsNonBlank() {
        for (int seed = 0; seed < 20; seed++)
            assertFalse(NpcNames.randomName(new Random(seed)).isBlank(), "seed " + seed);
    }
}
