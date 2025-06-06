<script setup lang="ts">
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, Location, Timer } from '@element-plus/icons-vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import MapFilter from '@/components/MapFilter.vue'
import LocationDetail from '@/components/LocationDetail.vue'
import StartPointSelector from '@/components/StartPointSelector.vue'
import RouteDetail from '@/components/RouteDetail.vue'
import ClearPathPointsButton from '@/components/ClearPathPointsButton.vue'
import RefreshCrowdButton from '@/components/RefreshCrowdButton.vue'
import LocationSearch from '@/components/LocationSearch.vue'
import NearbyServicesDialog from '@/components/NearbyServicesDialog.vue'
import type { Ref } from 'vue'

// 扩展 Leaflet 的类型定义
declare module 'leaflet' {
  interface MarkerOptions {
    isStartPoint?: boolean;
  }
  
  interface PolylineOptions {
    _isRoad?: boolean;
  }
}

// 添加 window.L 的类型声明
declare global {
  interface Window {
    L: any;
  }
}

interface Attraction {
  id: number
  title: string
  description: string
  image: string
  hot: number
  rating: number
  areaType: '校区' | '景区'
}

const route = useRoute()
const attraction = ref<Attraction | null>(null)
const loading = ref(true)
const mapContainer = ref<HTMLElement | null>(null)
const locations = ref<any[]>([])  // 添加 locations 状态
const roads = ref<any[]>([])      // 添加 roads 状态
let map: any = null
let countdownInterval: number | null = null
let countdown = ref(30)

// 添加路径规划相关的状态
const routePolyline = ref<any>(null)
const isPlanningRoute = ref(false)
const routeInfo = ref<RoutePath | null>(null)

// 添加调试信息
const debugInfo = ref({
  routeParams: '',
  mapLoaded: false,
  attractionLoaded: false,
  mapContainerExists: false,
  errors: [] as string[]
})

// 添加浮窗状态
const showFloatingWindow = ref(false)
const selectedLocation = ref<any>(null)

// 添加检查是否有起始点的计算属性
const hasStartPoint = computed(() => {
  return locations.value.some(loc => loc.isStartPoint)
})

// 检查是否有景点ID
const hasAttractionId = computed(() => {
  return route.params.id !== undefined
})

// 添加路径点相关计算属性
const hasPathPoint = computed(() => locations.value.some(loc => loc.isPathPoint))
const pathPointsCount = computed(() => locations.value.filter(loc => loc.isPathPoint).length)
const canPlanRoute = computed(() => hasStartPoint.value && pathPointsCount.value >= 1)

// 在 setup 中添加 routePoints 状态
const routePoints = ref<any[]>([])

// 添加交通方式状态
const transportMode = ref('walking')

// 多路径相关状态
interface RoutePath {
  route: { latitude: number; longitude: number }[];
  totalDistance: number;
  estimatedTime: number;
  electricStartPoint?: { latitude: number; longitude: number };
  electricEndPoint?: { latitude: number; longitude: number };
  [key: string]: any;
}
interface MultiRouteResult {
  distancePath: RoutePath;
  timePath: RoutePath;
  samePath: boolean;
}
const allRoutes = ref<MultiRouteResult | null>(null)
const selectedRouteType = ref<'distance' | 'time'>('distance')
const routeConfirmed = ref(false)
const multiRoutePolylines = ref<Record<string, any>>({})

const selectedRoute = computed<RoutePath | null>(() => {
  if (!allRoutes.value) return null
  if (allRoutes.value.samePath) return allRoutes.value.distancePath
  return selectedRouteType.value === 'distance' ? allRoutes.value.distancePath : allRoutes.value.timePath
})

// 创建自定义图标
const createIcon = (isSelected = false, crowdLevel = 0, isStartPoint = false, isPathPoint = false) => {
  if (isStartPoint) {
    const iconHtml = `
      <div class="custom-marker ${isSelected ? 'marker-shake' : ''}">
        <img src="/images/markers/marker-icon-2x-blue.png"
             style="width: 20px; height: 32px;" />
        <div class="marker-pulse"></div>
      </div>
    `
    return L.divIcon({
      html: iconHtml,
      className: 'custom-div-icon',
      iconSize: [20, 32],
      iconAnchor: [10, 32],
      popupAnchor: [0, -32]
    })
  }
  if (isPathPoint) {
    const iconHtml = `
      <div class="custom-marker ${isSelected ? 'marker-shake' : ''}">
        <img src="/images/markers/marker-icon-2x-gold.png"
             style="width: 20px; height: 32px;" />
        <div class="marker-pulse path-pulse"></div>
      </div>
    `
    return L.divIcon({
      html: iconHtml,
      className: 'custom-div-icon',
      iconSize: [20, 32],
      iconAnchor: [10, 32],
      popupAnchor: [0, -32]
    })
  }

  const getMarkerColor = (level: number): string => {
    switch (level) {
      case 0: return 'green';  // 一般
      case 1: return 'yellow'; // 拥挤
      case 2: return 'red';    // 十分拥挤
      default: return 'green';
    }
  }

  const color = getMarkerColor(crowdLevel)
  const iconHtml = `
    <div class="custom-marker ${isSelected ? 'marker-shake' : ''}">
      <img src="/images/markers/marker-icon-2x-${color}.png"
           style="width: 20px; height: 32px;" />
      ${isStartPoint ? '<div class="marker-pulse"></div>' : ''}
    </div>
  `
  return L.divIcon({
    html: iconHtml,
    className: 'custom-div-icon',
    iconSize: [20, 32],
    iconAnchor: [10, 32],
    popupAnchor: [0, -32]
  })
}

