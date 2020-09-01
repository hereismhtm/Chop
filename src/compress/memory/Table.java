package compress.memory;

import java.util.Arrays;

public class Table {
    private final int[] records = new int[16777216];

    public void reset() {
        Arrays.fill(records, 0);
    }

    public long getCount() {
        int count = 0;
        for (int record : records) {
            if (record != 0) count++;
        }
        return count;
    }

    public void updateValuePlus(long id) {
        records[(int) id]++;
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
            if (records[i] > maxValue && records[i] >= condition) {
                id = i;
                maxValue = records[i];
            }
        }
        return id;
    }
}
