package cz.zcu.kiv.imiger.plugin.jacc;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 */
public class TemporaryZipFileExtractor implements Closeable {

    private final Path tempDirectory;

    public TemporaryZipFileExtractor(byte[] zipFileData) throws IOException {
        tempDirectory = Files.createTempDirectory(getClass().getName());
        unzip(new ByteArrayInputStream(zipFileData), tempDirectory);
    }

    private static void unzip(InputStream is, Path targetDir) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(is)) {
            for (ZipEntry ze; (ze = zipIn.getNextEntry()) != null; ) {
                Path resolvedPath = targetDir.resolve(ze.getName());
                if (ze.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath.getParent());
                    Files.copy(zipIn, resolvedPath);
                }
            }
        }
    }

    public Path getTempDirectory() {
        return tempDirectory;
    }

    @Override
    public void close() throws IOException {
        deleteDirectory(tempDirectory);
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectory(entry);
                }
            }
        }
        Files.delete(path);
    }
}
