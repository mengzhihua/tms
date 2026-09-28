<template>
  <CrudPage title="客户" :api="basic.customer" :columns="columns" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'

const columns = [
  { prop: 'code', label: '编码', required: true },
  { prop: 'name', label: '名称', required: true },
  { prop: 'contact', label: '联系人' },
  { prop: 'phone', label: '电话' },
  { prop: 'province', label: '省份' },
  { prop: 'city', label: '城市' },
  { prop: 'address', label: '地址', span: 24 },
  { prop: 'lng', label: '经度', type: 'number' },
  { prop: 'lat', label: '纬度', type: 'number' },
  { prop: 'apiKey', label: '开放接口密钥' },
  { prop: 'callbackUrl', label: '回调地址', span: 24 },
  { prop: 'pushEnabled', label: '启用回调', type: 'bool' }
]
</script>
