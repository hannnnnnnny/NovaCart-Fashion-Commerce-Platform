import client, { unwrap } from './client'

export const authApi = {
  signup: (payload) => client.post('/auth/signup', payload).then(unwrap),
  login: (payload) => client.post('/auth/login', payload).then(unwrap),
  me: () => client.get('/auth/me').then(unwrap)
}

export const categoryApi = {
  list: () => client.get('/public/categories').then(unwrap)
}

export const listingApi = {
  search: (params) => client.get('/public/listings', { params }).then(unwrap),
  get: (id) => client.get(`/public/listings/${id}`).then(unwrap),
  create: (payload) => client.post('/listings', payload).then(unwrap),
  update: (id, payload) => client.put(`/listings/${id}`, payload).then(unwrap),
  remove: (id) => client.delete(`/listings/${id}`).then(unwrap),
  mine: (params) => client.get('/listings/mine', { params }).then(unwrap),
  toggleFavorite: (id) => client.post(`/listings/${id}/favorite`).then(unwrap),
  favorites: (params) => client.get('/listings/favorites', { params }).then(unwrap)
}

export const offerApi = {
  create: (payload) => client.post('/offers', payload).then(unwrap),
  accept: (id) => client.post(`/offers/${id}/accept`).then(unwrap),
  reject: (id) => client.post(`/offers/${id}/reject`).then(unwrap),
  counter: (id, payload) => client.post(`/offers/${id}/counter`, payload).then(unwrap),
  withdraw: (id) => client.post(`/offers/${id}/withdraw`).then(unwrap),
  acceptCounter: (id) => client.post(`/offers/${id}/accept-counter`).then(unwrap),
  received: (params) => client.get('/offers/received', { params }).then(unwrap),
  sent: (params) => client.get('/offers/sent', { params }).then(unwrap)
}

export const conversationApi = {
  list: () => client.get('/conversations').then(unwrap),
  start: (payload) => client.post('/conversations', payload).then(unwrap),
  open: (id) => client.get(`/conversations/${id}`).then(unwrap),
  send: (id, payload) => client.post(`/conversations/${id}/messages`, payload).then(unwrap),
  unread: () => client.get('/conversations/unread-count').then(unwrap)
}

export const orderApi = {
  create: (payload) => client.post('/orders', payload).then(unwrap),
  get: (id) => client.get(`/orders/${id}`).then(unwrap),
  pay: (id) => client.post(`/orders/${id}/pay`).then(unwrap),
  ship: (id, payload) => client.post(`/orders/${id}/ship`, payload).then(unwrap),
  confirmReceipt: (id) => client.post(`/orders/${id}/confirm-receipt`).then(unwrap),
  cancel: (id, payload) => client.post(`/orders/${id}/cancel`, payload).then(unwrap),
  buying: (params) => client.get('/orders/buying', { params }).then(unwrap),
  selling: (params) => client.get('/orders/selling', { params }).then(unwrap)
}

export const reviewApi = {
  create: (payload) => client.post('/reviews', payload).then(unwrap),
  forUser: (userId) => client.get(`/public/users/${userId}/reviews`).then(unwrap),
  forOrder: (orderId) => client.get(`/orders/${orderId}/reviews`).then(unwrap)
}

export const userApi = {
  me: () => client.get('/users/me').then(unwrap),
  updateMe: (payload) => client.put('/users/me', payload).then(unwrap),
  publicProfile: (id) => client.get(`/public/users/${id}`).then(unwrap),
  publicListings: (id, params) => client.get(`/public/users/${id}/listings`, { params }).then(unwrap)
}

export const uploadApi = {
  /**
   * Upload one or more image files via multipart form data.
   * Returns the parsed `UploadedImagesResponse` (`{ images: [{url, …}] }`).
   *
   * Axios picks the right multipart boundary automatically when the
   * payload is FormData, so we explicitly drop the JSON Content-Type
   * the global client sets — letting axios set the boundary header.
   */
  images: (files) => {
    const fd = new FormData()
    for (const f of files) fd.append('files', f)
    return client.post('/uploads/images', fd, {
      headers: { 'Content-Type': undefined }
    }).then(unwrap).then(resolveUploadedImages)
  }
}

function resolveUploadedImages(result) {
  const apiBase = import.meta.env.VITE_API_BASE_URL || '/api'
  if (!/^https?:\/\//.test(apiBase)) return result
  // Upload paths belong to the API origin when the SPA is hosted separately.
  return {
    ...result,
    images: result.images.map((image) => ({
      ...image,
      url: image.url.startsWith('/uploads/') ? new URL(image.url, apiBase).href : image.url
    }))
  }
}
