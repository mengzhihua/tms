<template><CrudPage title="区域" :api="basic.region" :columns="columns" /></template>
<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
const columns = [
  { prop: 'code', label: '编码', required: true },
  { prop: 'name', label: '名称' },
  { prop: 'level', label: '层级' },
  { prop: 'provinces', label: '省份' },
  { prop: 'cities', label: '城市' },
  { prop: 'status', label: '状态' }
]
</script>
