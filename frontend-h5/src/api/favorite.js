import request from './request'

// 收藏帖子
export const addFavorite = (postId) => request.post('/api/favorite/add', { postId })
// 取消收藏
export const cancelFavorite = (postId) => request.post('/api/favorite/cancel', { postId })
// 我的收藏（分页）
export const getMyFavorites = (params) => request.get('/api/favorite/list', { params })
// 收藏状态 + 收藏数
export const getFavoriteStatus = (postId) => request.get(`/api/favorite/status/${postId}`)
