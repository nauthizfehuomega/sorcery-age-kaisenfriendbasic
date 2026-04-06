package radon.jujutsu_kaisen.capability.data.ten_shadows;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import radon.jujutsu_kaisen.JJKConstants;
import radon.jujutsu_kaisen.ability.IAdditionalAdaptation;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.*;
import radon.jujutsu_kaisen.config.ConfigHolder;
import radon.jujutsu_kaisen.damage.JJKDamageSources;
import radon.jujutsu_kaisen.entity.ten_shadows.WheelEntity;

import java.util.*;

public class TenShadowsData implements ITenShadowsData {
    private static final float PROGRESS_STEP = 0.05F;
    private static final int PROGRESS_INTERVAL_TICKS = 20 * 20;
    private static final long DECAY_DELAY_TICKS = 60 * 20L;
    private static final float DECAY_STEP = 0.05F;
    private static final int DECAY_INTERVAL_TICKS = 20 * 20;

    private final Set<ResourceLocation> tamed;
    private final Set<ResourceLocation> dead;
    private final List<ItemStack> shadowInventory;
    // Legacy maps kept for migration compatibility.
    private final Map<Adaptation, Integer> adapted;
    private final Map<Adaptation, Integer> adapting;
    private final Map<Adaptation, Float> adaptationProgress;
    private final Map<Adaptation, Integer> adaptationTickBuffer;
    private final Map<Adaptation, Long> adaptationLastCombatTick;
    private final Map<Adaptation, Integer> adaptationCD = new HashMap<>();

    private TenShadowsMode mode;

    private LivingEntity owner;

    public TenShadowsData() {
        this.mode = TenShadowsMode.SUMMON;
        this.tamed = new HashSet<>();
        this.dead = new HashSet<>();
        this.adapted = new HashMap<>();
        this.adapting = new HashMap<>();
        this.adaptationProgress = new HashMap<>();
        this.adaptationTickBuffer = new HashMap<>();
        this.adaptationLastCombatTick = new HashMap<>();
        this.shadowInventory = new ArrayList<>();
    }

    @Override
    public void resetAdaptations() {
        this.adapted.clear();
        this.adapting.clear();
        this.adaptationProgress.clear();
        this.adaptationTickBuffer.clear();
        this.adaptationLastCombatTick.clear();
        this.adaptationCD.clear();
    }

