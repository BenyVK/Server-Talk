package ir.gharri.servertalk;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySqlStorage implements TextStorage {

    private final String host, database, username, password, table;
    private final int port;
    private final boolean useSSL;
    private Connection connection;

    public MySqlStorage(String host, int port, String database, String username,
                        String password, String table, boolean useSSL, int poolSize) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.table = table;
        this.useSSL = useSSL;
    }

    private synchronized Connection con() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=" + useSSL
                    + "&allowPublicKeyRetrieval=true&characterEncoding=utf8&useUnicode=true";
            connection = DriverManager.getConnection(url, username, password);
        }
        return connection;
    }

    @Override
    public void init() throws Exception {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (ClassNotFoundException ignored) {}

        try (Statement st = con().createStatement()) {
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS `" + table + "` (" +
                            "id VARCHAR(64) PRIMARY KEY," +
                            "text TEXT," +
                            "world VARCHAR(64)," +
                            "x DOUBLE, y DOUBLE, z DOUBLE," +
                            "size FLOAT," +
                            "bg_r INT, bg_g INT, bg_b INT, bg_a INT," +
                            "shadow BOOLEAN, seethrough BOOLEAN," +
                            "billboard VARCHAR(16)," +
                            "owner VARCHAR(32)" +
                            ") CHARACTER SET utf8mb4"
            );
        }
    }

    @Override
    public synchronized void save(StoredText3D t) throws Exception {
        String sql = "INSERT INTO `" + table + "` (id,text,world,x,y,z,size,bg_r,bg_g,bg_b,bg_a,shadow,seethrough,billboard,owner) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE text=VALUES(text),world=VALUES(world),x=VALUES(x),y=VALUES(y),z=VALUES(z)," +
                "size=VALUES(size),bg_r=VALUES(bg_r),bg_g=VALUES(bg_g),bg_b=VALUES(bg_b),bg_a=VALUES(bg_a)," +
                "shadow=VALUES(shadow),seethrough=VALUES(seethrough),billboard=VALUES(billboard),owner=VALUES(owner)";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setString(1, t.id);
            ps.setString(2, t.text);
            ps.setString(3, t.world);
            ps.setDouble(4, t.x);
            ps.setDouble(5, t.y);
            ps.setDouble(6, t.z);
            ps.setFloat(7, t.size);
            ps.setInt(8, t.bgR);
            ps.setInt(9, t.bgG);
            ps.setInt(10, t.bgB);
            ps.setInt(11, t.bgA);
            ps.setBoolean(12, t.shadow);
            ps.setBoolean(13, t.seeThrough);
            ps.setString(14, t.billboard);
            ps.setString(15, t.owner);
            ps.executeUpdate();
        }
    }

    @Override
    public void update(StoredText3D t) throws Exception { save(t); }

    @Override
    public synchronized void delete(String id) throws Exception {
        try (PreparedStatement ps = con().prepareStatement("DELETE FROM `" + table + "` WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public synchronized List<StoredText3D> loadAll() throws Exception {
        List<StoredText3D> list = new ArrayList<>();
        try (Statement st = con().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM `" + table + "`")) {
            while (rs.next()) {
                StoredText3D t = new StoredText3D();
                t.id = rs.getString("id");
                t.text = rs.getString("text");
                t.world = rs.getString("world");
                t.x = rs.getDouble("x");
                t.y = rs.getDouble("y");
                t.z = rs.getDouble("z");
                t.size = rs.getFloat("size");
                t.bgR = rs.getInt("bg_r");
                t.bgG = rs.getInt("bg_g");
                t.bgB = rs.getInt("bg_b");
                t.bgA = rs.getInt("bg_a");
                t.shadow = rs.getBoolean("shadow");
                t.seeThrough = rs.getBoolean("seethrough");
                t.billboard = rs.getString("billboard");
                t.owner = rs.getString("owner");
                list.add(t);
            }
        }
        return list;
    }

    @Override
    public synchronized void close() {
        try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
    }
}