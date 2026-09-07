<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-button type="primary" @click="scan">手动扫描 SLA</el-button>
      </div>
      <el-table :data="rows" border stripe>
        <el-table-column prop="code" label="异常编号" />
        <el-table-column prop="waybillCode" label="运单" />
        <el-table-column prop="type" label="类型" />
        <el-table-column prop="level" label="级别" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="claimStatus" label="理赔状态" />
        <el-table-column prop="description" label="描述" />
      </el-table>
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { exceptionApi } from '../../api'
const rows = ref([])
async function load() {
  const data = await exceptionApi.page({ current: 1, size: 100 })
  rows.value = data.records || []
}
async function scan() {
  await exceptionApi.scan()
  ElMessage.success('扫描完成')
  load()
}
onMounted(load)
</script>
