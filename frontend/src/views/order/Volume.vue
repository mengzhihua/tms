<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input-number v-model="volumeRatio" :min="1" />
        <span>抛比</span>
        <el-button type="primary" @click="calculate">计算</el-button>
        <el-button @click="addLine">添加明细</el-button>
      </div>
      <el-table :data="lines" border>
        <el-table-column label="数量"><template #default="{ row }"><el-input-number v-model="row.qty" /></template></el-table-column>
        <el-table-column label="长(cm)"><template #default="{ row }"><el-input-number v-model="row.lengthCm" /></template></el-table-column>
        <el-table-column label="宽(cm)"><template #default="{ row }"><el-input-number v-model="row.widthCm" /></template></el-table-column>
        <el-table-column label="高(cm)"><template #default="{ row }"><el-input-number v-model="row.heightCm" /></template></el-table-column>
        <el-table-column label="单件重量(kg)"><template #default="{ row }"><el-input-number v-model="row.weightKg" /></template></el-table-column>
        <el-table-column label="操作"><template #default="{ $index }"><el-button link type="danger" @click="lines.splice($index, 1)">删除</el-button></template></el-table-column>
      </el-table>
      <el-descriptions v-if="result" style="margin-top: 16px" border :column="4">
        <el-descriptions-item label="总件数">{{ result.totalQty }}</el-descriptions-item>
        <el-descriptions-item label="总重量">{{ result.totalWeightKg }}</el-descriptions-item>
        <el-descriptions-item label="总体积">{{ result.totalVolumeM3 }}</el-descriptions-item>
        <el-descriptions-item label="计费重">{{ result.chargeableWeightKg }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { order } from '../../api'

const volumeRatio = ref(6000)
const lines = ref([{ qty: 1, lengthCm: 100, widthCm: 50, heightCm: 20, weightKg: 1 }])
const result = ref(null)

function addLine() {
  lines.value.push({ qty: 1, lengthCm: 0, widthCm: 0, heightCm: 0, weightKg: 0 })
}

async function calculate() {
  result.value = await order.volume({ lines: lines.value, volumeRatio: volumeRatio.value })
}
</script>
