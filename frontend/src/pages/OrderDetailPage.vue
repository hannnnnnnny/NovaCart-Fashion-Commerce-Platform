<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { orderApi, reviewApi } from '../api/endpoints'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { apiError } from '../api/client'
import { formatPrice, formatRelative, formatDate } from '../utils/format'
import Avatar from '../components/Avatar.vue'
import Stars from '../components/Stars.vue'
import StarInput from '../components/StarInput.vue'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()

const order = ref(null)
const reviews = ref([])
const loading = ref(true)
const shipForm = ref({ carrier: '', trackingNumber: '' })
const showShipModal = ref(false)
const showReviewModal = ref(false)
const reviewForm = ref({ rating: 5, comment: '' })

const role = computed(() => {
  if (!order.value || !auth.user) return null
  if (order.value.buyer.id === auth.user.id) return 'buyer'
  if (order.value.seller.id === auth.user.id) return 'seller'
  return null
})

const myReview = computed(() => {
  if (!order.value || !auth.user) return null
  return reviews.value.find((r) => r.reviewer.id === auth.user.id)
})

async function load() {
  loading.value = true
  try {
    order.value = await orderApi.get(route.params.id)
    reviews.value = await reviewApi.forOrder(order.value.id)
  } catch (err) { toast.error(apiError(err)); router.replace({ name: 'orders' }) }
  finally { loading.value = false }
}

async function pay() {
  try { order.value = await orderApi.pay(order.value.id); toast.success(t('orderStatus.PAID')) }
  catch (err) { toast.error(apiError(err)) }
}

async function ship() {
  if (!shipForm.value.carrier || !shipForm.value.trackingNumber) return
  try {
    order.value = await orderApi.ship(order.value.id, shipForm.value)
    showShipModal.value = false
    toast.success(t('orderStatus.SHIPPED'))
  } catch (err) { toast.error(apiError(err)) }
}

async function confirmReceipt() {
  if (!confirm(t('orders.confirmReceipt') + '?')) return
  try { order.value = await orderApi.confirmReceipt(order.value.id); toast.success(t('orderStatus.COMPLETED')) }
  catch (err) { toast.error(apiError(err)) }
}

async function cancel() {
  const reason = prompt(t('orders.cancelReason'))
  // prompt() returns null when the user dismisses it — don't cancel then.
  if (reason === null) return
  try { order.value = await orderApi.cancel(order.value.id, { reason }); toast.success(t('orderStatus.CANCELLED')) }
  catch (err) { toast.error(apiError(err)) }
}

async function submitReview() {
  try {
    await reviewApi.create({ orderId: order.value.id, rating: reviewForm.value.rating, comment: reviewForm.value.comment })
    showReviewModal.value = false
    reviewForm.value = { rating: 5, comment: '' }
    reviews.value = await reviewApi.forOrder(order.value.id)
    toast.success(t('common.save'))
  } catch (err) { toast.error(apiError(err)) }
}

onMounted(load)
</script>

