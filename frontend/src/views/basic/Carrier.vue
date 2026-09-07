<template>
  <CrudPage title="承运商" :api="basic.carrier" :columns="columns" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'

const columns = [
  { prop: 'code', label: '编码', required: true },
  { prop: 'name', label: '名称', required: true },
  {
    prop: 'type',
    label: '类型',
    type: 'select',
    options: [
      { label: '自建', value: 'SELF' },
      { label: '三方', value: 'THIRD_PARTY' }
    ]
  },
  { prop: 'contact', label: '联系人' },
  { prop: 'phone', label: '电话' },
  { prop: 'apiProvider', label: '适配器' },
  { prop: 'apiKey', label: 'API Key' },
  { prop: 'status', label: '状态', type: 'status' }
]
</script>
