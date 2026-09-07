import http from './request'

export const crud = (base) => ({
  page: (params) => http.get(`${base}/page`, { params }),
  list: (params) => http.get(`${base}/list`, { params }),
  get: (id) => http.get(`${base}/${id}`),
  create: (data) => http.post(base, data),
  update: (id, data) => http.put(`${base}/${id}`, data),
  remove: (id) => http.delete(`${base}/${id}`)
})

export const basic = {
  carrier: crud('/basic/carrier'),
  vehicle: crud('/basic/vehicle'),
  driver: crud('/basic/driver'),
  site: crud('/basic/site'),
  customer: crud('/basic/customer'),
  route: crud('/basic/route'),
  geofence: crud('/basic/geofence'),
  rateRule: crud('/basic/rate-rule')
  ,region: crud('/basic/region')
  ,serviceLevel: crud('/basic/service-level')
  ,packageMaterial: crud('/basic/package-material')
  ,carrierCoverage: crud('/basic/carrier-coverage')
  ,selectionRule: crud('/basic/selection-rule')
}

export const order = {
  page: (params) => http.get('/order/page', { params }),
  get: (id) => http.get(`/order/${id}`),
  create: (data) => http.post('/order', data),
  update: (id, data) => http.put(`/order/${id}`, data),
  cancel: (id) => http.post(`/order/${id}/cancel`),
  volume: (data) => http.post('/order/volume/calc', data),
  loadCheck: (data) => http.post('/order/volume/load-check', data)
}

export const waybill = {
  page: (params) => http.get('/waybill/page', { params }),
  get: (id) => http.get(`/waybill/${id}`),
  create: (data) => http.post('/waybill', data),
  dispatch: (id) => http.post(`/waybill/${id}/dispatch`),
  depart: (id) => http.post(`/waybill/${id}/depart`),
  arrive: (id) => http.post(`/waybill/${id}/arrive`),
  sign: (id, data) => http.post(`/waybill/${id}/sign`, data),
  close: (id) => http.post(`/waybill/${id}/close`),
  cancel: (id) => http.post(`/waybill/${id}/cancel`),
  syncTrack: (id) => http.post(`/waybill/${id}/sync-track`)
  ,load: (id, data) => http.post(`/waybill/${id}/load`, data)
  ,loadingSheet: (id) => http.get(`/waybill/${id}/loading-sheet`)
}

export const dispatch = {
  pending: (params) => http.get('/dispatch/pending-orders', { params })
}

export const tracking = {
  vehicles: () => http.get('/tracking/vehicles'),
  gps: (data) => http.post('/tracking/gps', data),
  events: (params) => http.get('/tracking/events/page', { params }),
  alerts: (params) => http.get('/tracking/alerts/page', { params }),
  handleAlert: (id, data) => http.post(`/tracking/alerts/${id}/handle`, data),
  simulate: (id, steps) => http.post(`/tracking/simulate/${id}`, null, { params: { steps } })
}

export const billing = {
  page: (params) => http.get('/billing/page', { params }),
  calc: (data) => http.post('/billing/calc', data),
  pay: (id) => http.post(`/billing/${id}/pay`)
}

export const dashboard = () => http.get('/dashboard')

export const selection = {
  recommend: (id) => http.get(`/selection/recommend/${id}`),
  assign: (id, carrierCode) => http.post(`/selection/assign/${id}`, null, { params: { carrierCode } }),
  autoAssign: (ids) => http.post('/selection/auto-assign', { ids })
}

export const exceptionApi = {
  page: (params) => http.get('/exception/page', { params }),
  create: (data) => http.post('/exception', data),
  handle: (id, data) => http.post(`/exception/${id}/handle`, data),
  claim: (id, data) => http.post(`/exception/${id}/claim`, data),
  audit: (id, data) => http.post(`/exception/${id}/claim-audit`, data),
  pay: (id) => http.post(`/exception/${id}/claim-pay`),
  scan: () => http.post('/exception/scan'),
  summary: () => http.get('/exception/summary')
}

export const pod = {
  page: (params) => http.get('/pod/page', { params }),
  summary: () => http.get('/pod/summary'),
  return: (id, data) => http.post(`/pod/${id}/return`, data),
  archive: (id) => http.post(`/pod/${id}/archive`),
  lost: (id) => http.post(`/pod/${id}/lost`)
}

export const rating = {
  compute: (period) => http.post('/rating/compute', null, { params: { period } }),
  page: (params) => http.get('/rating/page', { params }),
  rank: (period) => http.get('/rating/rank', { params: { period } })
}

export const report = {
  sla: (params) => http.get('/report/sla', { params }),
  slaFlow: (params) => http.get('/report/sla-flow', { params }),
  transitSign: (params) => http.get('/report/transit-sign', { params }),
  quality: (params) => http.get('/report/quality', { params }),
  orderStructure: (params) => http.get('/report/order-structure', { params }),
  alertSummary: () => http.get('/report/alert-summary')
}

export const pushLog = {
  page: (params) => http.get('/push-log/page', { params }),
  retry: (id) => http.post(`/push-log/${id}/retry`)
}
