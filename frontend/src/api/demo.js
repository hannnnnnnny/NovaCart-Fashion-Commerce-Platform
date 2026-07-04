/*
 * Front-end demo mode.
 *
 * When the app is published to GitHub Pages there is no backend, so every
 * API call would 404. This module provides an in-browser axios adapter that
 * serves seeded data and a fake auth flow, letting visitors actually browse
 * listings, "log in" with the demo accounts, and click through the UI with
 * no server. Enabled via VITE_RENOVA_DEMO=true at build time.
 */

const asset = (name) => `${import.meta.env.BASE_URL}demo-images/products/${name}`
const now = Date.now()
const daysAgo = (d) => new Date(now - d * 86400000).toISOString()

const users = {
  1: {
    id: 1,
    email: 'ava@renova.local',
    displayName: 'Ava Thompson',
    avatarUrl: null,
    location: 'Auckland',
    role: 'USER',
    bio: 'Pre-loved fashion and homeware curator. Everything cleaned and checked.',
    averageRating: 4.8,
    ratingCount: 23,
    memberSince: daysAgo(320)
  },
  2: {
    id: 2,
    email: 'liam@renova.local',
    displayName: 'Liam Carter',
    avatarUrl: null,
    location: 'Wellington',
    role: 'USER',
    bio: 'Sports gear and home odds and ends. Happy to bundle.',
    averageRating: 4.6,
    ratingCount: 15,
    memberSince: daysAgo(210)
  },
  3: {
    id: 3,
    email: 'maya@renova.local',
    displayName: 'Maya Flores',
    avatarUrl: null,
    location: 'Christchurch',
    role: 'USER',
    bio: 'Vintage finds and camera gear. Fast shipper.',
    averageRating: 4.9,
    ratingCount: 11,
    memberSince: daysAgo(160)
  },
  4: {
    id: 4,
    email: 'theo@renova.local',
    displayName: 'Theo Novak',
    avatarUrl: null,
    location: 'Hamilton',
    role: 'USER',
    bio: 'Always open to a fair offer.',
    averageRating: 4.5,
    ratingCount: 8,
    memberSince: daysAgo(95)
  },
  5: {
    id: 5,
    email: 'ivy@renova.local',
    displayName: 'Ivy Bennett',
    avatarUrl: null,
    location: 'Dunedin',
    role: 'USER',
    bio: 'Plants, ceramics, and cosy knitwear.',
    averageRating: 5.0,
    ratingCount: 6,
    memberSince: daysAgo(70)
  }
}

const summaryUser = (u) => ({
  id: u.id,
  email: u.email,
  displayName: u.displayName,
  avatarUrl: u.avatarUrl,
  location: u.location,
  role: u.role
})
const publicUser = (u) => ({
  id: u.id,
  displayName: u.displayName,
  avatarUrl: u.avatarUrl,
  bio: u.bio,
  location: u.location,
  averageRating: u.averageRating,
  ratingCount: u.ratingCount,
  memberSince: u.memberSince
})

const categories = [
  { id: 1, name: "Women's Fashion", slug: 'women', icon: '👗', sortOrder: 1 },
  { id: 2, name: "Men's Fashion", slug: 'men', icon: '👔', sortOrder: 2 },
  { id: 3, name: 'Shoes', slug: 'shoes', icon: '👟', sortOrder: 3 },
  { id: 4, name: 'Bags & Accessories', slug: 'bags', icon: '👜', sortOrder: 4 },
  { id: 5, name: 'Home', slug: 'home', icon: '🏠', sortOrder: 5 },
  { id: 6, name: 'Sports', slug: 'sports', icon: '🏀', sortOrder: 6 }
]
const categoryById = (id) => categories.find((c) => c.id === id) || categories[0]

