CREATE TABLE IF NOT EXISTS tms_carrier (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32),
    contact VARCHAR(64),
    phone VARCHAR(32),
    api_provider VARCHAR(32),
    api_key VARCHAR(255),
    api_secret VARCHAR(255),
    status VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_vehicle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plate_no VARCHAR(32) NOT NULL UNIQUE,
    vehicle_type VARCHAR(32),
    carrier_code VARCHAR(32),
    max_weight_kg DECIMAL(18,3),
    max_volume_m3 DECIMAL(18,3),
    inner_length_cm DECIMAL(18,3),
    inner_width_cm DECIMAL(18,3),
    inner_height_cm DECIMAL(18,3),
    driver_code VARCHAR(32),
    status VARCHAR(16),
    lng DECIMAL(18,8),
    lat DECIMAL(18,8),
    last_gps_time TIMESTAMP,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_driver (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64),
    phone VARCHAR(32),
    id_card VARCHAR(64),
    license_type VARCHAR(16),
    carrier_code VARCHAR(32),
    status VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_site (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128),
    type VARCHAR(16),
    province VARCHAR(64),
    city VARCHAR(64),
    address VARCHAR(255),
    lng DECIMAL(18,8),
    lat DECIMAL(18,8),
    contact VARCHAR(64),
    phone VARCHAR(32),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128),
    contact VARCHAR(64),
    phone VARCHAR(32),
    province VARCHAR(64),
    city VARCHAR(64),
    address VARCHAR(255),
    lng DECIMAL(18,8),
    lat DECIMAL(18,8),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_route (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128),
    from_site_code VARCHAR(32),
    to_site_code VARCHAR(32),
    distance_km DECIMAL(18,3),
    estimated_hours DECIMAL(18,3),
    via_sites VARCHAR(255),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_geofence (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128),
    type VARCHAR(16),
    site_code VARCHAR(32),
    center_lng DECIMAL(18,8),
    center_lat DECIMAL(18,8),
    radius_m DECIMAL(18,3),
    polygon CLOB,
    alert_on_enter BOOLEAN,
    alert_on_exit BOOLEAN,
    status VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_rate_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128),
    carrier_code VARCHAR(32),
    charge_type VARCHAR(16),
    first_unit DECIMAL(18,3),
    first_price DECIMAL(18,3),
    add_unit DECIMAL(18,3),
    add_price DECIMAL(18,3),
    min_charge DECIMAL(18,3),
    volume_ratio DECIMAL(18,3),
    priority INT,
    status VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_transport_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    source_no VARCHAR(64),
    customer_code VARCHAR(32),
    order_type VARCHAR(16),
    from_site_code VARCHAR(32),
    consignor_name VARCHAR(64),
    consignor_phone VARCHAR(32),
    consignor_address VARCHAR(255),
    consignor_lng DECIMAL(18,8),
    consignor_lat DECIMAL(18,8),
    consignee_name VARCHAR(64),
    consignee_phone VARCHAR(32),
    consignee_address VARCHAR(255),
    consignee_lng DECIMAL(18,8),
    consignee_lat DECIMAL(18,8),
    required_delivery_time TIMESTAMP,
    total_qty DECIMAL(18,3),
    total_weight_kg DECIMAL(18,3),
    total_volume_m3 DECIMAL(18,6),
    volumetric_weight_kg DECIMAL(18,3),
    chargeable_weight_kg DECIMAL(18,3),
    volume_ratio DECIMAL(18,3),
    status VARCHAR(16),
    waybill_id BIGINT,
    waybill_code VARCHAR(32),
    signer VARCHAR(64),
    sign_time TIMESTAMP,
    pod_image VARCHAR(255),
    pod_remark VARCHAR(255),
    priority INT,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_transport_order_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    item_code VARCHAR(64),
    item_name VARCHAR(128),
    qty DECIMAL(18,3),
    length_cm DECIMAL(18,3),
    width_cm DECIMAL(18,3),
    height_cm DECIMAL(18,3),
    weight_kg DECIMAL(18,3),
    volume_m3 DECIMAL(18,6),
    line_weight_kg DECIMAL(18,3),
    line_volume_m3 DECIMAL(18,6),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_waybill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    carrier_code VARCHAR(32),
    carrier_type VARCHAR(16),
    vehicle_plate VARCHAR(32),
    driver_code VARCHAR(32),
    driver_name VARCHAR(64),
    route_code VARCHAR(32),
    from_site_code VARCHAR(32),
    planned_depart_time TIMESTAMP,
    planned_arrive_time TIMESTAMP,
    actual_depart_time TIMESTAMP,
    actual_arrive_time TIMESTAMP,
    order_count INT,
    total_weight_kg DECIMAL(18,3),
    total_volume_m3 DECIMAL(18,6),
    weight_load_rate DECIMAL(18,3),
    volume_load_rate DECIMAL(18,3),
    freight_amount DECIMAL(18,3),
    third_party_no VARCHAR(64),
    third_party_status VARCHAR(32),
    status VARCHAR(16),
    exception_flag BOOLEAN,
    exception_remark VARCHAR(255),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_tracking_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    waybill_id BIGINT,
    waybill_code VARCHAR(32),
    order_id BIGINT,
    event_type VARCHAR(32),
    lng DECIMAL(18,8),
    lat DECIMAL(18,8),
    speed_kmh DECIMAL(18,3),
    address VARCHAR(255),
    description VARCHAR(255),
    event_time TIMESTAMP,
    source VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_geofence_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    waybill_id BIGINT,
    geofence_code VARCHAR(32),
    inside BOOLEAN,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_tms_geofence_state UNIQUE(waybill_id,geofence_code)
);

