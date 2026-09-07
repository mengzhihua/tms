<template>
  <div class="page">
    <el-row :gutter="12">
      <el-col :span="14">
        <div class="card">
          <div class="toolbar">
            <el-button type="primary" @click="openForm()">新增围栏</el-button>
            <el-button @click="load">刷新</el-button>
          </div>
          <el-table :data="fences" border stripe>
            <el-table-column prop="code" label="编码" />
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="type" label="类型" />
            <el-table-column prop="siteCode" label="绑定站点" />
            <el-table-column prop="status" label="状态" />
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openForm(row)">编辑</el-button>
                <el-popconfirm title="确认删除？" @confirm="remove(row)">
                  <template #reference>
                    <el-button link type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="card">
          <h3>围栏地图预览</h3>
          <MiniMap :sites="sites" :fences="fences" />
        </div>
      </el-col>
    </el-row>

    <el-dialog v-model="visible" :title="form.id ? '编辑电子围栏' : '新增电子围栏'" width="620px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="编码">
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio-button label="CIRCLE">圆形</el-radio-button>
            <el-radio-button label="POLYGON">多边形</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="绑定站点">
          <el-select v-model="form.siteCode" clearable filterable style="width: 100%">
            <el-option
              v-for="site in sites"
              :key="site.code"
              :label="`${site.code} ${site.name || ''}`"
              :value="site.code"
            />
          </el-select>
        </el-form-item>
        <template v-if="form.type === 'CIRCLE'">
          <el-form-item label="中心经度"><el-input-number v-model="form.centerLng" :precision="8" /></el-form-item>
          <el-form-item label="中心纬度"><el-input-number v-model="form.centerLat" :precision="8" /></el-form-item>
          <el-form-item label="半径(m)"><el-input-number v-model="form.radiusM" :min="1" /></el-form-item>
        </template>
        <el-form-item v-else label="多边形 JSON">
          <el-input v-model="form.polygon" type="textarea" :rows="5" placeholder="[[121.4,31.2],[121.5,31.2],[121.5,31.3]]" />
        </el-form-item>
        <el-form-item label="进入告警"><el-switch v-model="form.alertOnEnter" /></el-form-item>
        <el-form-item label="离开告警"><el-switch v-model="form.alertOnExit" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { basic } from '../../api'
import MiniMap from '../../components/MiniMap.vue'

const fences = ref([])
const sites = ref([])
const visible = ref(false)
const form = reactive(emptyForm())

function emptyForm() {
  return {
    type: 'CIRCLE',
    status: 'ENABLED',
    alertOnEnter: true,
    alertOnExit: true,
    polygon: '[]'
  }
}

async function load() {
  const [fenceRows, siteRows] = await Promise.all([
    basic.geofence.list({ size: 200 }),
    basic.site.list({ size: 200 })
  ])
  fences.value = fenceRows || []
  sites.value = siteRows || []
}

function openForm(row) {
  Object.assign(form, emptyForm(), row || {})
  visible.value = true
}

async function save() {
  if (form.id) {
    await basic.geofence.update(form.id, form)
  } else {
    await basic.geofence.create(form)
  }
  ElMessage.success('保存成功')
  visible.value = false
  await load()
}

async function remove(row) {
  await basic.geofence.remove(row.id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(load)
</script>
