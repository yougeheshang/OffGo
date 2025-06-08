// 电动车模式：规划路径
async function planElectricRoute(start, end, waypoints) {
    console.log('[Electric] Starting route planning...');
    console.log('[Electric] Start point:', start);
    console.log('[Electric] End point:', end);
    console.log('[Electric] Waypoints:', waypoints);

    try {
        const response = await fetch('/api/route/plan', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                startPoint: start,
                endPoint: end,
                pathPoints: waypoints,
                transportMode: 'electric'
            })
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        console.log('[Electric] Route planning response:', data);

        // 检查是否有电动车上下车点
        if (data.electricStartPoint && data.electricEndPoint) {
            console.log('[Electric] Found electric vehicle points:');
            console.log('[Electric] Start point:', data.electricStartPoint);
            console.log('[Electric] End point:', data.electricEndPoint);
        } else {
            console.warn('[Electric] No electric vehicle points found, using walking mode');
        }

        // 检查路径中的道路
        if (data.roads && data.roads.length > 0) {
            console.log('[Electric] Roads in path:', data.roads.length);
            data.roads.forEach(road => {
                console.log(`[Electric] Road ${road.id}: type=${road.roadType}, crowdLevel=${road.crowdLevel}`);
            });
        } else {
            console.warn('[Electric] No roads found in path');
        }

        // 输出总距离和时间
        console.log('[Electric] Total distance:', data.totalDistance, 'meters');
        console.log('[Electric] Estimated time:', data.estimatedTime, 'minutes');
        console.log('[Electric] Start to road distance:', data.startToRoadDistance, 'meters');
        console.log('[Electric] End to road distance:', data.endToRoadDistance, 'meters');

        return data;
    } catch (error) {
        console.error('[Electric] Error planning route:', error);
        throw error;
    }
}

// 电动车模式：绘制路径
function drawElectricRoute(route, map) {
    console.log('[Electric] Drawing route on map...');
    
    if (!route || !route.route || route.route.length < 2) {
        console.error('[Electric] Invalid route data');
        return;
    }

    // 创建路径坐标数组
    const coordinates = route.route.map(point => [point.latitude, point.longitude]);
    console.log('[Electric] Route coordinates:', coordinates.length, 'points');
    
    // 创建路径线
    const path = L.polyline(coordinates, {
        color: '#4CAF50',  // 使用绿色
        weight: 5,
        opacity: 0.7
    }).addTo(map);
    
    // 如果有电动车上下车点，添加标记
    if (route.electricStartPoint && route.electricEndPoint) {
        console.log('[Electric] Adding electric vehicle points markers');
        
        // 添加上车点标记
        L.marker([route.electricStartPoint.latitude, route.electricStartPoint.longitude], {
            icon: L.divIcon({
                className: 'electric-start-marker',
                html: '<div class="marker electric-start"></div>'
            })
        }).addTo(map).bindPopup('电动车起点');
        
        // 添加下车点标记
        L.marker([route.electricEndPoint.latitude, route.electricEndPoint.longitude], {
            icon: L.divIcon({
                className: 'electric-end-marker',
                html: '<div class="marker electric-end"></div>'
            })
        }).addTo(map).bindPopup('电动车终点');
    }

    // 调整地图视图以显示整个路径
    map.fitBounds(path.getBounds());
    console.log('[Electric] Route drawing completed');
}

// 电动车模式：显示路径详情
function showElectricRouteDetails(route) {
    console.log('[Electric] Showing route details...');
    
    const detailsContainer = document.getElementById('routeDetails');
    if (!detailsContainer) {
        console.error('[Electric] Route details container not found');
        return;
    }

    // 构建详情HTML
    let detailsHtml = `
        <div class="route-details">
            <h3>电动车路线详情</h3>
            <div class="detail-item">
                <span class="label">总距离：</span>
                <span class="value">${route.totalDistance.toFixed(2)} 米</span>
            </div>
            <div class="detail-item">
                <span class="label">预计用时：</span>
                <span class="value">${route.estimatedTime.toFixed(2)} 分钟</span>
            </div>
    `;

    // 添加电动车上下车点信息
    if (route.electricStartPoint && route.electricEndPoint) {
        detailsHtml += `
            <div class="detail-item">
                <span class="label">电动车起点：</span>
                <span class="value">(${route.electricStartPoint.latitude.toFixed(6)}, ${route.electricStartPoint.longitude.toFixed(6)})</span>
            </div>
            <div class="detail-item">
                <span class="label">电动车终点：</span>
                <span class="value">(${route.electricEndPoint.latitude.toFixed(6)}, ${route.electricEndPoint.longitude.toFixed(6)})</span>
            </div>
        `;
    }

    // 添加道路信息
    if (route.roads && route.roads.length > 0) {
        detailsHtml += `
            <div class="detail-item">
                <span class="label">经过道路：</span>
                <span class="value">${route.roads.length} 条</span>
            </div>
            <div class="roads-list">
        `;

        route.roads.forEach(road => {
            detailsHtml += `
                <div class="road-item">
                    <span class="road-type ${road.roadType}">${road.roadType}</span>
                    <span class="road-crowd">拥挤度：${(road.crowdLevel * 100).toFixed(0)}%</span>
                </div>
            `;
        });

        detailsHtml += '</div>';
    }

    detailsHtml += '</div>';
    
    // 更新详情显示
    detailsContainer.innerHTML = detailsHtml;
    console.log('[Electric] Route details displayed');
}

// 电动车模式：处理路径规划错误
function handleElectricRouteError(error) {
    console.error('[Electric] Route planning error:', error);
    
    const errorContainer = document.getElementById('routeError');
    if (errorContainer) {
        errorContainer.innerHTML = `
            <div class="error-message">
                <h4>路径规划失败</h4>
                <p>${error.message}</p>
            </div>
        `;
    }
}

// 电动车模式：清除路径
function clearElectricRoute(map) {
    console.log('[Electric] Clearing route from map...');
    
    // 清除所有路径线
    map.eachLayer(layer => {
        if (layer instanceof L.Polyline) {
            map.removeLayer(layer);
        }
    });

    // 清除所有标记
    map.eachLayer(layer => {
        if (layer instanceof L.Marker) {
            map.removeLayer(layer);
        }
    });

    // 清除详情显示
    const detailsContainer = document.getElementById('routeDetails');
    if (detailsContainer) {
        detailsContainer.innerHTML = '';
    }

    // 清除错误信息
    const errorContainer = document.getElementById('routeError');
    if (errorContainer) {
        errorContainer.innerHTML = '';
    }

    console.log('[Electric] Route cleared');
} 