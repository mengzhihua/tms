<template>
  <div ref="el" class="chart"></div>
</template>

<script setup>
import * as echarts from 'echarts'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({ option: { type: Object, required: true } })
const el = ref()
let chart

function render() {
  if (!chart) chart = echarts.init(el.value)
  chart.setOption(props.option, true)
}

onMounted(() => nextTick(render))
watch(() => props.option, render, { deep: true })
onBeforeUnmount(() => chart?.dispose())
</script>

<style scoped>
.chart { width: 100%; height: 320px; }
</style>
