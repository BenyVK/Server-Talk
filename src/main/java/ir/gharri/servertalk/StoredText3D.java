package ir.gharri.servertalk;

import org.bukkit.Location;

public class StoredText3D {
    public String id;
    public String text;
    public String world;
    public double x, y, z;
    public float size;
    public int bgR, bgG, bgB, bgA;
    public boolean shadow;
    public boolean seeThrough;
    public String billboard;
    public String owner;

    public StoredText3D() {}

    public static StoredText3D fromLocation(String id, String text, Location loc,
                                            float size, int r, int g, int b, int a,
                                            boolean shadow, boolean seeThrough,
                                            String billboard, String owner) {
        StoredText3D s = new StoredText3D();
        s.id = id;
        s.text = text;
        s.world = loc.getWorld().getName();
        s.x = loc.getX();
        s.y = loc.getY();
        s.z = loc.getZ();
        s.size = size;
        s.bgR = r;
        s.bgG = g;
        s.bgB = b;
        s.bgA = a;
        s.shadow = shadow;
        s.seeThrough = seeThrough;
        s.billboard = billboard;
        s.owner = owner;
        return s;
    }
}