package com.tms.dispatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tms.dispatch.service.MapBinder.Assignment;
import com.tms.dispatch.service.MapBinder.Stop;
import com.tms.dispatch.service.MapBinder.Truck;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class MapBinderTest {
    @Test
    void nearerTruckTakesTheOrderUntilItIsFull() {
        Truck near = new Truck(1L, "沪A1", "4.2米", lng(121.47), lat(31.23), new BigDecimal("10"), new BigDecimal("20"));
        Truck far = new Truck(2L, "沪A2", "6.8米", lng(121.60), lat(31.40), new BigDecimal("100"), new BigDecimal("40"));
        Stop first = new Stop("TO-1", lng(121.47), lat(31.23), new BigDecimal("8"), new BigDecimal("1"), 1);
        Stop second = new Stop("TO-2", lng(121.47), lat(31.23), new BigDecimal("8"), new BigDecimal("1"), 1);
        List<Assignment> rows = MapBinder.bind(Arrays.asList(second, first), Arrays.asList(far, near));
        Assignment to1 = find(rows, "TO-1");
        Assignment to2 = find(rows, "TO-2");
        assertEquals(Long.valueOf(1L), to1.getVehicleId());
        assertEquals("绑到 沪A1，距离 0 公里", to1.getNote());
        assertEquals(Long.valueOf(2L), to2.getVehicleId());
    }

    @Test
    void missingPointOrCapacityStaysUnbound() {
        Truck truck = new Truck(1L, "沪A1", "4.2米", lng(121.47), lat(31.23), new BigDecimal("5"), new BigDecimal("5"));
        List<Assignment> rows = MapBinder.bind(
                Arrays.asList(
                        new Stop("TO-0", null, null, BigDecimal.ONE, BigDecimal.ONE, 0),
                        new Stop("TO-9", lng(121.47), lat(31.23), new BigDecimal("9"), BigDecimal.ONE, 0)),
                Collections.singletonList(truck));
        assertEquals("订单没有坐标", find(rows, "TO-0").getNote());
        assertNull(find(rows, "TO-0").getVehicleId());
        assertEquals("没有可绑车辆", find(rows, "TO-9").getNote());
    }

    private static Assignment find(List<Assignment> rows, String code) {
        for (Assignment row : rows) {
            if (code.equals(row.getOrderCode())) {
                return row;
            }
        }
        throw new AssertionError(code);
    }

    private static BigDecimal lng(double value) {
        return BigDecimal.valueOf(value);
    }

    private static BigDecimal lat(double value) {
        return BigDecimal.valueOf(value);
    }
}
