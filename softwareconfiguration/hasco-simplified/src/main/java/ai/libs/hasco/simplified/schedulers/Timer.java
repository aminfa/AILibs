package ai.libs.hasco.simplified.schedulers;

import java.util.function.Supplier;

class Timer {

    // Can be changed for tests to have control over time
    static Supplier<Long> TIME_SUPPLIER = System::currentTimeMillis;

    private Supplier<Long> currentTimeSupplier = TIME_SUPPLIER;

    private long timeStarted;

    private long maxTime;

    private boolean hasBeenInterrupted = false;
    private boolean hasEnded = false;

    private boolean hasStarted = false;

    public Timer(long maxTime) {
        this.maxTime = maxTime;
    }

    void start() {
        if(!hasStarted) {
            timeStarted = currentTimeSupplier.get();
            hasStarted = true;
        }
    }

    void interrupt() {
        hasBeenInterrupted = true;
    }

    void end() {
        hasEnded = true;
    }

    long maxTime() {
        return maxTime;
    }

    long timeoutTime() {
        return timeStarted + maxTime();
    }

    long runTime() {
        return currentTimeSupplier.get() - timeStarted;
    }

    boolean hasTimedOut() {
        if(hasBeenInterrupted)
            return false;
        if(!hasStarted)
            return false;

        return maxTime() < runTime();
    }

    long timeLeft() {
        return maxTime() - runTime();
    }

    public long getTimeStarted() {
        return timeStarted;
    }

    public boolean hasBeenInterrupted() {
        return hasBeenInterrupted;
    }

    public boolean hasStarted() {
        return hasStarted;
    }

    boolean hasEnded() {
        return hasEnded;
    }

    boolean hasFinished() {
        return hasBeenInterrupted() || hasEnded();
    }

    boolean isRunning() {
        return hasStarted() && !hasFinished();
    }
}
