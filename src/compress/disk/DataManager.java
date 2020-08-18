package compress.disk;

import compress.CompressMode;
import helpers.ShaSum;

import java.io.*;

public class DataManager {
    public static CompressMode compressMode;
    public boolean virtualWriterMode;
    public final ShaSum inputShaSum = new ShaSum();
    public final ShaSum outputShaSum = new ShaSum();

    private static Cache cache = null;
    private FileInputStream fileInputStream;
    private FileOutputStream fileOutputStream;
    private final File source;
    private final File destination;

    private final long sourceSize;
    private long destinationSize = 0;
    private int sections = 0;
    private int maxSectionSize = 0;

    public DataManager(File source, File destination) {
        this.source = source;
        this.destination = destination;
        sourceSize = source.length();

        if (cache == null) {
            cache = new Cache();
        } else {
            cache.reset();
        }

        try {
            fileInputStream = new FileInputStream(source);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private byte esc() {
        return cache.outBuffer[0];
    }

    private boolean updateEscapeByte() {
        final byte esc = esc();
        int newEscape = esc + 1;
        int i;
        while (newEscape <= Byte.MAX_VALUE) {
            i = 1;
            while (i < cache.outMark) {
                if (cache.outBuffer[i] == esc) {
                    i += 2;
                } else {
                    if (cache.outBuffer[i] == newEscape) break;
                    i++;
                }
            }
            if (i == cache.outMark) {
                cache.outBuffer[0] = (byte) newEscape;
                i = 1;
                while (i < cache.outMark) {
                    if (cache.outBuffer[i] == esc) {
                        cache.outBuffer[i] = (byte) newEscape;
                        i += 2;
                    } else {
                        i++;
                    }
                }
                return true;
            } else {
                newEscape++;
            }
        }
        return false;
    }

    private void newSection() {
        cache.outBuffer[cache.outMark++] = esc();
        cache.outBuffer[cache.outMark++] = Constants.SECTION_END_BYTE;
        try {
            sendOutSection();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
        }
        cache.outBuffer[0] = Byte.MIN_VALUE;
        cache.outMark = 1;
        sections++;
    }

    private void sendOutSection() throws IOException {
        if (!virtualWriterMode) {
            fileOutputStream.write(cache.outBuffer, 0, cache.outMark);
        }
        destinationSize += cache.outMark;

        if (cache.outMark > maxSectionSize) maxSectionSize = cache.outMark;
    }

    private void sendOut(byte b) throws IOException {
        if (!virtualWriterMode) {
            fileOutputStream.write(b);
        }
        destinationSize++;
    }

    private void sendOut(byte[] bytes) throws IOException {
        if (!virtualWriterMode) {
            fileOutputStream.write(bytes);
        }
        destinationSize += bytes.length;
    }

    private void write(byte b, boolean direct) {
        if (!direct)
            while (b == esc()) {
                if (!updateEscapeByte()) {
                    newSection();
                }
            }

        cache.outBuffer[cache.outMark++] = b;
        if (cache.outMark == cache.outBuffer.length - 2)
            newSection();
    }

    private File makeResultFile(byte[] header, File suffix) {
        File result = new File(suffix.getAbsolutePath().concat("^.cResult"));

        try {
            FileInputStream fIn = new FileInputStream(suffix);
            FileOutputStream fOut = new FileOutputStream(result);

            fOut.write(header);
            outputShaSum.update(header);
            int readBytes;
            while ((readBytes = fIn.read(cache.inBuffer)) != -1) {
                fOut.write(cache.inBuffer, 0, readBytes);
                outputShaSum.update(cache.inBuffer, 0, readBytes);
            }

            fIn.close();
            fOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
        }

        return result;
    }

    public int fetchBlock() {
        for (cache.blockMark = 0; cache.blockMark < cache.block.length; cache.blockMark++) {
            if (cache.inMark < cache.inBytes) {
                cache.block[cache.blockMark] = cache.inBuffer[cache.inMark++];
            } else {
                try {
                    cache.inBytes = fileInputStream.read(cache.inBuffer);
                    cache.inMark = 0;
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(2);
                }
                if (cache.inBytes != -1) {
                    cache.block[cache.blockMark] = cache.inBuffer[cache.inMark++];
                    if (virtualWriterMode) {
                        inputShaSum.update(cache.inBuffer, 0, cache.inBytes);
                    }
                } else break;
            }
        }
        return cache.blockMark;
    }

    public long getBlockId() {
        long id = 0;
        int b;
        for (cache.blockMark = 0; cache.blockMark < cache.block.length; cache.blockMark++) {
            b = cache.block[cache.blockMark];
            if (b < 0) b += 256;
            id += b * Math.pow(256, cache.blockMark);
        }
        return id;
    }

    public void initWriter(byte versionAndILR, byte[] matrix) {
        try {
            fileOutputStream = new FileOutputStream(destination);
            sendOut(versionAndILR);
            sendOut(matrix);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
        }

        cache.outBuffer[0] = Byte.MIN_VALUE;
        cache.outMark = 1;
        sections++;
    }

    public void writeBlock() {
        for (int i = 0; i < cache.block.length; i++)
            write(cache.block[i], false);
    }

    public void writeCompressedBlock(int index) {
        if (index > Byte.MAX_VALUE) index -= 256;
        write(esc(), true);
        write((byte) index, true);
    }

    public void flush() {
        if (cache.blockMark != cache.block.length) {
            for (int i = 0; i < cache.blockMark; i++)
                write(cache.block[i], false);
        }

        try {
            sendOutSection();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
        }
    }

    public void rewind() {
        cache.reset();

        try {
            fileInputStream.close();
            fileInputStream = new FileInputStream(source);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
        }

        destinationSize = 0;
        sections = 0;
        maxSectionSize = 0;
    }

    public void close(boolean inputOnly) {
        try {
            fileInputStream.close();
            if (!inputOnly) fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
        }
    }

    public long getSourceSize() {
        return sourceSize;
    }

    public long getDestinationSize() {
        return destinationSize;
    }

    public int getSections() {
        return sections;
    }

    public int getMaxSectionSize() {
        return maxSectionSize;
    }

    public boolean deleteSource() {
        return source.delete();
    }

    public boolean deleteDestination() {
        return destination.delete();
    }

    public boolean sourceSaveAs(File output, HeaderBuilder headerBuilder) {
        File saved = makeResultFile(headerBuilder.getBytes(), source);
        return source.delete() && saved.renameTo(output);
    }

    public boolean destinationSaveAs(File output, HeaderBuilder headerBuilder) {
        File saved = makeResultFile(headerBuilder.getBytes(), destination);
        return source.delete() && saved.renameTo(output);
    }

    private static class Cache {
        final byte[] inBuffer;
        final byte[] block;
        final byte[] outBuffer;

        int inBytes;
        int inMark;
        int blockMark;
        int outMark;

        Cache() {
            inBuffer = new byte[compressMode.BUFFER_LEN];
            block = new byte[compressMode.BLOCK_LEN];
            outBuffer = new byte[compressMode.BUFFER_LEN];
            reset();
        }

        void reset() {
            inBytes = 0;
            inMark = 0;
            blockMark = 0;
            outMark = 0;
        }
    }

    private static class Constants {
        static final byte SECTION_END_BYTE = -1;
    }
}
