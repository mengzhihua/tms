<template>
  <div class="page">
    <div class="card">
      <div class="toolbar"><el-button @click="load">刷新</el-button></div>
      <el-table :data="rows" border stripe>
        <el-table-column prop="code" label="回单编号" />
        <el-table-column prop="waybillCode" label="运单" />
        <el-table-column prop="orderCode" label="订单" />
        <el-table-column prop="receiptType" label="类型" />
        <el-table-column prop="status" label="状态" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button link @click="change(row, 'return')">收回</el-button>
            <el-button link @click="change(row, 'archive')">归档</el-button>
            <el-button link type="danger" @click="change(row, 'lost')">丢失</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { pod } from '../api'
const rows = ref([])
async function load() {
  const data = await pod.page({ current: 1, size: 100 })
  rows.value = data.records || []
}
async function change(row, action) {
  await pod[action](row.id)
  load()
}
onMounted(load)
</script>
