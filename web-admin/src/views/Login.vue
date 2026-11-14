<template>
  <div class="login-page">
    <div class="brand-panel">
      <div class="brand-top">
        <div class="brand-mark">悦</div>
        <div>
          <strong>悦享便利</strong>
          <span>RETAIL CONVENIENCE SAAS</span>
        </div>
      </div>
      <div class="brand-copy">
        <h1>让每一家便利店<br />都拥有数字化底座</h1>
        <p>总部、区域、门店、供应商、加盟商与消费者六方协同，商品、订货、库存、收银、会员、结算全链路在线。</p>
        <div class="brand-stats">
          <div><strong>200+</strong><span>连锁门店</span></div>
          <div><strong>10</strong><span>业务终端</span></div>
          <div><strong>99.5%</strong><span>服务可用性</span></div>
        </div>
      </div>
      <div class="brand-foot">
        <span>多租户隔离</span><i></i><span>全链路审计</span><i></i><span>食品效期合规</span>
      </div>
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
    </div>

    <div class="form-panel">
      <div class="form-box">
        <div class="form-head">
          <h2>欢迎回来</h2>
          <p>登录零售便利连锁 SaaS 平台</p>
        </div>

        <el-segmented v-model="mode" :options="modeOptions" size="large" class="mode-switch" />

        <el-form :model="form" label-position="top" @submit.prevent="submit">
          <el-form-item v-if="mode === 'tenant'" label="租户编码">
            <el-input v-model="form.tenantCode" size="large" placeholder="例如 yxbl">
              <template #prefix><el-icon><OfficeBuilding /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="账号">
            <el-input v-model="form.username" size="large" placeholder="请输入登录账号" autocomplete="username">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="请输入密码"
              autocomplete="current-password"
              @keyup.enter="submit"
            >
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <div class="form-row">
            <el-checkbox v-model="remember">记住账号</el-checkbox>
            <span class="muted">忘记密码请联系管理员</span>
          </div>
          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="submit">
            登录{{ mode === 'platform' ? '平台运营端' : '租户后台' }}
          </el-button>
        </el-form>

        <div class="demo-box">
          <div class="demo-title">
            <el-icon><MagicStick /></el-icon>
            <span>演示账号</span>
            <em>点击自动填入</em>
          </div>
          <div class="demo-list">
            <button type="button" @click="fill('yxbl', 'admin')">
              <strong>租户管理员</strong><span>yxbl / admin / Admin@123</span>
            </button>
            <button type="button" @click="fill('', 'admin')">
              <strong>平台超管</strong><span>admin / Admin@123</span>
            </button>
            <button type="button" @click="fill('yxbl', 'dianzhang')">
              <strong>门店店长</strong><span>yxbl / dianzhang / Admin@123</span>
            </button>
          </div>
        </div>

        <div class="form-footer">© 2026 悦享便利 · 零售便利连锁 SaaS 平台 V1.0</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const mode = ref<'tenant' | 'platform'>('tenant')
const loading = ref(false)
const remember = ref(true)
const form = reactive({
  tenantCode: localStorage.getItem('retail_tenant_code') || 'yxbl',
  username: localStorage.getItem('retail_username') || 'admin',
  password: 'Admin@123'
})

const modeOptions = [
  { label: '租户后台', value: 'tenant' },
  { label: '平台运营端', value: 'platform' }
]

watch(mode, (value) => {
  if (value === 'platform') {
    form.username = 'admin'
    form.password = 'Admin@123'
  } else {
    form.tenantCode = 'yxbl'
    form.username = 'admin'
    form.password = 'Admin@123'
  }
})

function fill(tenantCode: string, username: string) {
  mode.value = tenantCode ? 'tenant' : 'platform'
  form.tenantCode = tenantCode || 'yxbl'
  form.username = username
  form.password = 'Admin@123'
}

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号与密码')
    return
  }
  loading.value = true
  try {
    if (remember.value) {
      localStorage.setItem('retail_tenant_code', form.tenantCode)
      localStorage.setItem('retail_username', form.username)
    }
    if (mode.value === 'platform') {
      await auth.loginPlatform({ username: form.username, password: form.password })
      ElMessage.success('登录成功')
      router.push('/platform/dashboard')
    } else {
      await auth.loginTenant({
        tenantCode: form.tenantCode || 'yxbl',
        username: form.username,
        password: form.password
      })
      ElMessage.success('登录成功')
      router.push('/dashboard')
    }
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100%;
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  background: #f6f8fc;
}

