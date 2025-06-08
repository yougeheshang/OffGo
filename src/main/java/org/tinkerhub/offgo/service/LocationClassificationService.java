package org.tinkerhub.offgo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tinkerhub.offgo.entity.MapLocation;
import org.tinkerhub.offgo.entity.MapRoad;
import org.tinkerhub.offgo.repository.MapLocationRepository;
import org.tinkerhub.offgo.repository.MapRoadRepository;

import java.util.*;

@Service
public class LocationClassificationService {
    private static final Logger logger = LoggerFactory.getLogger(LocationClassificationService.class);

    @Autowired
    private MapLocationRepository mapLocationRepository;

    @Autowired
    private MapRoadRepository mapRoadRepository;

    // 英文类型到中文类型的映射
    private static final Map<String, String> TYPE_MAPPING = new HashMap<>();
    static {
        // 建筑物类型映射
        TYPE_MAPPING.put("park", "公园");
        TYPE_MAPPING.put("research_institute", "教学楼");
        TYPE_MAPPING.put("post_office", "邮局");
        TYPE_MAPPING.put("fast_food", "快餐");
        TYPE_MAPPING.put("cafe", "咖啡厅");
        TYPE_MAPPING.put("restaurant", "食堂");
        TYPE_MAPPING.put("shelter", "亭");
        TYPE_MAPPING.put("school", "教学楼");
        TYPE_MAPPING.put("university", "教学楼");
        TYPE_MAPPING.put("library", "图书馆");
        TYPE_MAPPING.put("hospital", "医院");
        TYPE_MAPPING.put("bank", "银行");
        TYPE_MAPPING.put("supermarket", "超市");
        TYPE_MAPPING.put("shop", "商店");
        TYPE_MAPPING.put("toilets", "洗手间");
        TYPE_MAPPING.put("stadium", "体育场");
        TYPE_MAPPING.put("sports_centre", "体育馆");
        TYPE_MAPPING.put("dormitory", "宿舍楼");
        TYPE_MAPPING.put("office", "办公楼");
        TYPE_MAPPING.put("museum", "博物馆");
        TYPE_MAPPING.put("theatre", "剧场");
        TYPE_MAPPING.put("gym", "健身房");
        TYPE_MAPPING.put("swimming_pool", "游泳馆");
        TYPE_MAPPING.put("pharmacy", "药店");
        TYPE_MAPPING.put("clinic", "医务室");
    }

    // 建筑物关键词映射
    private static final Map<String, String> BUILDING_KEYWORDS = new HashMap<>();
    static {
        // 教学楼
        BUILDING_KEYWORDS.put("教学楼", "教学楼");
        BUILDING_KEYWORDS.put("实验楼", "教学楼");
        BUILDING_KEYWORDS.put("教室", "教学楼");
        BUILDING_KEYWORDS.put("讲堂", "教学楼");
        BUILDING_KEYWORDS.put("报告厅", "教学楼");
        BUILDING_KEYWORDS.put("实验室", "教学楼");
        BUILDING_KEYWORDS.put("研究中心", "教学楼");
        BUILDING_KEYWORDS.put("学生社区活动中心", "教学楼");
        
        // 办公楼
        BUILDING_KEYWORDS.put("办公楼", "办公楼");
        BUILDING_KEYWORDS.put("行政楼", "办公楼");
        BUILDING_KEYWORDS.put("办公室", "办公楼");
        BUILDING_KEYWORDS.put("会议室", "办公楼");
        BUILDING_KEYWORDS.put("接待室", "办公楼");
        
        // 宿舍楼
        BUILDING_KEYWORDS.put("宿舍", "宿舍楼");
        BUILDING_KEYWORDS.put("公寓", "宿舍楼");
        BUILDING_KEYWORDS.put("学生公寓", "宿舍楼");
        BUILDING_KEYWORDS.put("教师公寓", "宿舍楼");
        BUILDING_KEYWORDS.put("留学生公寓", "宿舍楼");

        // 大门
        BUILDING_KEYWORDS.put("门", "大门");

        // 博物馆
        BUILDING_KEYWORDS.put("博物馆", "博物馆");
        BUILDING_KEYWORDS.put("展览馆", "博物馆");
        BUILDING_KEYWORDS.put("艺术馆", "博物馆");

        // 剧场
        BUILDING_KEYWORDS.put("剧场", "剧场");
        BUILDING_KEYWORDS.put("音乐厅", "剧场");
        BUILDING_KEYWORDS.put("礼堂", "剧场");
        BUILDING_KEYWORDS.put("演播厅", "剧场");
    }

