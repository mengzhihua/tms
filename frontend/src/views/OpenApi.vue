<template>
  <div class="page">
    <div class="card">
      <h2>开放接口</h2>
      <p>请求头：<code>X-Api-Key: demo-key-001</code></p>
      <p>POST /open/orders</p>
      <p>GET /open/orders/{code}/track</p>
      <p>GET /open/orders/track?sourceNo=</p>
      <el-table :data="rows" border>
        <el-table-column prop="orderCode" label="订单" />
        <el-table-column prop="eventType" label="事件" />
        <el-table-column prop="url" label="回调地址" />
        <el-table-column prop="status" label="状态" />
        <el-table-column label="操作"><template #default="{ row }"><el-button link @click="retry(row)">重试</el-button></template></el-table-column>
      </el-table>
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { pushLog } from '../api'
const rows = ref([])
async function load() {
  const data = await pushLog.page({ current: 1, size: 100 })
  rows.value = data.records || []
}
async function retry(row) {
  await pushLog.retry(row.id)
  load()
}
onMounted(load)
</script>
