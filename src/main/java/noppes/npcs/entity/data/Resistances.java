package noppes.npcs.entity.data;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Per-damage-type resistances, from 1.12.2 {@code noppes.npcs.entity.data.Resistances}. A value of
 * 1 is neutral, 0 doubles damage, 2 negates it (1.12.2 {@code applyResistance}: damage * (2 - v)).
 * Saved by {@link DataStats} as the {@code Resistances} list of {@code {K:string, V:float}}.
 */
public final class Resistances {

    public static final float MIN = 0.0f;
    public static final float MAX = 2.0f;

    private final Runnable onChange;
    private final Map<String, Float> data = new TreeMap<>();

    public Resistances(Runnable onChange) {
        this.onChange = onChange;
        data.put("arrow", 1.0f);
        data.put("mob", 1.0f);
        data.put("knockback", 1.0f);
        data.put("explosion", 1.0f);
    }

    /** Read-only view of the stored values. */
    public Map<String, Float> asMap() {
        return Collections.unmodifiableMap(data);
    }

    /** The value for a damage type, with the 1.12.2 aliases; 1.0 when nothing applies. */
    public float get(String damageName) {
        Float v = data.get(damageName);
        if (v != null) { return v; }
        String alias = switch (damageName) {
            case "explosion.player" -> "explosion";
            case "player" -> "mob";
            case "thrown" -> "arrow";
            default -> null;
        };
        if (alias != null && data.containsKey(alias)) { return data.get(alias); }
        return 1.0f;
    }

    public void set(String damageName, float value) {
        if (damageName == null || damageName.isEmpty()) { return; }
        if (Float.isNaN(value)) { value = 1.0f; }
        value = Math.clamp(value, MIN, MAX);
        Float old = data.put(damageName, value);
        if (old == null || old != value) { onChange.run(); }
    }

    public void save(ValueOutput out) {
        ValueOutput.ValueOutputList list = out.childrenList("Resistances");
        for (Map.Entry<String, Float> e : data.entrySet()) {
            ValueOutput entry = list.addChild();
            entry.putString("K", e.getKey());
            entry.putFloat("V", e.getValue());
        }
    }

    /**
     * A list replaces the map (1.12.2 {@code load} clears first); entries without a key are skipped,
     * an entry without a value is neutral. The pre-list compound {@code {Arrow,Melee,Knockback,Explosion}}
     * (1.12.2 {@code oldLoad}) sets those four; absent keeps the current values.
     */
    public void load(ValueInput in) {
        var list = in.childrenList("Resistances");
        if (list.isPresent()) {
            data.clear();
            for (ValueInput entry : list.get()) {
                String key = entry.getStringOr("K", "");
                if (key.isEmpty()) { continue; }
                float v = entry.getFloatOr("V", 1.0f);
                data.put(key, Float.isNaN(v) ? 1.0f : Math.clamp(v, MIN, MAX));
            }
            onChange.run();
            return;
        }
        in.child("Resistances").ifPresent(old -> {
            set("arrow", old.getFloatOr("Arrow", get("arrow")));
            set("mob", old.getFloatOr("Melee", get("mob")));
            set("knockback", old.getFloatOr("Knockback", get("knockback")));
            set("explosion", old.getFloatOr("Explosion", get("explosion")));
        });
    }
}