// 初始化地图
const initMap = () => {
  if (!mapContainer.value) {
    const error = '地图容器不存在'
    console.error(error)
    debugInfo.value.errors.push(error)
    return
  }

  try {
    map = L.map(mapContainer.value, {
      minZoom: 15,
      maxZoom: 18,
      maxBounds: [
        [39.98, 116.30],
        [40.02, 116.34]
      ],
      maxBoundsViscosity: 0.5
    }).setView([40.001, 116.324], 16)

    L.tileLayer('/tiles/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
      maxZoom: 18,
      minZoom: 15
    }).addTo(map)

    // 添加缩放事件监听
    map.on('zoomend', () => {
      if (allRoutes.value) {
        renderMultiRoutes()
      }
    })

    // 添加移动事件监听
    map.on('moveend', () => {
      if (allRoutes.value) {
        renderMultiRoutes()
      }
    })

    fetchOsmData()
    debugInfo.value.mapLoaded = true
  } catch (error) {
    const errorMsg = `地图初始化失败: ${error}`
    console.error(errorMsg)
    debugInfo.value.errors.push(errorMsg)
  }
}

// 生成矢量图形的函数
const generateVectorIcon = (name: string): string => {
  // 使用名字的第一个字符生成一个简单的矢量图形
  const firstChar = name.charAt(0).toUpperCase()
  const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEEAD']
  const color = colors[Math.floor(Math.random() * colors.length)]
  
  return `
    <svg viewBox="0 0 100 100" width="100" height="100">
      <circle cx="50" cy="50" r="45" fill="${color}" opacity="0.2"/>
      <text x="50" y="65" font-size="50" text-anchor="middle" fill="${color}">${firstChar}</text>
    </svg>
  `
}

// 生成描述的函数
const generateDescription = (name: string, type: string): string => {
  const descriptions = [
    `${name}是一个${type}，这里环境优美，是游客们喜爱的景点之一。`,
    `来到${name}，您可以感受到独特的${type}氛围，这里值得一游。`,
    `${name}作为${type}，展现了独特的魅力，是必访之地。`,
    `在${name}，您可以体验到${type}的独特魅力，这里风景宜人。`
  ]
  return descriptions[Math.floor(Math.random() * descriptions.length)]
}

// 处理位置点击事件
const handleLocationClick = (location: any) => {
  // 如果描述为空，生成描述
  if (!location.description) {
    location.description = generateDescription(location.name, location.type || '景点')
  }
  
  // 如果矢量图形为空，生成矢量图形
  if (!location.vectorIcon) {
    location.vectorIcon = generateVectorIcon(location.name)
  }
  
  selectedLocation.value = location
  showFloatingWindow.value = true
}

// 在 renderRoads 方法外部添加 getRoadColor 函数
const getRoadColor = (road: any): string => {
  // 使用统一的灰色
  return '#808080';
}

// 渲染道路
const renderRoads = () => {
  if (!map) return

  // 清除现有的道路
  map.eachLayer((layer: L.Layer) => {
    if (layer instanceof L.Polyline && (layer as any)._isRoad) {
      map.removeLayer(layer)
    }
  })

  // 不再渲染普通道路，只保留路径规划时显示的道路
}

// 获取OSM数据
const fetchOsmData = async () => {
  try {
    // 记录当前路径点和选中点
    const prevPathPoints = locations.value.filter(loc => loc.isPathPoint)
    const prevSelectedLocation = selectedLocation.value

    const response = await fetch('http://localhost:8050/api/osm/data')
    if (!response.ok) {
      throw new Error('Failed to fetch OSM data')
    }
    const data = await response.json()

    locations.value = data.locations || []
    roads.value = data.roads || []
      
      // 恢复路径点状态
      locations.value.forEach(loc => {
      if (prevPathPoints.some(prev => Math.abs(prev.latitude - loc.latitude) < 1e-6 && Math.abs(prev.longitude - loc.longitude) < 1e-6)) {
          loc.isPathPoint = true
        }
      })
    // 恢复选中点状态
    if (prevSelectedLocation) {
      const match = locations.value.find(loc => Math.abs(loc.latitude - prevSelectedLocation.latitude) < 1e-6 && Math.abs(loc.longitude - prevSelectedLocation.longitude) < 1e-6)
      if (match) selectedLocation.value = match
      }
      
      updateMarkersVisibility()
    renderRoads()
    } catch (error) {
    console.error('获取OSM数据失败:', error)
    debugInfo.value.errors.push(`获取OSM数据失败: ${error}`)
    }
}

const fetchAttractionData = async () => {
  try {
    const id = route.params.id
    debugInfo.value.routeParams = `ID: ${id}`

    const response = await fetch(`http://localhost:8050/api/attraction/getCards?id=${id}`)
    if (!response.ok) {
      throw new Error('Failed to fetch attraction data')
    }
    const data = await response.json()

    if (data.items && data.items.length > 0) {
      const targetAttraction = data.items.find((item: Attraction) => item.id === Number(id))
      if (targetAttraction) {
        attraction.value = targetAttraction
        debugInfo.value.attractionLoaded = true
      } else {
        debugInfo.value.errors.push('未找到对应的景点信息')
      }
    } else {
      debugInfo.value.errors.push('未找到景点信息')
    }
  } catch (error) {
    debugInfo.value.errors.push(`获取景点数据失败: ${error}`)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchAttractionData()
  
  // 重置所有兴趣点的起始点状态
  try {
    const response = await fetch('http://localhost:8050/api/osm/resetStartPoints', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      }
    })
    
    if (!response.ok) {
      throw new Error('Failed to reset start points')
    }
    
    // 删除所有用户创建的起始点
    const deleteResponse = await fetch('http://localhost:8050/api/osm/deleteAllStartPoints', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      }
    })
    
    if (!deleteResponse.ok) {
      throw new Error('Failed to delete start points')
    }
    
    // 重置本地数据
    locations.value = locations.value.map(loc => ({
      ...loc,
      isStartPoint: false
    }))
  } catch (error) {
    console.error('重置起始点状态失败:', error)
    ElMessage.error('重置起始点状态失败')
  }
  
  setTimeout(() => {
    initMap()
  }, 100)
})