<template>
  <main class="page">
    <div class="container" style="max-width: 900px" v-if="order">
      <div class="between" style="margin-bottom: 24px">
        <div>
          <h1>{{ order.listingTitle }}</h1>
          <div class="soft">{{ t('orders.orderNumber') }}{{ order.orderNumber }}</div>
        </div>
        <span class="badge" :class="{
          'badge-success': ['COMPLETED', 'DELIVERED'].includes(order.status),
          'badge-warning': ['PENDING_PAYMENT', 'PAID', 'SHIPPED'].includes(order.status),
          'badge-danger': order.status === 'CANCELLED'
        }">{{ t(`orderStatus.${order.status}`) }}</span>
      </div>

      <div class="grid grid-2">
        <section class="panel">
          <h3 style="margin-bottom: 16px">Timeline</h3>
          <div class="timeline">
            <div class="step" :class="{ done: !!order.createdAt }">
              <div class="dot"></div>
              <div><div class="bold">{{ t('orders.placedOn') }}</div><div class="when">{{ formatDate(order.createdAt, locale) }}</div></div>
            </div>
            <div class="step" :class="{ done: !!order.paidAt, active: order.status === 'PAID' }">
              <div class="dot"></div>
              <div><div class="bold">{{ t('orders.paidOn') }}</div><div class="when">{{ formatDate(order.paidAt, locale) || '—' }}</div></div>
            </div>
            <div class="step" :class="{ done: !!order.shippedAt, active: order.status === 'SHIPPED' }">
              <div class="dot"></div>
              <div>
                <div class="bold">{{ t('orders.shippedOn') }}</div>
                <div class="when">{{ formatDate(order.shippedAt, locale) || '—' }}</div>
                <div v-if="order.trackingNumber" class="soft">{{ order.carrier }} · {{ order.trackingNumber }}</div>
              </div>
            </div>
            <div class="step" :class="{ done: order.status === 'COMPLETED' }">
              <div class="dot"></div>
              <div><div class="bold">{{ t('orders.completedOn') }}</div><div class="when">{{ formatDate(order.completedAt, locale) || '—' }}</div></div>
            </div>
            <div v-if="order.status === 'CANCELLED'" class="step done">
              <div class="dot" style="background: var(--danger) !important"></div>
              <div>
                <div class="bold">{{ t('orders.cancelledOn') }}</div>
                <div class="when">{{ formatDate(order.cancelledAt, locale) }}</div>
                <div v-if="order.cancelReason" class="soft">{{ order.cancelReason }}</div>
              </div>
            </div>
          </div>

          <div class="divider"></div>

          <h3 style="margin-bottom: 12px">{{ t('checkout.shippingTo') }}</h3>
          <div class="soft"><span class="bold">{{ order.shippingName }}</span> · {{ order.shippingPhone }}</div>
          <div class="soft">{{ order.shippingAddress }}</div>
          <div v-if="order.buyerNote" class="soft" style="margin-top: 8px">"{{ order.buyerNote }}"</div>
        </section>

        <aside>
          <div class="panel" style="margin-bottom: 20px">
            <h3 style="margin-bottom: 12px">{{ t('checkout.orderSummary') }}</h3>
            <div class="between"><span class="muted">{{ t('common.price') }}</span><span>{{ formatPrice(order.agreedPrice) }}</span></div>
            <div class="between"><span class="muted">{{ t('common.shipping') }}</span><span>{{ Number(order.shippingFee) > 0 ? formatPrice(order.shippingFee) : t('common.free') }}</span></div>
            <div class="divider"></div>
            <div class="between"><span class="bold">{{ t('common.total') }}</span><span class="bold" style="font-family:var(--font-display); font-size:22px">{{ formatPrice(order.totalAmount) }}</span></div>
            <div class="soft" style="margin-top: 12px; font-size: 12px">{{ t('orders.escrowHint') }}</div>

            <div class="divider"></div>

            <div class="actions">
              <button v-if="role === 'buyer' && order.status === 'PENDING_PAYMENT'" class="btn btn-primary" @click="pay" type="button">{{ t('orders.pay') }}</button>
              <button v-if="role === 'seller' && order.status === 'PAID'" class="btn btn-primary" @click="showShipModal = true" type="button">{{ t('orders.ship') }}</button>
              <button v-if="role === 'buyer' && order.status === 'SHIPPED'" class="btn btn-primary" @click="confirmReceipt" type="button">{{ t('orders.confirmReceipt') }}</button>
              <button v-if="['PENDING_PAYMENT', 'PAID'].includes(order.status)" class="btn btn-outline" @click="cancel" type="button">{{ t('orders.cancel') }}</button>
              <button v-if="order.status === 'COMPLETED' && !myReview" class="btn btn-accent" @click="showReviewModal = true" type="button">{{ t('orders.leaveReview') }}</button>
            </div>
          </div>

          <div class="panel">
            <h3 style="margin-bottom: 12px">{{ role === 'buyer' ? t('common.seller') : t('common.buyer') }}</h3>
            <RouterLink :to="{ name: 'profile', params: { id: role === 'buyer' ? order.seller.id : order.buyer.id } }" class="row">
              <Avatar :user="role === 'buyer' ? order.seller : order.buyer" />
              <div>
                <div class="bold">{{ role === 'buyer' ? order.seller.displayName : order.buyer.displayName }}</div>
                <div class="soft"><Stars :rating="(role === 'buyer' ? order.seller : order.buyer).averageRating" :size="12" /></div>
              </div>
            </RouterLink>
          </div>
        </aside>
      </div>

      <section class="section" v-if="reviews.length">
        <h2 style="margin-bottom: 16px">{{ t('common.reviews') }}</h2>
        <div class="stack">
          <div v-for="r in reviews" :key="r.id" class="card">
            <div class="row" style="justify-content:space-between; margin-bottom: 8px">
              <div class="row">
                <Avatar :user="r.reviewer" />
                <div>
                  <div class="bold">{{ r.reviewer.displayName }}</div>
                  <Stars :rating="r.rating" :size="14" />
                </div>
              </div>
              <div class="soft">{{ formatRelative(r.createdAt, locale) }}</div>
            </div>
            <p style="margin: 0">{{ r.comment || '—' }}</p>
          </div>
        </div>
      </section>

      <div v-if="showShipModal" class="modal-overlay" @click.self="showShipModal = false">
        <div class="modal">
          <h3 style="margin-bottom: 16px">{{ t('orders.ship') }}</h3>
          <div class="field">
            <label class="label">{{ t('orders.shipping') }}</label>
            <input class="input" v-model="shipForm.carrier" maxlength="80" />
          </div>
          <div class="field">
            <label class="label">{{ t('orders.tracking') }}</label>
            <input class="input" v-model="shipForm.trackingNumber" maxlength="80" />
          </div>
          <div class="row" style="justify-content: flex-end">
            <button class="btn btn-ghost" @click="showShipModal = false" type="button">{{ t('common.cancel') }}</button>
            <button class="btn btn-primary" @click="ship" type="button">{{ t('common.submit') }}</button>
          </div>
        </div>
      </div>

      <div v-if="showReviewModal" class="modal-overlay" @click.self="showReviewModal = false">
        <div class="modal">
          <h3 style="margin-bottom: 16px">{{ t('orders.writeReview') }}</h3>
          <div class="field">
            <label class="label">{{ t('orders.rating') }}</label>
            <StarInput v-model="reviewForm.rating" />
          </div>
          <div class="field">
            <label class="label">{{ t('orders.comment') }}</label>
            <textarea class="textarea" v-model="reviewForm.comment" maxlength="1000"></textarea>
          </div>
          <div class="row" style="justify-content: flex-end">
            <button class="btn btn-ghost" @click="showReviewModal = false" type="button">{{ t('common.cancel') }}</button>
            <button class="btn btn-primary" @click="submitReview" type="button">{{ t('orders.submitReview') }}</button>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>
