<script setup>
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { orderApi } from '../api/endpoints'
import { useToastStore } from '../stores/toast'
import { apiError } from '../api/client'
import { formatPrice, formatRelative } from '../utils/format'
import DataState from '../components/DataState.vue'

const { t, locale } = useI18n()
const router = useRouter()
const toast = useToastStore()

const tab = ref('buying')
const orders = ref([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = tab.value === 'buying'
      ? await orderApi.buying({ page: 0, size: 50 })
      : await orderApi.selling({ page: 0, size: 50 })
    orders.value = result.content || []
  } catch (err) { error.value = apiError(err) } finally { loading.value = false }
}

function statusBadgeClass(status) {
  if (['COMPLETED', 'DELIVERED'].includes(status)) return 'badge-success'
  if (status === 'CANCELLED') return 'badge-danger'
  if (status === 'PENDING_PAYMENT') return 'badge-warning'
  return 'badge-info'
}

watch(tab, load)
onMounted(load)
</script>

<template>
  <main class="page">
    <div class="container">
      <h1 style="margin-bottom: 24px">{{ t('orders.title') }}</h1>
      <div class="tabs">
        <button class="tab" :class="{ 'is-active': tab === 'buying' }" @click="tab = 'buying'" type="button">{{ t('orders.tabBuying') }}</button>
        <button class="tab" :class="{ 'is-active': tab === 'selling' }" @click="tab = 'selling'" type="button">{{ t('orders.tabSelling') }}</button>
      </div>

      <DataState :loading="loading" :error="error" :empty="orders.length === 0" :empty-text="t('orders.empty')" @retry="load">
      <div class="stack">
        <RouterLink v-for="o in orders" :key="o.id" class="offer-card" style="cursor: pointer; color: inherit"
                    :to="{ name: 'order-detail', params: { id: o.id } }">
          <div class="thumb" :style="{ backgroundImage: o.listingCoverImageUrl ? `url('${o.listingCoverImageUrl}')` : '' }"></div>
          <div class="grow">
            <div class="row" style="justify-content: space-between">
              <div class="bold">{{ o.listingTitle }}</div>
              <span class="badge" :class="statusBadgeClass(o.status)">{{ t(`orderStatus.${o.status}`) }}</span>
            </div>
            <div class="soft">{{ t('orders.orderNumber') }}{{ o.orderNumber }} · {{ formatRelative(o.createdAt, locale) }}</div>
            <div class="soft">
              <span v-if="tab === 'buying'">{{ t('common.seller') }}: {{ o.seller.displayName }}</span>
              <span v-else>{{ t('common.buyer') }}: {{ o.buyer.displayName }}</span>
            </div>
          </div>
          <div class="text-right">
            <div class="amount" style="font-family: var(--font-display); font-weight: 700; font-size: 22px">{{ formatPrice(o.totalAmount) }}</div>
            <div class="soft">{{ t('common.total') }}</div>
          </div>
        </RouterLink>
      </div>
      </DataState>
    </div>
  </main>
</template>
