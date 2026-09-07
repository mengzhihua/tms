<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-button type="primary" @click="compute">计算本月评级</el-button>
      </div>
      <el-table :data="rows" border stripe>
        <el-table-column prop="carrierCode" label="承运商" />
        <el-table-column prop="period" label="周期" />
        <el-table-column prop="waybillCount" label="运单数" />
        <el-table-column prop="onTimeRate" label="准时率" />
        <el-table-column prop="exceptionRate" label="异常率" />
        <el-table-column prop="score" label="得分" />
        <el-table-column prop="grade" label="等级" />
      </el-table>
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { rating } from '../api'
const rows = ref([])
async function load() {
  const data = await rating.page({ current: 1, size: 100 })
  rows.value = data.records || []
}
async function compute() {
  await rating.compute(new Date().toISOString().slice(0, 7))
  load()
}
onMounted(load)
</script>
