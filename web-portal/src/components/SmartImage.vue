<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    src?: string | null
    alt?: string
    ratio?: string
    art?: string
    label?: string
    tone?: 'green' | 'cream' | 'tomato'
    eager?: boolean
  }>(),
  {
    src: null,
    alt: '',
    ratio: '4 / 3',
    art: '悦',
    label: '',
    tone: 'green',
    eager: false
  }
)

const failed = ref(false)
const loaded = ref(false)

const usable = computed(() => {
  const value = props.src
  if (!value) return false
  return /^https?:\/\//i.test(value) || value.startsWith('/uploads/')
})

watch(
  () => props.src,
  () => {
    failed.value = false
    loaded.value = false
  }
)
</script>

<template>
  <div class="smart-image" :class="[`is-${tone}`, { 'is-loaded': loaded }]" :style="{ aspectRatio: ratio }">
    <img
      v-if="usable && !failed"
      :src="src || undefined"
      :alt="alt"
      :loading="eager ? 'eager' : 'lazy'"
      decoding="async"
      @load="loaded = true"
      @error="failed = true"
    />
    <div v-if="!usable || failed" class="smart-image__fallback" aria-hidden="true">
      <span class="smart-image__art">{{ art }}</span>
      <span v-if="label" class="smart-image__label">{{ label }}</span>
      <span class="smart-image__line"></span>
    </div>
  </div>
</template>

<style scoped>
.smart-image {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: var(--green);
  isolation: isolate;
}

.smart-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transform: scale(1.03);
  transition: opacity 0.6s ease, transform 1.2s cubic-bezier(0.22, 1, 0.36, 1);
}

.smart-image.is-loaded img {
  opacity: 1;
  transform: scale(1);
}

.smart-image__fallback {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 12%, rgba(195, 163, 106, 0.22), transparent 45%),
    radial-gradient(circle at 82% 86%, rgba(214, 82, 47, 0.18), transparent 48%),
    linear-gradient(140deg, #1c4a3a 0%, #10251d 58%, #0b1c16 100%);
}

.smart-image.is-cream .smart-image__fallback {
  background:
    radial-gradient(circle at 20% 20%, rgba(195, 163, 106, 0.28), transparent 45%),
    linear-gradient(145deg, #efe6d5, #e3d6bf);
  color: rgba(16, 37, 29, 0.26);
}

.smart-image.is-tomato .smart-image__fallback {
  background:
    radial-gradient(circle at 78% 18%, rgba(246, 240, 228, 0.16), transparent 42%),
    linear-gradient(145deg, #d6522f, #8f3018);
}

.smart-image__fallback::before {
  content: "";
  position: absolute;
  inset: 14px;
  border: 1px solid rgba(246, 240, 228, 0.16);
  border-radius: 50%;
  transform: rotate(-18deg) scaleY(0.6);
}

.smart-image__fallback::after {
  content: "";
  position: absolute;
  width: 72%;
  height: 72%;
  border-radius: 50%;
  border: 1px solid rgba(246, 240, 228, 0.09);
}

.smart-image__art {
  position: relative;
  z-index: 1;
  font-family: var(--font-display);
  font-size: clamp(48px, 10vw, 96px);
  line-height: 1;
  color: rgba(246, 240, 228, 0.28);
  letter-spacing: -0.04em;
}

.smart-image.is-cream .smart-image__art {
  color: rgba(16, 37, 29, 0.1);
}

.smart-image__label {
  position: relative;
  z-index: 1;
  font-size: 10px;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: rgba(246, 240, 228, 0.5);
}

.smart-image.is-cream .smart-image__label {
  color: rgba(16, 37, 29, 0.35);
}

.smart-image__line {
  position: relative;
  z-index: 1;
  width: 34px;
  height: 1px;
  background: rgba(246, 240, 228, 0.28);
  margin-top: 4px;
}
</style>
