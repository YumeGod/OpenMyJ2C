package rip.jnic.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Zipper
{
    public static void extract(final Path archive, final Path target) throws IOException {
        final String name = archive.toFile().getName();
        if (name.contains("zip")) {
            unzip(archive, target);
        }
        else if (name.contains("tar.xz")) {
            unTarXZ(archive, target);
        }
        else if (name.contains("tar.xz")) {
            unTarXZ(archive, target);
        }
        else {
            if (!name.contains("tar")) {
                throw new RuntimeException("unsupported content type ");
            }
            unTar(archive, target);
        }
    }
    
    public static void unzip(final Path zip, final Path target) throws IOException {
        try (final ZipInputStream zin = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zip, new OpenOption[0])))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                extractEntry(target, zin, entry.getName(), entry.isDirectory());
            }
        }
    }
    
    public static void unXzip(final Path xzip, final Path target) throws IOException {
        try (final XZCompressorInputStream xin = new XZCompressorInputStream(new BufferedInputStream(Files.newInputStream(xzip, new OpenOption[0])))) {
            Files.copy(xin, target, new CopyOption[0]);
        }
    }
    
    public static void unTar(final Path tar, final Path target) throws IOException {
        try (final TarArchiveInputStream tin = new TarArchiveInputStream(new BufferedInputStream(Files.newInputStream(tar, new OpenOption[0])))) {
            TarArchiveEntry entry;
            while ((entry = tin.getNextTarEntry()) != null) {
                extractEntry(target, tin, entry.getName(), entry.isDirectory());
            }
        }
    }
    
    public static void unTarXZ(final Path tar, final Path target) throws IOException {
        try (final XZCompressorInputStream xzcis = new XZCompressorInputStream(new BufferedInputStream(Files.newInputStream(tar, new OpenOption[0])));
             final TarArchiveInputStream tin = new TarArchiveInputStream(xzcis, 1024)) {
            TarArchiveEntry entry;
            while ((entry = tin.getNextTarEntry()) != null) {
                extractEntry(target, tin, entry.getName(), entry.isDirectory());
            }
        }
    }
    
    private static void extractEntry(final Path target, final InputStream in, final String entryName, final boolean isDirectory) throws IOException {
        final Path entryPath = target.resolve(entryName);
        if (isDirectory) {
            Files.createDirectories(entryPath, (FileAttribute<?>[])new FileAttribute[0]);
        }
        else {
            final Path dir = entryPath.getParent();
            Files.createDirectories(dir, (FileAttribute<?>[])new FileAttribute[0]);
            Files.copy(in, entryPath, new CopyOption[0]);
        }
    }
}
