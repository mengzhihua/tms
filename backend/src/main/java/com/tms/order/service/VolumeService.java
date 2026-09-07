package com.tms.order.service;

import com.tms.basic.entity.Vehicle;
import com.tms.order.entity.TransportOrderLine;
import lombok.Data;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class VolumeService {
    @Data public static class LineResult { private BigDecimal unitVolumeM3,lineVolumeM3,lineWeightKg; }
    @Data public static class VolumeResult { private BigDecimal totalQty,totalWeightKg,totalVolumeM3,volumetricWeightKg,chargeableWeightKg; }
    @Data public static class LoadResult { private BigDecimal weightRate,volumeRate; private boolean fits; }
    public static LineResult calcLine(TransportOrderLine line) {
        BigDecimal qty = nz(line.getQty()), unit = line.getVolumeM3();
        if (unit == null) unit = nz(line.getLengthCm()).multiply(nz(line.getWidthCm())).multiply(nz(line.getHeightCm())).divide(new BigDecimal("1000000"), 9, RoundingMode.HALF_UP);
        LineResult r = new LineResult(); r.unitVolumeM3 = unit; r.lineVolumeM3 = unit.multiply(qty); r.lineWeightKg = nz(line.getWeightKg()).multiply(qty); return r;
    }
    public static VolumeResult calc(List<TransportOrderLine> lines, BigDecimal ratio) {
        BigDecimal qty=BigDecimal.ZERO, weight=BigDecimal.ZERO, volume=BigDecimal.ZERO;
        if (lines != null) for (TransportOrderLine l : lines) { LineResult r=calcLine(l); qty=qty.add(nz(l.getQty())); weight=weight.add(r.lineWeightKg); volume=volume.add(r.lineVolumeM3); }
        BigDecimal rr = ratio == null || ratio.signum() <= 0 ? new BigDecimal("6000") : ratio;
        VolumeResult out=new VolumeResult(); out.totalQty=scale(qty); out.totalWeightKg=scale(weight); out.totalVolumeM3=scale(volume); out.volumetricWeightKg=scale(volume.multiply(new BigDecimal("1000000")).divide(rr, 9, RoundingMode.HALF_UP)); out.chargeableWeightKg=scale(out.volumetricWeightKg.max(out.totalWeightKg)); return out;
    }
    public static LoadResult checkLoad(Vehicle vehicle, BigDecimal weight, BigDecimal volume) {
        BigDecimal wr = safeRate(weight, vehicle == null ? null : vehicle.getMaxWeightKg()), vr=safeRate(volume, vehicle == null ? null : vehicle.getMaxVolumeM3());
        LoadResult r=new LoadResult(); r.weightRate=wr.multiply(new BigDecimal("100")); r.volumeRate=vr.multiply(new BigDecimal("100")); r.fits=wr.compareTo(BigDecimal.ONE)<=0 && vr.compareTo(BigDecimal.ONE)<=0; return r;
    }
    private static BigDecimal safeRate(BigDecimal x, BigDecimal max) { return max == null || max.signum()==0 ? BigDecimal.ZERO : nz(x).divide(max, 6, RoundingMode.HALF_UP); }
    private static BigDecimal nz(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }
    private static BigDecimal scale(BigDecimal x) { return x.setScale(3, RoundingMode.HALF_UP); }
}