// 组件卸载时清理所有定时器
onUnmounted(() => {
  if (countdownInterval) {
    clearInterval(countdownInterval)
  }
})

// 在 setup 函数中添加
const selectedTypes = ref<string[]>([])
const selectedCrowdLevels = ref<string[]>([])

const handleFilterChange = (types: string[], crowdLevels: string[]) => {
  selectedTypes.value = types
  selectedCrowdLevels.value = crowdLevels
  updateMarkersVisibility()
}

const updateMarkersVisibility = () => {
  if (!map) return

  // 清除所有现有标记，但保留正在选择中的起始点标记
  map.eachLayer((layer: L.Layer) => {
    if (layer instanceof L.Marker) {
      // 检查是否是正在选择中的起始点标记
      const isSelectingStartPoint = layer.options.isStartPoint && 
        (layer as any)._isSelectingStartPoint === true
      
      if (!isSelectingStartPoint) {
        map.removeLayer(layer)
      }
    }
  })

  // 重新添加标记，根据筛选条件设置样式
  locations.value.forEach((location: any) => {
    if (!location.name || location.name === '道路节点') return

    const isTypeSelected = selectedTypes.value.length === 0 || selectedTypes.value.includes(location.type)
    const isCrowdLevelSelected = selectedCrowdLevels.value.length === 0 || 
      selectedCrowdLevels.value.includes(
        location.crowdLevel === 0 ? '一般' : 
        location.crowdLevel === 1 ? '拥挤' : '十分拥挤'
      )

    if (isTypeSelected && isCrowdLevelSelected) {
      const markerOptions: L.MarkerOptions = {
        icon: createIcon(false, location.crowdLevel, location.isStartPoint, location.isPathPoint),
        riseOnHover: true,
        isStartPoint: location.isStartPoint
      }
      
      const marker = L.marker([location.latitude, location.longitude], markerOptions) as L.Marker
      
      // 保存兴趣点数据到标记对象中
      (marker as any)._location = location
      
      // 添加点击事件
      marker.on('click', () => {
        marker.setIcon(createIcon(true, location.crowdLevel, location.isStartPoint, location.isPathPoint))
        handleLocationClick(location)
        setTimeout(() => {
          marker.setIcon(createIcon(false, location.crowdLevel, location.isStartPoint, location.isPathPoint))
        }, 500)
      })
      
      marker.addTo(map)
    }
  })
}

// 在 setup 中添加处理起始点选择的方法
const handleStartPointSelected = async (position: { 
  latitude: number; 
  longitude: number;
  isExistingPoint: boolean;
  markerId?: number;
}) => {
  console.log('起始点已选择:', position)
  
  try {
    // 先移除所有现有的起始点标记
    map.eachLayer((layer: any) => {
      if (layer instanceof L.Marker && layer.options.isStartPoint) {
        map.removeLayer(layer)
      }
    })
    
    if (position.isExistingPoint) {
      // 如果是已存在的兴趣点，更新其 isStartPoint 属性
      const updateData = {
        latitude: position.latitude,
        longitude: position.longitude,
        isStartPoint: true
      }
      console.log('更新起始点数据:', updateData)
      
      const response = await fetch('http://localhost:8050/api/osm/updateStartPoint', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(updateData)
      })
      
      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(`Failed to update start point: ${errorText}`)
      }
      
      // 更新本地数据
      const location = locations.value.find(loc => 
        loc.latitude === position.latitude && 
        loc.longitude === position.longitude
      )
      
      if (location) {
        location.isStartPoint = true
        // 更新标记
        updateMarkersVisibility()
      }
    } else {
      // 如果是新选择的起始点，创建新的兴趣点
      const newLocation = {
        name: '起始点',
        type: 'start_point',
        latitude: position.latitude,
        longitude: position.longitude,
        description: '用户选择的起始点',
        category: '服务设施',
        crowdLevel: 0,
        isStartPoint: true
      }
      
      console.log('创建新起始点数据:', newLocation)

      const response = await fetch('http://localhost:8050/api/osm/createStartPoint', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(newLocation)
      })
      
      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(`Failed to create start point: ${errorText}`)
      }
      
      // 重新获取数据
      await fetchOsmData()
    }
  } catch (error) {
    console.error('更新起始点失败:', error)
    ElMessage.error('更新起始点失败: ' + (error as Error).message)
  }
}

const handleStartPointRemoved = async () => {
  console.log('起始点已删除')
  
  try {
    // 查找当前作为起始点的位置
    const startPoint = locations.value.find(loc => loc.isStartPoint)
    
    if (startPoint) {
      // 更新数据库中的起始点状态
      const response = await fetch('http://localhost:8050/api/osm/updateStartPoint', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          latitude: startPoint.latitude,
          longitude: startPoint.longitude,
          isStartPoint: false,
          type: startPoint.type // 添加类型信息
        })
      })
      
      if (!response.ok) {
        throw new Error('Failed to update start point')
      }
      
      // 如果是专门创建的起始点，从本地数据中移除
      if (startPoint.type === 'start_point') {
        locations.value = locations.value.filter(loc => 
          loc.latitude !== startPoint.latitude || 
          loc.longitude !== startPoint.longitude
        )
        // 重置选中的位置
        selectedLocation.value = null
      } else {
        // 否则只更新 isStartPoint 属性
        startPoint.isStartPoint = false
      }
      
      // 更新标记
      updateMarkersVisibility()
      // 在起始点删除后自动清除路径
      clearRoute()
    }
  } catch (error) {
    console.error('删除起始点失败:', error)
    ElMessage.error('删除起始点失败')
  }
}

