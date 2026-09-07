<template>
  <div class="page">
    <el-row :gutter="12">
      <el-col :span="16">
        <div class="card map-card">
          <h3>车辆位置</h3>
          <MiniMap :sites="sites" :vehicles="vehicles" :fences="fences" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="card">
          <div class="toolbar">
            <el-button type="primary" @click="load">刷新</el-button>
          </div>
          <el-table :data="vehicles" size="small" @row-click="selected = $event">
            <el-table-column prop="plateNo" label="车牌" />
            <el-table-column prop="status" label="状态" />
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button link @click="simulate(row)">模拟行驶</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="card" style="margin-top: 12px">
          <h3>GPS 上报</h3>
          <el-form :model="gps" label-width="90px">
            <el-form-item label="车牌"><el-input v-model="gps.vehiclePlate" /></el-form-item>
            <el-form-item label="经度"><el-input-number v-model="gps.lng" /></el-form-item>
            <el-form-item label="纬度"><el-input-number v-model="gps.lat" /></el-form-item>
            <el-button type="primary" @click="submitGps">上报</el-button>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { basic, tracking } from '../../api'
import MiniMap from '../../components/MiniMap.vue'

const vehicles = ref([])
const sites = ref([])
const fences = ref([])
const selected = ref(null)
const gps = reactive({ vehiclePlate: '', lng: 121.47, lat: 31.23 })

async function load() {
  const [vehicleRows, siteRows, fenceRows] = await Promise.all([
    tracking.vehicles(),
    basic.site.list({ size: 200 }),
    basic.geofence.list({ size: 200 })
  ])
  vehicles.value = (vehicleRows || []).map((item) => item.vehicle).filter(Boolean)
  sites.value = siteRows || []
  fences.value = fenceRows || []
}

async function submitGps() {
  await tracking.gps(gps)
  ElMessage.success('GPS 已上报')
  await load()
}

async function simulate(row) {
  const waybillId = row.waybillId || selected.value?.waybill?.id
  if (!waybillId) {
    ElMessage.warning('车辆没有在途运单')
    return
  }
  await tracking.simulate(waybillId, 10)
  ElMessage.success('模拟行驶完成')
  await load()
}

onMounted(load)
</script>
