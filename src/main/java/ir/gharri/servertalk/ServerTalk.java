package ir.gharri.servertalk;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerTalk extends JavaPlugin implements CommandExecutor, Listener {

    private static final int BLANK_LINES = 150;
    private static final String TEXT_TAG = "servertalk_text3d";
    private static final String GROUP_PREFIX = "servertalk_group_";

    private FileConfiguration configVk;
    private File configVkFile;
    private TextStorage storage;
    private String currentMode = "JSON";

    private final Map<String, String> idToGroup = new ConcurrentHashMap<>();
    private final Map<String, String> groupToId = new ConcurrentHashMap<>();
    private final Map<String, StoredText3D> idToData = new ConcurrentHashMap<>();
    private final Map<String, UUID> idToEntity = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        configVkFile = new File(getDataFolder(), "configvk.yml");
        if (!configVkFile.exists()) {
            saveResource("configvk.yml", false);
        }
        configVk = YamlConfiguration.loadConfiguration(configVkFile);

        String mode = configVk.getString("StorageMode", "JSON").toUpperCase();
        try {
            storage = buildStorageFromConfig(mode);
            storage.init();
            currentMode = mode;
            getLogger().info("Storage initialized in " + mode + " mode.");
        } catch (Throwable e) {
            getLogger().severe("Failed to init storage! Falling back to NONE. " + e);
            storage = new NoneStorage();
            currentMode = "NONE";
        }

        purgeAllTexts();
        getServer().getPluginManager().registerEvents(this, this);
        loadAllTexts();

        registerCommand("clearchat");
        registerCommand("clearmassagechat");
        registerCommand("text3d");
        registerCommand("edittext3d");
        registerCommand("deletetext3d");
        registerCommand("servertalk");

        getLogger().info("Server Talk enabled!");
    }

    @Override
    public void onDisable() {
        purgeAllTexts();
        idToEntity.clear();
        if (storage != null) storage.close();
        getLogger().info("Server Talk disabled!");
    }

    private void registerCommand(String name) {
        var cmd = getCommand(name);
        if (cmd == null) {
            getLogger().severe("Command '" + name + "' not found in plugin.yml!");
            return;
        }
        cmd.setExecutor(this);
    }

    private void loadAllTexts() {
        try {
            List<StoredText3D> list = storage.loadAll();
            for (StoredText3D s : list) {
                registerAndSpawn(s);
            }
            getLogger().info("Loaded " + list.size() + " 3D text(s).");
        } catch (Throwable e) {
            getLogger().severe("Failed to load 3D texts: " + e);
        }
    }

    private void registerAndSpawn(StoredText3D s) {
        String groupTag = GROUP_PREFIX + s.id;
        idToGroup.put(s.id, groupTag);
        groupToId.put(groupTag, s.id);
        idToData.put(s.id, s);
        ensureSpawned(s);
    }

    private void ensureSpawned(StoredText3D s) {
        World world = Bukkit.getWorld(s.world);
        if (world == null) {
            getLogger().warning("World '" + s.world + "' not found for text " + s.id);
            return;
        }

        UUID existing = idToEntity.get(s.id);
        if (existing != null) {
            Entity e = Bukkit.getEntity(existing);
            if (e != null && e.isValid()) return;
        }

        Location loc = new Location(world, s.x, s.y, s.z);
        loc.getChunk().load();

        Component text = LegacyComponentSerializer.legacyAmpersand().deserialize(s.text);
        String groupTag = GROUP_PREFIX + s.id;

        TextDisplay td = world.spawn(loc, TextDisplay.class, d -> {
            d.setPersistent(false);
            d.text(text);
            try {
                d.setBillboard(Display.Billboard.valueOf(s.billboard));
            } catch (Exception ex) {
                d.setBillboard(Display.Billboard.CENTER);
            }
            d.setDefaultBackground(false);
            d.setBackgroundColor(Color.fromARGB(s.bgA, s.bgR, s.bgG, s.bgB));
            d.setSeeThrough(s.seeThrough);
            d.setShadowed(s.shadow);
            d.setLineWidth(300);
            d.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f(),
                    new Vector3f(s.size, s.size, s.size),
                    new AxisAngle4f()
            ));
            d.addScoreboardTag(TEXT_TAG);
            d.addScoreboardTag(groupTag);
        });
        idToEntity.put(s.id, td.getUniqueId());
    }

    private void purgeAllTexts() {
        for (World w : Bukkit.getWorlds()) {
            for (TextDisplay td : w.getEntitiesByClass(TextDisplay.class)) {
                if (td.getScoreboardTags().contains(TEXT_TAG)) td.remove();
            }
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        final int cx = event.getChunk().getX();
        final int cz = event.getChunk().getZ();
        final String worldName = event.getWorld().getName();
        final List<Entity> loaded = new ArrayList<>(event.getEntities());

        Bukkit.getScheduler().runTask(this, () -> {
            Set<UUID> live = new HashSet<>(idToEntity.values());
            for (Entity en : loaded) {
                if (en instanceof TextDisplay td
                        && td.getScoreboardTags().contains(TEXT_TAG)
                        && !live.contains(td.getUniqueId())) {
                    td.remove();
                }
            }
            for (StoredText3D s : new ArrayList<>(idToData.values())) {
                if (!s.world.equals(worldName)) continue;
                if (((int) Math.floor(s.x) >> 4) == cx && ((int) Math.floor(s.z) >> 4) == cz) {
                    ensureSpawned(s);
                }
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();

        if (name.equals("clearchat")) {
            if (!sender.hasPermission("servertalk.clearchat")) {
                sender.sendMessage(Component.text("You don't have permission to do this.", NamedTextColor.RED));
                return true;
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                clear(p);
                p.sendMessage(Component.text("Chat was cleared by " + sender.getName() + ".", NamedTextColor.GREEN));
            }
            return true;
        }

        if (name.equals("clearmassagechat")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }
            clear(player);
            player.sendMessage(Component.text("Your chat was cleared.", NamedTextColor.GREEN));
            return true;
        }

        if (name.equals("text3d")) return handleText3D(sender, args);
        if (name.equals("edittext3d")) return handleEditText3D(sender, args);
        if (name.equals("deletetext3d")) return handleDeleteText3D(sender, args);
        if (name.equals("servertalk")) return handleServerTalk(sender, args);

        return false;
    }

    private boolean handleText3D(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("servertalk.text3d")) {
            player.sendMessage(Component.text("You don't have permission to do this.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(Component.text(
                    "Usage: /text3d [size] <text with | for new line>  |  /text3d remove",
                    NamedTextColor.YELLOW));
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            String groupTag = findNearestGroup(player);
            if (groupTag == null) {
                player.sendMessage(Component.text("No 3D text found nearby.", NamedTextColor.RED));
                return true;
            }
            String id = groupToId.get(groupTag);
            if (id == null) removeGroup(groupTag);
            else deleteById(id);
            player.sendMessage(Component.text("3D text removed.", NamedTextColor.GREEN));
            return true;
        }

        float size = 5f;
        int start = 0;
        try {
            size = Float.parseFloat(args[0]);
            start = 1;
        } catch (NumberFormatException ignored) {}
        if (start >= args.length) {
            player.sendMessage(Component.text("Please write a text.", NamedTextColor.RED));
            return true;
        }
        if (size < 0.5f) size = 0.5f;
        if (size > 50f) size = 50f;

        String raw = String.join(" ", Arrays.copyOfRange(args, start, args.length)).replace("|", "\n");

        Location eye = player.getEyeLocation();
        Location base = eye.clone().add(eye.getDirection().setY(0).normalize().multiply(5));
        base.setY(eye.getY() + 1.0);

        String id = UUID.randomUUID().toString().substring(0, 8);

        StoredText3D data = StoredText3D.fromLocation(
                id, raw, base, size,
                0, 0, 0, 120,
                true, false, "CENTER", player.getName()
        );
        registerAndSpawn(data);
        try {
            storage.save(data);
        } catch (Throwable e) {
            player.sendMessage(Component.text("Warning: could not save to storage: " + e.getMessage(), NamedTextColor.RED));
            getLogger().severe("Save failed: " + e);
        }

        player.sendMessage(Component.text("3D text created. ID: ", NamedTextColor.GREEN)
                .append(Component.text(id, NamedTextColor.AQUA)));
        return true;
    }

    private boolean handleDeleteText3D(CommandSender sender, String[] args) {
        if (!sender.hasPermission("servertalk.deletetext3d")) {
            sender.sendMessage(Component.text("You don't have permission to do this.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("=== /deletetext3d (/dt3d) ===", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/dt3d <id>      delete the text with this ID", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/dt3d nearest   delete the nearest text", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/dt3d all       delete ALL texts (admin only)", NamedTextColor.YELLOW));
            return true;
        }

        String a = args[0];

        if (a.equalsIgnoreCase("all")) {
            if (!sender.hasPermission("servertalk.admin")) {
                sender.sendMessage(Component.text("Only admins can delete all texts.", NamedTextColor.RED));
                return true;
            }
            int n = idToData.size();
            for (String id : new ArrayList<>(idToData.keySet())) deleteById(id);
            purgeAllTexts();
            sender.sendMessage(Component.text("Deleted " + n + " 3D text(s).", NamedTextColor.GREEN));
            return true;
        }

        if (a.equalsIgnoreCase("nearest")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Only players can use this.");
                return true;
            }
            String tag = findNearestGroup(p);
            if (tag == null) {
                p.sendMessage(Component.text("No 3D text found nearby.", NamedTextColor.RED));
                return true;
            }
            String id = groupToId.get(tag);
            if (id == null) removeGroup(tag);
            else deleteById(id);
            p.sendMessage(Component.text("3D text deleted.", NamedTextColor.GREEN));
            return true;
        }

        if (idToData.containsKey(a)) {
            deleteById(a);
            sender.sendMessage(Component.text("3D text " + a + " deleted.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("No 3D text found with ID: " + a, NamedTextColor.RED));
        }
        return true;
    }

    private void deleteById(String id) {
        idToData.remove(id);
        String tag = idToGroup.remove(id);
        if (tag != null) groupToId.remove(tag);
        else tag = GROUP_PREFIX + id;
        removeGroup(tag);
        idToEntity.remove(id);
        try {
            storage.delete(id);
        } catch (Throwable e) {
            getLogger().severe("Delete failed for " + id + ": " + e);
        }
    }

    private boolean handleEditText3D(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("servertalk.edittext3d")) {
            player.sendMessage(Component.text("You don't have permission to do this.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sendEditHelp(player);
            return true;
        }

        if (idToData.containsKey(args[0])) {
            StoredText3D data = idToData.get(args[0]);
            if (data == null) {
                player.sendMessage(Component.text("Text data not found for ID: " + args[0], NamedTextColor.RED));
                return true;
            }
            String[] rest = Arrays.copyOfRange(args, 1, args.length);
            if (rest.length == 0) {
                player.sendMessage(Component.text("Usage: /et3d <id> <action> ...", NamedTextColor.YELLOW));
                return true;
            }
            return applyEdit(player, data, rest);
        }

        if (args[0].equalsIgnoreCase("id")) {
            if (args.length < 3) {
                player.sendMessage(Component.text("Usage: /et3d id <id> <action> ...", NamedTextColor.YELLOW));
                return true;
            }
            String id = args[1];
            StoredText3D data = idToData.get(id);
            if (data == null) {
                player.sendMessage(Component.text("No 3D text found with ID: " + id, NamedTextColor.RED));
                return true;
            }
            String[] rest = Arrays.copyOfRange(args, 2, args.length);
            return applyEdit(player, data, rest);
        }

        String groupTag = findNearestGroup(player);
        if (groupTag == null) {
            player.sendMessage(Component.text("No 3D text found nearby.", NamedTextColor.RED));
            player.sendMessage(Component.text("Tip: use /et3d id <id> <action> if you know the ID.", NamedTextColor.GRAY));
            return true;
        }
        String id = groupToId.get(groupTag);
        if (id == null) {
            player.sendMessage(Component.text("Internal error: group without ID.", NamedTextColor.RED));
            return true;
        }
        StoredText3D data = idToData.get(id);
        if (data == null) {
            player.sendMessage(Component.text("Text data not found for ID: " + id, NamedTextColor.RED));
            return true;
        }
        return applyEdit(player, data, args);
    }

    private boolean applyEdit(Player player, StoredText3D data, String[] args) {
        if (args.length == 0) {
            sendEditHelp(player);
            return true;
        }
        String sub = args[0].toLowerCase();
        String groupTag = idToGroup.get(data.id);
        World dataWorld = Bukkit.getWorld(data.world);
        List<TextDisplay> layers = findLayersByGroup(dataWorld != null ? dataWorld : player.getWorld(), groupTag);

        switch (sub) {
            case "pos": {
                if (args.length < 4) {
                    player.sendMessage(Component.text("Usage: /et3d id <id> pos <x> <y> <z>", NamedTextColor.YELLOW));
                    return true;
                }
                double x, y, z;
                try {
                    x = Double.parseDouble(args[1]);
                    y = Double.parseDouble(args[2]);
                    z = Double.parseDouble(args[3]);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Numbers are invalid.", NamedTextColor.RED));
                    return true;
                }
                double dx = x - data.x, dy = y - data.y, dz = z - data.z;
                for (TextDisplay td : layers) td.teleport(td.getLocation().add(dx, dy, dz));
                data.x = x; data.y = y; data.z = z;
                persist(data);
                player.sendMessage(Component.text("Position updated.", NamedTextColor.GREEN));
                return true;
            }

            case "move": {
                if (args.length < 4) {
                    player.sendMessage(Component.text("Usage: /et3d id <id> move <dx> <dy> <dz>", NamedTextColor.YELLOW));
                    return true;
                }
                double dx, dy, dz;
                try {
                    dx = Double.parseDouble(args[1]);
                    dy = Double.parseDouble(args[2]);
                    dz = Double.parseDouble(args[3]);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Numbers are invalid.", NamedTextColor.RED));
                    return true;
                }
                for (TextDisplay td : layers) td.teleport(td.getLocation().add(dx, dy, dz));
                data.x += dx; data.y += dy; data.z += dz;
                persist(data);
                player.sendMessage(Component.text("Moved.", NamedTextColor.GREEN));
                return true;
            }

            case "color": {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /et3d id <id> color <&c&lText>", NamedTextColor.YELLOW));
                    return true;
                }
                String raw = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                Component comp = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
                layers.forEach(td -> td.text(comp));
                data.text = raw;
                persist(data);
                player.sendMessage(Component.text("Text color updated.", NamedTextColor.GREEN));
                return true;
            }

            case "text": {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /et3d id <id> text <new text | for new line>", NamedTextColor.YELLOW));
                    return true;
                }
                String raw = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                Component comp = LegacyComponentSerializer.legacyAmpersand().deserialize(raw.replace("|", "\n"));
                layers.forEach(td -> td.text(comp));
                data.text = raw.replace("|", "\n");
                persist(data);
                player.sendMessage(Component.text("Text updated.", NamedTextColor.GREEN));
                return true;
            }

            case "bg": {
                if (args.length < 5) {
                    player.sendMessage(Component.text("Usage: /et3d id <id> bg <r> <g> <b> <a>", NamedTextColor.YELLOW));
                    return true;
                }
                int r, g, b, a;
                try {
                    r = clamp(Integer.parseInt(args[1]), 0, 255);
                    g = clamp(Integer.parseInt(args[2]), 0, 255);
                    b = clamp(Integer.parseInt(args[3]), 0, 255);
                    a = clamp(Integer.parseInt(args[4]), 0, 255);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Numbers are invalid.", NamedTextColor.RED));
                    return true;
                }
                Color color = Color.fromARGB(a, r, g, b);
                layers.forEach(td -> { td.setDefaultBackground(false); td.setBackgroundColor(color); });
                data.bgR = r; data.bgG = g; data.bgB = b; data.bgA = a;
                persist(data);
                player.sendMessage(Component.text("Background color updated.", NamedTextColor.GREEN));
                return true;
            }

            case "nobg": {
                layers.forEach(td -> { td.setDefaultBackground(false); td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); });
                data.bgR = 0; data.bgG = 0; data.bgB = 0; data.bgA = 0;
                persist(data);
                player.sendMessage(Component.text("Background removed.", NamedTextColor.GREEN));
                return true;
            }

            case "size": {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /et3d id <id> size <value>", NamedTextColor.YELLOW));
                    return true;
                }
                float s;
                try { s = Float.parseFloat(args[1]); }
                catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Invalid number.", NamedTextColor.RED));
                    return true;
                }
                if (s < 0.5f) s = 0.5f;
                if (s > 50f) s = 50f;
                final float finalS = s;
                layers.forEach(td -> td.setTransformation(new Transformation(
                        new Vector3f(), new AxisAngle4f(),
                        new Vector3f(finalS, finalS, finalS), new AxisAngle4f()
                )));
                data.size = s;
                persist(data);
                player.sendMessage(Component.text("Size updated.", NamedTextColor.GREEN));
                return true;
            }

            case "shadow": {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /et3d id <id> shadow <true|false>", NamedTextColor.YELLOW));
                    return true;
                }
                boolean val = Boolean.parseBoolean(args[1]);
                layers.forEach(td -> td.setShadowed(val));
                data.shadow = val;
                persist(data);
                player.sendMessage(Component.text("Shadow set to " + val + ".", NamedTextColor.GREEN));
                return true;
            }

            case "seethrough": {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /et3d id <id> seethrough <true|false>", NamedTextColor.YELLOW));
                    return true;
                }
                boolean val = Boolean.parseBoolean(args[1]);
                layers.forEach(td -> td.setSeeThrough(val));
                data.seeThrough = val;
                persist(data);
                player.sendMessage(Component.text("SeeThrough set to " + val + ".", NamedTextColor.GREEN));
                return true;
            }

            case "billboard": {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /et3d id <id> billboard <fixed|vertical|horizontal|center>", NamedTextColor.YELLOW));
                    return true;
                }
                Display.Billboard bb;
                try { bb = Display.Billboard.valueOf(args[1].toUpperCase()); }
                catch (IllegalArgumentException e) {
                    player.sendMessage(Component.text("Invalid billboard type.", NamedTextColor.RED));
                    return true;
                }
                layers.forEach(td -> td.setBillboard(bb));
                data.billboard = bb.name();
                persist(data);
                player.sendMessage(Component.text("Billboard set to " + bb + ".", NamedTextColor.GREEN));
                return true;
            }

            case "info": {
                player.sendMessage(Component.text("=== 3D Text Info ===", NamedTextColor.GOLD));
                player.sendMessage(Component.text("ID: " + data.id, NamedTextColor.AQUA));
                player.sendMessage(Component.text("Pos: " + fmt(data.x) + ", " + fmt(data.y) + ", " + fmt(data.z), NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Size: " + data.size, NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Billboard: " + data.billboard, NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Shadow: " + data.shadow + " | SeeThrough: " + data.seeThrough, NamedTextColor.YELLOW));
                player.sendMessage(Component.text("BG: " + data.bgR + "," + data.bgG + "," + data.bgB + "," + data.bgA, NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Owner: " + data.owner, NamedTextColor.YELLOW));
                return true;
            }

            case "delete":
            case "remove": {
                deleteById(data.id);
                player.sendMessage(Component.text("3D text deleted.", NamedTextColor.GREEN));
                return true;
            }

            default:
                sendEditHelp(player);
                return true;
        }
    }

    private boolean handleServerTalk(CommandSender sender, String[] args) {
        if (!sender.hasPermission("servertalk.admin")) {
            sender.sendMessage(Component.text("You don't have permission to do this.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("=== ServerTalk Admin ===", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/st storage <none|json|mysql|status>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/st reload", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/st list", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/dt3d <id|nearest|all>", NamedTextColor.YELLOW));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "storage": {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /st storage <none|json|mysql|status>", NamedTextColor.YELLOW));
                    return true;
                }
                String mode = args[1].toLowerCase();

                if (mode.equals("status")) {
                    sender.sendMessage(Component.text("Current storage mode: ", NamedTextColor.GOLD)
                            .append(Component.text(currentMode, NamedTextColor.AQUA)));
                    sender.sendMessage(Component.text("Loaded texts in memory: " + idToData.size(), NamedTextColor.YELLOW));
                    return true;
                }

                if (!mode.equals("none") && !mode.equals("json") && !mode.equals("mysql")) {
                    sender.sendMessage(Component.text("Invalid mode. Use: none | json | mysql", NamedTextColor.RED));
                    return true;
                }

                switchStorage(sender, mode.toUpperCase());
                return true;
            }

            case "reload": {
                try {
                    configVk = YamlConfiguration.loadConfiguration(configVkFile);
                    String m = configVk.getString("StorageMode", "JSON").toUpperCase();
                    if (!m.equals("NONE") && !m.equals("JSON") && !m.equals("MYSQL")) {
                        sender.sendMessage(Component.text("Invalid StorageMode in configvk.yml: " + m, NamedTextColor.RED));
                        return true;
                    }
                    if (!m.equals(currentMode)) {
                        switchStorage(sender, m);
                    } else {
                        sender.sendMessage(Component.text("configvk.yml reloaded. Storage: " + currentMode, NamedTextColor.GREEN));
                    }
                } catch (Throwable e) {
                    sender.sendMessage(Component.text("Reload failed: " + e.getMessage(), NamedTextColor.RED));
                }
                return true;
            }

            case "list": {
                sender.sendMessage(Component.text("=== Loaded 3D Texts (" + idToData.size() + ") ===", NamedTextColor.GOLD));
                for (StoredText3D data : idToData.values()) {
                    String preview = data.text.length() > 30 ? data.text.substring(0, 30) + "..." : data.text;
                    preview = preview.replace("\n", "\\n");
                    sender.sendMessage(Component.text(" • ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(data.id, NamedTextColor.AQUA))
                            .append(Component.text("  [" + data.world + " " +
                                    fmt(data.x) + "," + fmt(data.y) + "," + fmt(data.z) + "] ", NamedTextColor.GRAY))
                            .append(Component.text(preview, NamedTextColor.WHITE)));
                }
                return true;
            }

            default:
                sender.sendMessage(Component.text("Unknown subcommand. Use /st", NamedTextColor.RED));
                return true;
        }
    }

    private void switchStorage(CommandSender sender, String mode) {
        if (mode.equals(currentMode)) {
            sender.sendMessage(Component.text("Storage is already " + mode + ".", NamedTextColor.YELLOW));
            return;
        }

        TextStorage newStorage = null;
        try {
            TextStorage old = storage;
            newStorage = buildStorageFromConfig(mode);
            newStorage.init();

            int migrated = 0;
            for (StoredText3D data : new ArrayList<>(idToData.values())) {
                try {
                    newStorage.save(data);
                    migrated++;
                } catch (Throwable e) {
                    getLogger().warning("Failed to migrate text " + data.id + ": " + e);
                }
            }

            int imported = 0;
            if (!mode.equals("NONE")) {
                for (StoredText3D s : newStorage.loadAll()) {
                    if (!idToData.containsKey(s.id)) {
                        registerAndSpawn(s);
                        imported++;
                    }
                }
            }

            old.close();
            storage = newStorage;
            currentMode = mode;

            configVk.set("StorageMode", mode);
            configVk.save(configVkFile);

            sender.sendMessage(Component.text("Storage switched to " + mode + ". Copied " + migrated
                            + " text(s)" + (imported > 0 ? ", imported " + imported + " existing text(s)" : "") + ".",
                    NamedTextColor.GREEN));
            if (mode.equals("NONE")) {
                sender.sendMessage(Component.text("NONE mode: texts will disappear after a restart.", NamedTextColor.YELLOW));
            }
            getLogger().info("Storage switched to " + mode + " (copied " + migrated + ", imported " + imported + ").");
        } catch (Throwable e) {
            if (newStorage != null) {
                try { newStorage.close(); } catch (Throwable ignored) {}
            }
            sender.sendMessage(Component.text("Failed to switch storage: " + e.getMessage(), NamedTextColor.RED));
            getLogger().severe("Storage switch failed: " + e);
        }
    }

    private TextStorage buildStorageFromConfig(String mode) throws Exception {
        switch (mode) {
            case "MYSQL":
                return new MySqlStorage(
                        configVk.getString("MySQL.Host", "localhost"),
                        configVk.getInt("MySQL.Port", 3306),
                        configVk.getString("MySQL.Database", "servertalk"),
                        configVk.getString("MySQL.Username", "root"),
                        configVk.getString("MySQL.Password", ""),
                        configVk.getString("MySQL.Table", "3d_texts"),
                        configVk.getBoolean("MySQL.UseSSL", false),
                        configVk.getInt("MySQL.PoolSize", 4)
                );
            case "NONE":
                return new NoneStorage();
            default:
                File jsonFile = new File(getDataFolder(),
                        configVk.getString("Json.FileName", "3dtexts.json"));
                return new JsonStorage(jsonFile);
        }
    }

    private void sendEditHelp(Player player) {
        player.sendMessage(Component.text("======== /edittext3d ========", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Click any line to put the command in your chat.", NamedTextColor.GRAY));
        player.sendMessage(Component.empty());

        helpLine(player, "List all texts (with IDs)", "/st list",
                "Shows all stored 3D texts and their IDs.");

        helpLine(player, "View text info", "/et3d info",
                "Shows position, background color, size, etc. (nearest text)");

        helpLine(player, "Set exact position", "/et3d id <id> pos ",
                "Example: /et3d id a1b2c3d4 pos 100.5 70 200.5");

        helpLine(player, "Move relatively", "/et3d id <id> move ",
                "Example: /et3d id a1b2c3d4 move 0 1 0");

        helpLine(player, "Change text", "/et3d id <id> text ",
                "Example: /et3d id a1b2c3d4 text &a&lNEW TEXT | &eSecond line");

        helpLine(player, "Change text color", "/et3d id <id> color ",
                "Example: /et3d id a1b2c3d4 color &c&lRed Bold Text");

        helpLine(player, "Set background color", "/et3d id <id> bg ",
                "Example: /et3d id a1b2c3d4 bg 0 0 0 200  (R G B A, 0-255)");

        helpLine(player, "Remove background", "/et3d id <id> nobg",
                "Makes the plate behind the text transparent.");

        helpLine(player, "Change size", "/et3d id <id> size ",
                "Example: /et3d id a1b2c3d4 size 8");

        helpLine(player, "Toggle shadow", "/et3d id <id> shadow ",
                "Example: /et3d id a1b2c3d4 shadow true");

        helpLine(player, "Toggle see-through", "/et3d id <id> seethrough ",
                "Example: /et3d id a1b2c3d4 seethrough false");

        helpLine(player, "Set billboard mode", "/et3d id <id> billboard ",
                "fixed | vertical | horizontal | center");

        helpLine(player, "Delete text", "/dt3d ",
                "/dt3d <id>  |  /dt3d nearest  |  /dt3d all (admin)");

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("Tip: you can also use /et3d <id> <action> (without 'id').", NamedTextColor.GRAY));
        player.sendMessage(Component.text("==============================", NamedTextColor.GOLD));
    }

    private void helpLine(Player player, String title, String command, String example) {
        Component line = Component.text(" • ", NamedTextColor.DARK_GRAY)
                .append(Component.text(title, NamedTextColor.YELLOW))
                .append(Component.text("  [" + command.trim() + "]", NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.suggestCommand(command))
                        .hoverEvent(HoverEvent.showText(
                                Component.text(example, NamedTextColor.GREEN)
                        )));
        player.sendMessage(line);
    }

    private void persist(StoredText3D data) {
        try {
            storage.update(data);
        } catch (Throwable e) {
            getLogger().severe("Save failed: " + e);
        }
    }

    private void removeGroup(String groupTag) {
        if (groupTag == null) return;
        for (World w : Bukkit.getWorlds()) {
            for (TextDisplay td : w.getEntitiesByClass(TextDisplay.class)) {
                if (td.getScoreboardTags().contains(groupTag)) td.remove();
            }
        }
    }

    private List<TextDisplay> findLayersByGroup(World world, String groupTag) {
        List<TextDisplay> list = new ArrayList<>();
        if (groupTag == null) return list;
        for (TextDisplay td : world.getEntitiesByClass(TextDisplay.class)) {
            if (td.getScoreboardTags().contains(groupTag)) list.add(td);
        }
        return list;
    }

    private String findNearestGroup(Player player) {
        String groupTag = null;
        double best = Double.MAX_VALUE;
        for (Entity e : player.getNearbyEntities(15, 15, 15)) {
            if (e instanceof TextDisplay td && td.getScoreboardTags().contains(TEXT_TAG)) {
                double d = td.getLocation().distanceSquared(player.getLocation());
                if (d < best) {
                    best = d;
                    for (String t : td.getScoreboardTags()) {
                        if (t.startsWith(GROUP_PREFIX)) groupTag = t;
                    }
                }
            }
        }
        return groupTag;
    }

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    private String fmt(double d) {
        return String.format("%.2f", d);
    }

    private void clear(Player player) {
        for (int i = 0; i < BLANK_LINES; i++) {
            player.sendMessage(Component.empty());
        }
    }
}