<template>
  <div class="page">
    <div class="card">
      <h3>运费试算</h3>
      <el-form :model="form" label-width="110px" style="max-width: 620px">
        <el-form-item label="承运商"><el-input v-model="form.carrierCode" /></el-form-item>
        <el-form-item label="计费类型">
          <el-select v-model="form.chargeType" style="width: 100%">
            <el-option label="重量" value="WEIGHT" />
            <el-option label="体积" value="VOLUME" />
            <el-option label="件数" value="PIECE" />
            <el-option label="里程" value="DISTANCE" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单ID"><el-input-number v-model="form.orderId" /></el-form-item>
        <el-form-item label="距离(km)"><el-input-number v-model="form.distanceKm" /></el-form-item>
        <el-button type="primary" @click="calculate">试算</el-button>
      </el-form>
      <el-descriptions v-if="result" border :column="2" style="max-width: 620px; margin-top: 20px">
        <el-descriptions-item label="规则">{{ result.rule?.name }}</el-descriptions-item>
        <el-descriptions-item label="计费量">{{ result.quantity }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ result.amount }}</el-descriptions-item>
        <el-descriptions-item label="计算明细">{{ result.calcDetail }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { billing } from '../../api'

const form = reactive({ carrierCode: 'SELF01', chargeType: 'DISTANCE', distanceKm: 200 })
const result = ref(null)

async function calculate() {
  result.value = await billing.calc(form)
}
</script>
