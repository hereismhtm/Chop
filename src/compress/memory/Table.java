package compress.memory;

import java.util.Arrays;

public class Table {
    private final int[] records = new int[16777216];
    private long count = 0;

    public void reset() {
        Arrays.fill(records, 0);
        count = 0;
    }

    public long getCount() {
        return count;
    }

    public void updateValuePlus(long id) {
        int i = (int) id;
        records[i]++;
        if (records[i] == 1) count++;
    }

    public void updateValueNegative(long id) {
        records[(int) id] = -records[(int) id];
    }

    public int selectValue(long id) {
        return Math.abs(records[(int) id]);
    }

    public long selectIdOfMaxValue(int condition) {
        int id = -404;
        int maxValue = 0;
        for (int i = 0; i < records.length; i++) {
            if (records[i] >= condition && records[i] > maxValue) {
                id = i;
                maxValue = records[i];
            }
        }
        return id;
    }
}
