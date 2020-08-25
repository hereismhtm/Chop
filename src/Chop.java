import compress.CompressMode;
import compress.disk.DataManager;
import compress.disk.HeaderBuilder;

import compress.memory.Table;
import helpers.StopWatch;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

public class Chop {
    static final HeaderBuilder headerBuilder = new HeaderBuilder();
    static final long[] matrix = new long[0xff];
    static int round = 0;
    static long bestCompressedSize;
    static DataManager dm;
    static Table table;

    public static void main(String[] args) {
        CompressMode mode;
        File input;
        File output;

        switch (args.length) {
            case 0:
            case 1:
                System.out.println("(ง'̀-'́)ง compress mode and input file are not mentioned.");
                break;

            case 2:
            case 3:
                try {
                    mode = CompressMode.valueOf(args[0].toLowerCase());
                } catch (IllegalArgumentException e) {
                    mode = CompressMode.b3;
                    System.out.println("┬─┬ノ( º _ ºノ) compress mode is set to default one.");
                }
                input = new File(args[1]);
                output = new File(args.length == 3 ? args[2] : args[1].concat(Constants.EXT));

                if (!input.isFile())
                    System.out.println("¯\\_(ツ)_/¯ input file not found.");
                else {
                    System.out.println("mode: " + mode.name());
                    System.out.println("input file: " + input.getAbsolutePath());
                    System.out.println("output file: " + output.getAbsolutePath());

                    StopWatch stopWatch = new StopWatch();
                    stopWatch.start();
                    DataManager.compressMode = mode;
                    boolean success = compress(input, output);
                    stopWatch.stop();

                    if (success) {
                        System.out.println("\n(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧ Complete \u2714 ");
                        System.out.print("new smaller size: " + bestCompressedSize + " bytes \u2764 ");
                        System.out.println(readableSize(bestCompressedSize));
                        System.out.println("compress efficiency: " + outputEffAgainst(headerBuilder.originalSize));
                        System.out.println("sha1 fingerprint: " + dm.outputShaSum.digest());
                    } else {
                        System.out.println("\n(ಥ﹏ಥ) PROCESS FAILED!");
                    }

                    System.out.println("total rounds: " + round);
                    System.out.print("process time: ");
                    long minutes = stopWatch.getTimeMins();
                    long seconds = stopWatch.getTimeSecs();
                    seconds = seconds - (minutes * 60);
                    System.out.print(minutes + " minute and ");
                    System.out.println(seconds + " second");
                }
                break;
        }
    }

    static String outputEffAgainst(long size) {
        double percent = bestCompressedSize / (double) size * 100;
        return roundHalfUp2(100 - percent) + "%";
    }

    static String readableSize(long size) {
        double d = size;
        int i = 0;

        while (d >= 1024) {
            d /= 1024;
            i++;
        }
        d = roundHalfUp2(d);

        switch (i) {
            case 4:
                return String.valueOf(d).concat(" TB");
            case 3:
                return String.valueOf(d).concat(" GB");
            case 2:
                return String.valueOf(d).concat(" MB");
            case 1:
                return String.valueOf(d).concat(" KB");
            default:
                return "";
        }
    }

