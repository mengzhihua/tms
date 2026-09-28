# TMS 运输管理系统

TMS 是一个面向运输订单、智能筛单、调度运单、在途跟踪、电子围栏、三方物流、异常理赔、电子回单、承运商评级和计费结算的运输管理系统，参考富勒等行业标准 TMS 与科捷金库 TMS 的能力模型设计。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | JDK 8、Spring Boot 2.7.18、MyBatis-Plus 3.5.3、H2（默认）/ MySQL |
| 前端 | Vue 3、Vite 5、Element Plus、axios、ECharts |

项目亮点见 [docs/项目亮点.md](docs/项目亮点.md)。

怎么用、亮点和适用场景见 [docs/图文导读.md](docs/图文导读.md)。

## 功能范围

### 一期：标准 TMS 能力

| 模块 | 能力 |
| --- | --- |
| 基础数据 | 承运商、车辆、司机、站点、客户、线路、电子围栏、计费规则 |
| 运输订单 | 订单 CRUD、订单明细、体积/体积重/计费重、载重校验 |
| 调度运单 | 自建车辆和三方承运商调度、发车、到达、签收、关闭、取消 |
| 在途跟踪 | GPS 上报、轨迹事件、圆形/多边形围栏、进入/离开告警、模拟行驶 |
| 三方物流 | MOCK_SF、MOCK_JD、MOCK_ZTO 适配器、轨迹同步、回调签收 |
| 计费结算 | 重量、体积、件数、里程计费，首续单位、最低收费、支付 |
| 工作台 | 订单/运单状态、车辆、告警、运费、待调度订单和最近事件 |

### 二期：对标金库 TMS

| 模块 | 能力 |
| --- | --- |
| 基础属性 | 区域、服务时效等级、包材、承运商覆盖范围、筛单策略 |
| 智能筛单 | 按区域/时效/成本/评级推荐承运商，支持自动分配 |
| 时效预警 | 按服务时效计算承诺送达时间，定时扫描超时运单生成预警 |
| 异常理赔 | 异常登记与处理、理赔申请、理赔审核、理赔支付 |
| 装车交接 | 库房装车确认、装车单打印 |
| 逆向回货 | 由正向订单生成逆向订单、回货运单 |
| 电子回单 | 签收后生成回单，回单返回、归档、丢失登记 |
| 承运商评级 | 准时率、异常率、回单及时率综合评分与排名 |
| 开放接口 | OMS/WMS 下单、轨迹查询、状态回传（`X-Api-Key` 认证）与推送日志 |
| 数据分析 | SLA、服务质量、订单结构 ECharts 报表 |

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

演示数据包含 `WB-DEMO-OVERDUE` 超时演示运单，扫描 SLA 后会生成到达超时异常。

### 前端

要求 Node.js 18+：

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`。Vite 会将 `/api` 请求代理到后端 `8080`。

前端为中文界面，若在没有中文字体的 Linux 服务器/容器中用浏览器打开会显示为方块，安装字体即可（如 `apt-get install fonts-noto-cjk`）。

### 前端菜单

```text
工作台
基础数据   承运商 / 车辆 / 司机 / 站点 / 客户 / 线路 / 电子围栏 / 计费规则 / 区域 / 服务时效 / 包材 / 承运商覆盖 / 筛单策略
运输订单   订单管理 / 体积测算
调度管理   调度台 / 运单管理 / 装车单
在途监控   车辆监控 / 轨迹事件 / 围栏告警 / 异常理赔 / 超时预警
计费管理   运费试算 / 计费单
回单管理 / 承运商评级 / 数据分析 / 开放接口
```

## 冒烟测试

后端启动后执行：

```bash
./scripts/smoke.sh
```

脚本覆盖订单创建、体积测算、载重检查、自建运单、GPS/围栏、三方运单、轨迹同步、回调、Dashboard，以及二期的筛单、时效预警、异常理赔、逆向订单、回单、评级和开放接口。

## API 概览

统一响应格式为：

```json
{"code": 0, "msg": "success", "data": {}}
```

主要前缀：

```text
/api/basic/*       基础数据（carrier/vehicle/driver/site/customer/route/geofence/rate-rule/region/service-level/package-material/carrier-coverage/selection-rule）
/api/order/*       运输订单、体积测算、逆向订单
/api/waybill/*     运单、装车、装车单
/api/dispatch/*    调度
/api/tracking/*    GPS、轨迹、围栏告警
/api/thirdparty/*  三方物流与回调
/api/billing/*     计费
/api/selection/*   智能筛单
/api/exception/*   异常理赔、超时预警
/api/pod/*         电子回单
/api/rating/*      承运商评级
/api/report/*      数据分析
/api/push-log/*    开放接口推送日志
/api/dashboard     工作台
/open/*            客户开放接口（X-Api-Key）
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

## 控制塔对接

运单、账单和调度指令见 [技术方案](docs/技术方案.md)。

控制塔读 `GET /api/open/ir/snapshots`（运单、账单），写 `POST /api/open/ir/actions`，并回退 `/dispatch`、`/sync-track`、`/switch-carrier`。写指令不走登录口。没有 Key 时，读数回退运单分页、账单分页和工作台。

发行包默认端口是 `8082`，与控制塔种子里的 TMS 地址一致。`mvn spring-boot:run` 仍监听 `8080`。

## 发布包（开箱即用）

前端生产构建打进 Spring Boot 可执行 JAR。三种用法：

### 1. 服务端（任意已装 JDK 17 的机器）

```bash
java -jar tms-backend-1.0.0.jar --server.port=8082
```

Linux systemd 示例见发布包 `README.txt`。

### 2. 便携包（需本机已装 Java）

```bash
bash scripts/package-release.sh
unzip release/tms-1.0.0.zip
cd tms-1.0.0
```

| 系统 | 怎么用 |
| --- | --- |
| Linux | `./start.sh` |
| macOS | 双击 `start.command`，或 `./start.sh` |
| Windows | 双击 `start.bat` |

### 3. 原生包（捆绑 JRE，不必装 Java）

合并到默认分支且便携包冒烟通过后，GitHub Actions 自动发布 GitHub Release（也可在 Actions 里手动 `workflow_dispatch`）。分别在 Ubuntu / Windows / macOS 生成：

- `tms-1.0.0-linux-x64.zip` → `bin/tms`
- `tms-1.0.0-windows-x64.zip` → 双击 `tms.exe`
- `tms-1.0.0-macos-arm64.zip` → Apple Silicon（M 系列），双击 `tms.app`
- `tms-1.0.0-macos-x64.zip` → Intel Mac，双击 `tms.app`

浏览器访问 `http://127.0.0.1:8082`。本系统无登录页，打开即可用。

十二套系统可同时启动：OMS 8081 / WMS 8083 / TMS 8082 / BMS 8084 / SAP 8085 / OA 8086 / SRM 8087 / BOM 8088 / INV 8089 / IR 8090 / CRM 8091 / DMS 8092。


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
