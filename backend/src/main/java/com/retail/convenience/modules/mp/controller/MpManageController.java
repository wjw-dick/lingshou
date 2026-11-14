package com.retail.convenience.modules.mp.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.api.PageResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.modules.mp.entity.MpAccount;
import com.retail.convenience.modules.mp.entity.MpAutoReply;
import com.retail.convenience.modules.mp.entity.MpFans;
import com.retail.convenience.modules.mp.entity.MpMessage;
import com.retail.convenience.modules.mp.entity.MpTag;
import com.retail.convenience.modules.mp.entity.MpTemplate;
import com.retail.convenience.modules.mp.mapper.MpAccountMapper;
import com.retail.convenience.modules.mp.mapper.MpAutoReplyMapper;
import com.retail.convenience.modules.mp.mapper.MpFansMapper;
import com.retail.convenience.modules.mp.mapper.MpMessageMapper;
import com.retail.convenience.modules.mp.mapper.MpTagMapper;
import com.retail.convenience.modules.mp.mapper.MpTemplateMapper;
import com.retail.convenience.modules.mp.service.WeChatMpClient;
import com.retail.convenience.modules.system.entity.SysMenu;
import com.retail.convenience.modules.system.mapper.SysMenuMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant/mp")
public class MpManageController {

    private final MpAccountMapper accountMapper;
    private final MpTagMapper tagMapper;
    private final MpFansMapper fansMapper;
    private final MpMessageMapper messageMapper;
    private final MpTemplateMapper templateMapper;
    private final MpAutoReplyMapper autoReplyMapper;
    private final SysMenuMapper menuMapper;
    private final WeChatMpClient weChatClient;

    public MpManageController(MpAccountMapper accountMapper, MpTagMapper tagMapper,
                              MpFansMapper fansMapper, MpMessageMapper messageMapper,
                              MpTemplateMapper templateMapper, MpAutoReplyMapper autoReplyMapper,
                              SysMenuMapper menuMapper, WeChatMpClient weChatClient) {
        this.accountMapper = accountMapper;
        this.tagMapper = tagMapper;
        this.fansMapper = fansMapper;
        this.messageMapper = messageMapper;
        this.templateMapper = templateMapper;
        this.autoReplyMapper = autoReplyMapper;
        this.menuMapper = menuMapper;
        this.weChatClient = weChatClient;
    }

    @GetMapping("/tags")
    @RequirePerm("mp:view")
    public ApiResult<List<MpTag>> tags(@RequestParam(required = false) Long accountId) {
        return ApiResult.ok(tagMapper.selectList(new LambdaQueryWrapper<MpTag>()
                .eq(accountId != null, MpTag::getAccountId, accountId)
                .orderByAsc(MpTag::getId)));
    }

    @PostMapping("/tags")
    @RequirePerm("mp:update")
    public ApiResult<MpTag> createTag(@RequestBody MpTag tag) {
        if (tag.getName() == null || tag.getName().isBlank()) {
            throw new BizException("标签名称不能为空");
        }
        Long accountId = resolveAccountId(tag.getAccountId());
        if (tagMapper.selectCount(new LambdaQueryWrapper<MpTag>()
                .eq(MpTag::getAccountId, accountId).eq(MpTag::getName, tag.getName())) > 0) {
            throw new BizException("标签名称已存在");
        }
        tag.setId(null);
        tag.setAccountId(accountId);
        tag.setTenantId(AuthContext.require().getTenantId());
        tag.setFansCount(0);
        MpAccount account = accountMapper.selectById(accountId);
        try {
            JsonNode node = weChatClient.post(account, "tags/create", Map.of("tag", Map.of("name", tag.getName())));
            tag.setWxTagId(node.path("tag").path("id").asLong());
            tag.setSyncStatus("SYNCED");
        } catch (BizException e) {
            tag.setSyncStatus("LOCAL");
        }
        tagMapper.insert(tag);
        return ApiResult.ok(tag);
    }

    @DeleteMapping("/tags/{id}")
    @RequirePerm("mp:update")
    public ApiResult<Void> deleteTag(@PathVariable Long id) {
        MpTag tag = tagMapper.selectById(id);
        if (tag == null) {
            return ApiResult.ok();
        }
        if (tag.getWxTagId() != null) {
            try {
                weChatClient.post(accountMapper.selectById(tag.getAccountId()), "tags/delete",
                        Map.of("tag", Map.of("id", tag.getWxTagId())));
            } catch (BizException ignored) {
                // 微信未配置或删除失败时仍允许移除本地标签记录
            }
        }
        tagMapper.deleteById(id);
        return ApiResult.ok();
    }

