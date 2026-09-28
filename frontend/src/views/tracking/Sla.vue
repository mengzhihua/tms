<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <h2>在途超时预警</h2>
        <el-button type="primary" @click="scan">立即扫描</el-button>
      </div>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-statistic title="未关闭异常" :value="exceptionTotal" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="未处理围栏告警" :value="summary.unhandledGeofenceAlerts || 0" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="两小时内超时运单" :value="summary.imminentWaybills?.length || 0" />
        </el-col>
      </el-row>
    </div>
    <div class="card">
      <h3>即将超时运单</h3>
      <el-table :data="summary.imminentWaybills || []" border stripe>
        <el-table-column prop="code" label="运单号" />
        <el-table-column prop="carrierCode" label="承运商" />
        <el-table-column prop="status" label="状态" />
        <el-table-column label="承诺到达">
          <template #default="{ row }">{{ fmt(row.promisedArriveTime) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { exceptionApi, report } from '../../api'
import { fmt } from '../../utils'

const summary = reactive({ openExceptions: {}, unhandledGeofenceAlerts: 0, imminentWaybills: [] })
const exceptionTotal = computed(() =>
  Object.values(summary.openExceptions || {}).reduce((sum, value) => sum + Number(value || 0), 0)
)

async function load() {
  Object.assign(summary, await report.alertSummary({}))
}

async function scan() {
  const count = await exceptionApi.scan()
  ElMessage.success(`扫描完成，新增 ${count || 0} 条异常`)
  await load()
}

onMounted(load)
</script>
