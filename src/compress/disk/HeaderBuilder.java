package compress.disk;

public class HeaderBuilder {
    public final static int HEADER_SIZE = 49;

    public long originalSize;
    public String fingerprint;

    public byte[] getBytes() {
        byte[] headerBytes = new byte[HEADER_SIZE];

        headerBytes[0] = 66;
        headerBytes[1] = 72;
        headerBytes[2] = 83;
        int i = 3;

        long oSize = originalSize;
        int b;
        while (oSize > 0) {
            b = (int) oSize % 256;
            headerBytes[i++] = (byte) (b > Byte.MAX_VALUE ? b - 256 : b);
            oSize /= 256;
        }
        while (i < 9) headerBytes[i++] = 0;

        byte[] fb = fingerprint.getBytes();
        for (int j = 0; j < 40; j += 1) {
            headerBytes[i++] = fb[j];
        }

        return headerBytes;
    }
}