// 处理位置悬停事件
const handleLocationHover = (location: any) => {
  // 如果描述为空，生成描述
  if (!location.description) {
    location.description = generateDescription(location.name, location.type || '景点')
  }
  
  // 如果矢量图形为空，生成矢量图形
  if (!location.vectorIcon) {
    location.vectorIcon = generateVectorIcon(location.name)
  }
  
  selectedLocation.value = location
  showFloatingWindow.value = true
}

// 处理 goToLocation 和 deleteLocation 事件
const handleGoToLocation = (location: any) => {
  // 不能同时为起始点和路径点
  if (location.isStartPoint) return
  // 先清除其他同坐标的路径点
  locations.value.forEach(loc => {
    if (loc.latitude === location.latitude && loc.longitude === location.longitude) {
      loc.isPathPoint = true
      loc.isStartPoint = false
    } else if (!loc.isStartPoint) {
      // 只允许一个路径点
      // loc.isPathPoint = false
    }
  })
  updateMarkersVisibility()
}

const handleDeleteLocation = (location: any) => {
  if (location.isStartPoint) {
    location.isStartPoint = false
    handleStartPointRemoved()
    clearRoute() // 删除起始点时自动清除路径
  } else if (location.isPathPoint) {
    location.isPathPoint = false
    updateMarkersVisibility()
    clearRoute() // 删除路径点时自动清除路径
  }
}

// 添加处理交通方式切换的方法
const handleTransportModeChange = async (mode: string) => {
  transportMode.value = mode
  // 先清除当前路径
  clearRoute()
  // 如果有起始点和路径点，则自动开始规划
  if (hasStartPoint.value && hasPathPoint.value) {
    await handlePlanRoute()
  }
}

// 修改 handlePlanRoute 方法
const handlePlanRoute = async () => {
  if (!hasStartPoint.value || !hasPathPoint.value) {
    ElMessage.warning('请先选择起始点和路径点')
    return
  }
  try {
    isPlanningRoute.value = true
    const startPoint = locations.value.find(loc => loc.isStartPoint)
    const pathPoints = locations.value.filter(loc => loc.isPathPoint)
    const requestData = {
      startPoint: {
        latitude: startPoint.latitude,
        longitude: startPoint.longitude
      },
      pathPoints: pathPoints.map(point => ({
        latitude: point.latitude,
        longitude: point.longitude
      })),
      allowReturn: pathPoints.length >= 2,
      transportMode: transportMode.value
    }
    console.log('[PlanView] Planning route with mode:', transportMode.value);
    const response = await fetch('http://localhost:8050/api/route/planMulti', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestData)
    })
    const data = await response.json()
    console.log('[PlanView] Route planning response:', data);

    // 检查时间最短路径的时间是否比距离最短路径的时间长
    if (data.timePath.estimatedTime > data.distancePath.estimatedTime) {
      // 如果时间更长，则使用距离最短路径作为两种规划的结果
      data.timePath = { ...data.distancePath }
      data.samePath = true
    }

    allRoutes.value = data
    selectedRouteType.value = 'distance'
    routeConfirmed.value = false
    renderMultiRoutes()
    // 立即显示RouteDetail，routeConfirmed为false时也显示
    routeInfo.value = selectedRoute.value as RoutePath
    routePoints.value = (routeInfo.value && (routeInfo.value as RoutePath).route)
      ? ((routeInfo.value as RoutePath).route as { latitude: number; longitude: number }[]).map((p: {latitude: number, longitude: number}, idx: number) => {
          if (transportMode.value === 'electric' && routeInfo.value) {
            const electricStartPoint = (routeInfo.value as RoutePath).electricStartPoint;
            const electricEndPoint = (routeInfo.value as RoutePath).electricEndPoint;
            
            // 检查电动车上下车点是否存在
            const isElectricStart = electricStartPoint && 
              Math.abs(p.latitude - electricStartPoint.latitude) < 1e-6 &&
              Math.abs(p.longitude - electricStartPoint.longitude) < 1e-6;
            
            const isElectricEnd = electricEndPoint && 
              Math.abs(p.latitude - electricEndPoint.latitude) < 1e-6 &&
              Math.abs(p.longitude - electricEndPoint.longitude) < 1e-6;
            
            console.log('[PlanView] Checking electric points:', {
              point: p,
              electricStartPoint,
              electricEndPoint,
              isElectricStart,
              isElectricEnd
            });
            
            return {
              name: isElectricStart ? '电动车上车点' : (isElectricEnd ? '电动车下车点' : '途经点'),
              type: isElectricStart ? '电动车上车点' : (isElectricEnd ? '电动车下车点' : '途经点'),
              latitude: p.latitude,
              longitude: p.longitude,
              isElectricStart,
              isElectricEnd
            }
          }
          return {
            name: idx === 0 ? '起点' : (idx === ((routeInfo.value as RoutePath).route.length - 1) ? '终点' : '途经点'),
            type: idx === 0 ? '起点' : (idx === ((routeInfo.value as RoutePath).route.length - 1) ? '终点' : '途经点'),
            latitude: p.latitude,
            longitude: p.longitude
          }
        }) : []
  } catch (error) {
    console.error('[PlanView] Route planning failed:', error)
    const errMsg = (error && typeof error === 'object' && 'message' in error) ? (error as any).message : String(error)
    ElMessage.error('路径规划失败: ' + errMsg)
  } finally {
    isPlanningRoute.value = false
  }
}

