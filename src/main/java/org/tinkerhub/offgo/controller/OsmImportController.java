package org.tinkerhub.offgo.controller;

import org.tinkerhub.offgo.service.OsmImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/osm")
public class OsmImportController {
    private static final Logger logger = LoggerFactory.getLogger(OsmImportController.class);

    @Autowired
    private OsmImportService osmImportService;

    @GetMapping("/import")
    public ResponseEntity<String> importOsmDataGet() {
        return importOsmData();
    }

    @PostMapping("/import")
    public ResponseEntity<String> importOsmDataPost() {
        return importOsmData();
    }

    private ResponseEntity<String> importOsmData() {
        try {
            String osmFilePath = "src/main/resources/static/map/tsinghua.osm";
            File osmFile = new File(osmFilePath);
            
            if (!osmFile.exists()) {
                String errorMsg = "OSM file not found at path: " + osmFilePath;
                logger.error(errorMsg);
                return ResponseEntity.badRequest().body(errorMsg);
            }
            
            logger.info("Starting OSM data import from file: {}", osmFilePath);
            osmImportService.importOsmData(osmFilePath);
            logger.info("OSM data import completed successfully");
            
            return ResponseEntity.ok("OSM data imported successfully");
        } catch (ParserConfigurationException e) {
            String errorMsg = "XML Parser configuration error: " + e.getMessage();
            logger.error(errorMsg, e);
            return ResponseEntity.badRequest().body(errorMsg);
        } catch (IOException e) {
            String errorMsg = "File IO error: " + e.getMessage();
            logger.error(errorMsg, e);
            return ResponseEntity.badRequest().body(errorMsg);
        } catch (SAXException e) {
            String errorMsg = "XML parsing error: " + e.getMessage();
            logger.error(errorMsg, e);
            return ResponseEntity.badRequest().body(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Unexpected error: " + e.getMessage() + "\n" + e.toString();
            logger.error(errorMsg, e);
            return ResponseEntity.internalServerError().body(errorMsg);
        }
    }
} 