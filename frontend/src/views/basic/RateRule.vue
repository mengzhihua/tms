<template>
  <CrudPage title="计费规则" :api="basic.rateRule" :columns="columns" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { basic } from '../../api'

const columns = [
  { prop: 'code', label: '编码', required: true },
  { prop: 'name', label: '名称' },
  { prop: 'carrierCode', label: '承运商(空=通用)' },
  { prop: 'regionCode', label: '区域(空=通用)' },
  { prop: 'serviceLevelCode', label: '服务时效(空=通用)' },
  { prop: 'chargeType', label: '计费类型' },
  { prop: 'firstUnit', label: '首单位', type: 'number' },
  { prop: 'firstPrice', label: '首价', type: 'number' },
  { prop: 'addUnit', label: '续单位', type: 'number' },
  { prop: 'addPrice', label: '续价', type: 'number' },
  { prop: 'minCharge', label: '最低收费', type: 'number' },
  { prop: 'volumeRatio', label: '抛比', type: 'number' },
  { prop: 'priority', label: '优先级', type: 'number' },
  { prop: 'status', label: '状态', type: 'status' }
]
</script>
