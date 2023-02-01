// Decompiled with: CFR 0.152
// Class Version: 8
package rip.jnic.helpers;

import rip.jnic.env.SetupManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ProcessHelper {
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    private static void readStream(InputStream is, Consumer<String> consumer) {
        executor.submit(() -> {
            try (InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader reader = new BufferedReader(isr);){
                int count;
                char[] buf = new char[1024];
                while ((count = reader.read(buf)) != -1) {
                    consumer.accept(String.copyValueOf(buf, 0, count));
                }
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static ProcessResult run(Path directory, long timeLimit, List<String> command) throws IOException {
        Process process = new ProcessBuilder(command).directory(directory.toFile()).start();
        long startTime = System.currentTimeMillis();
        ProcessResult result = new ProcessResult();
        result.commandLine = String.join((CharSequence)" ", command);
        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();
        ProcessHelper.readStream(process.getInputStream(), stdoutBuilder::append);
        ProcessHelper.readStream(process.getErrorStream(), stderrBuilder::append);
        try {
            if (!process.waitFor(timeLimit, TimeUnit.MILLISECONDS)) {
                result.timeout = true;
                process.destroyForcibly();
            }
            process.waitFor();
        }
        catch (InterruptedException interruptedException) {
            // empty catch block
        }
        result.stdout = stdoutBuilder.toString();
        result.stderr = stderrBuilder.toString();
        result.execTime = System.currentTimeMillis() - startTime;
        result.exitCode = process.exitValue();
        return result;
    }

    public static class ProcessResult {
        public int exitCode;
        public long execTime;
        public boolean timeout;
        public String stdout = "";
        public String stderr = "";
        public String commandLine;

        public void check(String processName) {
            if (!this.timeout && this.exitCode == 0) {
                return;
            }
            if (this.timeout) {
                System.err.println(processName + " 编译超时,可能是您编译的类和方法太多或者机器性能较低导致编译超时");
            } else if (this.commandLine.contains("zig") && this.commandLine.contains("myj2c")) {
                System.err.println(processName + " 编译错误");
                System.err.println("已为您自动清理zig临时文件:" + SetupManager.getZigGlobalCacheDirectory(true) + " 请重新运行");
                System.out.println("如果再次运行失败,请手动删除后重试,手动删除后仍失败请反馈问题给开发者");
            }
            System.err.println("stdout: \n" + this.stdout);
            System.err.println("stderr: \n" + this.stderr);
            throw new RuntimeException(processName + " " + (this.timeout ? "命令执行超时" : "命令执行出错"));
        }
    }
}
