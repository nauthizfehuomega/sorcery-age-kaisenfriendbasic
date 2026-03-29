package radon.jujutsu_kaisen.ability.angel;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.base.Transformation;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.item.JJKItems;
import radon.jujutsu_kaisen.util.RotationUtil;

public class AngelWings extends Transformation {
    private static final float SPEED = 0.006F;

    @Override
    public boolean isScalable(LivingEntity owner) {
        return false;
    }

    @Override
    public boolean isValid(LivingEntity owner) {
        ISorcererData cap = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();
        return cap.hasTechnique(CursedTechnique.ANGEL) && super.isValid(owner);
    }

    private static double getDistanceGround(LivingEntity entity) {
        Vec3 pos = entity.position();
        Vec3 down = pos.add(0.0, -256.0, 0.0);

        var result = entity.level().clip(new net.minecraft.world.level.ClipContext(
                pos, down,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.ANY,
                entity
        ));
        if (result.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return pos.y - result.getLocation().y;
        }
        return Double.MAX_VALUE;
    }

    @Override
    public boolean shouldTrigger(PathfinderMob owner, @Nullable LivingEntity target) {
        return getDistanceGround(owner) > 4.0F;
    }

    @Override
    public ActivationType getActivationType(LivingEntity owner) {
        return ActivationType.TOGGLED;
    }

    @Override
    public void run(LivingEntity owner) {
        if (getDistanceGround(owner) > 4.0F) {
        owner.resetFallDistance();

        Vec3 movement = owner.getDeltaMovement();
      //  Vec3 look = RotationUtil.getTargetAdjustedLookAngle(owner);
        owner.setDeltaMovement(movement.x, movement.y, movement.z);

        float f = owner.xxa * 0.5F;
        float f1 = owner.zza;

        if (f1 <= 0.0F) {
           f1 *= 0.25F;
       }
        owner.moveRelative(SPEED, new Vec3(f, 0.0F, f1));
       }
    }

    @Override
    public float getCost(LivingEntity owner) {
        return 0.0F;
    }

    @Override
    public boolean isReplacement() {
        return false;
    }

    @Override
    public Item getItem() {
        return JJKItems.WINGS.get();
    }

    @Override
    public Part getBodyPart() {
        return Part.BODY;
    }

    @Override
    public void onRightClick(LivingEntity owner) {

    }

    @Override
    public void onEnabled(LivingEntity owner) {
        
    }

    @Override
    public void onDisabled(LivingEntity owner) {

    }
}
