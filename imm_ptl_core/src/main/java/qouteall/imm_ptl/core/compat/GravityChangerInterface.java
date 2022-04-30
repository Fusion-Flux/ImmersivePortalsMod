package qouteall.imm_ptl.core.compat;

import me.andrew.gravitychanger.accessor.EntityAccessor;
import me.andrew.gravitychanger.api.GravityChangerAPI;
import me.andrew.gravitychanger.util.RotationUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.my_util.DQuaternion;

import javax.annotation.Nullable;

public class GravityChangerInterface {
    public static Invoker invoker = new Invoker();
    
    public static class Invoker {
        public boolean isGravityChangerPresent() {
            return false;
        }
        
        public Vec3 getEyeOffset(Entity entity) {
            return new Vec3(0, entity.getEyeHeight(), 0);
        }
        
        public Direction getGravityDirection(Player entity) {
            return Direction.DOWN;
        }
        
        public void setGravityDirection(Entity entity, Direction direction) {
            if (entity instanceof Player && entity.level.isClientSide()) {
                warnGravityChangerNotPresent();
            }
        }
        
        @Nullable
        public DQuaternion getExtraCameraRotation(Direction gravityDirection) {
            return null;
        }
        
        public Vec3 getWorldVelocity(Entity entity) {
            return entity.getDeltaMovement();
        }
        
        public void setWorldVelocity(Entity entity, Vec3 newVelocity) {
            entity.setDeltaMovement(newVelocity);
        }
        
        public Vec3 transformPlayerToWorld(Direction gravity, Vec3 vec3d) {
            return vec3d;
        }
        
        public Vec3 transformWorldToPlayer(Direction gravity, Vec3 vec3d) {
            return vec3d;
        }
    }
    
    private static boolean warned = false;
    
    @Environment(EnvType.CLIENT)
    private static void warnGravityChangerNotPresent() {
        if (!warned) {
            warned = true;
            CHelper.printChat(new TranslatableComponent("imm_ptl.missing_gravity_changer")
                .append(McHelper.getLinkText("https://modrinth.com/mod/gravitychanger"))
            );
        }
    }
    
    public static class OnGravityChangerPresent extends Invoker {
        
        @Override
        public boolean isGravityChangerPresent() {
            return true;
        }
        
        @Override
        public Vec3 getEyeOffset(Entity entity) {
            if (entity instanceof Player player) {
                return GravityChangerAPI.getEyeOffset(player);
            }
            else {
                return super.getEyeOffset(entity);
            }
        }
        
        @Override
        public Direction getGravityDirection(Player entity) {
            return ((EntityAccessor) entity).gravitychanger$getAppliedGravityDirection();
        }
        
        @Override
        public void setGravityDirection(Entity entity, Direction direction) {
            GravityChangerAPI.setGravityDirection(entity,direction);
        }
        
        @Nullable
        @Override
        public DQuaternion getExtraCameraRotation(Direction gravityDirection) {
            if (gravityDirection == Direction.DOWN) {
                return null;
            }
            
            return DQuaternion.fromMcQuaternion(
                RotationUtil.getWorldRotationQuaternion(gravityDirection)
            );
        }
        
        @Override
        public Vec3 getWorldVelocity(Entity entity) {
            if (entity instanceof Player player) {
                return GravityChangerAPI.getWorldVelocity(player);
            }
            else {
                return super.getWorldVelocity(entity);
            }
        }
        
        @Override
        public void setWorldVelocity(Entity entity, Vec3 newVelocity) {
            if (entity instanceof Player player) {
                GravityChangerAPI.setWorldVelocity(player, newVelocity);
            }
            else {
                super.setWorldVelocity(entity, newVelocity);
            }
        }
        
        @Override
        public Vec3 transformPlayerToWorld(Direction gravity, Vec3 vec3d) {
            return RotationUtil.vecPlayerToWorld(vec3d, gravity);
        }
        
        @Override
        public Vec3 transformWorldToPlayer(Direction gravity, Vec3 vec3d) {
            return RotationUtil.vecWorldToPlayer(vec3d, gravity);
        }
    }
}
