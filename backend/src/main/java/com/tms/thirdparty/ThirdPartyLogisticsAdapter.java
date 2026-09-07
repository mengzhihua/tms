package com.tms.thirdparty;

import com.tms.dispatch.entity.Waybill;
import com.tms.order.entity.TransportOrder;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

public interface ThirdPartyLogisticsAdapter {
    String provider();

    ShipmentResult createShipment(Waybill waybill, List<TransportOrder> orders);

    List<TrackEvent> queryTrack(String thirdPartyNo);

    void cancel(String thirdPartyNo);

    @Data
    class ShipmentResult {
        private String thirdPartyNo;
        private String status;
    }

    @Data
    class TrackEvent {
        private String statusCode, description;
        private LocalDateTime eventTime;
    }
}
