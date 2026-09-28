<template><CrudPage title="承运商覆盖" :api="basic.carrierCoverage" :columns="columns" /></template>
<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
const columns = [
  { prop: 'carrierCode', label: '承运商' },
  { prop: 'regionCode', label: '区域' },
  { prop: 'serviceLevelCode', label: '服务时效' },
  { prop: 'promisedHours', label: '承诺小时', type: 'number' },
  { prop: 'status', label: '状态' }
]
</script>
