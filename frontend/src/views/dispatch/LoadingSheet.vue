<template>
  <div class="page">
    <div class="card loading-sheet">
      <div class="toolbar">
        <div>
          <h2>装车单 {{ sheet.waybill?.code || '' }}</h2>
          <div class="muted">封签号：{{ sheet.sealNo || '-' }}</div>
        </div>
        <el-button type="primary" @click="print">打印</el-button>
      </div>
      <el-descriptions v-if="sheet.waybill" border :column="3">
        <el-descriptions-item label="承运商">{{ sheet.waybill.carrierCode }}</el-descriptions-item>
        <el-descriptions-item label="车辆">{{ sheet.waybill.vehiclePlate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="装车人">{{ sheet.waybill.loaderName || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="sheet.orders || []" border stripe style="margin-top: 16px">
        <el-table-column prop="code" label="订单号" />
        <el-table-column prop="consignee" label="收货人" />
        <el-table-column prop="qty" label="件数" />
        <el-table-column prop="weight" label="重量(kg)" />
        <el-table-column prop="volume" label="体积(m³)" />
      </el-table>
      <div class="loading-totals">
        <strong>合计重量：{{ sheet.totals?.weight || 0 }} kg</strong>
        <strong>合计体积：{{ sheet.totals?.volume || 0 }} m³</strong>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { waybill } from '../../api'

const route = useRoute()
const sheet = reactive({ orders: [], totals: {}, waybill: null, sealNo: '' })

async function load() {
  Object.assign(sheet, await waybill.loadingSheet(route.params.id))
}

function print() {
  window.print()
}

onMounted(load)
</script>

<style scoped>
.loading-totals {
  display: flex;
  gap: 32px;
  justify-content: flex-end;
  margin-top: 16px;
}

@media print {
  .toolbar :deep(.el-button) {
    display: none;
  }
}
</style>