const rawListings = [
  { id: 1, sellerId: 1, categoryId: 1, title: 'Silk Wrap Dress', img: 'fashion-dress.jpg', price: 68, originalPrice: 180, condition: 'LIKE_NEW', status: 'ACTIVE', loc: 'Auckland', views: 142, favs: 18,
    desc: 'Elegant silk wrap dress, worn twice for events. No marks, freshly dry-cleaned. Size 10.' },
  { id: 2, sellerId: 1, categoryId: 4, title: 'Leather Tote Bag', img: 'fashion-bag.jpg', price: 95, originalPrice: 240, condition: 'GOOD', status: 'ACTIVE', loc: 'Auckland', views: 98, favs: 12,
    desc: 'Full-grain leather tote with a roomy interior. Light patina, all zips work perfectly.' },
  { id: 3, sellerId: 1, categoryId: 1, title: 'Tailored Wool Blazer', img: 'fashion-blazer.jpg', price: 54, originalPrice: 150, condition: 'GOOD', status: 'ACTIVE', loc: 'Auckland', views: 76, favs: 7,
    desc: 'Structured wool blazer, great for work. Minor wear on lining, exterior immaculate.' },
  { id: 4, sellerId: 1, categoryId: 4, title: 'Beaded Clutch', img: 'fashion-clutch.jpg', price: 28, originalPrice: 70, condition: 'LIKE_NEW', status: 'RESERVED', loc: 'Auckland', views: 51, favs: 9,
    desc: 'Hand-beaded evening clutch with chain strap. Stored in dust bag, like new.' },
  { id: 5, sellerId: 1, categoryId: 4, title: 'Gold Drop Earrings', img: 'fashion-earrings.jpg', price: 22, originalPrice: 55, condition: 'NEW', status: 'ACTIVE', loc: 'Auckland', views: 63, favs: 11,
    desc: 'Brand new gold-plated drop earrings, never worn. Hypoallergenic posts.' },
  { id: 6, sellerId: 2, categoryId: 3, title: 'Running Sneakers', img: 'sports-sneaker.jpg', price: 60, originalPrice: 160, condition: 'GOOD', status: 'ACTIVE', loc: 'Wellington', views: 120, favs: 14,
    desc: 'Cushioned running shoes, ~120km on them. Plenty of life left, cleaned and deodorised.' },
  { id: 7, sellerId: 2, categoryId: 6, title: 'Yoga Mat + Strap', img: 'sports-mat.jpg', price: 18, originalPrice: 45, condition: 'GOOD', status: 'ACTIVE', loc: 'Wellington', views: 44, favs: 5,
    desc: 'Non-slip 6mm yoga mat with carry strap. Wiped down, no tears.' },
  { id: 8, sellerId: 2, categoryId: 6, title: 'Weekender Duffel', img: 'sports-duffel.jpg', price: 35, originalPrice: 90, condition: 'LIKE_NEW', status: 'ACTIVE', loc: 'Wellington', views: 58, favs: 6,
    desc: 'Water-resistant duffel, used on one trip. Shoe compartment and laptop sleeve.' },
  { id: 9, sellerId: 2, categoryId: 5, title: 'Ceramic Mug Set (4)', img: 'home-cups.jpg', price: 24, originalPrice: 60, condition: 'NEW', status: 'ACTIVE', loc: 'Wellington', views: 39, favs: 4,
    desc: 'Set of four handmade ceramic mugs, unused gift. Dishwasher safe.' },
  { id: 10, sellerId: 2, categoryId: 5, title: 'Woven Throw Blanket', img: 'home-throw.jpg', price: 30, originalPrice: 80, condition: 'GOOD', status: 'SOLD', loc: 'Wellington', views: 71, favs: 8,
    desc: 'Chunky knit throw, washed and cosy. Sold — kept for demo browsing.' }
]

const favorites = new Set([2, 5])

const toSummary = (l) => ({
  id: l.id,
  title: l.title,
  price: l.price,
  originalPrice: l.originalPrice,
  condition: l.condition,
  location: l.loc,
  negotiable: true,
  coverImageUrl: asset(l.img),
  status: l.status,
  viewCount: l.views,
  favoriteCount: l.favs,
  createdAt: daysAgo(l.id * 3),
  category: categoryById(l.categoryId),
  seller: publicUser(users[l.sellerId])
})
const toDetail = (l) => ({
  ...toSummary(l),
  description: l.desc,
  shippingFee: l.price > 60 ? 0 : 6.5,
  imageUrls: [asset(l.img)],
  updatedAt: daysAgo(l.id),
  favorited: favorites.has(l.id)
})

const listingById = (id) => rawListings.find((l) => l.id === Number(id))

