#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-http://localhost:8080}/api"
J='Content-Type: application/json'
need(){ command -v "$1" >/dev/null || { echo "missing $1"; exit 1; }; }
need curl; need jq
call(){ local out; out=$(curl -sf -X "$1" "$BASE$2" -H "$J" ${3:+-d "$3"}); [ "$(echo "$out"|jq -r .code)" = "0" ] || { echo "FAIL $1 $2 -> $out"; exit 1; }; echo "$out"|jq -c .data; }
echo "== 1 volume"
V=$(call POST /order/volume/calc '{"volumeRatio":6000,"lines":[{"qty":2,"lengthCm":100,"widthCm":50,"heightCm":20,"weightKg":3}]}'); test "$(echo "$V"|jq -r .chargeableWeightKg)" = "33.333"
echo "== 2 create self orders"
O1=$(call POST /order '{"customerCode":"CUS01","orderType":"DELIVERY","fromSiteCode":"WH01","consigneeName":"苏州客户","consigneeLng":120.60,"consigneeLat":31.32,"lines":[{"itemCode":"SKU1","itemName":"货物","qty":2,"lengthCm":100,"widthCm":50,"heightCm":20,"weightKg":3}]}')
O2=$(call POST /order '{"customerCode":"CUS02","orderType":"DELIVERY","fromSiteCode":"WH01","consigneeName":"杭州客户","consigneeLng":120.15,"consigneeLat":30.27,"lines":[{"itemCode":"SKU2","itemName":"货物","qty":1,"lengthCm":100,"widthCm":50,"heightCm":20,"weightKg":2}]}')
I1=$(echo "$O1"|jq -r .id); I2=$(echo "$O2"|jq -r .id)
echo "== 2b intelligent selection and open API"
REC=$(call GET "/selection/recommend/$I1"); test "$(echo "$REC"|jq 'length')" -ge 1
call POST "/selection/assign/$I1?carrierCode=SELF01" >/dev/null
OPEN_SOURCE="SMOKE-OPEN-$(date +%s%N)"
OPEN_PAYLOAD="[{\"sourceNo\":\"$OPEN_SOURCE\",\"orderType\":\"DELIVERY\",\"fromSiteCode\":\"WH01\",\"consigneeName\":\"开放接口客户\",\"lines\":[{\"qty\":1,\"lengthCm\":10,\"widthCm\":10,\"heightCm\":10,\"weightKg\":1}]}]"
OPEN=$(curl -sf -X POST "$BASE/../open/orders" -H "$J" -H "X-Api-Key: demo-key-001" -d "$OPEN_PAYLOAD")
test "$(echo "$OPEN"|jq -r '.code')" = "0"; test "$(echo "$OPEN"|jq -r '.data[0].success')" = "true"
OPEN_DUP=$(curl -sf -X POST "$BASE/../open/orders" -H "$J" -H "X-Api-Key: demo-key-001" -d "$OPEN_PAYLOAD")
test "$(echo "$OPEN_DUP"|jq -r '.data[0].message')" = "重复推单"
echo "== 3 load check"
call POST /order/volume/load-check "{\"vehicleId\":1,\"orderIds\":[$I1,$I2]}" >/dev/null
echo "== 4 self waybill"
W=$(call POST /waybill "{\"carrierCode\":\"SELF01\",\"vehicleId\":1,\"driverCode\":\"DRV01\",\"routeCode\":\"R001\",\"fromSiteCode\":\"WH01\",\"orderIds\":[$I1,$I2]}"); WID=$(echo "$W"|jq -r .id)
W=$(call POST "/waybill/$WID/dispatch"); test "$(echo "$W"|jq -r .freightAmount)" != "0"
call POST "/waybill/$WID/load" "{\"sealNo\":\"SEAL-$WID\",\"loaderName\":\"装卸班组\",\"orderCodes\":[\"$(echo "$O1"|jq -r .code)\",\"$(echo "$O2"|jq -r .code)\"]}" >/dev/null
call GET "/waybill/$WID/loading-sheet" >/dev/null
call POST "/waybill/$WID/depart" >/dev/null
echo "== 5 simulate GPS and geofence"
call POST "/tracking/simulate/$WID?steps=10" | tee /tmp/tms-sim.json
test "$(jq -r '.' /tmp/tms-sim.json)" -gt 0
AL=$(call GET "/tracking/alerts/page?handled=false&size=100"); test "$(echo "$AL"|jq '.records|length')" -gt 0
call POST "/waybill/$WID/arrive" >/dev/null
for OID in "$I1" "$I2"; do call POST "/waybill/$WID/sign" "{\"orderId\":$OID,\"signer\":\"张三\",\"podImage\":\"http://demo/pod-$OID.jpg\"}" >/dev/null; done
W=$(call GET "/waybill/$WID"); test "$(echo "$W"|jq -r .status)" = "DELIVERED"
PODS=$(call GET "/pod/page?size=100"); test "$(echo "$PODS"|jq --argjson wid "$WID" '[.records[]|select(.waybillId==$wid and .status=="RETURNED")]|length')" -ge 1
call POST "/waybill/$WID/close" >/dev/null
FB=$(call GET "/billing/page?size=100"); test "$(echo "$FB"|jq "[.records[]|select(.waybillId==$WID and .status==\"BILLED\")] | length")" -gt 0
echo "== 6 third party SF"
O3=$(call POST /order '{"customerCode":"CUS03","orderType":"DELIVERY","fromSiteCode":"WH01","consigneeName":"上海客户","consigneeLng":121.47,"consigneeLat":31.23,"lines":[{"qty":1,"lengthCm":10,"widthCm":10,"heightCm":10,"weightKg":1}]}'); I3=$(echo "$O3"|jq -r .id)
W3=$(call POST /waybill "{\"carrierCode\":\"SF\",\"fromSiteCode\":\"WH01\",\"orderIds\":[$I3]}"); W3ID=$(echo "$W3"|jq -r .id)
W3=$(call POST "/waybill/$W3ID/dispatch"); TP=$(echo "$W3"|jq -r .thirdPartyNo); [[ "$TP" == SF* ]]
call POST "/waybill/$W3ID/sync-track" >/dev/null
TP_EVENTS=$(call GET "/tracking/events/page?waybillCode=$(echo "$W3"|jq -r .code)&size=100")
test "$(echo "$TP_EVENTS" | jq '[.records[] | select(.eventType=="THIRD_PARTY")] | length')" -ge 1
call POST "/thirdparty/callback/MOCK_SF" "{\"thirdPartyNo\":\"$TP\",\"status\":\"SIGNED\",\"description\":\"已签收\"}" >/dev/null
test "$(call GET "/order/$I3"|jq -r .status)" = "DELIVERED"
echo "== 8 exception, rating and reports"
EX=$(call POST /exception "{\"waybillId\":$WID,\"waybillCode\":\"$(echo "$W"|jq -r .code)\",\"type\":\"DAMAGE\",\"level\":\"HIGH\",\"description\":\"破损\"}"); EXID=$(echo "$EX"|jq -r .id)
call POST "/exception/$EXID/claim" '{"amount":100,"remark":"理赔申请"}' >/dev/null
call POST "/exception/$EXID/claim-audit" '{"approve":true,"remark":"同意"}' >/dev/null
call POST "/exception/$EXID/claim-pay" >/dev/null
call POST "/exception/scan" >/dev/null
MONTH=$(date +%Y-%m); call POST "/rating/compute?period=$MONTH" >/dev/null
for ENDPOINT in /report/sla /report/sla-flow /report/transit-sign /report/quality /report/order-structure /report/alert-summary; do call GET "$ENDPOINT" >/dev/null; done
PUSH=$(call GET "/push-log/page?size=100"); test "$(echo "$PUSH"|jq '[.records[]|select(.status=="SUCCESS")]|length')" -ge 1
echo "== 7 dashboard"
call GET /dashboard | jq -c '{orderStatusCounts,waybillStatusCounts,unhandledAlerts}'
echo "SMOKE OK"
