package me.myogoo.ae2fct.storage;

public final class FluidAccountingCheck {
    public static void main(String[] args) {
        for (int unit : new int[]{1, 2, 3, 1000, Integer.MAX_VALUE}) {
            for (long count : new long[]{Long.MIN_VALUE, -1, 0, 1, 2, Long.MAX_VALUE / unit, Long.MAX_VALUE}) {
                long target = FluidAccounting.requested(count, unit);
                check(target >= 0 && target % unit == 0, "bounded request");
                if (count <= 0) check(target == 0, "nonpositive request");
                for (long actual : new long[]{0, Math.min(1, target), target / 2, Math.max(0, target - 1), target}) {
                    var inserted = FluidAccounting.inserted(actual, unit, 0);
                    check(inserted.items() <= target / unit, "insert within offered count");
                    check(inserted.items() * unit - actual == inserted.remainder(), "insert conservation");
                    var extracted = FluidAccounting.extracted(actual, unit, 0);
                    check(actual - extracted.items() * unit == extracted.remainder(), "extract conservation");
                    check(inserted.remainder() < unit && extracted.remainder() < unit, "bounded remainder");
                }
            }
        }
        for (int actual = 0; actual <= 2000; actual++) {
            for (int before : new int[]{0, 1, 500, 999}) {
                var in = FluidAccounting.inserted(actual, 1000, before);
                check(in.items() * 1000 + before == actual + in.remainder(), "buffered insertion");
                var out = FluidAccounting.extracted(actual + before, 1000, 0);
                check(actual + before == out.items() * 1000 + out.remainder(), "combined extraction");
            }
        }
        System.out.println("FluidAccountingCheck passed: boundaries, overflow and all 0..2000 partial outcomes");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
