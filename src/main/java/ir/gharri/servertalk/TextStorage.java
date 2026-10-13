package ir.gharri.servertalk;

import java.util.List;

public interface TextStorage {
    void init() throws Exception;
    void save(StoredText3D text) throws Exception;
    void update(StoredText3D text) throws Exception;
    void delete(String id) throws Exception;
    List<StoredText3D> loadAll() throws Exception;
    void close();
}