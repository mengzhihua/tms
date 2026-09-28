<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.status" placeholder="状态" clearable>
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.type" placeholder="异常类型" clearable>
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.claimStatus" placeholder="理赔状态" clearable>
          <el-option v-for="item in claimStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="primary" @click="openCreate">登记异常</el-button>
        <el-button type="warning" @click="scan">手动扫描 SLA</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe size="small">
        <el-table-column prop="code" label="异常编号" width="150" />
        <el-table-column prop="waybillCode" label="运单" width="180" />
        <el-table-column prop="carrierCode" label="承运商" width="100" />
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)">{{ labelOf(typeOptions, row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.level)">{{ labelOf(levelOptions, row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ labelOf(statusOptions, row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="90" />
        <el-table-column label="理赔状态" width="100">
          <template #default="{ row }">
            <el-tag>{{ labelOf(claimStatusOptions, row.claimStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="claimAmount" label="理赔金额" width="100" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'CLOSED'" link type="primary" @click="openHandle(row)">
              处理
            </el-button>
            <el-button
              v-if="row.claimStatus === 'NONE' && row.status !== 'CLOSED'"
              link
              type="warning"
              @click="openClaim(row)"
            >
              申请理赔
            </el-button>
            <el-button v-if="row.claimStatus === 'APPLIED'" link type="success" @click="openAudit(row)">
              审核
            </el-button>
            <el-button v-if="row.claimStatus === 'APPROVED'" link type="danger" @click="pay(row)">
              支付
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="createVisible" title="登记异常" width="620px">
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="运单">
          <el-select v-model="createForm.waybill" clearable filterable style="width: 100%">
            <el-option
              v-for="item in waybills"
              :key="item.id"
              :label="`${item.code}（${labelOf(statusOptions, item.status)}）`"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="createForm.type" style="width: 100%">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别" prop="level">
          <el-select v-model="createForm.level" style="width: 100%">
            <el-option v-for="item in levelOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="createForm.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="handleVisible" title="处理异常" width="520px">
      <el-form :model="handleForm" label-width="90px">
        <el-form-item label="状态">
          <el-select v-model="handleForm.status" style="width: 100%">
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理人"><el-input v-model="handleForm.handler" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="handleForm.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="claimVisible" title="申请理赔" width="520px">
      <el-form :model="claimForm" label-width="90px">
        <el-form-item label="金额"><el-input-number v-model="claimForm.amount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="claimForm.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="claimVisible = false">取消</el-button>
        <el-button type="primary" @click="submitClaim">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="auditVisible" title="审核理赔" width="520px">
      <el-form :model="auditForm" label-width="90px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.approve">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="auditForm.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAudit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { exceptionApi, waybill } from '../../api'

const statusOptions = [
  { value: 'OPEN', label: '未处理' },
  { value: 'PROCESSING', label: '处理中' },
  { value: 'CLOSED', label: '已关闭' }
]

const typeOptions = [
  { value: 'DAMAGE', label: '货损' },
  { value: 'LOST', label: '丢失' },
  { value: 'DELAY', label: '延误' },
  { value: 'REJECT', label: '拒收' },
  { value: 'TIMEOUT_DEPART', label: '发车超时' },
  { value: 'TIMEOUT_ARRIVE', label: '到达超时' },
  { value: 'TIMEOUT_SIGN', label: '签收超时' },
  { value: 'OTHER', label: '其他' }
]

const levelOptions = [
  { value: 'LOW', label: '低' },
  { value: 'MEDIUM', label: '中' },
  { value: 'HIGH', label: '高' }
]

const claimStatusOptions = [
  { value: 'NONE', label: '未申请' },
  { value: 'APPLIED', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'PAID', label: '已支付' }
]

const rows = ref([])
const waybills = ref([])
const loading = ref(false)
const query = reactive({ current: 1, size: 100, status: '', type: '', claimStatus: '' })
const createVisible = ref(false)
const handleVisible = ref(false)
const claimVisible = ref(false)
const auditVisible = ref(false)
const createRef = ref()
const active = ref(null)
const createForm = reactive({ waybill: null, type: '', level: 'MEDIUM', description: '' })
const handleForm = reactive({ status: 'PROCESSING', handler: 'admin', remark: '' })
const claimForm = reactive({ amount: 0, remark: '' })
const auditForm = reactive({ approve: true, remark: '' })

const createRules = {
  type: [{ required: true, message: '请选择异常类型', trigger: 'change' }],
  level: [{ required: true, message: '请选择异常级别', trigger: 'change' }],
  description: [{ required: true, message: '请输入异常描述', trigger: 'blur' }]
}

function labelOf(options, value) {
  return options.find((item) => item.value === value)?.label || value || '-'
}

function typeTag(value) {
  return value && value.indexOf('TIMEOUT_') === 0 ? 'danger' : 'warning'
}

function levelTag(value) {
  return value === 'HIGH' ? 'danger' : value === 'MEDIUM' ? 'warning' : 'info'
}

function statusTag(value) {
  return value === 'CLOSED' ? 'success' : value === 'PROCESSING' ? 'warning' : 'danger'
}

async function load() {
  loading.value = true
  try {
    const page = await exceptionApi.page(query)
    rows.value = page.records || []
  } finally {
    loading.value = false
  }
}

async function loadWaybills() {
  const page = await waybill.page({ current: 1, size: 200 })
  waybills.value = page.records || []
}

async function scan() {
  const count = await exceptionApi.scan()
  ElMessage.success(`本次新增 ${count || 0} 条超时异常`)
  await load()
}

function openCreate() {
  createForm.waybill = null
  createForm.type = ''
  createForm.level = 'MEDIUM'
  createForm.description = ''
  createVisible.value = true
}

async function submitCreate() {
  const valid = await createRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  const selected = createForm.waybill
  await exceptionApi.create({
    waybillId: selected?.id,
    waybillCode: selected?.code,
    carrierCode: selected?.carrierCode,
    type: createForm.type,
    level: createForm.level,
    description: createForm.description
  })
  ElMessage.success('异常登记成功')
  createVisible.value = false
  await load()
}

function openHandle(row) {
  active.value = row
  handleForm.status = row.status === 'OPEN' ? 'PROCESSING' : 'CLOSED'
  handleForm.handler = row.handler || 'admin'
  handleForm.remark = row.handleRemark || ''
  handleVisible.value = true
}

async function submitHandle() {
  await exceptionApi.handle(active.value.id, handleForm)
  ElMessage.success('异常处理成功')
  handleVisible.value = false
  await load()
}

function openClaim(row) {
  active.value = row
  claimForm.amount = Number(row.claimAmount || 0)
  claimForm.remark = row.claimRemark || ''
  claimVisible.value = true
}

async function submitClaim() {
  await exceptionApi.claim(active.value.id, claimForm)
  ElMessage.success('理赔申请已提交')
  claimVisible.value = false
  await load()
}

function openAudit(row) {
  active.value = row
  auditForm.approve = true
  auditForm.remark = ''
  auditVisible.value = true
}

async function submitAudit() {
  await exceptionApi.audit(active.value.id, auditForm)
  ElMessage.success('理赔审核完成')
  auditVisible.value = false
  await load()
}

async function pay(row) {
  try {
    await ElMessageBox.confirm(`确认支付异常 ${row.code} 的理赔金额吗？`, '确认支付', {
      type: 'warning'
    })
  } catch (error) {
    return
  }
  await exceptionApi.pay(row.id)
  ElMessage.success('理赔已支付')
  await load()
}

onMounted(async () => {
  await Promise.all([load(), loadWaybills()])
})
</script>
