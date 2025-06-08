package org.tinkerhub.offgo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tinkerhub.offgo.service.OsmService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/osm")
public class OsmController {

    private final OsmService osmService;

    public OsmController(OsmService osmService) {
        this.osmService = osmService;
    }

    @PostMapping("/nearbyServices")
    public ResponseEntity<?> getNearbyServices(@RequestBody Map<String, Object> request) {
        try {
            double latitude = Double.parseDouble(request.get("latitude").toString());
            double longitude = Double.parseDouble(request.get("longitude").toString());
            double radius = Double.parseDouble(request.get("radius").toString());

            List<Map<String, Object>> services = osmService.findNearbyServices(latitude, longitude, radius);
            
            Map<String, Object> response = new HashMap<>();
            response.put("services", services);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to get nearby services: " + e.getMessage());
        }
    }
} 