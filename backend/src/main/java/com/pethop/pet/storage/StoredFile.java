package com.pethop.pet.storage;
public record StoredFile(String path, String contentType, long size, String originalFilename) { }
