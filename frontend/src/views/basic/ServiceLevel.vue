<template><CrudPage title="服务时效" :api="basic.serviceLevel" :columns="columns" /></template>
<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
const columns = [
  { prop: 'code', label: '编码', required: true },
  { prop: 'name', label: '名称' },
  { prop: 'promisedHours', label: '承诺小时', type: 'number' },
  { prop: 'priority', label: '优先级', type: 'number' },
  { prop: 'status', label: '状态' }
]
</script>
