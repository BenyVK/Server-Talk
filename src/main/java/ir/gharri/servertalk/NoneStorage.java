package ir.gharri.servertalk;

import java.util.ArrayList;
import java.util.List;

public class NoneStorage implements TextStorage {
    @Override public void init() {}
    @Override public void save(StoredText3D text) {}
    @Override public void update(StoredText3D text) {}
    @Override public void delete(String id) {}
    @Override public List<StoredText3D> loadAll() { return new ArrayList<>(); }
    @Override public void close() {}
}