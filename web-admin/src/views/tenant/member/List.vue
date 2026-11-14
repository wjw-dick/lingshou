<template>
  <div class="page">
    <PageHeader title="会员列表" subtitle="统一会员资产、积分、储值与优惠券运营视图。">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <div class="table-card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="昵称 / 手机号"
          clearable
          style="width: 220px"
          :prefix-icon="Search"
          @keyup.enter="search"
        />
        <el-select v-model="query.levelId" placeholder="会员等级" clearable style="width: 150px">
          <el-option v-for="level in levels" :key="level.id" :label="level.name" :value="level.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="会员状态" clearable style="width: 140px">
          <el-option label="正常" value="ACTIVE" />
          <el-option label="已停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted">共 {{ total }} 位会员</span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe :row-key="rowKey">
        <el-table-column label="会员" min-width="200">
          <template #default="{ row }">
            <div class="member-cell">
              <el-avatar :size="34" class="avatar">{{ initial(row.member.nickname) }}</el-avatar>
              <div>
                <strong>{{ row.member.nickname || '未命名会员' }}</strong>
                <span>{{ row.member.phoneMask || '未绑定手机' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="120">
          <template #default="{ row }">
            <el-tag effect="plain" round size="small">{{ row.levelName || '普通会员' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="积分" width="100" align="right">
          <template #default="{ row }">
            <span class="money">{{ number(row.member.points) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="储值余额" width="120" align="right">
          <template #default="{ row }">
            <span class="money">¥ {{ money(row.member.walletBalance) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="累计消费" width="130" align="right">
          <template #default="{ row }">
            <span class="money">¥ {{ money(row.member.totalConsume) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="订单数" width="90" align="right">
          <template #default="{ row }">{{ number(row.member.orderCount) }}</template>
        </el-table-column>
        <el-table-column label="标签" min-width="170">
          <template #default="{ row }">
            <div class="tag-list">
              <span v-for="tag in parseTags(row.member.tags)" :key="tag" class="tag-soft">{{ tag }}</span>
              <span v-if="!parseTags(row.member.tags).length" class="muted">-</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :status="row.member.status" /></template>
        </el-table-column>
        <el-table-column label="注册时间" width="150">
          <template #default="{ row }">{{ formatTime(row.member.registeredAt, 'YYYY-MM-DD') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDetail(row.member)">详情</el-button>
            <el-button text type="primary" size="small" @click="openEdit(row.member)">编辑</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无会员数据" :image-size="70" />
        </template>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50]"
          @current-change="load"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="会员详情" size="640px">
      <div v-loading="detailLoading" class="detail-wrap">
        <template v-if="detail">
          <div class="profile">
            <el-avatar :size="56" class="avatar avatar-lg">{{ initial(detail.member.nickname) }}</el-avatar>
            <div class="profile-main">
              <h3>{{ detail.member.nickname || '未命名会员' }}</h3>
              <p>ID {{ detail.member.id }} · {{ detail.member.phoneMask || '未绑定手机' }} · 注册于 {{ formatTime(detail.member.registeredAt, 'YYYY-MM-DD') }}</p>
            </div>
            <StatusTag :status="detail.member.status" />
          </div>

          <div class="asset-grid">
            <div class="asset-item">
              <span>当前积分</span>
              <strong>{{ number(detail.member.points) }}</strong>
            </div>
            <div class="asset-item">
              <span>储值余额</span>
              <strong>¥ {{ money(detail.member.walletBalance) }}</strong>
            </div>
            <div class="asset-item">
              <span>累计消费</span>
              <strong>¥ {{ money(detail.member.totalConsume) }}</strong>
            </div>
            <div class="asset-item">
              <span>累计订单</span>
              <strong>{{ number(detail.member.orderCount) }} 单</strong>
            </div>
          </div>

          <el-tabs v-model="detailTab">
            <el-tab-pane label="资产信息" name="asset">
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="会员等级">{{ levelName(detail.member.levelId) }}</el-descriptions-item>
                <el-descriptions-item label="生日">{{ detail.member.birthday || '-' }}</el-descriptions-item>
                <el-descriptions-item label="最近活跃">{{ formatTime(detail.member.lastActiveAt) }}</el-descriptions-item>
                <el-descriptions-item label="注册时间">{{ formatTime(detail.member.registeredAt) }}</el-descriptions-item>
                <el-descriptions-item label="标签" :span="2">
                  <div class="tag-list">
                    <span v-for="tag in parseTags(detail.member.tags)" :key="tag" class="tag-soft">{{ tag }}</span>
                    <span v-if="!parseTags(detail.member.tags).length" class="muted">暂无标签</span>
                  </div>
                </el-descriptions-item>
              </el-descriptions>
            </el-tab-pane>

            <el-tab-pane label="积分流水" name="points">
              <el-table :data="detail.pointFlows || []" stripe size="small" max-height="360">
                <el-table-column label="时间" width="140">
                  <template #default="{ row }">{{ formatTime(row.createdAt, 'MM-DD HH:mm') }}</template>
                </el-table-column>
                <el-table-column label="类型" width="110">
                  <template #default="{ row }">{{ pointBizLabel(row.bizType) }}</template>
                </el-table-column>
                <el-table-column label="变动" width="90" align="right">
                  <template #default="{ row }">
                    <span :class="row.changePoints >= 0 ? 'flow-plus' : 'flow-minus'">
                      {{ row.changePoints >= 0 ? '+' : '' }}{{ number(row.changePoints) }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="余额" width="90" align="right">
                  <template #default="{ row }">{{ number(row.balance) }}</template>
                </el-table-column>
                <el-table-column label="备注" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.remark || '-' }}</template>
                </el-table-column>
                <template #empty>
                  <el-empty description="暂无积分流水" :image-size="60" />
                </template>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="储值流水" name="wallet">
              <el-table :data="detail.walletFlows || []" stripe size="small" max-height="360">
                <el-table-column label="时间" width="140">
                  <template #default="{ row }">{{ formatTime(row.createdAt, 'MM-DD HH:mm') }}</template>
                </el-table-column>
                <el-table-column label="类型" width="110">
                  <template #default="{ row }">{{ walletBizLabel(row.bizType) }}</template>
                </el-table-column>
                <el-table-column label="变动" width="110" align="right">
                  <template #default="{ row }">
                    <span :class="row.changeAmount >= 0 ? 'flow-plus' : 'flow-minus'">
                      {{ row.changeAmount >= 0 ? '+' : '' }}¥ {{ money(row.changeAmount) }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="余额" width="110" align="right">
                  <template #default="{ row }">¥ {{ money(row.balance) }}</template>
                </el-table-column>
                <el-table-column label="备注" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.remark || '-' }}</template>
                </el-table-column>
                <template #empty>
                  <el-empty description="暂无储值流水" :image-size="60" />
                </template>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="优惠券" name="coupons">
              <el-table :data="detail.coupons || []" stripe size="small" max-height="360">
                <el-table-column label="优惠券" min-width="150">
                  <template #default="{ row }">{{ row.couponName }}</template>
                </el-table-column>
                <el-table-column label="类型" width="120">
                  <template #default="{ row }">
                    <el-tag effect="plain" size="small">{{ couponTypeLabel(row.type) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                  <template #default="{ row }"><StatusTag :status="row.memberCoupon.status" /></template>
                </el-table-column>
                <el-table-column label="领取时间" width="140">
                  <template #default="{ row }">{{ formatTime(row.memberCoupon.receivedAt, 'YYYY-MM-DD') }}</template>
                </el-table-column>
                <el-table-column label="使用时间" width="140">
                  <template #default="{ row }">{{ formatTime(row.memberCoupon.usedAt, 'YYYY-MM-DD') }}</template>
                </el-table-column>
                <template #empty>
                  <el-empty description="暂无优惠券" :image-size="60" />
                </template>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </template>
        <el-empty v-else-if="!detailLoading" description="会员信息加载失败" :image-size="70" />
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="primary" plain :icon="Coin" :disabled="!detail" @click="openPoints(detail.member)">调整积分</el-button>
          <el-button type="primary" plain :icon="Wallet" :disabled="!detail" @click="openRecharge(detail.member)">储值充值</el-button>
          <el-button type="primary" :disabled="!detail" @click="openEdit(detail.member)">编辑资料</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="editVisible" title="编辑会员" width="520px">
      <el-form ref="editRef" :model="editForm" :rules="editRules" label-width="90px">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" maxlength="32" placeholder="请输入会员昵称" />
        </el-form-item>
        <el-form-item label="会员等级" prop="levelId">
          <el-select v-model="editForm.levelId" placeholder="请选择会员等级" clearable style="width: 100%">
            <el-option v-for="level in levels" :key="level.id" :label="level.name" :value="level.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="生日" prop="birthday">
          <el-date-picker
            v-model="editForm.birthday"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择生日"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-select v-model="editForm.tags" multiple filterable allow-create default-first-option placeholder="选择或输入会员标签" style="width: 100%">
            <el-option v-for="tag in tagOptions" :key="tag.id" :label="tag.name" :value="tag.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editForm.status">
            <el-radio value="ACTIVE">正常</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pointsVisible" title="调整积分" width="460px">
      <el-form ref="pointsRef" :model="pointsForm" :rules="pointsRules" label-width="90px">
        <el-form-item label="调整积分" prop="points">
          <el-input-number v-model="pointsForm.points" :step="10" placeholder="正数增加，负数扣减" style="width: 100%" />
          <div class="form-tip">当前积分 {{ number(actionMember?.points) }}，正数增加、负数扣减。</div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="pointsForm.remark" type="textarea" :rows="2" maxlength="120" placeholder="选填，如：活动补发" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointsVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPoints">确认调整</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rechargeVisible" title="储值充值" width="480px">
      <el-form ref="rechargeRef" :model="rechargeForm" :rules="rechargeRules" label-width="90px">
        <el-form-item label="充值金额" prop="amount">
          <el-input-number v-model="rechargeForm.amount" :min="0.01" :precision="2" :step="50" style="width: 100%" />
        </el-form-item>
        <el-form-item label="赠送金额" prop="giftAmount">
          <el-input-number v-model="rechargeForm.giftAmount" :min="0" :precision="2" :step="10" style="width: 100%" />
          <div class="form-tip">当前余额 ¥ {{ money(actionMember?.walletBalance) }}，赠送金额将单独记入赠送流水。</div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="rechargeForm.remark" type="textarea" :rows="2" maxlength="120" placeholder="选填，如：门店充值活动" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRecharge">确认充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Coin, Refresh, Search, Wallet } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { api } from '@/api/http'
import dayjs from 'dayjs'

const rows = ref<any[]>([])
const levels = ref<any[]>([])
const tagOptions = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const query = reactive<{ keyword: string; levelId?: number; status?: string }>({
  keyword: '',
  levelId: undefined,
  status: undefined
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)
const detailTab = ref('asset')

const editVisible = ref(false)
const editRef = ref<any>()
const editForm = reactive<any>({ nickname: '', levelId: undefined, birthday: '', tags: [], status: 'ACTIVE' })
const editingId = ref<number>()

const pointsVisible = ref(false)
const pointsRef = ref<any>()
const pointsForm = reactive({ points: 100, remark: '' })
const actionMember = ref<any>(null)

const rechargeVisible = ref(false)
const rechargeRef = ref<any>()
const rechargeForm = reactive({ amount: 100, giftAmount: 0, remark: '' })
const actionMemberId = ref<number>()

const editRules = {
  nickname: [{ required: true, message: '请输入会员昵称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择会员状态', trigger: 'change' }]
}

const pointsRules = {
  points: [
    { required: true, message: '请输入调整积分', trigger: 'blur' },
    {
      validator: (_rule: any, value: any, callback: any) => {
        const n = Number(value)
        if (!Number.isInteger(n) || n === 0) {
          callback(new Error('积分需为非零整数'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const rechargeRules = {
  amount: [
    { required: true, message: '请输入充值金额', trigger: 'blur' },
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (!(Number(value) > 0)) {
          callback(new Error('充值金额必须大于 0'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  giftAmount: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (Number(value) < 0) {
          callback(new Error('赠送金额不能为负数'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  load()
  loadOptions()
})

async function loadOptions() {
  try {
    levels.value = await api.get('/tenant/members/levels')
  } catch {
    levels.value = []
  }
  try {
    tagOptions.value = await api.get('/tenant/members/tags')
  } catch {
    tagOptions.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const data = await api.get('/tenant/members', {
      page: page.value,
      size: size.value,
      keyword: query.keyword || undefined,
      levelId: query.levelId || undefined,
      status: query.status || undefined
    })
    rows.value = data?.records || []
    total.value = Number(data?.total || 0)
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  query.keyword = ''
  query.levelId = undefined
  query.status = undefined
  page.value = 1
  load()
}

function handleSizeChange() {
  page.value = 1
  load()
}

function rowKey(row: any) {
  return row.member.id
}

async function openDetail(member: any) {
  detailVisible.value = true
  detailTab.value = 'asset'
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await api.get(`/tenant/members/${member.id}`)
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

async function reloadDetail() {
  if (!detail.value) return
  try {
    detail.value = await api.get(`/tenant/members/${detail.value.member.id}`)
  } catch {
    // ignore
  }
}

function openEdit(member: any) {
  editingId.value = member.id
  editForm.nickname = member.nickname || ''
  editForm.levelId = member.levelId || undefined
  editForm.birthday = member.birthday || ''
  editForm.tags = parseTags(member.tags)
  editForm.status = member.status || 'ACTIVE'
  editVisible.value = true
  nextTick(() => editRef.value?.clearValidate())
}

function submitEdit() {
  editRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    saving.value = true
    try {
      await api.put(`/tenant/members/${editingId.value}`, {
        nickname: editForm.nickname,
        levelId: editForm.levelId,
        birthday: editForm.birthday || null,
        tags: JSON.stringify(editForm.tags || []),
        status: editForm.status
      })
      ElMessage.success('会员资料已更新')
      editVisible.value = false
      load()
      reloadDetail()
    } finally {
      saving.value = false
    }
  })
}

function openPoints(member: any) {
  actionMember.value = member
  actionMemberId.value = member.id
  pointsForm.points = 100
  pointsForm.remark = ''
  pointsVisible.value = true
  nextTick(() => pointsRef.value?.clearValidate())
}

function submitPoints() {
  pointsRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    saving.value = true
    try {
      await api.post(`/tenant/members/${actionMemberId.value}/points`, null, {
        params: { points: pointsForm.points, remark: pointsForm.remark || undefined }
      })
      ElMessage.success('积分调整成功')
      pointsVisible.value = false
      load()
      reloadDetail()
    } finally {
      saving.value = false
    }
  })
}

function openRecharge(member: any) {
  actionMember.value = member
  actionMemberId.value = member.id
  rechargeForm.amount = 100
  rechargeForm.giftAmount = 0
  rechargeForm.remark = ''
  rechargeVisible.value = true
  nextTick(() => rechargeRef.value?.clearValidate())
}

function submitRecharge() {
  rechargeRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    saving.value = true
    try {
      await api.post(`/tenant/members/${actionMemberId.value}/recharge`, null, {
        params: {
          amount: rechargeForm.amount,
          giftAmount: rechargeForm.giftAmount || undefined,
          remark: rechargeForm.remark || undefined
        }
      })
      ElMessage.success('充值成功')
      rechargeVisible.value = false
      load()
      reloadDetail()
    } finally {
      saving.value = false
    }
  })
}

function parseTags(value: any): string[] {
  if (!value) return []
  if (Array.isArray(value)) return value
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function levelName(levelId: any) {
  const level = levels.value.find((item) => item.id === levelId)
  return level ? level.name : '普通会员'
}

function pointBizLabel(type: string) {
  const map: Record<string, string> = {
    ADMIN: '后台调整',
    ORDER: '订单奖励',
    EXCHANGE: '积分兑换',
    REFUND: '退款回退'
  }
  return map[type] || type || '-'
}

function walletBizLabel(type: string) {
  const map: Record<string, string> = {
    RECHARGE: '充值',
    CONSUME: '消费',
    REFUND: '退款',
    GIFT: '充值赠送'
  }
  return map[type] || type || '-'
}

function couponTypeLabel(type: string) {
  const map: Record<string, string> = {
    FULL_REDUCTION: '满减券',
    DISCOUNT: '折扣券',
    PRODUCT: '商品券',
    FREIGHT: '运费券'
  }
  return map[type] || type || '-'
}

function initial(name: string) {
  return (name || '会').slice(0, 1)
}

function number(value: any) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function money(value: any) {
  return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(value: any, pattern = 'YYYY-MM-DD HH:mm') {
  return value ? dayjs(value).format(pattern) : '-'
}
</script>

<style scoped lang="scss">
.member-cell {
  display: flex;
  align-items: center;
  gap: 10px;

  .avatar {
    flex-shrink: 0;
    background: linear-gradient(135deg, #1f6feb, #14b8a6);
    color: #fff;
    font-size: 13px;
    font-weight: 600;
  }

  > div {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  strong {
    font-size: 12.5px;
    font-weight: 600;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  span {
    font-size: 11px;
    color: var(--ink-3);
  }
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.detail-wrap {
  min-height: 320px;
}

.profile {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 4px 0 16px;
  border-bottom: 1px dashed #eef1f5;

  .avatar-lg {
    background: linear-gradient(135deg, #1f6feb, #14b8a6);
    color: #fff;
    font-size: 20px;
    font-weight: 600;
  }

  .profile-main {
    flex: 1;
    min-width: 0;

    h3 {
      margin: 0;
      font-size: 17px;
      font-weight: 650;
    }

    p {
      margin: 5px 0 0;
      font-size: 12px;
      color: var(--ink-3);
    }
  }
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin: 16px 0 6px;
}

.asset-item {
  border: 1px solid var(--line);
  background: #fbfcfe;
  border-radius: 9px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;

  span {
    font-size: 11.5px;
    color: var(--ink-3);
  }

  strong {
    font-size: 18px;
    font-weight: 680;
    font-variant-numeric: tabular-nums;
  }
}

.flow-plus {
  color: #18a058;
  font-weight: 650;
}

.flow-minus {
  color: #d03050;
  font-weight: 650;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
