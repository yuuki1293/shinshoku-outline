// SPDX-License-Identifier: MIT
package jp.shinshoku.outline;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

public final class OutlineScreen extends Screen {
    private TextFieldWidget xField, yField;
    private String error = "";
    private int top;
    public OutlineScreen() { super(new LiteralText("侵食大会アウトライン")); }
    @Override protected void init() {
        top = Math.max(20, (height - 220)/2);
        int left = width/2-140;
        xField = new TextFieldWidget(textRenderer,left+74,top+26,100,20,new LiteralText("X座標"));
        yField = new TextFieldWidget(textRenderer,left+74,top+52,100,20,new LiteralText("段の下端Y"));
        xField.setMaxLength(12); yField.setMaxLength(6);
        xField.setText(Double.toString(OutlineClient.selectedCenterX()));
        yField.setText(Integer.toString(OutlineClient.selectedY));
        addDrawableChild(xField); addDrawableChild(yField);
        addDrawableChild(new ButtonWidget(left+180,top+26,46,20,new LiteralText("前列"),b -> move(-1)));
        addDrawableChild(new ButtonWidget(left+230,top+26,46,20,new LiteralText("次列"),b -> move(1)));
        addDrawableChild(new ButtonWidget(left+180,top+52,46,20,new LiteralText("下段"),b -> shift(-1)));
        addDrawableChild(new ButtonWidget(left+230,top+52,46,20,new LiteralText("上段"),b -> shift(1)));
        addDrawableChild(new ButtonWidget(left,top+80,136,20,new LiteralText("入力したX・Yで固定"),b -> apply()));
        addDrawableChild(new ButtonWidget(left+144,top+80,136,20,new LiteralText("足元を含む枠を自動選択"),b -> {
            OutlineClient.automatic=true; close();
        }));
        addDrawableChild(new ButtonWidget(left,top+108,136,20,flag("壁越し表示",OutlineClient.config.throughWalls),b -> {
            OutlineClient.config.throughWalls=!OutlineClient.config.throughWalls;
            b.setMessage(flag("壁越し表示",OutlineClient.config.throughWalls));
        }));
        addDrawableChild(new ButtonWidget(left+144,top+108,136,20,flag("座標表示",OutlineClient.config.hud),b -> {
            OutlineClient.config.hud=!OutlineClient.config.hud;
            b.setMessage(flag("座標表示",OutlineClient.config.hud));
        }));
        addDrawableChild(new ButtonWidget(left,top+136,280,20,modeLabel(),b -> {
            OutlineClient.config.mode=OutlineClient.config.mode==MiningMode.TEN ? MiningMode.ELEVEN_CHIMEGU : MiningMode.TEN;
            OutlineClient.selectedY=OutlineClient.config.mode.base(OutlineClient.selectedY);
            OutlineClient.updateSelection(client);
            try { yField.setText(Integer.toString(OutlineClient.config.mode.base(Integer.parseInt(yField.getText())))); }
            catch (NumberFormatException e) { yField.setText(Integer.toString(OutlineClient.selectedY)); }
            error="";
            b.setMessage(modeLabel());
            OutlineClient.save(client);
        }));
        addDrawableChild(new ButtonWidget(left,top+160,280,20,distanceLabel(),b -> {
            int d=OutlineClient.config.guideDistance;
            OutlineClient.config.guideDistance=d<16?16:d<32?32:d<64?64:d<128?128:8;
            b.setMessage(distanceLabel());
        }));
        addDrawableChild(new ButtonWidget(left,top+200,136,20,new LiteralText("基準X設定"),b -> {
            OutlineClient.save(client);
            client.setScreen(new OriginScreen());
        }));
        addDrawableChild(new ButtonWidget(left+144,top+200,136,20,new LiteralText("閉じる"),b -> close()));
    }
    private LiteralText flag(String label, boolean enabled) { return new LiteralText(label+": "+(enabled?"ON":"OFF")); }
    private LiteralText distanceLabel() { return new LiteralText("南北のガイド表示距離: 各"+OutlineClient.config.guideDistance+"ブロック"); }
    private LiteralText modeLabel() { return new LiteralText("モード: "+OutlineClient.config.mode.label()+"（クリックで切替）"); }
    private void move(int delta) {
        try {
            int i=Geometry.gridIndex(Double.parseDouble(xField.getText()),OutlineClient.config.baseX);
            xField.setText(Double.toString(Geometry.gridCenter(OutlineClient.config.baseX,i+delta)));
        } catch (NumberFormatException e) { error="Xに数値を入力してください"; }
    }
    private void shift(int delta) {
        try { yField.setText(Integer.toString(OutlineClient.config.mode.adjacent(Integer.parseInt(yField.getText()),delta))); }
        catch (NumberFormatException e) { error="Yに整数を入力してください"; }
    }
    private void apply() {
        try {
            double x=Double.parseDouble(xField.getText());
            int y=Integer.parseInt(yField.getText());
            int i=Geometry.gridIndex(x,OutlineClient.config.baseX);
            if (!Double.isFinite(x) || Math.abs(Geometry.gridCenter(OutlineClient.config.baseX,i)-x)>0.0001) {
                error="Xは現在の基準から11刻みの立ち位置です"; return;
            }
            if (!OutlineClient.config.mode.validBase(y)) {
                error=OutlineClient.config.mode==MiningMode.TEN ? "Yは10n+1（例: 61、71、81）です" : "Yは−59、−49、−38、−27…です";
                return;
            }
            if (client.world==null || y<client.world.getBottomY() || y>client.world.getTopY()-OutlineClient.config.mode.height(y)) {
                error="このワールドの高さの範囲外です"; return;
            }
            OutlineClient.selectedLane=i; OutlineClient.selectedY=y; OutlineClient.automatic=false;
            close();
        } catch (NumberFormatException e) { error="Xは数値、Yは整数で入力してください"; }
    }
    @Override public void tick() { xField.tick(); yField.tick(); }
    @Override public void close() { OutlineClient.save(client); client.setScreen(null); }
    @Override public boolean shouldPause() { return false; }
    @Override public void render(MatrixStack matrices,int mouseX,int mouseY,float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices,textRenderer,title,width/2,top-18,0xFFFFFF);
        int left=width/2-140;
        textRenderer.drawWithShadow(matrices,"X立ち位置",left,top+32,0xFFFFFF);
        textRenderer.drawWithShadow(matrices,"段の下端Y",left,top+58,0xFFFFFF);
        drawCenteredText(matrices,textRenderer,"水色: 採掘範囲 / 黄色: 立ち位置1×2",width/2,top+6,0xAADDFF);
        drawCenteredText(matrices,textRenderer,error,width/2,top+186,0xFF8888);
        super.render(matrices,mouseX,mouseY,delta);
    }
}
