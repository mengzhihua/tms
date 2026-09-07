<template>
  <svg class="mini-map" viewBox="0 0 800 420" role="img" aria-label="运输地图">
    <rect x="0" y="0" width="800" height="420" fill="#eef5fb" />
    <g v-for="fence in projectedFences" :key="fence.code">
      <circle
        v-if="fence.type === 'CIRCLE'"
        :cx="fence.x"
        :cy="fence.y"
        :r="fence.radius"
        fill="rgba(64, 158, 255, 0.1)"
        stroke="#409eff"
        stroke-dasharray="5 4"
      />
      <polygon
        v-else
        :points="fence.points"
        fill="rgba(103, 194, 58, 0.12)"
        stroke="#67c23a"
        stroke-dasharray="5 4"
      />
      <text :x="fence.x + 5" :y="fence.y - 5" fill="#606266">{{ fence.name }}</text>
    </g>
    <g v-for="site in projectedSites" :key="site.code">
      <circle :cx="site.x" :cy="site.y" r="5" fill="#e6a23c" />
      <text :x="site.x + 8" :y="site.y + 4" fill="#303133">{{ site.name }}</text>
    </g>
    <g v-for="vehicle in projectedVehicles" :key="vehicle.plateNo">
      <circle :cx="vehicle.x" :cy="vehicle.y" r="7" fill="#f56c6c" />
      <text :x="vehicle.x + 10" :y="vehicle.y + 4" fill="#303133">{{ vehicle.plateNo }}</text>
    </g>
    <polyline
      v-if="projectedPoints.length"
      :points="projectedPoints.map((point) => `${point.x},${point.y}`).join(' ')"
      fill="none"
      stroke="#409eff"
      stroke-width="2"
    />
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  sites: { type: Array, default: () => [] },
  fences: { type: Array, default: () => [] },
  vehicles: { type: Array, default: () => [] },
  points: { type: Array, default: () => [] }
})

const allPoints = computed(() => [
  ...props.sites.map((item) => [Number(item.lng), Number(item.lat)]),
  ...props.vehicles.map((item) => [Number(item.lng), Number(item.lat)]),
  ...props.points.map((item) => [Number(item.lng), Number(item.lat)])
])

const bounds = computed(() => {
  const points = allPoints.value.filter((point) => point.every(Number.isFinite))
  if (!points.length) {
    return { minLng: 120, maxLng: 122, minLat: 30, maxLat: 32 }
  }
  const lngs = points.map((point) => point[0])
  const lats = points.map((point) => point[1])
  return {
    minLng: Math.min(...lngs) - 0.05,
    maxLng: Math.max(...lngs) + 0.05,
    minLat: Math.min(...lats) - 0.05,
    maxLat: Math.max(...lats) + 0.05
  }
})

function project(lng, lat) {
  const b = bounds.value
  return {
    x: ((Number(lng) - b.minLng) / (b.maxLng - b.minLng)) * 760 + 20,
    y: 400 - ((Number(lat) - b.minLat) / (b.maxLat - b.minLat)) * 380
  }
}

const projectedSites = computed(() =>
  props.sites.map((site) => ({ ...site, ...project(site.lng, site.lat) }))
)

const projectedVehicles = computed(() =>
  props.vehicles.map((vehicle) => ({ ...vehicle, ...project(vehicle.lng, vehicle.lat) }))
)

const projectedPoints = computed(() =>
  props.points.map((point) => project(point.lng, point.lat))
)

const projectedFences = computed(() =>
  props.fences.map((fence) => {
    const center = project(fence.centerLng, fence.centerLat)
    const radius = Math.max(8, (Number(fence.radiusM || 0) / 1000) * 8)
    let polygon = []
    try {
      polygon = JSON.parse(fence.polygon || '[]')
    } catch {
      polygon = []
    }
    return {
      ...fence,
      ...center,
      radius,
      points: polygon.map((point) => {
        const projected = project(point[0], point[1])
        return `${projected.x},${projected.y}`
      }).join(' ')
    }
  })
)
</script>

<style scoped>
.mini-map {
  width: 100%;
  min-height: 300px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>
