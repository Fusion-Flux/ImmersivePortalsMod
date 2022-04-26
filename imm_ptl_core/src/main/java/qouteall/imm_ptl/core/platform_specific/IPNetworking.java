package qouteall.imm_ptl.core.platform_specific;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationServer;
import qouteall.q_misc_util.dimension.DimId;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.q_misc_util.MiscHelper;

import java.util.UUID;

public class IPNetworking {
    public static final ResourceLocation id_stcRedirected =
        new ResourceLocation("imm_ptl", "rd");
    public static final ResourceLocation id_ctsTeleport =
        new ResourceLocation("imm_ptl", "teleport");
    public static final ResourceLocation id_stcCustom =
        new ResourceLocation("imm_ptl", "stc_custom");
    public static final ResourceLocation id_stcSpawnEntity =
        new ResourceLocation("imm_ptl", "spawn_entity");
    public static final ResourceLocation id_stcDimensionConfirm =
        new ResourceLocation("imm_ptl", "dim_confirm");
    public static final ResourceLocation id_stcUpdateGlobalPortal =
        new ResourceLocation("imm_ptl", "upd_glb_ptl");
    public static final ResourceLocation id_ctsPlayerAction =
        new ResourceLocation("imm_ptl", "player_action");
    public static final ResourceLocation id_ctsRightClick =
        new ResourceLocation("imm_ptl", "right_click");
    
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(
            id_ctsTeleport,
            (server, player, handler, buf, responseSender) -> {
                processCtsTeleport(player, buf);
            }
        );
        
        ServerPlayNetworking.registerGlobalReceiver(
            id_ctsPlayerAction,
            (server, player, handler, buf, responseSender) -> {
                processCtsPlayerAction(player, buf);
            }
        );
        
        ServerPlayNetworking.registerGlobalReceiver(
            id_ctsRightClick,
            (server, player, handler, buf, responseSender) -> {
                processCtsRightClick(player, buf);
            }
        );
        
        
        
    }
    
    // TODO the packet is being serialized in server thread which may impact performance
    // create a new vanilla packet type to allow it to be serialized in networking thread
    public static Packet createRedirectedMessage(
        ResourceKey<Level> dimension,
        Packet packet
    ) {
        int messageType = 0;
        try {
            messageType = ConnectionProtocol.PLAY.getPacketId(PacketFlow.CLIENTBOUND, packet);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        
        DimId.writeWorldId(buf, dimension, false);
        
        buf.writeInt(messageType);
    
        packet.write(buf);
    
        return new ClientboundCustomPayloadPacket(id_stcRedirected, buf);
    }
    
    public static void sendRedirectedMessage(
        ServerPlayer player,
        ResourceKey<Level> dimension,
        Packet packet
    ) {
        player.connection.send( packet);
    }
    
    public static Packet createStcDimensionConfirm(
        ResourceKey<Level> dimensionType,
        Vec3 pos
    ) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        DimId.writeWorldId(buf, dimensionType, false);
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        return new ClientboundCustomPayloadPacket(id_stcDimensionConfirm, buf);
    }
    
    //NOTE my packet is redirected but I cannot get the packet handler info here
    public static Packet createStcSpawnEntity(
        Entity entity
    ) {
        EntityType entityType = entity.getType();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(EntityType.getKey(entityType).toString());
        buf.writeInt(entity.getId());
        DimId.writeWorldId(
            buf, entity.level.dimension(),
            entity.level.isClientSide
        );
        CompoundTag tag = new CompoundTag();
        entity.saveWithoutId(tag);
        buf.writeNbt(tag);
        return new ClientboundCustomPayloadPacket(id_stcSpawnEntity, buf);
    }
    
    public static Packet createGlobalPortalUpdate(
        GlobalPortalStorage storage
    ) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        
        DimId.writeWorldId(buf, storage.world.get().dimension(), false);
        buf.writeNbt(storage.save(new CompoundTag()));
        
        return new ClientboundCustomPayloadPacket(id_stcUpdateGlobalPortal, buf);
    }
    
    private static void processCtsTeleport(ServerPlayer player, FriendlyByteBuf buf) {
        ResourceKey<Level> dim = DimId.readWorldId(buf, false);
        Vec3 posBefore = new Vec3(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        UUID portalEntityId = buf.readUUID();
        
        MiscHelper.executeOnServerThread(() -> {
            IPGlobal.serverTeleportationManager.onPlayerTeleportedInClient(
                player,
                dim,
                posBefore,
                portalEntityId
            );
        });
    }
    
    private static void processCtsPlayerAction(ServerPlayer player, FriendlyByteBuf buf) {
        ResourceKey<Level> dim = DimId.readWorldId(buf, false);
        ServerboundPlayerActionPacket packet = new ServerboundPlayerActionPacket(buf);
        IPGlobal.serverTaskList.addTask(() -> {
            BlockManipulationServer.processBreakBlock(
                dim, packet,
                player
            );
            return true;
        });
    }
    
    private static void processCtsRightClick(ServerPlayer player, FriendlyByteBuf buf) {
        ResourceKey<Level> dim = DimId.readWorldId(buf, false);
        ServerboundUseItemOnPacket packet = new ServerboundUseItemOnPacket(buf);
        IPGlobal.serverTaskList.addTask(() -> {
            BlockManipulationServer.processRightClickBlock(
                dim, packet,
                player
            );
            return true;
        });
    }
    
}
