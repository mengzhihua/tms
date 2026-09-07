<template>
  <CrudPage title="线路" :api="basic.route" :columns="columns" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'

const columns = [
  { prop: 'code', label: '编码', required: true },
  { prop: 'name', label: '名称', required: true },
  { prop: 'fromSiteCode', label: '起点站' },
  { prop: 'toSiteCode', label: '终点站' },
  { prop: 'distanceKm', label: '距离(km)', type: 'number' },
  { prop: 'estimatedHours', label: '预计时长(h)', type: 'number' },
  { prop: 'viaSites', label: '途经站点' }
]
</script>
