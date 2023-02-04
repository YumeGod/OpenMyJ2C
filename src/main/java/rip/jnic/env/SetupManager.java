package rip.jnic.env;

import rip.jnic.helpers.ProcessHelper;
import rip.jnic.utils.FileUtils;
import rip.jnic.utils.Zipper;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;

public class SetupManager
{
    private static String OS;
    
    public static void init() {
        final String platformTypeName = getPlatformTypeName();
        String fileName = null;
        String dirName = null;
        if (platformTypeName != null && !"".equals(platformTypeName)) {
            if (isLinux()) {
                fileName = "zig-linux-" + platformTypeName + "-0.9.1.tar.xz";
                dirName = "zig-linux-" + platformTypeName + "-0.9.1";
            }
            else if (isMacOS()) {
                fileName = "zig-macos-" + platformTypeName + "-0.9.1.tar.xz";
                dirName = "zig-macos-" + platformTypeName + "-0.9.1";
            }
            else if (isWindows()) {
                fileName = "zig-windows-" + platformTypeName + "-0.9.1.zip";
                dirName = "zig-windows-" + platformTypeName + "-0.9.1";
            }
            downloadZigCompiler(fileName, dirName);
            return;
        }
        System.out.println("Current OS not supported");
    }
    
    private static String getPlatformTypeName() {
        final String lowerCase;
        final String platform = lowerCase = System.getProperty("os.arch").toLowerCase();
        String platformTypeName = null;
        switch (lowerCase) {
            case "x86_64":
            case "amd64": {
                platformTypeName = "x86_64";
                break;
            }
            case "aarch64": {
                platformTypeName = "aarch64";
                break;
            }
            case "x86": {
                platformTypeName = "i386";
                break;
            }
            default: {
                platformTypeName = "";
                break;
            }
        }
        return platformTypeName;
    }
    
    public static boolean isLinux() {
        return SetupManager.OS.indexOf("linux") >= 0;
    }
    
    public static boolean isMacOS() {
        return SetupManager.OS.indexOf("mac") >= 0 && SetupManager.OS.indexOf("os") > 0;
    }
    
    public static boolean isWindows() {
        return SetupManager.OS.indexOf("windows") >= 0;
    }
    
    public static void downloadZigCompiler(final String fileName, final String dirName) {
        if (Files.exists(Paths.get(dirName))) {
            System.out.println("Found compiler: " + dirName);
            return;
        }

        try {
            final String currentDir = System.getProperty("user.dir");
//            if (Files.exists(Paths.get(currentDir + File.separator + dirName, new String[0]), new LinkOption[0])) {
//                final String compilePath = currentDir + File.separator + dirName + File.separator + "zig" + (isWindows() ? ".exe" : "");
//                if (Files.exists(Paths.get(compilePath, new String[0]), new LinkOption[0])) {
//                    final ProcessHelper.ProcessResult compileRunresult = ProcessHelper.run(Paths.get(currentDir + File.separator + dirName, new String[0]), 160000L, Arrays.asList(compilePath, "version"));
//                    System.out.println("\nzig version:" + compileRunresult.stdout);
//                    if (compileRunresult.stdout.contains("0.9.1")) {
//                        System.out.println("Installing compile tool:" + currentDir + File.separator + dirName);
//                        return;
//                    }
//                }
//                FileUtils.clearDirectory(currentDir + File.separator + dirName);
//            }

            System.out.println("Downloading compile tool:");
            System.out.println("Downloading from: https://ziglang.org/download/0.9.1/" + fileName);
            final InputStream in = new URL("https://ziglang.org/download/0.9.1/" + fileName).openStream();
            Files.copy(in, Paths.get(currentDir + File.separator + fileName, new String[0]), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Download success");
            unzipFile(currentDir, fileName, currentDir);
            deleteFile(currentDir, fileName + ".temp");
            deleteFile(currentDir, fileName);
            System.out.println("Install success");
            if (!isWindows()) {
                final String compilePath2 = currentDir + File.separator + dirName + File.separator + "zig";
                ProcessHelper.run(Paths.get(currentDir, new String[0]), 160000L, Arrays.asList("chmod", "777", compilePath2));
                System.out.println("Execute permission set to 777");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void deleteFile(final String path, final String file) {
        new File(path + File.separator + file).delete();
    }
    
    public static void unzipFile(final String path, final String file, final String destination) {
        try {
            Zipper.extract(Paths.get(path + File.separator + file, new String[0]), Paths.get(destination, new String[0]));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static String getZigGlobalCacheDirectory(final boolean clear) {
        final String platformTypeName = getPlatformTypeName();
        String dirName = null;
        if (platformTypeName != null && !"".equals(platformTypeName)) {
            if (isLinux()) {
                dirName = "zig-linux-" + platformTypeName + "-0.9.1";
            }
            else if (isMacOS()) {
                dirName = "zig-macos-" + platformTypeName + "-0.9.1";
            }
            else if (isWindows()) {
                dirName = "zig-windows-" + platformTypeName + "-0.9.1";
            }
        }
        final String currentDir = System.getProperty("user.dir");
        if (Files.exists(Paths.get(currentDir + File.separator + dirName, new String[0]), new LinkOption[0])) {
            final String compilePath = currentDir + File.separator + dirName + File.separator + "zig" + (isWindows() ? ".exe" : "");
            if (Files.exists(Paths.get(compilePath, new String[0]), new LinkOption[0])) {
                try {
                    final ProcessHelper.ProcessResult compileRunresult = ProcessHelper.run(Paths.get(currentDir + File.separator + dirName, new String[0]), 160000L, Arrays.asList(compilePath, "env"));
                    final ObjectMapper mapper = new ObjectMapper();
                    final Map<String, String> map = mapper.readValue(compileRunresult.stdout, Map.class);
                    if (clear) {
                        FileUtils.clearDirectory(map.get("global_cache_dir"));
                    }
                    return map.get("global_cache_dir");
                }catch (Throwable e){
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Failed to get zig temp directory");
        return "";
    }
    
    static {
        SetupManager.OS = System.getProperty("os.name").toLowerCase();
    }
}