const confirmRoute = () => {
  routeConfirmed.value = true
  // 只保留选中路径的高亮，移除另一条
  if (!allRoutes.value) return
  if (allRoutes.value.samePath) {
    // yellow 路径不变
  } else {
    if (selectedRouteType.value === 'distance') {
      if (multiRoutePolylines.value.time) map.removeLayer(multiRoutePolylines.value.time)
      multiRoutePolylines.value.distance.setStyle({ opacity: 1 })
    } else {
      if (multiRoutePolylines.value.distance) map.removeLayer(multiRoutePolylines.value.distance)
      multiRoutePolylines.value.time.setStyle({ opacity: 1 })
    }
  }
  // 右侧 RouteDetail 显示
  routeInfo.value = selectedRoute.value as RoutePath
  // 更新 routePoints
  routePoints.value = (routeInfo.value && (routeInfo.value as RoutePath).route)
    ? ((routeInfo.value as RoutePath).route as { latitude: number; longitude: number }[]).map((p: {latitude: number, longitude: number}, idx: number) => ({
      name: idx === 0 ? '起点' : (idx === ((routeInfo.value as RoutePath).route.length - 1) ? '终点' : '途经点'),
      type: idx === 0 ? '起点' : (idx === ((routeInfo.value as RoutePath).route.length - 1) ? '终点' : '途经点'),
      latitude: p.latitude,
      longitude: p.longitude
    })) : []
}

const clearRoute = () => {
  // 清除所有路径段
  if (multiRoutePolylines.value.segments) {
    multiRoutePolylines.value.segments.forEach((segment: L.Polyline) => {
      if (segment && map) {
        map.removeLayer(segment)
      }
    })
  }
  // 清除其他路径
  Object.values(multiRoutePolylines.value).forEach(poly => {
    if (poly && map) {
      map.removeLayer(poly)
    }
  })
  multiRoutePolylines.value = {}
  allRoutes.value = null
  routeConfirmed.value = false
  routeInfo.value = null
  routePoints.value = []
}

// 在 setup 中添加清除所有路径点的方法
const handleClearAllPathPoints = () => {
  // 清除所有路径点
  locations.value.forEach(loc => {
    if (loc.isPathPoint) {
      loc.isPathPoint = false
    }
  })
  // 更新标记显示
  updateMarkersVisibility()
  // 清除路径
  clearRoute()
}

// 添加处理刷新拥挤度的方法
const handleCrowdLevelRefresh = async () => {
  try {
    // 重新获取 OSM 数据，包括道路和位置点
    await fetchOsmData()
    // 更新标记显示
    updateMarkersVisibility()
    // 重新渲染道路
    renderRoads()
  } catch (error) {
    console.error('刷新拥挤度失败:', error)
    ElMessage.error('刷新拥挤度失败')
  }
}

// 在 setup 中添加处理搜索选择的方法
const handleSearchSelect = (location: any) => {
  if (!map) return
  
  // 设置地图视图到最大缩放级别
  map.setZoom(18)
  
  // 将地图中心移动到选中的位置
  map.setView([location.latitude, location.longitude])
  
  // 高亮显示选中的标记
  map.eachLayer((layer: L.Layer) => {
    if (layer instanceof L.Marker) {
      const marker = layer as L.Marker
      const markerLocation = (marker as any)._location
      if (markerLocation && 
          markerLocation.latitude === location.latitude && 
          markerLocation.longitude === location.longitude) {
        marker.setIcon(createIcon(true, location.crowdLevel, location.isStartPoint, location.isPathPoint))
        setTimeout(() => {
          marker.setIcon(createIcon(false, location.crowdLevel, location.isStartPoint, location.isPathPoint))
        }, 500)
      }
    }
  })
}

// 在 setup 中添加处理 RouteDetail 节点点击的方法
const handleRouteNodeClick = (point: any) => {
  if (!map) return
  map.setView([point.latitude, point.longitude], 18)
}

const renderMultiRoutes = () => {
  // 清除所有现有路径段
  if (multiRoutePolylines.value.segments) {
    multiRoutePolylines.value.segments.forEach((segment: L.Polyline) => {
      if (segment && map) {
        map.removeLayer(segment)
      }
    })
  }
  multiRoutePolylines.value = {}
  if (!allRoutes.value || !map) return

  // 根据当前缩放级别计算路径宽度
  const currentZoom = map.getZoom()
  const baseWeight = 8 // 基础宽度
  const minWeight = 4  // 最小宽度
  const maxWeight = 12 // 最大宽度
  
  // 根据缩放级别动态计算路径宽度
  const getPathWeight = (zoom: number) => {
    if (zoom <= 15) return minWeight
    if (zoom >= 18) return maxWeight
    return baseWeight + (zoom - 15) * 2
  }

  const pathWeight = getPathWeight(currentZoom)
  const borderWeight = pathWeight + 2 // 边框宽度比路径宽度大2

  const drawRouteWithCrowdLevel = (route: RoutePath, color: string) => {
    const points = route.route.map((p: {latitude: number, longitude: number}) => [p.latitude, p.longitude] as [number, number])
    
    // 创建路径段
    for (let i = 0; i < points.length - 1; i++) {
      const start = points[i]
      const end = points[i + 1]
      
      // 找到这段路径对应的道路
      const road = roads.value.find(r => {
        const roadPoints = r.pathPoints.split(';')
          .filter((point: string) => point.trim() !== '')
          .map((point: string) => {
            const [lat, lon] = point.split(',').map(Number)
            return { latitude: lat, longitude: lon }
          })
        
        // 检查路径段是否与道路重叠
        for (let j = 0; j < roadPoints.length - 1; j++) {
          const roadStart = roadPoints[j]
          const roadEnd = roadPoints[j + 1]
          
          const startDist = Math.sqrt(
            Math.pow(start[0] - roadStart.latitude, 2) +
            Math.pow(start[1] - roadStart.longitude, 2)
          )
          const endDist = Math.sqrt(
            Math.pow(end[0] - roadEnd.latitude, 2) +
            Math.pow(end[1] - roadEnd.longitude, 2)
          )
          
          if (startDist < 0.0001 && endDist < 0.0001) {
            return true
          }
        }
        return false
      })
      
      // 使用道路的拥挤度或默认值
      const crowdLevel = road ? road.crowdLevel : 0.5
      const segmentColor = getCrowdLevelColor(crowdLevel)
      
      // 创建黑色边框，使用动态宽度
      const border = L.polyline([start, end], {
        color: '#000000',
        weight: borderWeight,
        opacity: 0.8,
        lineCap: 'round',
        lineJoin: 'round'
      }).addTo(map)
      
      // 创建主路径，使用动态宽度
      const segment = L.polyline([start, end], {
        color: segmentColor,
        weight: pathWeight,
        opacity: 0.8,
        lineCap: 'round',
        lineJoin: 'round'
      }).addTo(map)
      
      // 保存路径段引用
      if (!multiRoutePolylines.value.segments) {
        multiRoutePolylines.value.segments = []
      }
      multiRoutePolylines.value.segments.push(border)
      multiRoutePolylines.value.segments.push(segment)
    }
  }

  // 只渲染当前选中的路径
  if (allRoutes.value.samePath) {
    drawRouteWithCrowdLevel(allRoutes.value.distancePath, '#FFD600')
  } else {
    if (selectedRouteType.value === 'distance') {
      drawRouteWithCrowdLevel(allRoutes.value.distancePath, '#ff0000')
    } else {
      drawRouteWithCrowdLevel(allRoutes.value.timePath, '#00C853')
    }
  }
}

