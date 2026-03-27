package radon.jujutsu_kaisen.util;

import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class DomainTechniqueManager<T extends Enum<T>> {
    private final LinkedHashSet<T> remembered = new LinkedHashSet<>();
    private final List<T> cache = new ArrayList<>();
    private final Class<T> enumClass;
    private final Random random = new Random();

    private int index = -1;
    private int timer;
    private boolean dirty = true;

    private @Nullable T temporary;

    private boolean randomMode;
    private int baseInterval = 120;
    private int minInterval = 40;
    private int scaling = 5;
    private int maxChannelTicks = 40;

    public DomainTechniqueManager(Class<T> enumClass) {
        this.enumClass = enumClass;
    }

    public void remember(@Nullable T tech) {
        if (tech != null && this.remembered.add(tech)) {
            this.dirty = true;
        }
    }

    public Set<T> getRemembered() {
        return Collections.unmodifiableSet(this.remembered);
    }

    public boolean hasRemembered() {
        return !this.remembered.isEmpty();
    }

    public @Nullable T getTemporary() {
        return this.temporary;
    }

    public void setTemporary(@Nullable T technique) {
        this.temporary = technique;
    }

    public void tick(boolean domainActive, boolean isChanneling, int channelTicks, Runnable sync) {
        if (!domainActive || this.remembered.isEmpty()) {
            if (this.temporary != null) {
                this.temporary = null;
                sync.run();
            }
            return;
        }

        this.timer++;

        int interval = Math.max(this.minInterval, this.baseInterval - (this.remembered.size() * this.scaling));
        if (this.timer < interval) {
            return;
        }

        this.timer = 0;

        if (isChanneling && channelTicks < this.maxChannelTicks) {
            return;
        }

        this.rebuildCache();
        if (this.cache.isEmpty()) {
            return;
        }

        T next;
        if (this.randomMode) {
            next = this.cache.get(this.random.nextInt(this.cache.size()));
        } else {
            this.index = (this.index + 1) % this.cache.size();
            next = this.cache.get(this.index);
        }

        if (next != this.temporary) {
            this.temporary = next;
            sync.run();
        }
    }

    private void rebuildCache() {
        if (!this.dirty) {
            return;
        }
        this.cache.clear();
        this.cache.addAll(this.remembered);
        this.dirty = false;
    }

    public ListTag saveAsNames() {
        ListTag tag = new ListTag();
        for (T technique : this.remembered) {
            tag.add(StringTag.valueOf(technique.name()));
        }
        return tag;
    }

    public void loadFromNames(ListTag tag) {
        this.remembered.clear();

        for (int i = 0; i < tag.size(); i++) {
            try {
                T value = Enum.valueOf(this.enumClass, tag.getString(i));
                this.remembered.add(value);
            } catch (IllegalArgumentException ignored) {
                // Ignore stale enum names from old saves.
            }
        }
        this.dirty = true;
    }

    public void loadFromOrdinals(ListTag tag) {
        this.remembered.clear();
        T[] values = this.enumClass.getEnumConstants();

        for (Tag entry : tag) {
            int id = ((IntTag) entry).getAsInt();
            if (id >= 0 && id < values.length) {
                this.remembered.add(values[id]);
            }
        }
        this.dirty = true;
    }

    public void setRandomMode(boolean randomMode) {
        this.randomMode = randomMode;
    }

    public void setInterval(int baseInterval, int minInterval, int scaling) {
        this.baseInterval = baseInterval;
        this.minInterval = minInterval;
        this.scaling = scaling;
    }

    public void setMaxChannelTicks(int maxChannelTicks) {
        this.maxChannelTicks = maxChannelTicks;
    }
}
