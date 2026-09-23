package com.tms.thirdparty;

import com.tms.basic.entity.Carrier;
import com.tms.basic.mapper.CarrierMapper;
import com.tms.common.BizException;
import com.tms.dispatch.entity.Waybill;
import com.tms.integration.client.ExpressWaybillClient;
import com.tms.order.entity.TransportOrder;
import java.util.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ThirdPartyLogisticsGateway {
    private final CarrierMapper carrierMapper;
    private final List<ThirdPartyLogisticsAdapter> adapters;

    @Autowired(required = false)
    private ExpressWaybillClient expressWaybillClient;

    public ThirdPartyLogisticsGateway(
            CarrierMapper mapper, List<ThirdPartyLogisticsAdapter> adapters) {
        this.carrierMapper = mapper;
        this.adapters = adapters;
    }

    private ThirdPartyLogisticsAdapter adapter(String carrierCode) {
        Carrier c =
                carrierMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Carrier>()
                                .eq(Carrier::getCode, carrierCode));
        if (c == null) {
            throw new BizException("承运商不存在");
        }
        for (ThirdPartyLogisticsAdapter a : adapters) {
            if (a.provider().equals(c.getApiProvider())) {
                return a;
            }
        }
        throw new BizException("承运商未配置对接适配器");
    }

    public ShipmentResult create(Waybill w, List<TransportOrder> os) {
        if (expressWaybillClient != null) {
            Carrier liveCarrier =
                    carrierMapper.selectOne(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Carrier>()
                                    .eq(Carrier::getCode, w.getCarrierCode()));
            String trackingNo =
                    expressWaybillClient.issue(
                            w,
                            os,
                            liveCarrier == null ? null : liveCarrier.getApiBaseUrl(),
                            liveCarrier == null ? null : liveCarrier.getApiKey());
            if (trackingNo != null) {
                ShipmentResult live = new ShipmentResult();
                live.setThirdPartyNo(trackingNo);
                live.setStatus("ACCEPTED");
                return live;
            }
        }
        Carrier c =
                carrierMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Carrier>()
                                .eq(Carrier::getCode, w.getCarrierCode()));
        if (c == null || c.getApiKey() == null || c.getApiKey().isEmpty()) {
            throw new BizException("三方承运商apiKey不能为空");
        }
        ThirdPartyLogisticsAdapter.ShipmentResult r = adapter(w.getCarrierCode()).createShipment(w, os);
        ShipmentResult x = new ShipmentResult();
        x.setThirdPartyNo(r.getThirdPartyNo());
        x.setStatus(r.getStatus());
        return x;
    }

    public List<ThirdPartyLogisticsAdapter.TrackEvent> query(String carrier, String no) {
        return adapter(carrier).queryTrack(no);
    }

    public void cancel(String carrier, String no) {
        adapter(carrier).cancel(no);
    }

    @Data
    public static class ShipmentResult {
        private String thirdPartyNo, status;
    }
}