// 只显示用户选择的起点和路径点
const userRoutePoints = computed(() => {
  const points: any[] = []
  const start = locations.value.find(loc => loc.isStartPoint)
  if (start) {
    points.push({
      name: start.name || '起点',
      type: '起点',
      latitude: start.latitude,
      longitude: start.longitude,
      description: start.description || '路径起点'
    })
  }
  locations.value.filter(loc => loc.isPathPoint).forEach((loc, idx) => {
    points.push({
      name: loc.name || `路径点${idx + 1}`,
      type: '路径点',
      latitude: loc.latitude,
      longitude: loc.longitude,
      description: loc.description || '路径途经点'
    })
  })
  return points
})

// 获取路径段的拥挤度颜色
const getCrowdLevelColor = (crowdLevel: number): string => {
  // 使用渐变色表示拥挤度
  // 0-0.3: 绿色 (通畅)
  // 0.3-0.7: 黄色 (一般)
  // 0.7-1.0: 红色 (拥挤)
  if (crowdLevel <= 0.4) return '#4CAF50';  // 绿色
  if (crowdLevel <= 0.8) return '#FFC107';  // 黄色
  return '#F44336';  // 红色
}

// 在 setup 中添加
const showNearbyServicesDialog = ref(false)
const nearbyServices = ref<any[]>([])

// 处理显示周边服务设施
const handleShowNearbyServices = async (location: any) => {
  if (!location) {
    ElMessage.warning('未选择位置')
    return
  }
  
  try {
    console.log('Fetching nearby services for location:', location)
    
    const response = await fetch('http://localhost:8050/api/osm/nearbyServices', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        latitude: location.latitude,
        longitude: location.longitude,
        radius: 250 // 250米范围内
      })
    })
    
    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`Failed to fetch nearby services: ${errorText}`)
    }
    
    const data = await response.json()
    console.log('Received nearby services:', data)
    
    if (!data.services) {
      throw new Error('Invalid response format: missing services array')
    }
    
    nearbyServices.value = data.services
    showNearbyServicesDialog.value = true
  } catch (error) {
    console.error('获取周边服务设施失败:', error)
    ElMessage.error('获取周边服务设施失败: ' + (error as Error).message)
  }
}

// 处理服务选择
const handleServiceSelect = (service: any) => {
  if (!map) return
  
  // 设置地图视图到最大缩放级别
  map.setZoom(18)
  
  // 将地图中心移动到选中的位置
  map.setView([service.latitude, service.longitude])
  
  // 高亮显示选中的标记
  map.eachLayer((layer: L.Layer) => {
    if (layer instanceof L.Marker) {
      const marker = layer as L.Marker
      const markerLocation = (marker as any)._location
      if (markerLocation && 
          markerLocation.latitude === service.latitude && 
          markerLocation.longitude === service.longitude) {
        marker.setIcon(createIcon(true, markerLocation.crowdLevel, markerLocation.isStartPoint, markerLocation.isPathPoint))
        setTimeout(() => {
          marker.setIcon(createIcon(false, markerLocation.crowdLevel, markerLocation.isStartPoint, markerLocation.isPathPoint))
        }, 500)
      }
    }
  })
  
  // 关闭弹窗
  showNearbyServicesDialog.value = false
}
</script>

