// SPDX-License-Identifier: MIT
package jp.shinshoku.outline;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import java.util.Locale;

public final class OutlineClient implements ClientModInitializer {
    public static OutlineConfig config;
    public static boolean automatic = true;
    public static int selectedLane;
    public static int selectedY = 71;
    private static Object lastWorld;
    private static boolean selectOnJoin = true;
    private static boolean selectionAvailable = true;
    private static KeyBinding toggle, settings, lock;

    @Override public void onInitializeClient() {
        config = OutlineConfig.load();
        toggle = key("toggle", GLFW.GLFW_KEY_O);
        settings = key("settings", GLFW.GLFW_KEY_P);
        lock = key("lock", GLFW.GLFW_KEY_G);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != lastWorld) { selectOnJoin = true; lastWorld = client.world; }
            if (client.player == null || client.world == null) return;
            if (selectOnJoin) {
                automatic = true;
                updateSelection(client);
                selectOnJoin = false;
            }
            updateSelection(client);
            while (toggle.wasPressed()) { config.enabled = !config.enabled; save(client); }
            while (lock.wasPressed()) {
                if (automatic && !selectionAvailable) continue;
                automatic = !automatic;
                client.player.sendMessage(new LiteralText(automatic ? "侵食ガイド: 自動選択" : "侵食ガイド: X・Yを固定"), true);
            }
            while (settings.wasPressed()) client.setScreen(new OutlineScreen());
        });
        WorldRenderEvents.LAST.register(OutlineClient::render);
        HudRenderCallback.EVENT.register((matrices, delta) -> {
            MinecraftClient c = MinecraftClient.getInstance();
            if (!visible(c) || !config.hud) return;
            updateSelection(c);
            if (automatic && !selectionAvailable) {
                c.textRenderer.drawWithShadow(matrices,"侵食: 足元は最低Yより下です",8,8,0xFFCC55);
                return;
            }
            int standingY = config.mode.standingY(selectedY);
            double centerX=selectedCenterX();
            int x = (int)Math.floor(centerX);
            c.textRenderer.drawWithShadow(matrices, String.format(Locale.ROOT,
                    "侵食  X %.1f  足Y %d  [%s / %s]", centerX, standingY,
                    automatic ? "自動" : "固定", config.mode.label()), 8, 8, 0x55FFFF);
            c.textRenderer.drawWithShadow(matrices, String.format(Locale.ROOT,
                    "掘る範囲 X %d～%d / Y %d～%d / 南北方向", x-5, x+5, selectedY, selectedY+config.mode.height(selectedY)-1), 8, 20, 0xFFFFFF);
            c.textRenderer.drawWithShadow(matrices, toggle.getBoundKeyLocalizedText().getString()+": 表示  "
                    +settings.getBoundKeyLocalizedText().getString()+": 設定  "
                    +lock.getBoundKeyLocalizedText().getString()+": 固定/自動", 8, 32, 0xDDDDDD);
            String differenceFormat="立ち位置との差: X %+."+config.differenceDecimals+"f / Y %+."+config.differenceDecimals+"f";
            c.textRenderer.drawWithShadow(matrices,
                    String.format(Locale.ROOT, differenceFormat,
                            c.player.getX()-centerX, c.player.getY()-standingY), 8, 44, 0xFFCC55);
        });
    }

    public static double selectedCenterX() {
        return Geometry.gridCenter(config.baseX,selectedLane);
    }
    public static void setBaseX(double x) {
        if (!Geometry.validBaseX(x)) throw new IllegalArgumentException("Invalid base X");
        config.baseX=Geometry.blockCenter(x);
        MinecraftClient c=MinecraftClient.getInstance();
        updateSelection(c);
        save(c);
    }
    private static KeyBinding key(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.shinshoku_outline."+name,
                InputUtil.Type.KEYSYM, key, "category.shinshoku_outline"));
    }
    public static void save(MinecraftClient c) {
        if (!config.save() && c.player != null)
            c.player.sendMessage(new LiteralText("侵食ガイド: 設定を保存できませんでした（今回の起動中は有効）"), false);
    }
    public static void updateSelection(MinecraftClient c) {
        if (automatic && c.player != null && c.world != null) {
            int containing = Geometry.gridIndex(c.player.getX(), config.baseX);
            selectionAvailable = config.mode.containsHeight(c.player.getY());
            if (selectionAvailable) selectedLane = containing;
            selectedY = config.mode.base(c.player.getY());
        }
    }
    private static boolean visible(MinecraftClient c) {
        return config.enabled && c.world != null && c.player != null && !c.options.hudHidden;
    }
    private static void render(WorldRenderContext ctx) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (!visible(c)) return;
        updateSelection(c);
        if (automatic && !selectionAvailable) return;
        double centerX = selectedCenterX();
        // Only nearby guides are drawn; Z endpoints are display limits, not excavation boundaries.
        if (Math.abs(c.player.getX()-centerX) > 256 || Math.abs(c.player.getY()-selectedY) > 256) return;
        Vec3d camera = ctx.camera().getPos();
        Geometry.Point origin = new Geometry.Point(camera.x,camera.y,camera.z);
        // LAST already has the view rotation in RenderSystem's model-view matrix.
        // Applying ctx.matrixStack() here would rotate everything a second time.
        // Submit camera-relative vertices directly, just like vanilla debug overlays.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        if (config.throughWalls) RenderSystem.disableDepthTest(); else RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesShader);
        RenderSystem.lineWidth(2.0f);
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        try {
            for (var segment : Geometry.corridor(centerX,selectedY,11,config.mode.height(selectedY),c.player.getZ(),config.guideDistance))
                line(b,segment,origin,.15f,.9f,1f,.8f);
            for (var segment : Geometry.corridor(centerX,config.mode.standingY(selectedY),1,2,c.player.getZ(),config.guideDistance))
                line(b,segment,origin,1f,.8f,.1f,1f);
            Tessellator.getInstance().draw();
        } finally {
            RenderSystem.lineWidth(1.0f);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.enableTexture();
            RenderSystem.disableBlend();
        }
    }
    private static void line(VertexConsumer b, Geometry.Segment segment, Geometry.Point camera,
                             float r,float g,float blue,float a) {
        var start=Geometry.relative(segment.from(),camera);
        var end=Geometry.relative(segment.to(),camera);
        float dx=(float)(end.x()-start.x()), dy=(float)(end.y()-start.y()), dz=(float)(end.z()-start.z());
        float len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        b.vertex(start.x(),start.y(),start.z()).color(r,g,blue,a).normal(dx/len,dy/len,dz/len).next();
        b.vertex(end.x(),end.y(),end.z()).color(r,g,blue,a).normal(dx/len,dy/len,dz/len).next();
    }
}
