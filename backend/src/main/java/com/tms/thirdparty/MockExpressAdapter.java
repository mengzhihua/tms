package com.tms.thirdparty;

import com.tms.dispatch.entity.Waybill;
import com.tms.order.entity.TransportOrder;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockExpressAdapter implements ThirdPartyLogisticsAdapter {
    private static final int NODE_INTERVAL_SECONDS = 30;

    private final String provider;
    private final Map<String, LocalDateTime> shipmentTimes = new HashMap<>();

    public MockExpressAdapter(String provider) {
        this.provider = provider;
    }

    public String provider() {
        return provider;
    }

    public ShipmentResult createShipment(Waybill w, List<TransportOrder> os) {
        ShipmentResult r = new ShipmentResult();
        String thirdPartyNo = prefix() + "-" + System.currentTimeMillis();
        shipmentTimes.put(thirdPartyNo, LocalDateTime.now());
        r.setThirdPartyNo(thirdPartyNo);
        r.setStatus("ACCEPTED");
        return r;
    }

    public List<TrackEvent> queryTrack(String thirdPartyNo) {
        LocalDateTime baseTime = shipmentTimes.get(thirdPartyNo);
        if (baseTime == null) {
            baseTime = LocalDateTime.now();
        }

        String[] statusCodes = {"ACCEPTED", "IN_TRANSIT", "ARRIVED", "OUT_FOR_DELIVERY", "SIGNED"};
        String[] descriptions = {"已揽收", "运输中", "到达目的城市", "派送中", "已签收"};
        long elapsedSeconds = Duration.between(baseTime, LocalDateTime.now()).getSeconds();
        long cappedIndex =
                Math.min(statusCodes.length - 1, elapsedSeconds / NODE_INTERVAL_SECONDS);
        int currentIndex = (int) cappedIndex;

        List<TrackEvent> events = new ArrayList<>();
        for (int i = 0; i <= currentIndex; i++) {
            TrackEvent event = new TrackEvent();
            event.setStatusCode(statusCodes[i]);
            event.setDescription(descriptions[i]);
            event.setEventTime(baseTime.plusSeconds((long) i * NODE_INTERVAL_SECONDS));
            events.add(event);
        }
        return events;
    }

    public void cancel(String no) {}

    private String prefix() {
        return provider.endsWith("JD") ? "JD" : provider.endsWith("ZTO") ? "ZTO" : "SF";
    }
}
