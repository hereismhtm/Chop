import helpers.ShaSum;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

public class XChop {
    static ShaSum shaSum = new ShaSum();
    static final byte[] inBuffer = new byte[268435456];
    static final byte[] outBuffer = new byte[268435456];
    static int inBytes;
    static int inMark;
    static int outMark;
    static FileOutputStream fileOutputStream;

    static int round = 0;
    static boolean isLastRound;
    static long compressedSize;
    static long originalSize;
    static long extractedSize;
    static String fingerprint;

    public static void main(String[] args) {
        File input;
        File output;

        switch (args.length) {
            case 0:
                System.out.println("(ง'̀-'́)ง input file are not mentioned.");
                break;

            case 1:
            case 2:
                input = new File(args[0]);
                if (args.length == 2) {
                    output = new File(args[1]);
                } else if (args[0].endsWith(Constants.EXT)) {
                    output = new File(args[0].substring(0, args[0].lastIndexOf(Constants.EXT)));
                } else {
                    output = new File(args[0] + "^" + Constants.EXT + "Extract");
                }

                if (!input.isFile())
                    System.out.println("¯\\_(ツ)_/¯ input file not found.");
                else {
                    System.out.println("input file: " + input.getAbsolutePath());
                    System.out.println("output file: " + output.getAbsolutePath());

                    if (extract(input, output)) {
                        if (extractedSize == -1) {
                            System.exit(0);
                        }
                        System.out.println("\nƪ(˘⌣˘)ʃ Complete \u2714 ");
                        System.out.print("extracted data size: " + extractedSize + " bytes \u2764 ");
                        System.out.println(readableSize(extractedSize));
                        System.out.println("data origination: " + dataOrigination());
                        System.out.println("sha1 fingerprint: " + shaSum.digest());
                    } else {
                        System.out.println("\n(ಥ﹏ಥ) PROCESS FAILED!");
                    }

                    System.out.println("total rounds: " + round);
                }
                break;
        }
    }

    static String dataOrigination() {
        double percent = compressedSize / (double) extractedSize * 100;
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

    static boolean extract(File input, File output) {
        round++;
        File roundFile = new File(output.getAbsolutePath().concat("^.xR" + round));

        try {
            FileInputStream fileInputStream = new FileInputStream(input.getAbsoluteFile());
            if (round == 1) {
                compressedSize = input.length();
                if (fileInputStream.read(inBuffer, 0, 49) != 49
                        || inBuffer[0] != 66
                        || inBuffer[1] != 72
                        || inBuffer[2] != 83
                ) {
                    fileInputStream.close();
                    round = 0;
                    return false;
                }
                inMark = 3;
                originalSize = 0;
                while (inMark < 9) {
                    int b = inBuffer[inMark];
                    if (b < 0) b += 256;
                    originalSize += b * Math.pow(256, inMark - 3);
                    inMark++;
                }
                fingerprint = new String(Arrays.copyOfRange(inBuffer, 9, 49));

                System.out.println("input size: " + readableSize(compressedSize));
                System.out.print("and " + readableSize(originalSize) + " of data will be extracted, ");
                System.out.print("fire on hole? (press 'y' for \"Yes\") ");
                char keyPicked = (char) System.in.read();
                if (!String.valueOf(keyPicked).toLowerCase().equals("y")) {
                    System.out.println("--> No");
                    fileInputStream.close();
                    extractedSize = -1;
                    return true;
                }
                System.out.println("--> Yes");
                System.out.println("\nKABOOM!");
            }
            inBytes = fileInputStream.read(inBuffer);
            inMark = 0;
            fileOutputStream = new FileOutputStream(roundFile);
            outMark = 0;
            extractedSize = 0;

            isLastRound = inBuffer[inMark++] > 0;
            int matrixBlocks = inBuffer[inMark++];
            if (matrixBlocks < 0) matrixBlocks += 256;
            int blockLength = inBuffer[inMark++];
            byte[] matrix = new byte[matrixBlocks * blockLength];
            int index = 0;
            int stopIndex = matrix.length;
            while (index < stopIndex) {
                matrix[index++] = inBuffer[inMark++];
            }

            int escapeMode = 0;
            byte b;
            byte esc = inBuffer[inMark++];
            do {
                b = inBuffer[inMark++];

                if (escapeMode == -1) {
                    escapeMode = 0;
                    esc = b;
                } else if (escapeMode == 1) {
                    if (b == Constants.SECTION_END_BYTE) {
                        escapeMode = -1;
                    } else {
                        escapeMode = 0;
                        index = b < 0 ? b + 256 : b;
                        index *= blockLength;
                        stopIndex = index + blockLength;
                        while (index < stopIndex) {
                            write(matrix[index++]);
                        }
                    }
                } else if (b == esc) {
                    escapeMode = 1;
                } else {
                    write(b);
                }

                if (inMark == inBytes) {
                    inMark = (inBytes = fileInputStream.read(inBuffer)) != -1 ? 0 : -1;
                }
            } while (inMark != -1);
            if (outMark > 0) {
                fileOutputStream.write(outBuffer, 0, outMark);
                if (isLastRound) shaSum.update(outBuffer, 0, outMark);
                extractedSize += outMark;
                outMark = 0;
            }
            fileInputStream.close();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
        }

        if (round != 1 && !input.delete()) {
            return false;
        } else if (isLastRound) {
            if (extractedSize == originalSize && shaSum.digest().equals(fingerprint)) {
                return roundFile.renameTo(output);
            } else {
//                roundFile.delete();
                return false;
            }
        } else {
            return extract(roundFile, output);
        }
    }

    static void write(byte b) throws IOException {
        outBuffer[outMark++] = b;
        if (outMark == outBuffer.length) {
            fileOutputStream.write(outBuffer);
            if (isLastRound) shaSum.update(outBuffer);
            extractedSize += outBuffer.length;
            outMark = 0;
        }
    }

    private static class Constants {
        static final String EXT = ".bhs";
        static final byte SECTION_END_BYTE = -1;
    }
}