<template>
  <div class="plan-container">
    <!-- 添加搜索组件 -->
    <div v-if="hasAttractionId" class="search-container">
      <LocationSearch 
        :locations="locations"
        @select="handleSearchSelect"
      />
    </div>

    <!-- 删除调试信息面板 -->
    <!-- <div class="debug-panel">
      <h3>调试信息</h3>
      <pre>{{ JSON.stringify(debugInfo, null, 2) }}</pre>
    </div> -->

    <!-- 欢迎信息 -->
    <div v-if="!hasAttractionId" class="welcome-message">
      <el-empty description="请先在首页选择要去的景点">
        <template #description>
          <p class="welcome-text">选择一个景点开始规划您的行程</p>
          <el-button type="primary" @click="$router.push('/')">
            去选择景点
          </el-button>
        </template>
      </el-empty>
    </div>

    <!-- 地图容器 -->
    <div v-else class="map-wrapper">
      <div ref="mapContainer" class="map-container"></div>
      <ClearPathPointsButton @clear="handleClearAllPathPoints" />
      <RefreshCrowdButton @refresh="handleCrowdLevelRefresh" />
      <MapFilter 
        v-if="locations && locations.length > 0"
        :locations="locations" 
        @filterChange="handleFilterChange"
      />
      
      <!-- 路径信息显示 -->
      <div v-if="allRoutes" class="route-info-panel">
        <div class="route-info-row">
          <div class="route-info-choices">
            <div v-if="allRoutes.samePath">
              <div class="route-info-item yellow" @click="selectedRouteType = 'distance'">
                <b>距离/用时最短路径</b>
                <span class="icon-info"><el-icon><Location /></el-icon> {{ (allRoutes.distancePath.totalDistance/1000).toFixed(2) }}km</span>
                <span class="icon-info"><el-icon><Timer /></el-icon> {{ allRoutes.distancePath.estimatedTime.toFixed(2) }}min</span>
              </div>
            </div>
            <div v-else>
              <div
                class="route-info-item red"
                :class="{active: selectedRouteType === 'distance'}"
                @click="!routeConfirmed && (selectedRouteType = 'distance'); renderMultiRoutes()"
              >
                <b>距离最短路径</b>
                <span class="icon-info"><el-icon><Location /></el-icon> {{ (allRoutes.distancePath.totalDistance/1000).toFixed(2) }}km</span>
                <span class="icon-info"><el-icon><Timer /></el-icon> {{ allRoutes.distancePath.estimatedTime.toFixed(2) }}min</span>
              </div>
              <div
                class="route-info-item green"
                :class="{active: selectedRouteType === 'time'}"
                @click="!routeConfirmed && (selectedRouteType = 'time'); renderMultiRoutes()"
              >
                <b>用时最短路径</b>
                <span class="icon-info"><el-icon><Location /></el-icon> {{ (allRoutes.timePath.totalDistance/1000).toFixed(2) }}km</span>
                <span class="icon-info"><el-icon><Timer /></el-icon> {{ allRoutes.timePath.estimatedTime.toFixed(2) }}min</span>
              </div>
            </div>
          </div>
          <div class="route-info-btn-wrapper">
            <el-button
              :type="routeConfirmed ? 'danger' : 'primary'"
              @click="routeConfirmed ? clearRoute() : confirmRoute()"
            >
              {{ routeConfirmed ? '清除路径' : '确认路径' }}
            </el-button>
          </div>
        </div>
      </div>

      <RouteDetail
        v-if="routeInfo"
        :visible="true"
        :route-points="userRoutePoints"
        :distance="(routeInfo as RoutePath)?.totalDistance"
        :time="(routeInfo as RoutePath)?.estimatedTime"
        :transport-mode="transportMode"
        :area-type="attraction?.areaType"
        @nodeClick="handleRouteNodeClick"
        @transportModeChange="handleTransportModeChange"
      />
      <LocationDetail
        v-else
        :location="selectedLocation"
        :visible="true"
        :hasStartPoint="hasStartPoint"
        :isPathPoint="selectedLocation && selectedLocation.isPathPoint"
        :isStartPoint="selectedLocation && selectedLocation.isStartPoint"
        @goToLocation="handleGoToLocation"
        @deleteLocation="handleDeleteLocation"
        @startPointRemoved="handleStartPointRemoved"
        @showNearbyServices="handleShowNearbyServices"
        class="location-detail"
      />

      <StartPointSelector
        v-if="map"
        :map="map"
        :hasStartPoint="hasStartPoint"
        @startPointSelected="handleStartPointSelected"
        @startPointRemoved="handleStartPointRemoved"
        @hoverLocation="handleLocationHover"
      />

      <!-- 页面底部中间显示"开始规划"按钮 -->
      <div v-if="canPlanRoute" class="plan-route-btn-wrapper">
        <el-button 
          type="primary" 
          class="plan-route-btn" 
          @click="handlePlanRoute"
          :loading="isPlanningRoute"
        >
          {{ isPlanningRoute ? '规划中...' : '开始规划' }}
        </el-button>
      </div>

      <NearbyServicesDialog
        v-model:visible="showNearbyServicesDialog"
        :center-location="selectedLocation?.coordinates || { lat: 0, lng: 0 }"
        :services="nearbyServices"
        @select-service="handleServiceSelect"
      />
    </div>
  </div>
</template>

<style scoped>
.plan-container {
  width: calc(100% - 70px);
  height: 100vh;
  position: relative;
  overflow: hidden;
  margin-left: 70px;
}

/* 删除调试面板样式 */
/* .debug-panel {
  position: fixed;
  top: 80px;
  right: 20px;
  background: rgba(0, 0, 0, 0.8);
  color: #fff;
  padding: 15px;
  border-radius: 8px;
  max-width: 400px;
  max-height: 80vh;
  overflow: auto;
  z-index: 1000;
  font-family: monospace;
  font-size: 12px;
} */

/* 地图容器样式 */
.map-wrapper {
  width: 100%;
  height: 100vh;
  position: relative;
}

.map-container {
  width: 100%;
  height: 100%;
  position: relative;
  background-color: #f5f5f5;
}

/* 空盒矢量图形样式 */
.empty-box {
  position: absolute;
  top: 30%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 300px;
  height: 300px;
  opacity: 0.1;
  pointer-events: none;
}

