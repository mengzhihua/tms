<template>
  <div class="page">
    <el-row :gutter="12">
      <el-col :span="15">
        <div class="card">
          <div class="toolbar">
            <el-button type="primary" @click="load">刷新待调度订单</el-button>
            <span class="muted">已选 {{ selected.length }} 单</span>
          </div>
          <el-table :data="orders" border @selection-change="selected = $event">
            <el-table-column type="selection" width="50" />
            <el-table-column prop="code" label="订单号" />
            <el-table-column prop="consigneeName" label="收货人" />
            <el-table-column prop="totalWeightKg" label="重量(kg)" />
            <el-table-column prop="totalVolumeM3" label="体积(m³)" />
            <el-table-column prop="priority" label="优先级" />
          </el-table>
          <div class="dispatch-summary">
            <span>已选合计重量：{{ selectedWeight.toFixed(3) }} kg</span>
            <span>已选合计体积：{{ selectedVolume.toFixed(3) }} m³</span>
          </div>
        </div>
      </el-col>
      <el-col :span="9">
        <div class="card">
          <h3>创建运单</h3>
          <el-form :model="form" label-width="100px">
            <el-form-item label="承运商">
              <el-select v-model="form.carrierCode" filterable style="width: 100%" @change="carrierChanged">
                <el-option
                  v-for="carrier in carriers"
                  :key="carrier.code"
                  :label="`${carrier.code} ${carrier.name || ''}`"
                  :value="carrier.code"
                />
              </el-select>
            </el-form-item>
            <template v-if="!isThirdParty">
              <el-form-item label="车辆">
                <el-select v-model="form.vehicleId" filterable style="width: 100%" @change="checkLoad">
                  <el-option
                    v-for="vehicle in idleVehicles"
                    :key="vehicle.id"
                    :label="vehicleLabel(vehicle)"
                    :value="vehicle.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="司机">
                <el-select v-model="form.driverCode" filterable style="width: 100%">
                  <el-option
                    v-for="driver in filteredDrivers"
                    :key="driver.code"
                    :label="`${driver.code} ${driver.name || ''}`"
                    :value="driver.code"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="线路">
                <el-select v-model="form.routeCode" filterable clearable style="width: 100%">
                  <el-option
                    v-for="route in routes"
                    :key="route.code"
                    :label="`${route.code} ${route.name || ''}`"
                    :value="route.code"
                  />
                </el-select>
              </el-form-item>
            </template>
            <el-form-item label="起点站">
              <el-select v-model="form.fromSiteCode" filterable style="width: 100%">
                <el-option
                  v-for="site in sites"
                  :key="site.code"
                  :label="`${site.code} ${site.name || ''}`"
                  :value="site.code"
                />
              </el-select>
            </el-form-item>
            <template v-if="!isThirdParty && form.vehicleId">
              <div class="load-check">
                <div class="load-check-title">
                  <span>载重率</span>
                  <strong :class="{ 'danger-text': loadCheckResult.weightRate > 100 }">
                    {{ loadCheckResult.weightRate.toFixed(1) }}%
                  </strong>
                </div>
                <el-progress
                  :percentage="progressValue(loadCheckResult.weightRate)"
                  :status="progressStatus(loadCheckResult.weightRate)"
                  :format="() => `${loadCheckResult.weightRate.toFixed(1)}%`"
                />
                <div class="load-check-title">
                  <span>容积率</span>
                  <strong :class="{ 'danger-text': loadCheckResult.volumeRate > 100 }">
                    {{ loadCheckResult.volumeRate.toFixed(1) }}%
                  </strong>
                </div>
                <el-progress
                  :percentage="progressValue(loadCheckResult.volumeRate)"
                  :status="progressStatus(loadCheckResult.volumeRate)"
                  :format="() => `${loadCheckResult.volumeRate.toFixed(1)}%`"
                />
              </div>
            </template>
            <el-form-item>
              <el-button
                type="primary"
                :disabled="!canCreate"
                :loading="creating"
                @click="create"
              >
                创建运单
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { basic, dispatch, order, waybill } from '../../api'

