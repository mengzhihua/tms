<template><CrudPage title="包材" :api="basic.packageMaterial" :columns="columns" /></template>
<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'
const columns = [
  { prop: 'code', label: '编码', required: true },
  { prop: 'name', label: '名称' },
  { prop: 'lengthCm', label: '长(cm)', type: 'number' },
  { prop: 'widthCm', label: '宽(cm)', type: 'number' },
  { prop: 'heightCm', label: '高(cm)', type: 'number' },
  { prop: 'tareWeightKg', label: '皮重(kg)', type: 'number' },
  { prop: 'volumeM3', label: '体积(m³)', type: 'number' },
  { prop: 'status', label: '状态' }
]
</script>
