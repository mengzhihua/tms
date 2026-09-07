<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="订单号/来源单号/收货人" clearable />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openForm()">新建订单</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" border stripe size="small">
        <el-table-column prop="code" label="订单号" width="150" />
        <el-table-column prop="customerCode" label="客户" width="100" />
        <el-table-column prop="fromSiteCode" label="起点" width="90" />
        <el-table-column prop="consigneeName" label="收货人" width="110" />
        <el-table-column prop="totalQty" label="件数" width="80" />
        <el-table-column prop="chargeableWeightKg" label="计费重(kg)" width="110" />
        <el-table-column label="创建时间" width="175">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'CREATED'"
              link
              type="danger"
              @click="cancel(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @change="load"
        />
      </div>
    </div>

    <el-dialog v-model="visible" :title="form.id ? '编辑订单' : '新建订单'" width="900px">
      <el-form :model="form" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="来源单号"><el-input v-model="form.sourceNo" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户">
              <el-select
                v-model="form.customerCode"
                filterable
                clearable
                style="width: 100%"
                @change="customerChanged"
              >
                <el-option
                  v-for="customer in customers"
                  :key="customer.code"
                  :label="`${customer.code} ${customer.name || ''}`"
                  :value="customer.code"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订单类型">
              <el-select v-model="form.orderType" style="width: 100%">
                <el-option label="配送" value="DELIVERY" />
                <el-option label="调拨" value="TRANSFER" />
                <el-option label="退货" value="RETURN" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="起点站"><el-input v-model="form.fromSiteCode" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收货人"><el-input v-model="form.consigneeName" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收货电话"><el-input v-model="form.consigneePhone" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收货地址"><el-input v-model="form.consigneeAddress" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收货经度"><el-input-number v-model="form.consigneeLng" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收货纬度"><el-input-number v-model="form.consigneeLat" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="优先级"><el-input-number v-model="form.priority" :min="0" /></el-form-item>
          </el-col>
        </el-row>
        <el-divider>订单明细</el-divider>
        <el-table :data="form.lines" border size="small">
          <el-table-column label="货品编码">
            <template #default="{ row }"><el-input v-model="row.itemCode" /></template>
          </el-table-column>
          <el-table-column label="货品名称">
            <template #default="{ row }"><el-input v-model="row.itemName" /></template>
          </el-table-column>
          <el-table-column label="数量" width="100">
            <template #default="{ row }"><el-input-number v-model="row.qty" :min="1" /></template>
          </el-table-column>
          <el-table-column label="长(cm)" width="100">
            <template #default="{ row }"><el-input-number v-model="row.lengthCm" :min="0" /></template>
          </el-table-column>
          <el-table-column label="宽(cm)" width="100">
            <template #default="{ row }"><el-input-number v-model="row.widthCm" :min="0" /></template>
          </el-table-column>
          <el-table-column label="高(cm)" width="100">
            <template #default="{ row }"><el-input-number v-model="row.heightCm" :min="0" /></template>
          </el-table-column>
          <el-table-column label="单件重量" width="110">
            <template #default="{ row }"><el-input-number v-model="row.weightKg" :min="0" /></template>
          </el-table-column>
          <el-table-column label="体积(m³)" width="110">
            <template #default="{ row }">{{ lineVolume(row).toFixed(6) }}</template>
          </el-table-column>
          <el-table-column label="体积重(kg)" width="110">
            <template #default="{ row }">{{ lineVolumetricWeight(row).toFixed(3) }}</template>
          </el-table-column>
          <el-table-column label="计费重(kg)" width="110">
            <template #default="{ row }">{{ lineChargeableWeight(row).toFixed(3) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="70">
            <template #default="{ $index }">
              <el-button link type="danger" @click="form.lines.splice($index, 1)">删</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button style="margin-top: 8px" @click="addLine">添加明细</el-button>
        <div class="order-summary">
          <span>合计体积：{{ estimate.totalVolume.toFixed(6) }} m³</span>
          <span>合计重量：{{ estimate.totalWeight.toFixed(3) }} kg</span>
          <span>体积重：{{ estimate.volumetricWeight.toFixed(3) }} kg</span>
          <strong>计费重：{{ estimate.chargeableWeight.toFixed(3) }} kg</strong>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { basic, order } from '../../api'
import StatusTag from '../../components/StatusTag.vue'
import { fmt } from '../../utils'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const visible = ref(false)
const form = ref({})
const customers = ref([])
const query = reactive({ current: 1, size: 20, keyword: '' })

const estimate = computed(() => {
  const lines = form.value.lines || []
  const totalVolume = lines.reduce((sum, line) => sum + lineVolume(line), 0)
  const totalWeight = lines.reduce(
    (sum, line) => sum + Number(line.weightKg || 0) * Number(line.qty || 0),
    0
  )
  const ratio = Number(form.value.volumeRatio || 6000)
  const volumetricWeight = ratio > 0 ? (totalVolume * 1000000) / ratio : 0
  return {
    totalVolume,
    totalWeight,
    volumetricWeight,
    chargeableWeight: Math.max(totalWeight, volumetricWeight)
  }
})

async function load() {
  loading.value = true
  try {
    const page = await order.page(query)
    rows.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function emptyForm() {
  return {
    orderType: 'DELIVERY',
    volumeRatio: 6000,
    priority: 0,
    lines: []
  }
}

function lineVolume(line) {
  const unitVolume =
    Number(line.volumeM3 || 0) ||
    (Number(line.lengthCm || 0) * Number(line.widthCm || 0) * Number(line.heightCm || 0)) /
      1000000
  return unitVolume * Number(line.qty || 0)
}

function lineVolumetricWeight(line) {
  const ratio = Number(form.value.volumeRatio || 6000)
  return ratio > 0 ? (lineVolume(line) * 1000000) / ratio : 0
}

function lineChargeableWeight(line) {
  return Math.max(lineVolumetricWeight(line), Number(line.weightKg || 0) * Number(line.qty || 0))
}

async function openForm(row) {
  form.value = row ? await order.get(row.id) : emptyForm()
  if (!form.value.lines) {
    form.value.lines = []
  }
  visible.value = true
}

function customerChanged() {
  const customer = customers.value.find((item) => item.code === form.value.customerCode)
  if (!customer) {
    return
  }
  form.value.consigneeName = customer.contact || customer.name
  form.value.consigneePhone = customer.phone
  form.value.consigneeAddress = customer.address
  form.value.consigneeLng = customer.lng
  form.value.consigneeLat = customer.lat
}

function addLine() {
  form.value.lines.push({ qty: 1, lengthCm: 0, widthCm: 0, heightCm: 0, weightKg: 0 })
}

async function save() {
  if (form.value.id) {
    await order.update(form.value.id, form.value)
  } else {
    await order.create(form.value)
  }
  ElMessage.success('保存成功')
  visible.value = false
  await load()
}

async function cancel(row) {
  await order.cancel(row.id)
  ElMessage.success('订单已取消')
  await load()
}

onMounted(async () => {
  customers.value = (await basic.customer.list({ size: 200 })) || []
  await load()
})
</script>
