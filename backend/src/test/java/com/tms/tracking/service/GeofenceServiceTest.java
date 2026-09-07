package com.tms.tracking.service;

import static org.junit.jupiter.api.Assertions.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tms.basic.entity.Geofence;
import com.tms.basic.mapper.GeofenceMapper;
import com.tms.dispatch.entity.Waybill;
import com.tms.tracking.entity.GeofenceState;
import com.tms.tracking.mapper.GeofenceAlertMapper;
import com.tms.tracking.mapper.GeofenceStateMapper;
import com.tms.tracking.mapper.TrackingEventMapper;
import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GeofenceServiceTest {
    @Test
    void circleHaversine() {
        Geofence f = new Geofence();
        f.setType("CIRCLE");
        f.setCenterLng(new BigDecimal("121.47"));
        f.setCenterLat(new BigDecimal("31.23"));
        f.setRadiusM(new BigDecimal("1000"));
        assertTrue(GeofenceService.contains(f, new BigDecimal("121.47"), new BigDecimal("31.231")));
        assertFalse(GeofenceService.contains(f, new BigDecimal("121.47"), new BigDecimal("31.25")));
    }

    @Test
    void polygonRayCast() {
        Geofence f = new Geofence();
        f.setType("POLYGON");
        f.setPolygon("[[0,0],[10,0],[10,10],[0,10]]");
        assertTrue(GeofenceService.contains(f, new BigDecimal("5"), new BigDecimal("5")));
        assertFalse(GeofenceService.contains(f, new BigDecimal("15"), new BigDecimal("5")));
    }

    @Test
    void enterExitStateMachine() {
        Geofence f = new Geofence();
        f.setCode("GF");
        f.setName("f");
        f.setType("CIRCLE");
        f.setCenterLng(BigDecimal.ZERO);
        f.setCenterLat(BigDecimal.ZERO);
        f.setRadiusM(new BigDecimal("1000"));
        f.setAlertOnEnter(true);
        f.setAlertOnExit(true);
        f.setStatus("ENABLED");
        GeofenceMapper fm = Mockito.mock(GeofenceMapper.class);
        GeofenceStateMapper sm = Mockito.mock(GeofenceStateMapper.class);
        GeofenceAlertMapper am = Mockito.mock(GeofenceAlertMapper.class);
        TrackingEventMapper em = Mockito.mock(TrackingEventMapper.class);
        Mockito.when(fm.selectList(Mockito.any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(f));
        Waybill w = new Waybill();
        w.setId(1L);
        w.setCode("WB");
        Mockito.when(sm.selectOne(Mockito.any(LambdaQueryWrapper.class)))
                .thenReturn(null)
                .thenAnswer(
                        x -> {
                            GeofenceState s = new GeofenceState();
                            s.setInside(true);
                            return s;
                        });
        GeofenceService service = new GeofenceService(fm, sm, am, em);
        assertEquals(0, service.onPosition(w, BigDecimal.ZERO, BigDecimal.ZERO).size());
        assertEquals(1, service.onPosition(w, new BigDecimal(".02"), BigDecimal.ZERO).size());
    }
}
