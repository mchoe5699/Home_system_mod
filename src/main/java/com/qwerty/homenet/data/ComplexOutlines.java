package com.qwerty.homenet.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ComplexOutlines {
    private ComplexOutlines() {
    }

    public static List<ComplexOutlines.Segment> of(ZoneData.Zone owner, List<ZoneData.Zone> parcels) {
        List<ComplexOutlines.Segment> edges = new ArrayList<>();
        long left = owner.x1();
        long right = owner.x2() + 1L;
        long top = owner.z1();
        long bottom = owner.z2() + 1L;

        for (ComplexOutlines.Side side : ComplexOutlines.Side.values()) {
            boolean horizontal = side == ComplexOutlines.Side.TOP || side == ComplexOutlines.Side.BOTTOM;
            long start = horizontal ? left : top;
            long end = horizontal ? right : bottom;

            long fixed = switch (side) {
                case TOP -> top;
                case BOTTOM -> bottom;
                case LEFT -> left;
                case RIGHT -> right;
            };
            List<ComplexOutlines.Interval> covered = new ArrayList<>();
            if (!owner.complex().isEmpty()) {
                for (ZoneData.Zone other : parcels) {
                    if (other.id() != owner.id() && owner.complex().equals(other.complex())) {
                        long x1 = other.x1();
                        long x2 = other.x2() + 1L;
                        long z1 = other.z1();
                        long z2 = other.z2() + 1L;

                        boolean outsideOccupied = switch (side) {
                            case TOP -> z1 < fixed && z2 >= fixed;
                            case BOTTOM -> z1 <= fixed && z2 > fixed;
                            case LEFT -> x1 < fixed && x2 >= fixed;
                            case RIGHT -> x1 <= fixed && x2 > fixed;
                        };
                        if (outsideOccupied) {
                            long a = Math.max(start, horizontal ? x1 : z1);
                            long b = Math.min(end, horizontal ? x2 : z2);
                            if (a < b) {
                                covered.add(new ComplexOutlines.Interval(a, b));
                            }
                        }
                    }
                }
            }

            covered.sort(Comparator.comparingLong(ComplexOutlines.Interval::start));
            long cursor = start;

            for (ComplexOutlines.Interval interval : covered) {
                if (interval.start > cursor) {
                    add(edges, side, fixed, cursor, interval.start);
                }

                cursor = Math.max(cursor, interval.end);
            }

            if (cursor < end) {
                add(edges, side, fixed, cursor, end);
            }
        }

        return List.copyOf(edges);
    }

    private static void add(List<ComplexOutlines.Segment> edges, ComplexOutlines.Side side, long fixed, long start, long end) {
        edges.add(
            side != ComplexOutlines.Side.TOP && side != ComplexOutlines.Side.BOTTOM
                ? new ComplexOutlines.Segment(fixed, start, fixed, end, side)
                : new ComplexOutlines.Segment(start, fixed, end, fixed, side)
        );
    }

    public static int color(ZoneData.Zone owner, List<ZoneData.Zone> parcels) {
        return owner.complex().isEmpty()
            ? owner.color()
            : parcels.stream()
                .filter(p -> owner.complex().equals(p.complex()))
                .min(Comparator.comparingInt(ZoneData.Zone::id))
                .map(ZoneData.Zone::color)
                .orElse(owner.color());
    }

    private record Interval(long start, long end) {
    }

    public record Segment(long x1, long z1, long x2, long z2, ComplexOutlines.Side side) {
    }

    public static enum Side {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT;
    }
}
