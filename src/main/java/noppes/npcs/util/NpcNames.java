package noppes.npcs.util;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import nikedemos.markovnames.generators.MarkovAncientGreek;
import nikedemos.markovnames.generators.MarkovAztec;
import nikedemos.markovnames.generators.MarkovCustomNPCsClassic;
import nikedemos.markovnames.generators.MarkovGenerator;
import nikedemos.markovnames.generators.MarkovJapanese;
import nikedemos.markovnames.generators.MarkovOldNorse;
import nikedemos.markovnames.generators.MarkovRoman;
import nikedemos.markovnames.generators.MarkovSaami;
import nikedemos.markovnames.generators.MarkovSlavic;
import nikedemos.markovnames.generators.MarkovSpanish;
import nikedemos.markovnames.generators.MarkovWelsh;

/**
 * Lazily built Markov name generators, in the 1.12.2 {@code CustomNpcs.MARKOV_GENERATOR} order.
 * Each dictionary is read on first use so mod construction stays fast.
 */
public final class NpcNames {

    private static final List<Supplier<MarkovGenerator>> FACTORIES = List.of(
            () -> new MarkovRoman(3),
            () -> new MarkovJapanese(4),
            () -> new MarkovSlavic(3),
            () -> new MarkovWelsh(3),
            () -> new MarkovSaami(3),
            () -> new MarkovOldNorse(4),
            () -> new MarkovAncientGreek(3),
            () -> new MarkovAztec(3),
            () -> new MarkovCustomNPCsClassic(3),
            () -> new MarkovSpanish(3));

    private static final MarkovGenerator[] CACHE = new MarkovGenerator[FACTORIES.size()];

    private NpcNames() {}

    public static int generatorCount() {
        return FACTORIES.size();
    }

    public static MarkovGenerator generator(int id) {
        synchronized (CACHE) {
            MarkovGenerator g = CACHE[id];
            if (g == null) {
                g = FACTORIES.get(id).get();
                CACHE[id] = g;
            }
            return g;
        }
    }

    /** 1.12.2 DataDisplay: a random generator excluding the last one, random gender 0/1. */
    public static String randomName(Random random) {
        return generator(random.nextInt(generatorCount() - 1)).fetch(random.nextInt(2));
    }
}
