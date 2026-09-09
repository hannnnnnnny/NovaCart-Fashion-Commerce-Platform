<script setup>
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { listingApi, categoryApi } from '../api/endpoints'
import { useToastStore } from '../stores/toast'
import { apiError } from '../api/client'
import ListingCard from '../components/ListingCard.vue'
import DataState from '../components/DataState.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const toast = useToastStore()

const categories = ref([])
const listings = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const error = ref('')
const total = ref(0)
const page = ref(0)
// Mobile: filters live behind a toggle so they don't bury the results.
// Desktop ignores this (the sidebar is always shown via CSS).
const showFilters = ref(false)

const filters = ref({
  keyword: route.query.keyword || '',
  categoryId: route.query.categoryId ? Number(route.query.categoryId) : '',
  minPrice: route.query.minPrice || '',
  maxPrice: route.query.maxPrice || '',
  condition: route.query.condition || '',
  location: route.query.location || '',
  sort: route.query.sort || 'newest'
})

const CONDITIONS = ['NEW', 'LIKE_NEW', 'GOOD', 'FAIR', 'FOR_PARTS']
const SORTS = ['newest', 'price_asc', 'price_desc', 'popular']

async function loadCategories() {
  try {
    categories.value = await categoryApi.list()
  } catch (err) {
    toast.error(apiError(err, 'Categories could not be loaded.'))
  }
}

function searchParams() {
  return {
    page: page.value,
    size: 24,
    keyword: filters.value.keyword || undefined,
    categoryId: filters.value.categoryId || undefined,
    minPrice: filters.value.minPrice || undefined,
    maxPrice: filters.value.maxPrice || undefined,
    condition: filters.value.condition || undefined,
    location: filters.value.location || undefined,
    sort: filters.value.sort
  }
}

async function search() {
  loading.value = true
  error.value = ''
  try {
    const result = await listingApi.search(searchParams())
    listings.value = result.content || []
    total.value = result.totalElements || 0
  } catch (err) {
    error.value = apiError(err)
  } finally {
    loading.value = false
  }
}

// "Load more" appends the next page — the pragmatic pagination for a
// waterfall layout (numbered pages would reshuffle the columns).
async function loadMore() {
  loadingMore.value = true
  try {
    page.value += 1
    const result = await listingApi.search(searchParams())
    listings.value = [...listings.value, ...(result.content || [])]
    total.value = result.totalElements || 0
  } catch (err) {
    page.value = Math.max(0, page.value - 1)
    toast.error(apiError(err))
  } finally {
    loadingMore.value = false
  }
}

function applyFilters() {
  page.value = 0
  showFilters.value = false // collapse the mobile panel so results are visible
  const nextQuery = cleanQuery()
  if (queriesMatch(route.query, nextQuery)) {
    search()
    return
  }
  router.replace({ query: nextQuery })
}

function reset() {
  filters.value = { keyword: '', categoryId: '', minPrice: '', maxPrice: '', condition: '', location: '', sort: 'newest' }
  applyFilters()
}

function cleanQuery() {
  const q = {}
  Object.entries(filters.value).forEach(([k, v]) => {
    if (v !== '' && v !== null && v !== undefined) q[k] = v
  })
  return q
}

function queriesMatch(current, next) {
  const currentEntries = Object.entries(current).filter(([, value]) => value !== undefined)
  const nextEntries = Object.entries(next)
  if (currentEntries.length !== nextEntries.length) return false
  return nextEntries.every(([key, value]) => queryValue(current[key]) === queryValue(value))
}

function queryValue(value) {
  return Array.isArray(value) ? value.join(',') : String(value ?? '')
}

watch(() => route.query, (next) => {
  // Restore EVERY filter from the URL so back/forward keeps the form in sync
  // with the results (previously only keyword/categoryId were restored).
  filters.value = {
    keyword: next.keyword || '',
    categoryId: next.categoryId ? Number(next.categoryId) : '',
    minPrice: next.minPrice || '',
    maxPrice: next.maxPrice || '',
    condition: next.condition || '',
    location: next.location || '',
    sort: next.sort || 'newest'
  }
  page.value = 0
  search()
})

onMounted(async () => {
  await loadCategories()
  search()
})
</script>

<template>
  <main class="page">
    <div class="container">
      <h1 style="margin-bottom: 24px">{{ t('common.browse') }}</h1>
      <button class="btn btn-outline filters-toggle" type="button" @click="showFilters = !showFilters">
        {{ t('common.filters') }}{{ showFilters ? ' ▲' : ' ▼' }}
      </button>
      <div class="two-col">
        <aside class="filters" :class="{ 'filters-collapsed': !showFilters }">
          <div class="card">
            <div class="field">
              <label class="label">{{ t('common.categories') }}</label>
              <select class="select" v-model="filters.categoryId">
                <option value="">{{ t('common.anywhere') }}</option>
                <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.icon }} {{ c.name }}</option>
              </select>
            </div>
            <div class="field">
              <label class="label">{{ t('common.condition') }}</label>
              <select class="select" v-model="filters.condition">
                <option value="">{{ t('common.anyCondition') }}</option>
                <option v-for="c in CONDITIONS" :key="c" :value="c">{{ t(`condition.${c}`) }}</option>
              </select>
            </div>
            <div class="field">
              <label class="label">{{ t('common.price') }}</label>
              <div class="row">
                <input class="input" type="number" :placeholder="t('common.from')" v-model="filters.minPrice" />
                <input class="input" type="number" :placeholder="t('common.to')" v-model="filters.maxPrice" />
              </div>
            </div>
            <div class="field">
              <label class="label">{{ t('common.location') }}</label>
              <input class="input" v-model="filters.location" :placeholder="t('common.anywhere')" />
            </div>
            <div class="row" style="gap:8px">
              <button class="btn btn-primary grow" @click="applyFilters" type="button">{{ t('common.apply') }}</button>
              <button class="btn btn-ghost" @click="reset" type="button">{{ t('common.reset') }}</button>
            </div>
          </div>
        </aside>

        <section>
          <div class="between" style="margin-bottom: 16px">
            <div class="muted">{{ total }} {{ t('common.results') }}</div>
            <select class="select" style="max-width: 220px" v-model="filters.sort" @change="applyFilters">
              <option v-for="s in SORTS" :key="s" :value="s">{{ t(`sort.${s}`) }}</option>
            </select>
          </div>

          <DataState :loading="loading" :error="error" :empty="listings.length === 0" @retry="search">
            <div class="grid grid-listings">
              <ListingCard v-for="l in listings" :key="l.id" :listing="l" />
            </div>
            <div v-if="listings.length < total" class="text-center" style="margin-top: 24px">
              <button class="btn btn-outline" type="button" :disabled="loadingMore" @click="loadMore">
                {{ loadingMore ? t('common.loading') : t('common.loadMore') }}
              </button>
            </div>
          </DataState>
        </section>
      </div>
    </div>
  </main>
</template>
