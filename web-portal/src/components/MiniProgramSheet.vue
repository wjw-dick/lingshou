<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { miniProgramQr } from '@/lib/assets'
import { useLang } from '@/i18n'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ (event: 'close'): void }>()
const { t } = useLang()
const qrFailed = ref(false)

const qr = computed(() =>
  miniProgramQr(`${window.location.origin}/h5/deals`)
)

watch(
  () => props.open,
  (value) => {
    document.body.style.overflow = value ? 'hidden' : ''
  }
)

onUnmounted(() => {
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <Transition name="sheet">
      <div v-if="open" class="sheet-mask" @click.self="emit('close')">
        <div class="sheet-panel" role="dialog" aria-modal="true" aria-label="小程序下单">
          <span class="sheet-panel__handle"></span>
          <div class="sheet-panel__head">
            <p class="eyebrow">WECHAT MINI PROGRAM</p>
            <h2>{{ t('common.miniProgram') }}</h2>
          </div>
          <div class="sheet-panel__body">
            <div class="sheet-panel__qr">
              <img
                v-if="!qrFailed"
                :src="qr"
                alt="悦享便利小程序二维码"
                @error="qrFailed = true"
              />
              <span v-else class="sheet-panel__qr-fallback">悦享便利</span>
            </div>
            <p class="sheet-panel__desc">{{ t('h5.orderTips') }}</p>
            <p class="sheet-panel__note">{{ t('products.miniNote') }}</p>
          </div>
          <button type="button" class="btn btn--block" @click="emit('close')">
            {{ t('common.close') }}
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.sheet-panel__handle {
  display: block;
  width: 36px;
  height: 4px;
  border-radius: 999px;
  background: rgba(16, 37, 29, 0.14);
  margin: -8px auto 18px;
}

.sheet-panel__head h2 {
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 400;
  margin-top: 10px;
}

.sheet-panel__body {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 22px 0 24px;
}

.sheet-panel__qr {
  width: 168px;
  height: 168px;
  border-radius: 16px;
  background: var(--white);
  border: 1px solid var(--line);
  padding: 12px;
  display: grid;
  place-items: center;
}

.sheet-panel__qr img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.sheet-panel__qr-fallback {
  font-family: var(--font-display);
  font-size: 18px;
  color: var(--ink);
}

.sheet-panel__desc {
  margin-top: 18px;
  font-size: 13px;
  color: var(--ink-soft);
  max-width: 300px;
  line-height: 1.7;
}

.sheet-panel__note {
  margin-top: 6px;
  font-size: 11px;
  color: var(--muted);
}

.sheet-enter-active,
.sheet-leave-active {
  transition: opacity 0.3s ease;
}

.sheet-enter-active .sheet-panel,
.sheet-leave-active .sheet-panel {
  transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

.sheet-enter-from,
.sheet-leave-to {
  opacity: 0;
}

.sheet-enter-from .sheet-panel,
.sheet-leave-to .sheet-panel {
  transform: translateY(100%);
}
</style>
