<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.status" placeholder="状态" clearable>
          <el-option label="未计费" value="UNBILLED" />
          <el-option label="已计费" value="BILLED" />
          <el-option label="已支付" value="PAID" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <el-table :data="rows" border stripe>
        <el-table-column prop="code" label="计费单号" />
        <el-table-column prop="waybillCode" label="运单号" />
        <el-table-column prop="orderCode" label="订单号" />
        <el-table-column prop="chargeType" label="类型" />
        <el-table-column prop="quantity" label="计费量" />
        <el-table-column prop="amount" label="金额" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button v-if="row.status === 'BILLED'" link type="primary" @click="pay(row)">支付</el-button>
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
import { billing } from '../../api'
import StatusTag from '../../components/StatusTag.vue'

const rows = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 20, status: '' })

async function load() {
  const page = await billing.page(query)
  rows.value = page.records || []
  total.value = page.total || 0
}

async function pay(row) {
  await billing.pay(row.id)
  ElMessage.success('已支付')
  await load()
}

onMounted(load)
</script>
