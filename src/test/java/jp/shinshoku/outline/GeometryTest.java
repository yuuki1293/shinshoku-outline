// SPDX-License-Identifier: MIT
package jp.shinshoku.outline;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class GeometryTest {
    private static int checks;
    private static void check(boolean condition,String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        double[] xs;
        try (var reader=new InputStreamReader(Objects.requireNonNull(GeometryTest.class.getResourceAsStream("/lanes.json")),StandardCharsets.UTF_8)) {
            var rows=JsonParser.parseReader(reader).getAsJsonArray();
            xs=new double[rows.size()];
            for (int i=0;i<xs.length;i++) xs[i]=rows.get(i).getAsJsonObject().get("x").getAsDouble();
        }
        check(xs.length==273,"273 Excel lanes");
        check(xs[0]==-1487.5 && xs[272]==1504.5,"Excel endpoints");
        for (int i=0;i<xs.length;i++) {
            check(Geometry.nearestLane(xs[i],xs)==i,"select exact lane");
            var b=Geometry.bounds(xs[i],71,-10,10);
            check(b.maxX()-b.minX()==11 && b.maxY()-b.minY()==10,"11 x 10 geometry");
            check((b.minX()+b.maxX())/2.0==xs[i],"block center alignment including negatives");
            if (i>0) check(Geometry.bounds(xs[i-1],71,0,0).maxX()==b.minX(),"no gaps/overlaps");
        }
        check(Geometry.bounds(-1487.5,71,0,0).minX()==-1493,"negative coordinate must floor");
        check(Geometry.bounds(-1487.5,71,0,0).maxX()==-1482,"exclusive far edge");
        check(Geometry.layer(70)==61 && Geometry.layer(71)==71 && Geometry.layer(80.99)==71
                && Geometry.layer(81)==81,"containing positive layer and shared boundaries");
        check(Geometry.layer(-10)==-19 && Geometry.layer(-9)==-9,"containing negative layer");
        for (int i=0;i<xs.length;i++) {
            double left=Math.floor(xs[i])-5;
            check(Geometry.containingLane(left,xs)==i,"left boundary belongs to this lane");
            check(Geometry.containingLane(left+10.999,xs)==i,"right interior belongs to this lane");
            check(Geometry.containingLane(left+11,xs)==(i+1<xs.length?i+1:-1),"right boundary belongs to next lane");
        }
        check(Geometry.containingLane(-1493.001,xs)==-1 && Geometry.containingLane(1510,xs)==-1,"no containing lane outside Excel bounds");
        for (double y=-70;y<=330;y+=.25) {
            int base=Geometry.layer(y);
            check(base<=y && y<base+10 && Geometry.validLayer(base),"selected Y range contains actual feet");
        }
        check(Geometry.validLayer(-59) && Geometry.validLayer(1) && !Geometry.validLayer(0),"Y congruence");
        check(Geometry.nearestLane(-100000,xs)==0 && Geometry.nearestLane(100000,xs)==272,"outside table clamp");
        for (int y=-59;y<=301;y+=10) check(Geometry.layer(y)==y,"all world layers");
        var outer=Geometry.corridor(-1487.5,71,11,10,.1,32);
        check(outer.equals(Geometry.corridor(-1487.5,71,11,10,15.99,32)),"movement inside a chunk must not move the world grid");
        var moved=Geometry.corridor(-1487.5,71,11,10,16.01,32);
        for (var line:outer) {
            if (line.from().z()>=-16 && line.to().z()<=48)
                check(moved.contains(line),"shared sections remain at exact world coordinates after chunk crossing");
            check(line.from().z()%16==0 && line.to().z()%16==0,"Z grid is world aligned");
        }
        var inner=Geometry.corridor(-1487.5,71,1,2,-.1,32);
        check(inner.equals(Geometry.corridor(-1487.5,71,1,2,-15.99,32)),"negative Z chunk alignment");
        for (var line:inner) for (var point:new Geometry.Point[]{line.from(),line.to()}) {
            check(point.x()==-1488 || point.x()==-1487,"1-block standing width");
            check(point.y()==71 || point.y()==73,"2-block standing height");
        }
        var worldPoint=new Geometry.Point(-1488,73,29999984);
        var camA=new Geometry.Point(-1487.25,72.62,29999982.75);
        var relative=Geometry.relative(worldPoint,camA);
        check(relative.equals(new Geometry.Point(-.75,.37999999999999545,1.25)),"camera subtraction before float conversion at world edge");
        var camB=new Geometry.Point(-1490,80,29999980);
        var relativeB=Geometry.relative(worldPoint,camB);
        check(relativeB.x()+camB.x()==worldPoint.x() && relativeB.y()+camB.y()==worldPoint.y()
                && relativeB.z()+camB.z()==worldPoint.z(),"camera movement preserves world location");
        MiningMode special=MiningMode.ELEVEN_CHIMEGU;
        check(!special.containsHeight(-59.001) && special.containsHeight(-59),"special mode minimum Y");
        check(special.base(-59)==-59 && special.height(-59)==10 && special.base(-49.001)==-59,"bottom layer is ten high");
        check(special.base(-49)==-49 && special.base(-38.001)==-49 && special.height(-49)==11,"first eleven-high layer");
        check(special.base(-38)==-38 && special.base(-27)==-27,"eleven layer boundaries");
        check(special.adjacent(-59,1)==-49 && special.adjacent(-49,-1)==-59
                && special.adjacent(-49,1)==-38 && special.adjacent(-38,-1)==-49
                && special.adjacent(-59,-1)==-59,"navigation across exceptional bottom layer");
        check(!special.validBase(-60) && !special.validBase(-48) && special.validBase(-59)
                && special.validBase(-49) && special.validBase(-38),"manual Y validation");
        for (MiningMode mode:MiningMode.values()) {
            for (double feet=-59;feet<320;feet+=.125) {
                int base=mode.base(feet), height=mode.height(base);
                check(base<=feet && feet<base+height && mode.validBase(base),"mode selects containing layer");
                check(mode.base(base+height)==base+height,"layers join without gap or overlap");
                if (mode==MiningMode.TEN) check(base==Geometry.layer(feet) && height==10,"legacy mode unchanged");
            }
            var gson=new com.google.gson.Gson();
            check(gson.fromJson(gson.toJson(mode),MiningMode.class)==mode,"mode serializes and restores");
        }
        var upper=Geometry.corridor(-1487.5,-49,11,special.height(-49),0,32);
        check(special.standingY(-59)==-59,"bottom ten-high layer keeps standing Y");
        check(special.standingY(-49)==-48 && special.standingY(-38)==-37,"eleven-high standing Y raised one block");
        check(MiningMode.TEN.standingY(71)==71 && MiningMode.TEN.standingY(-49)==-49,"ten mode standing Y unchanged");
        for (var segment:Geometry.corridor(-1487.5,special.standingY(-49),1,2,0,32))
            for (var point:new Geometry.Point[]{segment.from(),segment.to()})
                check(point.y()==-48 || point.y()==-46,"raised standing outline stays two blocks tall");
        for (var segment:upper) for (var point:new Geometry.Point[]{segment.from(),segment.to()})
            check(point.y()==-49 || point.y()==-38,"eleven-high rendered outline endpoints");
        var shifted=Geometry.shiftedCenters(xs,100.2);
        check(shifted[0]==100.5 && shifted[272]==3092.5,"custom origin shifts entire table");
        check(Geometry.blockCenter(-.1)==-.5 && Geometry.blockCenter(100.9)==100.5,"current block center for both signs");
        for (int i=0;i<shifted.length;i++) {
            check(Geometry.containingLane(shifted[i],shifted)==i,"automatic selection uses shifted grid");
            if (i>0) check(shifted[i]-shifted[i-1]==11,"custom origin preserves spacing");
        }
        check(java.util.Arrays.equals(xs,Geometry.shiftedCenters(xs,Geometry.DEFAULT_BASE_X)),"reset restores all Excel coordinates exactly");
        check(!Geometry.validBaseX(Double.NaN) && !Geometry.validBaseX(Double.POSITIVE_INFINITY)
                && !Geometry.validBaseX(30000000) && Geometry.validBaseX(-1487.5),"origin validation");
        for (double anchor:new double[]{-1487.5,100.5,-.5}) {
            for (int index:new int[]{-1000000,-274,-1,0,1,273,1000000}) {
                double center=Geometry.gridCenter(anchor,index);
                check(Geometry.gridIndex(center,anchor)==index,"unbounded positive and negative grid indices");
                check(Geometry.gridIndex(center-5.5,anchor)==index,"inclusive left edge");
                check(Geometry.gridIndex(center+5.499,anchor)==index,"right interior");
                check(Geometry.gridIndex(center+5.5,anchor)==index+1,"exclusive right edge");
                check(Geometry.gridCenter(anchor,index+1)-center==11,"spacing stays eleven beyond old endpoints");
            }
        }
        for (int i=0;i<xs.length;i++) check(Geometry.gridCenter(Geometry.DEFAULT_BASE_X,i)==xs[i],"default grid preserves original coordinates");
        System.out.println("Geometry tests passed: "+checks+" checks");
    }
}
