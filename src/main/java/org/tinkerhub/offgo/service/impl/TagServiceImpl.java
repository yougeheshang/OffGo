package org.tinkerhub.offgo.service.impl;

import org.tinkerhub.offgo.dao.TagDao;
import org.tinkerhub.offgo.entity.Tag;
import org.tinkerhub.offgo.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private TagDao tagDao;

    @Override
    public List<Tag> getAllTags() {
        return tagDao.findAll();
    }

    @Override
    public List<Tag> getTagsByAttractionId(Long attractionId) {
        return tagDao.findByAttractionId(attractionId);
    }

    @Override
    @Transactional
    public Tag createTag(Tag tag) {
        tagDao.insert(tag);
        return tag;
    }

    @Override
    @Transactional
    public void addTagToAttraction(Long attractionId, Long tagId) {
        tagDao.addTagToAttraction(attractionId, tagId);
    }

    @Override
    @Transactional
    public void removeTagFromAttraction(Long attractionId, Long tagId) {
        tagDao.removeTagFromAttraction(attractionId, tagId);
    }

    @Override
    @Transactional
    public void removeAllTagsFromAttraction(Long attractionId) {
        tagDao.removeAllTagsFromAttraction(attractionId);
    }
} 