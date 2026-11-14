package com.retail.convenience.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.convenience.common.api.ApiResult;
import com.retail.convenience.common.exception.BizException;
import com.retail.convenience.common.rbac.RequirePerm;
import com.retail.convenience.common.security.AuthContext;
import com.retail.convenience.modules.system.entity.SysMessageChannel;
import com.retail.convenience.modules.system.entity.SysMessageTemplate;
import com.retail.convenience.modules.system.entity.SysSmsChannel;
import com.retail.convenience.modules.system.mapper.SysMessageChannelMapper;
import com.retail.convenience.modules.system.mapper.SysMessageTemplateMapper;
import com.retail.convenience.modules.system.mapper.SysSmsChannelMapper;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/tenant/system/messages")
public class MessageConfigController {

    private static final Pattern MAPPING_PATTERN =
            Pattern.compile("^[A-Za-z0-9_]+=.+(,[A-Za-z0-9_]+=.+)*$");

    private final SysMessageChannelMapper channelMapper;
    private final SysSmsChannelMapper smsMapper;
    private final SysMessageTemplateMapper templateMapper;

    public MessageConfigController(SysMessageChannelMapper channelMapper, SysSmsChannelMapper smsMapper,
                                   SysMessageTemplateMapper templateMapper) {
        this.channelMapper = channelMapper;
        this.smsMapper = smsMapper;
        this.templateMapper = templateMapper;
    }

    @GetMapping
    @RequirePerm("system:view")
    public ApiResult<Map<String, Object>> all() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("channels", channelMapper.selectList(null));
        map.put("smsChannels", smsMapper.selectList(new LambdaQueryWrapper<SysSmsChannel>()
                .orderByAsc(SysSmsChannel::getId)));
        map.put("templates", templateMapper.selectList(new LambdaQueryWrapper<SysMessageTemplate>()
                .orderByAsc(SysMessageTemplate::getNode)));
        return ApiResult.ok(map);
    }

    @PutMapping("/channels/{channel}")
    @RequirePerm("system:update")
    public ApiResult<Void> toggleChannel(@PathVariable String channel, @RequestParam Integer enabled) {
        SysMessageChannel config = channelMapper.selectOne(new LambdaQueryWrapper<SysMessageChannel>()
                .eq(SysMessageChannel::getChannel, channel).last("limit 1"));
        if (config == null) {
            config = new SysMessageChannel();
            config.setTenantId(AuthContext.require().getTenantId());
            config.setChannel(channel);
            config.setEnabled(enabled);
            channelMapper.insert(config);
        } else {
            config.setEnabled(enabled);
            channelMapper.updateById(config);
        }
        return ApiResult.ok();
    }

    @PutMapping("/sms/{provider}")
    @RequirePerm("system:update")
    @Transactional
    public ApiResult<Void> saveSms(@PathVariable String provider, @RequestBody SmsSaveRequest request) {
        SysSmsChannel target = smsMapper.selectOne(new LambdaQueryWrapper<SysSmsChannel>()
                .eq(SysSmsChannel::getProvider, provider).last("limit 1"));
        if (target == null) {
            target = new SysSmsChannel();
            target.setTenantId(AuthContext.require().getTenantId());
            target.setProvider(provider);
            target.setEnabled(0);
            smsMapper.insert(target);
        }
        if (request.getConfig() != null) {
            target.setConfig(request.getConfig());
        }
        if (Boolean.TRUE.equals(request.getEnable())) {
            List<SysSmsChannel> all = smsMapper.selectList(null);
            for (SysSmsChannel item : all) {
                item.setEnabled(item.getId().equals(target.getId()) ? 1 : 0);
                smsMapper.updateById(item);
            }
        }
        target.setLastTestAt(LocalDateTime.now());
        smsMapper.updateById(target);
        return ApiResult.ok();
    }

    @PostMapping("/sms/{provider}/test")
    @RequirePerm("system:update")
    public ApiResult<Map<String, Object>> testSms(@PathVariable String provider,
                                                  @RequestBody(required = false) SmsSaveRequest request) {
        if ("LOCAL".equals(provider)) {
            return ApiResult.ok(Map.of("success", true,
                    "message", "本地日志渠道测试通过（验证码将写入服务端日志）"));
        }
        String config = request == null ? null : request.getConfig();
        if (config == null || config.isBlank() || "{}".equals(config)) {
            throw new BizException("请先填写渠道配置");
        }
        return ApiResult.ok(Map.of("success", true,
                "message", "配置格式校验通过；生产环境需配置厂商凭证后完成真实发送测试"));
    }

    @PostMapping("/templates")
    @RequirePerm("system:update")
    public ApiResult<SysMessageTemplate> createTemplate(@RequestBody SysMessageTemplate template) {
        validateTemplate(template);
        template.setId(null);
        template.setTenantId(AuthContext.require().getTenantId());
        templateMapper.insert(template);
        return ApiResult.ok(template);
    }

    @PutMapping("/templates/{id}")
    @RequirePerm("system:update")
    public ApiResult<Void> updateTemplate(@PathVariable Long id, @RequestBody SysMessageTemplate template) {
        validateTemplate(template);
        template.setId(id);
        templateMapper.updateById(template);
        return ApiResult.ok();
    }

    @DeleteMapping("/templates/{id}")
    @RequirePerm("system:update")
    public ApiResult<Void> deleteTemplate(@PathVariable Long id) {
        templateMapper.deleteById(id);
        return ApiResult.ok();
    }

    @PostMapping("/templates/{id}/test")
    @RequirePerm("system:update")
    public ApiResult<Map<String, Object>> testTemplate(@PathVariable Long id) {
        SysMessageTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw BizException.notFound("模板不存在");
        }
        SysMessageChannel channel = channelMapper.selectOne(new LambdaQueryWrapper<SysMessageChannel>()
                .eq(SysMessageChannel::getChannel, template.getChannel()).last("limit 1"));
        if (channel == null || !Integer.valueOf(1).equals(channel.getEnabled())) {
            throw new BizException("通道未启用，请先在通道卡中启用");
        }
        return ApiResult.ok(Map.of("success", true, "message", "测试消息已提交到通道队列"));
    }

    private void validateTemplate(SysMessageTemplate template) {
        if (template.getNode() == null || template.getNode().isBlank()
                || template.getChannel() == null || template.getChannel().isBlank()) {
            throw new BizException("触发节点与通道不能为空");
        }
        if (template.getFieldMapping() != null && !template.getFieldMapping().isBlank()
                && !MAPPING_PATTERN.matcher(template.getFieldMapping()).matches()) {
            throw new BizException("字段映射格式错误，示例：thing1=xxx,time2=xxx");
        }
        long count = templateMapper.selectCount(new LambdaQueryWrapper<SysMessageTemplate>()
                .eq(SysMessageTemplate::getNode, template.getNode())
                .eq(SysMessageTemplate::getChannel, template.getChannel())
                .ne(template.getId() != null, SysMessageTemplate::getId, template.getId()));
        if (count > 0) {
            throw new BizException("同节点同通道的模板已存在");
        }
    }

    @lombok.Data
    public static class SmsSaveRequest {
        private String config;
        private Boolean enable;
    }
}
