// SPDX-License-Identifier: MIT
package jp.shinshoku.outline;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

public final class OriginScreen extends Screen {
    private TextFieldWidget baseField;
    private String message="";
    private int top;
    public OriginScreen() { super(new LiteralText("基準X設定")); }
    @Override protected void init() {
        top=Math.max(24,(height-190)/2);
        int left=width/2-140;
        baseField=new TextFieldWidget(textRenderer,left+64,top+40,136,20,new LiteralText("基準X（A1）"));
        baseField.setMaxLength(24);
        baseField.setText(Double.toString(OutlineClient.config.baseX));
        addDrawableChild(baseField);
        addDrawableChild(new ButtonWidget(left+208,top+40,72,20,new LiteralText("適用"),b -> {
            try { apply(Double.parseDouble(baseField.getText())); }
            catch (NumberFormatException e) { message="X座標を数値で入力してください"; }
        }));
        addDrawableChild(new ButtonWidget(left,top+70,280,20,new LiteralText("現在のX座標を基準にする"),b -> {
            if (client.player!=null) apply(client.player.getX());
        }));
        addDrawableChild(new ButtonWidget(left,top+96,280,20,new LiteralText("元の値にリセット（−1487.5）"),b -> apply(Geometry.DEFAULT_BASE_X)));
        addDrawableChild(new ButtonWidget(left,top+158,280,20,new LiteralText("戻る"),b -> close()));
    }
    private void apply(double x) {
        if (!Geometry.validBaseX(x)) { message="全列がワールド内に収まるXを入力してください"; return; }
        OutlineClient.setBaseX(x);
        baseField.setText(Double.toString(OutlineClient.config.baseX));
        message="基準Xを適用しました";
    }
    @Override public void tick() { baseField.tick(); }
    @Override public boolean shouldPause() { return false; }
    @Override public void close() { client.setScreen(new OutlineScreen()); }
    @Override public void render(MatrixStack matrices,int mouseX,int mouseY,float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices,textRenderer,title,width/2,top-16,0xFFFFFF);
        drawCenteredText(matrices,textRenderer,"A1の立ち位置を基準に273列全体を移動",width/2,top+4,0xAADDFF);
        drawCenteredText(matrices,textRenderer,"ブロック中央（小数 .5）に合わせます",width/2,top+18,0xBBBBBB);
        textRenderer.drawWithShadow(matrices,"基準X",width/2-140,top+46,0xFFFFFF);
        drawCenteredText(matrices,textRenderer,"適用中: "+OutlineClient.config.baseX,width/2,top+126,0xFFFFFF);
        drawCenteredText(matrices,textRenderer,message,width/2,top+140,0xFFCC55);
        super.render(matrices,mouseX,mouseY,delta);
    }
}