    @PostMapping("/tags/sync")
    @RequirePerm("mp:update")
    public ApiResult<Map<String, Object>> syncTags(@RequestParam Long accountId) {
        MpAccount account = requireAccount(accountId);
        JsonNode node = weChatClient.get(account, "tags/get");
        int count = 0;
        for (JsonNode tag : node.path("tags")) {
            long wxId = tag.path("id").asLong();
            MpTag exists = tagMapper.selectOne(new LambdaQueryWrapper<MpTag>()
                    .eq(MpTag::getAccountId, accountId).eq(MpTag::getWxTagId, wxId).last("limit 1"));
            if (exists == null) {
                MpTag newTag = new MpTag();
                newTag.setAccountId(accountId);
                newTag.setTenantId(AuthContext.require().getTenantId());
                newTag.setWxTagId(wxId);
                newTag.setName(tag.path("name").asText());
                newTag.setFansCount(tag.path("count").asInt());
                newTag.setSyncStatus("SYNCED");
                tagMapper.insert(newTag);
            } else {
                exists.setName(tag.path("name").asText());
                exists.setFansCount(tag.path("count").asInt());
                exists.setSyncStatus("SYNCED");
                tagMapper.updateById(exists);
            }
            count++;
        }
        return ApiResult.ok(Map.of("synced", count));
    }

