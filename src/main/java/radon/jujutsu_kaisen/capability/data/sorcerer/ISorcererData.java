package radon.jujutsu_kaisen.capability.data.sorcerer;

import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import org.jetbrains.annotations.Nullable;

import com.mojang.authlib.GameProfile;

import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.*;

import java.util.*;

@AutoRegisterCapability
public interface ISorcererData {
    void attack(DamageSource source, LivingEntity target);

    void tick(LivingEntity owner);

    void init(LivingEntity owner);

    int getCursedEnergyColor();

    void setCursedEnergyColor(int color);

    float getMaximumOutput();

    void increaseOutput();

    void decreaseOutput();

    void maxOutput();

    int getPoints();

    void setPoints(int points);

    void addPoints(int points);

    void usePoints(int count);

    boolean isUnlocked(Ability ability);

    void unlock(Ability ability);

    void unlockAll(List<Ability> abilities);

    void lock(Ability ability);

    void lockAll();

    void createPact(UUID recipient, Pact pact);

    Map<UUID, Set<Pact>> getAcceptedPacts();

    boolean hasPact(UUID recipient, Pact pact);

    Player getPactPartner(UUID recipient, Pact pact);

    void removePact(UUID recipient, Pact pact);

    void createPactCreationRequest(UUID recipient, Pact pact);

    void createPactRemovalRequest(UUID recipient, Pact pact);

    void removePactCreationRequest(UUID recipient, Pact pact);

    void removePactRemovalRequest(UUID recipient, Pact pact);

    boolean hasRequestedPactCreation(UUID recipient, Pact pact);

    boolean hasRequestedPactRemoval(UUID recipient, Pact pact);

    void addBindingVow(BindingVow vow);

    void removeBindingVow(BindingVow vow);

    boolean hasBindingVow(BindingVow vow);

    void addBindingVowCooldown(BindingVow vow);

    int getRemainingCooldown(BindingVow vow);

    void resetVows();

    float getAdditionalEnergy();

    void setAdditionalEnergy(float additionalEnergy);

    int getShieldTicks();

    void setShieldTicks(int ticks);

    boolean isCooldownDone(BindingVow vow);

    void addChant(Ability ability, String chant);

    void addChants(Ability ability, Set<String> chants);

    void removeChant(Ability ability, String chant);

    boolean hasChant(Ability ability, String chant);

    boolean isChantsAvailable(Set<String> chants);

    @Nullable Ability getAbility(String chant);

    @Nullable Ability getAbility(Set<String> chants);

    Set<String> getFirstChants();

    Set<String> getFirstChants(Ability ability);

    //void unlockDomain();

    float getOutput();

    float getAbilityPower();

    float getRealPower();

    float getRealPower(float exp);

    float getExperience();

    void setExperience(float experience);

    boolean addExperience(float amount);

    float getDomainSize();

    void setDomainSize(float domainSize);

    boolean hasToggled(Ability ability);

    @Nullable CursedTechnique getAdditional();

    void setAdditional(CursedTechnique technique);

    @Nullable CursedTechnique getTechnique();
    Set<CursedTechnique> getTechniques();

    boolean hasTechnique(CursedTechnique technique);

    void setTechnique(@Nullable CursedTechnique technique);

    CursedEnergyNature getNature();

    void setNature(CursedEnergyNature nature);

    void setGrade(SorcererGrade grade);

    boolean hasTrait(Trait trait);

    void addTrait(Trait trait);

    void addTraits(List<Trait> traits);

    void removeTrait(Trait trait);

    Set<Trait> getTraits();

    void setTraits(Set<Trait> traits);

    void setType(JujutsuType type);

    JujutsuType getType();

    void toggle(Ability ability);

    void clearToggled();

    Set<Ability> getToggled();

    void clearCooldown(Ability ability);

    void addCooldown(Ability ability);

    void setCooldown(Ability ability, int time);

    int getRemainingCooldown(Ability ability);

    void disrupt(Ability ability, int duration);

    boolean isCooldownDone(Ability ability);

    void addDuration(Ability ability);

    void increaseBrainDamage();
    int getBrainDamage();
    
    void hurtThroat(int cooldown);
    
    int getThroatDamage();

    boolean isThroatDamaged();

    void setBurnout(int duration);

    void setDisable(int duration);

    void setDisarmed(int duration);

    void setSilenced(int duration);

    void setSelfHit(int duration);

    void setExtraMeleeTaken(int duration);

    int getBurnout();

    int getDisable();

    int getDisarmed();

