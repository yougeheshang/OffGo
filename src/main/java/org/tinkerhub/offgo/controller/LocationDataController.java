package org.tinkerhub.offgo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tinkerhub.offgo.service.LocationDataService;

@RestController
@RequestMapping("/api/location")
public class LocationDataController {

    @Autowired
    private LocationDataService locationDataService;

    @GetMapping("/update-all")
    public ResponseEntity<String> updateAllLocations() {
        try {
            locationDataService.updateLocationData();
            return ResponseEntity.ok("All locations updated successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to update locations: " + e.getMessage());
        }
    }

    @GetMapping("/update/{id}")
    public ResponseEntity<String> updateSingleLocation(@PathVariable Long id) {
        try {
            locationDataService.updateSingleLocation(id);
            return ResponseEntity.ok("Location updated successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to update location: " + e.getMessage());
        }
    }

    @GetMapping("/update-unknown-to-road-nodes")
    public ResponseEntity<String> updateUnknownLocationsToRoadNodes() {
        try {
            locationDataService.updateUnknownLocationsToRoadNodes();
            return ResponseEntity.ok("Unknown locations updated to road nodes successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to update unknown locations: " + e.getMessage());
        }
    }
} 