<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.waybillCode" placeholder="运单号" clearable />
        <el-select v-model="query.eventType" placeholder="事件类型" clearable>
          <el-option v-for="type in types" :key="type" :label="type" :value="type" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <el-table :data="rows" border stripe>
        <el-table-column prop="waybillCode" label="运单号" />
        <el-table-column prop="eventType" label="事件类型" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="source" label="来源" />
        <el-table-column prop="eventTime" label="时间" />
        <el-table-column prop="lng" label="经度" />
        <el-table-column prop="lat" label="纬度" />
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { tracking } from '../../api'

const types = ['CREATED', 'DISPATCHED', 'DEPARTED', 'GPS', 'GEOFENCE_ENTER', 'GEOFENCE_EXIT', 'ARRIVED', 'SIGNED', 'THIRD_PARTY']
const rows = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 20, waybillCode: '', eventType: '' })

async function load() {
  const page = await tracking.events(query)
  rows.value = page.records || []
  total.value = page.total || 0
}

onMounted(load)
</script>
