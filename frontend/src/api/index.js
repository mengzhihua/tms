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
