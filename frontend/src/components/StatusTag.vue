<template>
  <el-tag :type="meta.type" size="small">{{ meta.text }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: {
    type: String,
    default: ''
  }
})

const dictionary = {
  CREATED: ['新建', 'info'],
  DISPATCHED: ['已调度', 'primary'],
  IN_TRANSIT: ['运输中', 'warning'],
  ARRIVED: ['已到达', 'primary'],
  DELIVERED: ['已送达', 'success'],
  CLOSED: ['已关闭', 'success'],
  CANCELLED: ['已取消', 'danger'],
  EXCEPTION: ['异常', 'danger'],
  UNBILLED: ['未计费', 'warning'],
  BILLED: ['已计费', 'primary'],
  PAID: ['已支付', 'success'],
  IDLE: ['空闲', 'success'],
  MAINTENANCE: ['维修中', 'danger'],
  ENABLED: ['启用', 'success'],
  DISABLED: ['停用', 'info'],
  ENTER: ['进入', 'success'],
  EXIT: ['离开', 'warning']
}

const meta = computed(() => {
  const item = dictionary[props.value]
  return item ? { text: item[0], type: item[1] } : { text: props.value, type: 'info' }
})
</script>
