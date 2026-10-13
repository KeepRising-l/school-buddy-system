import request from './request'

// 发布失败由页面用 Dialog 完整展示后端拒绝原因（违禁词/超时长/进行中帖子上限/限流…），
// 故标记 silent 跳过拦截器的 toast，避免"toast 一闪而过 + 弹窗"重复提示
export const createPost = (data) => request.post('/api/post/create', data, { silent: true })
export const getPostDetail = (id) => request.get(`/api/post/detail/${id}`)
export const cancelPost = (id) => request.post(`/api/post/cancel/${id}`)
export const getSquareList = (params) => request.get('/api/square/list', { params })
// 评论发送失败由页面用 Dialog 展示具体原因（如"该组局已取消，暂不可评论"），故标记 silent 跳过拦截器的 toast，
// 避免"toast + 弹窗"重复提示
export const addComment = (data) => request.post('/api/post/comment', data, { silent: true })
export const getComments = (postId, params) => request.get(`/api/post/comments/${postId}`, { params })