    // 服务设施关键词映射
    private static final Map<String, String> SERVICE_KEYWORDS = new HashMap<>();
    static {
        // 商店
        SERVICE_KEYWORDS.put("商店", "商店");
        SERVICE_KEYWORDS.put("超市", "超市");
        SERVICE_KEYWORDS.put("商场", "商场");
        SERVICE_KEYWORDS.put("便利店", "商店");
        SERVICE_KEYWORDS.put("文具店", "商店");
        SERVICE_KEYWORDS.put("书店", "书店");
        SERVICE_KEYWORDS.put("打印店", "打印店");
        SERVICE_KEYWORDS.put("养发", "理发店");
        SERVICE_KEYWORDS.put("美发", "理发店");
        SERVICE_KEYWORDS.put("Coffee", "咖啡厅");
        SERVICE_KEYWORDS.put("有理咖（紫荆公寓店）", "咖啡厅");
        SERVICE_KEYWORDS.put("亮视点", "眼镜店");
        SERVICE_KEYWORDS.put("眼镜", "眼镜店");
        SERVICE_KEYWORDS.put("眼睛", "眼镜店");
        SERVICE_KEYWORDS.put("外贸", "小卖铺");
        SERVICE_KEYWORDS.put("小卖部", "小卖铺");
        SERVICE_KEYWORDS.put("西王庄便民蔬果", "小卖铺");
        SERVICE_KEYWORDS.put("摄影工作室", "摄影工作室");
        SERVICE_KEYWORDS.put("咔咔印像馆", "摄影工作室");
        SERVICE_KEYWORDS.put("鹏起广告快印制作中心", "广告制作中心");
        SERVICE_KEYWORDS.put("护肤", "美容店");
        SERVICE_KEYWORDS.put("美容", "美容店");
        SERVICE_KEYWORDS.put("同仁堂", "药店");
        SERVICE_KEYWORDS.put("花房", "花店");
        SERVICE_KEYWORDS.put("复印店", "复印店");
        SERVICE_KEYWORDS.put("紫荆物业管理处", "物业");
        SERVICE_KEYWORDS.put("芝兰园餐厅", "食堂");
        SERVICE_KEYWORDS.put("能量中心", "教学楼");
        
        // 食堂
        SERVICE_KEYWORDS.put("食堂", "食堂");
        SERVICE_KEYWORDS.put("餐厅", "食堂");
        SERVICE_KEYWORDS.put("饭堂", "食堂");
        SERVICE_KEYWORDS.put("美食广场", "食堂");
        SERVICE_KEYWORDS.put("快餐", "快餐");
        SERVICE_KEYWORDS.put("咖", "咖啡厅");
        SERVICE_KEYWORDS.put("茶室", "茶室");
        SERVICE_KEYWORDS.put("赛百味", "快餐");
        SERVICE_KEYWORDS.put("麦当劳", "快餐");
        SERVICE_KEYWORDS.put("肯德基", "快餐");
        SERVICE_KEYWORDS.put("必胜客", "快餐");
        SERVICE_KEYWORDS.put("汉堡王", "快餐");
        SERVICE_KEYWORDS.put("德克士", "快餐");
        SERVICE_KEYWORDS.put("华莱士", "快餐");
        SERVICE_KEYWORDS.put("沙县小吃", "快餐");
        
        // 洗手间
        SERVICE_KEYWORDS.put("厕所", "洗手间");
        SERVICE_KEYWORDS.put("洗手间", "洗手间");
        SERVICE_KEYWORDS.put("卫生间", "洗手间");
        SERVICE_KEYWORDS.put("盥洗室", "洗手间");
        
        // 图书馆
        SERVICE_KEYWORDS.put("图书馆", "图书馆");
        SERVICE_KEYWORDS.put("阅览室", "图书馆");
        SERVICE_KEYWORDS.put("资料室", "图书馆");
        SERVICE_KEYWORDS.put("自习室", "图书馆");
        SERVICE_KEYWORDS.put("电子阅览室", "图书馆");
        
        // 医疗设施
        SERVICE_KEYWORDS.put("医院", "医院");
        SERVICE_KEYWORDS.put("医务室", "医务室");
        SERVICE_KEYWORDS.put("诊所", "医务室");
        SERVICE_KEYWORDS.put("药店", "药店");
        
        // 体育设施
        SERVICE_KEYWORDS.put("体育场", "体育场");
        SERVICE_KEYWORDS.put("体育馆", "体育馆");
        SERVICE_KEYWORDS.put("游泳馆", "游泳馆");
        SERVICE_KEYWORDS.put("健身", "健身房");
        SERVICE_KEYWORDS.put("网球场", "网球场");
        SERVICE_KEYWORDS.put("篮球场", "篮球场");
        SERVICE_KEYWORDS.put("足球场", "足球场");
        
        // 其他服务设施
        SERVICE_KEYWORDS.put("银行", "银行");
        SERVICE_KEYWORDS.put("邮局", "邮局");
        SERVICE_KEYWORDS.put("快递点", "快递点");
        SERVICE_KEYWORDS.put("洗衣", "洗衣房");
        SERVICE_KEYWORDS.put("维修点", "维修点");
    }