    @GetMapping("/fans")
    @RequirePerm("mp:view")
    public ApiResult<PageResult<MpFans>> fans(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String openid,
            @RequestParam(required = false) String nickname) {
        Page<MpFans> result = fansMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MpFans>()
                        .eq(accountId != null, MpFans::getAccountId, accountId)
                        .like(openid != null && !openid.isBlank(), MpFans::getOpenid, openid)
                        .like(nickname != null && !nickname.isBlank(), MpFans::getNickname, nickname)
                        .orderByDesc(MpFans::getSubscribeTime));
        return ApiResult.ok(PageResult.of(result));
    }

    @PostMapping("/fans/sync")
    @RequirePerm("mp:update")
    public ApiResult<Map<String, Object>> syncFans(@RequestParam Long accountId) {
        MpAccount account = requireAccount(accountId);
        JsonNode list = weChatClient.get(account, "user/get?");
        List<String> openids = new ArrayList<>();
        for (JsonNode item : list.path("data").path("openid")) {
            openids.add(item.asText());
        }
        if (openids.isEmpty() && list.has("data")) {
            String single = list.path("data").path("openid").asText(null);
            if (single != null && !single.isBlank()) {
                openids.add(single);
            }
        }
        int synced = 0;
        if (!openids.isEmpty()) {
            JsonNode info = weChatClient.post(account, "user/info/batchget",
                    Map.of("user_list", openids.stream().map(id -> Map.of("openid", id, "lang", "zh_CN")).toList()));
            for (JsonNode user : info.path("user_info_list")) {
                upsertFan(accountId, user);
                synced++;
            }
        }
        return ApiResult.ok(Map.of("synced", synced));
    }

    @PostMapping("/fans/{id}/tags")
    @RequirePerm("mp:update")
    public ApiResult<Void> tagFan(@PathVariable Long id, @RequestBody List<Long> wxTagIds) {
        MpFans fan = fansMapper.selectById(id);
        if (fan == null) {
            throw BizException.notFound("粉丝不存在");
        }
        fan.setTags(wxTagIds.toString());
        fansMapper.updateById(fan);
        try {
            weChatClient.post(accountMapper.selectById(fan.getAccountId()), "tags/members/batchtagging",
                    Map.of("openid_list", List.of(fan.getOpenid()), "tagid", wxTagIds.isEmpty() ? 0 : wxTagIds.get(0)));
        } catch (BizException ignored) {
            // 本地已更新，微信侧待配置凭证后同步
        }
        return ApiResult.ok();
    }

    @PutMapping("/fans/{id}/remark")
    @RequirePerm("mp:update")
    public ApiResult<Void> remark(@PathVariable Long id, @RequestParam String remark) {
        MpFans fan = fansMapper.selectById(id);
        if (fan == null) {
            throw BizException.notFound("粉丝不存在");
        }
        fan.setRemark(remark);
        fansMapper.updateById(fan);
        try {
            weChatClient.post(accountMapper.selectById(fan.getAccountId()), "user/info/updateremark",
                    Map.of("openid", fan.getOpenid(), "remark", remark));
        } catch (BizException ignored) {
        }
        return ApiResult.ok();
    }

    @GetMapping("/messages")
    @RequirePerm("mp:view")
    public ApiResult<PageResult<MpMessage>> messages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String msgType,
            @RequestParam(required = false) String openid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Page<MpMessage> result = messageMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MpMessage>()
                        .eq(accountId != null, MpMessage::getAccountId, accountId)
                        .eq(msgType != null && !msgType.isBlank(), MpMessage::getMsgType, msgType)
                        .like(openid != null && !openid.isBlank(), MpMessage::getOpenid, openid)
                        .ge(start != null, MpMessage::getCreatedAt, start)
                        .le(end != null, MpMessage::getCreatedAt, end)
                        .orderByDesc(MpMessage::getCreatedAt));
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/messages/{id}")
    @RequirePerm("mp:view")
    public ApiResult<MpMessage> message(@PathVariable Long id) {
        MpMessage message = messageMapper.selectById(id);
        if (message == null) {
            throw BizException.notFound("消息不存在");
        }
        return ApiResult.ok(message);
    }

    @PostMapping("/messages/send")
    @RequirePerm("mp:update")
    public ApiResult<Void> sendMessage(@RequestBody Map<String, Object> request) {
        Long accountId = resolveAccountId(request.get("accountId") == null ? null
                : Long.valueOf(String.valueOf(request.get("accountId"))));
        String openid = String.valueOf(request.get("openid"));
        String content = String.valueOf(request.get("content"));
        MpAccount account = requireAccount(accountId);
        weChatClient.post(account, "message/custom/send",
                Map.of("touser", openid, "msgtype", "text", "text", Map.of("content", content)));
        MpMessage message = new MpMessage();
        message.setAccountId(accountId);
        message.setOpenid(openid);
        message.setMsgType("text");
        message.setDirection("OUT");
        message.setContent(content);
        messageMapper.insert(message);
        return ApiResult.ok();
    }

    @GetMapping("/templates")
    @RequirePerm("mp:view")
    public ApiResult<List<MpTemplate>> templates(@RequestParam(required = false) Long accountId) {
        return ApiResult.ok(templateMapper.selectList(new LambdaQueryWrapper<MpTemplate>()
                .eq(accountId != null, MpTemplate::getAccountId, accountId)
                .orderByAsc(MpTemplate::getId)));
    }

    @PostMapping("/templates/sync")
    @RequirePerm("mp:update")
    public ApiResult<Map<String, Object>> syncTemplates(@RequestParam Long accountId) {
        MpAccount account = requireAccount(accountId);
        JsonNode node = weChatClient.get(account, "template/get_all_private_template");
        int synced = 0;
        for (JsonNode item : node.path("template_list")) {
            String templateId = item.path("template_id").asText();
            MpTemplate exists = templateMapper.selectOne(new LambdaQueryWrapper<MpTemplate>()
                    .eq(MpTemplate::getAccountId, accountId)
                    .eq(MpTemplate::getWxTemplateId, templateId).last("limit 1"));
            if (exists == null) {
                exists = new MpTemplate();
                exists.setAccountId(accountId);
                exists.setWxTemplateId(templateId);
                exists.setTenantId(AuthContext.require().getTenantId());
                templateMapper.insert(exists);
            }
            exists.setTitle(item.path("title").asText());
            exists.setContent(item.path("content").asText());
            exists.setExample(item.path("example").asText());
            exists.setPrimaryIndustry(item.path("primary_industry").asText());
            exists.setSecondaryIndustry(item.path("secondary_industry").asText());
            templateMapper.updateById(exists);
            synced++;
        }
        return ApiResult.ok(Map.of("synced", synced));
    }

    @GetMapping("/menu/current")
    @RequirePerm("mp:view")
    public ApiResult<List<SysMenu>> menuCurrent() {
        return ApiResult.ok(menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getEndType, "MP").orderByAsc(SysMenu::getSortNo)));
    }

    @PostMapping("/menu/publish")
    @RequirePerm("mp:update")
    public ApiResult<Map<String, Object>> publishMenu(@RequestParam Long accountId) {
        MpAccount account = requireAccount(accountId);
        List<SysMenu> flat = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getEndType, "MP").eq(SysMenu::getVisible, 1).orderByAsc(SysMenu::getSortNo));
        List<SysMenu> top = flat.stream().filter(m -> m.getParentId() == 0).toList();
        if (top.isEmpty()) {
            throw new BizException("请先在菜单可视化中配置公众号菜单");
        }
        if (top.size() > 3) {
            throw new BizException("公众号一级菜单最多 3 个");
        }
        List<Map<String, Object>> buttons = new ArrayList<>();
        for (SysMenu root : top) {
            Map<String, Object> button = new LinkedHashMap<>();
            button.put("name", root.getTitle());
            List<SysMenu> children = flat.stream().filter(m -> m.getParentId().equals(root.getId())).toList();
            if (children.isEmpty()) {
                button.put("type", "view");
                button.put("url", root.getPath() == null ? "https://example.com" : root.getPath());
            } else {
                List<Map<String, Object>> subs = new ArrayList<>();
                for (SysMenu child : children) {
                    subs.add(Map.of("type", "view", "name", child.getTitle(),
                            "url", child.getPath() == null ? "https://example.com" : child.getPath()));
                }
                button.put("sub_button", subs);
            }
            buttons.add(button);
        }
        weChatClient.post(account, "menu/create", Map.of("button", buttons));
        return ApiResult.ok(Map.of("success", true,
                "message", "菜单已发布，需重新进入公众号后可见（微信客户端有缓存）"));
    }

    @PostMapping("/menu/clear")
    @RequirePerm("mp:update")
    public ApiResult<Void> clearMenu(@RequestParam Long accountId) {
        weChatClient.get(requireAccount(accountId), "menu/delete");
        return ApiResult.ok();
    }

    @GetMapping("/auto-replies")
    @RequirePerm("mp:view")
    public ApiResult<List<MpAutoReply>> autoReplies(@RequestParam(required = false) Long accountId,
                                                    @RequestParam(required = false) String scene) {
        return ApiResult.ok(autoReplyMapper.selectList(new LambdaQueryWrapper<MpAutoReply>()
                .eq(accountId != null, MpAutoReply::getAccountId, accountId)
                .eq(scene != null && !scene.isBlank(), MpAutoReply::getScene, scene)
                .orderByAsc(MpAutoReply::getId)));
    }

    @PostMapping("/auto-replies")
    @RequirePerm("mp:update")
    public ApiResult<MpAutoReply> createAutoReply(@RequestBody MpAutoReply reply) {
        if (reply.getScene() == null) {
            throw new BizException("请选择回复场景");
        }
        if ("KEYWORD".equals(reply.getScene()) && (reply.getKeyword() == null || reply.getKeyword().isBlank())) {
            throw new BizException("关键词回复必须填写关键词");
        }
        reply.setId(null);
        reply.setAccountId(resolveAccountId(reply.getAccountId()));
        reply.setTenantId(AuthContext.require().getTenantId());
        reply.setEnabled(reply.getEnabled() == null ? 1 : reply.getEnabled());
        autoReplyMapper.insert(reply);
        return ApiResult.ok(reply);
    }

    @PutMapping("/auto-replies/{id}")
    @RequirePerm("mp:update")
    public ApiResult<Void> updateAutoReply(@PathVariable Long id, @RequestBody MpAutoReply reply) {
        reply.setId(id);
        reply.setAccountId(resolveAccountId(reply.getAccountId()));
        autoReplyMapper.updateById(reply);
        return ApiResult.ok();
    }

    @DeleteMapping("/auto-replies/{id}")
    @RequirePerm("mp:update")
    public ApiResult<Void> deleteAutoReply(@PathVariable Long id) {
        autoReplyMapper.deleteById(id);
        return ApiResult.ok();
    }

    private void upsertFan(Long accountId, JsonNode user) {
        String openid = user.path("openid").asText();
        MpFans fan = fansMapper.selectOne(new LambdaQueryWrapper<MpFans>()
                .eq(MpFans::getAccountId, accountId).eq(MpFans::getOpenid, openid).last("limit 1"));
        if (fan == null) {
            fan = new MpFans();
            fan.setAccountId(accountId);
            fan.setTenantId(AuthContext.require().getTenantId());
            fan.setOpenid(openid);
            fan.setSubscribeStatus(1);
            fan.setSubscribeTime(LocalDateTime.now());
            fan.setSyncedAt(LocalDateTime.now());
            fan.setNickname(user.path("nickname").asText());
            fan.setAvatar(user.path("headimgurl").asText());
            fan.setGender(user.path("sex").asInt());
            fan.setRegion(user.path("country").asText() + " " + user.path("province").asText()
                    + " " + user.path("city").asText());
            fansMapper.insert(fan);
        } else {
            fan.setNickname(user.path("nickname").asText());
            fan.setAvatar(user.path("headimgurl").asText());
            fan.setSyncedAt(LocalDateTime.now());
            fansMapper.updateById(fan);
        }
    }

    private MpAccount requireAccount(Long accountId) {
        MpAccount account = accountMapper.selectById(accountId);
        if (account == null) {
            throw BizException.notFound("公众号账号不存在");
        }
        return account;
    }

    private Long resolveAccountId(Long accountId) {
        if (accountId != null) {
            return accountId;
        }
        MpAccount account = accountMapper.selectOne(new LambdaQueryWrapper<MpAccount>()
                .orderByAsc(MpAccount::getId).last("limit 1"));
        if (account == null) {
            throw new BizException("请先配置公众号账号");
        }
        return account.getId();
    }
}
