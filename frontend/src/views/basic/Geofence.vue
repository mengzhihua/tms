<template>
  <div class="page">
    <CrudPage title="电子围栏" :api="basic.geofence" :columns="columns" />
  </div>
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'

const columns = [
  { prop: 'code', label: '编码', required: true },
  { prop: 'name', label: '名称', required: true },
  { prop: 'type', label: '类型' },
  { prop: 'siteCode', label: '绑定站点' },
  { prop: 'centerLng', label: '中心经度', type: 'number' },
  { prop: 'centerLat', label: '中心纬度', type: 'number' },
  { prop: 'radiusM', label: '半径(m)', type: 'number' },
  { prop: 'polygon', label: '多边形(JSON)', type: 'textarea', span: 24 },
  { prop: 'alertOnEnter', label: '进入告警', type: 'bool' },
  { prop: 'alertOnExit', label: '离开告警', type: 'bool' },
  { prop: 'status', label: '状态', type: 'status' }
]
</script>
