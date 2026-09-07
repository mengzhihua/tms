package com.tms.thirdparty;
import com.tms.dispatch.entity.Waybill; import com.tms.order.entity.TransportOrder; import java.time.*; import java.util.*;
public class MockExpressAdapter implements ThirdPartyLogisticsAdapter {
 private final String provider;
 public MockExpressAdapter(String provider){this.provider=provider;}
 public String provider(){return provider;}
 public ShipmentResult createShipment(Waybill w,List<TransportOrder> os){ShipmentResult r=new ShipmentResult();r.setThirdPartyNo(prefix()+"-"+System.currentTimeMillis());r.setStatus("ACCEPTED");return r;}
 public List<TrackEvent> queryTrack(String no){List<TrackEvent> x=new ArrayList<>();TrackEvent e=new TrackEvent();e.setStatusCode("ACCEPTED");e.setDescription("已揽收");e.setEventTime(LocalDateTime.now());x.add(e);return x;}
 public void cancel(String no){}
 private String prefix(){return provider.endsWith("JD")?"JD":provider.endsWith("ZTO")?"ZTO":"SF";}
}
