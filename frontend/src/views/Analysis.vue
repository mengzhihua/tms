<template>
  <div class="page">
    <el-row :gutter="12">
      <el-col :span="12"><div class="card"><h3>SLA 承运商分析</h3><Chart :option="slaOption" /></div></el-col>
      <el-col :span="12"><div class="card"><h3>运输签收趋势</h3><Chart :option="trendOption" /></div></el-col>
    </el-row>
    <div class="card" style="margin-top: 12px">
      <h3>质量分析</h3>
      <el-table :data="quality" border><el-table-column prop="carrier" label="承运商" /><el-table-column prop="exceptionCount" label="异常数" /></el-table>
    </div>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import Chart from '../components/Chart.vue'
import { report } from '../api'
const sla = ref([])
const trend = ref([])
const quality = ref([])
const slaOption = computed(() => ({
  xAxis: { type: 'category', data: sla.value.map((item) => item.carrier) },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', data: sla.value.map((item) => item.onTimeRate) }]
}))
const trendOption = computed(() => ({
  xAxis: { type: 'category', data: trend.value.map((item) => item.date) },
  yAxis: { type: 'value' },
  series: [{ type: 'line', data: trend.value.map((item) => item.signed) }]
}))
onMounted(async () => {
  sla.value = await report.sla({})
  trend.value = await report.transitSign({})
  quality.value = await report.quality({})
})
</script>
