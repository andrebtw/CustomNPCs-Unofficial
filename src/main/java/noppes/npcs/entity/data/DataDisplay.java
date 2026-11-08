package noppes.npcs.entity.data;

import java.util.Objects;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Display subset of the 1.12.2 {@code noppes.npcs.entity.data.DataDisplay}: name, title, skin,
 * name visibility and model size. Entity-independent; the owner is told about changes through
 * {@code onChange}. NBT keys are the 1.12.2 ones, stored flat in the entity's save data.
 */
public final class DataDisplay {

    public static final String DEFAULT_TEXTURE = "customnpcs:textures/entity/humanmale/steve.png";
    public static final float DEFAULT_SIZE = 5.0f;
    /** Size 5 is scale 1, so these match the vanilla SCALE attribute range 0.0625..16. */
    public static final float MIN_SIZE = 0.3125f;
    public static final float MAX_SIZE = 80f;

    private final Runnable onChange;
    private String name = "";
    private String title = "";
    private String texture = DEFAULT_TEXTURE;
    private int showName = 0;
    private float size = DEFAULT_SIZE;

    public DataDisplay(Runnable onChange) {
        this.onChange = onChange;
    }

    public String getName() { return name; }

    public void setName(String value) {
        value = Objects.requireNonNullElse(value, "");
        if (!value.equals(name)) { name = value; onChange.run(); }
    }

    public String getTitle() { return title; }

    public void setTitle(String value) {
        value = Objects.requireNonNullElse(value, "");
        if (!value.equals(title)) { title = value; onChange.run(); }
    }

    public String getTexture() { return texture; }

    public void setTexture(String value) {
        value = Objects.requireNonNullElse(value, "");
        if (!value.equals(texture)) { texture = value; onChange.run(); }
    }

    /** 0 = always, 1 = never, 2 = only while attacking. */
    public int getShowName() { return showName; }

    public void setShowName(int value) {
        if (value < 0 || value > 2) { value = 0; }
        if (value != showName) { showName = value; onChange.run(); }
    }

    public float getSize() { return size; }

    public void setSize(float value) {
        if (Float.isNaN(value)) { value = DEFAULT_SIZE; }
        value = Math.clamp(value, MIN_SIZE, MAX_SIZE);
        if (value != size) { size = value; onChange.run(); }
    }

    /** Multiplier for the vanilla SCALE attribute. */
    public float scale() { return size / DEFAULT_SIZE; }

    public void save(ValueOutput out) {
        out.putString("Name", name);
        out.putString("Title", title);
        out.putString("Texture", texture);
        out.putInt("ShowName", showName);
        out.putFloat("Size", size);
    }

    public void load(ValueInput in) {
        setName(in.getStringOr("Name", name));
        setTitle(in.getStringOr("Title", title));
        setTexture(in.getStringOr("Texture", texture));
        setShowName(in.getIntOr("ShowName", showName));
        setSize(in.getFloatOr("Size", size));
    }
}
