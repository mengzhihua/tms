<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.handled" placeholder="处理状态" clearable>
          <el-option label="未处理" :value="false" />
          <el-option label="已处理" :value="true" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <el-table :data="rows" border stripe>
        <el-table-column prop="waybillCode" label="运单号" />
        <el-table-column prop="geofenceName" label="围栏" />
        <el-table-column prop="alertType" label="类型" />
        <el-table-column label="时间" width="175">
          <template #default="{ row }">{{ fmt(row.alertTime) }}</template>
        </el-table-column>
        <el-table-column prop="handled" label="已处理">
          <template #default="{ row }">{{ row.handled ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button v-if="!row.handled" link type="primary" @click="handle(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { tracking } from '../../api'
import { fmt } from '../../utils'

const rows = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 20, handled: null })

async function load() {
  const page = await tracking.alerts(query)
  rows.value = page.records || []
  total.value = page.total || 0
}

async function handle(row) {
  await tracking.handleAlert(row.id, { handler: 'admin', handleRemark: '前端已处理' })
  ElMessage.success('告警已处理')
  await load()
}

onMounted(load)
</script>
