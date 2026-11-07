package com.ap4k.speedruntimer;

import com.ap4k.speedruntimer.client.TimerConfig;
import com.ap4k.speedruntimer.client.TimerEvents;
import com.ap4k.speedruntimer.client.TimerHudLayer;
import com.ap4k.speedruntimer.client.TimerKeys;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@Mod(value = SpeedrunTimer.MODID, dist = Dist.CLIENT)
public class SpeedrunTimer {

    public static final String MODID = "speedruntimer";

    public SpeedrunTimer(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, TimerConfig.SPEC);
        modBus.addListener(RegisterKeyMappingsEvent.class, TimerKeys::register);
        modBus.addListener(RegisterGuiLayersEvent.class, SpeedrunTimer::onRegisterLayers);
        TimerEvents.init();
    }

    private static void onRegisterLayers(RegisterGuiLayersEvent event) {
        // над хотбаром, чтобы не драться за место с броней и голодом
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_LEVEL, id("timer"), TimerHudLayer::render);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
