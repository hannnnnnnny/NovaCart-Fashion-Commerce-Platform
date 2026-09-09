<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { offerApi } from '../api/endpoints'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { apiError } from '../api/client'
import { formatPrice, formatRelative } from '../utils/format'
import DataState from '../components/DataState.vue'

const { t, locale } = useI18n()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()

const tab = ref('received')
const received = ref([])
const sent = ref([])
const loading = ref(false)
const error = ref('')
const acting = ref(false)
const counterFor = ref(null)
const counterAmount = ref('')
const counterMessage = ref('')

async function refresh() {
  loading.value = true
  error.value = ''
  try {
    const [r, s] = await Promise.all([
      offerApi.received({ page: 0, size: 50 }),
      offerApi.sent({ page: 0, size: 50 })
    ])
    received.value = r.content || []
    sent.value = s.content || []
  } catch (err) { error.value = apiError(err) } finally { loading.value = false }
}

// Wraps an offer action with a busy flag so double-clicks can't double-submit.
async function act(fn, successKey) {
  if (acting.value) return
  acting.value = true
  try { await fn(); toast.success(t(successKey)); await refresh() }
  catch (err) { toast.error(apiError(err)) }
  finally { acting.value = false }
}

function accept(offer) { act(() => offerApi.accept(offer.id), 'offerStatus.ACCEPTED') }
function reject(offer) {
  if (!confirm(t('offer.confirmReject'))) return
  act(() => offerApi.reject(offer.id), 'offerStatus.REJECTED')
}
function withdraw(offer) {
  if (!confirm(t('offer.confirmWithdraw'))) return
  act(() => offerApi.withdraw(offer.id), 'offerStatus.WITHDRAWN')
}
function acceptCounter(offer) { act(() => offerApi.acceptCounter(offer.id), 'offerStatus.ACCEPTED') }

function openCounter(offer) {
  counterFor.value = offer
  counterAmount.value = ''
  counterMessage.value = ''
}
async function submitCounter() {
  const amount = Number(counterAmount.value)
  if (!counterAmount.value || Number.isNaN(amount) || amount <= 0) {
    toast.error(t('offer.invalidAmount'))
    return
  }
  if (acting.value) return
  acting.value = true
  try {
    await offerApi.counter(counterFor.value.id, { amount, message: counterMessage.value })
    counterFor.value = null
    toast.success(t('offer.sent'))
    await refresh()
  } catch (err) { toast.error(apiError(err)) } finally { acting.value = false }
}

function checkoutWithOffer(offer) {
  router.push({ name: 'checkout', params: { listingId: offer.listingId }, query: { offerId: offer.id } })
}

onMounted(refresh)
</script>

