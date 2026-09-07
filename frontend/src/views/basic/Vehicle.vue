<template>
  <CrudPage title="车辆" :api="basic.vehicle" :columns="columns" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'

const columns = [
  { prop: 'plateNo', label: '车牌号', required: true },
  { prop: 'vehicleType', label: '车型' },
  { prop: 'carrierCode', label: '承运商' },
  { prop: 'maxWeightKg', label: '最大载重(kg)', type: 'number' },
  { prop: 'maxVolumeM3', label: '最大容积(m³)', type: 'number' },
  { prop: 'driverCode', label: '司机' },
  { prop: 'status', label: '状态', type: 'status' },
  { prop: 'lng', label: '经度', type: 'number' },
  { prop: 'lat', label: '纬度', type: 'number' }
]
</script>
