package com.tms.dispatch.service;

import com.tms.tracking.service.GeofenceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;

/** 按坐标把订单绑到最近、并且还装得下的空闲车。没有坐标或没有空位时不绑。 */
public final class MapBinder {
    private MapBinder() {}

    @Getter
    public static final class Stop {
        private final String code;
        private final BigDecimal lng;
        private final BigDecimal lat;
        private final BigDecimal weightKg;
        private final BigDecimal volumeM3;
        private final int priority;

        public Stop(String code, BigDecimal lng, BigDecimal lat, BigDecimal weightKg, BigDecimal volumeM3, int priority) {
            this.code = code;
            this.lng = lng;
            this.lat = lat;
            this.weightKg = weightKg;
            this.volumeM3 = volumeM3;
            this.priority = priority;
        }
    }

    @Getter
    public static final class Truck {
        private final Long id;
        private final String plateNo;
        private final String vehicleType;
        private final BigDecimal lng;
        private final BigDecimal lat;
        private final BigDecimal maxWeightKg;
        private final BigDecimal maxVolumeM3;

        public Truck(
                Long id,
                String plateNo,
                String vehicleType,
                BigDecimal lng,
                BigDecimal lat,
                BigDecimal maxWeightKg,
                BigDecimal maxVolumeM3) {
            this.id = id;
            this.plateNo = plateNo;
            this.vehicleType = vehicleType;
            this.lng = lng;
            this.lat = lat;
            this.maxWeightKg = maxWeightKg;
            this.maxVolumeM3 = maxVolumeM3;
        }
    }

    @Getter
    public static final class Assignment {
        private final String orderCode;
        private final Long vehicleId;
        private final String plateNo;
        private final String vehicleType;
        private final BigDecimal distanceKm;
        private final String note;
        private final BigDecimal orderLng;
        private final BigDecimal orderLat;
        private final BigDecimal vehicleLng;
        private final BigDecimal vehicleLat;

        private Assignment(
                String orderCode,
                Long vehicleId,
                String plateNo,
                String vehicleType,
                BigDecimal distanceKm,
                String note,
                BigDecimal orderLng,
                BigDecimal orderLat,
                BigDecimal vehicleLng,
                BigDecimal vehicleLat) {
            this.orderCode = orderCode;
            this.vehicleId = vehicleId;
            this.plateNo = plateNo;
            this.vehicleType = vehicleType;
            this.distanceKm = distanceKm;
            this.note = note;
            this.orderLng = orderLng;
            this.orderLat = orderLat;
            this.vehicleLng = vehicleLng;
            this.vehicleLat = vehicleLat;
        }
    }

    public static List<Assignment> bind(List<Stop> orders, List<Truck> vehicles) {
        List<Stop> sorted = new ArrayList<Stop>();
        if (orders != null) {
            sorted.addAll(orders);
        }
        sorted.sort(Comparator.comparingInt(Stop::getPriority).reversed().thenComparing(Stop::getCode, Comparator.nullsLast(String::compareTo)));
        List<Seat> seats = new ArrayList<Seat>();
        if (vehicles != null) {
            for (Truck truck : vehicles) {
                if (truck != null && truck.lng != null && truck.lat != null) {
                    seats.add(new Seat(truck));
                }
            }
        }
        List<Assignment> result = new ArrayList<Assignment>();
        for (Stop order : sorted) {
            if (order == null) {
                continue;
            }
            if (order.lng == null || order.lat == null) {
                result.add(unbound(order, "订单没有坐标"));
                continue;
            }
            Seat chosen = null;
            double best = Double.MAX_VALUE;
            for (Seat seat : seats) {
                if (!seat.fits(order)) {
                    continue;
                }
                double meters = GeofenceService.haversine(seat.truck.lat, seat.truck.lng, order.lat, order.lng);
                if (chosen == null || meters < best) {
                    chosen = seat;
                    best = meters;
                }
            }
            if (chosen == null) {
                result.add(unbound(order, "没有可绑车辆"));
                continue;
            }
            chosen.take(order);
            BigDecimal km = BigDecimal.valueOf(best / 1000d).setScale(1, RoundingMode.HALF_UP);
            result.add(new Assignment(
                    order.code,
                    chosen.truck.id,
                    chosen.truck.plateNo,
                    chosen.truck.vehicleType,
                    km,
                    "绑到 " + chosen.truck.plateNo + "，距离 " + km.stripTrailingZeros().toPlainString() + " 公里",
                    order.lng,
                    order.lat,
                    chosen.truck.lng,
                    chosen.truck.lat));
        }
        return result;
    }

    private static Assignment unbound(Stop order, String note) {
        return new Assignment(order.code, null, null, null, null, note, order.lng, order.lat, null, null);
    }

    private static final class Seat {
        private final Truck truck;
        private BigDecimal weightLeft;
        private BigDecimal volumeLeft;

        private Seat(Truck truck) {
            this.truck = truck;
            this.weightLeft = truck.maxWeightKg;
            this.volumeLeft = truck.maxVolumeM3;
        }

        private boolean fits(Stop order) {
            return room(weightLeft, order.weightKg) && room(volumeLeft, order.volumeM3);
        }

        private void take(Stop order) {
            weightLeft = spend(weightLeft, order.weightKg);
            volumeLeft = spend(volumeLeft, order.volumeM3);
        }

        private static boolean room(BigDecimal left, BigDecimal need) {
            if (left == null) {
                return true;
            }
            return left.compareTo(need == null ? BigDecimal.ZERO : need) >= 0;
        }

        private static BigDecimal spend(BigDecimal left, BigDecimal need) {
            if (left == null) {
                return null;
            }
            return left.subtract(need == null ? BigDecimal.ZERO : need);
        }
    }
}
