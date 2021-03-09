package helpers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha160Sum {
    private MessageDigest mesDig;

    public Sha160Sum() {
        try {
            mesDig = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public Sha160Sum update(byte b) {
        mesDig.update(b);

        return this;
    }

    public Sha160Sum update(byte[] bytes) {
        mesDig.update(bytes);

        return this;
    }

    public Sha160Sum update(byte[] bytes, int i, int i1) {
        while (i < i1)
            mesDig.update(bytes[i++]);

        return this;
    }

    public String digest() {
        return convertBytesToHex(mesDig.digest());
    }

    public void reset() {
        mesDig.reset();
    }

    private static String convertBytesToHex(byte[] data) {
        StringBuilder hexData = new StringBuilder();
        for (byte aData : data)
            hexData.append(Integer.toString((aData & 0xff) + 0x100, 16).substring(1));

        return hexData.toString();
    }
}
