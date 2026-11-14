import { reactive } from 'vue'

export const toastState = reactive({ text: '', visible: false })
let timer: number | undefined

export function showToast(text: string) {
  toastState.text = text
  toastState.visible = true
  if (timer) window.clearTimeout(timer)
  timer = window.setTimeout(() => {
    toastState.visible = false
  }, 2200)
}

export function useToast() {
  return { toastState, showToast }
}
