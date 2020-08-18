package helpers;

public class StopWatch {
    private long startTime = 0;
    private long stopTime = 0;
    private boolean running = false;

    public void start() {
        this.startTime = System.currentTimeMillis();
        this.running = true;
    }

    public void stop() {
        this.stopTime = System.currentTimeMillis();
        this.running = false;
    }

    // get time in milliseconds
    public long getTime() {
        long time;
        if (running) {
            time = (System.currentTimeMillis() - startTime);
        } else {
            time = (stopTime - startTime);
        }
        return time;
    }

    // get time in seconds
    public long getTimeSecs() {
        long time;
        if (running) {
            time = ((System.currentTimeMillis() - startTime) / 1000);
        } else {
            time = ((stopTime - startTime) / 1000);
        }
        return time;
    }

    // get time in minutes
    public long getTimeMins() {
        long time;
        if (running) {
            time = ((System.currentTimeMillis() - startTime) / 60000);
        } else {
            time = ((stopTime - startTime) / 60000);
        }
        return time;
    }
}