    private float clampProgress(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private float getProgress(Adaptation adaptation) {
        return this.adaptationProgress.getOrDefault(adaptation, 0.0F);
    }

    private void setProgress(Adaptation adaptation, float value) {
        float clamped = this.clampProgress(value);
        if (clamped <= 0.0F) {
            this.adaptationProgress.remove(adaptation);
            this.adapted.remove(adaptation);
            return;
        }
        this.adaptationProgress.put(adaptation, clamped);
        if (clamped >= 1.0F) {
            this.adapted.put(adaptation, Math.max(1, this.adapted.getOrDefault(adaptation, 0)));
        } else {
            this.adapted.remove(adaptation);
        }
    }

    private boolean isFullyAdapted(Adaptation adaptation) {
        return this.getProgress(adaptation) >= 1.0F;
    }

    private void updateAdaptation() {
        ISorcererData cap = this.owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();

        if (!cap.hasToggled(JJKAbilities.WHEEL.get()) || (cap.hasToggled(JJKAbilities.DOMAIN_AMPLIFICATION.get()) && cap.getExperience() < ConfigHolder.SERVER.requiredExperienceForExperienced.get().floatValue()) ) {
            this.adapting.clear();
            this.adapted.clear();
            this.adaptationProgress.clear();
            this.adaptationTickBuffer.clear();
            this.adaptationLastCombatTick.clear();
            return;
        } else if (cap.hasToggled(JJKAbilities.DOMAIN_AMPLIFICATION.get()   ) && cap.getExperience() >= ConfigHolder.SERVER.requiredExperienceForExperienced.get().floatValue()) {
            return;
        }
        
        long gameTime = this.owner.level().getGameTime();

        Iterator<Map.Entry<Adaptation, Integer>> iter = this.adapting.entrySet().iterator();
  
        while (iter.hasNext()) {
            Map.Entry<Adaptation, Integer> entry = iter.next();
            Adaptation adaptation = entry.getKey();
            if (adaptation == null) {
                iter.remove();
                continue;
            }

            if (this.isFullyAdapted(adaptation)) {
                iter.remove();
                continue;
            }

            int buffer = this.adaptationTickBuffer.getOrDefault(adaptation, 0) + 1;
            boolean completed = false;
            while (buffer >= PROGRESS_INTERVAL_TICKS) {
                buffer -= PROGRESS_INTERVAL_TICKS;
                float previous = this.getProgress(adaptation);
                float next = this.clampProgress(previous + PROGRESS_STEP);
                this.setProgress(adaptation, next);
                if (next >= 1.0F && previous < 1.0F) {
                    completed = true;
                    iter.remove();
                    this.owner.level().playSound(null, this.owner.getX(), this.owner.getY(), this.owner.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.MASTER, 3.0F, 1.0F);
                    break;
                }
            }
            this.adaptationTickBuffer.put(adaptation, buffer);
            entry.setValue(Math.round(this.getProgress(adaptation) * JJKConstants.REQUIRED_ADAPTATION));

            if (completed) {
                WheelEntity wheel = cap.getSummonByClass(WheelEntity.class);
                if (wheel != null) {
                    wheel.spin();
                }
            }
        }

        Set<Adaptation> tracked = new HashSet<>(this.adaptationProgress.keySet());
        for (Adaptation adaptation : tracked) {
            if (this.adapting.containsKey(adaptation)) {
                continue;
            }
            long lastCombat = this.adaptationLastCombatTick.getOrDefault(adaptation, gameTime);
            long idleTicks = gameTime - lastCombat;
            if (idleTicks <= DECAY_DELAY_TICKS) {
                continue;
            }

            int buffer = this.adaptationTickBuffer.getOrDefault(adaptation, 0) + 1;
            while (buffer >= DECAY_INTERVAL_TICKS) {
                buffer -= DECAY_INTERVAL_TICKS;
                this.setProgress(adaptation, this.getProgress(adaptation) - DECAY_STEP);
            }

            if (!this.adaptationProgress.containsKey(adaptation)) {
                this.adapting.remove(adaptation);
                this.adaptationTickBuffer.remove(adaptation);
                this.adaptationLastCombatTick.remove(adaptation);
                this.adapted.remove(adaptation);
                continue;
            }

            this.adaptationTickBuffer.put(adaptation, buffer);
        }

        Iterator<Map.Entry<Adaptation, Integer>> cdIter = this.adaptationCD.entrySet().iterator();
        while (cdIter.hasNext()) {
            Map.Entry<Adaptation, Integer> cdEntry = cdIter.next();
            int cd = cdEntry.getValue() - 1;
            if (cd <= 0) {
                cdIter.remove();
            } else {
                cdEntry.setValue(cd);
            }
        }
    }

    @Override
    public void tick(LivingEntity owner) {
        if (this.owner == null) {
            this.owner = owner;
        }

      //  if (!this.owner.level().isClientSide) {
            this.updateAdaptation();
       // }
    }

    @Override
    public void init(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    public boolean hasTamed(Registry<EntityType<?>> registry, EntityType<?> entity) {
        return this.tamed.contains(registry.getKey(entity));
    }

    @Override
    public void tame(Registry<EntityType<?>> registry, EntityType<?> entity) {
        this.tamed.add(registry.getKey(entity));
    }

    @Override
    public void setTamed(Set<ResourceLocation> tamed) {
        this.tamed.clear();
        this.tamed.addAll(tamed);
    }

    @Override
    public Set<ResourceLocation> getTamed() {
        return this.tamed;
    }

    @Override
    public boolean isDead(Registry<EntityType<?>> registry, EntityType<?> entity) {
        return this.dead.contains(registry.getKey(entity));
    }

    @Override
    public Set<ResourceLocation> getDead() {
        return this.dead;
    }

    @Override
    public void setDead(Set<ResourceLocation> dead) {
        this.dead.clear();
        this.dead.addAll(dead);
    }

    @Override
    public void kill(Registry<EntityType<?>> registry, EntityType<?> entity) {
        this.dead.add(registry.getKey(entity));
    }

    @Override
    public void revive(boolean full) {
        this.dead.clear();

        if (full) {
            this.tamed.clear();
        }
    }

    @Override
    public Map<Adaptation, Integer> getAdapted() {
        return this.adapted;
    }

    @Override
    public void addAdapted(Map<Adaptation, Integer> adaptations) {
        this.adapted.putAll(adaptations);
    }

    @Override
    public Map<Adaptation, Integer> getAdapting() {
        return this.adapting;
    }

    @Override
    public void addAdapting(Map<Adaptation, Integer> adapting) {
        this.adapting.putAll(adapting);
    }

    @Override
    public void addShadowInventory(ItemStack stack) {
        this.shadowInventory.add(stack);
    }

    @Override
    public ItemStack getShadowInventory(int index) {
        return this.shadowInventory.get(index);
    }

    @Override
    public List<ItemStack> getShadowInventory() {
        return this.shadowInventory;
    }

    @Override
    public void removeShadowInventory(int index) {
        this.shadowInventory.remove(index);
    }

    @Override
    public int getAdaptation(Ability ability) {
        for (Map.Entry<Adaptation, Integer> entry : this.adapted.entrySet()) {
            Adaptation adapted = entry.getKey();

            Ability current = adapted.getAbility();

            if (current == null) continue;

            if (current == ability) return entry.getValue();

            Ability.Classification first = current.getClassification();
            Ability.Classification second = ability.getClassification();

            if (first == Ability.Classification.NONE || second == Ability.Classification.NONE) continue;
            if (first == second) return entry.getValue();
        }
        return 0;
    }

    @Override
    public float getAdaptationProgress(DamageSource source) {
        return this.getAdaptationProgress(this.getAdaptation(source));
    }

    @Override
    public float getAdaptationProgress(Adaptation adaptation) {
        return this.getProgress(adaptation);
    }

    @Override
    public Adaptation.Type getAdaptationType(DamageSource source) {
        Adaptation adaptation = this.getAdaptation(source);
        return this.getAdaptationType(adaptation);
    }

    @Override
    public Adaptation.Type getAdaptationType(Adaptation adaptation) {
        RegistryAccess registry = this.owner.level().registryAccess();
        Registry<DamageType> types = registry.registryOrThrow(Registries.DAMAGE_TYPE);

        DamageType type = types.get(adaptation.getKey());

        if (type == types.get(DamageTypes.MOB_ATTACK) || type == types.get(DamageTypes.PLAYER_ATTACK)) {
            return Adaptation.Type.COUNTER;
        }
        return Adaptation.Type.DAMAGE;
    }

    // @Override
    // public Map<Adaptation.Type, Float> getAdaptationTypes() {
    //     Map<Adaptation.Type, Float> adaptations = new HashMap<>();

    //     for (Adaptation adaptation : this.adapting.keySet()) {
    //         adaptations.put(this.getAdaptationType(adaptation), this.getAdaptationProgress(adaptation));
    //     }
    //     // for (Adaptation adaptation : this.adapted.keySet()) {
    //     //     adaptations.put(this.getAdaptationType(adaptation), this.getAdaptationProgress(adaptation));
    //     // }
    //     return adaptations;
    // }

    // @Override
    // public boolean isAdaptedTo(DamageSource source) {
    //     Adaptation adaptation = this.getAdaptation(source);
    //     return this.adapted.contains(adaptation);
    // }

    private Adaptation getAdaptation(DamageSource source) {
        RegistryAccess registry = this.owner.level().registryAccess();
        Registry<DamageType> types = registry.registryOrThrow(Registries.DAMAGE_TYPE);
        return new Adaptation(types.getKey(source.type()),
                source instanceof JJKDamageSources.JujutsuDamageSource cap ? cap.getAbility() : null);
    }

     @Override
    public boolean isAdaptedTo(DamageSource source) {
        if (source instanceof JJKDamageSources.JujutsuDamageSource jujutsu) {
            Ability ability = jujutsu.getAbility();

            if (ability != null) {
                return this.isAdaptedTo(ability);
            }
        }
        Adaptation adaptation = this.getAdaptation(source);
        return this.isFullyAdapted(adaptation);
    }

    @Override
    public boolean isAdaptedTo(Ability ability) {
        for (Adaptation adaptation : this.adaptationProgress.keySet() ) {
            if (!this.isFullyAdapted(adaptation)) continue;
            Ability current = adaptation.getAbility();

            if (current == null) continue;

            if (current == ability) return true;

            Ability.Classification first = current.getClassification();
            Ability.Classification second = ability.getClassification();

            if (first == Ability.Classification.NONE || second == Ability.Classification.NONE) continue;
            if (first == second) return true;
        }
        return false;
    }

    @Override
    public boolean isAdaptedTo(CursedTechnique technique) {
        for (Ability ability : technique.getAbilities()) {
            if (this.isAdaptedTo(ability)) return true;
        }
        return false;
    }


    @Override
    public void tryAdapt(DamageSource source) {
        RegistryAccess registry = this.owner.level().registryAccess();
        Registry<DamageType> types = registry.registryOrThrow(Registries.DAMAGE_TYPE);
        ISorcererData cap = this.owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();
        if (cap.hasToggled(JJKAbilities.DOMAIN_AMPLIFICATION.get())) {
            return;
        }
        ResourceLocation key = types.getKey(source.type());
        Ability ability = source instanceof JJKDamageSources.JujutsuDamageSource jujutsu ? jujutsu.getAbility() : null;
        if (key == null && ability == null) return;

        Adaptation adaptation = new Adaptation(key != null ? key : JJKDamageSources.JUJUTSU.location(), ability);
        if (this.adaptationCD.containsKey(adaptation)) {
            return;
        }
        long gameTime = this.owner.level().getGameTime();
        this.adaptationLastCombatTick.put(adaptation, gameTime);
        this.adaptationProgress.putIfAbsent(adaptation, this.clampProgress((float) this.adapting.getOrDefault(adaptation, 0) / JJKConstants.REQUIRED_ADAPTATION));
        this.adaptationTickBuffer.putIfAbsent(adaptation, 0);
        if (!this.adapting.containsKey(adaptation)) {
            this.adapting.put(adaptation, 0);    
        } else {
            int timer = this.adapting.get(adaptation); 
            timer += JJKConstants.ADAPTATION_STEP;
            this.adapting.put(adaptation, timer);
        }
         this.adaptationCD.put(adaptation, JJKConstants.ADAPT_CD_TIME);
    }

    @Override
    public void tryAdapt(Ability ability) {
        Adaptation adaptation = new Adaptation(JJKDamageSources.JUJUTSU.location(), ability);
        ISorcererData cap = this.owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();
        if (cap.hasToggled(JJKAbilities.DOMAIN_AMPLIFICATION.get())) {
            return;
        }
        if (this.isAdaptedTo(ability) && (!(ability instanceof IAdditionalAdaptation additional) || (this.adapted != null &&  this.adapted.get(adaptation) != null && this.adapted.get(adaptation) >= additional.getAdditional() + 1)) )
            return;
        if (this.adaptationCD.containsKey(adaptation)) {
            return;
        }
        long gameTime = this.owner.level().getGameTime();
        this.adaptationLastCombatTick.put(adaptation, gameTime);
        this.adaptationProgress.putIfAbsent(adaptation, this.clampProgress((float) this.adapting.getOrDefault(adaptation, 0) / JJKConstants.REQUIRED_ADAPTATION));
        this.adaptationTickBuffer.putIfAbsent(adaptation, 0);
        if (!this.adapting.containsKey(adaptation)) {
            this.adapting.put(adaptation, 0);
        } else {
            int timer = this.adapting.get(adaptation);
            timer += JJKConstants.ADAPTATION_STEP;
            this.adapting.put(adaptation, timer);
        }
           this.adaptationCD.put(adaptation, JJKConstants.ADAPT_CD_TIME);
    }

    @Override
    public TenShadowsMode getMode() {
        return this.mode;
    }

    @Override
    public void setMode(TenShadowsMode mode) {
        this.mode = mode;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("mode", this.mode.ordinal());

        ListTag tamedTag = new ListTag();

        for (ResourceLocation key : this.tamed) {
            tamedTag.add(StringTag.valueOf(key.toString()));
        }
        nbt.put("tamed", tamedTag);

        ListTag deadTag = new ListTag();

        for (ResourceLocation key : this.dead) {
            deadTag.add(StringTag.valueOf(key.toString()));
        }
        nbt.put("dead", deadTag);

        ListTag adaptedTag = new ListTag();

        for (Map.Entry<Adaptation, Integer> adaptation : this.adapted.entrySet()) {
            CompoundTag data = new CompoundTag();
            Adaptation key = adaptation.getKey();
            if (key == null || key.getAbility() == null || key.getKey() == null) continue;
            data.put("adaptation", key.serializeNBT());
            data.putInt("stage", adaptation.getValue());
            adaptedTag.add(data);
        }
        nbt.put("adapted", adaptedTag);

        ListTag adaptingTag = new ListTag();
        for (Map.Entry<Adaptation, Integer> entry : this.adapting.entrySet()) {
            CompoundTag data = new CompoundTag();
            data.put("adaptation", entry.getKey().serializeNBT());
            data.putInt("stage", entry.getValue());
            adaptingTag.add(data);
        }
        nbt.put("adapting", adaptingTag);

        ListTag adaptationProgressTag = new ListTag();
        for (Map.Entry<Adaptation, Float> entry : this.adaptationProgress.entrySet()) {
            Adaptation adaptation = entry.getKey();
            if (adaptation == null || (adaptation.getAbility() == null && adaptation.getKey() == null)) {
                continue;
            }
            CompoundTag data = new CompoundTag();
            data.put("adaptation", adaptation.serializeNBT());
            data.putFloat("progress", this.clampProgress(entry.getValue()));
            data.putLong("lastCombat", this.adaptationLastCombatTick.getOrDefault(adaptation, 0L));
            data.putInt("buffer", this.adaptationTickBuffer.getOrDefault(adaptation, 0));
            adaptationProgressTag.add(data);
        }
        nbt.put("adaptation_progress", adaptationProgressTag);

        ListTag shadowInventoryTag = new ListTag();

        for (ItemStack stack : this.shadowInventory) {
            shadowInventoryTag.add(stack.save(new CompoundTag()));
        }
        nbt.put("shadow_inventory", shadowInventoryTag);

        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.mode = TenShadowsMode.values()[nbt.getInt("mode")];

        this.tamed.clear();

        for (Tag key : nbt.getList("tamed", Tag.TAG_STRING)) {
            this.tamed.add(new ResourceLocation(key.getAsString()));
        }

        this.dead.clear();

        for (Tag key : nbt.getList("dead", Tag.TAG_STRING)) {
            this.dead.add(new ResourceLocation(key.getAsString()));
        }

        this.adapted.clear();
        this.adapting.clear();
        this.adaptationProgress.clear();
        this.adaptationTickBuffer.clear();
        this.adaptationLastCombatTick.clear();

        if (nbt.contains("adaptation_progress", Tag.TAG_LIST)) {
            for (Tag key : nbt.getList("adaptation_progress", Tag.TAG_COMPOUND)) {
                CompoundTag data = (CompoundTag) key;
                Adaptation adaptation = new Adaptation(data.getCompound("adaptation"));
                if (adaptation.getAbility() == null && adaptation.getKey() == null) continue;
                float progress = this.clampProgress(data.getFloat("progress"));
                if (progress <= 0.0F) continue;
                this.adaptationProgress.put(adaptation, progress);
                this.adaptationLastCombatTick.put(adaptation, data.getLong("lastCombat"));
                this.adaptationTickBuffer.put(adaptation, Math.max(0, data.getInt("buffer")));
                if (progress >= 1.0F) {
                    this.adapted.put(adaptation, 1);
                } else {
                    this.adapting.put(adaptation, Math.round(progress * JJKConstants.REQUIRED_ADAPTATION));
                }
            }
        } else {
            for (Tag key : nbt.getList("adapted", Tag.TAG_COMPOUND)) {
                CompoundTag data = (CompoundTag) key;
                Adaptation adaptation = new Adaptation(data.getCompound("adaptation"));
                if (adaptation.getAbility() == null && adaptation.getKey() == null) continue;
                this.adapted.put(adaptation, data.getInt("stage"));
                this.adaptationProgress.put(adaptation, 1.0F);
                this.adaptationTickBuffer.put(adaptation, 0);
                this.adaptationLastCombatTick.put(adaptation, 0L);
            }

            for (Tag key : nbt.getList("adapting", Tag.TAG_COMPOUND)) {
                CompoundTag data = (CompoundTag) key;
                Adaptation adaptation = new Adaptation(data.getCompound("adaptation"));
                if (adaptation.getAbility() == null && adaptation.getKey() == null) continue;
                int timer = Math.max(0, data.getInt("stage"));
                float progress = this.clampProgress((float) timer / JJKConstants.REQUIRED_ADAPTATION);
                if (progress <= 0.0F) continue;
                this.adapting.put(adaptation, timer);
                this.adaptationProgress.put(adaptation, progress);
                this.adaptationTickBuffer.put(adaptation, 0);
                this.adaptationLastCombatTick.put(adaptation, 0L);
            }
        }

        this.shadowInventory.clear();

        for (Tag key : nbt.getList("shadow_inventory", Tag.TAG_COMPOUND)) {
            this.shadowInventory.add(ItemStack.of((CompoundTag) key));
        }
    }

    
}
