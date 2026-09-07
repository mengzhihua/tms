<template>
  <div class="page">
    <el-row :gutter="12">
      <el-col v-for="stat in stats" :key="stat.label" :span="4">
        <div class="stat">
          <div class="label">{{ stat.label }}</div>
          <div class="value" :style="{ color: stat.color }">{{ stat.value }}</div>
        </div>
      </el-col>
    </el-row>
    <el-row :gutter="12" style="margin-top: 12px">
      <el-col :span="12">
        <div class="card">
          <h3>订单状态分布</h3>
          <el-table :data="orderCounts" size="small">
            <el-table-column prop="status" label="状态" />
            <el-table-column prop="count" label="数量" />
          </el-table>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="card">
          <h3>运单状态分布</h3>
          <el-table :data="waybillCounts" size="small">
            <el-table-column prop="status" label="状态" />
            <el-table-column prop="count" label="数量" />
          </el-table>
        </div>
      </el-col>
    </el-row>
    <el-row :gutter="12" style="margin-top: 12px">
      <el-col :span="12">
        <div class="card">
          <h3>待调度订单 TOP10</h3>
          <el-table :data="data.pendingOrdersTop10 || []" size="small">
            <el-table-column prop="code" label="订单号" />
            <el-table-column prop="consigneeName" label="收货人" />
            <el-table-column prop="priority" label="优先级" />
          </el-table>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="card">
          <h3>最近事件</h3>
          <el-table :data="data.recentEvents || []" size="small">
            <el-table-column prop="eventType" label="类型" width="140" />
            <el-table-column prop="description" label="描述" />
            <el-table-column prop="eventTime" label="时间" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { dashboard } from '../api'

const data = ref({})

const toRows = (value) =>
  Object.entries(value || {}).map(([status, count]) => ({ status, count }))

const stats = computed(() => [
  { label: '在途车辆', value: data.value.inTransitVehicles ?? '-', color: '#409eff' },
  { label: '空闲车辆', value: data.value.idleVehicles ?? '-', color: '#67c23a' },
  { label: '今日运单', value: data.value.todayWaybills ?? '-' },
  { label: '未处理告警', value: data.value.unhandledAlerts ?? '-', color: '#f56c6c' },
  { label: '运费合计', value: data.value.monthFreightAmount ?? '-', color: '#e6a23c' },
  { label: '待调度订单', value: data.value.pendingOrdersTop10?.length ?? '-' }
])

const orderCounts = computed(() => toRows(data.value.orderStatusCounts))
const waybillCounts = computed(() => toRows(data.value.waybillStatusCounts))

onMounted(async () => {
  data.value = await dashboard()
})
</script>