    @Transactional
    public void classifyLocations() {
        logger.info("开始对位置点进行分类...");
        List<MapLocation> locations = mapLocationRepository.findByNameIsNotNull();
        logger.info("找到 {} 个命名位置点", locations.size());

        int buildingCount = 0;
        int serviceCount = 0;
        Map<String, Integer> typeCounts = new HashMap<>();

        for (MapLocation location : locations) {
            String name = location.getName();
            if (name == null || name.trim().isEmpty()) {
                continue;
            }

            // 首先检查是否为大门
            if (name.contains("门")) {
                location.setCategory("建筑物");
                location.setType("大门");
                buildingCount++;
                typeCounts.merge("大门", 1, Integer::sum);
                continue;
            }

            // 检查是否包含"系"或"学院"
            if (name.contains("系") || name.contains("学院")) {
                location.setCategory("建筑物");
                location.setType("教学楼");
                buildingCount++;
                typeCounts.merge("教学楼", 1, Integer::sum);
                logger.info("将位置点 {} 设置为教学楼（系/学院）", name);
                continue;
            }

            // 检查是否包含"清真"
            if (name.contains("清真")) {
                location.setCategory("服务设施");
                location.setType("食堂");
                serviceCount++;
                typeCounts.merge("食堂", 1, Integer::sum);
                logger.info("将位置点 {} 设置为食堂（清真）", name);
                continue;
            }

            // 检查原有英文类型并转换为中文
            String originalType = location.getType();
            if (originalType != null && TYPE_MAPPING.containsKey(originalType)) {
                String chineseType = TYPE_MAPPING.get(originalType);
                location.setType(chineseType);
                
                // 根据类型设置类别
                if (isBuildingType(chineseType)) {
                    location.setCategory("建筑物");
                    buildingCount++;
                } else {
                    location.setCategory("服务设施");
                    serviceCount++;
                }
                typeCounts.merge(chineseType, 1, Integer::sum);
                continue;
            }

            // 检查是否为建筑物
            String buildingType = findBuildingType(name);
            if (buildingType != null) {
                location.setCategory("建筑物");
                location.setType(buildingType);
                buildingCount++;
                typeCounts.merge(buildingType, 1, Integer::sum);
                continue;
            }

            // 检查是否为服务设施
            String serviceType = findServiceType(name);
            if (serviceType != null) {
                location.setCategory("服务设施");
                location.setType(serviceType);
                serviceCount++;
                typeCounts.merge(serviceType, 1, Integer::sum);
                continue;
            }
        }

        // 保存更新后的位置点
        mapLocationRepository.saveAll(locations);

        // 输出统计信息
        logger.info("分类完成！统计信息：");
        logger.info("建筑物总数：{}", buildingCount);
        logger.info("服务设施总数：{}", serviceCount);
        logger.info("各类型数量统计：");
        typeCounts.forEach((type, count) -> 
            logger.info("{}: {}", type, count));
    }

    private boolean isBuildingType(String type) {
        return Arrays.asList("教学楼", "办公楼", "宿舍楼", "大门", "景点", "博物馆", "剧场").contains(type);
    }

    private String findBuildingType(String name) {
        for (Map.Entry<String, String> entry : BUILDING_KEYWORDS.entrySet()) {
            if (name.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String findServiceType(String name) {
        for (Map.Entry<String, String> entry : SERVICE_KEYWORDS.entrySet()) {
            if (name.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
} 