// ---------------------------------------------------------------------------
// Transaction graph for the static demo (no backend). Built so a visitor who
// "logs in" as ava@renova.local sees populated messages, offers, orders, and
// reviews — mirroring the backend seed. Records are stored raw and projected
// relative to the current user, so logging in as anyone else still works.
// ---------------------------------------------------------------------------

// Conversations: each has a buyer, a seller, a listing, and a message thread.
const rawConversations = [
  {
    id: 1, listingId: 6, buyerId: 1, sellerId: 2,
    messages: [
      { senderId: 1, body: 'Hi! Are the running sneakers still available?', days: 2, mins: 0 },
      { senderId: 2, body: 'Yep, barely 120km on them. Cleaned and ready to post.', days: 2, mins: 35 },
      { senderId: 1, body: 'Great — I just sent an offer at $50.', days: 1, mins: 0 },
      { senderId: 2, body: 'Accepted! I\'ll ship tomorrow and send tracking.', days: 1, mins: 20, unreadFor: 'buyer' }
    ]
  },
  {
    id: 2, listingId: 1, buyerId: 3, sellerId: 1,
    messages: [
      { senderId: 3, body: 'Is the silk wrap dress true to a size 10?', days: 1, mins: 0 },
      { senderId: 1, body: 'Runs slightly small — a comfy 8 to 10.', days: 1, mins: 25 },
      { senderId: 3, body: 'Perfect, putting an offer in now!', days: 0, mins: 90, unreadFor: 'seller' }
    ]
  },
  {
    id: 3, listingId: 2, buyerId: 4, sellerId: 1,
    messages: [
      { senderId: 4, body: 'Would you ship the leather tote to Hamilton?', days: 3, mins: 0 },
      { senderId: 1, body: 'Absolutely — free shipping on this one.', days: 3, mins: 40 }
    ]
  }
]

const convLastMsg = (c) => c.messages[c.messages.length - 1]
const convUnreadFor = (c, uid) => {
  const side = uid === c.buyerId ? 'buyer' : uid === c.sellerId ? 'seller' : null
  if (!side) return 0
  return c.messages.filter((m) => m.unreadFor === side).length
}
const msgTime = (m) => new Date(now - m.days * 86400000 + (m.mins || 0) * 60000).toISOString()

function conversationSummary(c, uid) {
  const l = listingById(c.listingId)
  const counterpartyId = uid === c.buyerId ? c.sellerId : c.buyerId
  const last = convLastMsg(c)
  return {
    id: c.id,
    listingId: c.listingId,
    listingTitle: l ? l.title : 'Listing',
    listingCoverImageUrl: l ? asset(l.img) : null,
    listingStatus: l ? l.status : 'ACTIVE',
    counterparty: publicUser(users[counterpartyId]),
    role: uid === c.buyerId ? 'BUYER' : 'SELLER',
    lastMessagePreview: last ? last.body : '',
    lastMessageAt: last ? msgTime(last) : daysAgo(1),
    unreadCount: convUnreadFor(c, uid)
  }
}
function conversationDetail(c, uid) {
  return {
    conversation: conversationSummary(c, uid),
    messages: c.messages.map((m, i) => ({
      id: c.id * 100 + i,
      conversationId: c.id,
      senderId: m.senderId,
      senderName: users[m.senderId] ? users[m.senderId].displayName : 'User',
      body: m.body,
      createdAt: msgTime(m),
      readAt: m.unreadFor ? null : msgTime(m)
    }))
  }
}

// Offers: buyer makes an offer on a listing (the seller is the listing owner).
const rawOffers = [
  { id: 1, listingId: 1, buyerId: 3, amount: 55, message: 'Love this dress! Would $55 work?', status: 'PENDING', fromSeller: false, days: 1 },
  { id: 2, listingId: 2, buyerId: 4, amount: 80, message: '$80 shipped to Hamilton?', status: 'PENDING', fromSeller: false, days: 2 },
  { id: 3, listingId: 6, buyerId: 1, amount: 50, message: 'Would you take $50?', status: 'ACCEPTED', fromSeller: false, days: 1, respondedDays: 1 },
  { id: 4, listingId: 7, buyerId: 1, amount: 14, message: '$14 for the yoga mat?', status: 'PENDING', fromSeller: false, days: 0 }
]
function offerResponse(o) {
  const l = listingById(o.listingId)
  return {
    id: o.id,
    listingId: o.listingId,
    listingTitle: l ? l.title : 'Listing',
    listingCoverImageUrl: l ? asset(l.img) : null,
    buyer: publicUser(users[o.buyerId]),
    seller: publicUser(users[l ? l.sellerId : 1]),
    amount: o.amount,
    message: o.message,
    status: o.status,
    fromSeller: o.fromSeller,
    parentOfferId: null,
    createdAt: daysAgo(o.days),
    respondedAt: o.respondedDays != null ? daysAgo(o.respondedDays) : null
  }
}

