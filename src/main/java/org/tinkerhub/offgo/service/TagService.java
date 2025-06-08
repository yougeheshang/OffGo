package org.tinkerhub.offgo.service;

import org.tinkerhub.offgo.entity.Tag;
import java.util.List;

public interface TagService {
    List<Tag> getAllTags();
    List<Tag> getTagsByAttractionId(Long attractionId);
    Tag createTag(Tag tag);
    void addTagToAttraction(Long attractionId, Long tagId);
    void removeTagFromAttraction(Long attractionId, Long tagId);
    void removeAllTagsFromAttraction(Long attractionId);
} 