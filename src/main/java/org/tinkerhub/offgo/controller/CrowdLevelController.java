package org.tinkerhub.offgo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tinkerhub.offgo.service.CrowdLevelService;

@RestController
@RequestMapping("/api/osm")
public class CrowdLevelController {

    @Autowired
    private CrowdLevelService crowdLevelService;

    @PostMapping("/refreshCrowdLevel")
    public ResponseEntity<String> refreshCrowdLevel() {
        try {
            crowdLevelService.updateCrowdLevels();
            return ResponseEntity.ok("拥挤度更新成功");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("拥挤度更新失败: " + e.getMessage());
        }
    }
} 