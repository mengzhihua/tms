import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'

export const menus = [
  {
    path: '/dashboard',
    name: '工作台',
    icon: 'Odometer',
    component: () => import('../views/Dashboard.vue')
  },
  {
    path: '/basic',
    name: '基础数据',
    icon: 'Setting',
    children: [
      { path: 'carrier', name: '承运商', component: () => import('../views/basic/Carrier.vue') },
      { path: 'vehicle', name: '车辆', component: () => import('../views/basic/Vehicle.vue') },
      { path: 'driver', name: '司机', component: () => import('../views/basic/Driver.vue') },
      { path: 'site', name: '站点', component: () => import('../views/basic/Site.vue') },
      { path: 'customer', name: '客户', component: () => import('../views/basic/Customer.vue') },
      { path: 'route', name: '线路', component: () => import('../views/basic/Route.vue') },
      { path: 'geofence', name: '电子围栏', component: () => import('../views/basic/Geofence.vue') },
      { path: 'rate-rule', name: '计费规则', component: () => import('../views/basic/RateRule.vue') }
    ]
  },
  {
    path: '/orders',
    name: '运输订单',
    icon: 'Document',
    children: [
      { path: 'order', name: '订单管理', component: () => import('../views/order/Order.vue') },
      { path: 'volume', name: '体积测算', component: () => import('../views/order/Volume.vue') }
    ]
  },
  {
    path: '/dispatch',
    name: '调度管理',
    icon: 'Van',
    children: [
      { path: 'console', name: '调度台', component: () => import('../views/dispatch/Dispatch.vue') },
      { path: 'waybill', name: '运单管理', component: () => import('../views/dispatch/Waybill.vue') }
    ]
  },
  {
    path: '/tracking',
    name: '在途监控',
    icon: 'Location',
    children: [
      { path: 'monitor', name: '车辆监控', component: () => import('../views/tracking/Monitor.vue') },
      { path: 'events', name: '轨迹事件', component: () => import('../views/tracking/Events.vue') },
      { path: 'alerts', name: '围栏告警', component: () => import('../views/tracking/Alerts.vue') }
    ]
  },
  {
    path: '/billing',
    name: '计费管理',
    icon: 'Money',
    children: [
      { path: 'calc', name: '运费试算', component: () => import('../views/billing/Calc.vue') },
      { path: 'bill', name: '计费单', component: () => import('../views/billing/Bill.vue') }
    ]
  }
]

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: menus.flatMap((menu) =>
      menu.children
        ? menu.children.map((child) => ({
            path: `${menu.path}/${child.path}`,
            name: child.name,
            component: child.component
          }))
        : [{ path: menu.path, name: menu.name, component: menu.component }]
    )
  }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
