package ir.gharri.servertalk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JsonStorage implements TextStorage {

    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, StoredText3D> cache = new ConcurrentHashMap<>();

    public JsonStorage(File file) {
        this.file = file;
    }

    @Override
    public void init() throws Exception {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        if (!file.exists()) {
            file.createNewFile();
            writeAll();
        }
        cache.clear();
        if (file.length() > 0) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<List<StoredText3D>>() {}.getType();
                List<StoredText3D> list = gson.fromJson(reader, type);
                if (list != null) for (StoredText3D s : list) cache.put(s.id, s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override public void save(StoredText3D text) { cache.put(text.id, text); writeAll(); }
    @Override public void update(StoredText3D text) { cache.put(text.id, text); writeAll(); }
    @Override public void delete(String id) { cache.remove(id); writeAll(); }
    @Override public List<StoredText3D> loadAll() { return new ArrayList<>(cache.values()); }
    @Override public void close() {}

    private void writeAll() {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(new ArrayList<>(cache.values()), writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}