# TMS 运输管理系统

TMS 是一个面向运输订单、调度运单、在途跟踪、电子围栏、三方物流和计费结算的示例系统。

## 功能范围

| 模块 | 能力 |
| --- | --- |
| 基础数据 | 承运商、车辆、司机、站点、客户、线路、电子围栏、计费规则 |
| 运输订单 | 订单 CRUD、订单明细、体积/体积重/计费重、载重校验 |
| 调度运单 | 自建车辆和三方承运商调度、发车、到达、签收、关闭、取消 |
| 在途跟踪 | GPS 上报、轨迹事件、圆形/多边形围栏、进入/离开告警、模拟行驶 |
| 三方物流 | MOCK_SF、MOCK_JD、MOCK_ZTO 适配器、轨迹同步、回调签收 |
| 计费结算 | 重量、体积、件数、里程计费，首续单位、最低收费、支付 |
| 工作台 | 订单/运单状态、车辆、告警、运费、待调度订单和最近事件 |

## 目录

```text
backend/   Spring Boot 2.7 + MyBatis-Plus 后端，端口 8080
frontend/  Vue 3 + Vite + Element Plus 前端，端口 5173
scripts/   冒烟测试脚本
```

## 快速开始

### 后端

要求 JDK 8 和 Maven：

```bash
cd backend
mvn spring-boot:run
```

默认使用 H2 文件数据库 `backend/data/tms`，H2 Console 为 `http://localhost:8080/h2`。
MySQL 配置通过 `mysql` profile 启用。

> 中文乱码：演示数据 `data.sql` 与接口响应均按 UTF-8 处理（`spring.sql.init.encoding`、`server.servlet.encoding`、`-Dfile.encoding=UTF-8`）。
> 若之前已在 Windows/GBK 环境启动过并生成了乱码数据，请停止后端、删除 `backend/data` 目录后重新启动，演示数据会重新初始化。
> 用 `java -jar` 直接运行时请加 `-Dfile.encoding=UTF-8`。

### 前端

要求 Node.js 18+：

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`。Vite 会将 `/api` 请求代理到后端 `8080`。

## 冒烟测试

后端启动后执行：

```bash
./scripts/smoke.sh
```

脚本覆盖订单创建、体积测算、载重检查、自建运单、GPS/围栏、三方运单、轨迹同步、回调和 Dashboard。

## API 概览

统一响应格式为：

```json
{"code": 0, "msg": "success", "data": {}}
```

主要前缀：

```text
/api/basic/*
/api/order/*
/api/waybill/*
/api/dispatch/*
/api/tracking/*
/api/thirdparty/*
/api/billing/*
/api/dashboard
```

## 三方物流接入

实现 `ThirdPartyLogisticsAdapter` 即可接入新的承运商，核心方法包括：

```java
String provider();
ShipmentResult createShipment(Waybill waybill, List<TransportOrder> orders);
List<TrackEvent> queryTrack(String thirdPartyNo);
void cancel(String thirdPartyNo);
```

回调格式示例：

```json
{
  "thirdPartyNo": "SF-123",
  "status": "SIGNED",
  "description": "已签收",
  "eventTime": "2025-01-01T12:00:00"
}
```

SIGNED 回调会复用运单到达和逐单签收流程。

## 围栏与体积测算

圆形围栏使用 haversine 计算米制距离，边界点视为在围栏内；多边形围栏使用射线法。
首次 GPS 位置只初始化围栏状态，状态从外到内或从内到外且开启对应告警时写入告警和轨迹事件。

体积测算按 `长(cm) × 宽(cm) × 高(cm) / 1,000,000` 计算立方米。
体积重为 `总体积 × 1,000,000 / 抛比`，计费重取实际重量和体积重的较大值。

## 二期能力矩阵

| 金库 TMS 能力 | 本系统模块 | 主要接口 |
| --- | --- | --- |
| 区域与服务时效 | 基础数据、区域匹配、承运商覆盖 | `/api/basic/region`、`/api/basic/service-level` |
| 智能筛单与承运商推荐 | selection | `/api/selection/recommend/{orderId}`、`/api/selection/auto-assign` |
| SLA 超时与异常理赔 | exc、SLA 定时扫描 | `/api/exception/page`、`/api/exception/scan` |
| 装车交接与装车单 | dispatch | `/api/waybill/{id}/load`、`/api/waybill/{id}/loading-sheet` |
| 逆向订单与电子回单 | order、pod | `/api/order/{id}/reverse`、`/api/pod/page` |
| 承运商服务评级 | rating | `/api/rating/compute`、`/api/rating/rank` |
| 客户开放接口与回调 | openapi | `/open/orders`、`/open/orders/{code}/track` |
| SLA、质量、订单结构分析 | report | `/api/report/sla`、`/api/report/quality` |

开放接口使用 `X-Api-Key` 认证。演示客户 `CUS01` 的密钥为
`demo-key-001`，回调地址指向本地模拟接收器。
