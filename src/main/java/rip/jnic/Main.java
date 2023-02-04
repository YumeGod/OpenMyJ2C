package rip.jnic;

import rip.jnic.env.LicenseManager;
import rip.jnic.env.SetupManager;
import rip.jnic.utils.StringUtils;
import rip.jnic.xml.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;


public class Main
{
    public static final String VERSION = "2022.1009.05";
    private static final char[] DIGITS;
    
    public static void main(final String[] args) throws IOException {
        System.out.println("                __  _____  __    _____   ______\n               /  |/  /\\ \\/ /   / /__ \\ / ____/\n              / /|_/ /  \\  /_  / /__/ // /     \n             / /  / /   / / /_/ // __// /___   \n            /_/  /_/   /_/\\____//____/\\____/\n\n    MuYang Java to C Bytecode Translator V2022.1009.05\n\n            Copyright (c) MYJ2C 2022-2024\n\n==========================================================\n");
        System.out.println("Checking authorization...");
        String key = null;
        final String path = System.getProperty("user.dir") + File.separator + "myj2c.licence";
        if (new File(path).exists()) {
            System.out.println("Reading authorization file...\n");
            final String value = LicenseManager.getValue("offline");
            Label_0512: {
                if (! StringUtils.equals(value, "true")) {
                    try {
                        final URL url = new URL("https://gitee.com/myj2c/myj2c/raw/master/code");
                        final HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                        if (200 != conn.getResponseCode()) {
                            System.out.println("Failed to request banned hwid");
                            return;
                        }
                        final InputStreamReader inputReader = new InputStreamReader(conn.getInputStream());
                        final BufferedReader bufferedReader = new BufferedReader(inputReader);
                        String temp;
                        while ((temp = bufferedReader.readLine()) != null) {
                            if (!temp.trim().equals("") && key.equals(temp.trim())) {
                                System.out.println("Your hwid has been banned...");
                                return;
                            }
                        }
                        bufferedReader.close();
                        inputReader.close();
                        break Label_0512;
                    }
                    catch (Exception e2) {
                        if (e2.getMessage().contains("PKIX path building failed")) {
                            System.out.println("Failed to request banned hwid, may caused by manually modified system time");
                        }
                        else {
                            System.out.println("Failed to request banned hwid, may caused by bad internet connection");
                        }
                        return;
                    }
                }
                System.out.println("You are currently using offline mode...");
            }
        }
        else {
            System.out.println("\nUnable to find authorization file...\n");
        }
        System.out.println("\nInitializing...");
        SetupManager.init();
        System.out.println("Initialize finished\n");
        System.exit(new CommandLine(new NativeObfuscatorRunner()).setCaseInsensitiveEnumValuesAllowed(true).execute(args));
    }
    
    private static int getLength(final Object array) {
        if (array == null) {
            return 0;
        }
        return Array.getLength(array);
    }
    
