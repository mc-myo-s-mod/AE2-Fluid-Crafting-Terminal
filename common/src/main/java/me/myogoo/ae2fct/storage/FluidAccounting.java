package me.myogoo.ae2fct.storage;

/** Whole-item accounting; the physical host owns the returned remainder. */
public final class FluidAccounting {
    private FluidAccounting() {
    }

    public record Result(long items, long remainder) {
    }

    public static long requested(long count, int unit) {
        if (unit <= 0) throw new IllegalArgumentException("Nonpositive fluid unit");
        return count <= 0 ? 0 : Math.min(count, Long.MAX_VALUE / unit) * unit;
    }

    public static Result inserted(long actual, int unit, long buffered) {
        long fraction = actual % unit;
        return new Result(actual / unit + (fraction == 0 ? 0 : 1),
                buffered + (fraction == 0 ? 0 : unit - fraction));
    }

    public static Result extracted(long actual, int unit, long unconsumedBuffer) {
        return new Result(actual / unit, unconsumedBuffer + actual % unit);
    }
}
