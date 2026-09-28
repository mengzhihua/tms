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
        <el-table-column label="创建时间" width="175">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="540">
          <template #default="{ row }">
            <el-button v-if="row.status === 'CREATED'" link @click="action(row, 'dispatch')">调度</el-button>
            <el-button
              v-if="['CREATED', 'DISPATCHED'].includes(row.status)"
              link
              type="danger"
              @click="action(row, 'cancel')"
            >
              取消
            </el-button>
            <el-button v-if="row.status === 'DISPATCHED'" link @click="action(row, 'depart')">发车</el-button>
            <el-button
              v-if="row.status === 'DISPATCHED' && row.carrierType === 'SELF'"
              link
              type="warning"
              @click="openLoad(row)"
            >
              装车交接
            </el-button>
            <el-button
              v-if="row.carrierType === 'SELF'"
              link
              @click="openLoadingSheet(row)"
            >
              装车单
            </el-button>
            <el-button
              v-if="row.status === 'DISPATCHED' && row.thirdPartyNo"
              link
              @click="action(row, 'syncTrack')"
            >
              同步轨迹
            </el-button>
            <el-button v-if="row.status === 'IN_TRANSIT'" link @click="action(row, 'arrive')">到达</el-button>
            <el-button v-if="row.status === 'DELIVERED'" link @click="action(row, 'close')">关闭</el-button>
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
        <el-descriptions-item label="车辆">{{ current.vehiclePlate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="司机">{{ current.driverName || current.driverCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="线路">{{ current.routeCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="起点站">{{ current.fromSiteCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="三方单号">{{ current.thirdPartyNo }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ fmt(current.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="发车时间">{{ fmt(current.actualDepartTime) }}</el-descriptions-item>
        <el-descriptions-item label="到达时间">{{ fmt(current.actualArriveTime) }}</el-descriptions-item>
        <el-descriptions-item label="承诺到达">{{ fmt(current.promisedArriveTime) }}</el-descriptions-item>
        <el-descriptions-item label="准时">
          <el-tag v-if="current.onTime === true" type="success">准时</el-tag>
          <el-tag v-else-if="current.onTime === false" type="danger">超时</el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="装车状态">{{ current.loadStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="封签号">{{ current.sealNo || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="current" class="toolbar detail-actions">
        <el-button v-if="current.status === 'CREATED'" type="primary" @click="operate('dispatch')">调度</el-button>
        <el-button
          v-if="['CREATED', 'DISPATCHED'].includes(current.status)"
          type="danger"
          plain
          @click="operate('cancel')"
        >
          取消
        </el-button>
        <el-button v-if="current.status === 'DISPATCHED'" type="primary" @click="operate('depart')">发车</el-button>
        <el-button
          v-if="current.status === 'DISPATCHED' && current.carrierType === 'SELF'"
          type="warning"
          @click="openLoad(current)"
        >
          装车交接
        </el-button>
        <el-button
          v-if="current.carrierType === 'SELF'"
          @click="openLoadingSheet(current)"
        >
          装车单
        </el-button>
        <el-button
          v-if="current.status === 'DISPATCHED' && current.thirdPartyNo"
          @click="operate('syncTrack')"
        >
          同步三方轨迹
        </el-button>
        <el-button v-if="current.status === 'IN_TRANSIT'" type="primary" @click="operate('arrive')">到达</el-button>
        <el-button
          v-if="current.status === 'IN_TRANSIT' && current.carrierType !== 'THIRD_PARTY'"
          @click="simulate"
        >
          模拟行驶
        </el-button>
        <el-button v-if="current.status === 'DELIVERED'" type="primary" @click="operate('close')">关闭</el-button>
      </div>
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
              @click="openSign(row)"
            >
              签收
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <h3>事件时间线</h3>
      <el-timeline>
        <el-timeline-item v-for="event in current?.events || []" :key="event.id">
          {{ fmt(event.eventTime) }} {{ event.eventType }} {{ event.description || '' }}
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
    <el-dialog v-model="signVisible" title="订单签收" width="420px">
      <el-form :model="signForm" label-width="90px">
        <el-form-item label="签收人">
          <el-input v-model="signForm.signer" />
        </el-form-item>
        <el-form-item label="异常签收">
          <el-switch v-model="signForm.exception" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="signForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="signVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSign">确认签收</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="loadVisible" title="装车交接" width="520px">
      <el-form :model="loadForm" label-width="90px">
        <el-form-item label="订单">
          <el-checkbox-group v-model="loadForm.orderCodes">
            <el-checkbox
              v-for="item in (current?.orders || [])"
              :key="item.code"
              :label="item.code"
            >
              {{ item.code }} {{ item.consigneeName || '' }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="封签号"><el-input v-model="loadForm.sealNo" /></el-form-item>
        <el-form-item label="装车人"><el-input v-model="loadForm.loaderName" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="loadVisible = false">取消</el-button>
        <el-button type="primary" @click="submitLoad">确认交接</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { tracking, waybill } from '../../api'
import StatusTag from '../../components/StatusTag.vue'
import { fmt } from '../../utils'

const rows = ref([])
const query = reactive({ current: 1, size: 20, keyword: '' })
const visible = ref(false)
const current = ref(null)
const signVisible = ref(false)
const signOrder = ref(null)
const signForm = reactive({ signer: '前端签收', exception: false, remark: '' })
const loadVisible = ref(false)
const loadForm = reactive({ sealNo: '', loaderName: '', orderCodes: [] })
const router = useRouter()

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

async function openLoad(row) {
  current.value = await waybill.get(row.id)
  loadForm.sealNo = current.value.sealNo || ''
  loadForm.loaderName = current.value.loaderName || ''
  loadForm.orderCodes = (current.value.orders || []).map((item) => item.code)
  loadVisible.value = true
}

async function submitLoad() {
  await waybill.load(current.value.id, { ...loadForm })
  ElMessage.success('装车交接完成')
  loadVisible.value = false
  await detail(current.value)
  await load()
}

function openLoadingSheet(row) {
  router.push(`/dispatch/loading-sheet/${row.id}`)
}

async function operate(name) {
  await waybill[name](current.value.id)
  ElMessage.success('操作成功')
  await detail(current.value)
  await load()
}

async function simulate() {
  await tracking.simulate(current.value.id, 10)
  ElMessage.success('模拟行驶完成')
  await detail(current.value)
}

function openSign(order) {
  signOrder.value = order
  signForm.signer = '前端签收'
  signForm.exception = false
  signForm.remark = ''
  signVisible.value = true
}

async function submitSign() {
  await waybill.sign(current.value.id, {
    orderId: signOrder.value.id,
    signer: signForm.signer,
    exception: signForm.exception,
    remark: signForm.remark
  })
  ElMessage.success('签收成功')
  signVisible.value = false
  await detail(current.value)
  await load()
}

onMounted(load)
</script>
