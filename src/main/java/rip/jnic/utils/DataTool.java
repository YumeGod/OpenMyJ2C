package rip.jnic.utils;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class DataTool
{
    private static final byte[] HEADER;
    
    public static void compress(final String fromDir, final String toFile, final int level) throws IOException {
        final long start = System.nanoTime();
        final long startMs = System.currentTimeMillis();
        final AtomicBoolean title = new AtomicBoolean();
        final long size = getSize(new File(fromDir), new Runnable() {
            int count;
            long lastTime = start;
            
            @Override
            public void run() {
                ++this.count;
                if (this.count % 1000 == 0) {
                    final long now = System.nanoTime();
                    if (now - this.lastTime > TimeUnit.SECONDS.toNanos(3L)) {
                        this.lastTime = now;
                    }
                }
            }
        });
        final InputStream in = getDirectoryInputStream(fromDir);
        final String temp = toFile + ".temp";
        OutputStream out = new BufferedOutputStream(new FileOutputStream(toFile), 1048576);
        final Deflater def = new Deflater();
        def.setLevel(level);
        out = new BufferedOutputStream(new DeflaterOutputStream(out, def), 1048576);
        sort(in, out, temp, size);
        in.close();
        out.close();
    }
    
    private static long getSize(final File f, final Runnable r) {
        long size = 40L;
        if (f.isDirectory()) {
            final File[] list = f.listFiles();
            if (list != null) {
                for (final File c : list) {
                    size += getSize(c, r);
                }
            }
        }
        else {
            size += f.length();
        }
        r.run();
        return size;
    }
    
    private static InputStream getDirectoryInputStream(final String dir) {
        final File f = new File(dir);
        if (!f.isDirectory() || !f.exists()) {
            throw new IllegalArgumentException("Not an existing directory: " + dir);
        }
        return new InputStream() {
            private final String baseDir;
            private final ArrayList<String> files = new ArrayList<String>();
            private String current;
            private ByteArrayInputStream meta;
            private DataInputStream fileIn;
            private long remaining;
            
            {
                final File f = new File(dir);
                this.baseDir = f.getAbsolutePath();
                this.addDirectory(f);
            }
            
            private void addDirectory(final File f) {
                final File[] list = f.listFiles();
                if (list != null) {
                    for (final File c : list) {
                        if (c.isDirectory()) {
                            this.files.add(c.getAbsolutePath());
                        }
                    }
                    for (final File c : list) {
                        if (c.isFile()) {
                            this.files.add(c.getAbsolutePath());
                        }
                    }
                }
            }
            
            @Override
            public int read() throws IOException {
                if (this.meta != null) {
                    final int x = this.meta.read();
                    if (x >= 0) {
                        return x;
                    }
                    this.meta = null;
                }
                if (this.fileIn != null) {
                    if (this.remaining > 0L) {
                        final int x = this.fileIn.read();
                        --this.remaining;
                        if (x < 0) {
                            throw new EOFException();
                        }
                        return x;
                    }
                    else {
                        this.fileIn.close();
                        this.fileIn = null;
                    }
                }
                if (this.files.isEmpty()) {
                    return -1;
                }
                this.current = this.files.remove(this.files.size() - 1);
                final File f = new File(this.current);
                if (f.isDirectory()) {
                    this.addDirectory(f);
                }
                final ByteArrayOutputStream metaOut = new ByteArrayOutputStream();
                final DataOutputStream out = new DataOutputStream(metaOut);
                final boolean isFile = f.isFile();
                out.writeInt(0);
                out.write(isFile ? 1 : 0);
                out.write(f.canWrite() ? 0 : 1);
                DataTool.writeVarLong(out, f.lastModified());
                if (isFile) {
                    DataTool.writeVarLong(out, this.remaining = f.length());
                    this.fileIn = new DataInputStream(new BufferedInputStream(new FileInputStream(this.current), 1048576));
                }
                if (!this.current.startsWith(this.baseDir)) {
                    throw new IOException("File " + this.current + " does not start with " + this.baseDir);
                }
                final String n = this.current.substring(this.baseDir.length() + 1);
                out.writeUTF(n);
                out.writeInt(metaOut.size());
                out.flush();
                byte[] bytes = metaOut.toByteArray();
                System.arraycopy(bytes, bytes.length - 4, bytes, 0, 4);
                bytes = Arrays.copyOf(bytes, bytes.length - 4);
                this.meta = new ByteArrayInputStream(bytes);
                return this.meta.read();
            }
            
            @Override
            public int read(final byte[] buff, final int offset, final int length) throws IOException {
                if (this.meta != null || this.fileIn == null || this.remaining == 0L) {
                    return super.read(buff, offset, length);
                }
                final int l = (int)Math.min(length, this.remaining);
                this.fileIn.readFully(buff, offset, l);
                this.remaining -= l;
                return l;
            }
        };
    }
    
    private static void sort(final InputStream in, final OutputStream out, String tempFileName, final long size) throws IOException {
        final int bufferSize = 33554432;
        final DataOutputStream tempOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempFileName), 1048576));
        final byte[] bytes = new byte[bufferSize];
        List<Long> segmentStart = new ArrayList<Long>();
        long outPos = 0L;
        long id = 1L;
        while (true) {
            final int len = readFully(in, bytes, bytes.length);
            if (len == 0) {
                break;
            }
            final TreeMap<Chunk, Chunk> map = new TreeMap<Chunk, Chunk>();
            int pos = 0;
            while (pos < len) {
                final int[] key = getKey(bytes, pos, len);
                final int l = key[3];
                final byte[] buff = Arrays.copyOfRange(bytes, pos, pos + l);
                pos += l;
                final Chunk c = new Chunk(null, key, buff);
                final Chunk old = map.get(c);
                if (old == null) {
                    (c.idList = new ArrayList<Long>()).add(id);
                    map.put(c, c);
                }
                else {
                    old.idList.add(id);
                }
                ++id;
            }
            segmentStart.add(outPos);
            for (final Chunk c2 : map.keySet()) {
                outPos += c2.write(tempOut, true);
            }
            outPos += writeVarLong(tempOut, 0L);
        }
        tempOut.close();
        long tempSize = new File(tempFileName).length();
        final int blockSize = 64;
        boolean merge = false;
        while (segmentStart.size() > blockSize) {
            merge = true;
            final ArrayList<Long> segmentStart2 = new ArrayList<Long>();
            outPos = 0L;
            final DataOutputStream tempOut2 = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempFileName + ".b"), 1048576));
            while (segmentStart.size() > 0) {
                segmentStart2.add(outPos);
                final int s = Math.min(segmentStart.size(), blockSize);
                final List<Long> start = segmentStart.subList(0, s);
                final TreeSet<ChunkStream> segmentIn = new TreeSet<ChunkStream>();
                final long read = openSegments(start, segmentIn, tempFileName, true);
                Chunk last = null;
                final Iterator<Chunk> it = merge(segmentIn);
                while (it.hasNext()) {
                    final Chunk c3 = it.next();
                    if (last == null) {
                        last = c3;
                    }
                    else if (last.compareTo(c3) == 0) {
                        last.idList.addAll(c3.idList);
                    }
                    else {
                        outPos += last.write(tempOut2, true);
                        last = c3;
                    }
                }
                if (last != null) {
                    outPos += last.write(tempOut2, true);
                }
                outPos += writeVarLong(tempOut2, 0L);
                segmentStart = segmentStart.subList(s, segmentStart.size());
            }
            segmentStart = segmentStart2;
            tempOut2.close();
            tempSize = new File(tempFileName).length();
            new File(tempFileName).delete();
            tempFileName += ".b";
        }
        final TreeSet<ChunkStream> segmentIn2 = new TreeSet<ChunkStream>();
        final long read2 = openSegments(segmentStart, segmentIn2, tempFileName, true);
        final DataOutputStream dataOut = new DataOutputStream(out);
        dataOut.write(DataTool.HEADER);
        writeVarLong(dataOut, size);
        Chunk last2 = null;
        final Iterator<Chunk> it2 = merge(segmentIn2);
        while (it2.hasNext()) {
            final Chunk c4 = it2.next();
            if (last2 == null) {
                last2 = c4;
            }
            else if (last2.compareTo(c4) == 0) {
                last2.idList.addAll(c4.idList);
            }
            else {
                last2.write(dataOut, false);
                last2 = c4;
            }
        }
        if (last2 != null) {
            last2.write(dataOut, false);
        }
        new File(tempFileName).delete();
        writeVarLong(dataOut, 0L);
        dataOut.flush();
    }
    
    private static long openSegments(final List<Long> segmentStart, final TreeSet<ChunkStream> segmentIn, final String tempFileName, final boolean readKey) throws IOException {
        long inPos = 0L;
        final int bufferTotal = 67108864;
        final int bufferPerStream = bufferTotal / segmentStart.size();
        for (int i = 0; i < segmentStart.size(); ++i) {
            final InputStream in = new FileInputStream(tempFileName);
            in.skip(segmentStart.get(i));
            final ChunkStream s = new ChunkStream(i);
            s.readKey = readKey;
            s.in = new DataInputStream(new BufferedInputStream(in, bufferPerStream));
            inPos += s.readNext();
            if (s.current != null) {
                segmentIn.add(s);
            }
        }
        return inPos;
    }
    
    private static Iterator<Chunk> merge(final TreeSet<ChunkStream> segmentIn) {
        return new Iterator<Chunk>() {
            @Override
            public boolean hasNext() {
                return !segmentIn.isEmpty();
            }
            
            @Override
            public Chunk next() {
                final ChunkStream s = segmentIn.first();
                segmentIn.remove(s);
                final Chunk c = s.current;
                final int len = s.readNext();
                if (s.current != null) {
                    segmentIn.add(s);
                }
                return c;
            }
        };
    }
    
    private static int readFully(final InputStream in, final byte[] buffer, final int max) throws IOException {
        int result = 0;
        int l;
        for (int len = Math.min(max, buffer.length); len > 0; len -= l) {
            l = in.read(buffer, result, len);
            if (l < 0) {
                break;
            }
            result += l;
        }
        return result;
    }
    
    private static int[] getKey(final byte[] data, final int start, final int maxPos) {
        final int minLen = 4096;
        final int mask = 4095;
        long min = Long.MAX_VALUE;
        int pos = start;
        for (int j = 0; pos < maxPos; ++pos, ++j) {
            if (pos > start + 10) {
                final long hash = getSipHash24(data, pos - 10, pos, 111L, 11224L);
                if (hash < min) {
                    min = hash;
                }
                if (j > minLen) {
                    if ((hash & mask) == 0x1L) {
                        break;
                    }
                    if (j > minLen * 4 && (hash & mask >> 1) == 0x1L) {
                        break;
                    }
                    if (j > minLen * 16) {
                        break;
                    }
                }
            }
        }
        final int len = pos - start;
        final int[] counts = new int[8];
        for (int i = start; i < pos; ++i) {
            final int x = data[i] & 0xFF;
            final int[] array = counts;
            final int n = x >> 5;
            ++array[n];
        }
        int cs = 0;
        for (int k = 0; k < 8; ++k) {
            cs *= 2;
            if (counts[k] > len / 32) {
                ++cs;
            }
        }
        final int[] key = { (int)(min >>> 32), (int)min, cs, len };
        return key;
    }
    
    private static long getSipHash24(final byte[] b, final int start, final int end, final long k0, final long k1) {
        long v0 = k0 ^ 0x736F6D6570736575L;
        long v2 = k1 ^ 0x646F72616E646F6DL;
        long v3 = k0 ^ 0x6C7967656E657261L;
        long v4 = k1 ^ 0x7465646279746573L;
        for (int off = start; off <= end + 8; off += 8) {
            long m;
            int repeat;
            if (off <= end) {
                m = 0L;
                int i;
                for (i = 0; i < 8 && off + i < end; ++i) {
                    m |= (b[off + i] & 0xFFL) << 8 * i;
                }
                if (i < 8) {
                    m |= end - start << 56;
                }
                v4 ^= m;
                repeat = 2;
            }
            else {
                m = 0L;
                v3 ^= 0xFFL;
                repeat = 4;
            }
            for (int i = 0; i < repeat; ++i) {
                v0 += v2;
                v3 += v4;
                v2 = Long.rotateLeft(v2, 13);
                v4 = Long.rotateLeft(v4, 16);
                v2 ^= v0;
                v4 ^= v3;
                v0 = Long.rotateLeft(v0, 32);
                v3 += v2;
                v0 += v4;
                v2 = Long.rotateLeft(v2, 17);
                v4 = Long.rotateLeft(v4, 21);
                v2 ^= v3;
                v4 ^= v0;
                v3 = Long.rotateLeft(v3, 32);
            }
            v0 ^= m;
        }
        return v0 ^ v2 ^ v3 ^ v4;
    }
    
    private static int getHash(final long key) {
        int hash = (int)(key >>> 32 ^ key);
        hash = (hash >>> 16 ^ hash) * 73244475;
        hash = (hash >>> 16 ^ hash) * 73244475;
        hash ^= hash >>> 16;
        return hash;
    }
    
    static int writeVarLong(final OutputStream out, long x) throws IOException {
        int len;
        for (len = 0; (x & 0xFFFFFFFFFFFFFF80L) != 0x0L; x >>>= 7, ++len) {
            out.write((byte)(0x80L | (x & 0x7FL)));
        }
        out.write((byte)x);
        return ++len;
    }
    
    static long readVarLong(final InputStream in) throws IOException {
        long x = in.read();
        if (x < 0L) {
            throw new EOFException();
        }
        x = (byte)x;
        if (x >= 0L) {
            return x;
        }
        x &= 0x7FL;
        for (int s = 7; s < 64; s += 7) {
            long b = in.read();
            if (b < 0L) {
                throw new EOFException();
            }
            b = (byte)b;
            x |= (b & 0x7FL) << s;
            if (b >= 0L) {
                break;
            }
        }
        return x;
    }
    
    static {
        HEADER = new byte[] { 72, 50, 65, 49 };
    }
    
    static class ChunkStream implements Comparable<ChunkStream>
    {
        final int id;
        Chunk current;
        DataInputStream in;
        boolean readKey;
        
        ChunkStream(final int id) {
            this.id = id;
        }
        
        int readNext() {
            this.current = null;
            this.current = Chunk.read(this.in, this.readKey);
            if (this.current == null) {
                return 0;
            }
            return this.current.value.length;
        }
        
        @Override
        public int compareTo(final ChunkStream o) {
            final int comp = this.current.compareTo(o.current);
            if (comp != 0) {
                return comp;
            }
            return Integer.signum(this.id - o.id);
        }
    }
    
    static class Chunk implements Comparable<Chunk>
    {
        ArrayList<Long> idList;
        final byte[] value;
        private final int[] sortKey;
        
        Chunk(final ArrayList<Long> idList, final int[] sortKey, final byte[] value) {
            this.idList = idList;
            this.sortKey = sortKey;
            this.value = value;
        }
        
        public static Chunk read(final DataInputStream in, final boolean readKey) {
            try {
                final ArrayList<Long> idList = new ArrayList<Long>();
                while (true) {
                    final long x = DataTool.readVarLong(in);
                    if (x == 0L) {
                        break;
                    }
                    idList.add(x);
                }
                if (idList.isEmpty()) {
                    in.close();
                    return null;
                }
                int[] key = null;
                if (readKey) {
                    key = new int[4];
                    for (int i = 0; i < key.length; ++i) {
                        key[i] = in.readInt();
                    }
                }
                final int len = (int)DataTool.readVarLong(in);
                final byte[] value = new byte[len];
                in.readFully(value);
                return new Chunk(idList, key, value);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        int write(final DataOutputStream out, final boolean writeKey) throws IOException {
            int len = 0;
            for (final long x : this.idList) {
                len += DataTool.writeVarLong(out, x);
            }
            len += DataTool.writeVarLong(out, 0L);
            if (writeKey) {
                for (int i = 0; i < this.sortKey.length; ++i) {
                    out.writeInt(this.sortKey[i]);
                    len += 4;
                }
            }
            len += DataTool.writeVarLong(out, this.value.length);
            out.write(this.value);
            len += this.value.length;
            return len;
        }
        
        @Override
        public int compareTo(final Chunk o) {
            if (this.sortKey == null) {
                final long a = this.idList.get(0);
                final long b = o.idList.get(0);
                if (a < b) {
                    return -1;
                }
                if (a > b) {
                    return 1;
                }
                return 0;
            }
            else {
                for (int i = 0; i < this.sortKey.length; ++i) {
                    if (this.sortKey[i] < o.sortKey[i]) {
                        return -1;
                    }
                    if (this.sortKey[i] > o.sortKey[i]) {
                        return 1;
                    }
                }
                if (this.value.length < o.value.length) {
                    return -1;
                }
                if (this.value.length > o.value.length) {
                    return 1;
                }
                for (int i = 0; i < this.value.length; ++i) {
                    final int a2 = this.value[i] & 0xFF;
                    final int b2 = o.value[i] & 0xFF;
                    if (a2 < b2) {
                        return -1;
                    }
                    if (a2 > b2) {
                        return 1;
                    }
                }
                return 0;
            }
        }
    }
}
