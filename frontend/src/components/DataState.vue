<script setup>
import { useI18n } from 'vue-i18n'

/**
 * Uniform loading / error / empty / content wrapper so every data page shows
 * the right state instead of collapsing errors into "empty" (the audit found
 * failed fetches rendering as "Nothing here yet." once the toast faded).
 *
 * Priority: loading > error > empty > content (default slot).
 */
const props = defineProps({
  loading: { type: Boolean, default: false },
  error: { type: [String, Boolean], default: '' },
  empty: { type: Boolean, default: false },
  emptyText: { type: String, default: '' }
})
const emit = defineEmits(['retry'])
const { t } = useI18n()
</script>

<template>
  <div v-if="loading" class="data-state muted" role="status">{{ t('common.loading') }}</div>

  <div v-else-if="error" class="empty-state data-state-error" role="alert">
    <p style="margin-bottom: 12px">{{ typeof error === 'string' && error ? error : t('errors.generic') }}</p>
    <button class="btn btn-outline btn-sm" type="button" @click="emit('retry')">{{ t('common.retry') }}</button>
  </div>

  <div v-else-if="empty" class="empty-state">{{ emptyText || t('common.empty') }}</div>

  <slot v-else />
</template>
