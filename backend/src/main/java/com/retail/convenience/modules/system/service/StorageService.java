package com.retail.convenience.modules.system.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.common.util.AesUtil;
import com.retail.convenience.modules.platform.service.PlatformQuotaService;
import com.retail.convenience.modules.system.entity.SysFile;
import com.retail.convenience.modules.system.entity.SysStorageConfig;
import com.retail.convenience.modules.system.mapper.SysFileMapper;
import com.retail.convenience.modules.system.mapper.SysStorageConfigMapper;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class StorageService {

    private final SysStorageConfigMapper configMapper;
    private final SysFileMapper fileMapper;
    private final PlatformQuotaService quotaService;
    private final ObjectMapper objectMapper;
    private final String localRoot;
    private final String cryptoSecret;

    public StorageService(SysStorageConfigMapper configMapper, SysFileMapper fileMapper,
                          PlatformQuotaService quotaService, ObjectMapper objectMapper,
                          @Value("${app.storage.local-root:./data/uploads}") String localRoot,
                          @Value("${app.jwt.secret}") String cryptoSecret) {
        this.configMapper = configMapper;
        this.fileMapper = fileMapper;
        this.quotaService = quotaService;
        this.objectMapper = objectMapper;
        this.localRoot = localRoot;
        this.cryptoSecret = cryptoSecret;
    }

    public SysStorageConfig currentConfig(Long tenantId) {
        SysStorageConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysStorageConfig>()
                .eq(SysStorageConfig::getTenantId, tenantId).last("limit 1"));
        if (config == null) {
            config = new SysStorageConfig();
            config.setTenantId(tenantId);
            config.setDriver("LOCAL");
            config.setEnabled(1);
            config.setConfig("{\"rootPath\":\"./data/uploads\",\"publicBaseUrl\":\"/uploads\"}");
        }
        return config;
    }

    public Map<String, Object> test(String driver, Map<String, Object> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        if ("LOCAL".equals(driver)) {
            try {
                Path root = Path.of(localRoot, "tenants", "_test");
                Files.createDirectories(root);
                Path file = root.resolve("test-" + System.currentTimeMillis() + ".txt");
                Files.writeString(file, "ok");
                Files.deleteIfExists(file);
                result.put("success", true);
                result.put("message", "本地磁盘可写，连接测试通过");
                return result;
            } catch (Exception e) {
                throw new BizException("本地磁盘不可写：" + e.getMessage());
            }
        }
        if ("MINIO".equals(driver) || "S3".equals(driver)) {
            try (MinioClient client = buildMinio(config)) {
                String bucket = String.valueOf(config.get("bucket"));
                boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists && Boolean.TRUE.equals(config.get("autoCreateBucket"))) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    exists = true;
                }
                if (!exists) {
                    throw new BizException("Bucket 不存在：" + bucket);
                }
                result.put("success", true);
                result.put("message", "对象存储连接测试通过");
                return result;
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                throw new BizException("对象存储连接失败：" + e.getMessage());
            }
        }
        if ("OSS".equals(driver) || "COS".equals(driver)) {
            String bucket = String.valueOf(config.get("bucket"));
            try {
                if ("OSS".equals(driver)) {
                    OSS client = buildOss(config);
                    try {
                        try {
                            client.getBucketInfo(bucket);
                        } catch (Exception e) {
                            if (Boolean.TRUE.equals(config.get("autoCreateBucket"))) {
                                client.createBucket(bucket);
                            } else {
                                throw new BizException("OSS 连接失败：" + e.getMessage());
                            }
                        }
                    } finally {
                        client.shutdown();
                    }
                } else {
                    COSClient client = buildCos(config);
                    try {
                        client.getBucketAcl(bucket);
                    } finally {
                        client.shutdown();
                    }
                }
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                throw new BizException(driver + " 连接失败：" + e.getMessage());
            }
            result.put("success", true);
            result.put("message", "对象存储连接测试通过");
            return result;
        }
        throw new BizException("不支持的存储驱动：" + driver);
    }

    public SysFile upload(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择文件");
        }
        Long tenantId = AuthContext.require().getTenantId();
        quotaService.assertStorage(tenantId, file.getSize());
        SysStorageConfig config = currentConfig(tenantId);
        Map<String, Object> map = parse(config.getConfig());
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "bin";
        String objectName = "tenants/" + tenantId + "/" + (bizType == null ? "general" : bizType)
                + "/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            String url;
            if ("MINIO".equals(config.getDriver()) || "S3".equals(config.getDriver())) {
                try (MinioClient client = buildMinio(map); InputStream in = file.getInputStream()) {
                    String bucket = String.valueOf(map.get("bucket"));
                    boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                    if (!exists) {
                        client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    }
                    client.putObject(PutObjectArgs.builder().bucket(bucket).object(objectName)
                            .stream(in, file.getSize(), -1).contentType(file.getContentType()).build());
                    String base = map.get("publicBaseUrl") == null
                            ? String.valueOf(map.get("endpoint")) + "/" + bucket
                            : String.valueOf(map.get("publicBaseUrl"));
                    url = base.replaceAll("/$", "") + "/" + objectName;
                }
            } else if ("OSS".equals(config.getDriver())) {
                OSS client = buildOss(map);
                try {
                    String bucket = String.valueOf(map.get("bucket"));
                    if (!client.doesBucketExist(bucket)) {
                        client.createBucket(bucket);
                    }
                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentType(file.getContentType());
                    client.putObject(bucket, objectName, file.getInputStream(), metadata);
                    url = publicUrl(map, objectName, "https://" + bucket + "."
                            + host(map.get("endpoint")) + "/");
                } finally {
                    client.shutdown();
                }
            } else if ("COS".equals(config.getDriver())) {
                COSClient client = buildCos(map);
                try {
                    String bucket = String.valueOf(map.get("bucket"));
                    com.qcloud.cos.model.ObjectMetadata cosMeta = new com.qcloud.cos.model.ObjectMetadata();
                    cosMeta.setContentType(file.getContentType());
                    PutObjectRequest request = new PutObjectRequest(bucket, objectName,
                            file.getInputStream(), cosMeta);
                    client.putObject(request);
                    url = publicUrl(map, objectName, "https://" + bucket + ".cos."
                            + map.get("region") + ".myqcloud.com/");
                } finally {
                    client.shutdown();
                }
            } else {
                Path target = Path.of(localRoot).resolve(objectName).toAbsolutePath().normalize();
                Files.createDirectories(target.getParent());
                file.transferTo(target.toFile());
                url = "/uploads/" + objectName;
            }
            SysFile record = new SysFile();
            record.setTenantId(tenantId);
            record.setName(original);
            record.setPath(objectName);
            record.setUrl(url);
            record.setSize(file.getSize());
            record.setContentType(file.getContentType());
            record.setBizType(bizType == null ? "GENERAL" : bizType);
            record.setDriver(config.getDriver());
            fileMapper.insert(record);
            return record;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("文件上传失败：" + e.getMessage());
        }
    }

    public void delete(String objectName) {
        Long tenantId = AuthContext.require().getTenantId();
        SysStorageConfig config = currentConfig(tenantId);
        try {
            if ("MINIO".equals(config.getDriver()) || "S3".equals(config.getDriver())) {
                Map<String, Object> map = parse(config.getConfig());
                try (MinioClient client = buildMinio(map)) {
                    client.removeObject(RemoveObjectArgs.builder()
                            .bucket(String.valueOf(map.get("bucket"))).object(objectName).build());
                }
            } else if ("OSS".equals(config.getDriver())) {
                Map<String, Object> map = parse(config.getConfig());
                OSS client = buildOss(map);
                try {
                    client.deleteObject(String.valueOf(map.get("bucket")), objectName);
                } finally {
                    client.shutdown();
                }
            } else if ("COS".equals(config.getDriver())) {
                Map<String, Object> map = parse(config.getConfig());
                COSClient client = buildCos(map);
                try {
                    client.deleteObject(String.valueOf(map.get("bucket")), objectName);
                } finally {
                    client.shutdown();
                }
            } else {
                Files.deleteIfExists(Path.of(localRoot).resolve(objectName));
            }
            fileMapper.delete(new LambdaQueryWrapper<SysFile>()
                    .eq(SysFile::getTenantId, tenantId).eq(SysFile::getPath, objectName));
        } catch (Exception e) {
            throw new BizException("文件删除失败：" + e.getMessage());
        }
    }

    public Map<String, Object> maskedConfig(SysStorageConfig config) {
        Map<String, Object> map = parse(config.getConfig());
        for (String key : map.keySet()) {
            if (key.toLowerCase().contains("secret") || key.toLowerCase().contains("accesskey")) {
                map.put(key, AesUtil.mask(String.valueOf(map.get(key))));
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("driver", config.getDriver());
        result.put("config", map);
        result.put("enabled", config.getEnabled());
        result.put("lastTestAt", config.getLastTestAt());
        return result;
    }

    public Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private OSS buildOss(Map<String, Object> config) {
        String endpoint = String.valueOf(config.get("endpoint"));
        if (!endpoint.startsWith("http")) {
            endpoint = "https://" + endpoint;
        }
        return new OSSClientBuilder().build(endpoint,
                decryptValue(config.get("accessKey")), decryptValue(config.get("secretKey")));
    }

    private COSClient buildCos(Map<String, Object> config) {
        COSCredentials credentials = new BasicCOSCredentials(
                decryptValue(config.get("accessKey")), decryptValue(config.get("secretKey")));
        ClientConfig clientConfig = new ClientConfig(new Region(String.valueOf(config.get("region"))));
        return new COSClient(credentials, clientConfig);
    }

    private String publicUrl(Map<String, Object> config, String objectName, String defaultBase) {
        Object base = config.get("publicBaseUrl");
        String prefix = base == null || String.valueOf(base).isBlank()
                ? defaultBase : String.valueOf(base);
        return prefix.replaceAll("/$", "") + "/" + objectName;
    }

    private String host(Object endpoint) {
        if (endpoint == null) {
            return "";
        }
        return String.valueOf(endpoint).replaceFirst("^https?://", "").replaceAll("/$", "");
    }

    private MinioClient buildMinio(Map<String, Object> config) {
        String endpoint = String.valueOf(config.get("endpoint"));
        if (!endpoint.startsWith("http")) {
            endpoint = "https://" + endpoint;
        }
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(decryptValue(config.get("accessKey")), decryptValue(config.get("secretKey")));
        Object region = config.get("region");
        if (region != null && !String.valueOf(region).isBlank()) {
            builder.region(String.valueOf(region));
        }
        return builder.build();
    }

    public String decryptValue(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return text.startsWith("ENC:") ? AesUtil.decrypt(text, cryptoSecret) : text;
    }
}
