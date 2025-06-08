const handlePlanRoute = async () => {
  if (!startPoint.value || !endPoint.value) {
    ElMessage.warning('请选择起点和终点')
    return
  }

  try {
    const response = await axios.post('/api/route/plan', {
      start: {
        latitude: startPoint.value.latitude,
        longitude: startPoint.value.longitude
      },
      end: {
        latitude: endPoint.value.latitude,
        longitude: endPoint.value.longitude
      },
      waypoints: waypoints.value.map(wp => ({
        latitude: wp.latitude,
        longitude: wp.longitude
      })),
      transportMode: transportMode.value
    })

    routeInfo.value = response.data
    console.log('Route info:', routeInfo.value)

    // 处理路径点
    routePoints.value = (routeInfo.value && (routeInfo.value as RoutePath).route)
      ? ((routeInfo.value as RoutePath).route as { latitude: number; longitude: number }[]).map((p: {latitude: number, longitude: number}, idx: number) => {
          if (transportMode.value === 'electric') {
            // 使用后端返回的电动车上下车点信息
            const isElectricStart = Math.abs(p.latitude - (routeInfo.value as RoutePath).electricStartPoint.latitude) < 1e-6 &&
                                  Math.abs(p.longitude - (routeInfo.value as RoutePath).electricStartPoint.longitude) < 1e-6;
            const isElectricEnd = Math.abs(p.latitude - (routeInfo.value as RoutePath).electricEndPoint.latitude) < 1e-6 &&
                                Math.abs(p.longitude - (routeInfo.value as RoutePath).electricEndPoint.longitude) < 1e-6;
            
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
    console.error('Error fetching route:', error)
  }
} 