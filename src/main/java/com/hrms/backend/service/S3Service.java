package com.hrms.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.UUID;

@Service
public class S3Service {

    @Value("${app.storage.type:local}")
    private String storageType;

    @Value("${app.storage.local-dir:./uploads}")
    private String localUploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    // Generic upload method used by documents, resumes, and logos
    public String uploadFile(MultipartFile file, String folderPrefix) throws IOException {
        String originalName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "file";

        String safePrefix = folderPrefix != null ? folderPrefix.replace("\\", "/") : "";
        if (!safePrefix.isEmpty() && !safePrefix.endsWith("/")) {
            safePrefix += "/";
        }

        String uniqueFileName = safePrefix + UUID.randomUUID() + "_" + originalName;

        if ("local".equalsIgnoreCase(storageType)) {
            Path basePath = Paths.get(localUploadDir).toAbsolutePath().normalize();
            Path targetPath = basePath.resolve(uniqueFileName).normalize();
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return baseUrl + "/uploads/" + uniqueFileName;
        }

        try (S3Client s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build()) {

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + uniqueFileName;
        }
    }

    public String uploadResume(MultipartFile file) throws IOException {
        return uploadFile(file, "resumes/");
    }

    public String uploadLogo(MultipartFile file) throws IOException {
        return uploadFile(file, "logos/");
    }

    // Creates secure 15-minute viewing link (or direct local link when app.storage.type=local)
    public String generatePresignedUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return null;

        if ("local".equalsIgnoreCase(storageType)) {
            if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                return fileUrl;
            }
            String cleanPath = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            return baseUrl + "/uploads/" + cleanPath;
        }

        String fileKey = fileUrl.contains(".amazonaws.com/")
                ? fileUrl.substring(fileUrl.indexOf(".amazonaws.com/") + 15)
                : fileUrl;

        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build()) {

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();
        }
    }
}