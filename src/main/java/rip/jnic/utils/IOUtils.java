package rip.jnic.utils;

import java.io.*;
import java.nio.charset.Charset;

public final class IOUtils
{
    public static final int DEFAULT_BUFFER_SIZE = 8192;
    
    public static byte[] readFullyWithoutClosing(final InputStream stream) throws IOException {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        copyTo(stream, result);
        return result.toByteArray();
    }
    
    public static ByteArrayOutputStream readFully(final InputStream stream) throws IOException {
        Throwable t = null;
        try {
            final ByteArrayOutputStream result = new ByteArrayOutputStream();
            copyTo(stream, result);
            return result;
        }
        catch (Throwable t2) {
            t = t2;
            throw t2;
        }
        finally {
            if (stream != null) {
                if (t != null) {
                    try {
                        stream.close();
                    }
                    catch (Throwable t3) {
                        t.addSuppressed(t3);
                    }
                }
                else {
                    stream.close();
                }
            }
        }
    }
    
    public static byte[] readFullyAsByteArray(final InputStream stream) throws IOException {
        return readFully(stream).toByteArray();
    }
    
    public static String readFullyAsString(final InputStream stream) throws IOException {
        return readFully(stream).toString("UTF-8");
    }
    
    public static String readFullyAsString(final InputStream stream, final Charset charset) throws IOException {
        return readFully(stream).toString(charset.name());
    }
    
    public static void write(final String text, final OutputStream outputStream) throws IOException {
        write(text.getBytes(), outputStream);
    }
    
    public static void write(final byte[] bytes, final OutputStream outputStream) throws IOException {
        copyTo(new ByteArrayInputStream(bytes), outputStream);
    }
    
    public static void copyTo(final InputStream src, final OutputStream dest) throws IOException {
        copyTo(src, dest, new byte[8192]);
    }
    
    public static void copyTo(final InputStream src, final OutputStream dest, final byte[] buf) throws IOException {
        while (true) {
            final int len = src.read(buf);
            if (len == -1) {
                break;
            }
            dest.write(buf, 0, len);
        }
    }
}