CREATE TABLE IF NOT EXISTS tms_geofence_alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    waybill_id BIGINT,
    waybill_code VARCHAR(32),
    vehicle_plate VARCHAR(32),
    geofence_code VARCHAR(32),
    geofence_name VARCHAR(128),
    alert_type VARCHAR(8),
    lng DECIMAL(18,8),
    lat DECIMAL(18,8),
    alert_time TIMESTAMP,
    handled BOOLEAN,
    handler VARCHAR(64),
    handle_remark VARCHAR(255),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_freight_bill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    waybill_id BIGINT,
    waybill_code VARCHAR(32),
    order_id BIGINT,
    order_code VARCHAR(32),
    carrier_code VARCHAR(32),
    rule_code VARCHAR(32),
    charge_type VARCHAR(16),
    quantity DECIMAL(18,3),
    amount DECIMAL(18,3),
    calc_detail VARCHAR(255),
    status VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE tms_customer ADD COLUMN IF NOT EXISTS api_key VARCHAR(128);
ALTER TABLE tms_customer ADD COLUMN IF NOT EXISTS callback_url VARCHAR(255);
ALTER TABLE tms_customer ADD COLUMN IF NOT EXISTS push_enabled BOOLEAN;
ALTER TABLE tms_rate_rule ADD COLUMN IF NOT EXISTS region_code VARCHAR(32);
ALTER TABLE tms_rate_rule ADD COLUMN IF NOT EXISTS service_level_code VARCHAR(32);
ALTER TABLE tms_transport_order ADD COLUMN IF NOT EXISTS service_level_code VARCHAR(32);
ALTER TABLE tms_transport_order ADD COLUMN IF NOT EXISTS region_code VARCHAR(32);
ALTER TABLE tms_transport_order ADD COLUMN IF NOT EXISTS carrier_code VARCHAR(32);
ALTER TABLE tms_transport_order ADD COLUMN IF NOT EXISTS recommend_remark VARCHAR(255);
ALTER TABLE tms_transport_order ADD COLUMN IF NOT EXISTS origin_order_code VARCHAR(32);
ALTER TABLE tms_transport_order ADD COLUMN IF NOT EXISTS consignee_province VARCHAR(64);
ALTER TABLE tms_transport_order ADD COLUMN IF NOT EXISTS consignee_city VARCHAR(64);
ALTER TABLE tms_transport_order ADD COLUMN IF NOT EXISTS promised_arrive_time TIMESTAMP;
ALTER TABLE tms_transport_order_line ADD COLUMN IF NOT EXISTS package_code VARCHAR(32);
ALTER TABLE tms_waybill ADD COLUMN IF NOT EXISTS service_level_code VARCHAR(32);
ALTER TABLE tms_waybill ADD COLUMN IF NOT EXISTS promised_arrive_time TIMESTAMP;
ALTER TABLE tms_waybill ADD COLUMN IF NOT EXISTS load_status VARCHAR(16);
ALTER TABLE tms_waybill ADD COLUMN IF NOT EXISTS seal_no VARCHAR(64);
ALTER TABLE tms_waybill ADD COLUMN IF NOT EXISTS loader_name VARCHAR(64);
ALTER TABLE tms_waybill ADD COLUMN IF NOT EXISTS load_time TIMESTAMP;
ALTER TABLE tms_waybill ADD COLUMN IF NOT EXISTS on_time BOOLEAN;

