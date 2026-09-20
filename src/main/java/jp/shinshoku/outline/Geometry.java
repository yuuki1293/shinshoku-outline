// SPDX-License-Identifier: MIT
package jp.shinshoku.outline;

/** All ranges use block boundaries; maximum coordinates are exclusive. */
public final class Geometry {
    public static final double DEFAULT_BASE_X = -1487.5;
    public static double blockCenter(double x) { return Math.floor(x)+0.5; }
    public static boolean validBaseX(double x) {
        return Double.isFinite(x) && blockCenter(x)-5.5>=-30000000
                && blockCenter(x)+272*11+5.5<=30000000;
    }
    public static double[] shiftedCenters(double[] original,double baseX) {
        double[] result=new double[original.length];
        for (int i=0;i<result.length;i++) result[i]=blockCenter(baseX)+(original[i]-DEFAULT_BASE_X);
        return result;
    }
    private Geometry() {}
    public static int nearestLane(double x, double[] lanes) {
        int best = 0;
        for (int i = 1; i < lanes.length; i++)
            if (Math.abs(lanes[i] - x) < Math.abs(lanes[best] - x)) best = i;
        return best;
    }
    public static int containingLane(double x, double[] lanes) {
        for (int i=0;i<lanes.length;i++) {
            int left=(int)Math.floor(lanes[i])-5;
            if (x>=left && x<left+11) return i;
        }
        return -1;
    }
    public static int layer(double feetY) { return (int)Math.floor((feetY - 1) / 10) * 10 + 1; }
    public static boolean validLayer(int y) { return Math.floorMod(y - 1, 10) == 0; }
    public static Bounds bounds(double centerX, int feetY, int zMin, int zMax) {
        int x = (int)Math.floor(centerX);
        return new Bounds(x - 5, feetY, zMin, x + 6, feetY + 10, zMax + 1);
    }
    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}

    public record Point(double x, double y, double z) {}
    public record Segment(Point from, Point to) {}
    public static Point relative(Point world, Point camera) {
        // Subtract as doubles before the renderer converts to float (far-world precision).
        return new Point(world.x-camera.x,world.y-camera.y,world.z-camera.z);
    }
    public static java.util.List<Segment> corridor(double centerX, int feetY, int width, int height,
                                                  double playerZ, int distance) {
        int chunkZ=(int)Math.floor(playerZ/16.0);
        int radius=(distance+15)/16;
        int start=(chunkZ-radius)*16, end=(chunkZ+radius+1)*16;
        double left=centerX-width/2.0, right=centerX+width/2.0;
        java.util.List<Segment> lines=new java.util.ArrayList<>();
        // All segments and cross-sections are anchored to world coordinates.
        // Only the set of visible 16-block sections changes while travelling north/south.
        for (int z=start;z<end;z+=16) {
            for (double x:new double[]{left,right}) for (int y:new int[]{feetY,feetY+height})
                lines.add(new Segment(new Point(x,y,z),new Point(x,y,z+16)));
        }
        for (int z=start;z<=end;z+=16) {
            lines.add(new Segment(new Point(left,feetY,z),new Point(right,feetY,z)));
            lines.add(new Segment(new Point(left,feetY+height,z),new Point(right,feetY+height,z)));
            lines.add(new Segment(new Point(left,feetY,z),new Point(left,feetY+height,z)));
            lines.add(new Segment(new Point(right,feetY,z),new Point(right,feetY+height,z)));
        }
        return java.util.List.copyOf(lines);
    }
}