<template>
  <main class="page">
    <div class="container">
      <h1 style="margin-bottom: 24px">{{ t('common.offers') }}</h1>
      <div class="tabs">
        <button class="tab" :class="{ 'is-active': tab === 'received' }" @click="tab = 'received'" type="button">{{ t('offer.receivedTab') }} ({{ received.length }})</button>
        <button class="tab" :class="{ 'is-active': tab === 'sent' }" @click="tab = 'sent'" type="button">{{ t('offer.sentTab') }} ({{ sent.length }})</button>
      </div>

      <DataState :loading="loading" :error="error" @retry="refresh">
      <div class="stack">
        <template v-if="tab === 'received'">
          <div v-if="received.length === 0" class="empty-state">{{ t('offer.noOffers') }}</div>
          <div v-for="o in received" :key="o.id" class="offer-card">
            <div class="thumb" :style="{ backgroundImage: o.listingCoverImageUrl ? `url('${o.listingCoverImageUrl}')` : '' }"></div>
            <div class="grow">
              <RouterLink :to="{ name: 'listing-detail', params: { id: o.listingId } }" class="bold">{{ o.listingTitle }}</RouterLink>
              <div class="row" style="margin-top:4px">
                <span class="amount">{{ formatPrice(o.amount) }}</span>
                <span class="badge" :class="{
                  'badge-success': o.status === 'ACCEPTED',
                  'badge-warning': o.status === 'PENDING' || o.status === 'COUNTERED',
                  'badge-danger': o.status === 'REJECTED' || o.status === 'WITHDRAWN'
                }">{{ t(`offerStatus.${o.status}`) }}</span>
                <span v-if="o.fromSeller" class="badge badge-info">{{ t('offer.counterTitle') }}</span>
              </div>
              <div class="soft">{{ t('common.buyer') }}: {{ o.buyer.displayName }} · {{ formatRelative(o.createdAt, locale) }}</div>
              <div v-if="o.message" class="soft" style="margin-top:4px">"{{ o.message }}"</div>
            </div>
            <div class="actions" style="min-width: 160px" v-if="!o.fromSeller">
              <template v-if="o.status === 'PENDING'">
                <button class="btn btn-primary btn-sm" @click="accept(o)" type="button">{{ t('offer.accept') }}</button>
                <button class="btn btn-outline btn-sm" @click="openCounter(o)" type="button">{{ t('offer.counter') }}</button>
                <button class="btn btn-ghost btn-sm" @click="reject(o)" type="button">{{ t('offer.reject') }}</button>
              </template>
            </div>
            <div class="actions" style="min-width: 160px" v-else>
              <button v-if="o.status === 'PENDING'" class="btn btn-ghost btn-sm" @click="withdraw(o)" type="button">{{ t('offer.withdraw') }}</button>
            </div>
          </div>
        </template>

        <template v-else>
          <div v-if="sent.length === 0" class="empty-state">{{ t('offer.noOffers') }}</div>
          <div v-for="o in sent" :key="o.id" class="offer-card">
            <div class="thumb" :style="{ backgroundImage: o.listingCoverImageUrl ? `url('${o.listingCoverImageUrl}')` : '' }"></div>
            <div class="grow">
              <RouterLink :to="{ name: 'listing-detail', params: { id: o.listingId } }" class="bold">{{ o.listingTitle }}</RouterLink>
              <div class="row" style="margin-top:4px">
                <span class="amount">{{ formatPrice(o.amount) }}</span>
                <span class="badge" :class="{
                  'badge-success': o.status === 'ACCEPTED',
                  'badge-warning': o.status === 'PENDING' || o.status === 'COUNTERED',
                  'badge-danger': o.status === 'REJECTED' || o.status === 'WITHDRAWN'
                }">{{ t(`offerStatus.${o.status}`) }}</span>
                <span v-if="o.fromSeller" class="badge badge-info">{{ t('offer.counterTitle') }}</span>
              </div>
              <div class="soft">{{ t('offer.seller') }}: {{ o.seller.displayName }} · {{ formatRelative(o.createdAt, locale) }}</div>
              <div v-if="o.message" class="soft" style="margin-top:4px">"{{ o.message }}"</div>
            </div>
            <div class="actions" style="min-width: 160px">
              <button v-if="o.status === 'PENDING' && o.fromSeller" class="btn btn-primary btn-sm" @click="acceptCounter(o)" type="button">{{ t('offer.acceptCounter') }}</button>
              <button v-if="o.status === 'PENDING' && !o.fromSeller" class="btn btn-ghost btn-sm" @click="withdraw(o)" type="button">{{ t('offer.withdraw') }}</button>
              <button v-if="o.status === 'ACCEPTED'" class="btn btn-accent btn-sm" @click="checkoutWithOffer(o)" type="button">{{ t('offer.checkout') }}</button>
            </div>
          </div>
        </template>
      </div>
      </DataState>

      <div v-if="counterFor" class="modal-overlay" @click.self="counterFor = null">
        <div class="modal">
          <h3 style="margin-bottom: 12px">{{ t('offer.counterTitle') }}</h3>
          <div class="muted" style="margin-bottom:12px">{{ counterFor.listingTitle }} — {{ formatPrice(counterFor.amount) }}</div>
          <div class="field">
            <label class="label">{{ t('offer.amount') }}</label>
            <input class="input" type="number" v-model="counterAmount" step="0.01" min="0.01" />
          </div>
          <div class="field">
            <label class="label">{{ t('offer.message') }}</label>
            <textarea class="textarea" v-model="counterMessage" maxlength="500"></textarea>
          </div>
          <div class="row" style="justify-content:flex-end">
            <button class="btn btn-ghost" @click="counterFor = null" type="button">{{ t('common.cancel') }}</button>
            <button class="btn btn-primary" @click="submitCounter" type="button">{{ t('common.send') }}</button>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>