CREATE TABLE IF NOT EXISTS tms_region (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128),
    level VARCHAR(16),
    provinces VARCHAR(255),
    cities VARCHAR(255),
    status VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_service_level (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128),
    promised_hours DECIMAL(18,3),
    priority INT,
    status VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_package_material (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128),
    length_cm DECIMAL(18,3),
    width_cm DECIMAL(18,3),
    height_cm DECIMAL(18,3),
    tare_weight_kg DECIMAL(18,3),
    volume_m3 DECIMAL(18,6),
    status VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_carrier_coverage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrier_code VARCHAR(32),
    region_code VARCHAR(32),
    service_level_code VARCHAR(32),
    promised_hours DECIMAL(18,3),
    status VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_selection_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128),
    strategy VARCHAR(16),
    region_code VARCHAR(32),
    customer_code VARCHAR(32),
    service_level_code VARCHAR(32),
    designated_carrier VARCHAR(32),
    priority INT,
    status VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_transport_exception (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    waybill_id BIGINT,
    waybill_code VARCHAR(32),
    order_id BIGINT,
    order_code VARCHAR(32),
    carrier_code VARCHAR(32),
    type VARCHAR(32),
    level VARCHAR(16),
    source VARCHAR(16),
    description VARCHAR(255),
    status VARCHAR(16),
    handler VARCHAR(64),
    handle_remark VARCHAR(255),
    handle_time TIMESTAMP,
    claim_flag BOOLEAN,
    claim_amount DECIMAL(18,3),
    claim_status VARCHAR(16),
    claim_remark VARCHAR(255),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_pod_receipt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    waybill_id BIGINT,
    waybill_code VARCHAR(32),
    order_id BIGINT,
    order_code VARCHAR(32),
    carrier_code VARCHAR(32),
    receipt_type VARCHAR(16),
    status VARCHAR(16),
    signer VARCHAR(64),
    sign_time TIMESTAMP,
    image_url VARCHAR(255),
    returned_time TIMESTAMP,
    archived_time TIMESTAMP,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tms_carrier_rating (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrier_code VARCHAR(32),
    carrier_name VARCHAR(128),
    period VARCHAR(16),
    waybill_count INT,
    closed_count INT,
    on_time_count INT,
    on_time_rate DECIMAL(18,3),
    exception_count INT,
    exception_rate DECIMAL(18,3),
    pod_return_rate DECIMAL(18,3),
    avg_transit_hours DECIMAL(18,3),
    claim_amount DECIMAL(18,3),
    score DECIMAL(18,3),
    grade VARCHAR(2),
    computed_at TIMESTAMP,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_tms_carrier_rating UNIQUE(carrier_code, period)
);

CREATE TABLE IF NOT EXISTS tms_push_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_code VARCHAR(32),
    order_code VARCHAR(32),
    event_type VARCHAR(32),
    url VARCHAR(255),
    payload CLOB,
    response_code INT,
    response_body CLOB,
    success BOOLEAN,
    retry_count INT,
    next_retry_time TIMESTAMP,
    status VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