.brand-panel {
  position: relative;
  overflow: hidden;
  background: linear-gradient(150deg, #0b1426 0%, #122a52 55%, #0d3f4b 100%);
  color: #fff;
  padding: 44px 56px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 100vh;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 14px;
  position: relative;
  z-index: 2;

  .brand-mark {
    width: 44px;
    height: 44px;
    border-radius: 13px;
    background: linear-gradient(135deg, #2f7df6, #14b8a6);
    display: grid;
    place-items: center;
    font-weight: 700;
    font-size: 22px;
    box-shadow: 0 10px 24px rgba(47, 125, 246, 0.35);
  }

  strong { display: block; font-size: 18px; letter-spacing: 0.06em; }
  span { display: block; font-size: 10px; letter-spacing: 0.18em; color: #7f93b3; margin-top: 4px; }
}

.brand-copy {
  position: relative;
  z-index: 2;
  max-width: 560px;

  h1 {
    font-size: clamp(34px, 4.2vw, 54px);
    line-height: 1.18;
    margin: 0 0 22px;
    letter-spacing: -0.03em;
    font-weight: 650;
  }

  p {
    color: #a6b7d1;
    font-size: 15px;
    line-height: 1.9;
    margin: 0;
    max-width: 500px;
  }
}

.brand-stats {
  display: flex;
  gap: 44px;
  margin-top: 46px;

  div { display: flex; flex-direction: column; gap: 6px; }
  strong { font-size: 28px; font-weight: 650; color: #fff; }
  span { font-size: 12px; color: #7e92b0; }
}

.brand-foot {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #7e92b0;

  i { width: 3px; height: 3px; border-radius: 50%; background: #3b4f70; }
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.5;
}
.glow-1 { width: 380px; height: 380px; background: #1f6feb; right: -120px; top: 10%; }
.glow-2 { width: 300px; height: 300px; background: #0d9488; left: -80px; bottom: -60px; }

.form-panel {
  display: grid;
  place-items: center;
  padding: 40px 24px;
}

.form-box {
  width: min(420px, 100%);
}

.form-head {
  margin-bottom: 24px;
  h2 { font-size: 28px; margin: 0; letter-spacing: -0.02em; }
  p { margin: 8px 0 0; color: var(--ink-3); font-size: 13.5px; }
}

.mode-switch {
  width: 100%;
  margin-bottom: 22px;
  --el-segmented-item-selected-bg-color: #1f6feb;
  --el-segmented-item-selected-color: #fff;
}

.form-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: -4px 0 18px;
  font-size: 12.5px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 9px;
  background: linear-gradient(90deg, #1f6feb, #2563eb);
  border: none;
  box-shadow: 0 10px 24px rgba(31, 111, 235, 0.26);
}

.demo-box {
  margin-top: 28px;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 14px;
  background: #fbfcfe;
}

.demo-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #475467;
  margin-bottom: 10px;
  em { margin-left: auto; font-style: normal; font-weight: 400; color: #98a2b3; }
}

.demo-list {
  display: grid;
  gap: 6px;

  button {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
    border: 1px solid transparent;
    background: #fff;
    border-radius: 7px;
    padding: 8px 10px;
    cursor: pointer;
    text-align: left;
    transition: all 0.15s;

    &:hover { border-color: #b9d3fa; background: #f5f9ff; }
    strong { font-size: 12px; color: #344054; }
    span { font-size: 11px; color: #98a2b3; }
  }
}

.form-footer {
  text-align: center;
  margin-top: 30px;
  font-size: 11.5px;
  color: #b0b8c4;
}

@media (max-width: 960px) {
  .login-page { grid-template-columns: 1fr; }
  .brand-panel { display: none; }
}
</style>
