package com.example.buddychat.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

// =======================================================================
// Angle Buffer
// =======================================================================
/**
 * Thread-safe bounded ring buffer for angle samples (degrees). <ul>
 *     <li> push() overwrites oldest when full </li>
 *     <li> averageCircular() computes correct mean across 0/360 wrap </li>
 *     <li> averageCircularAndClear() atomically grabs & clears </li>
 * </ul>
 */
public final class AngleBuffer {
    private final float[]       buf;
    private final int           capacity;
    private final ReentrantLock lock = new ReentrantLock();

    // Ring state
    private int head = 0;  // index of oldest element
    private int size = 0;  // number of valid elements

    // Configuration
    private final boolean normalizeTo360;  // normalize angles into [0,360)
    private final Float   nullSentinel;    // e.g., -100f means "ignore"; null to disable

    // -----------------------------------------------------------------------
    // Instantiate
    // -----------------------------------------------------------------------
    /**
     * @param capacity        max number of samples to retain (>=1)
     * @param normalizeTo360  if true, angles are normalized into [0, 360)
     * @param nullSentinel    if non-null, samples equal to this value are ignored (e.g., -100f)
     */
    public AngleBuffer(int capacity, boolean normalizeTo360, Float nullSentinel) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be >= 1");
        this.buf = new float[capacity];
        this.capacity = capacity;
        this.normalizeTo360 = normalizeTo360;
        this.nullSentinel = nullSentinel;
    }

    /** Convenience ctor: capacity N, normalize to [0,360), ignore -100f. */
    public static AngleBuffer defaultAudio(int capacity) {
        return new AngleBuffer(capacity, /*normalizeTo360=*/true, /*nullSentinel=*/-100f);
    }

    /** Push a sample; overwrites oldest when full. Ignores nullSentinel/NaN. */
    public void push(float angleDeg) {
        if (Float.isNaN(angleDeg)) return;
        if (nullSentinel != null && angleDeg == nullSentinel) return;

        final float a = normalizeTo360 ? norm360(angleDeg) : angleDeg;

        lock.lock();
        try {
            int tail = (head + size) % capacity; // next write position
            buf[tail] = a;
            if (size == capacity) {
                // buffer full -> advance head (drop oldest)
                head = (head + 1) % capacity;
            } else { size++; }
        } finally { lock.unlock(); }
    }

    /** Clear all samples. */
    public void clear() { lock.lock(); try { head = 0; size = 0; } finally { lock.unlock(); } }

    /** Current number of samples. */
    public int size() { lock.lock(); try { return size; } finally { lock.unlock(); } }

    /** Snapshot copy of current samples (oldest -> newest). */
    public List<Float> snapshot() { lock.lock();
        try {
            List<Float> out = new ArrayList<>(size);
            for (int i = 0; i < size; i++) { out.add(buf[(head + i) % capacity]); }
            return out;
        } finally { lock.unlock(); }
    }

    /**
     * Atomically grab & clear all samples (oldest -> newest).
     * Faster than snapshot() + clear().
     */
    public List<Float> drain() { lock.lock();
        try {
            List<Float> out = new ArrayList<>(size);
            for (int i = 0; i < size; i++) { out.add(buf[(head + i) % capacity]); }
            head = 0; size = 0;
            return out;
        } finally { lock.unlock(); }
    }

    /** Linear mean (not wrap-safe). Useful if angles are already unwrapped. */
    public float averageLinear() { lock.lock();
        try {
            if (size == 0) return 0f;
            double sum = 0;
            for (int i = 0; i < size; i++) sum += buf[(head + i) % capacity];
            return (float) (sum / size);
        } finally { lock.unlock(); }
    }

    /** Proper circular mean (wrap-safe for degrees). Returns in [-180,180) if normalizeTo360, else raw domain. */
    public float averageCircular() { lock.lock();
        try {
            if (size == 0) return 0f;
            double sx = 0, sy = 0;
            for (int i = 0; i < size; i++) {
                double r = Math.toRadians(buf[(head + i) % capacity]);
                sx += Math.cos(r);
                sy += Math.sin(r);
            }
            float deg = (float) Math.toDegrees(Math.atan2(sy, sx));
            // return normalized to [-180, 180)
            if (normalizeTo360) {
                // convert [-180,180) to [0,360)
                return norm360(deg);
            }
            return deg;
        } finally { lock.unlock(); }
    }

    /** Atomically compute circular mean and clear buffer. */
    public float averageCircularAndClear() { lock.lock();
        try {
            float mean = averageCircular(); // safe: we hold lock, so this uses current state
            head = 0; size = 0;
            return mean;
        } finally { lock.unlock(); }
    }


    private static float norm360(float deg) {
        float m = deg % 360f;
        if (m < 0) m += 360f;
        return m;
    }

}
