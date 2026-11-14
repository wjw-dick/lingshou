<script setup lang="ts">
import { toastState } from '@/composables/useToast'
</script>

<template>
  <Transition name="toast">
    <div v-if="toastState.visible" class="toast-host" role="status">{{ toastState.text }}</div>
  </Transition>
</template>

<style scoped>
.toast-host {
  position: fixed;
  left: 50%;
  bottom: calc(32px + env(safe-area-inset-bottom));
  transform: translateX(-50%);
  z-index: 300;
  padding: 12px 20px;
  border-radius: 999px;
  background: rgba(16, 37, 29, 0.94);
  color: var(--cream);
  font-size: 13px;
  letter-spacing: 0.04em;
  box-shadow: 0 12px 40px rgba(16, 37, 29, 0.28);
  backdrop-filter: blur(10px);
  white-space: nowrap;
}

.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, 8px);
}
</style>
