package com.retail.convenience.modules.mp.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.security.TenantContext;
import com.retail.convenience.common.util.AesUtil;
import com.retail.convenience.modules.mp.entity.MpAccount;
import com.retail.convenience.modules.mp.entity.MpAutoReply;
import com.retail.convenience.modules.mp.entity.MpFans;
import com.retail.convenience.modules.mp.entity.MpMessage;
import com.retail.convenience.modules.mp.mapper.MpAccountMapper;
import com.retail.convenience.modules.mp.mapper.MpAutoReplyMapper;
import com.retail.convenience.modules.mp.mapper.MpFansMapper;
import com.retail.convenience.modules.mp.mapper.MpMessageMapper;
import com.retail.convenience.modules.platform.entity.PlatformTenant;
import com.retail.convenience.modules.platform.mapper.PlatformTenantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin-api/mp/open/{appId}")
public class MpCallbackController {

    private final PlatformTenantMapper tenantMapper;
    private final MpAccountMapper accountMapper;
    private final MpFansMapper fansMapper;
    private final MpMessageMapper messageMapper;
    private final MpAutoReplyMapper autoReplyMapper;
    private final String cryptoSecret;

    public MpCallbackController(PlatformTenantMapper tenantMapper, MpAccountMapper accountMapper,
                                MpFansMapper fansMapper, MpMessageMapper messageMapper,
                                MpAutoReplyMapper autoReplyMapper,
                                @Value("${app.jwt.secret}") String cryptoSecret) {
        this.tenantMapper = tenantMapper;
        this.accountMapper = accountMapper;
        this.fansMapper = fansMapper;
        this.messageMapper = messageMapper;
        this.autoReplyMapper = autoReplyMapper;
        this.cryptoSecret = cryptoSecret;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(@PathVariable String appId,
                                         @RequestParam String signature,
                                         @RequestParam String timestamp,
                                         @RequestParam String nonce,
                                         @RequestParam String echostr) {
        MpAccount account = findAccount(appId);
        if (account == null) {
            return ResponseEntity.status(404).body("account not found");
        }
        String token = decrypt(account.getToken());
        if (token.isBlank() || !signature(token, timestamp, nonce).equals(signature)) {
            return ResponseEntity.status(403).body("invalid signature");
        }
        return ResponseEntity.ok(echostr);
    }

    @PostMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> receive(@PathVariable String appId,
                                          @RequestParam String signature,
                                          @RequestParam String timestamp,
                                          @RequestParam String nonce,
                                          @RequestParam(required = false) String encrypt_type,
                                          @RequestBody String body) {
        if ("aes".equalsIgnoreCase(encrypt_type)) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN)
                    .body("暂不支持安全模式（AES）回调，请在公众号后台配置明文模式");
        }
        MpAccount account = findAccount(appId);
        if (account == null) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN).body("account not found");
        }
        String token = decrypt(account.getToken());
        if (token.isBlank() || !signature(token, timestamp, nonce).equals(signature)) {
            return ResponseEntity.status(403).contentType(MediaType.TEXT_PLAIN).body("invalid signature");
        }
        TenantContext.set(account.getTenantId(), null);
        try {
            Document document = parse(body);
            String from = text(document, "FromUserName");
            String to = text(document, "ToUserName");
            String msgType = text(document, "MsgType");
            String content = text(document, "Content");
            String event = text(document, "Event");
            MpFans fan = fansMapper.selectOne(new LambdaQueryWrapper<MpFans>()
                    .eq(MpFans::getAccountId, account.getId())
                    .eq(MpFans::getOpenid, from).last("limit 1"));
            if (fan == null) {
                fan = new MpFans();
                fan.setAccountId(account.getId());
                fan.setOpenid(from);
                fan.setSubscribeStatus(1);
                fan.setSubscribeTime(LocalDateTime.now());
                fansMapper.insert(fan);
            } else if ("unsubscribe".equalsIgnoreCase(event)) {
                fan.setSubscribeStatus(0);
                fansMapper.updateById(fan);
            } else if ("subscribe".equalsIgnoreCase(event)) {
                fan.setSubscribeStatus(1);
                fan.setSubscribeTime(LocalDateTime.now());
                fansMapper.updateById(fan);
            }
            MpMessage message = new MpMessage();
            message.setAccountId(account.getId());
            message.setOpenid(from);
            message.setMsgType(msgType == null || msgType.isBlank() ? "event" : msgType);
            message.setDirection("IN");
            message.setContent("event".equalsIgnoreCase(msgType) ? event : content);
            message.setRawPayload(body.length() > 4000 ? body.substring(0, 4000) : body);
            messageMapper.insert(message);

            String reply = findReply(account.getId(), msgType, event, content);
            if (reply == null || reply.isBlank()) {
                return ResponseEntity.ok("success");
            }
            MpMessage out = new MpMessage();
            out.setAccountId(account.getId());
            out.setOpenid(from);
            out.setMsgType("text");
            out.setDirection("OUT");
            out.setContent(reply);
            messageMapper.insert(out);
            String xml = "<xml><ToUserName><![CDATA[" + from + "]]></ToUserName>"
                    + "<FromUserName><![CDATA[" + to + "]]></FromUserName>"
                    + "<CreateTime>" + System.currentTimeMillis() / 1000 + "</CreateTime>"
                    + "<MsgType><![CDATA[text]]></MsgType>"
                    + "<Content><![CDATA[" + reply.replace("]]>", "") + "]]></Content></xml>";
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(xml);
        } catch (Exception e) {
            log.warn("公众号回调处理失败: {}", e.getMessage());
            return ResponseEntity.ok("success");
        } finally {
            TenantContext.clear();
        }
    }

    private String findReply(Long accountId, String msgType, String event, String content) {
        String scene = null;
        String keyword = null;
        if ("event".equalsIgnoreCase(msgType) && "subscribe".equalsIgnoreCase(event)) {
            scene = "FOLLOW";
        } else if ("text".equalsIgnoreCase(msgType)) {
            scene = "KEYWORD";
            keyword = content == null ? "" : content.trim();
        } else {
            scene = "MESSAGE";
        }
        if ("KEYWORD".equals(scene)) {
            List<MpAutoReply> replies = autoReplyMapper.selectList(new LambdaQueryWrapper<MpAutoReply>()
                    .eq(MpAutoReply::getAccountId, accountId)
                    .eq(MpAutoReply::getScene, "KEYWORD")
                    .eq(MpAutoReply::getEnabled, 1));
            for (MpAutoReply reply : replies) {
                if ("EXACT".equals(reply.getMatchType()) && keyword.equals(reply.getKeyword())) {
                    return reply.getContent();
                }
                if ("PART".equals(reply.getMatchType()) && reply.getKeyword() != null
                        && keyword.contains(reply.getKeyword())) {
                    return reply.getContent();
                }
            }
        }
        MpAutoReply fallback = autoReplyMapper.selectOne(new LambdaQueryWrapper<MpAutoReply>()
                .eq(MpAutoReply::getAccountId, accountId)
                .eq(MpAutoReply::getScene, scene)
                .eq(MpAutoReply::getEnabled, 1)
                .last("limit 1"));
        return fallback == null ? null : fallback.getContent();
    }

    private MpAccount findAccount(String appId) {
        List<PlatformTenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<PlatformTenant>()
                .eq(PlatformTenant::getDeleted, 0)
                .in(PlatformTenant::getStatus, "ACTIVE", "TRIAL", "OVERDUE"));
        for (PlatformTenant tenant : tenants) {
            TenantContext.set(tenant.getTenantId(), tenant.getTenantCode());
            try {
                MpAccount account = accountMapper.selectOne(new LambdaQueryWrapper<MpAccount>()
                        .eq(MpAccount::getAppId, appId).last("limit 1"));
                if (account != null) {
                    return account;
                }
            } finally {
                TenantContext.clear();
            }
        }
        return null;
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String text(Document document, String tag) {
        NodeList nodes = document.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    private String signature(String token, String timestamp, String nonce) {
        try {
            String[] parts = {token, timestamp, nonce};
            Arrays.sort(parts);
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(String.join("", parts).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "";
        }
    }

    private String decrypt(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.startsWith("ENC:") ? AesUtil.decrypt(value, cryptoSecret) : value;
    }
}
