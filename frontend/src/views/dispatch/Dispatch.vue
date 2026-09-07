<template>
  <div class="page">
    <el-row :gutter="12">
      <el-col :span="15">
        <div class="card">
          <div class="toolbar">
            <el-button type="primary" @click="load">刷新待调度订单</el-button>
          </div>
          <el-table :data="orders" border @selection-change="selected = $event">
            <el-table-column type="selection" width="50" />
            <el-table-column prop="code" label="订单号" />
            <el-table-column prop="consigneeName" label="收货人" />
            <el-table-column prop="totalWeightKg" label="重量(kg)" />
            <el-table-column prop="totalVolumeM3" label="体积(m³)" />
            <el-table-column prop="priority" label="优先级" />
          </el-table>
        </div>
      </el-col>
      <el-col :span="9">
        <div class="card">
          <h3>创建运单</h3>
          <el-form label-width="100px">
            <el-form-item label="承运商">
              <el-input v-model="form.carrierCode" placeholder="SELF01 / SF" />
            </el-form-item>
            <el-form-item label="车辆ID">
              <el-input-number v-model="form.vehicleId" :min="1" style="width: 100%" />
            </el-form-item>
            <el-form-item label="司机编码">
              <el-input v-model="form.driverCode" />
            </el-form-item>
            <el-form-item label="线路编码">
              <el-input v-model="form.routeCode" />
            </el-form-item>
            <el-form-item label="起点站">
              <el-input v-model="form.fromSiteCode" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :disabled="!selected.length" @click="create">创建运单</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dispatch, waybill } from '../../api'

const orders = ref([])
const selected = ref([])
const form = reactive({ carrierCode: 'SELF01', driverCode: 'DRV01', fromSiteCode: 'WH01' })

async function load() {
  orders.value = await dispatch.pending({})
}

async function create() {
  await waybill.create({
    ...form,
    orderIds: selected.value.map((item) => item.id)
  })
  ElMessage.success('运单创建成功')
  selected.value = []
  await load()
}

onMounted(load)
</script>