.empty-box-svg {
  width: 100%;
  height: 100%;
  animation: rotate 20s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.welcome-message {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  padding: 20px;
  margin-top: 60px;
  overflow: hidden;
}

.welcome-icon {
  color: #696969;
  margin-bottom: 20px;
}

.welcome-text {
  font-size: 18px;
  color: #606266;
  margin: 16px 0;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .plan-container {
    width: 100%;
    margin-left: 0;
  }

  .map-wrapper {
    height: calc(100vh - 50px);
    margin-top: 50px;
  }

  .welcome-message {
    margin-top: 50px;
  }

  .welcome-message {
    padding: 10px;
  }
  
  .welcome-text {
    font-size: 16px;
  }
}

/* 调整弹出窗口样式 */
:deep(.location-popup) {
  padding: 8px;
  min-width: 200px;
}

:deep(.location-popup h3) {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 14px;
  font-weight: bold;
}

:deep(.location-popup p) {
  margin: 4px 0;
  color: #666;
  font-size: 12px;
  line-height: 1.4;
}

:deep(.location-type) {
  color: #888;
  font-style: italic;
  font-size: 11px;
}

:deep(.road-popup) {
  padding: 5px;
}

:deep(.road-popup h3) {
  margin: 0 0 5px 0;
  color: #333;
  font-size: 14px;
}

:deep(.road-type) {
  color: #888;
  font-style: italic;
  font-size: 12px;
}

/* 自定义标记点样式 */
:deep(.custom-div-icon) {
  background: none;
  border: none;
}

:deep(.custom-marker) {
  position: relative;
  transform-origin: bottom center;
}

:deep(.marker-shake) {
  animation: shake 0.5s ease;
}

:deep(.marker-pulse) {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  background: rgba(0, 123, 255, 0.3);
  border-radius: 50%;
  z-index: -1;
  animation: pulse 2s ease-out infinite;
}

:deep(.path-pulse) {
  background: rgba(255, 215, 0, 0.3);  /* 金色，透明度0.3 */
}

@keyframes pulse {
  0% {
    transform: translate(-50%, -50%) scale(0.5);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) scale(2);
    opacity: 0;
  }
}

@keyframes shake {
  0%, 100% {
    transform: rotate(0deg);
  }
  25% {
    transform: rotate(-8deg);
  }
  75% {
    transform: rotate(8deg);
  }
}

:deep(.crowd-level) {
  margin-top: 8px;
  padding: 4px 8px;
  background-color: #f5f5f5;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
}

/* 确保筛选器在地图上层 */
:deep(.map-filter) {
  z-index: 1000;
}

/* 更新筛选器浮窗样式 */
:deep(.map-filter) {
  background: rgba(0, 0, 0, 0.7);
  color: white;
}

:deep(.map-filter__title) {
  color: white;
}

:deep(.filter-item__label) {
  color: white;
}

:deep(.filter-item__count) {
  color: #ccc;
}

/* 更新兴趣点详细信息浮窗样式 */
:deep(.floating-window) {
  background: rgba(0, 0, 0, 0.7);
  color: white;
}

:deep(.location-title) {
  color: white;
}

:deep(.location-description) {
  color: white;
}

:deep(.vector-icon) {
  color: white;
}

:deep(.go-button) {
  background-color: #1890ff;
  color: white;
}

.plan-route-btn-wrapper {
  position: fixed;
  left: 50%;
  bottom: 40px;
  transform: translateX(-50%);
  z-index: 2000;
  animation: slideUp 0.5s ease-out;
}

.plan-route-btn {
  font-size: 20px;
  padding: 12px 48px;
  background: linear-gradient(90deg, rgba(24, 144, 255, 0.8) 0%, rgba(9, 109, 217, 0.8) 100%);
  color: #fff;
  border: none;
  border-radius: 30px;
  box-shadow: 0 2px 12px 0 rgba(24, 144, 255, 0.2);
  transition: all 0.3s ease;
  backdrop-filter: blur(4px);
}

.plan-route-btn:hover {
  background: linear-gradient(90deg, rgba(64, 169, 255, 0.9) 0%, rgba(24, 144, 255, 0.9) 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px 0 rgba(24, 144, 255, 0.3);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translate(-50%, 20px);
  }
  to {
    opacity: 1;
    transform: translate(-50%, 0);
  }
}

/* 路径信息面板样式 */
.route-info-panel {
  position: fixed;
  bottom: 100px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 15px 32px;
  border-radius: 8px;
  z-index: 1000;
  backdrop-filter: blur(4px);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  min-width: 520px;
  max-width: 700px;
  display: flex;
  flex-direction: column;
}

.route-info-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.route-info-choices {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.route-info-item {
  display: flex;
  align-items: center;
  gap: 18px;
  font-size: 16px;
  margin-bottom: 10px;
  padding: 10px 0;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.route-info-item.active {
  background: rgba(255,255,255,0.08);
}

.route-info-item.yellow {
  color: #FFD600;
}

.route-info-item.red {
  color: #ff0000;
}

.route-info-item.green {
  color: #00C853;
}

.icon-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 15px;
  color: #fff;
  background: rgba(255,255,255,0.06);
  border-radius: 4px;
  padding: 2px 8px;
}

.icon-info .el-icon {
  font-size: 16px;
  color: #FFD600;
  margin-right: 2px;
}

.route-info-item.red .icon-info .el-icon {
  color: #ff0000;
}

.route-info-item.green .icon-info .el-icon {
  color: #00C853;
}

.route-info-item.yellow .icon-info .el-icon {
  color: #FFD600;
}

/* 添加搜索容器样式 */
.search-container {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 400px;
  z-index: 1000;
  padding: 0 20px;
}

.route-info-btn-wrapper {
  display: flex;
  align-items: center;
  margin-left: 24px;
  margin-top: 0;
  height: 100%;
}

.route-info-btn-wrapper .el-button {
  margin: 0;
  height: 40px;
  width: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}
</style> 