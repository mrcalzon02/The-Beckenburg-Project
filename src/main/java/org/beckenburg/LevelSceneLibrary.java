package org.beckenburg;

import org.beckenburg.editor.InMemoryRuntimeScene;
import org.beckenburg.editor.RuntimeSceneDocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Safe file-backed workspace for independent authored level scenes. */
public final class LevelSceneLibrary {
    private LevelSceneLibrary() {}
    public static Path directory() {
        return Path.of(System.getProperty("user.home"), ".beckenburg", "editor", "scenes")
            .toAbsolutePath().normalize();
    }
    public static String validateId(String id) {
        if(id == null || !id.matches("[A-Za-z][A-Za-z0-9_-]{0,47}"))
            throw new IllegalArgumentException("Level ID: 1–48 letters, digits, _ or -, starting with a letter");
        return id;
    }
    public static Path path(String id) {
        return directory().resolve(validateId(id) + ".bscn");
    }
    /** New means a genuinely empty scene, never an implicit copy of the Castle. */
    public static InMemoryRuntimeScene blank(String id) throws IOException {
        Path target = path(id);
        if(Files.exists(target)) throw new IOException("Level already exists: " + target);
        return new InMemoryRuntimeScene(validateId(id));
    }
    public static List<Path> savedScenes() throws IOException {
        if(!Files.isDirectory(directory())) return List.of();
        List<Path> found = new ArrayList<>();
        try(var paths=Files.list(directory())) {
            paths.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".bscn"))
                .sorted().forEach(found::add);
        }
        return List.copyOf(found);
    }
    public static InMemoryRuntimeScene open(Path file) throws IOException {
        return RuntimeSceneDocument.load(file.toAbsolutePath().normalize());
    }
}
