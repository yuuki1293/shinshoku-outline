// SPDX-License-Identifier: MIT
package jp.shinshoku.outline;

public enum MiningMode {
    TEN("10段"), ELEVEN_CHIMEGU("11段+ちーめぐ");

    private final String label;
    MiningMode(String label) { this.label=label; }
    public String label() { return label; }
    public boolean containsHeight(double feetY) { return this==TEN || feetY>=-59; }
    public int base(double feetY) {
        if (this==TEN) return Geometry.layer(feetY);
        if (feetY < -49) return -59;
        return -49+(int)Math.floor((feetY+49)/11)*11;
    }
    public int height(int baseY) { return this==TEN || baseY==-59 ? 10 : 11; }
    public int standingY(int baseY) { return baseY+(height(baseY)==11?1:0); }
    public boolean validBase(int y) {
        return this==TEN ? Geometry.validLayer(y) : y==-59 || (y>=-49 && Math.floorMod(y+49,11)==0);
    }
    public int adjacent(int y,int direction) {
        int current=base(y);
        if (direction>0) return current+height(current);
        if (this==ELEVEN_CHIMEGU && current<=-49) return -59;
        return current-(this==TEN?10:11);
    }
}
