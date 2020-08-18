package compress;

public enum CompressMode {
    b3(3, 536870912),
    b4(4, 1073741824),
    b5(5, 1610612736),
    b6(6, 1610612736);

    public final int BLOCK_LEN;
    public final int BUFFER_LEN;

    CompressMode(int blockLength, int bufferLength) {
        this.BLOCK_LEN = blockLength;
        this.BUFFER_LEN = bufferLength;
    }
}