// Orders: a buyer's purchase from a seller, in various lifecycle states.
const rawOrders = [
  { id: 1, num: 'RN10231007', listingId: 6, buyerId: 1, sellerId: 2, agreed: 50, ship: 0, status: 'SHIPPED', days: 2, carrier: 'NZ Post', tracking: 'NZ1234567', name: 'Ava Thompson', phone: '+64 21 555 0142', addr: '14 Ponsonby Rd, Auckland 1011' },
  { id: 2, num: 'RN10240412', listingId: 9, buyerId: 1, sellerId: 2, agreed: 24, ship: 0, status: 'COMPLETED', days: 14, carrier: 'CourierPost', tracking: 'NZ9988776', name: 'Ava Thompson', phone: '+64 21 555 0142', addr: '14 Ponsonby Rd, Auckland 1011' },
  { id: 3, num: 'RN10255533', listingId: 3, buyerId: 3, sellerId: 1, agreed: 54, ship: 0, status: 'PAID', days: 2, name: 'Maya Flores', phone: '+64 21 555 0188', addr: '5 Colombo St, Christchurch 8011' },
  { id: 4, num: 'RN10260088', listingId: 4, buyerId: 5, sellerId: 1, agreed: 28, ship: 0, status: 'COMPLETED', days: 10, carrier: 'NZ Post', tracking: 'NZ5544332', name: 'Ivy Bennett', phone: '+64 21 555 0170', addr: '88 George St, Dunedin 9016' }
]
function orderResponse(o) {
  const l = listingById(o.listingId)
  const paid = ['PAID', 'SHIPPED', 'DELIVERED', 'COMPLETED'].includes(o.status)
  const shipped = ['SHIPPED', 'DELIVERED', 'COMPLETED'].includes(o.status)
  const done = o.status === 'COMPLETED'
  return {
    id: o.id,
    orderNumber: o.num,
    listingId: o.listingId,
    listingTitle: l ? l.title : 'Listing',
    listingCoverImageUrl: l ? asset(l.img) : null,
    buyer: publicUser(users[o.buyerId]),
    seller: publicUser(users[o.sellerId]),
    agreedPrice: o.agreed,
    shippingFee: o.ship,
    totalAmount: o.agreed + o.ship,
    shippingName: o.name,
    shippingPhone: o.phone,
    shippingAddress: o.addr,
    buyerNote: null,
    trackingNumber: shipped ? o.tracking || null : null,
    carrier: shipped ? o.carrier || null : null,
    status: o.status,
    createdAt: daysAgo(o.days),
    paidAt: paid ? daysAgo(o.days) : null,
    shippedAt: shipped ? daysAgo(Math.max(0, o.days - 1)) : null,
    deliveredAt: done ? daysAgo(Math.max(0, o.days - 3)) : null,
    completedAt: done ? daysAgo(Math.max(0, o.days - 3)) : null,
    cancelledAt: null,
    cancelReason: null
  }
}

// Reviews left on completed orders (reviewee gets the rating).
const rawReviews = [
  { id: 1, orderId: 4, listingId: 4, reviewerId: 5, revieweeId: 1, rating: 5, comment: 'Beautiful clutch, even nicer in person. Fast shipping!', role: 'BUYER_REVIEWS_SELLER', days: 9 },
  { id: 2, orderId: 2, listingId: 9, reviewerId: 1, revieweeId: 2, rating: 5, comment: 'Mugs arrived perfectly packed. Thanks Liam!', role: 'BUYER_REVIEWS_SELLER', days: 12 },
  { id: 3, orderId: 4, listingId: 4, reviewerId: 1, revieweeId: 5, rating: 5, comment: 'Smooth, friendly buyer. Recommended.', role: 'SELLER_REVIEWS_BUYER', days: 9 }
]
function reviewResponse(r) {
  const l = listingById(r.listingId)
  return {
    id: r.id,
    orderId: r.orderId,
    listingTitle: l ? l.title : 'Listing',
    reviewer: publicUser(users[r.reviewerId]),
    reviewee: publicUser(users[r.revieweeId]),
    rating: r.rating,
    comment: r.comment,
    role: r.role,
    createdAt: daysAgo(r.days)
  }
}