const orders = ref([])
const selected = ref([])
const carriers = ref([])
const idleVehicles = ref([])
const drivers = ref([])
const routes = ref([])
const sites = ref([])
const creating = ref(false)
const loadCheckResult = reactive({ weightRate: 0, volumeRate: 0 })
const form = reactive({
  carrierCode: 'SELF01',
  vehicleId: null,
  driverCode: '',
  routeCode: '',
  fromSiteCode: 'WH01'
})

const selectedWeight = computed(() =>
  selected.value.reduce((sum, item) => sum + Number(item.totalWeightKg || 0), 0)
)
const selectedVolume = computed(() =>
  selected.value.reduce((sum, item) => sum + Number(item.totalVolumeM3 || 0), 0)
)
const carrier = computed(() => carriers.value.find((item) => item.code === form.carrierCode))
const isThirdParty = computed(() => carrier.value?.type === 'THIRD_PARTY')
const filteredDrivers = computed(() =>
  drivers.value.filter(
    (driver) => !form.carrierCode || !driver.carrierCode || driver.carrierCode === form.carrierCode
  )
)
const exceedsCapacity = computed(
  () => loadCheckResult.weightRate > 100 || loadCheckResult.volumeRate > 100
)
const canCreate = computed(
  () =>
    selected.value.length > 0 &&
    Boolean(form.carrierCode) &&
    Boolean(form.fromSiteCode) &&
    (isThirdParty.value || (form.vehicleId && form.driverCode && !exceedsCapacity.value))
)

function vehicleLabel(vehicle) {
  return `${vehicle.plateNo} ${vehicle.vehicleType || ''} ${vehicle.maxWeightKg || 0}kg / ${vehicle.maxVolumeM3 || 0}m³`
}

function progressValue(value) {
  return Math.min(100, Number(value || 0))
}

function progressStatus(value) {
  return Number(value || 0) > 100 ? 'exception' : undefined
}

async function loadOptions() {
  const [carrierRows, vehicleRows, driverRows, routeRows, siteRows] = await Promise.all([
    basic.carrier.list({ size: 200 }),
    basic.vehicle.list({ status: 'IDLE', size: 200 }),
    basic.driver.list({ size: 200 }),
    basic.route.list({ size: 200 }),
    basic.site.list({ size: 200 })
  ])
  carriers.value = carrierRows || []
  idleVehicles.value = (vehicleRows || []).filter((item) => item.status === 'IDLE')
  drivers.value = driverRows || []
  routes.value = routeRows || []
  sites.value = siteRows || []
  if (!form.fromSiteCode && sites.value.some((site) => site.code === 'WH01')) {
    form.fromSiteCode = 'WH01'
  }
  carrierChanged()
}

async function load() {
  orders.value = await dispatch.pending({})
  await checkLoad()
}

async function carrierChanged() {
  if (isThirdParty.value) {
    form.vehicleId = null
    form.driverCode = ''
    form.routeCode = ''
    loadCheckResult.weightRate = 0
    loadCheckResult.volumeRate = 0
    return
  }
  const firstDriver = filteredDrivers.value[0]
  if (!filteredDrivers.value.some((driver) => driver.code === form.driverCode)) {
    form.driverCode = firstDriver?.code || ''
  }
  await checkLoad()
}

async function checkLoad() {
  if (isThirdParty.value || !form.vehicleId || !selected.value.length) {
    loadCheckResult.weightRate = 0
    loadCheckResult.volumeRate = 0
    return
  }
  const result = await order.loadCheck({
    vehicleId: form.vehicleId,
    orderIds: selected.value.map((item) => item.id)
  })
  loadCheckResult.weightRate = Number(result?.weightRate || 0)
  loadCheckResult.volumeRate = Number(result?.volumeRate || 0)
}

async function create() {
  creating.value = true
  try {
    const created = await waybill.create({
      ...form,
      orderIds: selected.value.map((item) => item.id)
    })
    ElMessage.success(`运单 ${created.code} 创建成功`)
    selected.value = []
    await load()
  } finally {
    creating.value = false
  }
}

watch(selected, checkLoad)
watch(() => form.vehicleId, checkLoad)
onMounted(async () => {
  await loadOptions()
  await load()
})
</script>
