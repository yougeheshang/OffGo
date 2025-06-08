package org.tinkerhub.offgo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tinkerhub.offgo.service.PointOfInterestService;

@RestController
@RequestMapping("/api/points-of-interest")
public class PointOfInterestController {

    @Autowired
    private PointOfInterestService pointOfInterestService;

    @PostMapping("/update-names")
    public ResponseEntity<String> updatePointOfInterestNames() {
        try {
            pointOfInterestService.updatePointOfInterestNames();
            return ResponseEntity.ok("兴趣点名称更新成功");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("更新失败：" + e.getMessage());
        }
    }

    @PostMapping("/delete-nameless")
    public ResponseEntity<String> deleteNamelessPoints() {
        try {
            pointOfInterestService.deleteNamelessPoints();
            return ResponseEntity.ok("删除没有名称的兴趣点成功");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("删除失败：" + e.getMessage());
        }
    }

    @PostMapping("/delete-specific-types")
    public ResponseEntity<String> deleteSpecificTypePoints() {
        try {
            pointOfInterestService.deleteSpecificTypePoints();
            return ResponseEntity.ok("删除特定类型的兴趣点成功");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("删除失败：" + e.getMessage());
        }
    }
} 