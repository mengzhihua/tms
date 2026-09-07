<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="运单号/车牌/三方单号" clearable />
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <el-table :data="rows" border stripe>
        <el-table-column prop="code" label="运单号" />
        <el-table-column prop="carrierCode" label="承运商" />
        <el-table-column prop="vehiclePlate" label="车辆" />
        <el-table-column prop="thirdPartyNo" label="三方单号" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="360">
          <template #default="{ row }">
            <el-button v-if="row.status === 'CREATED'" link @click="action(row, 'dispatch')">调度</el-button>
            <el-button v-if="row.status === 'DISPATCHED'" link @click="action(row, 'depart')">发车</el-button>
            <el-button v-if="row.status === 'IN_TRANSIT'" link @click="action(row, 'arrive')">到达</el-button>
            <el-button v-if="row.thirdPartyNo" link @click="action(row, 'syncTrack')">同步轨迹</el-button>
            <el-button link @click="detail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="visible" title="运单详情" size="55%">
      <el-descriptions v-if="current" border :column="2">
        <el-descriptions-item label="运单号">{{ current.code }}</el-descriptions-item>
        <el-descriptions-item label="状态"><StatusTag :value="current.status" /></el-descriptions-item>
        <el-descriptions-item label="承运商">{{ current.carrierCode }}</el-descriptions-item>
        <el-descriptions-item label="三方单号">{{ current.thirdPartyNo }}</el-descriptions-item>
      </el-descriptions>
      <h3>订单</h3>
      <el-table :data="current?.orders || []" size="small">
        <el-table-column prop="code" label="订单号" />
        <el-table-column prop="status" label="状态" />
        <el-table-column label="签收">
          <template #default="{ row }">
            <el-button
              v-if="!['DELIVERED', 'EXCEPTION'].includes(row.status)"
              link
              type="primary"
              @click="sign(row)"
            >
              签收
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <h3>事件时间线</h3>
      <el-timeline>
        <el-timeline-item v-for="event in current?.events || []" :key="event.id">
          {{ event.eventTime }} {{ event.eventType }} {{ event.description }}
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { waybill } from '../../api'
import StatusTag from '../../components/StatusTag.vue'

const rows = ref([])
const query = reactive({ current: 1, size: 20, keyword: '' })
const visible = ref(false)
const current = ref(null)

async function load() {
  const page = await waybill.page(query)
  rows.value = page.records || []
}

async function action(row, name) {
  await waybill[name](row.id)
  ElMessage.success('操作成功')
  await load()
}

async function detail(row) {
  current.value = await waybill.get(row.id)
  visible.value = true
}

async function sign(order) {
  await waybill.sign(current.value.id, { orderId: order.id, signer: '前端签收' })
  ElMessage.success('签收成功')
  await detail(current.value)
  await load()
}

onMounted(load)
</script>
