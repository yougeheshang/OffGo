package org.tinkerhub.offgo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tinkerhub.offgo.model.RouteRequest;
import org.tinkerhub.offgo.model.RouteResponse;
import org.tinkerhub.offgo.service.RouteService;

@RestController
@RequestMapping("/api/route")
@CrossOrigin(origins = "*")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @PostMapping("/plan")
    public ResponseEntity<RouteResponse> planRoute(@RequestBody RouteRequest request) {
        try {
            RouteResponse response = routeService.planRoute(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/planMulti")
    public ResponseEntity<?> planRouteMulti(@RequestBody RouteRequest request) {
        try {
            return ResponseEntity.ok(routeService.planRouteMulti(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
} 