function paginate(items, params = {}) {
  const page = Number(params.page ?? 0)
  const size = Number(params.size ?? 12)
  const start = page * size
  const slice = items.slice(start, start + size)
  return {
    content: slice,
    page,
    size,
    totalElements: items.length,
    totalPages: Math.max(1, Math.ceil(items.length / size)),
    first: page === 0,
    last: start + size >= items.length,
    empty: slice.length === 0
  }
}

function searchListings(params = {}) {
  let items = rawListings.filter((l) => l.status !== 'REMOVED')
  if (params.categoryId) items = items.filter((l) => l.categoryId === Number(params.categoryId))
  // The real API uses `keyword`; match title + description.
  const kw = params.keyword ?? params.q
  if (kw) {
    const q = String(kw).toLowerCase()
    items = items.filter((l) => l.title.toLowerCase().includes(q) || l.desc.toLowerCase().includes(q))
  }
  if (params.condition) items = items.filter((l) => l.condition === params.condition)
  if (params.minPrice != null && params.minPrice !== '') items = items.filter((l) => l.price >= Number(params.minPrice))
  if (params.maxPrice != null && params.maxPrice !== '') items = items.filter((l) => l.price <= Number(params.maxPrice))
  if (params.location) {
    const loc = String(params.location).toLowerCase()
    items = items.filter((l) => (l.loc || '').toLowerCase().includes(loc))
  }
  if (params.sort === 'price_asc') items = [...items].sort((a, b) => a.price - b.price)
  else if (params.sort === 'price_desc') items = [...items].sort((a, b) => b.price - a.price)
  else if (params.sort === 'popular') items = [...items].sort((a, b) => b.favs - a.favs)
  else items = [...items].sort((a, b) => b.id - a.id)
  return paginate(items.map(toSummary), params)
}

function currentUserId(config) {
  const auth = config.headers?.Authorization || config.headers?.authorization || ''
  const m = /demo\.(\d+)/.exec(auth)
  return m ? Number(m[1]) : 1
}

