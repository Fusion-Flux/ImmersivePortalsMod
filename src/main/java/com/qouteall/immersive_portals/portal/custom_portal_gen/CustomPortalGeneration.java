package com.qouteall.immersive_portals.portal.custom_portal_gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.ListCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.qouteall.immersive_portals.portal.custom_portal_gen.form.PortalGenForm;
import com.qouteall.immersive_portals.portal.custom_portal_gen.trigger.PortalGenTrigger;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Function;

public class CustomPortalGeneration {
    public static final Codec<List<RegistryKey<World>>> dimensionListCodec =
        new ListCodec<>(World.CODEC);
    
    public static RegistryKey<Registry<Codec<CustomPortalGeneration>>> schemaRegistryKey = RegistryKey.ofRegistry(
        new Identifier("imm_ptl:custom_portal_gen_schema")
    );
    
    public static final Codec<CustomPortalGeneration> codecV1 = RecordCodecBuilder.create(instance -> {
        return instance.group(
            dimensionListCodec.fieldOf("from").forGetter(o -> o.fromDimensions),
            World.CODEC.fieldOf("to").forGetter(o -> o.toDimension),
            Codec.INT.fieldOf("space_ratio_from").forGetter(o -> o.spaceRatioFrom),
            Codec.INT.fieldOf("space_ratio_to").forGetter(o -> o.spaceRatioTo),
            Codec.BOOL.fieldOf("two_way").forGetter(o -> o.twoWay),
            PortalGenForm.codec.fieldOf("form").forGetter(o -> o.form),
            PortalGenTrigger.codec.fieldOf("trigger").forGetter(o -> o.trigger)
        ).apply(instance, instance.stable(CustomPortalGeneration::new));
    });
    
    public static SimpleRegistry<Codec<CustomPortalGeneration>> schemaRegistry = Util.make(() -> {
        SimpleRegistry<Codec<CustomPortalGeneration>> registry = new SimpleRegistry<>(
            schemaRegistryKey, Lifecycle.stable()
        );
        Registry.register(
            registry, new Identifier("imm_ptl:v1"), codecV1
        );
        return registry;
    });
    
    public static final Codec<CustomPortalGeneration> codec = schemaRegistry.dispatchStable(
        "schema_version", e -> codecV1, Function.identity()
    );
    
    public final List<RegistryKey<World>> fromDimensions;
    public final RegistryKey<World> toDimension;
    public final int spaceRatioFrom;
    public final int spaceRatioTo;
    public final boolean twoWay;
    public final PortalGenForm form;
    public final PortalGenTrigger trigger;
    
    public CustomPortalGeneration(
        List<RegistryKey<World>> fromDimensions, RegistryKey<World> toDimension,
        int spaceRatioFrom, int spaceRatioTo, boolean twoWay,
        PortalGenForm form, PortalGenTrigger trigger
    ) {
        this.fromDimensions = fromDimensions;
        this.toDimension = toDimension;
        this.spaceRatioFrom = spaceRatioFrom;
        this.spaceRatioTo = spaceRatioTo;
        this.twoWay = twoWay;
        this.form = form;
        this.trigger = trigger;
    }
}
