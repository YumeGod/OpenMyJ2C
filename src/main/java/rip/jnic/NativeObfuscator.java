// Decompiled with: CFR 0.152
// Class Version: 8
package rip.jnic;

import rip.jnic.asm.ClassMetadataReader;
import rip.jnic.asm.SafeClassWriter;
import rip.jnic.cache.CachedClassInfo;
import rip.jnic.cache.CachedFieldInfo;
import rip.jnic.cache.CachedMethodInfo;
import rip.jnic.cache.ClassNodeCache;
import rip.jnic.cache.FieldNodeCache;
import rip.jnic.cache.MethodNodeCache;
import rip.jnic.cache.NodeCache;
import rip.jnic.env.LicenseManager;
import rip.jnic.env.SetupManager;
import rip.jnic.helpers.ProcessHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import rip.jnic.utils.DataTool;
import rip.jnic.utils.FileUtils;
import rip.jnic.utils.StringUtils;
import rip.jnic.xml.Config;
import rip.jnic.xml.Match;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class NativeObfuscator {
    private final Snippets snippets;
    private InterfaceStaticClassProvider staticClassProvider;
    private final MethodProcessor methodProcessor;
    private final NodeCache<String> cachedStrings;
    private final ClassNodeCache cachedClasses;
    private final MethodNodeCache cachedMethods;
    private final FieldNodeCache cachedFields;
    private Map<String, String> classMethodNameMap = new HashMap<String, String>();
    private Map<String, String> noInitClassMap = new HashMap<String, String>();
    private StringBuilder nativeMethods;
    private BootstrapMethodsPool bootstrapMethodsPool;
    private int currentClassId;
    private int methodIndex;
    private String nativeDir;
    private static String separator = File.separator;

    public NativeObfuscator() {
        this.snippets = new Snippets();
        this.cachedStrings = new NodeCache("(cstrings[%d])");
        this.cachedClasses = new ClassNodeCache("(cclasses[%d])");
        this.cachedMethods = new MethodNodeCache("(cmethods[%d])", this.cachedClasses);
        this.cachedFields = new FieldNodeCache("(cfields[%d])", this.cachedClasses);
        this.methodProcessor = new MethodProcessor(this);
    }

    public void process(Path inputJarPath, Path output, Config config, List<Path> inputLibs, String plainLibName, boolean useAnnotations) throws IOException {
        Path outputDir;
        ArrayList<Path> libs = new ArrayList<Path>(inputLibs);
        libs.add(inputJarPath);
        ArrayList<String> whiteList = new ArrayList<String>();
        if (config.getIncludes() != null) {
            for (Match match : config.getIncludes()) {
                StringBuilder stringBuilder = new StringBuilder();
                if (StringUtils.isNotEmpty(match.getClassName())) {
                    stringBuilder.append(match.getClassName().replaceAll("\\.", "/"));
                }
                if (StringUtils.isNotEmpty(match.getMethodName())) {
                    stringBuilder.append("#" + match.getMethodName());
                    if (StringUtils.isNotEmpty(match.getMethodDesc())) {
                        stringBuilder.append("!" + match.getMethodDesc());
                    }
                } else if (StringUtils.isNotEmpty(match.getMethodDesc())) {
                    stringBuilder.append("#**!" + match.getMethodDesc());
                }
                whiteList.add(stringBuilder.toString());
            }
        }
        ArrayList<String> blackList = new ArrayList<String>();
        if (config.getExcludes() != null) {
            for (Match include : config.getExcludes()) {
                StringBuilder stringBuilder = new StringBuilder();
                if (StringUtils.isNotEmpty(include.getClassName())) {
                    stringBuilder.append(include.getClassName().replaceAll("\\.", "/"));
                }
                if (StringUtils.isNotEmpty(include.getMethodName())) {
                    stringBuilder.append("#" + include.getMethodName());
                    if (StringUtils.isNotEmpty(include.getMethodDesc())) {
                        stringBuilder.append("!" + include.getMethodDesc());
                    }
                } else if (StringUtils.isNotEmpty(include.getMethodDesc())) {
                    stringBuilder.append("#**!" + include.getMethodDesc());
                }
                blackList.add(stringBuilder.toString());
            }
        }
        ClassMethodFilter classMethodFilter = new ClassMethodFilter(blackList, whiteList, useAnnotations);
        ClassMetadataReader metadataReader = new ClassMetadataReader(libs.stream().map(x -> {
            try {
                return new JarFile(x.toFile());
            }
            catch (IOException ex) {
                return null;
            }
        }).collect(Collectors.toList()));
        String outputName = inputJarPath.getFileName().toString();
        if (output.toFile().isDirectory()) {
            outputDir = output;
        } else {
            outputDir = output.getParent();
            outputName = output.getFileName().toString();
        }
        Path cppDir = outputDir.resolve("cpp");
        Files.createDirectories(cppDir, new FileAttribute[0]);
        Util.copyResource("jni.h", cppDir);
        HashMap map = new HashMap();
        HashMap classNameMap = new HashMap();
        StringBuilder instructions = new StringBuilder();
        File jarFile = inputJarPath.toAbsolutePath().toFile();
        Path temp = Files.createTempDirectory("native-myj2c-", new FileAttribute[0]);
        Path tempFile = temp.resolve(UUID.randomUUID() + ".data");
        Path encryptFile = temp.resolve(UUID.randomUUID() + ".data");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(outputDir.resolve(outputName), new OpenOption[0]));
             ZipOutputStream source = new ZipOutputStream(Files.newOutputStream(tempFile, new OpenOption[0]));){
            Manifest mf;
            JarFile jar = new JarFile(jarFile);
            System.out.println("正在解析 " + jarFile + "...");
            this.nativeDir = "myj2c/" + NativeObfuscator.getRandomString(6);
            this.bootstrapMethodsPool = new BootstrapMethodsPool(this.nativeDir);
            this.staticClassProvider = new InterfaceStaticClassProvider(this.nativeDir);
            this.methodIndex = 1;
            jar.stream().forEach(entry -> {
                block17: {
                    try {
                        if (!entry.getName().endsWith(".class")) break block17;
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try (InputStream in = jar.getInputStream((ZipEntry)entry);){
                            Util.transfer(in, baos);
                        }
                        byte[] src = baos.toByteArray();
                        ClassReader classReader = new ClassReader(src);
                        ClassNode classNode = new ClassNode(458752);
                        classReader.accept(classNode, 0);
                        if (classMethodFilter.shouldProcess(classNode)) {
                            boolean isStatic;
                            Map<String, CachedClassInfo> cache = this.cachedClasses.getCache();
                            CachedClassInfo classInfo = new CachedClassInfo(classNode.name, classNode.name, "", cache.size(), Util.getFlag(classNode.access, 8));
                            for (FieldNode field : classNode.fields) {
                                isStatic = Util.getFlag(field.access, 8);
                                CachedFieldInfo cachedFieldInfo = new CachedFieldInfo(classNode.name, field.name, field.desc, isStatic);
                                classInfo.addCachedField(cachedFieldInfo);
                            }
                            for (MethodNode method : classNode.methods) {
                                if ("<clinit>".equals(method.name)) continue;
                                isStatic = Util.getFlag(method.access, 8);
                                CachedMethodInfo cachedMethodInfo = new CachedMethodInfo(classNode.name, method.name, method.desc, isStatic);
                                classInfo.addCachedMethod(cachedMethodInfo);
                            }
                            cache.put(classNode.name, classInfo);
                        }
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
            });
            AtomicInteger classNumber = new AtomicInteger();
            AtomicInteger methodNumber = new AtomicInteger();
            jar.stream().forEach(entry -> {
                if (entry.getName().equals("META-INF/MANIFEST.MF")) {
                    return;
                }
                try {
                    if (!entry.getName().endsWith(".class")) {
                        Util.writeEntry(jar, out, entry);
                        return;
                    }
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (InputStream in = jar.getInputStream((ZipEntry)entry);){
                        Util.transfer(in, baos);
                    }
                    byte[] src = baos.toByteArray();
                    if (Util.byteArrayToInt(Arrays.copyOfRange(src, 0, 4)) != -889275714) {
                        Util.writeEntry(out, entry.getName(), src);
                        return;
                    }
                    this.nativeMethods = new StringBuilder();
                    ClassReader classReader = new ClassReader(src);
                    ClassNode rawClassNode = new ClassNode(458752);
                    classReader.accept(rawClassNode, 2);
                    if (!classMethodFilter.shouldProcess(rawClassNode) || rawClassNode.methods.stream().noneMatch(method -> MethodProcessor.shouldProcess(method) && classMethodFilter.shouldProcess(rawClassNode, (MethodNode)method))) {
                        if (useAnnotations) {
                            ClassMethodFilter.cleanAnnotations(rawClassNode);
                            SafeClassWriter clearedClassWriter = new SafeClassWriter(metadataReader, 458752);
                            rawClassNode.accept(clearedClassWriter);
                            Util.writeEntry(out, entry.getName(), clearedClassWriter.toByteArray());
                            return;
                        }
                        Util.writeEntry(out, entry.getName(), src);
                        return;
                    }
                    Util.writeEntry(jar, source, entry);
                    classNumber.getAndIncrement();
                    SafeClassWriter preprocessorClassWriter = new SafeClassWriter(metadataReader, 458755);
                    rawClassNode.accept(preprocessorClassWriter);
                    classReader = new ClassReader(preprocessorClassWriter.toByteArray());
                    ClassNode classNode = new ClassNode(458752);
                    classReader.accept(classNode, 0);
                    if (classNode.methods.stream().noneMatch(x -> x.name.equals("<clinit>"))) {
                        classNode.methods.add(new MethodNode(458752, 8, "<clinit>", "()V", null, new String[0]));
                    }
                    this.staticClassProvider.newClass();
                    map.put(classNode.name, classNode);
                    classNameMap.put(classNode.name, classReader.getClassName());
                    instructions.append("\n//" + classNode.name + "\n");
                    for (int i = 0; i < classNode.methods.size(); ++i) {
                        MethodNode method2 = classNode.methods.get(i);
                        if (!MethodProcessor.shouldProcess(method2) || !classMethodFilter.shouldProcess(classNode, method2) && !"<clinit>".equals(method2.name)) continue;
                        MethodContext context = new MethodContext(this, method2, this.methodIndex, classNode, this.currentClassId);
                        this.methodProcessor.processMethod(context);
                        instructions.append(context.output.toString().replace("\n", "\n    "));
                        this.nativeMethods.append((CharSequence)context.nativeMethods);
                        if ((classNode.access & 0x200) > 0) {
                            method2.access &= 0xFFFFFEFF;
                        }
                        ++this.methodIndex;
                        if ("<clinit>".equals(method2.name)) continue;
                        methodNumber.getAndIncrement();
                    }
                    if (!this.staticClassProvider.isEmpty()) {
                        this.cachedStrings.getPointer(this.staticClassProvider.getCurrentClassName().replace('/', '.'));
                    }
                    if (useAnnotations) {
                        ClassMethodFilter.cleanAnnotations(classNode);
                    }
                    classNode.visitMethod(4361, "$myj2cLoader", "()V", null, new String[0]);
                    classNode.version = 52;
                    SafeClassWriter classWriter = new SafeClassWriter(metadataReader, 458755);
                    classNode.accept(classWriter);
                    Util.writeEntry(out, entry.getName(), classWriter.toByteArray());
                    ++this.currentClassId;
                }
                catch (IOException ex) {
                    System.out.println("Error while processing " + entry.getName() + ex.getMessage());
                }
            });
            source.flush();
            source.close();
            if (!Files.exists(Paths.get(outputDir + separator + "build" + separator + "lib", new String[0]), new LinkOption[0])) {
                Files.createDirectories(Paths.get(outputDir + separator + "build" + separator + "lib", new String[0]), new FileAttribute[0]);
            }
            try {
                FileUtils.encryptFile(tempFile, encryptFile, "MY20150501@2022");
                Files.deleteIfExists(tempFile);
            }
            catch (Exception exception) {
                // empty catch block
            }
            DataTool.compress(temp.toString(), outputDir + separator + "build" + separator + "lib" + separator + tempFile.toFile().getName(), Integer.getInteger("level", 1));
            FileUtils.clearDirectory(temp.toString());
            for (ClassNode ifaceStaticClass : this.staticClassProvider.getReadyClasses()) {
                SafeClassWriter classWriter = new SafeClassWriter(metadataReader, 458755);
                ifaceStaticClass.accept(classWriter);
                Util.writeEntry(out, ifaceStaticClass.name + ".class", classWriter.toByteArray());
            }
            Path loader = Files.createTempFile("bin", null, new FileAttribute[0]);
            try {
                byte[] arrayOfByte = new byte[2048];
                Path datFile = null;
                try {
                    InputStream inputStream = NativeObfuscator.class.getResourceAsStream("/myj2c.bin");
                    if (inputStream == null) {
                        throw new UnsatisfiedLinkError(String.format("Failed to open dat file: myj2c.bin", new Object[0]));
                    }
                    try {
                        int size;
                        datFile = Files.createTempFile("dat", null, new FileAttribute[0]);
                        FileOutputStream fileOutputStream = new FileOutputStream(datFile.toFile());
                        while ((size = inputStream.read(arrayOfByte)) != -1) {
                            fileOutputStream.write(arrayOfByte, 0, size);
                        }
                        fileOutputStream.close();
                    }
                    catch (Throwable throwable) {
                        throw throwable;
                    }
                    finally {
                        inputStream.close();
                    }
                }
                catch (IOException exception) {
                    throw new UnsatisfiedLinkError(String.format("Failed to copy file: %s", exception.getMessage()));
                }
                FileUtils.decryptToFile(datFile, loader, "MY20150501@2022");
                Files.deleteIfExists(datFile);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            JarFile loaderJar = new JarFile(loader.toFile());
            HashMap classMap = new HashMap();
            loaderJar.stream().forEach(entry -> {
                if (entry.getName().endsWith(".class")) {
                    classMap.put(entry.getName().replace(".class", ""), entry.getName());
                }
            });
            loaderJar.stream().forEach(entry -> {
                if (!entry.getName().endsWith(".class")) {
                    return;
                }
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (InputStream in = loaderJar.getInputStream((ZipEntry)entry);){
                        Util.transfer(in, baos);
                    }
                    byte[] src = baos.toByteArray();
                    ClassReader classReader = new ClassReader(src);
                    ClassNode rawClassNode = new ClassNode(458752);
                    classReader.accept(rawClassNode, 2);
                    ClassNode resultLoaderClass = new ClassNode(458752);
                    final String originalLoaderClassName = rawClassNode.name;
                    if (StringUtils.contains(entry.getName(), "Loader")) {
                        final String loaderClassName = this.nativeDir + "/Loader";
                        if (plainLibName != null) {
                            if (StringUtils.contains(entry.getName(), "LoaderPlain")) {
                                rawClassNode.methods.forEach(method -> {
                                    for (int i = 0; i < method.instructions.size(); ++i) {
                                        AbstractInsnNode insnNode = method.instructions.get(i);
                                        if (!(insnNode instanceof LdcInsnNode) || !(((LdcInsnNode)insnNode).cst instanceof String) || !((LdcInsnNode)insnNode).cst.equals("%LIB_NAME%")) continue;
                                        ((LdcInsnNode)insnNode).cst = plainLibName;
                                    }
                                });
                                rawClassNode.accept(new ClassRemapper(resultLoaderClass, new Remapper(){

                                    @Override
                                    public String map(String internalName) {
                                        return internalName.equals(originalLoaderClassName) ? loaderClassName : (classMap.get(internalName) != null ? NativeObfuscator.this.nativeDir + "/" + internalName : internalName);
                                    }
                                }));
                                SafeClassWriter classWriter = new SafeClassWriter(metadataReader, 458755);
                                for (ClassNode bootstrapClass : this.bootstrapMethodsPool.getClasses()) {
                                    bootstrapClass.accept(classWriter);
                                }
                                resultLoaderClass.accept(classWriter);
                                Util.writeEntry(out, loaderClassName + ".class", classWriter.toByteArray());
                            }
                        } else if (StringUtils.contains(entry.getName(), "LoaderUnpack")) {
                            rawClassNode.accept(new ClassRemapper(resultLoaderClass, new Remapper(){

                                @Override
                                public String map(String internalName) {
                                    return internalName.equals(originalLoaderClassName) ? loaderClassName : (classMap.get(internalName) != null ? NativeObfuscator.this.nativeDir + "/" + internalName : internalName);
                                }
                            }));
                            this.rewriteClass(resultLoaderClass);
                            SafeClassWriter classWriter = new SafeClassWriter(metadataReader, 458755);
                            for (ClassNode bootstrapClass : this.bootstrapMethodsPool.getClasses()) {
                                bootstrapClass.accept(classWriter);
                            }
                            resultLoaderClass.accept(classWriter);
                            Util.writeEntry(out, loaderClassName + ".class", classWriter.toByteArray());
                        }
                    } else if (StringUtils.isEmpty(plainLibName)) {
                        final String loaderClassName = this.nativeDir + "/" + originalLoaderClassName;
                        rawClassNode.accept(new ClassRemapper(resultLoaderClass, new Remapper(){

                            @Override
                            public String map(String internalName) {
                                return internalName.equals(originalLoaderClassName) ? loaderClassName : (classMap.get(internalName) != null ? NativeObfuscator.this.nativeDir + "/" + internalName : internalName);
                            }
                        }));
                        this.rewriteClass(resultLoaderClass);
                        SafeClassWriter classWriter = new SafeClassWriter(metadataReader, 458755);
                        resultLoaderClass.accept(classWriter);
                        Util.writeEntry(out, this.nativeDir + "/" + originalLoaderClassName + ".class", classWriter.toByteArray());
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            });
            try {
                loaderJar.close();
                Files.deleteIfExists(loader);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("共找到 " + classNumber.get() + " 个类文件 " + methodNumber.get() + " 个方法需要MYJ2C编译");
            if ("1".equals(LicenseManager.getValue("type"))) {
                if (methodNumber.get() > Integer.parseInt(LicenseManager.getValue("method")) || classNumber.get() > Integer.parseInt(LicenseManager.getValue("class"))) {
                    System.out.println("您使用的是个人版授权,需要编译的类或方法超过最大数量！！！");
                    return;
                }
                System.out.println("将使用个人版授权为您编译！！！\n");
            } else if ("2".equals(LicenseManager.getValue("type"))) {
                System.out.println("将使用专业版授权为您编译！！！\n");
            }
            boolean free = false;
            if (StringUtils.isEmpty(LicenseManager.getValue("type"))) {
                if (methodNumber.get() == 1 && classNumber.get() == 1) {
                    free = true;
                    System.out.println("将使用免费版授权为您编译,编译文件不会过期！！！\n");
                } else {
                    System.out.println("将使用试用版授权为您编译,当前编译的程序将在一周后过期,过期后将不能正常运行！！！\n");
                }
            }
            System.out.println("正在把class文件转换成C语言代码");
            BufferedWriter mainWriter = Files.newBufferedWriter(cppDir.resolve("myj2c.c"), new OpenOption[0]);
            mainWriter.append("#include <jni.h>\n#include <stdatomic.h>\n#include <string.h>\n#include <time.h>\n#include <stdbool.h>\n#include <math.h>\n\n");
            String signCode = "8964";
            String sign = LicenseManager.getValue("sign");
            String appInfo = "";
            if (!(free || signCode.equals(LicenseManager.v(66)) && sign.equals(LicenseManager.v(88)))) {
                mainWriter.append("int info = 0;\n");
                mainWriter.append("long expire = " + (System.currentTimeMillis() / 1000L + 604800L) + ";\n");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                appInfo = "\n              __  _____  __    _____   ______\n             /  |/  /\\ \\/ /   / /__ \\ / ____/\n            / /|_/ /  \\  /_  / /__/ // /     \n           / /  / /   / / /_/ // __// /___   \n          /_/  /_/   /_/\\____//____/\\____/\n\n            该应用使用MYJ2C试用版创建 \n\n            应用将在 " + sdf.format(new Date(System.currentTimeMillis() + 604800000L)) + "过期 \n\n         移除该信息请加QQ群:197453088\n\n========================================================\n\n";
            }
            if (free && StringUtils.isEmpty(LicenseManager.getValue("type"))) {
                mainWriter.append("int info = 0;\n");
                appInfo = "\n              __  _____  __    _____   ______\n             /  |/  /\\ \\/ /   / /__ \\ / ____/\n            / /|_/ /  \\  /_  / /__/ // /     \n           / /  / /   / / /_/ // __// /___   \n          /_/  /_/   /_/\\____//____/\\____/\n\n            该应用使用MYJ2C免费版创建 \n\n            (c) 2022-2024 MYJ2C 版权所有\n\n========================================================\n\n";
            }
            if ("1".equals(LicenseManager.getValue("type"))) {
                mainWriter.append("int info = 0;\n");
                appInfo = "\n              __  _____  __    _____   ______\n             /  |/  /\\ \\/ /   / /__ \\ / ____/\n            / /|_/ /  \\  /_  / /__/ // /     \n           / /  / /   / / /_/ // __// /___   \n          /_/  /_/   /_/\\____//____/\\____/\n\n            " + (LicenseManager.getValue("product") == null ? "该应用使用MYJ2C个人版创建" : LicenseManager.getValue("product")) + " \n\n            (c) 2022-2024 " + LicenseManager.getValue("name") + " 版权所有\n\n========================================================\n\n";
            }
            if (!"".equals(appInfo)) {
                mainWriter.append("struct cached_system {\n    jclass clazz;\n    jfieldID id_0;\n};\n\nstatic const struct cached_system* cc_system(JNIEnv *env) {\n    static struct cached_system cache;\n    static atomic_flag lock;\n    if (cache.clazz) return &cache;\n\n    jclass clazz = (*env)->FindClass(env, \"java/lang/System\");\n    while (atomic_flag_test_and_set(&lock)) {}\n    if (!cache.clazz) {\n        cache.clazz = (*env)->NewGlobalRef(env, clazz);\n        cache.id_0 = (*env)->GetStaticFieldID(env, clazz, \"out\", \"Ljava/io/PrintStream;\");\n    }\n    atomic_flag_clear(&lock);\n    return &cache;\n}\n\nstruct cached_print {\n    jclass clazz;\n    jmethodID method_0;\n};\n\nstatic const struct cached_print* cc_print(JNIEnv *env) {\n    static struct cached_print cache;\n    static atomic_flag lock;\n    if (cache.clazz) return &cache;\n\n    jclass clazz = (*env)->FindClass(env, \"java/io/PrintStream\");\n    while (atomic_flag_test_and_set(&lock)) {}\n    if (!cache.clazz) {\n        cache.clazz = (*env)->NewGlobalRef(env, clazz);\n        cache.method_0 = (*env)->GetMethodID(env, clazz, \"println\", \"(Ljava/lang/String;)V\");\n    }\n    atomic_flag_clear(&lock);\n    return &cache;\n}");
            }
            mainWriter.append("void throw_exception(JNIEnv *env, const char *exception, const char *error, int line) {\n        jclass exception_ptr = (*env)->FindClass(env, exception);\n        if ((*env)->ExceptionCheck(env)) {\n            (*env)->ExceptionDescribe(env);\n            (*env)->ExceptionClear(env);\n            return;\n        }\n        char str[strlen(error) + 10];\n        sprintf(str, \"%s on %d\", error, line);\n        (*env)->ThrowNew(env, exception_ptr,  str);\n        (*env)->DeleteLocalRef(env, exception_ptr);\n    }\n\n");
            for (Map.Entry<String, CachedClassInfo> next : this.cachedClasses.getCache().entrySet()) {
                int id = next.getValue().getId();
                String fieldStr = "";
                List<CachedFieldInfo> fields = next.getValue().getCachedFields();
                for (int i = 0; i < fields.size(); ++i) {
                    CachedFieldInfo fieldInfo = fields.get(i);
                    if ("<init>".equals(fieldInfo.getName())) continue;
                    fieldStr = fieldStr + "    jfieldID id_" + i + ";\n";
                }
                String methodStr = "";
                List<CachedMethodInfo> methods = next.getValue().getCachedMethods();
                for (int i = 0; i < methods.size(); ++i) {
                    CachedMethodInfo methodInfo = (CachedMethodInfo)methods.get(i);
                    methodStr = methodStr + "    jmethodID method_" + i + ";\n";
                }
                mainWriter.append("struct cached_c_" + id + " {\n    jclass clazz;\n" + fieldStr + methodStr + "    jboolean initialize;\n};\n\n");
                String cachedFieldStr = "";
                for (int i = 0; i < fields.size(); ++i) {
                    CachedFieldInfo fieldInfo = fields.get(i);
                    if ("<init>".equals(fieldInfo.getName())) continue;
                    cachedFieldStr = fieldInfo.isStatic() ? cachedFieldStr + "        cache.id_" + i + " = (*env)->GetStaticFieldID(env, clazz, \"" + fieldInfo.getName() + "\", \"" + fieldInfo.getDesc() + "\");\n" : cachedFieldStr + "        cache.id_" + i + " = (*env)->GetFieldID(env, clazz, \"" + fieldInfo.getName() + "\", \"" + fieldInfo.getDesc() + "\");\n";
                }
                String cachedMethodStr = "";
                for (int i = 0; i < methods.size(); ++i) {
                    Object methodInfo = (CachedMethodInfo)methods.get(i);
                    cachedMethodStr = ((CachedMethodInfo)methodInfo).isStatic() ? cachedMethodStr + "        cache.method_" + i + " = (*env)->GetStaticMethodID(env, clazz, \"" + ((CachedMethodInfo)methodInfo).getName() + "\", \"" + ((CachedMethodInfo)methodInfo).getDesc() + "\");\n" : cachedMethodStr + "        cache.method_" + i + " = (*env)->GetMethodID(env, clazz, \"" + ((CachedMethodInfo)methodInfo).getName() + "\", \"" + ((CachedMethodInfo)methodInfo).getDesc() + "\");\n";
                }
                mainWriter.append("static const struct cached_c_" + id + "* c_" + id + "_(JNIEnv *env) {\n    static struct cached_c_" + id + " cache;\n    static atomic_flag lock;\n    if (cache.initialize) return &cache;\n    cache.initialize = JNI_FALSE;\n    jclass clazz = (*env)->FindClass(env, \"" + next.getKey() + "\");\n    while (atomic_flag_test_and_set(&lock)) {}\n    if (!cache.initialize) {\n        cache.clazz = (*env)->NewGlobalRef(env, clazz);\n        if ((*env)->ExceptionCheck(env) && !clazz) {\n            cache.initialize = JNI_FALSE;\n            (*env)->ExceptionDescribe(env);\n            (*env)->ExceptionClear(env);\n            atomic_flag_clear(&lock);\n            return &cache;\n        }\n" + cachedFieldStr + cachedMethodStr + "        cache.initialize = JNI_TRUE;\n    }\n    atomic_flag_clear(&lock);\n    return &cache;\n}\n\n");
            }
            mainWriter.append(instructions);
            for (Map.Entry<String, CachedClassInfo> next : this.cachedClasses.getCache().entrySet()) {
                String methodName;
                ClassNode classNode = (ClassNode)map.get(next.getKey());
                if (classNode == null) continue;
                String registrationMethods = "";
                int methodCount = 0;
                for (MethodNode method : classNode.methods) {
                    if ("<init>".equals(method.name) || "<clinit>".equals(method.name) || "$myj2cLoader".equals(method.name)) continue;
                    methodName = null;
                    methodName = "$myj2cClinit".equals(method.name) ? this.getClassMethodNameMap().get(classNode.name + ".<clinit>()V") : this.getClassMethodNameMap().get(classNode.name + "." + method.name + method.desc);
                    if (methodName == null) continue;
                    registrationMethods = registrationMethods + "            {\"" + method.name + "\", \"" + method.desc + "\", (void *) &" + methodName + "},\n";
                    ++methodCount;
                }
                String className = (String)classNameMap.get(classNode.name);
                if (!Util.isValidJavaFullClassName(className.replaceAll("/", "."))) continue;
                String licenseInfo = "if(info == 0){\n  info = 1;\n    jvalue cstack0; memset(&cstack0, 0, sizeof(jvalue));\n    jvalue cstack1; memset(&cstack1, 0, sizeof(jvalue));\n    \n    cstack0.l = (*env)->GetStaticObjectField(env, cc_system(env)->clazz, cc_system(env)->id_0); \n    if ((*env)->ExceptionCheck(env)) { return; }\n    cstack1.l = (*env)->NewString(env, (unsigned short[]) {" + Util.utf82unicode(appInfo) + "}, " + appInfo.length() + ");\n    (*env)->CallVoidMethod(env, cstack0.l, cc_print(env)->method_0, cstack1.l);\n    if ((*env)->ExceptionCheck(env)) { return; }\n}\n";
                methodName = NativeSignature.getJNICompatibleName(className);
                if (!(free || signCode.equals(LicenseManager.v(66)) && sign.equals(LicenseManager.v(88)))) {
                    mainWriter.append("/* Native registration for <" + className + "> */\nJNIEXPORT void JNICALL Java_" + methodName + "__00024myj2cLoader(JNIEnv *env, jclass clazz) {\n    JNINativeMethod table[] = {\n" + registrationMethods + "    };\n" + licenseInfo + "\nif(time(NULL)<expire){\n    (*env)->RegisterNatives(env, clazz, table, " + methodCount + ");\n}else{\n    jvalue cstack0; memset(&cstack0, 0, sizeof(jvalue));\n    jvalue cstack1; memset(&cstack1, 0, sizeof(jvalue));\n    \n    cstack0.l = (*env)->GetStaticObjectField(env, cc_system(env)->clazz, cc_system(env)->id_0); \n    if ((*env)->ExceptionCheck(env)) { return; }\n    cstack1.l = (*env)->NewString(env, (unsigned short[]) {" + Util.utf82unicode("该应用使用MYJ2C试用版本创建，试用过期，已不能运行！！！") + "}, " + "该应用使用MYJ2C试用版本创建，试用过期，已不能运行！！！".length() + ");\n    (*env)->CallVoidMethod(env, cstack0.l, cc_print(env)->method_0, cstack1.l);\n    if ((*env)->ExceptionCheck(env)) { return; }\n    exit(-1);\n}\n}\n\n");
                    continue;
                }
                if (free || "1".equals(LicenseManager.getValue("type"))) {
                    mainWriter.append("/* Native registration for <" + className + "> */\nJNIEXPORT void JNICALL Java_" + methodName + "__00024myj2cLoader(JNIEnv *env, jclass clazz) {\n    JNINativeMethod table[] = {\n" + registrationMethods + "    };\n" + licenseInfo + "\n    (*env)->RegisterNatives(env, clazz, table, " + methodCount + ");\n}\n\n");
                    continue;
                }
                mainWriter.append("/* Native registration for <" + className + "> */\nJNIEXPORT void JNICALL Java_" + methodName + "__00024myj2cLoader(JNIEnv *env, jclass clazz) {\n    JNINativeMethod table[] = {\n" + registrationMethods + "    };\n    (*env)->RegisterNatives(env, clazz, table, " + methodCount + ");\n}\n\n");
            }
            mainWriter.close();
            if (StringUtils.isEmpty(plainLibName)) {
                System.out.println("开始编译动态链接库文件");
                ArrayList<String> libNames = new ArrayList<String>();
                Iterator<String> iterator = config.getTargets().iterator();
                while (iterator.hasNext()) {
                    String libName;
                    String platformTypeName;
                    String osName;
                    String target;
                    switch (target = iterator.next()) {
                        case "WINDOWS_X86_64": {
                            osName = "windows";
                            platformTypeName = "x86_64";
                            libName = "x64-windows.dll";
                            break;
                        }
                        case "MACOS_X86_64": {
                            osName = "macos";
                            platformTypeName = "x86_64";
                            libName = "x64-macos.dylib";
                            break;
                        }
                        case "LINUX_X86_64": {
                            osName = "linux";
                            platformTypeName = "x86_64";
                            libName = "x64-linux.so";
                            break;
                        }
                        case "WINDOWS_AARCH64": {
                            osName = "windows";
                            platformTypeName = "aarch64";
                            libName = "arm64-windows.dll";
                            break;
                        }
                        case "MACOS_AARCH64": {
                            osName = "macos";
                            platformTypeName = "aarch64";
                            libName = "arm64-macos.dylib";
                            break;
                        }
                        case "LINUX_AARCH64": {
                            osName = "linux";
                            platformTypeName = "aarch64";
                            libName = "arm64-linux.so";
                            break;
                        }
                        default: {
                            platformTypeName = "";
                            osName = "";
                            libName = "";
                        }
                    }
                    String currentOSName = "";
                    if (SetupManager.isWindows()) {
                        currentOSName = "windows";
                    }
                    if (SetupManager.isLinux()) {
                        currentOSName = "linux";
                    }
                    if (SetupManager.isMacOS()) {
                        currentOSName = "macos";
                    }
                    String currentPlatformTypeName = "";
                    switch (System.getProperty("os.arch").toLowerCase()) {
                        case "x86_64":
                        case "amd64": {
                            currentPlatformTypeName = "x86_64";
                            break;
                        }
                        case "aarch64": {
                            currentPlatformTypeName = "aarch64";
                            break;
                        }
                        case "x86": {
                            currentPlatformTypeName = "i386";
                            break;
                        }
                        default: {
                            currentPlatformTypeName = "";
                        }
                    }
                    System.out.println("正在编译:" + target);
                    String compilePath = System.getProperty("user.dir") + separator + "zig-" + currentOSName + "-" + currentPlatformTypeName + "-0.9.1" + separator + "zig" + (SetupManager.isWindows() ? ".exe" : "");
                    if (Files.exists(Paths.get(compilePath, new String[0]), new LinkOption[0])) {
                        ProcessHelper.ProcessResult compileRunresult = ProcessHelper.run(outputDir, 600000L, Arrays.asList(compilePath, "cc", "-O2", "-fno-sanitize=undefined", "-funroll-loops", "-target", platformTypeName + "-" + osName + "-gnu", "-fPIC", "-shared", "-s", "-fvisibility=hidden", "-fvisibility-inlines-hidden", "-I." + separator + "cpp", "-o." + separator + "build" + separator + "lib" + separator + libName, "." + separator + "cpp" + separator + "myj2c.c"));
                        System.out.println(String.format("编译完成耗时 %dms", compileRunresult.execTime));
                        libNames.add(libName);
                        compileRunresult.check("zig build");
                        continue;
                    }
                    Path parent = Paths.get(System.getProperty("user.dir"), new String[0]).getParent();
                    ProcessHelper.ProcessResult compileRunresult = ProcessHelper.run(outputDir, 600000L, Arrays.asList(parent.toFile().getAbsolutePath() + separator + "zig-" + currentOSName + "-" + currentPlatformTypeName + "-0.9.1" + separator + "zig" + (SetupManager.isWindows() ? ".exe" : ""), "cc", "-O2", "-fno-sanitize=undefined", "-funroll-loops", "-target", platformTypeName + "-" + osName + "-gnu", "-fPIC", "-shared", "-s", "-fvisibility=hidden", "-fvisibility-inlines-hidden", "-I." + separator + "cpp", "-o." + separator + "build" + separator + "lib" + separator + libName, "." + separator + "cpp" + separator + "myj2c.c"));
                    System.out.println(String.format("编译完成耗时 %dms", compileRunresult.execTime));
                    libNames.add(libName);
                    compileRunresult.check("zig build");
                }
                System.out.println("正在压缩已编译的动态链接库文件");
                DataTool.compress(outputDir + separator + "build" + separator + "lib", outputDir + separator + "data.dat", Integer.getInteger("level", 1));
                System.out.println("正在重新打包");
                Util.writeEntry(out, this.nativeDir + "/data.dat", Files.readAllBytes(Paths.get(outputDir + separator + "data.dat", new String[0])));
                try {
                    System.out.println("清理临时文件");
                    FileUtils.clearDirectory(outputDir + separator + "cpp");
                    FileUtils.clearDirectory(outputDir + separator + "build");
                    Files.deleteIfExists(Paths.get(outputDir + separator + "data.dat", new String[0]));
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            if ((mf = jar.getManifest()) != null) {
                mf.getMainAttributes().put(new Attributes.Name("Built-By"), "MYJ2C V2022.1009.05");
                mf.getMainAttributes().put(new Attributes.Name("Built-Tools-Info"), "QQ GROUP:197453088");
                mf.getMainAttributes().put(new Attributes.Name("Built-Tools-Info-URL"), "www.myj2c.cn");
                out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
                mf.write(out);
            }
            out.setComment("Created-By MuYang Java to C Bytecode Translator V2022.1009.05 \nQQGROUP:197453088");
            out.closeEntry();
            metadataReader.close();
            System.out.println("MYJ2C编译任务已成功");
        }
    }

    public Snippets getSnippets() {
        return this.snippets;
    }

    public InterfaceStaticClassProvider getStaticClassProvider() {
        return this.staticClassProvider;
    }

    public NodeCache<String> getCachedStrings() {
        return this.cachedStrings;
    }

    public ClassNodeCache getCachedClasses() {
        return this.cachedClasses;
    }

    public MethodNodeCache getCachedMethods() {
        return this.cachedMethods;
    }

    public FieldNodeCache getCachedFields() {
        return this.cachedFields;
    }

    public String getNativeDir() {
        return this.nativeDir;
    }

    public BootstrapMethodsPool getBootstrapMethodsPool() {
        return this.bootstrapMethodsPool;
    }

    public Map<String, String> getClassMethodNameMap() {
        return this.classMethodNameMap;
    }

    public Map<String, String> getNoInitClassMap() {
        return this.noInitClassMap;
    }

    private static String getRandomString(int length) {
        String str = "zxcvbnmlkjhgfdsaqwertyuiopQWERTYUIOPASDFGHJKLZXCVBNM1234567890";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        sb.append(str.charAt(random.nextInt(26)));
        for (int i = 0; i < length - 1; ++i) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    private void varargsAccess(MethodNode methodNode) {
        if ((methodNode.access & 0x1000) == 0 && (methodNode.access & 0x40) == 0) {
            methodNode.access |= 0x80;
        }
    }

    private void bridgeAccess(MethodNode methodNode) {
        if (!methodNode.name.contains("<") && !Modifier.isAbstract(methodNode.access)) {
            methodNode.access |= 0x40;
        }
    }

    private void syntheticAccess(ClassNode classNode) {
        classNode.access |= 0x1000;
        classNode.fields.forEach(fieldNode -> fieldNode.access |= 0x1000);
        classNode.methods.forEach(methodNode -> methodNode.access |= 0x1000);
    }

    private void changeSource(ClassNode classNode) {
        classNode.sourceFile = this.getMassiveString();
        classNode.sourceDebug = this.getMassiveString();
    }

    private void changeSignature(ClassNode classNode) {
        classNode.signature = this.getMassiveString();
        classNode.fields.forEach(fieldNode -> {
            fieldNode.signature = this.getMassiveString();
        });
        classNode.methods.forEach(methodNode -> {
            methodNode.signature = this.getMassiveString();
        });
    }

    private void deprecatedAccess(ClassNode classNode) {
        classNode.access |= 0x20000;
        classNode.methods.forEach(methodNode -> methodNode.access |= 0x20000);
        classNode.fields.forEach(fieldNode -> fieldNode.access |= 0x20000);
    }

    private void transientAccess(ClassNode classNode) {
        classNode.fields.forEach(fieldNode -> fieldNode.access |= 0x80);
    }

    private void removeNop(ClassNode classNode) {
        classNode.methods.parallelStream().forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray()).filter(insnNode -> insnNode.getOpcode() == 0).forEach(insnNode -> methodNode.instructions.remove((AbstractInsnNode)insnNode)));
    }

    private String getMassiveString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Short.MAX_VALUE; ++i) {
            builder.append(" ");
        }
        return builder.toString();
    }

    private void rewriteClass(ClassNode classNode) {
        this.removeNop(classNode);
        if (!Modifier.isInterface(classNode.access)) {
            this.transientAccess(classNode);
        }
        this.deprecatedAccess(classNode);
        this.changeSource(classNode);
        this.changeSignature(classNode);
        this.syntheticAccess(classNode);
        classNode.methods.forEach(methodNode -> {
            this.bridgeAccess((MethodNode)methodNode);
            this.varargsAccess((MethodNode)methodNode);
        });
    }
}