    static double roundHalfUp2(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    static boolean compress(File input, File output) {
        round++;
        File roundFile = new File(output.getAbsolutePath().concat("^.cR" + round));
        dm = new DataManager(input.getAbsoluteFile(), roundFile);
        if (table == null)
            table = new Table();
        else
            table.reset();

        if (round == 1) {
            headerBuilder.originalSize = dm.getSourceSize();
            bestCompressedSize = headerBuilder.originalSize;
            System.out.print("input size: " + headerBuilder.originalSize + " bytes ");
            System.out.println("/ " + readableSize(headerBuilder.originalSize));
        }
        System.out.println("\nROUND #" + round);

        System.out.println("\u2699 preparing ...");
        int matrixBlocks = buildMatrix();
        long domination = calculateMatrixDomination();
        System.out.println("\u27A5 |Done|");

        System.out.println(table.getCount() + " blocks in memory");
        System.out.println("matrix blocks [" + matrixBlocks + "]");
        System.out.println("domination " + domination);

        System.out.print("--> starting ");
        dm.virtualWriterMode = true;
        fullPassIndexer(matrixBlocks);
        if (round == 1) {
            headerBuilder.fingerprint = dm.inputShaSum.digest();
        }
        long nextCompressedSize = dm.getSourceSize()
                - domination
                + ((1 + dm.getSections()) * 3 - 2)
                + 1
                + HeaderBuilder.HEADER_SIZE;
        if (nextCompressedSize < bestCompressedSize) {
            dm.rewind();
            System.out.println("now");
        } else {
            dm.close(true);
            System.out.println("\n--> round " + round + " canceled");
            round--;
            return round != 0
                    && dm.sourceSaveAs(output, headerBuilder);
        }

        System.out.println("\u2699 compressing ...");
        dm.virtualWriterMode = false;
        dm.initWriter(
                (byte) (round == 1 ? Constants.VERSION_NUMBER : -Constants.VERSION_NUMBER),
                getMatrixBytes(matrixBlocks)
        );
        fullPassIndexer(matrixBlocks);
        dm.close(false);
        System.out.println("\u27A5 |Done|");

        if (dm.getDestinationSize() + HeaderBuilder.HEADER_SIZE == nextCompressedSize) {
            if (round != 1 && !dm.deleteSource()) {
                return false;
            } else {
                bestCompressedSize = dm.getDestinationSize() + HeaderBuilder.HEADER_SIZE;
                System.out.println(dm.getSections() + " sections, max " + dm.getMaxSectionSize());
                System.out.println("size down to " + bestCompressedSize + " bytes");
                System.out.println("efficiency " + outputEffAgainst(dm.getSourceSize()));
                if (bestCompressedSize <= Constants.SINGULARITY_SIZE) {
                    return dm.destinationSaveAs(output, headerBuilder);
                }
                return compress(roundFile, output);
            }
        } else {
            System.out.println("--> round " + round + " ignored");
            round--;
            return dm.deleteDestination()
                    && round != 0
                    && dm.sourceSaveAs(output, headerBuilder);
        }
    }

    static void fullPassIndexer(int matrixBlocks) {
        int index;
        int indexCorrector = matrix.length - matrixBlocks;

        while (dm.fetchBlock() == DataManager.compressMode.BLOCK_LEN) {
            index = Arrays.binarySearch(matrix, dm.getBlockId());
            if (index < 0) {
                dm.writeBlock();
            } else {
                index -= indexCorrector;
                dm.writeCompressedBlock(index);
            }
        }
        dm.flush();
    }

    static int buildMatrix() {
        while (dm.fetchBlock() == DataManager.compressMode.BLOCK_LEN) {
            table.updateValuePlus(dm.getBlockId());
        }
        dm.rewind();

        int mbc = 1 + DataManager.compressMode.BLOCK_LEN;
        mbc /= DataManager.compressMode.BLOCK_LEN - 2;

        int i;
        Arrays.fill(matrix, Constants.NO_BLOCK_ID);
        for (i = 0; i < matrix.length; i++) {
            matrix[i] = table.selectIdOfMaxValue(mbc);
            if (matrix[i] == -404) {
                matrix[i] = Constants.NO_BLOCK_ID;
                break;
            }
            table.updateValueNegative(matrix[i]);
//            System.out.print("BLK(" + matrix[i] + ")  ");
//            System.out.println("X" + table.selectValue(matrix[i]));
        }
        Arrays.sort(matrix);
        return i;
    }

    static long calculateMatrixDomination() {
        int bl = DataManager.compressMode.BLOCK_LEN;
        long domination = 0;
        for (long id : matrix) {
            if (id == Constants.NO_BLOCK_ID) continue;
            domination += table.selectValue(id) * (bl - 2) - bl;
        }
        return domination - 2;
    }

    static byte[] getMatrixBytes(int matrixBlocks) {
        byte[] m = new byte[2 + (matrixBlocks * DataManager.compressMode.BLOCK_LEN)];
        m[0] = (byte) (matrixBlocks > Byte.MAX_VALUE ? matrixBlocks - 256 : matrixBlocks);
        m[1] = (byte) DataManager.compressMode.BLOCK_LEN;
        int i = 2;
        for (long id : matrix) {
            if (id == Constants.NO_BLOCK_ID) continue;
//            System.out.println("BLK(" + id + ")");
            idExpander(id, m, i);
//            System.out.println("--------");
            i += DataManager.compressMode.BLOCK_LEN;
        }
        return m;
    }

    static void idExpander(long id, byte[] intoArray, int startIndex) {
        int endIndex = startIndex + DataManager.compressMode.BLOCK_LEN;
        int b;

        while (id > 0) {
            b = (int) (id % 256);
            if (b > Byte.MAX_VALUE) b -= 256;
            intoArray[startIndex++] = (byte) b;
//            System.out.println("byte " + b);
            id /= 256;
        }

        while (startIndex < endIndex) {
            intoArray[startIndex++] = 0;
//            System.out.println("byte 0");
        }
    }

    private static class Constants {
        static final String EXT = ".bhs";
        static final int VERSION_NUMBER = 1;
        static final int NO_BLOCK_ID = -1;
        static final int SINGULARITY_SIZE = 0/*1048576*/;
    }
}
