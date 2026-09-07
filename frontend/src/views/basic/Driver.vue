<template>
  <CrudPage title="司机" :api="basic.driver" :columns="columns" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'

const columns = [
  { prop: 'code', label: '编码', required: true },
  { prop: 'name', label: '姓名', required: true },
  { prop: 'phone', label: '电话' },
  { prop: 'idCard', label: '身份证号' },
  { prop: 'licenseType', label: '驾照类型' },
  { prop: 'carrierCode', label: '承运商' },
  { prop: 'status', label: '状态', type: 'status' }
]
</script>
