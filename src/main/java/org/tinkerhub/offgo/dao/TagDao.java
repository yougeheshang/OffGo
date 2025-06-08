package org.tinkerhub.offgo.dao;

import org.tinkerhub.offgo.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TagDao {
    
    @Select("SELECT * FROM tags")
    List<Tag> findAll();
    
    @Select("SELECT t.* FROM tags t " +
            "JOIN attraction_tags at ON t.id = at.tag_id " +
            "WHERE at.attraction_id = #{attractionId}")
    List<Tag> findByAttractionId(Long attractionId);
    
    @Insert("INSERT INTO tags (name) VALUES (#{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Tag tag);
    
    @Insert("INSERT INTO attraction_tags (attraction_id, tag_id) VALUES (#{attractionId}, #{tagId})")
    int addTagToAttraction(@Param("attractionId") Long attractionId, @Param("tagId") Long tagId);
    
    @Delete("DELETE FROM attraction_tags WHERE attraction_id = #{attractionId}")
    int removeAllTagsFromAttraction(Long attractionId);
    
    @Delete("DELETE FROM attraction_tags WHERE attraction_id = #{attractionId} AND tag_id = #{tagId}")
    int removeTagFromAttraction(@Param("attractionId") Long attractionId, @Param("tagId") Long tagId);
} 