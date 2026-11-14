package com.retail.convenience.modules.mp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.util.AesUtil;
import com.retail.convenience.modules.mp.entity.MpAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@Service
public class WeChatMpClient {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String cryptoSecret;

    public WeChatMpClient(StringRedisTemplate redis, ObjectMapper objectMapper,
                          @Value("${app.jwt.secret}") String cryptoSecret) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.cryptoSecret = cryptoSecret;
    }

    public String accessToken(MpAccount account) {
        String secret = account.getAppSecret() == null ? "" : AesUtil.decrypt(account.getAppSecret(), cryptoSecret);
        if (secret.isBlank()) {
            throw new BizException("公众号 AppSecret 未配置，无法调用微信接口");
        }
        String key = "mp:token:" + account.getId();
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + account.getAppId() + "&secret=" + secret;
        try {
            JsonNode node = objectMapper.readTree(restTemplate.getForObject(url, String.class));
            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                throw new BizException("获取微信 access_token 失败：" + node.get("errmsg").asText());
            }
            String token = node.get("access_token").asText();
            redis.opsForValue().set(key, token, Duration.ofSeconds(Math.max(60, node.get("expires_in").asInt() - 300)));
            return token;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("调用微信接口失败：" + e.getMessage());
        }
    }

    public JsonNode get(MpAccount account, String path) {
        return execute(account, "GET", path, null);
    }

    public JsonNode post(MpAccount account, String path, Object body) {
        return execute(account, "POST", path, body);
    }

    public JsonNode postFile(MpAccount account, String path, MultipartFile file) {
        try {
            String url = "https://api.weixin.qq.com/cgi-bin/" + path
                    + "?access_token=" + accessToken(account);
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("media", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
                }
            });
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
            JsonNode node = objectMapper.readTree(response.getBody());
            check(node);
            return node;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("上传微信素材失败：" + e.getMessage());
        }
    }

    private JsonNode execute(MpAccount account, String method, String path, Object body) {
        try {
            String token = accessToken(account);
            String separator = path.contains("?") ? "&" : "?";
            String url = "https://api.weixin.qq.com/cgi-bin/" + path + separator + "access_token=" + token;
            String response;
            if ("GET".equals(method)) {
                response = restTemplate.getForObject(url, String.class);
            } else {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                String json = body == null ? "{}" : objectMapper.writeValueAsString(body);
                response = restTemplate.postForObject(url, new HttpEntity<>(json, headers), String.class);
            }
            JsonNode node = objectMapper.readTree(response);
            check(node);
            return node;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("微信接口调用失败：" + e.getMessage());
        }
    }

    private void check(JsonNode node) {
        if (node.has("errcode") && node.get("errcode").asInt() != 0) {
            throw new BizException("微信接口错误 [" + node.get("errcode").asInt() + "] "
                    + node.path("errmsg").asText());
        }
    }
}
