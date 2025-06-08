package org.tinkerhub.offgo.controller;

import org.tinkerhub.offgo.entity.Tag;
import org.tinkerhub.offgo.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private TagService tagService;

    @GetMapping
    public ResponseEntity<List<Tag>> getAllTags() {
        return ResponseEntity.ok(tagService.getAllTags());
    }

    @GetMapping("/attraction/{attractionId}")
    public ResponseEntity<List<Tag>> getTagsByAttractionId(@PathVariable Long attractionId) {
        return ResponseEntity.ok(tagService.getTagsByAttractionId(attractionId));
    }

    @PostMapping
    public ResponseEntity<Tag> createTag(@RequestBody Tag tag) {
        return ResponseEntity.ok(tagService.createTag(tag));
    }

    @PostMapping("/attraction/{attractionId}/tag/{tagId}")
    public ResponseEntity<Void> addTagToAttraction(
            @PathVariable Long attractionId,
            @PathVariable Long tagId) {
        tagService.addTagToAttraction(attractionId, tagId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/attraction/{attractionId}/tag/{tagId}")
    public ResponseEntity<Void> removeTagFromAttraction(
            @PathVariable Long attractionId,
            @PathVariable Long tagId) {
        tagService.removeTagFromAttraction(attractionId, tagId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/attraction/{attractionId}")
    public ResponseEntity<Void> removeAllTagsFromAttraction(@PathVariable Long attractionId) {
        tagService.removeAllTagsFromAttraction(attractionId);
        return ResponseEntity.ok().build();
    }
} 