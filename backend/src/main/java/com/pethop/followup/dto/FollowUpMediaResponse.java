package com.pethop.followup.dto;
import com.pethop.followup.FollowUpMedia;
import java.util.UUID;
public record FollowUpMediaResponse(UUID id,String url,String originalFilename,String contentType,long fileSize){public static FollowUpMediaResponse from(FollowUpMedia m){return new FollowUpMediaResponse(m.getId(),"/api/v1/follow-up-media/"+m.getId(),m.getOriginalFilename(),m.getContentType(),m.getFileSize());}}
