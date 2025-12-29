package com.quizgenix.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class ImageService {

    @Autowired
    private Cloudinary cloudinary;

    // 1. UPLOAD
    public String uploadImage(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("resource_type", "auto"));
        return (String) uploadResult.get("secure_url");
    }

    // 2. DELETE (New!)
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty())
            return;

        try {
            // We need the "Public ID" to delete.
            // URL format: https://.../upload/v12345/MyImageID.jpg

            // 1. Get the filename (MyImageID.jpg)
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);

            // 2. Remove the extension (.jpg) to get "MyImageID"
            String publicId = filename.substring(0, filename.lastIndexOf("."));

            // 3. Call Cloudinary API to delete
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

        } catch (IOException e) {
            // If it fails, print error but don't stop the account deletion
            System.err.println("Warning: Could not delete image from Cloudinary: " + e.getMessage());
        }
    }
}