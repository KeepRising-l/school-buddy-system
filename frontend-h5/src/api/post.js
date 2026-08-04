import request from './request'

export const createPost = (data) => request.post('/api/post/create', data)
export const getPostDetail = (id) => request.get(`/api/post/detail/${id}`)
export const cancelPost = (id) => request.post(`/api/post/cancel/${id}`)
export const getSquareList = (params) => request.get('/api/square/list', { params })
export const addComment = (data) => request.post('/api/post/comment', data)
export const getComments = (postId, params) => request.get(`/api/post/comments/${postId}`, { params })