// Returns the unwrapped payload for a request, or throws { status } for 404.
function route(config) {
  const method = (config.method || 'get').toUpperCase()
  const path = (config.url || '').split('?')[0].replace(/\/$/, '')
  const params = config.params || {}
  const body = parseBody(config.data)
  const id = (re) => {
    const m = re.exec(path)
    return m ? Number(m[1]) : null
  }

  // ---- auth ----
  if (path === '/auth/login' && method === 'POST') {
    const u = Object.values(users).find((x) => x.email === (body.email || '').toLowerCase()) || users[1]
    return { token: `demo.${u.id}`, expiresInMinutes: 120, user: summaryUser(u) }
  }
  if (path === '/auth/signup' && method === 'POST') {
    const u = { ...users[1], email: body.email || 'guest@renova.local', displayName: body.displayName || 'Demo Guest' }
    return { token: 'demo.1', expiresInMinutes: 120, user: summaryUser(u) }
  }
  if (path === '/auth/me' || path === '/users/me') {
    return method === 'PUT' ? { ...summaryUser(users[currentUserId(config)]), ...body } : summaryUser(users[currentUserId(config)])
  }

  // ---- public catalogue ----
  if (path === '/public/categories') return categories
  if (path === '/public/listings') return searchListings(params)
  let m
  if ((m = /^\/public\/listings\/(\d+)$/.exec(path))) {
    const l = rawListings.find((x) => x.id === Number(m[1]))
    if (!l) throw { status: 404, message: 'Listing not found' }
    return toDetail(l)
  }
  if ((m = /^\/public\/users\/(\d+)\/listings$/.exec(path))) {
    return paginate(rawListings.filter((l) => l.sellerId === Number(m[1])).map(toSummary), params)
  }
  if ((m = /^\/public\/users\/(\d+)\/reviews$/.exec(path))) {
    return rawReviews.filter((r) => r.revieweeId === Number(m[1])).map(reviewResponse)
  }
  if ((m = /^\/public\/users\/(\d+)$/.exec(path))) {
    const u = users[Number(m[1])]
    if (!u) throw { status: 404, message: 'User not found' }
    return publicUser(u)
  }

  // ---- listings (auth) ----
  if (path === '/listings/mine') return paginate(rawListings.filter((l) => l.sellerId === currentUserId(config)).map(toSummary), params)
  if (path === '/listings/favorites') return paginate(rawListings.filter((l) => favorites.has(l.id)).map(toSummary), params)
  if ((m = /^\/listings\/(\d+)\/favorite$/.exec(path)) && method === 'POST') {
    const lid = Number(m[1])
    if (favorites.has(lid)) favorites.delete(lid)
    else favorites.add(lid)
    return { favorited: favorites.has(lid) }
  }
  if (path === '/listings' && method === 'POST') return { ...toDetail(rawListings[0]), id: 999, title: body.title || 'New listing' }
  if ((m = /^\/listings\/(\d+)$/.exec(path))) return { id: Number(m[1]) }

  // ---- offers (relative to the logged-in user) ----
  const uid = currentUserId(config)
  if (path === '/offers/received') {
    // offers on listings the current user is selling
    const mine = rawOffers.filter((o) => { const l = listingById(o.listingId); return l && l.sellerId === uid })
    return paginate(mine.map(offerResponse), params)
  }
  if (path === '/offers/sent') {
    const mine = rawOffers.filter((o) => o.buyerId === uid)
    return paginate(mine.map(offerResponse), params)
  }
  if ((m = /^\/offers\/(\d+)\/(accept|reject|counter|withdraw|accept-counter)$/.exec(path)) && method === 'POST') {
    const o = rawOffers.find((x) => x.id === Number(m[1]))
    if (!o) throw { status: 404, message: 'Offer not found' }
    const next = {
      accept: 'ACCEPTED', reject: 'REJECTED', counter: 'COUNTERED',
      withdraw: 'WITHDRAWN', 'accept-counter': 'ACCEPTED'
    }[m[2]]
    o.status = next
    o.respondedDays = 0
    // Accepting an offer reserves its listing and rejects the siblings,
    // mirroring the backend so the UI reflects a real state change.
    if (m[2] === 'accept' || m[2] === 'accept-counter') {
      const listing = listingById(o.listingId)
      if (listing) listing.status = 'RESERVED'
      rawOffers.forEach((x) => {
        if (x.id !== o.id && x.listingId === o.listingId && x.status === 'PENDING') x.status = 'REJECTED'
      })
    }
    return offerResponse(o)
  }

  // ---- orders ----
  if (path === '/orders/buying') {
    return paginate(rawOrders.filter((o) => o.buyerId === uid).map(orderResponse), params)
  }
  if (path === '/orders/selling') {
    return paginate(rawOrders.filter((o) => o.sellerId === uid).map(orderResponse), params)
  }
  if ((m = /^\/orders\/(\d+)\/reviews$/.exec(path))) {
    return rawReviews.filter((r) => r.orderId === Number(m[1])).map(reviewResponse)
  }
  if ((m = /^\/orders\/(\d+)\/(pay|ship|confirm-receipt|cancel)$/.exec(path)) && method === 'POST') {
    const o = rawOrders.find((x) => x.id === Number(m[1]))
    if (!o) throw { status: 404, message: 'Order not found' }
    // Advance the in-memory order so the detail page re-renders the new state
    // (and, critically, returns a full order object — not { ok: true } — which
    // the page assigns to `order.value`).
    if (m[2] === 'pay') o.status = 'PAID'
    else if (m[2] === 'ship') { o.status = 'SHIPPED'; o.carrier = body.carrier || o.carrier; o.tracking = body.trackingNumber || o.tracking }
    else if (m[2] === 'confirm-receipt') { o.status = 'COMPLETED'; const l = listingById(o.listingId); if (l) l.status = 'SOLD' }
    else if (m[2] === 'cancel') o.status = 'CANCELLED'
    return orderResponse(o)
  }
  if ((m = /^\/orders\/(\d+)$/.exec(path))) {
    const o = rawOrders.find((x) => x.id === Number(m[1]))
    if (!o) throw { status: 404, message: 'Order not found' }
    return orderResponse(o)
  }
  if (path === '/orders' && method === 'POST') {
    return orderResponse(rawOrders[0]) // demo checkout returns a sample order
  }

  // ---- conversations ----
  if (path === '/conversations') {
    if (method === 'POST') {
      // starting/continuing a conversation: return the matching thread or the first one
      const lid = Number(body.listingId)
      const c = rawConversations.find((x) => x.listingId === lid) || rawConversations[0]
      return conversationDetail(c, uid)
    }
    const mine = rawConversations
      .filter((c) => c.buyerId === uid || c.sellerId === uid)
      .sort((a, b) => new Date(conversationSummary(b, uid).lastMessageAt) - new Date(conversationSummary(a, uid).lastMessageAt))
    return mine.map((c) => conversationSummary(c, uid))
  }
  if (path === '/conversations/unread-count') {
    const total = rawConversations
      .filter((c) => c.buyerId === uid || c.sellerId === uid)
      .reduce((sum, c) => sum + convUnreadFor(c, uid), 0)
    return { count: total }
  }
  if ((m = /^\/conversations\/(\d+)\/messages$/.exec(path)) && method === 'POST') {
    return {
      id: Date.now(), conversationId: Number(m[1]), senderId: uid,
      senderName: users[uid] ? users[uid].displayName : 'You',
      body: body.body || '', createdAt: new Date().toISOString(), readAt: null
    }
  }
  if ((m = /^\/conversations\/(\d+)$/.exec(path))) {
    const c = rawConversations.find((x) => x.id === Number(m[1]))
    if (!c) throw { status: 404, message: 'Conversation not found' }
    return conversationDetail(c, uid)
  }

  // ---- reviews ----
  if (path === '/reviews' && method === 'POST') {
    const order = rawOrders.find((x) => x.id === Number(body.orderId))
    if (!order) throw { status: 404, message: 'Order not found' }
    const buyerReviews = order.buyerId === uid
    const revieweeId = buyerReviews ? order.sellerId : order.buyerId
    const review = {
      id: Date.now(),
      orderId: order.id,
      listingId: order.listingId,
      reviewerId: uid,
      revieweeId,
      rating: Number(body.rating) || 5,
      comment: body.comment || '',
      role: buyerReviews ? 'BUYER_REVIEWS_SELLER' : 'SELLER_REVIEWS_BUYER',
      days: 0
    }
    rawReviews.push(review)
    return reviewResponse(review)
  }

  // ---- image upload ----
  if (path === '/uploads/images' && method === 'POST') {
    // config.data is a FormData; turn each real File into an object URL so the
    // uploaded photo shows in the preview even though there's no backend.
    const files = (config.data && typeof config.data.getAll === 'function')
      ? config.data.getAll('files') : []
    const images = files.map((f, i) => ({
      url: (typeof URL !== 'undefined' && typeof URL.createObjectURL === 'function' && f && f.size != null)
        ? URL.createObjectURL(f)
        : asset('fashion-dress.jpg'),
      contentType: (f && f.type) || 'image/jpeg',
      size: (f && f.size) || 0,
      originalName: (f && f.name) || `image-${i + 1}.jpg`
    }))
    return { images }
  }

  // default: succeed quietly so the demo never shows a hard error
  if (method === 'GET') return null
  return { ok: true }
}

function parseBody(data) {
  if (!data) return {}
  if (typeof data === 'object') return data
  try {
    return JSON.parse(data)
  } catch {
    return {}
  }
}

// Axios adapter: resolve with an ApiResponse-shaped envelope, or reject like the real API.
export function demoAdapter(config) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      try {
        const data = route(config)
        resolve({
          data: { success: true, message: '', data, timestamp: new Date().toISOString() },
          status: 200,
          statusText: 'OK',
          headers: { 'content-type': 'application/json' },
          config
        })
      } catch (err) {
        const status = err?.status || 500
        reject({
          response: {
            status,
            data: { success: false, message: err?.message || 'Demo request failed', data: null }
          },
          config,
          isAxiosError: true
        })
      }
    }, 180) // small latency so loading states are visible
  })
}

export const DEMO_MODE = import.meta.env.VITE_RENOVA_DEMO === 'true'