    private static byte[] digest(final byte[] input, final String algorithm, final byte[] salt, final int iterations) {
        try {
            final MessageDigest digest = MessageDigest.getInstance(algorithm);
            if (salt != null) {
                digest.update(salt);
            }
            byte[] result = digest.digest(input);
            for (int i = 1; i < iterations; ++i) {
                digest.reset();
                result = digest.digest(result);
            }
            return result;
        }
        catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static String encodeHex(final byte[] input) {
        final int l = input.length;
        final char[] out = new char[l << 1];
        int i = 0;
        int j = 0;
        while (i < l) {
            out[j++] = Main.DIGITS[(0xF0 & input[i]) >>> 4];
            out[j++] = Main.DIGITS[0xF & input[i]];
            ++i;
        }
        return new String(out);
    }
    
    static {
        DIGITS = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    }
    
    @CommandLine.Command(name = "MYJ2C-Bytecode-Translator", mixinStandardHelpOptions = true, version = { "MYJ2C Bytecode Translator 2022.1009.05" }, description = { "Translator .jar file into .c files and generates output .jar file" })
    private static class NativeObfuscatorRunner implements Callable<Integer>
    {
        @CommandLine.Parameters(index = "0", description = { "Jar file to transpile" })
        private File jarFile;
        @CommandLine.Parameters(index = "1", description = { "Output directory" })
        private String outputDirectory;
        @CommandLine.Option(names = { "-c", "--config" }, defaultValue = "config.xml", description = { "Config file" })
        private File config;
        @CommandLine.Option(names = { "-l", "--libraries" }, description = { "Directory for dependent libraries" })
        private File librariesDirectory;
        @CommandLine.Option(names = { "--plain-lib-name" }, description = { "Plain library name for LoaderPlain" })
        private String libraryName;
        @CommandLine.Option(names = { "-a", "--annotations" }, description = { "Use annotations to ignore/include native obfuscation" })
        private boolean useAnnotations;
        
        @Override
        public Integer call() throws Exception {
            System.out.println("Reading configuration file:" + this.config.toPath());
            final StringBuilder stringBuilder = new StringBuilder();
            if (Files.exists(this.config.toPath(), new LinkOption[0])) {
                try (final BufferedReader br = Files.newBufferedReader(this.config.toPath())) {
                    String str;
                    while ((str = br.readLine()) != null) {
                        stringBuilder.append(str);
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                final ObjectMapper objectMapper = new XmlMapper();
                final Config configInfo;
                configInfo = objectMapper.readValue(stringBuilder.toString(), Config.class);
                final List<Path> libs = new ArrayList<Path>();
                if (this.librariesDirectory != null) {
                    Files.walk(this.librariesDirectory.toPath(), FileVisitOption.FOLLOW_LINKS).filter(f -> f.toString().endsWith(".jar") || f.toString().endsWith(".zip")).forEach(libs::add);
                }
                if (new File(this.outputDirectory).isDirectory()) {
                    final File outFile = new File(this.outputDirectory, this.jarFile.getName());
                    if (outFile.exists()) {
                        outFile.renameTo(new File(this.outputDirectory, this.jarFile.getName() + ".BACKUP"));
                    }
                }
                else {
                    final File outFile = new File(this.outputDirectory);
                    if (outFile.exists()) {
                        outFile.renameTo(new File(this.outputDirectory + ".BACKUP"));
                    }
                }
                new NativeObfuscator().process(this.jarFile.toPath(), Paths.get(this.outputDirectory, new String[0]), configInfo, libs, this.libraryName, this.useAnnotations);
                return 0;
            }
            final Path path = Files.createFile(this.config.toPath(), (FileAttribute<?>[])new FileAttribute[0]);
            stringBuilder.append("<myj2c>\n\t<targets>\n\t\t<target>WINDOWS_X86_64</target>\n\t\t<!--<target>WINDOWS_AARCH64</target>\n\t\t<target>MACOS_X86_64</target>\n\t\t<target>MACOS_AARCH64</target>-->\n\t\t<target>LINUX_X86_64</target>\n\t\t<!--<target>LINUX_AARCH64</target>-->\n\t</targets>\n\t<include>\n\t\t<!-- match支持 Ant 风格的路径匹配 ? 匹配一个字符, * 匹配多个字符, ** 匹配多层路径 -->\n\t\t<match className=\"**\" />\n\t\t<!--<match className=\"cn/myj2c/web/**\" />-->\n\t\t<!--<match className=\"cn.myj2c.service.**\" />-->\n\t</include>\n\t<exclude>\n\t\t<!--<match className=\"cn/myj2c/Main\" methodName=\"main\" methodDesc=\"(\\[Ljava/lang/String;)V\"/>-->\n\t\t<!--<match className=\"cn.myj2c.test.**\" />-->\n\t</exclude>\n</myj2c>\n");
            Files.write(path, stringBuilder.toString().getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
            System.out.println("Unable to read configuration file, automatically generated default config");
            System.out.println("MyJ2C will now quit, please run again after editing the configuration file");
            return 0;
        }
    }
}