    int getSilenced();

    int getSelfHit();

    int getExtraMeleeTaken();

    boolean hasBurnout();

    boolean hasDisable();

    boolean hasDisarmed();

    boolean hasSilenced();

    boolean hasSelfHit();

    boolean hasExtraMeleeTaken();

    void resetCooldowns();

    void resetBurnout();

    void resetDisable();

    void resetDisarmed();

    void resetSilenced();

    void resetSelfHit();

    float getMaxEnergy();

    void setMaxEnergy(float maxEnergy);

    float getEnergy();

    void addEnergy(float amount);

    void useEnergy(float amount);

    void setEnergy(float energy);

    float getExtraEnergy();

    void addExtraEnergy(float amount);

    void resetExtraEnergy();

    void onBlackFlash();

    long getLastBlackFlashTime();

    boolean addBlackFlash();

    void removeLife();
        
    void setLives(int count);

    int getLives();

    boolean checkWombAwakened();

    void setWombAwakened(boolean bool);

    void moreBlackFlash(boolean bool);

    void resetBlackFlash();

    boolean isInZone();

    void delayTickEvent(Runnable task, int delay);
    
    void uncopy(CursedTechnique technique);

    void unsteal(CursedTechnique technique);

    void copy(@Nullable CursedTechnique technique);

  
    void steal(@Nullable CursedTechnique technique);

    void resetSteal();

    void setStolen(Set<CursedTechnique> stolenTechs);
    
    void resetCopy();

    void setCopies(Set<CursedTechnique> copiedTechs);

    Set<CursedTechnique> getCopied();
    Set<CursedTechnique> getRemembered();

    void setCurrentCopied(@Nullable CursedTechnique technique);

    @Nullable CursedTechnique getCurrentCopied();

    void setTemporaryTechnique(@Nullable CursedTechnique technique);

    @Nullable CursedTechnique getTemporaryTechnique();

    void absorb(@Nullable CursedTechnique technique);

    void unabsorb(CursedTechnique technique);

    Set<CursedTechnique> getAbsorbed();

    void setCurrentAbsorbed(@Nullable CursedTechnique technique);

    @Nullable CursedTechnique getCurrentAbsorbed();

    Set<CursedTechnique> getStolen();

    void setCurrentStolen(@Nullable CursedTechnique technique);

    @Nullable CursedTechnique getCurrentStolen();

    @Nullable CursedTechnique getLastStolen();

    @Nullable CursedTechnique getLatestStolen();

    void addStolen(CursedTechnique technique);

    boolean hasStolen(CursedTechnique technique);

    // void setStolenSkinTexture(ResourceLocation skin);

    // void setStolenSkinProfile(GameProfile profile);


    // @Nullable GameProfile getStolenSkinProfile();

    // @Nullable ResourceLocation getStolenSkinTexture();

    int getTransfiguredSouls();

    void increaseTransfiguredSouls();

    void decreaseTransfiguredSouls();

    void useTransfiguredSouls(int amount);

    @Nullable Ability getChanneled();

    void channel(@Nullable Ability ability);

    boolean isChanneling(Ability ability);

    int getCharge(Ability ability);

    void wipe(ServerPlayer owner);

    void generate(ServerPlayer owner);

    void addSummon(Entity entity);

    void removeSummon(Entity entity);

    List<Entity> getSummons();

    <T extends Entity> @Nullable T getSummonByClass(Class<T> clazz);

    <T extends Entity> List<T> getSummonsByClass(Class<T> clazz);

    <T extends Entity> void unsummonByClass(Class<T> clazz);

    <T extends Entity> void removeSummonByClass(Class<T> clazz);

    <T extends Entity> boolean hasSummonOfClass(Class<T> clazz);

    void addCurse(AbsorbedCurse curse);

    void removeCurse(AbsorbedCurse curse);

    List<AbsorbedCurse> getCurses();

    @Nullable
    AbsorbedCurse getCurse(EntityType<?> type);

    boolean hasCurse(EntityType<?> type);

    List<AbstractMap.SimpleEntry<Vec3, Float>> getFrames();
    void addFrame(Vec3 frame, float yaw);
    void removeFrame(AbstractMap.SimpleEntry<Vec3, Float> frame);
    void resetFrames();

    int getSpeedStacks();
    void addSpeedStack();
    void lowerSpeedStacks();
    void resetSpeedStacks();

    int getFingers();
    void setFingers(int count);
    int addFingers(int count);

    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);

    void addDash();
    int getDash();
    void subDash();
    void resetDash();
}
