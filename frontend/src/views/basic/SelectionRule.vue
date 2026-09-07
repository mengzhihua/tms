<template><CrudPage title="筛单策略" :api="basic.selectionRule" :columns="columns" /></template>
<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
const columns = [
  { prop: 'code', label: '编码', required: true },
  { prop: 'name', label: '名称' },
  { prop: 'strategy', label: '策略' },
  { prop: 'regionCode', label: '区域' },
  { prop: 'customerCode', label: '客户' },
  { prop: 'serviceLevelCode', label: '服务时效' },
  { prop: 'designatedCarrier', label: '指定承运商' },
  { prop: 'priority', label: '优先级', type: 'number' },
  { prop: 'status', label: '状态' }
]
</script>
